package com.simplemusic.upload;

import com.simplemusic.SimpleMusic;
import com.simplemusic.config.MusicConfig;
import com.simplemusic.disc.DiscPlayback;
import com.simplemusic.disc.TrackRef;
import com.simplemusic.net.SheetEditPayload;
import com.simplemusic.net.TrackDataPayload;
import com.simplemusic.net.TrackRequestPayload;
import com.simplemusic.net.UploadBeginPayload;
import com.simplemusic.net.UploadChunkPayload;
import com.simplemusic.net.UploadResultPayload;
import com.simplemusic.registry.MusicComponents;
import com.simplemusic.sheet.NoteCode;
import com.simplemusic.sheet.SheetContent;
import com.simplemusic.sheet.SheetMusicItem;
import com.simplemusic.sheet.SheetSounds;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Server half of Simple Music's network: saving sheets (the server parses again, all or nothing), uploading tracks onto
 * sheets (chunked upload, Ogg check, quota, moderation U-B; the disc press then presses them onto discs) and handing
 * out track files (only to players a block near them
 * plays the track for, rate limited). One store per running server.
 */
public final class MusicServer {
    private static MinecraftServer server;
    private static TrackStore store;
    private static final Map<UUID, Upload> UPLOADS = new HashMap<>();
    private static final Map<UUID, Deque<Download>> DOWNLOADS = new LinkedHashMap<>();
    /** Clock for the upload rate limit (tests replace it). */
    public static java.util.function.LongSupplier clock = System::currentTimeMillis;

    private record Upload(UploadSession session, int hand) {}

    private static final class Download {
        final String hash;
        final byte[] data;
        int next;

        Download(String hash, byte[] data) {
            this.hash = hash;
            this.data = data;
        }

        int chunks() {
            return (data.length + UploadSession.MAX_CHUNK - 1) / UploadSession.MAX_CHUNK;
        }
    }

    private MusicServer() {}

    /** The track store of the running server ({@code <world>/simplemusic}); null without a server. */
    public static TrackStore store(MinecraftServer current) {
        if (current == null) return null;
        if (server != current || store == null) {
            server = current;
            store = new TrackStore(current.getWorldPath(LevelResource.ROOT).resolve(SimpleMusic.MOD_ID));
            store.cleanTmp();
        }
        return store;
    }

    public static void stopped() {
        server = null;
        store = null;
        UPLOADS.clear();
        DOWNLOADS.clear();
        DiscPlayback.clear();
        com.simplemusic.disc.SheetPlayback.clear();
        com.simplemusic.pocket.PocketPlayback.clear();
    }

    private static boolean op(ServerPlayer player) {
        return Commands.LEVEL_GAMEMASTERS.check(player.permissions());
    }

    /** Single player and the LAN host: always allowed and approved at once (concept §6). */
    public static boolean host(ServerPlayer player) {
        MinecraftServer s = player.level().getServer();
        return s.isSingleplayerOwner(player.nameAndId());
    }

    // ------------------------------------------------------------------ sheets

    public static void handleSheetEdit(SheetEditPayload payload, ServerPlayer player) {
        if (!SimpleMusic.on()) return;
        InteractionHand hand = payload.hand() == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof SheetMusicItem)) return;
        SheetContent old = SheetMusicItem.content(stack);
        SheetContent written = edited(old, payload, player.getName().getString());
        if (written == null) return;
        if (stack.getCount() > 1) {
            ItemStack rest = stack.copyWithCount(stack.getCount() - 1);
            stack.setCount(1);
            stack.set(MusicComponents.SHEET, written);
            player.getInventory().placeItemBackInInventory(rest, net.minecraft.util.Prediction.SERVER_ONLY);
            return;
        }
        stack.set(MusicComponents.SHEET, written);
    }

    /**
     * The new content, or null when refused: signed sheets are read-only; an unsigned draft may still have problems
     * (it is never pressed: the disc press only takes codes that parse), signing needs a title and a code that parses with
     * the server rules (sound blocklist).
     */
    public static SheetContent edited(SheetContent old, SheetEditPayload payload, String author) {
        if (old.signed()) return null;
        if (payload.code().length() > NoteCode.MAX_CODE_LENGTH) return null;
        String title = payload.title().strip();
        if (title.length() > NoteCode.MAX_TITLE_LENGTH) return null;
        if (payload.sign()) {
            if (title.isEmpty() || payload.code().isBlank()) return null;
            if (!NoteCode.parse(payload.code(), SheetSounds.policy()).ok()) return null;
            return new SheetContent(payload.code(), title, author, true);
        }
        return new SheetContent(payload.code(), title, "", false);
    }

    // ------------------------------------------------------------------ uploads

    private static void reply(ServerPlayer player, String key, Object... args) {
        List<String> list = new ArrayList<>();
        for (Object a : args) list.add(String.valueOf(a));
        SimpleMusic.toClient.accept(player, new UploadResultPayload(key, list));
    }

    /** Why this player may not burn now, or null. */
    public static String refusal(ServerPlayer player, int size) {
        TrackStore s = store(player.level().getServer());
        boolean host = host(player);
        if (!host) {
            if (MusicConfig.uploads == MusicConfig.Uploads.OFF) return "uploads_off";
            if (MusicConfig.uploads == MusicConfig.Uploads.OP && !op(player)) return "ops_only";
            if (s.banned(player.getUUID())) return "banned";
            if (s.countBy(player.getUUID()) >= MusicConfig.quotaPerPlayer) return "quota";
        }
        if (size <= 0) return "empty";
        if (size > MusicConfig.maxUploadMiB << 20) return "too_big";
        if (s.totalBytes() + size > ((long) MusicConfig.storeLimitMiB << 20)) return "store_full";
        if (UPLOADS.containsKey(player.getUUID())) return "busy";
        return null;
    }

    public static void handleUploadBegin(UploadBeginPayload payload, ServerPlayer player) {
        if (!SimpleMusic.on()) return;
        UPLOADS.values().removeIf(u -> u.session().expired(clock.getAsLong()));
        InteractionHand hand = payload.hand() == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (!uploadTarget(player.getItemInHand(hand))) {
            reply(player, "no_sheet");
            return;
        }
        String title = payload.title().strip();
        if (title.isEmpty() || title.length() > NoteCode.MAX_TITLE_LENGTH) {
            reply(player, "bad_title", NoteCode.MAX_TITLE_LENGTH);
            return;
        }
        String refused = refusal(player, payload.size());
        if (refused != null) {
            switch (refused) {
                case "quota" -> reply(player, refused, MusicConfig.quotaPerPlayer);
                case "too_big" -> reply(player, refused, MusicConfig.maxUploadMiB);
                default -> reply(player, refused);
            }
            return;
        }
        UPLOADS.put(player.getUUID(), new Upload(new UploadSession(payload.size(), title, MusicConfig.uploadKiBPerSecond * 1024L, clock.getAsLong()), payload.hand()));
        reply(player, "next", 0, MusicConfig.uploadKiBPerSecond);
    }

    public static void handleUploadChunk(UploadChunkPayload payload, ServerPlayer player) {
        Upload upload = UPLOADS.get(player.getUUID());
        if (upload == null) return;
        String problem = upload.session().accept(payload.index(), payload.bytes(), clock.getAsLong());
        if (problem != null) {
            UPLOADS.remove(player.getUUID());
            reply(player, problem);
            return;
        }
        if (!upload.session().complete()) {
            reply(player, "next", payload.index() + 1, MusicConfig.uploadKiBPerSecond);
            return;
        }
        UPLOADS.remove(player.getUUID());
        finish(player, upload);
    }

    /** An upload goes onto an unsigned sheet of music (owner 21.10.: uploads run through the sheet's editor). */
    public static boolean uploadTarget(ItemStack stack) {
        return stack.getItem() instanceof SheetMusicItem && !SheetMusicItem.content(stack).signed();
    }

    /** Checks and stores a complete upload, turns the sheet into an upload sheet (track + signed title). */
    private static void finish(ServerPlayer player, Upload upload) {
        byte[] data = upload.session().bytes();
        OggCheck.Result check = OggCheck.check(data, MusicConfig.uploadLimits());
        if (!check.ok()) {
            reply(player, check.key(), check.args().toArray());
            return;
        }
        InteractionHand hand = upload.hand() == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack sheet = player.getItemInHand(hand);
        if (!uploadTarget(sheet)) {
            reply(player, "no_sheet");
            return;
        }
        byte[] clean = OggCheck.strip(data);
        boolean approved = host(player) || MusicConfig.uploads == MusicConfig.Uploads.EVERYONE
                || (MusicConfig.uploads == MusicConfig.Uploads.OP && op(player));
        TrackStore.Track track;
        try {
            track = store(player.level().getServer()).add(clean, upload.session().title(), check.info().seconds(), player.getUUID(),
                    player.getName().getString(), approved, System.currentTimeMillis());
        } catch (IOException e) {
            SimpleMusic.LOG.warn("Could not store an upload: {}", e.toString());
            reply(player, "store_failed");
            return;
        }
        if (track.status() == TrackStore.Status.REJECTED || track.status() == TrackStore.Status.DELETED) {
            reply(player, "removed_before");
            return;
        }
        ItemStack done = sheet.copyWithCount(1);
        done.set(MusicComponents.TRACK, new TrackRef(track.hash(), upload.session().title(), track.seconds(), player.getName().getString()));
        done.set(MusicComponents.SHEET, new SheetContent("", upload.session().title(), player.getName().getString(), true));
        // like saving a sheet: the hand keeps the written one, the rest of a stack goes back blank
        ItemStack rest = sheet.getCount() > 1 ? sheet.copyWithCount(sheet.getCount() - 1) : ItemStack.EMPTY;
        player.setItemInHand(hand, done);
        if (!rest.isEmpty()) player.getInventory().placeItemBackInInventory(rest, net.minecraft.util.Prediction.SERVER_ONLY);
        reply(player, track.status() == TrackStore.Status.APPROVED ? "accepted" : "pending", track.hash().substring(0, 8));
    }

    // ------------------------------------------------------------------ downloads

    public static void handleTrackRequest(TrackRequestPayload payload, ServerPlayer player) {
        String hash = payload.sha256();
        if (!UploadSession.isHash(hash)) return;
        TrackStore s = store(player.level().getServer());
        TrackStore.Track track = s.get(hash);
        if (track == null || !track.audibleFor(player.getUUID()) || !DiscPlayback.playsFor(player, hash)
                && !com.simplemusic.pocket.PocketPlayback.playsFor(player, hash)) return;
        Deque<Download> queue = DOWNLOADS.computeIfAbsent(player.getUUID(), k -> new ArrayDeque<>());
        if (queue.stream().anyMatch(d -> d.hash.equals(hash)) || queue.size() >= 3) return;
        try {
            queue.add(new Download(hash, s.read(hash)));
        } catch (IOException e) {
            SimpleMusic.LOG.warn("Could not read track {}: {}", hash, e.toString());
        }
    }

    /** Sends download chunks within the global rate (config {@code downloadKiBPerSecond}), round robin. */
    public static void tick(MinecraftServer current) {
        com.simplemusic.pocket.PocketPlayback.tick(current);
        if (DOWNLOADS.isEmpty()) return;
        long budget = MusicConfig.downloadKiBPerSecond * 1024L / 20;
        boolean sent = true;
        while (budget > 0 && sent) {
            sent = false;
            Iterator<Map.Entry<UUID, Deque<Download>>> it = DOWNLOADS.entrySet().iterator();
            while (it.hasNext() && budget > 0) {
                Map.Entry<UUID, Deque<Download>> e = it.next();
                ServerPlayer player = current.getPlayerList().getPlayer(e.getKey());
                Download d = e.getValue().peek();
                if (player == null || d == null) {
                    it.remove();
                    continue;
                }
                int from = d.next * UploadSession.MAX_CHUNK;
                int to = Math.min(d.data.length, from + UploadSession.MAX_CHUNK);
                SimpleMusic.toClient.accept(player, new TrackDataPayload(d.hash, d.next, d.data.length, java.util.Arrays.copyOfRange(d.data, from, to)));
                budget -= to - from;
                sent = true;
                if (++d.next >= d.chunks()) e.getValue().poll();
                if (e.getValue().isEmpty()) it.remove();
            }
        }
    }

    /** Test access: pending downloads of a player. */
    public static int queuedDownloads(UUID player) {
        Deque<Download> q = DOWNLOADS.get(player);
        return q == null ? 0 : q.size();
    }

    public static boolean uploading(UUID player) {
        return UPLOADS.containsKey(player);
    }
}

package com.simplemusic.pocket;

import com.simplemusic.SimpleMusic;
import com.simplemusic.disc.CustomDiscItem;
import com.simplemusic.disc.TrackRef;
import com.simplemusic.net.TrackPlayPayload;
import com.simplemusic.registry.MusicComponents;
import com.simplemusic.upload.MusicServer;
import com.simplemusic.upload.TrackStore;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.component.BundleContents;

/**
 * Server side of the Pocket Jukebox: at most one session per player (starting another jukebox pauses the first). The
 * carrier alone gets a {@link TrackPlayPayload#carried carried} play message - the Vanilla song's sound event or the
 * upload hash, with the position - and hears it on the client, not positional. Discs play in the order they went in,
 * after the last one the first again. A session stops when its disc is gone from the jukebox or the jukebox from the
 * player's inventory (cursor included). Kept in memory only: a restart or log out ends it, the stack keeps the position
 * it was last paused at.
 */
public final class PocketPlayback {
    /** What a disc plays: an upload hash or (song) a Vanilla disc's sound event, and its length in ticks. */
    public record Track(String key, boolean song, int ticks) {}

    private static final class Session {
        final long id;
        final int fromEnd;
        final ItemStack disc;
        final Track track;
        final long started;

        Session(long id, int fromEnd, ItemStack disc, Track track, long started) {
            this.id = id;
            this.fromEnd = fromEnd;
            this.disc = disc;
            this.track = track;
            this.started = started;
        }
    }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private PocketPlayback() {}

    public static PocketState state(ItemStack jukebox) {
        return jukebox.getOrDefault(MusicComponents.POCKET, PocketState.NEW);
    }

    /** Whether {@code jukebox} is the one playing for the player. */
    public static boolean active(ServerPlayer player, ItemStack jukebox) {
        Session s = SESSIONS.get(player.getUUID());
        return s != null && state(jukebox).id() == s.id;
    }

    /** The disc (counted from the first one put in) playing for the player, or -1 (tests). */
    public static int playing(ServerPlayer player) {
        Session s = SESSIONS.get(player.getUUID());
        return s == null ? -1 : s.fromEnd;
    }

    /** Ticks into the current disc, or -1 (tests). */
    public static long elapsed(ServerPlayer player) {
        Session s = SESSIONS.get(player.getUUID());
        return s == null ? -1 : now(player) - s.started;
    }

    /** Whether a pocket jukebox plays {@code hash} for the player (the download gate). */
    public static boolean playsFor(ServerPlayer player, String hash) {
        Session s = SESSIONS.get(player.getUUID());
        return s != null && s.track.key().equals(hash);
    }

    /** Pauses {@code jukebox} if it is the one playing for the player (writes the position); true when it was. */
    public static boolean pauseIfActive(ServerPlayer player, ItemStack jukebox) {
        Session s = SESSIONS.get(player.getUUID());
        if (s == null || state(jukebox).id() != s.id) return false;
        pause(player, jukebox, s);
        return true;
    }

    /** Use: pause the playing jukebox, otherwise start this one (pausing another). True when it plays now. */
    public static boolean toggle(ServerPlayer player, ItemStack jukebox) {
        Session s = SESSIONS.get(player.getUUID());
        if (s != null && state(jukebox).id() == s.id) {
            pause(player, jukebox, s);
            return false;
        }
        if (s != null) {
            ItemStack other = find(player, s.id);
            if (other != null) pause(player, other, s);
            else end(player);
        }
        return start(player, jukebox);
    }

    private static boolean start(ServerPlayer player, ItemStack jukebox) {
        BundleContents contents = PocketJukeboxItem.contents(jukebox);
        PocketState st = state(jukebox);
        long id = st.id();
        while (id == 0L) id = player.getRandom().nextLong();
        int size = contents.size();
        if (size == 0) return false;
        int from = Math.floorMod(st.fromEnd(), size);
        for (int i = 0; i < size; i++) {
            int fromEnd = (from + i) % size;
            ItemStack disc = PocketJukeboxItem.disc(contents, fromEnd);
            Track track = track(player, disc);
            if (track == null) continue;
            int ticks = i == 0 && st.ticks() < track.ticks() ? Math.max(0, st.ticks()) : 0;
            begin(player, jukebox, id, fromEnd, disc, track, ticks);
            return true;
        }
        PocketJukeboxItem.sound(player, SoundEvents.BUNDLE_INSERT_FAIL);
        return false;
    }

    private static void begin(ServerPlayer player, ItemStack jukebox, long id, int fromEnd, ItemStack disc, Track track, int ticks) {
        SESSIONS.put(player.getUUID(), new Session(id, fromEnd, disc.copy(), track, now(player) - ticks));
        jukebox.set(MusicComponents.POCKET, new PocketState(id, fromEnd, ticks));
        PocketJukeboxItem.select(jukebox, PocketJukeboxItem.contents(jukebox).size() - 1 - fromEnd);
        SimpleMusic.toClient.accept(player, TrackPlayPayload.carried(track.key(), ticks, track.ticks() / 20));
    }

    private static void pause(ServerPlayer player, ItemStack jukebox, Session s) {
        int ticks = (int) Math.min(Integer.MAX_VALUE, now(player) - s.started);
        jukebox.set(MusicComponents.POCKET, new PocketState(s.id, s.fromEnd, ticks));
        PocketJukeboxItem.select(jukebox, -1);
        end(player);
    }

    /** Ends the player's session and tells the client. */
    private static void end(ServerPlayer player) {
        if (SESSIONS.remove(player.getUUID()) != null) SimpleMusic.toClient.accept(player, TrackPlayPayload.carriedStop());
    }

    /** What a disc plays for the player: a Vanilla song, or an upload the player may hear; null = skip it. */
    private static Track track(ServerPlayer player, ItemStack disc) {
        return track(player.level().getServer(), disc, player.getUUID());
    }

    /**
     * What a disc plays: a Vanilla song, or an upload the server has and {@code listener} may hear ({@code null} = any
     * stored upload: a placed jukebox, where {@link com.simplemusic.disc.DiscPlayback} asks each listener); null = skip it.
     */
    public static Track track(MinecraftServer server, ItemStack disc, UUID listener) {
        TrackRef ref = CustomDiscItem.track(disc);
        if (disc.getItem() instanceof CustomDiscItem && ref != null) {
            TrackStore store = MusicServer.store(server);
            TrackStore.Track stored = store == null ? null : store.get(ref.sha256());
            if (stored == null || listener != null && !stored.audibleFor(listener)) return null;
            return new Track(ref.sha256(), false, Math.max(1, ref.seconds()) * 20);
        }
        Optional<Holder<JukeboxSong>> song = JukeboxSong.fromStack(disc);
        if (song.isEmpty()) return null;
        JukeboxSong value = song.get().value();
        return new Track(value.soundEvent().value().location().toString(), true, Math.max(20, value.lengthInTicks()));
    }

    /** The jukebox with this session id in the player's inventory or on the cursor, or null. */
    static ItemStack find(ServerPlayer player, long id) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof PocketJukeboxItem && state(stack).id() == id) return stack;
        }
        ItemStack carried = player.containerMenu == null ? ItemStack.EMPTY : player.containerMenu.getCarried();
        return carried.getItem() instanceof PocketJukeboxItem && state(carried).id() == id ? carried : null;
    }

    private static long now(ServerPlayer player) {
        return player.level().getServer().getTickCount();
    }

    /** Once per server tick: stop what lost its jukebox or disc, go on to the next disc at the end of one. */
    public static void tick(MinecraftServer server) {
        if (SESSIONS.isEmpty()) return;
        for (Map.Entry<UUID, Session> e : Map.copyOf(SESSIONS).entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(e.getKey());
            Session s = e.getValue();
            if (player == null) {
                SESSIONS.remove(e.getKey());
                continue;
            }
            ItemStack jukebox = find(player, s.id);
            if (jukebox == null || !SimpleMusic.on()) {
                if (jukebox != null) PocketJukeboxItem.select(jukebox, -1);
                end(player);
                continue;
            }
            BundleContents contents = PocketJukeboxItem.contents(jukebox);
            if (!ItemStack.isSameItemSameComponents(PocketJukeboxItem.disc(contents, s.fromEnd), s.disc)) {
                // the playing disc was taken out: stop, the next use starts at the top of the next one
                jukebox.set(MusicComponents.POCKET, new PocketState(s.id, contents.isEmpty() ? 0 : s.fromEnd % contents.size(), 0));
                PocketJukeboxItem.select(jukebox, -1);
                end(player);
                continue;
            }
            PocketJukeboxItem.select(jukebox, contents.size() - 1 - s.fromEnd);
            if (server.getTickCount() - s.started < s.track.ticks()) continue;
            int size = contents.size();
            boolean next = false;
            for (int i = 1; i <= size && !next; i++) {
                int fromEnd = (s.fromEnd + i) % size;
                ItemStack disc = PocketJukeboxItem.disc(contents, fromEnd);
                Track track = track(player, disc);
                if (track == null) continue;
                begin(player, jukebox, s.id, fromEnd, disc, track, 0);
                next = true;
            }
            if (!next) {
                jukebox.set(MusicComponents.POCKET, new PocketState(s.id, s.fromEnd, 0));
                PocketJukeboxItem.select(jukebox, -1);
                end(player);
            }
        }
    }

    /** Server stop: forget everything. */
    public static void clear() {
        SESSIONS.clear();
    }
}

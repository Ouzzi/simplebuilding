package com.simplemusic.pocket;

import com.simplemusic.SimpleMusic;
import com.simplemusic.disc.CustomDiscItem;
import com.simplemusic.disc.DiscPlayback;
import com.simplemusic.disc.TrackRef;
import com.simplemusic.net.TrackPlayPayload;
import com.simplemusic.registry.MusicComponents;
import com.simplemusic.upload.MusicServer;
import com.simplemusic.upload.TrackStore;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Server side of the Pocket Jukebox away from a block: one session per jukebox (its id in {@code simplemusic:pocket}),
 * at most one carried per player (starting another pauses the first). Discs play in the order they went in, after the
 * last one the first again.
 *
 * <p>Who holds it (PJ23-5, owner 23.10.: "Wiedergabe läuft weiter ... im Inventar, als gedropptes Item ... beim
 * Abstellen/Aufheben ohne Unterbrechung"): the <em>carrier</em> (inventory or cursor) alone hears it, not positional; a
 * <em>dropped</em> jukebox plays at the item entity for everyone within {@link DiscPlayback#RANGE} and follows it. When the
 * holder loses it (a creative cursor the server does not see, a pickup, a break of the placed block) the session is
 * <em>lost</em> and looked for every tick - in any player's inventory, then as an item near where it was - for
 * {@link #GRACE} ticks before it stops. A change of holder never sends a stop: the client moves the sound of the same
 * session (see {@link TrackPlayPayload#session}). Kept in memory only: a restart or log out ends it, the stack keeps the
 * position it was last paused at.
 */
public final class PocketPlayback {
    /** What a disc plays: an upload hash or (song) a Vanilla disc's sound event, and its length in ticks. */
    public record Track(String key, boolean song, int ticks) {}

    /** How long a lost jukebox goes on playing while it is looked for (a creative cursor): 10 s. */
    public static final int GRACE = 200;
    /** A broken placed jukebox drops at once: its item is looked for 1 s. */
    public static final int BLOCK_GRACE = 20;
    /** How far from where it was lost a dropped jukebox is looked for. */
    static final double SEARCH = 16.0;

    private static final class Session {
        final long id;
        int fromEnd;
        ItemStack disc;
        Track track;
        long started;
        /** The player carrying it, or null. */
        UUID carrier;
        /** The dropped item entity, or -1. */
        int entity = -1;
        ResourceKey<Level> dim;
        Vec3 where = Vec3.ZERO;
        /** Server tick it went missing, -1 while held; {@link #grace} ticks later it stops. */
        long lost = -1;
        int grace = GRACE;
        /** Players hearing it at the dropped item. */
        final Set<UUID> told = new HashSet<>();

        Session(long id) {
            this.id = id;
        }
    }

    private static final Map<Long, Session> SESSIONS = new HashMap<>();

    private PocketPlayback() {}

    public static PocketState state(ItemStack jukebox) {
        return jukebox.getOrDefault(MusicComponents.POCKET, PocketState.NEW);
    }

    private static Session carried(ServerPlayer player) {
        for (Session s : SESSIONS.values()) if (player.getUUID().equals(s.carrier)) return s;
        return null;
    }

    /** Whether {@code jukebox} plays for the player (carried, or lost and just turning up again). */
    public static boolean active(ServerPlayer player, ItemStack jukebox) {
        long id = state(jukebox).id();
        Session s = id == 0L ? null : SESSIONS.get(id);
        return s != null && (player.getUUID().equals(s.carrier) || s.lost >= 0);
    }

    /** Whether the jukebox with this id plays anywhere (tests). */
    public static boolean playing(long id) {
        return SESSIONS.containsKey(id);
    }

    /** The dropped item a session plays at, or -1 (tests). */
    public static int entityOf(long id) {
        Session s = SESSIONS.get(id);
        return s == null ? -1 : s.entity;
    }

    /** The player carrying a playing session, or null (tests). */
    public static UUID carrierOf(long id) {
        Session s = SESSIONS.get(id);
        return s == null ? null : s.carrier;
    }

    /** The disc (counted from the first one put in) playing for the player, or -1 (tests). */
    public static int playing(ServerPlayer player) {
        Session s = carried(player);
        return s == null ? -1 : s.fromEnd;
    }

    /** Ticks into the current disc, or -1 (tests). */
    public static long elapsed(ServerPlayer player) {
        Session s = carried(player);
        return s == null ? -1 : now(player.level().getServer()) - s.started;
    }

    /** Whether a pocket jukebox plays {@code hash} for the player (the download gate). */
    public static boolean playsFor(ServerPlayer player, String hash) {
        for (Session s : SESSIONS.values()) {
            if (s.track.key().equals(hash) && (player.getUUID().equals(s.carrier) || s.told.contains(player.getUUID()))) return true;
        }
        return false;
    }

    /**
     * Putting it down: if {@code jukebox} plays for the player its position goes into the stack and the session ends
     * <em>without</em> a stop, the block then starts the same session at the same position (PJ23-5). True when it played.
     */
    public static boolean handOverToBlock(ServerPlayer player, ItemStack jukebox) {
        Session s = carried(player);
        if (s == null || state(jukebox).id() != s.id) return false;
        jukebox.set(MusicComponents.POCKET, new PocketState(s.id, s.fromEnd, ticks(player.level().getServer(), s)));
        jukebox.remove(MusicComponents.PLAYING);
        SESSIONS.remove(s.id);
        return true;
    }

    /**
     * A placed jukebox was removed while it played (broken, pushed, blown up): the session goes on, lost, and moves to the
     * dropped item that turns up nearby; who heard it at the block hears it there (PJ23-5).
     */
    public static void blockRemoved(ServerLevel level, BlockPos pos, long id, int fromEnd, ItemStack disc, Track track, long elapsed, Set<UUID> told) {
        if (id == 0L || track == null || SESSIONS.containsKey(id)) return;
        Session s = new Session(id);
        s.fromEnd = fromEnd;
        s.disc = disc.copy();
        s.track = track;
        s.started = now(level.getServer()) - Math.max(0, elapsed);
        s.dim = level.dimension();
        s.where = Vec3.atCenterOf(pos);
        s.lost = now(level.getServer());
        s.grace = BLOCK_GRACE;
        s.told.addAll(told);
        SESSIONS.put(id, s);
    }

    /** Use: pause the playing jukebox, otherwise start this one (pausing another). True when it plays now. */
    public static boolean toggle(ServerPlayer player, ItemStack jukebox) {
        Session s = carried(player);
        long id = state(jukebox).id();
        if (s != null && id == s.id) {
            pause(player.level().getServer(), s, jukebox);
            startStopSound(player.level(), player.position(), false);
            return false;
        }
        if (s != null) {
            ItemStack other = find(player, s.id);
            if (other != null) pause(player.level().getServer(), s, other);
            else end(player.level().getServer(), s);
        }
        boolean started = start(player, jukebox);
        if (started) startStopSound(player.level(), player.position(), true);
        return started;
    }

    /** Starts it for the carrier again (a put down that did not happen), without a sound. */
    public static boolean resume(ServerPlayer player, ItemStack jukebox) {
        return carried(player) == null && start(player, jukebox);
    }

    private static boolean start(ServerPlayer player, ItemStack jukebox) {
        BundleContents contents = PocketJukeboxItem.contents(jukebox);
        PocketState st = state(jukebox);
        long id = st.id();
        Session running = SESSIONS.get(id);
        if (running != null) {
            // still playing somewhere (dropped, lost): that session is this jukebox, end it first
            end(player.level().getServer(), running);
        }
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
            Session s = new Session(id);
            SESSIONS.put(id, s);
            s.carrier = player.getUUID();
            s.dim = player.level().dimension();
            s.where = player.position();
            begin(player.level().getServer(), s, jukebox, fromEnd, disc, track, ticks);
            return true;
        }
        PocketJukeboxItem.sound(player, SoundEvents.BUNDLE_INSERT_FAIL);
        return false;
    }

    /** Plays {@code disc} {@code ticks} in at the session's holder and tells who hears it. */
    private static void begin(MinecraftServer server, Session s, ItemStack jukebox, int fromEnd, ItemStack disc, Track track, int ticks) {
        s.fromEnd = fromEnd;
        s.disc = disc.copy();
        s.track = track;
        s.started = now(server) - ticks;
        s.lost = -1;
        jukebox.set(MusicComponents.POCKET, new PocketState(s.id, fromEnd, ticks));
        jukebox.set(MusicComponents.PLAYING, Unit.INSTANCE);
        ServerPlayer carrier = s.carrier == null ? null : server.getPlayerList().getPlayer(s.carrier);
        if (carrier != null) SimpleMusic.toClient.accept(carrier, TrackPlayPayload.carried(track.key(), ticks, track.ticks() / 20, s.id));
        else if (s.entity >= 0) tell(server, s, true);
    }

    private static int ticks(MinecraftServer server, Session s) {
        return (int) Math.min(Integer.MAX_VALUE, now(server) - s.started);
    }

    /** Pause: the position goes into the stack, everyone who hears it is told to stop. */
    private static void pause(MinecraftServer server, Session s, ItemStack jukebox) {
        jukebox.set(MusicComponents.POCKET, new PocketState(s.id, s.fromEnd, ticks(server, s)));
        jukebox.remove(MusicComponents.PLAYING);
        end(server, s);
    }

    /** Ends the session and tells its carrier and listeners. */
    private static void end(MinecraftServer server, Session s) {
        if (SESSIONS.remove(s.id) == null) return;
        ServerPlayer carrier = s.carrier == null ? null : server.getPlayerList().getPlayer(s.carrier);
        if (carrier != null) SimpleMusic.toClient.accept(carrier, TrackPlayPayload.carriedStop(s.id));
        for (UUID id : s.told) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null && player != carrier) SimpleMusic.toClient.accept(player, TrackPlayPayload.stop(BlockPos.ZERO, s.id));
        }
        s.told.clear();
    }

    /**
     * Start (Vanilla's copper bulb click and a disc laid on like into a chiseled bookshelf) or pause (the bulb's off click
     * and the disc lifted): a record player's sounds from Vanilla ones, for everyone near (PJ23-3, variant A).
     */
    public static void startStopSound(Level level, Vec3 at, boolean start) {
        level.playSound(null, at.x, at.y, at.z, start ? SoundEvents.COPPER_BULB_TURN_ON : SoundEvents.COPPER_BULB_TURN_OFF,
                SoundSource.RECORDS, 0.6F, start ? 1.4F : 1.2F);
        level.playSound(null, at.x, at.y, at.z, start ? SoundEvents.CHISELED_BOOKSHELF_INSERT : SoundEvents.CHISELED_BOOKSHELF_PICKUP,
                SoundSource.RECORDS, 0.7F, 1.3F);
    }

    /** What a disc plays for the player: a Vanilla song, or an upload the player may hear; null = skip it. */
    private static Track track(ServerPlayer player, ItemStack disc) {
        return track(player.level().getServer(), disc, player.getUUID());
    }

    /**
     * What a disc plays: a Vanilla song, or an upload the server has and {@code listener} may hear ({@code null} = any
     * stored upload: a placed or dropped jukebox, where each listener is asked); null = skip it.
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
            if (ours(stack, id)) return stack;
        }
        ItemStack carried = player.containerMenu == null ? ItemStack.EMPTY : player.containerMenu.getCarried();
        return ours(carried, id) ? carried : null;
    }

    private static boolean ours(ItemStack stack, long id) {
        return stack.getItem() instanceof PocketJukeboxItem && state(stack).id() == id;
    }

    private static long now(MinecraftServer server) {
        return server.getTickCount();
    }

    /** Once per server tick: follow each jukebox to its holder, stop what lost its disc, go on to the next disc at the end of one. */
    public static void tick(MinecraftServer server) {
        if (SESSIONS.isEmpty()) return;
        for (Session s : Map.copyOf(SESSIONS).values()) {
            if (SESSIONS.get(s.id) != s) continue;
            ServerLevel level = s.dim == null ? null : server.getLevel(s.dim);
            ItemStack jukebox = null;
            ItemEntity item = null;
            if (s.carrier != null) {
                ServerPlayer player = server.getPlayerList().getPlayer(s.carrier);
                if (player == null) {
                    SESSIONS.remove(s.id);
                    continue;
                }
                jukebox = find(player, s.id);
                if (jukebox != null) {
                    level = player.level();
                    s.dim = level.dimension();
                    s.where = player.position();
                }
            } else if (s.entity >= 0 && level != null) {
                Entity entity = level.getEntity(s.entity);
                if (entity instanceof ItemEntity e && e.isAlive() && ours(e.getItem(), s.id)) {
                    item = e;
                    jukebox = e.getItem();
                    s.where = e.position();
                }
            }
            if (jukebox == null) {
                if (s.lost < 0) s.lost = now(server);
                if (!SimpleMusic.on() || level == null || !adopt(server, s, level) && now(server) - s.lost > s.grace) end(server, s);
                continue;
            }
            s.lost = -1;
            if (!SimpleMusic.on()) {
                pause(server, s, jukebox);
                if (item != null) item.setItem(jukebox.copy());
                continue;
            }
            follow(server, s, level, jukebox, item);
        }
    }

    /** The held session's tick: the disc still there (or moved up), the next disc at the end, the item's listeners. */
    private static void follow(MinecraftServer server, Session s, ServerLevel level, ItemStack jukebox, ItemEntity item) {
        BundleContents contents = PocketJukeboxItem.contents(jukebox);
        if (!ItemStack.isSameItemSameComponents(PocketJukeboxItem.disc(contents, s.fromEnd), s.disc)) {
            int moved = -1;
            for (int i = 0; i < contents.size() && moved < 0; i++) {
                if (ItemStack.isSameItemSameComponents(PocketJukeboxItem.disc(contents, i), s.disc)) moved = i;
            }
            if (moved >= 0) {
                // another disc was taken out before this one: it plays on, one place further up the list
                s.fromEnd = moved;
                jukebox.set(MusicComponents.POCKET, new PocketState(s.id, moved, state(jukebox).ticks()));
            } else {
                // the playing disc was taken out: stop, the next use starts at the top of the next one
                jukebox.set(MusicComponents.POCKET, new PocketState(s.id, contents.isEmpty() ? 0 : s.fromEnd % contents.size(), 0));
                jukebox.remove(MusicComponents.PLAYING);
                end(server, s);
                if (item != null) item.setItem(jukebox.copy());
                return;
            }
        }
        if (!jukebox.has(MusicComponents.PLAYING)) {
            jukebox.set(MusicComponents.PLAYING, Unit.INSTANCE);
            if (item != null) item.setItem(jukebox.copy());
        }
        long elapsed = now(server) - s.started;
        if (elapsed < s.track.ticks()) {
            if (item != null && elapsed % 20 == 0) {
                tell(server, s, false);
                level.sendParticles(ParticleTypes.NOTE, s.where.x, s.where.y + 0.6, s.where.z, 0, level.getRandom().nextInt(4) / 24.0F, 0.0, 0.0, 1.0);
            }
            return;
        }
        int size = contents.size();
        ServerPlayer carrier = s.carrier == null ? null : server.getPlayerList().getPlayer(s.carrier);
        for (int i = 1; i <= size; i++) {
            int fromEnd = (s.fromEnd + i) % size;
            ItemStack disc = PocketJukeboxItem.disc(contents, fromEnd);
            Track track = carrier != null ? track(carrier, disc) : track(server, disc, null);
            if (track == null) continue;
            begin(server, s, jukebox, fromEnd, disc, track, 0);
            if (item != null) item.setItem(jukebox.copy());
            return;
        }
        jukebox.set(MusicComponents.POCKET, new PocketState(s.id, s.fromEnd, 0));
        jukebox.remove(MusicComponents.PLAYING);
        end(server, s);
        if (item != null) item.setItem(jukebox.copy());
    }

    /** A lost session looks for its jukebox: in a player's inventory (picked up, back from the cursor), else dropped nearby. */
    private static boolean adopt(MinecraftServer server, Session s, ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            ItemStack stack = find(player, s.id);
            if (stack == null) continue;
            Session other = carried(player);
            if (other != null && other != s) {
                // the finder already carries a playing jukebox: this one pauses
                pause(server, s, stack);
                return true;
            }
            for (UUID id : s.told) {
                ServerPlayer listener = server.getPlayerList().getPlayer(id);
                if (listener != null && listener != player) SimpleMusic.toClient.accept(listener, TrackPlayPayload.stop(BlockPos.ZERO, s.id));
            }
            ServerPlayer before = s.carrier == null ? null : server.getPlayerList().getPlayer(s.carrier);
            if (before != null && before != player) SimpleMusic.toClient.accept(before, TrackPlayPayload.carriedStop(s.id));
            s.told.clear();
            s.carrier = player.getUUID();
            s.entity = -1;
            s.lost = -1;
            s.where = player.position();
            stack.set(MusicComponents.PLAYING, Unit.INSTANCE);
            SimpleMusic.toClient.accept(player, TrackPlayPayload.carried(s.track.key(), ticks(server, s), s.track.ticks() / 20, s.id));
            return true;
        }
        AABB around = new AABB(s.where, s.where).inflate(SEARCH);
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, around, e -> e.isAlive() && ours(e.getItem(), s.id))) {
            if (s.carrier != null) s.told.add(s.carrier);
            s.carrier = null;
            s.entity = e.getId();
            s.lost = -1;
            s.where = e.position();
            ItemStack stack = e.getItem().copy();
            stack.set(MusicComponents.PLAYING, Unit.INSTANCE);
            e.setItem(stack);
            tell(server, s, true);
            return true;
        }
        return false;
    }

    /**
     * Players within range of the dropped jukebox hear it (all of them when {@code all}: it just moved or changed disc),
     * listeners that left are told to stop.
     */
    private static void tell(MinecraftServer server, Session s, boolean all) {
        ServerLevel level = s.dim == null ? null : server.getLevel(s.dim);
        if (level == null) return;
        int offset = ticks(server, s);
        Set<UUID> near = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(s.where) > (double) DiscPlayback.RANGE * DiscPlayback.RANGE) continue;
            if (!s.track.song() && track(server, s.disc, player.getUUID()) == null) continue;
            near.add(player.getUUID());
            if (s.told.add(player.getUUID()) || all) {
                SimpleMusic.toClient.accept(player, TrackPlayPayload.dropped(BlockPos.containing(s.where), s.track.key(), offset,
                        Math.max(1, s.track.ticks() / 20), s.id, s.entity));
            }
        }
        for (UUID id : Set.copyOf(s.told)) {
            if (near.contains(id)) continue;
            s.told.remove(id);
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) SimpleMusic.toClient.accept(player, TrackPlayPayload.stop(BlockPos.ZERO, s.id));
        }
    }

    /** Server stop: forget everything. */
    public static void clear() {
        SESSIONS.clear();
    }
}

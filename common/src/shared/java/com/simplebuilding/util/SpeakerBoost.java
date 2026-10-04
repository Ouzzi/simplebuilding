package com.simplebuilding.util;

import com.simplebuilding.blocks.custom.SpeakerBlock;
import com.simplebuilding.config.ServerTuning;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Lautsprecher (Besitzer 2026-10-03/04, {@code McVersion.MUSIC_DISCS}): Musik-Verstärker gehoeren zum
 * Plattenspieler, Noten-Verstärker zum Notenblock.
 *
 * <p><b>Verstaerkung:</b> direkt angrenzende Lautsprecher der passenden Art (die sechs Nachbarn, hoechstens
 * {@code server.speakers.maxSpeakers}, Standard 2, hart 3) erhoehen die Hoerweite der Quelle: Faktor =
 * 1 + Anzahl x {@code boostPercent} (Standard 50 %, hart 50 %), also hoechstens 2,5-fach. Lautstaerken ueber 1 heben in
 * Minecraft nicht den Pegel, sondern die Reichweite der linearen Abschwaechung (16 Bloecke je Lautstaerke-Einheit).
 *
 * <p><b>Kette (2026-10-04):</b> ein Lautsprecher an der Quelle oder an einem schon gespeisten Lautsprecher derselben
 * Art gibt den Ton weiter (Breitensuche ueber die sechs Nachbarn, nur geladene Chunks, hoechstens
 * {@code server.speakers.maxChain} Lautsprecher, Standard 16, hart 64). Jeder Kettenlautsprecher ist ein weiterer
 * Abspielpunkt derselben Wiedergabe mit der Lautstaerke der Quelle - nicht lauter, unverzoegert. Jeder Spieler hoert
 * die Quelle genau einmal, am naechsten Abspielpunkt ({@link #nearest}): der Notenblock schickt je Spieler ein
 * Klang-Paket ({@link #playChained}), der Plattenspieler-Klang des Clients wandert mit dem Spieler zum naechsten Punkt
 * ({@code ChainedJukeboxSound}). Ketten werden zwischengespeichert; jedes Setzen oder Abbauen eines Lautsprechers
 * macht den Speicher ungueltig ({@link #invalidateChains}), spaetestens nach {@link #CACHE_TICKS} wird neu gesucht.
 */
public final class SpeakerBoost {
    /** Welche Klangquelle ein Lautsprecher verstaerkt. */
    public enum Source {
        /** Plattenspieler (Musik-Verstärker). */
        JUKEBOX,
        /** Notenblock (Noten-Verstärker). */
        NOTE_BLOCK
    }

    /** Vanillas Lautstaerke eines Plattenspieler-Stuecks ({@code SimpleSoundInstance#forJukeboxSong}). */
    public static final float JUKEBOX_VOLUME = 4.0F;
    /** Vanillas Lautstaerke eines Notenblock-Tons ({@code NoteBlock#triggerEvent}). */
    public static final float NOTE_BLOCK_VOLUME = 3.0F;
    /** Bloecke Hoerweite je Lautstaerke-Einheit (Vanilla-Standard der Klang-Definition). */
    public static final int BLOCKS_PER_VOLUME = 16;
    /** Bis hierhin schickt Vanilla Start und Stopp eines Plattenspielers ({@code ServerLevel#levelEvent}). */
    public static final double VANILLA_EVENT_RANGE = 64.0;
    /** Eine zwischengespeicherte Kette gilt hoechstens so viele Ticks. */
    public static final long CACHE_TICKS = 100;
    /** Hoechstens so viele Ketten im Speicher (danach wird er geleert). */
    public static final int CACHE_SIZE = 256;

    private static final AtomicLong GENERATION = new AtomicLong();
    private static final Map<ChainKey, CachedChain> CACHE = new LinkedHashMap<>();

    private record ChainKey(ResourceKey<Level> dimension, BlockPos pos, Source source) {
    }

    private record CachedChain(long generation, long gameTime, List<BlockPos> chain) {
    }

    private SpeakerBoost() {
    }

    // =====================================================================================
    // Verstaerkung an der Quelle
    // =====================================================================================

    /** Direkt angrenzende Lautsprecher der Art {@code source} (0 bis 6, ohne Obergrenze). */
    public static int adjacentSpeakers(BlockGetter level, BlockPos pos, Source source) {
        int count = 0;
        for (Direction direction : Direction.values()) {
            if (isSpeaker(level, pos.relative(direction), source)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isSpeaker(BlockGetter level, BlockPos pos, Source source) {
        return level.getBlockState(pos).getBlock() instanceof SpeakerBlock speaker && speaker.source() == source;
    }

    /** Faktor fuer {@code speakers} angrenzende Lautsprecher: gedeckelt durch die Server-Config. */
    public static float multiplier(int speakers) {
        if (!com.simplebuilding.version.McVersion.MUSIC_DISCS) {
            return 1.0F;
        }
        return 1.0F + Math.min(Math.max(speakers, 0), ServerTuning.maxSpeakers()) * ServerTuning.speakerBoost();
    }

    /** Faktor fuer die Quelle bei {@code pos}: 1 ohne passende Lautsprecher. */
    public static float multiplier(BlockGetter level, BlockPos pos, Source source) {
        if (!com.simplebuilding.version.McVersion.MUSIC_DISCS) {
            return 1.0F;
        }
        return multiplier(adjacentSpeakers(level, pos, source));
    }

    /** Hoechster Faktor, den die Server-Config gerade zulaesst. */
    public static float maxMultiplier() {
        return multiplier(ServerTuning.maxSpeakers());
    }

    /** Hoerweite in Bloecken fuer eine Lautstaerke (Vanilla: hoechstens eine Einheit zaehlt nach unten). */
    public static double reach(float volume) {
        return Math.max(volume, 1.0F) * BLOCKS_PER_VOLUME;
    }

    /** Lautstaerke des Notenblocks bei {@code pos} (Vanilla 3,0). */
    public static float noteBlockVolume(BlockGetter level, BlockPos pos) {
        return NOTE_BLOCK_VOLUME * multiplier(level, pos, Source.NOTE_BLOCK);
    }

    /** Bis hierhin muessen Start und Stopp eines Plattenspielers mit diesem Faktor reichen (ab der Quelle). */
    public static double jukeboxEventRange(float multiplier) {
        return Math.max(VANILLA_EVENT_RANGE, reach(JUKEBOX_VOLUME * multiplier));
    }

    /**
     * Bis hierhin muss ein Plattenspieler-Stopp reichen: die groesste Hoerweite plus die laengste Kette (eine Kette aus
     * Nachbarn reicht hoechstens so viele Bloecke weit wie sie Lautsprecher hat). Auch ein inzwischen abgebauter
     * Lautsprecher laesst so kein Stueck weiterlaufen.
     */
    public static double jukeboxStopRange() {
        return jukeboxEventRange(maxMultiplier()) + (com.simplebuilding.version.McVersion.MUSIC_DISCS ? ServerTuning.maxSpeakerChain() : 0);
    }

    // =====================================================================================
    // Kette
    // =====================================================================================

    /**
     * Die Kette der Quelle bei {@code source}: alle Lautsprecher der Art {@code kind}, die ueber direkte Nachbarschaft
     * mit der Quelle verbunden sind (Breitensuche, naechste zuerst), hoechstens {@code server.speakers.maxChain}, nur in
     * geladenen Chunks. Leer ohne das Feature oder mit {@code maxChain} 0.
     */
    public static List<BlockPos> chain(LevelReader level, BlockPos source, Source kind) {
        int max = com.simplebuilding.version.McVersion.MUSIC_DISCS ? ServerTuning.maxSpeakerChain() : 0;
        List<BlockPos> out = new ArrayList<>();
        if (max <= 0) {
            return out;
        }
        Set<BlockPos> seen = new HashSet<>();
        seen.add(source.immutable());
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(source.immutable());
        while (!queue.isEmpty() && out.size() < max) {
            BlockPos at = queue.poll();
            for (Direction direction : Direction.values()) {
                BlockPos next = at.relative(direction);
                if (out.size() >= max || !seen.add(next) || !level.hasChunkAt(next) || !isSpeaker(level, next, kind)) {
                    continue;
                }
                out.add(next);
                queue.add(next);
            }
        }
        return out;
    }

    /** Wie {@link #chain}, auf dem Server zwischengespeichert (ungueltig bei Lautsprecher-Aenderung, spaetestens nach 100 Ticks). */
    public static List<BlockPos> cachedChain(ServerLevel level, BlockPos source, Source kind) {
        ChainKey key = new ChainKey(level.dimension(), source.immutable(), kind);
        long generation = GENERATION.get();
        long now = level.getGameTime();
        synchronized (CACHE) {
            CachedChain cached = CACHE.get(key);
            if (cached != null && cached.generation() == generation && now - cached.gameTime() <= CACHE_TICKS && now >= cached.gameTime()) {
                return cached.chain();
            }
        }
        List<BlockPos> chain = List.copyOf(chain(level, source, kind));
        synchronized (CACHE) {
            if (CACHE.size() >= CACHE_SIZE) {
                CACHE.clear();
            }
            CACHE.put(key, new CachedChain(generation, now, chain));
        }
        return chain;
    }

    /** Ein Lautsprecher wurde gesetzt oder abgebaut: alle zwischengespeicherten Ketten sind ungueltig. */
    public static void invalidateChains() {
        GENERATION.incrementAndGet();
    }

    /** Die Abspielpunkte: die Quelle zuerst, dann ihre Kette (Blockmitten). */
    public static List<Vec3> points(BlockPos source, List<BlockPos> chain) {
        List<Vec3> out = new ArrayList<>(chain.size() + 1);
        out.add(Vec3.atCenterOf(source));
        for (BlockPos pos : chain) {
            out.add(Vec3.atCenterOf(pos));
        }
        return out;
    }

    /** Der Abspielpunkt, der {@code listener} am naechsten ist (bei Gleichstand der fruehere, also die Quelle). */
    public static Vec3 nearest(List<Vec3> points, Vec3 listener) {
        Vec3 best = points.get(0);
        double bestDistance = best.distanceToSqr(listener);
        for (int i = 1; i < points.size(); i++) {
            double distance = points.get(i).distanceToSqr(listener);
            if (distance < bestDistance) {
                best = points.get(i);
                bestDistance = distance;
            }
        }
        return best;
    }

    /**
     * Notenblock mit Kette (Server): statt Vanillas Rundsendung bekommt jeder Spieler in Hoerweite genau ein Klang-Paket,
     * nach Reichweitenpruefung am naechsten Abspielpunkt, ohne Entfernungsdaempfung beim Empfaenger. Liefert Spieler -&gt; Punkt.
     */
    public static Map<ServerPlayer, Vec3> playChained(ServerLevel level, @Nullable Entity except, BlockPos source, List<BlockPos> chain,
                                                     Holder<SoundEvent> sound, SoundSource category, float volume, float pitch, long seed) {
        List<Vec3> points = points(source, chain);
        double range = sound.value().getRange(volume);
        Map<ServerPlayer, Vec3> heard = new LinkedHashMap<>();
        for (ServerPlayer player : level.players()) {
            if (player == except) {
                continue;
            }
            Vec3 at = nearest(points, player.position());
            if (amplifiedGain(at, player.position(), range) > 0.0F) {
                var note = new ClientboundSoundPacket(sound, category, at.x, at.y, at.z, volume, pitch, seed);
                if (com.simplebuilding.platform.PlatformServices.canSendToPlayer(player,
                        com.simplebuilding.networking.AmplifiedNotePayload.ID)) {
                    com.simplebuilding.platform.PlatformServices.sendToPlayer(player,
                            new com.simplebuilding.networking.AmplifiedNotePayload(note));
                } else {
                    player.connection.send(note);
                }
                heard.put(player, at);
            }
        }
        return heard;
    }

    /** Full gain inside the existing radius, silence outside; no distance or chain falloff. */
    public static float amplifiedGain(Vec3 playback, Vec3 listener, double range) {
        return playback.distanceToSqr(listener) < range * range ? 1.0F : 0.0F;
    }

    /**
     * Schickt {@code packet} an jeden Spieler dieser Dimension, der weiter als Vanillas 64 Bloecke von der Quelle weg ist
     * (die Naeheren hat Vanilla schon bedient), aber naeher als {@code range} an irgendeinem Abspielpunkt (Quelle oder
     * Kette). Liefert die Empfaenger; jeder Spieler bekommt hoechstens ein Paket.
     */
    public static List<ServerPlayer> sendBeyondVanilla(ServerLevel level, BlockPos pos, List<BlockPos> chain, Packet<?> packet, double range) {
        List<ServerPlayer> sent = new ArrayList<>();
        List<Vec3> points = points(pos, chain);
        double reachFromSource = range + chain.size();
        if (reachFromSource <= VANILLA_EVENT_RANGE) {
            return sent;
        }
        Vec3 source = points.get(0);
        double near = VANILLA_EVENT_RANGE * VANILLA_EVENT_RANGE;
        double far = range * range;
        for (ServerPlayer player : level.players()) {
            Vec3 at = player.position();
            if (source.distanceToSqr(at) >= near && nearest(points, at).distanceToSqr(at) < far) {
                player.connection.send(packet);
                sent.add(player);
            }
        }
        return sent;
    }

    /** Wie oben ohne Kette (nur die Quelle). */
    public static int sendBeyondVanilla(ServerLevel level, BlockPos pos, Packet<?> packet, double range) {
        return sendBeyondVanilla(level, pos, List.of(), packet, range).size();
    }
}

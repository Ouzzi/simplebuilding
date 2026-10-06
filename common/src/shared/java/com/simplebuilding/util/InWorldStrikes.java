package com.simplebuilding.util;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Gemeinsame Regeln fuer In-World-Umwandlungen mit mehreren Schlaegen (Besitzer 2026-10-06, Plan
 * {@code docs/ai/PLAN-HAMMER12-2026-10-06.md}, Tabelle {@code docs/ai/INWORLD-UMWANDLUNGEN-2026-10-06.md}).
 *
 * <ul>
 *   <li><b>Ausloeser:</b> immer Rechtsklick (use). Ein gehaltener Rechtsklick wiederholt alle 4 Ticks; ein Schlag
 *       zaehlt erst {@link #MIN_INTERVAL} Ticks nach dem vorigen.</li>
 *   <li><b>Zaehlung:</b> je Dimension und Blockposition (jeder Spieler setzt fort), verfaellt nach
 *       {@link #RESET_TICKS} ohne Schlag oder wenn der Block wechselt.</li>
 *   <li><b>Rueckmeldung ({@link #feedback}):</b> Risse wie beim Abbauen, Partikel des Blocks und sein Schlagklang
 *       mit steigender Tonhoehe. Der letzte Schlag nimmt die Risse weg; seinen Abschluss-Klang spielt die Umwandlung.</li>
 * </ul>
 * Nur Server.
 */
public final class InWorldStrikes {
    /** So viele Ticks muessen zwischen zwei gezaehlten Schlaegen liegen (gehaltener Rechtsklick: alle 4 Ticks). */
    public static final int MIN_INTERVAL = 8;
    /** Ohne weiteren Schlag verfaellt die Zaehlung nach so vielen Ticks. */
    public static final int RESET_TICKS = 100;

    private record Key(ResourceKey<Level> dimension, BlockPos pos) {
    }

    private record Count(String kind, BlockState state, int done, long last) {
    }

    private static final Map<Key, Count> COUNTS = new HashMap<>();

    private InWorldStrikes() {
    }

    /**
     * Ein Schlag der Umwandlung {@code kind} auf {@code state} bei {@code pos} zur Zeit {@code now}. Liefert die Zahl
     * der bisher gezaehlten Schlaege einschliesslich dieses (1..total), oder 0, wenn er zu kurz nach dem vorigen kam.
     * Beim {@code total}-ten Schlag ist die Zaehlung wieder frei.
     */
    public static int count(ServerLevel level, BlockPos pos, String kind, BlockState state, int total, long now) {
        Key key = new Key(level.dimension(), pos.immutable());
        synchronized (COUNTS) {
            Count last = COUNTS.get(key);
            boolean same = last != null && last.kind().equals(kind) && last.state().equals(state)
                    && now >= last.last() && now - last.last() <= RESET_TICKS;
            if (same && now - last.last() < MIN_INTERVAL) {
                return 0;
            }
            int done = same ? last.done() + 1 : 1;
            if (done >= total) {
                COUNTS.remove(key);
            } else {
                COUNTS.put(key, new Count(kind, state, done, now));
            }
            return Math.min(done, total);
        }
    }

    /** Die gezaehlten Schlaege an {@code pos} (0, wenn keine laufen). */
    public static int done(ServerLevel level, BlockPos pos) {
        synchronized (COUNTS) {
            Count count = COUNTS.get(new Key(level.dimension(), pos.immutable()));
            return count == null ? 0 : count.done();
        }
    }

    /** Vergisst die Zaehlung an {@code pos} und nimmt die Risse weg. */
    public static void clear(ServerLevel level, BlockPos pos) {
        synchronized (COUNTS) {
            COUNTS.remove(new Key(level.dimension(), pos.immutable()));
        }
        level.destroyBlockProgress(crackId(pos), pos, -1);
    }

    /**
     * Die gemeinsame Rueckmeldung zu Schlag {@code done} von {@code total} auf {@code shown}: Risse (Stufe waechst
     * gleichmaessig bis 9; beim letzten Schlag weg), Block-Partikel (mehr je Schlag) und der Schlagklang des Blocks mit
     * steigender Tonhoehe.
     */
    public static void feedback(ServerLevel level, BlockPos pos, BlockState shown, int done, int total) {
        crack(level, pos, done, total);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, shown), pos.getX() + 0.5, pos.getY() + 0.5,
                pos.getZ() + 0.5, 4 + 2 * Math.min(done, 8), 0.3, 0.3, 0.3, 0.05);
        var sound = shown.getSoundType();
        level.playSound(null, pos, sound.getHitSound(), SoundSource.BLOCKS, 0.9F,
                0.8F + 0.5F * Math.min(done, total) / Math.max(1, total));
    }

    /** Nur die Risse zu Schlag {@code done} von {@code total} (beim letzten Schlag weg). */
    public static void crack(ServerLevel level, BlockPos pos, int done, int total) {
        level.destroyBlockProgress(crackId(pos), pos, done >= total ? -1 : crackStage(done, total));
    }

    /** Riss-Stufe 0..9 nach {@code done} von {@code total} Schlaegen, -1 fuer keine. */
    public static int crackStage(int done, int total) {
        if (done <= 0 || total <= 0) {
            return -1;
        }
        return Math.max(0, Math.min(9, done * 10 / total - 1));
    }

    /** Negative Riss-Kennung je Position (kollidiert nie mit Spieler-Ids; eigener Bereich gegenueber SimpleLib). */
    public static int crackId(BlockPos pos) {
        return -0x4000_0000 - (int) (pos.asLong() & 0x0FFF_FFFF);
    }

    /** Jede Sekunde (aus {@link SledgehammerProgress#tick}): verfallene Zaehlungen wegraeumen, ihre Risse weg. */
    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) {
            return;
        }
        Map<Key, Count> expired = new HashMap<>();
        synchronized (COUNTS) {
            COUNTS.entrySet().removeIf(entry -> {
                ServerLevel level = server.getLevel(entry.getKey().dimension());
                boolean gone = level == null || level.getGameTime() - entry.getValue().last() > RESET_TICKS
                        || level.getGameTime() < entry.getValue().last()
                        || (level.isLoaded(entry.getKey().pos()) && !level.getBlockState(entry.getKey().pos()).equals(entry.getValue().state()));
                if (gone) {
                    expired.put(entry.getKey(), entry.getValue());
                }
                return gone;
            });
        }
        expired.forEach((key, count) -> {
            ServerLevel level = server.getLevel(key.dimension());
            if (level != null) {
                level.destroyBlockProgress(crackId(key.pos()), key.pos(), -1);
            }
        });
    }
}

package com.simplebuilding.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
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
 *   <li><b>Schrittweise Ergebnisse ({@link #strikeYield}):</b> jeder Schlag zeigt seinen Teil des Ergebnisses sofort als
 *       schwebendes Item ueber dem Block (Vanilla-{@code ItemDisplay}: nicht aufsammelbar, kein Trichter, kein
 *       Zusammenfuehren). Der letzte Schlag ({@link #release}) macht alle Teile an ihren Stellen zu echten Items. Bricht
 *       die Umwandlung ab (Zeit, Block weg), verschwinden die Anzeigen - nichts doppelt, nichts verloren. Anzeigen ohne
 *       laufende Umwandlung (z. B. nach einem Neustart) raeumt {@link #tick} weg.</li>
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

    /** Markierung der schwebenden Teil-Anzeigen. */
    public static final String PENDING_TAG = "simplebuilding.strike_part";
    /** Groesse der Teil-Anzeige (wie ein liegendes Item). */
    private static final float PART_SCALE = 0.4F;

    private static final class Pending {
        final String kind;
        final List<UUID> displays = new ArrayList<>();
        final List<Vec3> spots = new ArrayList<>();
        final List<ItemStack> parts = new ArrayList<>();
        int shown;

        Pending(String kind) {
            this.kind = kind;
        }
    }

    private static final Map<Key, Pending> PENDING = new HashMap<>();

    /** Ergebnis eines gezaehlten Schlags mit Teil-Ergebnis: Schlag {@code done} von {@code total}; am Ende {@code finished}. */
    public record Strike(int done, int total, boolean finished) {
    }

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

    /**
     * Ein Schlag mit schrittweisem Ergebnis (Besitzer 2026-10-06): zaehlt wie {@link #count}, gibt die gemeinsame
     * Rueckmeldung und zeigt den Teil dieses Schlags ({@link #part}) als schwebendes Item. {@code null}, wenn der Schlag
     * zu frueh kam. Ist {@link Strike#finished()}, wandelt der Aufrufer den Block um und ruft {@link #release}.
     */
    public static @Nullable Strike strikeYield(ServerLevel level, BlockPos pos, String kind, BlockState state, ItemStack result,
                                               int total, long now) {
        int done = count(level, pos, kind, state, total, now);
        if (done == 0) {
            return null;
        }
        Key key = new Key(level.dimension(), pos.immutable());
        if (done == 1) {
            discard(level, key);
        }
        feedback(level, pos, state, done, total);
        if (done >= total) {
            return new Strike(done, total, true);
        }
        ItemStack part = part(result, total, done);
        if (!part.isEmpty()) {
            Pending pending;
            synchronized (PENDING) {
                pending = PENDING.computeIfAbsent(key, k -> new Pending(kind));
            }
            Vec3 spot = spot(pos, pending.parts.size());
            Display.ItemDisplay display = new Display.ItemDisplay(net.minecraft.world.entity.EntityTypes.ITEM_DISPLAY, level);
            ((com.simplebuilding.mixin.ItemDisplayStrikeAccessor) display).simplebuilding$setItemStack(part.copyWithCount(1));
            ((com.simplebuilding.mixin.DisplayStrikeAccessor) display).simplebuilding$setTransformation(new com.mojang.math.Transformation(
                    new org.joml.Vector3f(), new org.joml.Quaternionf(), new org.joml.Vector3f(PART_SCALE), new org.joml.Quaternionf()));
            display.setPos(spot.x, spot.y, spot.z);
            display.setYRot(pending.parts.size() * 137.5F);
            display.addTag(PENDING_TAG);
            if (level.addFreshEntity(display)) {
                pending.displays.add(display.getUUID());
            }
            pending.spots.add(spot);
            pending.parts.add(part.copy());
            pending.shown += part.getCount();
        }
        return new Strike(done, total, false);
    }

    /**
     * Der letzte Schlag: die Anzeigen weg, an ihren Stellen die echten Items, dazu der Rest von {@code result} (was die
     * Anzeigen noch nicht zeigten) in der Blockmitte. Ohne laufende Umwandlung kommt das ganze Ergebnis heraus.
     */
    public static void release(ServerLevel level, BlockPos pos, ItemStack result) {
        Key key = new Key(level.dimension(), pos.immutable());
        Pending pending;
        synchronized (PENDING) {
            pending = PENDING.remove(key);
        }
        int shown = 0;
        if (pending != null) {
            removeDisplays(level, pending);
            for (int i = 0; i < pending.parts.size(); i++) {
                ItemStack part = pending.parts.get(i);
                int count = Math.min(part.getCount(), result.getCount() - shown);
                if (count > 0 && part.is(result.getItem())) {
                    drop(level, pending.spots.get(i), result.copyWithCount(count));
                    shown += count;
                }
            }
        }
        int rest = result.getCount() - shown;
        if (rest > 0) {
            drop(level, Vec3.atCenterOf(pos), result.copyWithCount(rest));
        }
        level.destroyBlockProgress(crackId(pos), pos, -1);
    }

    /** Der Teil von {@code result}, den Schlag {@code done} von {@code total} bringt: gleich verteilt, der Rest zuletzt. */
    public static ItemStack part(ItemStack result, int total, int done) {
        if (result.isEmpty() || total <= 0 || done <= 0 || done > total) {
            return ItemStack.EMPTY;
        }
        int base = result.getCount() / total;
        int count = done < total ? base : result.getCount() - base * (total - 1);
        return count <= 0 ? ItemStack.EMPTY : result.copyWithCount(count);
    }

    /** So viele Items zeigen die schwebenden Anzeigen an {@code pos} gerade (Tests). */
    public static int shown(ServerLevel level, BlockPos pos) {
        synchronized (PENDING) {
            Pending pending = PENDING.get(new Key(level.dimension(), pos.immutable()));
            return pending == null ? 0 : pending.shown;
        }
    }

    private static Vec3 spot(BlockPos pos, int index) {
        double angle = Math.toRadians(index * 137.5);
        double radius = 0.18 + 0.05 * (index % 4);
        return new Vec3(pos.getX() + 0.5 + Math.cos(angle) * radius, pos.getY() + 1.12 + 0.03 * (index % 3),
                pos.getZ() + 0.5 + Math.sin(angle) * radius);
    }

    private static void drop(ServerLevel level, Vec3 at, ItemStack stack) {
        while (!stack.isEmpty()) {
            ItemStack batch = stack.split(stack.getMaxStackSize());
            ItemEntity entity = new ItemEntity(level, at.x, at.y, at.z, batch, 0.0, 0.1, 0.0);
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
        }
    }

    private static void discard(ServerLevel level, Key key) {
        Pending pending;
        synchronized (PENDING) {
            pending = PENDING.remove(key);
        }
        if (pending != null) {
            removeDisplays(level, pending);
        }
    }

    private static void removeDisplays(ServerLevel level, Pending pending) {
        for (UUID id : pending.displays) {
            Entity entity = level.getEntity(id);
            if (entity != null) {
                entity.discard();
            }
        }
    }

    /** Tests: der naechste Schlag an {@code pos} zaehlt sofort (als laege der vorige {@link #MIN_INTERVAL} Ticks zurueck). */
    public static void allowNextStrike(ServerLevel level, BlockPos pos) {
        Key key = new Key(level.dimension(), pos.immutable());
        synchronized (COUNTS) {
            Count count = COUNTS.get(key);
            if (count != null) {
                COUNTS.put(key, new Count(count.kind(), count.state(), count.done(), count.last() - MIN_INTERVAL));
            }
        }
    }

    /** Die gezaehlten Schlaege an {@code pos} (0, wenn keine laufen). */
    public static int done(ServerLevel level, BlockPos pos) {
        synchronized (COUNTS) {
            Count count = COUNTS.get(new Key(level.dimension(), pos.immutable()));
            return count == null ? 0 : count.done();
        }
    }

    /** Vergisst die Zaehlung an {@code pos}, nimmt schwebende Teile und die Risse weg. */
    public static void clear(ServerLevel level, BlockPos pos) {
        synchronized (COUNTS) {
            COUNTS.remove(new Key(level.dimension(), pos.immutable()));
        }
        discard(level, new Key(level.dimension(), pos.immutable()));
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
                discard(level, key);
            }
        });
        if (server.getTickCount() % RESET_TICKS == 0) {
            removeOrphans(server);
        }
    }

    /** Teil-Anzeigen ohne laufende Umwandlung (Neustart, Absturz, entladene Welt) verschwinden. */
    private static void removeOrphans(MinecraftServer server) {
        Set<UUID> live = new HashSet<>();
        synchronized (PENDING) {
            PENDING.values().forEach(pending -> live.addAll(pending.displays));
        }
        for (ServerLevel level : server.getAllLevels()) {
            for (Display.ItemDisplay display : level.getEntities(net.minecraft.world.entity.EntityTypes.ITEM_DISPLAY,
                    entity -> entity.entityTags().contains(PENDING_TAG))) {
                if (!live.contains(display.getUUID())) {
                    display.discard();
                }
            }
        }
    }
}

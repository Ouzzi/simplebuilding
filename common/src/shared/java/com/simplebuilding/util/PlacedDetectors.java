package com.simplebuilding.util;

import com.simplebuilding.blocks.entity.custom.PlacedTemplateBlockEntity;
import com.simplebuilding.config.ServerTuning;
import com.simplebuilding.items.custom.OreDetectorItem;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Der abgelegte Detector (Besitzer 2026-09-29): ein kalibrierter Detector (Modus "Kalibriert" mit
 * Zielblock, {@link OreDetectorItem#isArmed}) legt sich mit Schleichen + Rechtsklick wie eine
 * Schmiedevorlage ab ({@link PlacedTemplates}, derselbe Block, pixelgenaue Trefferform, bricht mit
 * allen Daten als er selbst ab). Dort arbeitet er weiter - ohne Einblendung, nur mit Klang und
 * Partikeln:
 * <ul>
 *   <li><b>Suche:</b> alle {@link #interval()} Ticks dieselbe Suche wie in der Hand
 *   ({@link OreDetectorItem#findTarget}, samt Radius/Constructor's Touch des Stapels), aber von der
 *   Mitte der Platte aus. Findet er den Zielblock, laeuft eine Sculk-Vibration vom Fundort zum
 *   Detector, Blockpartikel zeichnen den Strahl, dazu Glocke und Sculk-Klicken wie in der Hand
 *   (hoeher je naeher). Kostet keine Haltbarkeit.</li>
 *   <li><b>Traeger:</b> haelt ein Spieler oder Mob in {@link #HOLDER_RANGE} Bloecken den Zielblock als
 *   Item in einer Hand, schlaegt der Detector ebenfalls an - eine
 *   Vibration fliegt vom Detector zum Traeger, dazu ein hoher Resonanzton.</li>
 * </ul>
 *
 * <p>Sparsam: ein Takt je Platte (versetzt nach Position), und nur, wenn ein Spieler in
 * {@link #LISTEN_RANGE} Bloecken ist - sonst hoert und sieht es ohnehin niemand. Die Kugelsuche
 * teilt sich die Tick-Kappe der gehaltenen Detectors ({@link OreDetectorItem#takeScanBudget}); die
 * Traegersuche ist eine Entity-Abfrage im Wuerfel von {@link #HOLDER_RANGE}. Der Serverschalter
 * {@code server.features.oreDetector} gilt auch abgelegt.
 */
public final class PlacedDetectors {
    /** So weit (Bloecke) bemerkt ein abgelegter Detector einen Traeger des Zielblocks. */
    public static final double HOLDER_RANGE = 8.0;
    /** Ohne Spieler in dieser Entfernung (Bloecke) ruht der Detector ganz. */
    public static final double LISTEN_RANGE = 32.0;
    /** Abgelegt sucht er halb so oft wie in der Haupthand. */
    public static final int INTERVAL_FACTOR = 2;

    private PlacedDetectors() {
    }

    /** Ticks zwischen zwei Suchen: doppelter Haupthand-Takt ({@code server.oreDetector.scanIntervalTicks}). */
    public static int interval() {
        return ServerTuning.oreDetectorInterval() * INTERVAL_FACTOR;
    }

    /** Was ein Takt gefunden hat (fuer Tests); beides null = nichts. */
    public record Result(@Nullable BlockPos found, @Nullable LivingEntity holder) {
        public static final Result NONE = new Result(null, null);
    }

    /** Ein Server-Tick der abgelegten Platte: ist sie ein scharfer Detector und ist dieser Tick dran, sucht sie. */
    public static Result tick(ServerLevel level, BlockPos pos, PlacedTemplateBlockEntity be) {
        ItemStack stack = be.getTemplate();
        // Billigste Pruefungen zuerst: Item-Art und Takt; die Komponenten liest erst isArmed.
        if (!(stack.getItem() instanceof OreDetectorItem)
                || Math.floorMod(level.getGameTime() + pos.asLong(), interval()) != 0
                || !ServerTuning.get().features.oreDetector || !OreDetectorItem.isArmed(stack)
                || !level.hasNearbyAlivePlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, LISTEN_RANGE)) {
            return Result.NONE;
        }
        return scan(level, pos, be, true);
    }

    /**
     * Sucht jetzt (ohne Takt- und Spielerpruefung) nach Traegern und dem Zielblock und gibt die
     * Rueckmeldung. {@code useBudget}: die Kugelsuche zaehlt gegen die Tick-Kappe (Tests: false).
     */
    public static Result scan(ServerLevel level, BlockPos pos, PlacedTemplateBlockEntity be, boolean useBudget) {
        ItemStack stack = be.getTemplate();
        if (!(stack.getItem() instanceof OreDetectorItem detector)) {
            return Result.NONE;
        }
        BlockState target = OreDetectorItem.calibratedTarget(stack, level.registryAccess());
        if (target == null) {
            return Result.NONE;
        }
        Vec3 origin = be.surfaceCentre();

        LivingEntity holder = nearestHolder(level, pos, target);
        if (holder != null) {
            double distance = holder.getEyePosition().distanceTo(origin);
            level.sendParticles(new VibrationParticleOption(new EntityPositionSource(holder, holder.getEyeHeight() * 0.7F),
                    Math.max(4, (int) Math.round(distance * 1.5))), origin.x, origin.y, origin.z, 1, 0.0, 0.0, 0.0, 0.0);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 1.6F);
            level.playSound(null, pos, SoundEvents.SCULK_CLICKING, SoundSource.BLOCKS, 0.5F, 1.8F);
        }

        BlockPos found = null;
        if (!useBudget || OreDetectorItem.takeScanBudget(level.getGameTime())) {
            found = detector.findTarget(level, stack, origin);
        }
        if (found != null) {
            BlockState state = level.getBlockState(found);
            Vec3 end = Vec3.atCenterOf(found);
            double distance = origin.distanceTo(end);
            // Die Vibration laeuft vom Fundort zum Detector: so zeigt sie, woher das Signal kommt.
            level.sendParticles(new VibrationParticleOption(new BlockPositionSource(pos), Math.max(4, (int) Math.round(distance * 1.5))),
                    end.x, end.y, end.z, 1, 0.0, 0.0, 0.0, 0.0);
            int count = OreDetectorItem.beamParticles(distance, null);
            Vec3 step = end.subtract(origin).scale(1.0 / (count + 1));
            for (int i = 1; i <= count; i++) {
                Vec3 p = origin.add(step.scale(i));
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
            level.sendParticles(ParticleTypes.END_ROD, end.x, end.y, end.z, 1, 0.1, 0.1, 0.1, 0.01);
            float pitch = (float) Math.max(0.6, Math.min(2.0, 1.8 - distance / 32.0));
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.9F, pitch);
            level.playSound(null, pos, SoundEvents.SCULK_CLICKING, SoundSource.BLOCKS, 0.6F, 2.0F);
        }
        return new Result(found, holder);
    }

    /** Der naechste Spieler oder Mob in {@link #HOLDER_RANGE}, der den Zielblock in der Hand haelt; sonst null. */
    public static @Nullable LivingEntity nearestHolder(ServerLevel level, BlockPos pos, BlockState target) {
        Item item = target.getBlock().asItem();
        Vec3 centre = Vec3.atCenterOf(pos);
        double rangeSq = HOLDER_RANGE * HOLDER_RANGE;
        List<LivingEntity> holders = level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(HOLDER_RANGE),
                entity -> entity.isAlive() && !entity.isSpectator() && entity.distanceToSqr(centre) <= rangeSq
                        && holds(entity, target, item));
        LivingEntity best = null;
        double bestSq = Double.MAX_VALUE;
        for (LivingEntity entity : holders) {
            double d = entity.distanceToSqr(centre);
            if (d < bestSq) {
                best = entity;
                bestSq = d;
            }
        }
        return best;
    }

    /** Haelt {@code entity} den Zielblock als Item in einer der beiden Haende? */
    public static boolean holds(LivingEntity entity, BlockState target, Item item) {
        return item != Items.AIR && (entity.getMainHandItem().is(item) || entity.getOffhandItem().is(item));
    }
}

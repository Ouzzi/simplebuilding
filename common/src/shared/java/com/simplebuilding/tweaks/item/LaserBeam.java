package com.simplebuilding.tweaks.item;

import com.simplebuilding.tweaks.mixin.FireBlockInvoker;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.SoulFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Was der Strahl der Amethystlinse auf dem Server an Bloecken bewirkt, solange er auf demselben
 * Block (und derselben Seite) ruht. Jede Wirkung braucht eine Verweildauer; waehrenddessen steigt
 * Rauch auf, am Ende gibt es Partikel und ein Geraeusch und die Ladung sinkt um
 * {@link LaserPointerItem#EFFECT_COST}.
 *
 * <ul>
 *   <li>Eis schmilzt zu Wasser (im Nether verdampft es, wie in Vanilla); Packeis wird zu Eis und
 *       Blaueis zu Packeis - eine Stufe je Durchgang statt Wasser aus einem Block, der in Vanilla
 *       nie schmilzt.</li>
 *   <li>Schneeschichten, Schneebloecke und Pulverschnee schmelzen weg.</li>
 *   <li>Brennbare Bloecke (Zuendwert des Feuers &gt; 0: Wolle, Bretter, Laub, Staemme ...) fangen
 *       nach laengerem Strahlen Feuer auf der angestrahlten Seite - nur, wo Feuer sich nach der
 *       Spielregel {@code fire_spread_radius_around_player} ausbreiten darf.</li>
 *   <li>Seelensand/-erde bekommt oben Seelenfeuer; Lagerfeuer, Seelenlagerfeuer, Kerzen und
 *       Kerzenkuchen werden angezuendet.</li>
 *   <li>Nasse Schwaemme trocknen (die "coole" Zusatzwirkung, wie ein Schwamm im Nether).</li>
 *   <li>Nie: Netherportale (anders als Feuerzeug - Feuer in einem leeren Portalrahmen wird nicht
 *       gesetzt) und TNT (kein Fernzuender auf {@link LaserPointerItem#EFFECT_RANGE} Bloecke).</li>
 * </ul>
 *
 * <p>Schutz: der Spieler muss den Block beruehren duerfen ({@code mayInteract}: Spawnschutz,
 * Weltgrenze) und dort bauen ({@code mayUseItemAt}: Abenteuermodus), fuer Feuer auch am Feuerplatz.
 */
public final class LaserBeam {

    public static final int MELT_TICKS = 40;
    public static final int IGNITE_TICKS = 60;
    public static final int SOUL_FIRE_TICKS = 40;
    public static final int LIGHT_TICKS = 20;
    public static final int DRY_TICKS = 100;

    public enum Effect {
        MELT(MELT_TICKS), IGNITE(IGNITE_TICKS), SOUL_FIRE(SOUL_FIRE_TICKS), LIGHT(LIGHT_TICKS), DRY(DRY_TICKS);

        public final int ticks;

        Effect(int ticks) {
            this.ticks = ticks;
        }
    }

    private record Dwell(BlockPos pos, Direction face, int ticks, long lastTick) {
    }

    private static final Map<ServerPlayer, Dwell> DWELLS = new WeakHashMap<>();

    private LaserBeam() {
    }

    /** Vergisst die Verweildauer eines Spielers (Strahl losgelassen oder woanders hin). */
    public static void reset(ServerPlayer player) {
        DWELLS.remove(player);
    }

    /**
     * Ein Tick Strahl auf {@code hit}. Zaehlt die Verweildauer hoch und loest die Wirkung aus,
     * sobald sie erreicht ist.
     *
     * @return die ausgeloeste Wirkung, sonst null
     */
    public static @Nullable Effect beamAt(ServerPlayer player, ItemStack stack, BlockHitResult hit) {
        ServerLevel level = player.level();
        BlockPos pos = hit.getBlockPos();
        Direction face = hit.getDirection();
        BlockState state = level.getBlockState(pos);
        Effect effect = LaserPointerItem.isEmpty(stack) ? null : effectFor(level, pos, state, face);
        if (effect == null || !allowed(player, level, pos, face, stack, effect)) {
            DWELLS.remove(player);
            return null;
        }
        long now = level.getGameTime();
        Dwell dwell = DWELLS.get(player);
        int ticks = dwell != null && dwell.pos().equals(pos) && dwell.face() == face && now - dwell.lastTick() <= 2
                ? dwell.ticks() + 1 : 1;
        Vec3 at = hit.getLocation();
        if (ticks < effect.ticks) {
            DWELLS.put(player, new Dwell(pos.immutable(), face, ticks, now));
            if (ticks % 4 == 0) {
                level.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 1, 0.03, 0.03, 0.03, 0.0);
            }
            return null;
        }
        DWELLS.remove(player);
        if (!apply(player, level, pos, state, face, at, effect)) {
            return null;
        }
        LaserPointerItem.drain(player, stack, LaserPointerItem.EFFECT_COST);
        return effect;
    }

    /** Welche Wirkung der Strahl auf diesem Block haette (ohne Schutz-/Regelpruefung). */
    public static @Nullable Effect effectFor(ServerLevel level, BlockPos pos, BlockState state, Direction face) {
        if (state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE)
                || state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW)) {
            return Effect.MELT;
        }
        if (CampfireBlock.canLight(state) || CandleBlock.canLight(state) || CandleCakeBlock.canLight(state)) {
            return Effect.LIGHT;
        }
        if (state.is(Blocks.WET_SPONGE)) {
            return Effect.DRY;
        }
        if (state.is(Blocks.TNT)) {
            return null;
        }
        BlockPos firePos = pos.relative(face);
        if (!level.getBlockState(firePos).isAir()) {
            return null;
        }
        if (face == Direction.UP && SoulFireBlock.canSurviveOnBlock(state)) {
            return Effect.SOUL_FIRE;
        }
        if (isFlammable(state)) {
            return Effect.IGNITE;
        }
        return null;
    }

    /** Brennbar wie fuer das Feuer selbst: Zuendwert aus der Feuer-Tabelle (FireBlock#setFlammable). */
    public static boolean isFlammable(BlockState state) {
        return ((FireBlockInvoker) Blocks.FIRE).simplebuilding$getIgniteOdds(state) > 0;
    }

    private static boolean allowed(ServerPlayer player, ServerLevel level, BlockPos pos, Direction face, ItemStack stack, Effect effect) {
        if (!player.mayInteract(level, pos) || !player.mayUseItemAt(pos, face, stack)) {
            return false;
        }
        if (effect == Effect.IGNITE || effect == Effect.SOUL_FIRE) {
            BlockPos firePos = pos.relative(face);
            if (!player.mayInteract(level, firePos) || !player.mayUseItemAt(firePos, face, stack)) {
                return false;
            }
        }
        // Brennbares faengt nur Feuer, wo sich Feuer ausbreiten darf (Spielregel); Seelenfeuer
        // breitet sich nie aus und zuendet wie ein Feuerzeug auch ohne die Regel.
        return effect != Effect.IGNITE || level.canSpreadFireAround(pos.relative(face));
    }

    private static boolean apply(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state, Direction face, Vec3 at, Effect effect) {
        switch (effect) {
            case MELT -> {
                BlockState melted = meltedState(level, pos, state);
                if (melted.isAir()) {
                    level.removeBlock(pos, false);
                } else {
                    level.setBlockAndUpdate(pos, melted);
                    if (melted.is(Blocks.WATER)) {
                        level.neighborChanged(pos, Blocks.WATER, null);
                    }
                }
                level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
                level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 4, 0.1, 0.1, 0.1, 0.01);
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4f, 1.6f + level.getRandom().nextFloat() * 0.3f);
            }
            case LIGHT -> {
                level.setBlock(pos, state.setValue(BlockStateProperties.LIT, true), Block.UPDATE_ALL_IMMEDIATE);
                level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
                level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 0.8f, level.getRandom().nextFloat() * 0.4f + 0.8f);
            }
            case DRY -> {
                level.setBlock(pos, Blocks.SPONGE.defaultBlockState(), Block.UPDATE_ALL);
                level.levelEvent(2009, pos, 0);
                level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
                level.playSound(null, pos, SoundEvents.WET_SPONGE_DRIES, SoundSource.BLOCKS, 1.0f, (1.0f + level.getRandom().nextFloat() * 0.2f) * 0.7f);
            }
            case IGNITE, SOUL_FIRE -> {
                BlockPos firePos = pos.relative(face);
                // Nie ein Netherportal: BaseFireBlock#onPlace wuerde einen leeren Rahmen sofort fuellen.
                if (PortalShape.findEmptyPortalShape(level, firePos, Direction.Axis.X).isPresent()) {
                    return false;
                }
                BlockState fire = BaseFireBlock.getState(level, firePos);
                if (!level.getBlockState(firePos).isAir() || !fire.canSurvive(level, firePos)
                        || (effect == Effect.SOUL_FIRE) != fire.is(Blocks.SOUL_FIRE)) {
                    return false;
                }
                level.setBlock(firePos, fire, Block.UPDATE_ALL_IMMEDIATE);
                level.gameEvent(player, GameEvent.BLOCK_PLACE, pos);
                level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 3, 0.05, 0.05, 0.05, 0.01);
                level.playSound(null, firePos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 0.8f, level.getRandom().nextFloat() * 0.4f + 0.8f);
            }
        }
        return true;
    }

    /** Eis/Frosteis -> Wasser (verdampft, wo Wasser verdampft), Packeis -> Eis, Blaueis -> Packeis, Schnee -> Luft. */
    public static BlockState meltedState(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.is(Blocks.BLUE_ICE)) {
            return Blocks.PACKED_ICE.defaultBlockState();
        }
        if (state.is(Blocks.PACKED_ICE)) {
            return Blocks.ICE.defaultBlockState();
        }
        if (state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE)) {
            return level.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, pos)
                    ? Blocks.AIR.defaultBlockState() : Blocks.WATER.defaultBlockState();
        }
        return Blocks.AIR.defaultBlockState();
    }
}

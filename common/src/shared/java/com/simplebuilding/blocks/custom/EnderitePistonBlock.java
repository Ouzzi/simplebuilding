package com.simplebuilding.blocks.custom;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.util.ModTags;
import com.simplebuilding.util.PistonBreach;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

/**
 * Enderitkolben: bricht gewoehnliche Bloecke genau wie der Netheritkolben (alles aus
 * {@link NetheriteBreakerPistonBlock} gilt unveraendert), durchbricht aber tiefer.
 *
 * <p>Steht ein durchbrechbarer Block ({@link PistonBreach}) direkt vorn und bezahlt ein
 * Redstoneblock daneben, geht der Durchbruch bis zu {@link #BREACH_DEPTH} Zellen in Blickrichtung:
 * <ul>
 *   <li>Luft wird uebersprungen;</li>
 *   <li>an einem Block im Tag {@code simplebuilding:piston_breach_immune} endet der Durchbruch, auch
 *       wenn der Brecher ihn sonst brechen duerfte;</li>
 *   <li>ein durchbrechbarer Block verschwindet ohne Drop;</li>
 *   <li>ein gewoehnlicher Block, den der Brecher bei der anliegenden Signalstaerke brechen darf
 *       (Haerte 0 bis {@code (Signal / 15) * 50}, Push-Reaktion nicht BLOCK), wird wie vom Brecher
 *       zerstoert, mit seinem normalen Drop;</li>
 *   <li>an allem anderen (ein unzerstoerbarer Block mit Block-Entity, ein zu harter Block,
 *       Push-Reaktion BLOCK, Fluessigkeiten) endet der Durchbruch ebenfalls.</li>
 * </ul>
 * Danach verschwinden der Redstoneblock und der Kolben selbst, wie beim Netheritkolben.
 */
public class EnderitePistonBlock extends NetheriteBreakerPistonBlock {
    public static final MapCodec<EnderitePistonBlock> CODEC = simpleCodec(EnderitePistonBlock::new);

    /** Wie viele Zellen vor der Front der Durchbruch hoechstens reicht. */
    public static final int BREACH_DEPTH = 3;

    public EnderitePistonBlock(Properties settings) {
        super(settings);
    }

    @Override
    @SuppressWarnings("unchecked")
    public MapCodec<PistonBaseBlock> codec() {
        return (MapCodec<PistonBaseBlock>) (Object) CODEC;
    }

    @Override
    protected void breach(Level world, BlockPos pos, Direction facing) {
        // Vor dem Durchbruch gemessen: der bezahlende Redstoneblock liegt noch daneben.
        int power = world.getBestNeighborSignal(pos);
        float breakThreshold = (power / 15.0f) * 50.0f;
        for (int depth = 1; depth <= BREACH_DEPTH; depth++) {
            BlockPos target = pos.relative(facing, depth);
            BlockState targetState = world.getBlockState(target);
            if (targetState.isAir()) {
                continue;
            }
            if (targetState.is(ModTags.Blocks.PISTON_BREACH_IMMUNE)) {
                return;
            }
            if (PistonBreach.isBreachable(targetState, world, target)) {
                world.destroyBlock(target, false);
            } else if (breakerCanBreak(targetState, world, target, breakThreshold)) {
                world.destroyBlock(target, true);
            } else {
                return;
            }
        }
    }

    /** Die Regel des normalen Brechers aus {@link NetheriteBreakerPistonBlock#triggerEvent}. */
    private static boolean breakerCanBreak(BlockState state, Level world, BlockPos pos, float breakThreshold) {
        float hardness = state.getDestroySpeed(world, pos);
        return hardness >= 0 && hardness <= breakThreshold && state.getPistonPushReaction() != PushReaction.BLOCK;
    }
}

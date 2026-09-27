package com.simplebuilding.blocks.custom;

import com.simplebuilding.version.BlockCodecs;

import com.simplebuilding.version.McVersion;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.util.ModTags;
import com.simplebuilding.util.PistonBoreEffects;
import com.simplebuilding.util.PistonBreach;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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
 * Jeder zerstoerte Block zeigt seine Bruchpartikel und spielt seinen Abbauklang und den Bohrklang
 * der Mod ({@link PistonBoreEffects}). Danach verschwinden der Redstoneblock und der Kolben selbst,
 * wie beim Netheritkolben.
 */
public class EnderitePistonBlock extends NetheriteBreakerPistonBlock {
    public static final MapCodec<EnderitePistonBlock> CODEC = BlockCodecs.simple(EnderitePistonBlock::new);

    /** Wie viele Zellen vor der Front der Durchbruch hoechstens reicht. */
    public static final int BREACH_DEPTH = 3;

    public EnderitePistonBlock(Properties settings) {
        super(settings);
    }

    // No @Override: MC 26.3 removed block codecs; this only overrides on 26.2.
    @SuppressWarnings("unchecked")
    public MapCodec<PistonBaseBlock> codec() {
        return (MapCodec<PistonBaseBlock>) (Object) CODEC;
    }

    /**
     * @return ob der vorderste Block zerstoert wurde. Jeder Block geht vorher durch den
     *         Plattform-Wächter ({@link #mayBreak}); lehnt er einen tieferen ab, endet der Durchbruch
     *         dort, lehnt er schon den vordersten ab, passiert gar nichts.
     */
    @Override
    protected boolean breach(ServerLevel world, BlockPos pos, Direction facing) {
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
                return true;
            }
            boolean breachable = PistonBreach.isBreachable(targetState, world, target);
            if (!breachable && !breakerCanBreak(targetState, world, target, breakThreshold)) {
                return true;
            }
            if (!mayBreak(world, pos, facing, target)) {
                return depth > 1;
            }
            PistonBoreEffects.destroy(world, target, !breachable);
        }
        return true;
    }

    /** Die Regel des normalen Brechers aus {@link NetheriteBreakerPistonBlock#triggerEvent}. */
    private static boolean breakerCanBreak(BlockState state, Level world, BlockPos pos, float breakThreshold) {
        float hardness = state.getDestroySpeed(world, pos);
        return hardness >= 0 && hardness <= breakThreshold && McVersion.pushReaction(state) != McVersion.PUSH_BLOCKED;
    }
}

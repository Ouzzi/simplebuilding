package com.simplebuilding.modules.simplelayers.block;

import com.simplebuilding.modules.simplelayers.LayersRegistry;
import com.simplebuilding.modules.simplelayers.SimpleLayers;
import com.simplebuilding.modules.simplelayers.config.LayersConfig;
import com.simplebuilding.modules.simplelayers.world.BackroomEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Backrooms block (wallpaper, damp carpet, ceiling tile, neon light, EXIT sign, exit door). Outside the Backrooms an ordinary
 * decoration block. A survival player with the Backroom effect ({@link BackroomEffect}, handed out inside the Backrooms while
 * {@code backrooms.unbreakableWalls} is on) cannot break it - otherwise one would dig straight through the maze; inside the
 * Backrooms no piston moves it either ({@code PistonBaseBlockMixin}). The exception is Simple Machines' Netherite Piston, which
 * breaks the block in front of it (plan docs/ai/PLAN-B-LAYERS13-2026-11-09.md). BACK18: the same holds in every Backrooms level and
 * for the Vanilla blocks the deeper levels are built of (tag {@code simplelayers:backrooms_structure}, {@code BlockStateBaseMixin}).
 */
public class BackroomBlock extends Block {
    public BackroomBlock(Properties properties) {
        super(properties);
    }

    /** Whether {@code player} may not break Backrooms blocks: survival (or adventure) with the Backroom effect. */
    public static boolean protectedFor(Player player) {
        return !player.isCreative() && BackroomEffect.holder() != null && player.hasEffect(BackroomEffect.holder());
    }

    /** Part of the rooms: a Backrooms block, or a block of the levels' Vanilla palette (tag {@code backrooms_structure}). */
    public static boolean structural(BlockState state) {
        return state.getBlock() instanceof BackroomBlock || state.is(LayersRegistry.BACKROOMS_STRUCTURE);
    }

    /** BACK18: a Vanilla palette block that {@code player} cannot mine here (inside the Backrooms, with the effect). */
    public static boolean heldFor(BlockState state, Player player) {
        return !(state.getBlock() instanceof BackroomBlock) && state.is(LayersRegistry.BACKROOMS_STRUCTURE) && protectedFor(player)
                && SimpleLayers.inBackrooms(player.level());
    }

    /** Whether pistons must leave this block where it is: part of the rooms, inside the Backrooms, protection on. */
    public static boolean heldInPlace(BlockState state, Level level) {
        return LayersConfig.unbreakableWalls && SimpleLayers.inBackrooms(level) && structural(state);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return protectedFor(player) ? 0.0F : super.getDestroyProgress(state, player, level, pos);
    }
}

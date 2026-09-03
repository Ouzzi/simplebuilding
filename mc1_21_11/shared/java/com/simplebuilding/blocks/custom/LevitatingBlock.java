package com.simplebuilding.blocks.custom;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.entity.LevitatingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ein Block, der nach oben fällt – das Gegenstück zu Vanillas {@link FallingBlock}.
 *
 * <p>Aufgebaut wie das Original: sobald der Platz darüber frei ist, verwandelt der
 * Block sich in eine {@link LevitatingBlockEntity} und steigt als Entity auf. Bis
 * 2026-09 wurde er stattdessen alle zwei Ticks eine Position weiter nach oben
 * gesetzt; das war ein Sprung pro Zehntelsekunde und sah nicht aus wie fallender
 * Sand. Die Bewegung selbst, das Landen und das Zerbrechen am Baulimit stecken
 * jetzt in der Entity.
 */
public class LevitatingBlock extends Block {
    public static final MapCodec<LevitatingBlock> CODEC = simpleCodec(LevitatingBlock::new);

    /** Wie Vanilla: zwei Ticks Vorlauf, damit ein frisch gesetzter Block nicht sofort losfliegt. */
    private static final int DELAY_AFTER_PLACE = 2;

    public LevitatingBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        world.scheduleTick(pos, this, DELAY_AFTER_PLACE);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState,
                                     RandomSource random) {
        // Ändert sich etwas in der Nachbarschaft, erneut prüfen, ob der Weg nach oben frei ist.
        tickView.scheduleTick(pos, this, DELAY_AFTER_PLACE);
        return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        // Spiegelbild von FallingBlock.tick: dort isFree(unten) und y >= getMinY().
        if (FallingBlock.isFree(world.getBlockState(pos.above())) && pos.getY() <= world.getMaxY()) {
            LevitatingBlockEntity.rise(world, pos, state);
        }
    }
}

package com.simplebuilding.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import org.jetbrains.annotations.Nullable;

/**
 * Astral and Nihilith Lamp (Nachtrag 44, 2026-10-12): the lamp of a redstone line. Since N51 (2026-10-13) it takes the
 * signal of every line ({@link EndSignalBlock#receiverPower}: redstone, Astral, Nihil and Bridge dust beside it or on
 * top, levers, redstone blocks and the line blocks) and behaves like a copper bulb: every rising edge toggles
 * {@link #LIT}; while lit it gives light 15. {@code POWER} (inherited) keeps the last incoming signal so the edge is
 * seen; old lamps saved before Nachtrag 36 carried the same {@code power} property and load as an unlit lamp.
 */
public class EndSignalLampBlock extends EndSignalBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public EndSignalLampBlock(boolean astral, Properties properties) {
        super(astral, properties);
        registerDefaultState(defaultBlockState().setValue(LIT, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (!level.isClientSide() && !old.is(this)) update(state, level, pos);
    }

    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if (!level.isClientSide()) update(state, level, pos);
    }

    /** Stores the incoming power and toggles the light on a rising edge (copper bulb rule). */
    public void update(BlockState state, Level level, BlockPos pos) {
        int power = SignalDustBlock.enabled() ? incomingPower(state, level, pos) : 0;
        boolean rising = power > 0 && state.getValue(POWER) == 0;
        BlockState next = state.setValue(POWER, power);
        if (rising) {
            next = next.cycle(LIT);
            level.playSound(null, pos, next.getValue(LIT) ? SoundEvents.COPPER_BULB_TURN_ON : SoundEvents.COPPER_BULB_TURN_OFF,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        if (next != state) level.setBlock(pos, next, rising ? Block.UPDATE_ALL : Block.UPDATE_CLIENTS);
    }
}

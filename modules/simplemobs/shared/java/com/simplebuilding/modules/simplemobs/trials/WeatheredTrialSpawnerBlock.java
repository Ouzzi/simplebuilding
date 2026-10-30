package com.simplebuilding.modules.simplemobs.trials;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TrialSpawnerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * The Weathered Trial Spawner (BS27-2): the Vanilla Trial Spawner's whole behaviour (it IS a {@link TrialSpawnerBlock} on the
 * Vanilla block entity, whose valid blocks a mixin widens) in verdigris copper. {@link #KIND} and {@link #LEVEL} tell whether
 * an Evil / Void Omen turned this trial evil (kind) and how strong (level 1-5); they reset when the spawner stops being ominous.
 */
public class WeatheredTrialSpawnerBlock extends TrialSpawnerBlock {
    public static final EnumProperty<TrialKind> KIND = EnumProperty.create("kind", TrialKind.class);
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 5);

    public WeatheredTrialSpawnerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(KIND, TrialKind.WEATHERED).setValue(LEVEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(KIND, LEVEL);
    }

    public static TrialKind kind(BlockState state) {
        return state.hasProperty(KIND) ? state.getValue(KIND) : TrialKind.WEATHERED;
    }

    public static int level(BlockState state) {
        return state.hasProperty(LEVEL) ? state.getValue(LEVEL) : 0;
    }
}

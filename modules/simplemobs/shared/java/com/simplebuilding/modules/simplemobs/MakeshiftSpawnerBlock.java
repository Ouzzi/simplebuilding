package com.simplebuilding.modules.simplemobs;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The Makeshift Spawner: a Vanilla spawner (the Vanilla block entity, spawn eggs work as usual; a mixin widens the entity's valid
 * blocks) that runs only every {@link MobsConfig#makeshiftSpawnerSlowdown}th tick on the server, so delay and spawning are that
 * many times slower. Without a spawn egg it has no mob and does nothing.
 */
public class MakeshiftSpawnerBlock extends SpawnerBlock {
    public MakeshiftSpawnerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return super.getTicker(level, state, type);
        return createTickerHelper(type, BlockEntityTypes.MOB_SPAWNER, (l, pos, s, be) -> {
            if (runsOn(l.getGameTime())) SpawnerBlockEntity.serverTick(l, pos, s, be);
        });
    }

    /** Whether the server ticks the spawner on this game tick: every {@link MobsConfig#makeshiftSpawnerSlowdown}th one. */
    public static boolean runsOn(long gameTime) {
        return gameTime % Math.max(1, MobsConfig.makeshiftSpawnerSlowdown) == 0;
    }
}

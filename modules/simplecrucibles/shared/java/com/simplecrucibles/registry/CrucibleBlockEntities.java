package com.simplecrucibles.registry;

import com.simplecrucibles.SimpleCrucibles;
import com.simplecrucibles.crucible.CrucibleBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** The block entity type is built per loader (constructors differ) and handed to {@link #register}. */
public final class CrucibleBlockEntities {
    public static BlockEntityType<CrucibleBlockEntity> CRUCIBLE;
    public static BlockEntityType<com.simplecrucibles.crucible.CrucibleBarrelBlockEntity> BARREL;

    public static void register(BlockEntityType<CrucibleBlockEntity> type) {
        CRUCIBLE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SimpleCrucibles.id("crucible"), type);
    }

    public static void registerBarrel(BlockEntityType<com.simplecrucibles.crucible.CrucibleBarrelBlockEntity> type) {
        BARREL = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SimpleCrucibles.id("barrel"), type);
    }

    /** The crucible type accepts every crucible block, also partner tiers registered after it (load order free). */
    public static boolean isCrucible(net.minecraft.world.level.block.state.BlockState state) {
        return state.getBlock() instanceof com.simplecrucibles.crucible.CrucibleBlock;
    }

    public static boolean isBarrel(net.minecraft.world.level.block.state.BlockState state) {
        return state.getBlock() instanceof com.simplecrucibles.crucible.CrucibleBarrelBlock;
    }

    private CrucibleBlockEntities() {}
}

package com.simplelib.registry;

import com.simplelib.SimpleLib;
import com.simplelib.crucible.CrucibleBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** The block entity type is built per loader (constructors differ) and handed to {@link #register}. */
public final class LibBlockEntities {
    public static BlockEntityType<CrucibleBlockEntity> CRUCIBLE;
    public static BlockEntityType<com.simplelib.crucible.CrucibleBarrelBlockEntity> BARREL;

    public static void register(BlockEntityType<CrucibleBlockEntity> type) {
        CRUCIBLE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SimpleLib.id("crucible"), type);
    }

    public static void registerBarrel(BlockEntityType<com.simplelib.crucible.CrucibleBarrelBlockEntity> type) {
        BARREL = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SimpleLib.id("barrel"), type);
    }

    /** The crucible type accepts every crucible block, also partner tiers registered after it (load order free). */
    public static boolean isCrucible(net.minecraft.world.level.block.state.BlockState state) {
        return state.getBlock() instanceof com.simplelib.crucible.CrucibleBlock;
    }

    public static boolean isBarrel(net.minecraft.world.level.block.state.BlockState state) {
        return state.getBlock() instanceof com.simplelib.crucible.CrucibleBarrelBlock;
    }

    private LibBlockEntities() {}
}

package com.simplesandwiches.registry;

import com.simplesandwiches.Sandwiches;
import com.simplesandwiches.block.CuttingBoardBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** The block entity type is built per loader (constructors differ) and handed to {@link #register}. */
public final class ModBlockEntities {
    public static BlockEntityType<CuttingBoardBlockEntity> CUTTING_BOARD;

    public static void register(BlockEntityType<CuttingBoardBlockEntity> type) {
        CUTTING_BOARD = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Sandwiches.id("cutting_board"), type);
    }

    private ModBlockEntities() {}
}

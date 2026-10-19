package com.simplestorage.registry;

import com.simplestorage.SimpleStorage;
import com.simplestorage.crate.CrateBlockEntity;
import com.simplestorage.table.StorageCraftingTableBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** The block entity types are built per loader (constructors differ) and handed to {@link #register}. */
public final class StorageBlockEntities {
    public static BlockEntityType<CrateBlockEntity> CRATE;
    public static BlockEntityType<StorageCraftingTableBlockEntity> STORAGE_CRAFTING_TABLE;

    public static void register(BlockEntityType<CrateBlockEntity> crate, BlockEntityType<StorageCraftingTableBlockEntity> table) {
        CRATE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SimpleStorage.id("crate"), crate);
        STORAGE_CRAFTING_TABLE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SimpleStorage.id(SimpleStorage.STORAGE_CRAFTING_TABLE), table);
    }

    private StorageBlockEntities() {}
}

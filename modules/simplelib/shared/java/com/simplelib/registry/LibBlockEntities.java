package com.simplelib.registry;

import com.simplelib.SimpleLib;
import com.simplelib.crucible.CrucibleBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** The block entity type is built per loader (constructors differ) and handed to {@link #register}. */
public final class LibBlockEntities {
    public static BlockEntityType<CrucibleBlockEntity> CRUCIBLE;

    public static void register(BlockEntityType<CrucibleBlockEntity> type) {
        CRUCIBLE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SimpleLib.id("crucible"), type);
    }

    private LibBlockEntities() {}
}

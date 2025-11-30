package com.simplebuilding.block.entity;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.block.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder; // IMPORTANT IMPORT
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {
    /* TODO: add ReinforcedShulkerBoxBlockEntity
    public static final BlockEntityType<ReinforcedShulkerBoxBlockEntity> REINFORCED_SHULKER_BOX_ENTITY =
                Registry.register(Registries.BLOCK_ENTITY_TYPE,
                        Identifier.of(Simplebuilding.MOD_ID, "reinforced_shulker_box"),
                        // Use FabricBlockEntityTypeBuilder instead of vanilla Builder
                        FabricBlockEntityTypeBuilder.create(ReinforcedShulkerBoxBlockEntity::new,
                                ModBlocks.REINFORCED_SHULKER_BOX).build());
    */

    public static void registerBlockEntities() {
        Simplebuilding.LOGGER.info("Registering Block Entities for " + Simplebuilding.MOD_ID);
    }
}
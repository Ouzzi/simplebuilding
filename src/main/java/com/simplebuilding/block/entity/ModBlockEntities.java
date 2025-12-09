package com.simplebuilding.block.entity;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.block.ModBlocks;
import com.simplebuilding.block.entity.custom.NetheriteShulkerBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {

    public static final BlockEntityType<NetheriteShulkerBlockEntity> NETHERITE_SHULKER_BE =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(Simplebuilding.MOD_ID, "netherite_shulker_be"),
                    FabricBlockEntityTypeBuilder.create(NetheriteShulkerBlockEntity::new, ModBlocks.NETHERITE_SHULKER_BLOCK).build(null));

    public static void registerBlockEntities() {
        Simplebuilding.LOGGER.info("Registering Block Entities for " + Simplebuilding.MOD_ID);
    }
}
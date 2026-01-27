package com.simplebuilding.blocks.entity;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.entity.custom.ModBlastFurnaceBlockEntity;
import com.simplebuilding.blocks.entity.custom.ModFurnaceBlockEntity;
import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.blocks.entity.custom.ModSmokerBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {

    // public static BlockEntityType<ModChestBlockEntity> MOD_CHEST_BE;
    public static BlockEntityType<ModBlastFurnaceBlockEntity> MOD_BLAST_FURNACE_BE;
    public static BlockEntityType<ModHopperBlockEntity> MOD_HOPPER_BE;
    public static BlockEntityType<ModFurnaceBlockEntity> MOD_FURNACE_BE;
    public static BlockEntityType<ModSmokerBlockEntity> MOD_SMOKER_BE;

    public static void registerBlockEntities() {
        MOD_HOPPER_BE = Registry.register(Registries.BLOCK_ENTITY_TYPE,
                Identifier.of(Simplebuilding.MOD_ID, "mod_hopper"),
                FabricBlockEntityTypeBuilder.create(ModHopperBlockEntity::new,
                        ModBlocks.REINFORCED_HOPPER,
                        ModBlocks.NETHERITE_HOPPER
                ).build());

        /* todo chest:

        MOD_CHEST_BE = Registry.register(Registries.BLOCK_ENTITY_TYPE,
                Identifier.of(Simplebuilding.MOD_ID, "mod_chest"),
                FabricBlockEntityTypeBuilder.create(ModChestBlockEntity::new,
                        ModBlocks.REINFORCED_CHEST, ModBlocks.NETHERITE_CHEST).build());
         */

        MOD_BLAST_FURNACE_BE = Registry.register(Registries.BLOCK_ENTITY_TYPE,
                Identifier.of(Simplebuilding.MOD_ID, "mod_blast_furnace"),
                FabricBlockEntityTypeBuilder.create(ModBlastFurnaceBlockEntity::new,
                        ModBlocks.REINFORCED_BLAST_FURNACE, ModBlocks.NETHERITE_BLAST_FURNACE).build());

        MOD_FURNACE_BE = Registry.register(Registries.BLOCK_ENTITY_TYPE,
                Identifier.of(Simplebuilding.MOD_ID, "mod_furnace"),
                FabricBlockEntityTypeBuilder.create(ModFurnaceBlockEntity::new,
                        ModBlocks.REINFORCED_FURNACE, ModBlocks.NETHERITE_FURNACE).build());

        MOD_SMOKER_BE = Registry.register(Registries.BLOCK_ENTITY_TYPE,
                Identifier.of(Simplebuilding.MOD_ID, "mod_smoker"),
                FabricBlockEntityTypeBuilder.create(ModSmokerBlockEntity::new,
                        ModBlocks.REINFORCED_SMOKER, ModBlocks.NETHERITE_SMOKER).build());
    }
}
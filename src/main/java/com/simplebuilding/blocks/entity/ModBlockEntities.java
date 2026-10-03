package com.simplebuilding.blocks.entity;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.entity.custom.BackpackBlockEntity;
import com.simplebuilding.blocks.entity.custom.ModBlastFurnaceBlockEntity;
import com.simplebuilding.blocks.entity.custom.ModFurnaceBlockEntity;
import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.blocks.entity.custom.ModSmokerBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {

    public static BlockEntityType<ModBlastFurnaceBlockEntity> MOD_BLAST_FURNACE_BE;
    public static BlockEntityType<ModHopperBlockEntity> MOD_HOPPER_BE;
    public static BlockEntityType<ModFurnaceBlockEntity> MOD_FURNACE_BE;
    public static BlockEntityType<ModSmokerBlockEntity> MOD_SMOKER_BE;
    public static BlockEntityType<BackpackBlockEntity> BACKPACK_BE;
    public static BlockEntityType<com.simplebuilding.blocks.entity.custom.PlacedTemplateBlockEntity> PLACED_TEMPLATE_BE;
    public static BlockEntityType<com.simplebuilding.blocks.entity.custom.PlacedBundleBlockEntity> PLACED_BUNDLE_BE;
    /** Kleinteile auf einem Fleck; nur, wenn es den Block gibt (McVersion.SMALL_PLACEABLES). */
    public static BlockEntityType<com.simplebuilding.blocks.entity.custom.PlacedSmallPartsBlockEntity> PLACED_SMALL_PARTS_BE;
    public static BlockEntityType<com.simplebuilding.blocks.entity.custom.TieredChestBlockEntity> TIERED_CHEST_BE;
    /** Auto-Schmied; nur, wenn es den Block gibt (McVersion.AUTO_SMITHER). */
    public static BlockEntityType<com.simplebuilding.blocks.entity.custom.AutoSmitherBlockEntity> AUTO_SMITHER_BE;
    public static BlockEntityType<com.simplebuilding.blocks.entity.custom.TieredShulkerBoxBlockEntity> TIERED_SHULKER_BOX_BE;

    public static void registerBlockEntities() {
        MOD_HOPPER_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "mod_hopper"),
                FabricBlockEntityTypeBuilder.create(ModHopperBlockEntity::new,
                        ModBlocks.REINFORCED_HOPPER,
                        ModBlocks.NETHERITE_HOPPER,
                        ModBlocks.ENDERITE_HOPPER
                ).build());


        MOD_BLAST_FURNACE_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "mod_blast_furnace"),
                FabricBlockEntityTypeBuilder.create(ModBlastFurnaceBlockEntity::new,
                        ModBlocks.REINFORCED_BLAST_FURNACE, ModBlocks.NETHERITE_BLAST_FURNACE, ModBlocks.ENDERITE_BLAST_FURNACE).build());

        MOD_FURNACE_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "mod_furnace"),
                FabricBlockEntityTypeBuilder.create(ModFurnaceBlockEntity::new,
                        ModBlocks.REINFORCED_FURNACE, ModBlocks.NETHERITE_FURNACE, ModBlocks.ENDERITE_FURNACE).build());

        MOD_SMOKER_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "mod_smoker"),
                FabricBlockEntityTypeBuilder.create(ModSmokerBlockEntity::new,
                        ModBlocks.REINFORCED_SMOKER, ModBlocks.NETHERITE_SMOKER, ModBlocks.ENDERITE_SMOKER).build());

        BACKPACK_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "backpack"),
                FabricBlockEntityTypeBuilder.create(BackpackBlockEntity::new,
                        ModBlocks.BACKPACK, ModBlocks.REINFORCED_BACKPACK,
                        ModBlocks.NETHERITE_BACKPACK, ModBlocks.ENDERITE_BACKPACK).build());

        TIERED_CHEST_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "tiered_chest"),
                FabricBlockEntityTypeBuilder.create(com.simplebuilding.blocks.entity.custom.TieredChestBlockEntity::new,
                        ModBlocks.tieredChests()).build());

        TIERED_SHULKER_BOX_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "tiered_shulker_box"),
                FabricBlockEntityTypeBuilder.create(com.simplebuilding.blocks.entity.custom.TieredShulkerBoxBlockEntity::new,
                        ModBlocks.REINFORCED_SHULKER_BOX, ModBlocks.NETHERITE_SHULKER_BOX, ModBlocks.ENDERITE_SHULKER_BOX).build());

        PLACED_TEMPLATE_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "placed_smithing_template"),
                FabricBlockEntityTypeBuilder.create(com.simplebuilding.blocks.entity.custom.PlacedTemplateBlockEntity::new,
                        ModBlocks.PLACED_SMITHING_TEMPLATE, ModBlocks.PLACED_BLUEPRINT).build());

        PLACED_BUNDLE_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "placed_bundle"),
                FabricBlockEntityTypeBuilder.create(com.simplebuilding.blocks.entity.custom.PlacedBundleBlockEntity::new, ModBlocks.PLACED_BUNDLE).build());

        if (ModBlocks.AUTO_SMITHER != null) {
            AUTO_SMITHER_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "auto_smither"),
                    FabricBlockEntityTypeBuilder.create(com.simplebuilding.blocks.entity.custom.AutoSmitherBlockEntity::new, ModBlocks.AUTO_SMITHER).build());
        }

        if (ModBlocks.PLACED_SMALL_PARTS != null) {
            PLACED_SMALL_PARTS_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "placed_small_parts"),
                    FabricBlockEntityTypeBuilder.create(com.simplebuilding.blocks.entity.custom.PlacedSmallPartsBlockEntity::new, ModBlocks.PLACED_SMALL_PARTS).build());
        }
    }
}
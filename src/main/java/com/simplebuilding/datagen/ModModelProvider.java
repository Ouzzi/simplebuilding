package com.simplebuilding.datagen;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.items.ModItems;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.block.Block;
import net.minecraft.client.data.*;
import net.minecraft.item.Item;
import net.minecraft.state.property.Properties;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import java.util.Optional;


public class ModModelProvider extends FabricModelProvider {

    // --- Custom Model Definitions (da Vanilla Fields fehlen könnten) ---
    // Wir verweisen auf die Vanilla JSON Dateien
    private static final Model HOPPER_MODEL = new Model(Optional.of(Identifier.ofVanilla("block/hopper")), Optional.empty(), TextureKey.BOTTOM, TextureKey.TOP, TextureKey.SIDE, TextureKey.INSIDE);
    private static final Model HOPPER_SIDE_MODEL = new Model(Optional.of(Identifier.ofVanilla("block/hopper_side")), Optional.empty(), TextureKey.BOTTOM, TextureKey.TOP, TextureKey.SIDE, TextureKey.INSIDE);
    private static final Model PISTON_BASE_MODEL = new Model(Optional.of(Identifier.ofVanilla("block/piston_base")), Optional.empty(), TextureKey.BOTTOM, TextureKey.SIDE, TextureKey.PLATFORM);

    public ModModelProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator) {
        // --- 1. Basic Blocks ---
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.CONSTRUCTION_LIGHT);
        blockStateModelGenerator.registerParentedItemModel(ModBlocks.CONSTRUCTION_LIGHT, ModelIds.getBlockModelId(ModBlocks.CONSTRUCTION_LIGHT));

        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.CRACKED_DIAMOND_BLOCK);
        blockStateModelGenerator.registerParentedItemModel(ModBlocks.CRACKED_DIAMOND_BLOCK, ModelIds.getBlockModelId(ModBlocks.CRACKED_DIAMOND_BLOCK));

        // --- 2. Blast Furnaces ---
        blockStateModelGenerator.registerCooker(ModBlocks.REINFORCED_BLAST_FURNACE, TexturedModel.ORIENTABLE);
        blockStateModelGenerator.registerCooker(ModBlocks.NETHERITE_BLAST_FURNACE, TexturedModel.ORIENTABLE);
        blockStateModelGenerator.registerParentedItemModel(ModBlocks.REINFORCED_BLAST_FURNACE, ModelIds.getBlockModelId(ModBlocks.REINFORCED_BLAST_FURNACE));
        blockStateModelGenerator.registerParentedItemModel(ModBlocks.NETHERITE_BLAST_FURNACE, ModelIds.getBlockModelId(ModBlocks.NETHERITE_BLAST_FURNACE));

        // --- Standard Furnaces ---
        blockStateModelGenerator.registerCooker(ModBlocks.REINFORCED_FURNACE, TexturedModel.ORIENTABLE);
        blockStateModelGenerator.registerCooker(ModBlocks.NETHERITE_FURNACE, TexturedModel.ORIENTABLE);
        blockStateModelGenerator.registerParentedItemModel(ModBlocks.REINFORCED_FURNACE, ModelIds.getBlockModelId(ModBlocks.REINFORCED_FURNACE));
        blockStateModelGenerator.registerParentedItemModel(ModBlocks.NETHERITE_FURNACE, ModelIds.getBlockModelId(ModBlocks.NETHERITE_FURNACE));

        // --- Smokers ---
        blockStateModelGenerator.registerCooker(ModBlocks.REINFORCED_SMOKER, TexturedModel.ORIENTABLE);
        blockStateModelGenerator.registerCooker(ModBlocks.NETHERITE_SMOKER, TexturedModel.ORIENTABLE);
        blockStateModelGenerator.registerParentedItemModel(ModBlocks.REINFORCED_SMOKER, ModelIds.getBlockModelId(ModBlocks.REINFORCED_SMOKER));
        blockStateModelGenerator.registerParentedItemModel(ModBlocks.NETHERITE_SMOKER, ModelIds.getBlockModelId(ModBlocks.NETHERITE_SMOKER));



        // --- 3. Chests ---
        // todo chest:

        // blockStateModelGenerator.registerChest(ModBlocks.REINFORCED_CHEST, ModBlocks.REINFORCED_CHEST, Identifier.of(Simplebuilding.MOD_ID, "entity/chest/reinforced_chest"), false);
        // blockStateModelGenerator.registerChest(ModBlocks.NETHERITE_CHEST, ModBlocks.NETHERITE_CHEST, Identifier.of(Simplebuilding.MOD_ID, "entity/chest/netherite_chest"), false);

        //blockStateModelGenerator.registerParentedItemModel(ModBlocks.REINFORCED_CHEST, ModelIds.getBlockModelId(ModBlocks.REINFORCED_CHEST));
        //blockStateModelGenerator.registerParentedItemModel(ModBlocks.NETHERITE_CHEST, ModelIds.getBlockModelId(ModBlocks.NETHERITE_CHEST));

        // --- 4. Hoppers ---
        registerCustomHopper(blockStateModelGenerator, ModBlocks.REINFORCED_HOPPER);
        registerCustomHopper(blockStateModelGenerator, ModBlocks.NETHERITE_HOPPER);

        // --- 5. Pistons ---
        // Reinforced Piston is a real Piston (has EXTENDED property)
        registerCustomPiston(blockStateModelGenerator, ModBlocks.REINFORCED_PISTON);
        registerCustomPiston(blockStateModelGenerator, ModBlocks.NETHERITE_PISTON);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.NETHERITE_PISTON_HEAD);

    }

    private void registerCustomHopper(BlockStateModelGenerator generator, Block block) {
        TextureMap textures = new TextureMap()
                .put(TextureKey.TOP, TextureMap.getSubId(block, "_top"))
                .put(TextureKey.SIDE, TextureMap.getSubId(block, "_outside"))
                .put(TextureKey.INSIDE, TextureMap.getSubId(block, "_inside"))
                .put(TextureKey.BOTTOM, TextureMap.getSubId(block, "_outside"));

        Identifier modelDown = HOPPER_MODEL.upload(block, textures, generator.modelCollector);
        Identifier modelSide = HOPPER_SIDE_MODEL.upload(block, "_side", textures, generator.modelCollector);

        generator.blockStateCollector.accept(VariantsBlockModelDefinitionCreator.of(block)
                .with(BlockStateVariantMap.models(Properties.HOPPER_FACING)
                        .register(Direction.DOWN, BlockStateModelGenerator.createWeightedVariant(modelDown))
                        .register(Direction.NORTH, BlockStateModelGenerator.createWeightedVariant(modelSide))
                        .register(Direction.EAST, BlockStateModelGenerator.createWeightedVariant(modelSide).apply(BlockStateModelGenerator.ROTATE_Y_90))
                        .register(Direction.SOUTH, BlockStateModelGenerator.createWeightedVariant(modelSide).apply(BlockStateModelGenerator.ROTATE_Y_180))
                        .register(Direction.WEST, BlockStateModelGenerator.createWeightedVariant(modelSide).apply(BlockStateModelGenerator.ROTATE_Y_270))
                ));
    }

    private void registerCustomPiston(BlockStateModelGenerator generator, Block block) {
        TextureMap textureMap = new TextureMap()
                .put(TextureKey.BOTTOM, TextureMap.getSubId(block, "_bottom"))
                .put(TextureKey.SIDE, TextureMap.getSubId(block, "_side"))
                .put(TextureKey.PLATFORM, TextureMap.getSubId(block, "_top"))
                .put(TextureKey.INSIDE, TextureMap.getSubId(block, "_inner"));

        Identifier baseModelId = PISTON_BASE_MODEL.upload(block, "_base", textureMap, generator.modelCollector);

        generator.registerPiston(block, BlockStateModelGenerator.createWeightedVariant(baseModelId), textureMap);
        TextureMap inventoryMap = new TextureMap()
                .put(TextureKey.BOTTOM, TextureMap.getSubId(block, "_bottom"))
                .put(TextureKey.TOP, TextureMap.getSubId(block, "_top"))
                .put(TextureKey.SIDE, TextureMap.getSubId(block, "_side"));

        Identifier inventoryModelId = Models.CUBE_BOTTOM_TOP.upload(block, "_inventory", inventoryMap, generator.modelCollector);

        generator.registerParentedItemModel(block, inventoryModelId);
    }

    @Override
    public void generateItemModels(ItemModelGenerator itemModelGenerator) {

        // --- 1. RANGEFINDER (Generated / Flach) ---
        itemModelGenerator.register(ModItems.OCTANT, Models.GENERATED);
        for (DyeColor color : DyeColor.values()) {
            Item item = ModItems.COLORED_OCTANT_ITEMS.get(color);
            if (item != null) itemModelGenerator.register(item, Models.GENERATED);
        }

        // --- 2. CHISELS ---
        itemModelGenerator.register(ModItems.STONE_CHISEL, Models.HANDHELD);
        itemModelGenerator.register(ModItems.COPPER_CHISEL, Models.HANDHELD);
        itemModelGenerator.register(ModItems.IRON_CHISEL, Models.HANDHELD);
        itemModelGenerator.register(ModItems.GOLD_CHISEL, Models.HANDHELD);
        itemModelGenerator.register(ModItems.DIAMOND_CHISEL, Models.HANDHELD);
        itemModelGenerator.register(ModItems.NETHERITE_CHISEL, Models.HANDHELD);

        // --- 3. SPATULAS ---
        itemModelGenerator.register(ModItems.STONE_SPATULA, Models.HANDHELD);
        itemModelGenerator.register(ModItems.COPPER_SPATULA, Models.HANDHELD);
        itemModelGenerator.register(ModItems.IRON_SPATULA, Models.HANDHELD);
        itemModelGenerator.register(ModItems.GOLD_SPATULA, Models.HANDHELD);
        itemModelGenerator.register(ModItems.DIAMOND_SPATULA, Models.HANDHELD);
        itemModelGenerator.register(ModItems.NETHERITE_SPATULA, Models.HANDHELD);

        // --- WANDS ---
        itemModelGenerator.register(ModItems.COPPER_BUILDING_WAND, Models.HANDHELD);
        itemModelGenerator.register(ModItems.IRON_BUILDING_WAND, Models.HANDHELD);
        itemModelGenerator.register(ModItems.GOLD_BUILDING_WAND, Models.HANDHELD);
        itemModelGenerator.register(ModItems.DIAMOND_BUILDING_WAND, Models.HANDHELD);
        itemModelGenerator.register(ModItems.NETHERITE_BUILDING_WAND, Models.HANDHELD);

        // --- SLEDGEHAMMERS ---
        itemModelGenerator.register(ModItems.STONE_SLEDGEHAMMER, Models.HANDHELD);
        itemModelGenerator.register(ModItems.COPPER_SLEDGEHAMMER, Models.HANDHELD);
        itemModelGenerator.register(ModItems.IRON_SLEDGEHAMMER, Models.HANDHELD);
        itemModelGenerator.register(ModItems.GOLD_SLEDGEHAMMER, Models.HANDHELD);
        itemModelGenerator.register(ModItems.DIAMOND_SLEDGEHAMMER, Models.HANDHELD);
        itemModelGenerator.register(ModItems.NETHERITE_SLEDGEHAMMER, Models.HANDHELD);

        // --- CORES & MISC ---
        itemModelGenerator.register(ModItems.COPPER_CORE, Models.GENERATED);
        itemModelGenerator.register(ModItems.IRON_CORE, Models.GENERATED);
        itemModelGenerator.register(ModItems.GOLD_CORE, Models.GENERATED);
        itemModelGenerator.register(ModItems.DIAMOND_CORE, Models.GENERATED);
        itemModelGenerator.register(ModItems.NETHERITE_CORE, Models.GENERATED);

        itemModelGenerator.register(ModItems.REINFORCED_BUNDLE, Models.GENERATED);
        itemModelGenerator.register(ModItems.NETHERITE_BUNDLE, Models.GENERATED);
        itemModelGenerator.register(ModItems.QUIVER, Models.GENERATED);
        itemModelGenerator.register(ModItems.NETHERITE_QUIVER, Models.GENERATED);
        itemModelGenerator.register(ModItems.VELOCITY_GAUGE, Models.GENERATED);
        itemModelGenerator.register(ModItems.DIAMOND_PEBBLE, Models.GENERATED);
        itemModelGenerator.register(ModItems.CRACKED_DIAMOND, Models.GENERATED);

        // Hoppers hier auch, da Generated Item Model für Inventory
        itemModelGenerator.register(ModItems.REINFORCED_HOPPER, Models.GENERATED);
        itemModelGenerator.register(ModItems.NETHERITE_HOPPER, Models.GENERATED);
    }
}

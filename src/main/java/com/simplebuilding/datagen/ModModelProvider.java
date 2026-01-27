package com.simplebuilding.datagen;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.items.ModItems;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.block.Block;
import net.minecraft.client.data.*;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
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

        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.POLISHED_END_STONE);

        registerMirroredChecker(blockStateModelGenerator, ModBlocks.PURPUR_QUARTZ_CHECKER);
        registerMirroredChecker(blockStateModelGenerator, ModBlocks.LAPIS_QUARTZ_CHECKER);
        registerMirroredChecker(blockStateModelGenerator, ModBlocks.BLACKSTONE_QUARTZ_CHECKER);
        registerMirroredChecker(blockStateModelGenerator, ModBlocks.RESIN_QUARTZ_CHECKER);

        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.ASTRAL_PURPUR_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.NIHIL_PURPUR_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.ASTRAL_END_STONE);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.NIHIL_END_STONE);

        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.SUSPENDED_SAND);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.SUSPENDED_GRAVEL);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.LEVITATING_SAND);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.LEVITATING_GRAVEL);


        // --- 1. Basic Blocks ---
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.CONSTRUCTION_LIGHT);
        blockStateModelGenerator.registerParentedItemModel(ModBlocks.CONSTRUCTION_LIGHT, ModelIds.getBlockModelId(ModBlocks.CONSTRUCTION_LIGHT));

        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.CRACKED_DIAMOND_BLOCK);
        blockStateModelGenerator.registerParentedItemModel(ModBlocks.CRACKED_DIAMOND_BLOCK, ModelIds.getBlockModelId(ModBlocks.CRACKED_DIAMOND_BLOCK));

        // --- NEW: Enderite Blocks ---
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.ENDERITE_BLOCK);
        blockStateModelGenerator.registerParentedItemModel(ModBlocks.ENDERITE_BLOCK, ModelIds.getBlockModelId(ModBlocks.ENDERITE_BLOCK));

        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.NIHILITH_ORE);
        blockStateModelGenerator.registerParentedItemModel(ModBlocks.NIHILITH_ORE, ModelIds.getBlockModelId(ModBlocks.NIHILITH_ORE));

        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.ASTRALIT_ORE);
        blockStateModelGenerator.registerParentedItemModel(ModBlocks.ASTRALIT_ORE, ModelIds.getBlockModelId(ModBlocks.ASTRALIT_ORE));


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
        itemModelGenerator.register(ModItems.ENDERITE_CHISEL, Models.HANDHELD); // NEW

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
        itemModelGenerator.register(ModItems.ENDERITE_BUILDING_WAND, Models.HANDHELD); // NEW

        // --- SLEDGEHAMMERS ---
        itemModelGenerator.register(ModItems.STONE_SLEDGEHAMMER, Models.HANDHELD);
        itemModelGenerator.register(ModItems.COPPER_SLEDGEHAMMER, Models.HANDHELD);
        itemModelGenerator.register(ModItems.IRON_SLEDGEHAMMER, Models.HANDHELD);
        itemModelGenerator.register(ModItems.GOLD_SLEDGEHAMMER, Models.HANDHELD);
        itemModelGenerator.register(ModItems.DIAMOND_SLEDGEHAMMER, Models.HANDHELD);
        itemModelGenerator.register(ModItems.NETHERITE_SLEDGEHAMMER, Models.HANDHELD);
        itemModelGenerator.register(ModItems.ENDERITE_SLEDGEHAMMER, Models.HANDHELD); // NEW

        // --- NEW: ENDERITE TOOLS (HANDHELD) ---
        itemModelGenerator.register(ModItems.ENDERITE_SWORD, Models.HANDHELD);
        itemModelGenerator.register(ModItems.ENDERITE_PICKAXE, Models.HANDHELD);
        itemModelGenerator.register(ModItems.ENDERITE_AXE, Models.HANDHELD);
        itemModelGenerator.register(ModItems.ENDERITE_SHOVEL, Models.HANDHELD);
        itemModelGenerator.register(ModItems.ENDERITE_HOE, Models.HANDHELD);

        // --- NEW: ENDERITE ARMOR (GENERATED) ---
        itemModelGenerator.register(ModItems.ENDERITE_HELMET, Models.GENERATED);
        itemModelGenerator.register(ModItems.ENDERITE_CHESTPLATE, Models.GENERATED);
        itemModelGenerator.register(ModItems.ENDERITE_LEGGINGS, Models.GENERATED);
        itemModelGenerator.register(ModItems.ENDERITE_BOOTS, Models.GENERATED);

        // --- NEW: ENDERITE MATERIALS (GENERATED) ---
        itemModelGenerator.register(ModItems.ENDERITE_CORE, Models.GENERATED);
        itemModelGenerator.register(ModItems.RAW_ENDERITE, Models.GENERATED);
        itemModelGenerator.register(ModItems.ENDERITE_INGOT, Models.GENERATED);
        itemModelGenerator.register(ModItems.ENDERITE_SCRAP, Models.GENERATED);
        itemModelGenerator.register(ModItems.NIHILITH_SHARD, Models.GENERATED);
        itemModelGenerator.register(ModItems.ASTRALIT_DUST, Models.GENERATED);
        itemModelGenerator.register(ModItems.ENDERITE_UPGRADE_TEMPLATE, Models.GENERATED);


        // --- CORES & MISC ---
        itemModelGenerator.register(ModItems.VELOCITY_GAUGE, Models.GENERATED);
        itemModelGenerator.register(ModItems.ORE_DETECTOR, Models.GENERATED);
        itemModelGenerator.register(ModItems.MAGNET, Models.GENERATED);
        itemModelGenerator.register(ModItems.ROTATOR, Models.HANDHELD);

        itemModelGenerator.register(ModItems.COPPER_CORE, Models.GENERATED);
        itemModelGenerator.register(ModItems.IRON_CORE, Models.GENERATED);
        itemModelGenerator.register(ModItems.GOLD_CORE, Models.GENERATED);
        itemModelGenerator.register(ModItems.DIAMOND_CORE, Models.GENERATED);
        itemModelGenerator.register(ModItems.NETHERITE_CORE, Models.GENERATED);

        itemModelGenerator.register(ModItems.REINFORCED_BUNDLE, Models.GENERATED);
        itemModelGenerator.register(ModItems.NETHERITE_BUNDLE, Models.GENERATED);
        itemModelGenerator.register(ModItems.QUIVER, Models.GENERATED);
        itemModelGenerator.register(ModItems.NETHERITE_QUIVER, Models.GENERATED);
        itemModelGenerator.register(ModItems.DIAMOND_PEBBLE, Models.GENERATED);
        itemModelGenerator.register(ModItems.CRACKED_DIAMOND, Models.GENERATED);

        // Hoppers hier auch, da Generated Item Model für Inventory
        itemModelGenerator.register(ModItems.REINFORCED_HOPPER, Models.GENERATED);
        itemModelGenerator.register(ModItems.NETHERITE_HOPPER, Models.GENERATED);

        itemModelGenerator.register(ModItems.GLOWING_TRIM_TEMPLATE, Models.GENERATED);
        itemModelGenerator.register(ModItems.EMITTING_TRIM_TEMPLATE, Models.GENERATED);

        itemModelGenerator.register(ModItems.BASIC_UPGRADE_TEMPLATE, Models.GENERATED);

        itemModelGenerator.register(ModItems.NETHERITE_NUGGET, Models.GENERATED);
        itemModelGenerator.register(ModItems.NETHERITE_APPLE, Models.GENERATED);
        itemModelGenerator.register(ModItems.NETHERITE_CARROT, Models.GENERATED);


        String[] enchants = {
                "vein_miner", "deep_pockets", "strip_miner", "versatility",
                "drawer", "kinetic_protection", "double_jump", "override",
                "funnel", "range"
        };

        for (String suffix : enchants) {
            Identifier textureId = Identifier.of(Simplebuilding.MOD_ID, "item/enchanted_book_" + suffix);
            Models.GENERATED.upload(
                    Identifier.of(Simplebuilding.MOD_ID, "item/enchanted_book_" + suffix),
                    TextureMap.layer0(textureId),
                    itemModelGenerator.modelCollector
            );
        }

        itemModelGenerator.register(ModItems.ENDERITE_BUNDLE, Models.GENERATED);
        itemModelGenerator.register(ModItems.ENDERITE_QUIVER, Models.GENERATED);
        itemModelGenerator.register(ModItems.ENDERITE_APPLE, Models.GENERATED);
        itemModelGenerator.register(ModItems.ENDERITE_CARROT, Models.GENERATED);
    }

    private void registerMirroredChecker(BlockStateModelGenerator generator, Block block) {
        Identifier blockId = Registries.BLOCK.getId(block);
        String name = blockId.getPath();

        // Pfad zur normalen Textur: block/blockname
        Identifier normalTexture = Identifier.of(Simplebuilding.MOD_ID, "block/" + name);
        // Pfad zur gespiegelten Textur: block/blockname_mirror
        Identifier mirrorTexture = Identifier.of(Simplebuilding.MOD_ID, "block/" + name + "_mirror");

        // Wir definieren manuell, welche Seite welche Textur bekommt
        TextureMap textureMap = new TextureMap()
                .put(TextureKey.PARTICLE, normalTexture)
                .put(TextureKey.UP, normalTexture)    // Oben: Normal
                .put(TextureKey.DOWN, normalTexture)  // Unten: Normal
                .put(TextureKey.EAST, normalTexture)  // Rechts: Normal
                .put(TextureKey.WEST, normalTexture)  // Links: Normal
                .put(TextureKey.NORTH, mirrorTexture) // Vorne: Gespiegelt
                .put(TextureKey.SOUTH, mirrorTexture);// Hinten: Gespiegelt

        // Modell erstellen (CUBE = voller Würfel mit 6 Seiten-Definitionen)
        Identifier modelId = Models.CUBE.upload(block, textureMap, generator.modelCollector);

        // WICHTIG: Die ID muss in einen WeightedVariant umgewandelt werden!
        generator.registerAxisRotated(block, BlockStateModelGenerator.createWeightedVariant(modelId));
    }
}

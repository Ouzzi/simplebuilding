package com.simplebuilding.datagen;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.items.ModArmorMaterials;
import com.simplebuilding.items.ModItems;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.client.data.*;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
// MC 1.21.11: TextureMapping arbeitet noch direkt mit Identifier; den Wrapper
// net.minecraft.client.resources.model.sprite.Material gibt es erst ab 26.2
// (dort ist new Material(id) nur "Sprite-Id ohne forceTranslucent" und serialisiert
// zur selben Zeichenkette). Deshalb hier ueberall die nackte Identifier.
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import java.util.Optional;


public class ModModelProvider extends FabricModelProvider {

    // --- Custom Model Definitions (da Vanilla Fields fehlen könnten) ---
    // Wir verweisen auf die Vanilla JSON Dateien
    private static final ModelTemplate HOPPER_MODEL = new ModelTemplate(Optional.of(Identifier.withDefaultNamespace("block/hopper")), Optional.empty(), TextureSlot.BOTTOM, TextureSlot.TOP, TextureSlot.SIDE, TextureSlot.INSIDE);
    private static final ModelTemplate HOPPER_SIDE_MODEL = new ModelTemplate(Optional.of(Identifier.withDefaultNamespace("block/hopper_side")), Optional.empty(), TextureSlot.BOTTOM, TextureSlot.TOP, TextureSlot.SIDE, TextureSlot.INSIDE);
    private static final ModelTemplate PISTON_BASE_MODEL = new ModelTemplate(Optional.of(Identifier.withDefaultNamespace("block/piston_base")), Optional.empty(), TextureSlot.BOTTOM, TextureSlot.SIDE, TextureSlot.PLATFORM);

    public ModModelProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators blockStateModelGenerator) {

        blockStateModelGenerator.createTrivialCube(ModBlocks.POLISHED_END_STONE);

        registerMirroredChecker(blockStateModelGenerator, ModBlocks.PURPUR_QUARTZ_CHECKER);
        registerMirroredChecker(blockStateModelGenerator, ModBlocks.LAPIS_QUARTZ_CHECKER);
        registerMirroredChecker(blockStateModelGenerator, ModBlocks.BLACKSTONE_QUARTZ_CHECKER);
        registerMirroredChecker(blockStateModelGenerator, ModBlocks.RESIN_QUARTZ_CHECKER);

        blockStateModelGenerator.createTrivialCube(ModBlocks.ASTRAL_PURPUR_BLOCK);
        blockStateModelGenerator.createTrivialCube(ModBlocks.NIHIL_PURPUR_BLOCK);
        blockStateModelGenerator.createTrivialCube(ModBlocks.ASTRAL_END_STONE);
        blockStateModelGenerator.createTrivialCube(ModBlocks.NIHIL_END_STONE);

        blockStateModelGenerator.createTrivialCube(ModBlocks.SUSPENDED_SAND);
        blockStateModelGenerator.createTrivialCube(ModBlocks.SUSPENDED_GRAVEL);
        blockStateModelGenerator.createTrivialCube(ModBlocks.LEVITATING_SAND);
        blockStateModelGenerator.createTrivialCube(ModBlocks.LEVITATING_GRAVEL);


        // --- 1. Basic Blocks ---
        blockStateModelGenerator.createTrivialCube(ModBlocks.CONSTRUCTION_LIGHT);
        blockStateModelGenerator.registerSimpleItemModel(ModBlocks.CONSTRUCTION_LIGHT, ModelLocationUtils.getModelLocation(ModBlocks.CONSTRUCTION_LIGHT));

        blockStateModelGenerator.createTrivialCube(ModBlocks.CRACKED_DIAMOND_BLOCK);
        blockStateModelGenerator.registerSimpleItemModel(ModBlocks.CRACKED_DIAMOND_BLOCK, ModelLocationUtils.getModelLocation(ModBlocks.CRACKED_DIAMOND_BLOCK));

        // --- NEW: Enderite Blocks ---
        blockStateModelGenerator.createTrivialCube(ModBlocks.ENDERITE_BLOCK);
        blockStateModelGenerator.registerSimpleItemModel(ModBlocks.ENDERITE_BLOCK, ModelLocationUtils.getModelLocation(ModBlocks.ENDERITE_BLOCK));

        blockStateModelGenerator.createTrivialCube(ModBlocks.NIHILITH_ORE);
        blockStateModelGenerator.registerSimpleItemModel(ModBlocks.NIHILITH_ORE, ModelLocationUtils.getModelLocation(ModBlocks.NIHILITH_ORE));

        blockStateModelGenerator.createTrivialCube(ModBlocks.ASTRALIT_ORE);
        blockStateModelGenerator.registerSimpleItemModel(ModBlocks.ASTRALIT_ORE, ModelLocationUtils.getModelLocation(ModBlocks.ASTRALIT_ORE));


        // --- 2. Blast Furnaces ---
        blockStateModelGenerator.createFurnace(ModBlocks.REINFORCED_BLAST_FURNACE, TexturedModel.ORIENTABLE_ONLY_TOP);
        blockStateModelGenerator.createFurnace(ModBlocks.NETHERITE_BLAST_FURNACE, TexturedModel.ORIENTABLE_ONLY_TOP);
        blockStateModelGenerator.registerSimpleItemModel(ModBlocks.REINFORCED_BLAST_FURNACE, ModelLocationUtils.getModelLocation(ModBlocks.REINFORCED_BLAST_FURNACE));
        blockStateModelGenerator.registerSimpleItemModel(ModBlocks.NETHERITE_BLAST_FURNACE, ModelLocationUtils.getModelLocation(ModBlocks.NETHERITE_BLAST_FURNACE));

        // --- Standard Furnaces ---
        blockStateModelGenerator.createFurnace(ModBlocks.REINFORCED_FURNACE, TexturedModel.ORIENTABLE_ONLY_TOP);
        blockStateModelGenerator.createFurnace(ModBlocks.NETHERITE_FURNACE, TexturedModel.ORIENTABLE_ONLY_TOP);
        blockStateModelGenerator.registerSimpleItemModel(ModBlocks.REINFORCED_FURNACE, ModelLocationUtils.getModelLocation(ModBlocks.REINFORCED_FURNACE));
        blockStateModelGenerator.registerSimpleItemModel(ModBlocks.NETHERITE_FURNACE, ModelLocationUtils.getModelLocation(ModBlocks.NETHERITE_FURNACE));

        // --- Smokers ---
        blockStateModelGenerator.createFurnace(ModBlocks.REINFORCED_SMOKER, TexturedModel.ORIENTABLE_ONLY_TOP);
        blockStateModelGenerator.createFurnace(ModBlocks.NETHERITE_SMOKER, TexturedModel.ORIENTABLE_ONLY_TOP);
        blockStateModelGenerator.registerSimpleItemModel(ModBlocks.REINFORCED_SMOKER, ModelLocationUtils.getModelLocation(ModBlocks.REINFORCED_SMOKER));
        blockStateModelGenerator.registerSimpleItemModel(ModBlocks.NETHERITE_SMOKER, ModelLocationUtils.getModelLocation(ModBlocks.NETHERITE_SMOKER));



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
        blockStateModelGenerator.createTrivialCube(ModBlocks.NETHERITE_PISTON_HEAD);

    }

    private void registerCustomHopper(BlockModelGenerators generator, Block block) {
        TextureMapping textures = new TextureMapping()
                .put(TextureSlot.TOP, TextureMapping.getBlockTexture(block, "_top"))
                .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(block, "_outside"))
                .put(TextureSlot.INSIDE, TextureMapping.getBlockTexture(block, "_inside"))
                .put(TextureSlot.BOTTOM, TextureMapping.getBlockTexture(block, "_outside"));

        Identifier modelDown = HOPPER_MODEL.create(block, textures, generator.modelOutput);
        Identifier modelSide = HOPPER_SIDE_MODEL.createWithSuffix(block, "_side", textures, generator.modelOutput);

        generator.blockStateOutput.accept(MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(BlockStateProperties.FACING_HOPPER)
                        .select(Direction.DOWN, BlockModelGenerators.plainVariant(modelDown))
                        .select(Direction.NORTH, BlockModelGenerators.plainVariant(modelSide))
                        .select(Direction.EAST, BlockModelGenerators.plainVariant(modelSide).with(BlockModelGenerators.Y_ROT_90))
                        .select(Direction.SOUTH, BlockModelGenerators.plainVariant(modelSide).with(BlockModelGenerators.Y_ROT_180))
                        .select(Direction.WEST, BlockModelGenerators.plainVariant(modelSide).with(BlockModelGenerators.Y_ROT_270))
                ));
    }

    private void registerCustomPiston(BlockModelGenerators generator, Block block) {
        TextureMapping textureMap = new TextureMapping()
                .put(TextureSlot.BOTTOM, TextureMapping.getBlockTexture(block, "_bottom"))
                .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(block, "_side"))
                .put(TextureSlot.PLATFORM, TextureMapping.getBlockTexture(block, "_top"))
                .put(TextureSlot.INSIDE, TextureMapping.getBlockTexture(block, "_inner"));

        Identifier baseModelId = PISTON_BASE_MODEL.createWithSuffix(block, "_base", textureMap, generator.modelOutput);

        generator.createPistonVariant(block, BlockModelGenerators.plainVariant(baseModelId), textureMap);
        TextureMapping inventoryMap = new TextureMapping()
                .put(TextureSlot.BOTTOM, TextureMapping.getBlockTexture(block, "_bottom"))
                .put(TextureSlot.TOP, TextureMapping.getBlockTexture(block, "_top"))
                .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(block, "_side"));

        Identifier inventoryModelId = ModelTemplates.CUBE_BOTTOM_TOP.createWithSuffix(block, "_inventory", inventoryMap, generator.modelOutput);

        generator.registerSimpleItemModel(block, inventoryModelId);
    }

    @Override
    public void generateItemModels(ItemModelGenerators itemModelGenerator) {

        // --- 1. RANGEFINDER (Generated / Flach) ---
        itemModelGenerator.generateFlatItem(ModItems.OCTANT, ModelTemplates.FLAT_ITEM);
        for (DyeColor color : DyeColor.values()) {
            Item item = ModItems.COLORED_OCTANT_ITEMS.get(color);
            if (item != null) itemModelGenerator.generateFlatItem(item, ModelTemplates.FLAT_ITEM);
        }

        // --- 2. CHISELS ---
        itemModelGenerator.generateFlatItem(ModItems.STONE_CHISEL, ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.COPPER_CHISEL, ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.IRON_CHISEL, ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.GOLD_CHISEL, ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.DIAMOND_CHISEL, ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.NETHERITE_CHISEL, ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.ENDERITE_CHISEL, ModelTemplates.FLAT_HANDHELD_ITEM); // NEW

        // --- WANDS ---
        itemModelGenerator.generateFlatItem(ModItems.COPPER_BUILDING_WAND, ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.IRON_BUILDING_WAND, ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.GOLD_BUILDING_WAND, ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.DIAMOND_BUILDING_WAND, ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.NETHERITE_BUILDING_WAND, ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.ENDERITE_BUILDING_WAND, ModelTemplates.FLAT_HANDHELD_ITEM); // NEW

        // --- SLEDGEHAMMERS ---
        itemModelGenerator.generateFlatItem(ModItems.STONE_SLEDGEHAMMER, ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.COPPER_SLEDGEHAMMER, ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.IRON_SLEDGEHAMMER, ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.GOLD_SLEDGEHAMMER, ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.DIAMOND_SLEDGEHAMMER, ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.NETHERITE_SLEDGEHAMMER, ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.ENDERITE_SLEDGEHAMMER, ModelTemplates.FLAT_HANDHELD_ITEM); // NEW

        // --- NEW: ENDERITE TOOLS (HANDHELD) ---
        itemModelGenerator.generateFlatItem(ModItems.ENDERITE_SWORD, ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.ENDERITE_PICKAXE, ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.ENDERITE_AXE, ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.ENDERITE_SHOVEL, ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.ENDERITE_HOE, ModelTemplates.FLAT_HANDHELD_ITEM);

        // --- NEW: ENDERITE ARMOR (TRIM-AWARE) ---
        itemModelGenerator.generateTrimmableItem(ModItems.ENDERITE_HELMET, ModArmorMaterials.ENDERITE_ASSET_KEY, ItemModelGenerators.TRIM_PREFIX_HELMET, false);
        itemModelGenerator.generateTrimmableItem(ModItems.ENDERITE_CHESTPLATE, ModArmorMaterials.ENDERITE_ASSET_KEY, ItemModelGenerators.TRIM_PREFIX_CHESTPLATE, false);
        itemModelGenerator.generateTrimmableItem(ModItems.ENDERITE_LEGGINGS, ModArmorMaterials.ENDERITE_ASSET_KEY, ItemModelGenerators.TRIM_PREFIX_LEGGINGS, false);
        itemModelGenerator.generateTrimmableItem(ModItems.ENDERITE_BOOTS, ModArmorMaterials.ENDERITE_ASSET_KEY, ItemModelGenerators.TRIM_PREFIX_BOOTS, false);

        // --- NEW: ENDERITE MATERIALS (GENERATED) ---
        itemModelGenerator.generateFlatItem(ModItems.ENDERITE_CORE, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.RAW_ENDERITE, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.ENDERITE_INGOT, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.ENDERITE_SCRAP, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.NIHILITH_SHARD, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.ASTRALIT_DUST, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.ENDERITE_UPGRADE_TEMPLATE, ModelTemplates.FLAT_ITEM);


        // --- CORES & MISC ---
        itemModelGenerator.generateFlatItem(ModItems.VELOCITY_GAUGE, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.ORE_DETECTOR, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.MAGNET, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.ROTATOR, ModelTemplates.FLAT_HANDHELD_ITEM);

        itemModelGenerator.generateFlatItem(ModItems.COPPER_CORE, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.IRON_CORE, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.GOLD_CORE, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.DIAMOND_CORE, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.NETHERITE_CORE, ModelTemplates.FLAT_ITEM);

        itemModelGenerator.generateFlatItem(ModItems.REINFORCED_BUNDLE, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.NETHERITE_BUNDLE, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.QUIVER, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.NETHERITE_QUIVER, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.DIAMOND_PEBBLE, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.CRACKED_DIAMOND, ModelTemplates.FLAT_ITEM);

        // Hoppers hier auch, da Generated Item Model für Inventory
        itemModelGenerator.generateFlatItem(ModItems.REINFORCED_HOPPER, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.NETHERITE_HOPPER, ModelTemplates.FLAT_ITEM);

        itemModelGenerator.generateFlatItem(ModItems.GLOWING_TRIM_TEMPLATE, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.EMITTING_TRIM_TEMPLATE, ModelTemplates.FLAT_ITEM);

        itemModelGenerator.generateFlatItem(ModItems.BASIC_UPGRADE_TEMPLATE, ModelTemplates.FLAT_ITEM);

        itemModelGenerator.generateFlatItem(ModItems.NETHERITE_NUGGET, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.ENDERITE_NUGGET, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.NETHERITE_APPLE, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.NETHERITE_CARROT, ModelTemplates.FLAT_ITEM);


        String[] enchants = {
                "fast_chiseling", "constructors_touch", "color_palette", "master_builder",
                "break_through", "radius", "cover", "bridge", "linear",
                "vein_miner", "deep_pockets", "strip_miner", "versatility",
                "drawer", "kinetic_protection", "double_jump", "override",
                "funnel", "range"
        };

        for (String suffix : enchants) {
            Identifier textureId = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "item/enchanted_book_" + suffix);
            ModelTemplates.FLAT_ITEM.create(
                    Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "item/enchanted_book_" + suffix),
                    TextureMapping.layer0(textureId),
                    itemModelGenerator.modelOutput
            );
        }

        itemModelGenerator.generateFlatItem(ModItems.ENDERITE_BUNDLE, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.ENDERITE_QUIVER, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.ENDERITE_APPLE, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.ENDERITE_CARROT, ModelTemplates.FLAT_ITEM);

        // Die verzauberten Aepfel borgen sich die Textur ihrer gewoehnlichen Variante.
        // Wichtig ist die ZWEIARMIGE generateFlatItem-Variante: sie schreibt nicht nur das Modell
        // unter models/item/, sondern auch die seit MC 1.21.4 noetige Item-Definition unter
        // assets/simplebuilding/items/. Ohne die zeigt der Client "No model loaded for default
        // item model ID" und rendert das Platzhaltermodell - und beide Aepfel sind ueber
        // ModLootTableModifications erreichbar, der Fehler waere also sichtbar gewesen.
        itemModelGenerator.generateFlatItem(ModItems.ENCHANTED_NETHERITE_APPLE, ModItems.NETHERITE_APPLE, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.ENCHANTED_ENDERITE_APPLE, ModItems.ENDERITE_APPLE, ModelTemplates.FLAT_ITEM);
    }

    private void registerMirroredChecker(BlockModelGenerators generator, Block block) {
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
        String name = blockId.getPath();

        // Pfad zur normalen Textur: block/blockname
        Identifier normalTexture = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "block/" + name);
        // Pfad zur gespiegelten Textur: block/blockname_mirror
        Identifier mirrorTexture = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "block/" + name + "_mirror");

        // Wir definieren manuell, welche Seite welche Textur bekommt
        TextureMapping textureMap = new TextureMapping()
                .put(TextureSlot.PARTICLE, normalTexture)
                .put(TextureSlot.UP, normalTexture)
                .put(TextureSlot.DOWN, normalTexture)
                .put(TextureSlot.EAST, normalTexture)
                .put(TextureSlot.WEST, normalTexture)
                .put(TextureSlot.NORTH, mirrorTexture)
                .put(TextureSlot.SOUTH, mirrorTexture);

        // Modell erstellen (CUBE = voller Würfel mit 6 Seiten-Definitionen)
        Identifier modelId = ModelTemplates.CUBE.create(block, textureMap, generator.modelOutput);

        // WICHTIG: Die ID muss in einen WeightedVariant umgewandelt werden!
        generator.createAxisAlignedPillarBlockCustomModel(block, BlockModelGenerators.plainVariant(modelId));
    }
}
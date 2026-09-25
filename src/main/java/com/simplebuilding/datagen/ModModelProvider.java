package com.simplebuilding.datagen;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.BackpackBlock;
import com.simplebuilding.items.ModArmorMaterials;
import com.simplebuilding.items.ModItems;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.client.data.*;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import com.simplebuilding.util.DyedStorage;
import net.minecraft.client.color.item.Dye;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.renderer.item.BundleSelectedItemSpecialRenderer;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.properties.conditional.BundleHasSelectedItem;
import net.minecraft.client.renderer.item.properties.select.DisplayContext;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.resources.model.sprite.Material;
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
    // Handgeschriebene Vorlage (assets/simplebuilding/models/block/template_backpack.json): Sack
    // plus Vordertasche, Vorderseite nach Norden; die Stufen setzen nur ihre Texturen ein.
    private static final ModelTemplate BACKPACK_MODEL = new ModelTemplate(Optional.of(Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "block/template_backpack")), Optional.empty(), TextureSlot.FRONT, TextureSlot.BACK, TextureSlot.SIDE, TextureSlot.TOP, TextureSlot.PARTICLE);
    private static final TextureSlot FRONT_OVERLAY = TextureSlot.create("front_overlay");
    private static final TextureSlot BACK_OVERLAY = TextureSlot.create("back_overlay");
    private static final TextureSlot SIDE_OVERLAY = TextureSlot.create("side_overlay");
    private static final TextureSlot TOP_OVERLAY = TextureSlot.create("top_overlay");
    /** Zwei-Ebenen-Vorlage des gefaerbten abgestellten Rucksacks (Ressource, nicht aus Datagen). */
    private static final ModelTemplate BACKPACK_DYED_MODEL = new ModelTemplate(Optional.of(Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "block/template_backpack_dyed")), Optional.of("_dyed"), TextureSlot.FRONT, TextureSlot.BACK, TextureSlot.SIDE, TextureSlot.TOP, FRONT_OVERLAY, BACK_OVERLAY, SIDE_OVERLAY, TOP_OVERLAY, TextureSlot.PARTICLE);

    public ModModelProvider(FabricPackOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators blockStateModelGenerator) {

        blockStateModelGenerator.createTrivialCube(ModBlocks.POLISHED_END_STONE);

        registerMirroredChecker(blockStateModelGenerator, ModBlocks.PURPUR_QUARTZ_CHECKER);
        registerMirroredChecker(blockStateModelGenerator, ModBlocks.LAPIS_QUARTZ_CHECKER);
        registerMirroredChecker(blockStateModelGenerator, ModBlocks.BLACKSTONE_QUARTZ_CHECKER);
        registerMirroredChecker(blockStateModelGenerator, ModBlocks.RESIN_QUARTZ_CHECKER);
        registerMirroredChecker(blockStateModelGenerator, ModBlocks.NIHILITH_QUARTZ_CHECKER);
        registerMirroredChecker(blockStateModelGenerator, ModBlocks.ASTRALIT_QUARTZ_CHECKER);
        registerMirroredChecker(blockStateModelGenerator, ModBlocks.ENDER_QUARTZ_CHECKER);

        blockStateModelGenerator.createTrivialCube(ModBlocks.ASTRAL_PURPUR_BLOCK);
        blockStateModelGenerator.createTrivialCube(ModBlocks.NIHIL_PURPUR_BLOCK);
        blockStateModelGenerator.createTrivialCube(ModBlocks.ASTRAL_END_STONE);
        blockStateModelGenerator.createTrivialCube(ModBlocks.NIHIL_END_STONE);

        // Astralit-/Nihilith-Bausatz: Ziegelfamilie (Treppe, Stufe, Mauer samt Inventarmodell),
        // Saeule mit eigener Stirnseite (_top) und gemeisselte Ziegel als einfacher Wuerfel.
        for (ModBlocks.EndPalette palette : ModBlocks.END_PALETTES) {
            registerEndPalette(blockStateModelGenerator, palette);
        }

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
        blockStateModelGenerator.createFurnace(ModBlocks.ENDERITE_BLAST_FURNACE, TexturedModel.ORIENTABLE_ONLY_TOP);
        blockStateModelGenerator.registerSimpleItemModel(ModBlocks.ENDERITE_BLAST_FURNACE, ModelLocationUtils.getModelLocation(ModBlocks.ENDERITE_BLAST_FURNACE));

        // --- Standard Furnaces ---
        blockStateModelGenerator.createFurnace(ModBlocks.REINFORCED_FURNACE, TexturedModel.ORIENTABLE_ONLY_TOP);
        blockStateModelGenerator.createFurnace(ModBlocks.NETHERITE_FURNACE, TexturedModel.ORIENTABLE_ONLY_TOP);
        blockStateModelGenerator.registerSimpleItemModel(ModBlocks.REINFORCED_FURNACE, ModelLocationUtils.getModelLocation(ModBlocks.REINFORCED_FURNACE));
        blockStateModelGenerator.registerSimpleItemModel(ModBlocks.NETHERITE_FURNACE, ModelLocationUtils.getModelLocation(ModBlocks.NETHERITE_FURNACE));
        blockStateModelGenerator.createFurnace(ModBlocks.ENDERITE_FURNACE, TexturedModel.ORIENTABLE_ONLY_TOP);
        blockStateModelGenerator.registerSimpleItemModel(ModBlocks.ENDERITE_FURNACE, ModelLocationUtils.getModelLocation(ModBlocks.ENDERITE_FURNACE));

        // --- Smokers ---
        blockStateModelGenerator.createFurnace(ModBlocks.REINFORCED_SMOKER, TexturedModel.ORIENTABLE_ONLY_TOP);
        blockStateModelGenerator.createFurnace(ModBlocks.NETHERITE_SMOKER, TexturedModel.ORIENTABLE_ONLY_TOP);
        blockStateModelGenerator.registerSimpleItemModel(ModBlocks.REINFORCED_SMOKER, ModelLocationUtils.getModelLocation(ModBlocks.REINFORCED_SMOKER));
        blockStateModelGenerator.registerSimpleItemModel(ModBlocks.NETHERITE_SMOKER, ModelLocationUtils.getModelLocation(ModBlocks.NETHERITE_SMOKER));
        blockStateModelGenerator.createFurnace(ModBlocks.ENDERITE_SMOKER, TexturedModel.ORIENTABLE_ONLY_TOP);
        blockStateModelGenerator.registerSimpleItemModel(ModBlocks.ENDERITE_SMOKER, ModelLocationUtils.getModelLocation(ModBlocks.ENDERITE_SMOKER));



        // --- 3. Chests ---
        // todo chest:

        // blockStateModelGenerator.registerChest(ModBlocks.REINFORCED_CHEST, ModBlocks.REINFORCED_CHEST, Identifier.of(Simplebuilding.MOD_ID, "entity/chest/reinforced_chest"), false);
        // blockStateModelGenerator.registerChest(ModBlocks.NETHERITE_CHEST, ModBlocks.NETHERITE_CHEST, Identifier.of(Simplebuilding.MOD_ID, "entity/chest/netherite_chest"), false);

        //blockStateModelGenerator.registerParentedItemModel(ModBlocks.REINFORCED_CHEST, ModelIds.getBlockModelId(ModBlocks.REINFORCED_CHEST));
        //blockStateModelGenerator.registerParentedItemModel(ModBlocks.NETHERITE_CHEST, ModelIds.getBlockModelId(ModBlocks.NETHERITE_CHEST));

        // --- 4. Hoppers ---
        registerCustomHopper(blockStateModelGenerator, ModBlocks.REINFORCED_HOPPER);
        registerCustomHopper(blockStateModelGenerator, ModBlocks.NETHERITE_HOPPER);
        registerCustomHopper(blockStateModelGenerator, ModBlocks.ENDERITE_HOPPER);

        // --- 5. Pistons ---
        // Reinforced Piston is a real Piston (has EXTENDED property)
        registerCustomPiston(blockStateModelGenerator, ModBlocks.REINFORCED_PISTON);
        registerStickyPistonVariant(blockStateModelGenerator, ModBlocks.REINFORCED_STICKY_PISTON, ModBlocks.REINFORCED_PISTON);
        registerCustomPiston(blockStateModelGenerator, ModBlocks.NETHERITE_PISTON);
        registerCustomPiston(blockStateModelGenerator, ModBlocks.ENDERITE_PISTON);
        blockStateModelGenerator.createTrivialCube(ModBlocks.NETHERITE_PISTON_HEAD);

        // --- 6. Rucksaecke (abgestellt) ---
        registerBackpack(blockStateModelGenerator, ModBlocks.BACKPACK);
        registerBackpack(blockStateModelGenerator, ModBlocks.REINFORCED_BACKPACK);
        registerBackpack(blockStateModelGenerator, ModBlocks.NETHERITE_BACKPACK);
        registerBackpack(blockStateModelGenerator, ModBlocks.ENDERITE_BACKPACK);
    }

    /** Ein Blockmodell je Stufe aus der Vorlage, gedreht nach HORIZONTAL_FACING (Norden = 0). */
    /** Zwei-Schicht-Vorlagen fuer die offenen gefaerbten Buendel (Vanillas Anzeige-Versatz, dazu layer1). */
    private static final ModelTemplate BUNDLE_OPEN_FRONT_DYED = new ModelTemplate(
            Optional.of(Identifier.withDefaultNamespace("item/template_bundle_open_front")), Optional.empty(),
            TextureSlot.LAYER0, TextureSlot.LAYER1);
    private static final ModelTemplate BUNDLE_OPEN_BACK_DYED = new ModelTemplate(
            Optional.of(Identifier.withDefaultNamespace("item/template_bundle_open_back")), Optional.empty(),
            TextureSlot.LAYER0, TextureSlot.LAYER1);

    /**
     * Buendel der Mod wie Vanillas Buendel: geschlossen das flache Symbol, im Inventar mit
     * ausgewaehltem Eintrag offen (Rueckseite, der ausgewaehlte Gegenstand, Vorderseite). Jede
     * der drei Flaechen gibt es ungefaerbt und - mit {@code minecraft:dyed_color} - als
     * Leder-Ebene mit Farbquelle {@code minecraft:dye} plus ungefaerbter Beschlag-Ebene.
     */
    private static void generateDyeableBundle(ItemModelGenerators generator, Item item) {
        ItemModel.Unbaked closed = dyeable(generator, item, "", ModelTemplates.FLAT_ITEM, ModelTemplates.TWO_LAYERED_ITEM);
        ItemModel.Unbaked back = dyeable(generator, item, "_open_back", ModelTemplates.BUNDLE_OPEN_BACK_INVENTORY, BUNDLE_OPEN_BACK_DYED);
        ItemModel.Unbaked front = dyeable(generator, item, "_open_front", ModelTemplates.BUNDLE_OPEN_FRONT_INVENTORY, BUNDLE_OPEN_FRONT_DYED);
        ItemModel.Unbaked open = ItemModelUtils.composite(back, new BundleSelectedItemSpecialRenderer.Unbaked(), front);
        ItemModel.Unbaked inGui = ItemModelUtils.conditional(new BundleHasSelectedItem(), open, closed);
        generator.itemModelOutput.accept(item, ItemModelUtils.select(new DisplayContext(), closed,
                ItemModelUtils.when(ItemDisplayContext.GUI, inGui)));
    }

    /** Eine Flaeche {@code <id><suffix>}: ungefaerbt, oder mit Farbe aus _dyed (getoent) und _dyed_overlay. */
    private static ItemModel.Unbaked dyeable(ItemModelGenerators generator, Item item, String suffix,
                                             ModelTemplate plainTemplate, ModelTemplate dyedTemplate) {
        Identifier plain = plainTemplate.create(ModelLocationUtils.getModelLocation(item, suffix),
                TextureMapping.layer0(TextureMapping.getItemTexture(item, suffix)), generator.modelOutput);
        Identifier dyed = dyedTemplate.create(ModelLocationUtils.getModelLocation(item, suffix + "_dyed"),
                TextureMapping.layered(TextureMapping.getItemTexture(item, suffix + "_dyed"),
                        TextureMapping.getItemTexture(item, suffix + "_dyed_overlay")),
                generator.modelOutput);
        return ItemModelUtils.conditional(ItemModelUtils.hasComponent(DataComponents.DYED_COLOR),
                ItemModelUtils.tintedModel(dyed, new Dye(DyedStorage.UNDYED)), ItemModelUtils.plainModel(plain));
    }

    /**
     * Faerbbarer Rucksack bzw. faerbbares Buendel: ohne {@code minecraft:dyed_color} das flache
     * Symbol der Stufe, mit Farbe wie Vanillas Lederruestung die Leder-Ebene {@code <id>_dyed}
     * (Farbquelle {@code minecraft:dye}) und darueber die ungefaerbte Beschlag-Ebene
     * {@code <id>_dyed_overlay} (beide aus tools/textures/generate_textures.py).
     */
    private static void generateDyeableItem(ItemModelGenerators generator, Item item) {
        Identifier plain = ModelTemplates.FLAT_ITEM.create(item, TextureMapping.layer0(item), generator.modelOutput);
        Identifier dyed = ModelTemplates.TWO_LAYERED_ITEM.create(ModelLocationUtils.getModelLocation(item, "_dyed"),
                TextureMapping.layered(TextureMapping.getItemTexture(item, "_dyed"), TextureMapping.getItemTexture(item, "_dyed_overlay")),
                generator.modelOutput);
        generator.itemModelOutput.accept(item, ItemModelUtils.conditional(
                ItemModelUtils.hasComponent(DataComponents.DYED_COLOR),
                ItemModelUtils.tintedModel(dyed, new Dye(DyedStorage.UNDYED)),
                ItemModelUtils.plainModel(plain)));
    }

    private void registerBackpack(BlockModelGenerators generator, Block block) {
        TextureMapping textures = new TextureMapping()
                .put(TextureSlot.FRONT, TextureMapping.getBlockTexture(block, "_front"))
                .put(TextureSlot.BACK, TextureMapping.getBlockTexture(block, "_back"))
                .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(block, "_side"))
                .put(TextureSlot.TOP, TextureMapping.getBlockTexture(block, "_top"))
                .put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture(block, "_side"));
        Identifier model = BACKPACK_MODEL.create(block, textures, generator.modelOutput);
        // Gefaerbt (BackpackBlock.DYED): Leder-Ebene (getoent, tintindex 0) und Beschlag-Ebene.
        TextureMapping dyedTextures = new TextureMapping()
                .put(TextureSlot.FRONT, TextureMapping.getBlockTexture(block, "_front_dyed"))
                .put(TextureSlot.BACK, TextureMapping.getBlockTexture(block, "_back_dyed"))
                .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(block, "_side_dyed"))
                .put(TextureSlot.TOP, TextureMapping.getBlockTexture(block, "_top_dyed"))
                .put(FRONT_OVERLAY, TextureMapping.getBlockTexture(block, "_front_dyed_overlay"))
                .put(BACK_OVERLAY, TextureMapping.getBlockTexture(block, "_back_dyed_overlay"))
                .put(SIDE_OVERLAY, TextureMapping.getBlockTexture(block, "_side_dyed_overlay"))
                .put(TOP_OVERLAY, TextureMapping.getBlockTexture(block, "_top_dyed_overlay"))
                .put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture(block, "_side"));
        Identifier dyedModel = BACKPACK_DYED_MODEL.create(block, dyedTextures, generator.modelOutput);
        generator.blockStateOutput.accept(MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(BlockStateProperties.HORIZONTAL_FACING, BackpackBlock.DYED)
                        .generate((facing, dyed) -> {
                            Identifier id = dyed ? dyedModel : model;
                            return switch (facing) {
                                case EAST -> BlockModelGenerators.plainVariant(id).with(BlockModelGenerators.Y_ROT_90);
                                case SOUTH -> BlockModelGenerators.plainVariant(id).with(BlockModelGenerators.Y_ROT_180);
                                case WEST -> BlockModelGenerators.plainVariant(id).with(BlockModelGenerators.Y_ROT_270);
                                default -> BlockModelGenerators.plainVariant(id);
                            };
                        })
                ));
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

    /**
     * Die klebrige Variante eines Kolbens, wie Vanilla sie fuer Blocks.STICKY_PISTON baut: Boden
     * und Seiten sowie das ausgefahrene Basismodell ({@code <base>_base}) kommen vom normalen
     * Kolben, nur die Schubplatte ist die klebrige Textur {@code <base>_top_sticky}.
     */
    private void registerStickyPistonVariant(BlockModelGenerators generator, Block sticky, Block base) {
        TextureMapping textureMap = new TextureMapping()
                .put(TextureSlot.BOTTOM, TextureMapping.getBlockTexture(base, "_bottom"))
                .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(base, "_side"))
                .put(TextureSlot.PLATFORM, TextureMapping.getBlockTexture(base, "_top_sticky"));

        generator.createPistonVariant(sticky, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(base, "_base")), textureMap);
        TextureMapping inventoryMap = new TextureMapping()
                .put(TextureSlot.BOTTOM, TextureMapping.getBlockTexture(base, "_bottom"))
                .put(TextureSlot.TOP, TextureMapping.getBlockTexture(base, "_top_sticky"))
                .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(base, "_side"));

        Identifier inventoryModelId = ModelTemplates.CUBE_BOTTOM_TOP.createWithSuffix(sticky, "_inventory", inventoryMap, generator.modelOutput);

        generator.registerSimpleItemModel(sticky, inventoryModelId);
    }

    @Override
    public void generateItemModels(ItemModelGenerators itemModelGenerator) {

        // --- 1. RANGEFINDER (Generated / Flach) ---
        itemModelGenerator.generateFlatItem(ModItems.OCTANT, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.BLUEPRINT, ModelTemplates.FLAT_ITEM);
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

        // Die sechs Alt-Spatel (LegacySpatulaMigration) waren bisher modelllos, und jeder Start
        // meldete sie mit "No model loaded". Gehalten wie die Meissel, deren Variante sie sind;
        // die Textur ist textures/item/<stufe>_spatula.png.
        itemModelGenerator.generateFlatItem(ModItems.STONE_SPATULA, ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.COPPER_SPATULA, ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.IRON_SPATULA, ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.GOLD_SPATULA, ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.DIAMOND_SPATULA, ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.NETHERITE_SPATULA, ModelTemplates.FLAT_HANDHELD_ITEM);

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
        itemModelGenerator.generateFlatItem(ModItems.ENDER_QUARTZ, ModelTemplates.FLAT_ITEM);
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

        generateDyeableBundle(itemModelGenerator, ModItems.REINFORCED_BUNDLE);
        generateDyeableBundle(itemModelGenerator, ModItems.NETHERITE_BUNDLE);
        itemModelGenerator.generateFlatItem(ModItems.QUIVER, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.REINFORCED_QUIVER, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.NETHERITE_QUIVER, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.LEATHER_SHEET, ModelTemplates.FLAT_ITEM);
        // Layout-Platzhalter der Kreativ-Tabs: zeichnet nichts (minecraft:empty).
        itemModelGenerator.itemModelOutput.accept(ModItems.CREATIVE_SPACER, new net.minecraft.client.renderer.item.EmptyModel.Unbaked());
        itemModelGenerator.generateFlatItem(ModItems.DIAMOND_PEBBLE, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.CRACKED_DIAMOND, ModelTemplates.FLAT_ITEM);

        // Hoppers hier auch, da Generated Item Model für Inventory
        itemModelGenerator.generateFlatItem(ModItems.REINFORCED_HOPPER, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.NETHERITE_HOPPER, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.ENDERITE_HOPPER, ModelTemplates.FLAT_ITEM);

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
                    TextureMapping.layer0(new Material(textureId)),
                    itemModelGenerator.modelOutput
            );
        }

        // Eigene Buecher fuer die Vanilla-Verzauberungen (Auswahl in assets/minecraft/items/enchanted_book.json).
        for (String vanilla : com.simplebuilding.enchantment.VanillaBookTextures.VANILLA) {
            Identifier bookId = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID,
                    com.simplebuilding.enchantment.VanillaBookTextures.modelPath(vanilla));
            ModelTemplates.FLAT_ITEM.create(bookId, TextureMapping.layer0(new Material(bookId)), itemModelGenerator.modelOutput);
        }

        generateDyeableBundle(itemModelGenerator, ModItems.ENDERITE_BUNDLE);
        itemModelGenerator.generateFlatItem(ModItems.ENDERITE_QUIVER, ModelTemplates.FLAT_ITEM);
        // Rucksaecke: flaches Symbol im Inventar (textures/item/<id>.png), nicht das Blockmodell.
        generateDyeableItem(itemModelGenerator, ModItems.BACKPACK);
        generateDyeableItem(itemModelGenerator, ModItems.REINFORCED_BACKPACK);
        generateDyeableItem(itemModelGenerator, ModItems.NETHERITE_BACKPACK);
        generateDyeableItem(itemModelGenerator, ModItems.ENDERITE_BACKPACK);
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

    /** Grundblock und gemeisselte Ziegel als Wuerfel, Ziegel und polierter Block je als Familie, Saeule mit Stirnseite. */
    private void registerEndPalette(BlockModelGenerators generator, ModBlocks.EndPalette palette) {
        if (palette.blockStairs() != null) {
            // Enderquarz: Treppe und Stufe am Grundblock, wie quartz_stairs/quartz_slab
            generator.family(palette.block()).stairs(palette.blockStairs()).slab(palette.blockSlab());
        } else {
            generator.createTrivialCube(palette.block());
        }
        generator.family(palette.bricks()).stairs(palette.brickStairs()).slab(palette.brickSlab()).wall(palette.brickWall());
        generator.family(palette.polished()).stairs(palette.polishedStairs()).slab(palette.polishedSlab()).wall(palette.polishedWall());
        generator.createAxisAlignedPillarBlock(palette.pillar(), TexturedModel.COLUMN_ALT);
        generator.createTrivialCube(palette.chiseled());
    }

    private void registerMirroredChecker(BlockModelGenerators generator, Block block) {
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
        String name = blockId.getPath();

        // Pfad zur normalen Textur: block/blockname
        Identifier normalTexture = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "block/" + name);
        // Pfad zur gespiegelten Textur: block/blockname_mirror
        Identifier mirrorTexture = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "block/" + name + "_mirror");

        // Wir definieren manuell, welche Seite welche Textur bekommt
        Material normalMaterial = new Material(normalTexture);
        Material mirrorMaterial = new Material(mirrorTexture);

        TextureMapping textureMap = new TextureMapping()
                .put(TextureSlot.PARTICLE, normalMaterial)
                .put(TextureSlot.UP, normalMaterial)
                .put(TextureSlot.DOWN, normalMaterial)
                .put(TextureSlot.EAST, normalMaterial)
                .put(TextureSlot.WEST, normalMaterial)
                .put(TextureSlot.NORTH, mirrorMaterial)
                .put(TextureSlot.SOUTH, mirrorMaterial);

        // Modell erstellen (CUBE = voller Würfel mit 6 Seiten-Definitionen)
        Identifier modelId = ModelTemplates.CUBE.create(block, textureMap, generator.modelOutput);

        // WICHTIG: Die ID muss in einen WeightedVariant umgewandelt werden!
        generator.createAxisAlignedPillarBlockCustomModel(block, BlockModelGenerators.plainVariant(modelId));
    }
}
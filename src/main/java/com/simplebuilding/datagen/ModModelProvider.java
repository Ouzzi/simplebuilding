package com.simplebuilding.datagen;

import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.data.models.ItemModelOutput;
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
import net.minecraft.world.level.block.state.properties.PistonType;
import java.util.Optional;


public class ModModelProvider extends FabricModelProvider {

    // --- Custom Model Definitions (da Vanilla Fields fehlen könnten) ---
    // Wir verweisen auf die Vanilla JSON Dateien
    private static final ModelTemplate HOPPER_MODEL = new ModelTemplate(Optional.of(Identifier.withDefaultNamespace("block/hopper")), Optional.empty(), TextureSlot.BOTTOM, TextureSlot.TOP, TextureSlot.SIDE, TextureSlot.INSIDE);
    private static final ModelTemplate HOPPER_SIDE_MODEL = new ModelTemplate(Optional.of(Identifier.withDefaultNamespace("block/hopper_side")), Optional.empty(), TextureSlot.BOTTOM, TextureSlot.TOP, TextureSlot.SIDE, TextureSlot.INSIDE);
    private static final ModelTemplate PISTON_BASE_MODEL = new ModelTemplate(Optional.of(Identifier.withDefaultNamespace("block/piston_base")), Optional.empty(), TextureSlot.BOTTOM, TextureSlot.SIDE, TextureSlot.PLATFORM);
    // Handgeschriebene Vorlagen (assets/simplebuilding/models/block/template_tiered_piston_head*.json):
    // Vanillas template_piston_head(_short), nur die Stange mit eigener Textur (#arm) statt des
    // Plattformrands aus #side.
    private static final TextureSlot ARM = TextureSlot.create("arm");
    private static final ModelTemplate PISTON_HEAD_MODEL = new ModelTemplate(Optional.of(Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "block/template_tiered_piston_head")), Optional.empty(), TextureSlot.PLATFORM, TextureSlot.SIDE, TextureSlot.UNSTICKY, ARM);
    private static final ModelTemplate PISTON_HEAD_SHORT_MODEL = new ModelTemplate(Optional.of(Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "block/template_tiered_piston_head_short")), Optional.empty(), TextureSlot.PLATFORM, TextureSlot.SIDE, TextureSlot.UNSTICKY, ARM);
    // Handgeschriebene Vorlage (assets/simplebuilding/models/block/template_backpack.json): Sack
    // plus Vordertasche, Vorderseite nach Norden; die Stufen setzen nur ihre Texturen ein.
    private static final ModelTemplate BACKPACK_MODEL = new ModelTemplate(Optional.of(Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "block/template_backpack")), Optional.empty(), TextureSlot.FRONT, TextureSlot.BACK, TextureSlot.SIDE, TextureSlot.TOP, TextureSlot.PARTICLE);
    private static final TextureSlot FRONT_OVERLAY = TextureSlot.create("front_overlay");
    private static final TextureSlot BACK_OVERLAY = TextureSlot.create("back_overlay");
    private static final TextureSlot SIDE_OVERLAY = TextureSlot.create("side_overlay");
    private static final TextureSlot TOP_OVERLAY = TextureSlot.create("top_overlay");
    /** Zwei-Ebenen-Vorlage des gefaerbten abgestellten Rucksacks (Ressource, nicht aus Datagen). */
    private static final ModelTemplate BACKPACK_DYED_MODEL = new ModelTemplate(Optional.of(Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "block/template_backpack_dyed")), Optional.of("_dyed"), TextureSlot.FRONT, TextureSlot.BACK, TextureSlot.SIDE, TextureSlot.TOP, FRONT_OVERLAY, BACK_OVERLAY, SIDE_OVERLAY, TOP_OVERLAY, TextureSlot.PARTICLE);

    // Abgestelltes Buendel (PlacedBundleBlock): handgeschriebene Vorlagen aus
    // tools/textures/placed_bundle_textures.py, eine 32x32-Textur je Stufe; gefaerbt zwei Ebenen
    // (#bundle getoent, #overlay ungefaerbt) wie beim Rucksack.
    private static final TextureSlot PLACED_BUNDLE_TEXTURE = TextureSlot.create("bundle");
    private static final TextureSlot PLACED_BUNDLE_OVERLAY = TextureSlot.create("overlay");
    private static final ModelTemplate PLACED_BUNDLE_MODEL = new ModelTemplate(Optional.of(Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "block/template_placed_bundle")), Optional.empty(), PLACED_BUNDLE_TEXTURE, TextureSlot.PARTICLE);
    private static final ModelTemplate PLACED_BUNDLE_DYED_MODEL = new ModelTemplate(Optional.of(Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "block/template_placed_bundle_dyed")), Optional.empty(), PLACED_BUNDLE_TEXTURE, PLACED_BUNDLE_OVERLAY, TextureSlot.PARTICLE);

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
        registerMirroredChecker(blockStateModelGenerator, ModBlocks.POLISHED_ASTRALIT_CHECKER);
        registerMirroredChecker(blockStateModelGenerator, ModBlocks.POLISHED_NIHILITH_CHECKER);
        registerMirroredChecker(blockStateModelGenerator, ModBlocks.POLISHED_ENDER_QUARTZ_CHECKER);

        blockStateModelGenerator.createTrivialCube(ModBlocks.ASTRAL_PURPUR_BLOCK);
        blockStateModelGenerator.createTrivialCube(ModBlocks.NIHIL_PURPUR_BLOCK);
        blockStateModelGenerator.createTrivialCube(ModBlocks.ASTRAL_END_STONE);
        blockStateModelGenerator.createTrivialCube(ModBlocks.NIHIL_END_STONE);

        // Astralit-/Nihilith-Bausatz: Ziegelfamilie (Treppe, Stufe, Mauer samt Inventarmodell),
        // Saeule mit eigener Stirnseite (_top) und gemeisselte Ziegel als einfacher Wuerfel.
        for (ModBlocks.EndPalette palette : ModBlocks.END_PALETTES) {
            registerEndPalette(blockStateModelGenerator, palette);
        }
        // Alternativbloecke (2026-10-03): einfache Wuerfel mit eigener Textur.
        for (ModBlocks.EndAlternates alternates : ModBlocks.END_ALTERNATES) {
            alternates.alternates().forEach(blockStateModelGenerator::createTrivialCube);
        }

        // Schwebender/aufsteigender Sand und Kies sehen aus wie Vanilla-Sand und -Kies: die Modelle zeigen direkt
        // auf minecraft:block/sand bzw. gravel (vorher byte-gleiche Kopien im Mod, Textur-Audit 2026-10-02).
        vanillaCube(blockStateModelGenerator, ModBlocks.SUSPENDED_SAND, net.minecraft.world.level.block.Blocks.SAND);
        vanillaCube(blockStateModelGenerator, ModBlocks.SUSPENDED_GRAVEL, net.minecraft.world.level.block.Blocks.GRAVEL);
        vanillaCube(blockStateModelGenerator, ModBlocks.LEVITATING_SAND, net.minecraft.world.level.block.Blocks.SAND);
        vanillaCube(blockStateModelGenerator, ModBlocks.LEVITATING_GRAVEL, net.minecraft.world.level.block.Blocks.GRAVEL);


        // --- 1. Basic Blocks ---
        // Lautsprecher (2026-10-03): Seiten mit Membran (_side), oben/unten Holz (_top).
        if (ModBlocks.JUKEBOX_AMPLIFIER != null) {
            for (net.minecraft.world.level.block.Block speaker : java.util.List.of(ModBlocks.JUKEBOX_AMPLIFIER, ModBlocks.NOTE_AMPLIFIER)) {
                blockStateModelGenerator.createTrivialBlock(speaker, TexturedModel.COLUMN);
                blockStateModelGenerator.registerSimpleItemModel(speaker, ModelLocationUtils.getModelLocation(speaker));
            }
        }
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
        // Truhen: wie Vanillas Truhen nur ein Partikel-Blockmodell (gezeichnet wird vom
        // TieredChestRenderer), das Item ueber Vanillas Spezialmodell "minecraft:chest" mit der
        // Stufen-Textur aus dem Truhen-Atlas.
        registerTieredChest(blockStateModelGenerator, ModBlocks.REINFORCED_CHEST, ModBlocks.CRACKED_DIAMOND_BLOCK);
        registerTieredChest(blockStateModelGenerator, ModBlocks.NETHERITE_CHEST, net.minecraft.world.level.block.Blocks.NETHERITE_BLOCK);
        registerTieredChest(blockStateModelGenerator, ModBlocks.ENDERITE_CHEST, ModBlocks.ENDERITE_BLOCK);
        if (com.simplebuilding.version.McVersion.TRAPPED_TIERED_CHESTS) {
            registerTieredChest(blockStateModelGenerator, ModBlocks.REINFORCED_TRAPPED_CHEST, ModBlocks.CRACKED_DIAMOND_BLOCK);
            registerTieredChest(blockStateModelGenerator, ModBlocks.NETHERITE_TRAPPED_CHEST, net.minecraft.world.level.block.Blocks.NETHERITE_BLOCK);
            registerTieredChest(blockStateModelGenerator, ModBlocks.ENDERITE_TRAPPED_CHEST, ModBlocks.ENDERITE_BLOCK);
        }

        // Gestufte Shulkerkisten: wie Vanillas Shulkerkisten ein Partikel-Blockmodell (gezeichnet wird
        // vom TieredShulkerBoxRenderer), das Item ueber Vanillas Spezialmodell "minecraft:shulker_box",
        // je nach minecraft:base_color mit der Textur aus Stufe und Farbe (TieredShulkerBoxRenderer#textureId).
        registerTieredShulkerBox(blockStateModelGenerator, ModBlocks.REINFORCED_SHULKER_BOX);
        registerTieredShulkerBox(blockStateModelGenerator, ModBlocks.NETHERITE_SHULKER_BOX);
        registerTieredShulkerBox(blockStateModelGenerator, ModBlocks.ENDERITE_SHULKER_BOX);

        // --- 4. Hoppers ---
        registerCustomHopper(blockStateModelGenerator, ModBlocks.REINFORCED_HOPPER);
        registerCustomHopper(blockStateModelGenerator, ModBlocks.NETHERITE_HOPPER);
        registerCustomHopper(blockStateModelGenerator, ModBlocks.ENDERITE_HOPPER);

        // --- 5. Pistons ---
        // Reinforced Piston is a real Piston (has EXTENDED property)
        registerCustomPiston(blockStateModelGenerator, ModBlocks.REINFORCED_PISTON);
        registerStickyPistonVariant(blockStateModelGenerator, ModBlocks.REINFORCED_STICKY_PISTON, ModBlocks.REINFORCED_PISTON);
        registerWearingPiston(blockStateModelGenerator, ModBlocks.NETHERITE_PISTON);
        registerWearingPiston(blockStateModelGenerator, ModBlocks.ENDERITE_PISTON);
        registerPistonHead(blockStateModelGenerator, ModBlocks.REINFORCED_PISTON_HEAD, ModBlocks.REINFORCED_PISTON, true);
        registerPistonHead(blockStateModelGenerator, ModBlocks.NETHERITE_PISTON_HEAD, ModBlocks.NETHERITE_PISTON, false);
        registerPistonHead(blockStateModelGenerator, ModBlocks.ENDERITE_PISTON_HEAD, ModBlocks.ENDERITE_PISTON, false);

        // --- 6. Rucksaecke (abgestellt) ---
        registerBackpack(blockStateModelGenerator, ModBlocks.BACKPACK);
        registerBackpack(blockStateModelGenerator, ModBlocks.REINFORCED_BACKPACK);
        registerBackpack(blockStateModelGenerator, ModBlocks.NETHERITE_BACKPACK);
        registerBackpack(blockStateModelGenerator, ModBlocks.ENDERITE_BACKPACK);
        // Abgelegte Schmiedevorlage: gezeichnet vom PlacedTemplateRenderer, das Blockmodell traegt
        // nur die Partikeltextur (dunkel wie die Vorlagen selbst).
        blockStateModelGenerator.createParticleOnlyBlock(ModBlocks.PLACED_SMITHING_TEMPLATE, net.minecraft.world.level.block.Blocks.POLISHED_DEEPSLATE);
        // Abgelegte Blaupause: ebenso, Partikel blau wie das Papier.
        blockStateModelGenerator.createParticleOnlyBlock(ModBlocks.PLACED_BLUEPRINT, net.minecraft.world.level.block.Blocks.LAPIS_BLOCK);
        registerPlacedBundle(blockStateModelGenerator);

        // --- 7. Aus Simple Tweaks: Druckplatten und Pads ---
        com.simplebuilding.tweaks.datagen.TweaksModelGen.blocks(blockStateModelGenerator);
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
    /**
     * Rotator (seit 2026-09-28 mit Ladung wie die Amethystlinse): leer (Schaden = Haltbarkeit,
     * normiert 1,0) zeigt {@code item/rotator_empty} - die Enderperle ist erloschen. Auf 26.3
     * ({@code GADGET_REWORK}, Besitzer 2026-10-02) ist {@code item/rotator} eine Ruhe-Animation, und solange
     * ein Klick den anvisierten Block drehen wuerde ({@code simplebuilding:transform_hint}, dieselbe Frage
     * wie der Hand-Hinweis), dreht sich {@code item/rotator_active}. Texturen nur im 26.3-Overlay.
     */
    private static void generateRotator(ItemModelGenerators generator) {
        Item rotator = ModItems.ROTATOR;
        Identifier full = ModelTemplates.FLAT_HANDHELD_ITEM.create(rotator, TextureMapping.layer0(rotator), generator.modelOutput);
        Identifier empty = ModelTemplates.FLAT_HANDHELD_ITEM.create(ModelLocationUtils.getModelLocation(rotator, "_empty"),
                TextureMapping.layer0(TextureMapping.getItemTexture(rotator, "_empty")), generator.modelOutput);
        ItemModel.Unbaked charged = ItemModelUtils.plainModel(full);
        if (com.simplebuilding.version.McVersion.GADGET_REWORK) {
            net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties.ID_MAPPER.put(
                    com.simplebuilding.client.property.TransformHintModelProperty.ID,
                    com.simplebuilding.client.property.TransformHintModelProperty.CODEC);
            Identifier active = ModelTemplates.FLAT_HANDHELD_ITEM.create(ModelLocationUtils.getModelLocation(rotator, "_active"),
                    TextureMapping.layer0(TextureMapping.getItemTexture(rotator, "_active")), generator.modelOutput);
            charged = ItemModelUtils.conditional(new com.simplebuilding.client.property.TransformHintModelProperty(),
                    ItemModelUtils.plainModel(active), charged);
        }
        generator.itemModelOutput.accept(rotator, ItemModelUtils.rangeSelect(
                new net.minecraft.client.renderer.item.properties.numeric.Damage(true),
                charged, ItemModelUtils.override(ItemModelUtils.plainModel(empty), 1.0F)));
    }

    /**
     * Erzdetektor als Kompass (2026-09-28): liegt ein {@code lodestone_tracker} auf dem Stapel (der
     * Server setzt ihn auf das naechste gefundene Erz), zeigt eine Amethyst-Nadel mit Vanillas
     * Kompass-Eigenschaft dorthin - 32 Stellungen wie der Kompass, Bild 16 = oben. Ebene 0 ist das
     * Gehaeuse ({@code item/detector_dial}), Ebene 1 die Nadel ({@code item/detector_needle_NN}),
     * getoent mit der Farbe aus {@code custom_model_data} (heller je naeher, siehe
     * {@code OreDetectorItem.RESONANCE_COLORS}). Ohne Ziel ruht die Nadel ({@code item/detector}).
     * Kein Ausholen der Hand, wenn der Server Nadel oder Haltbarkeit aendert. Der Auswahl-Schimmer eines
     * kalibrierten Detektors laeuft auf der Nadel ({@code client.render.OreDetectorGlint}).
     */
    private static void generateOreDetector(ItemModelGenerators generator) {
        Item detector = ModItems.ORE_DETECTOR;
        Identifier idle = ModelTemplates.FLAT_ITEM.create(detector, TextureMapping.layer0(detector), generator.modelOutput);
        java.util.List<net.minecraft.client.renderer.item.RangeSelectItemModel.Entry> frames = new java.util.ArrayList<>();
        ItemModel.Unbaked north = oreDetectorFrame(generator, 16);
        frames.add(ItemModelUtils.override(north, 0.0F));
        for (int i = 1; i < 32; i++) {
            frames.add(ItemModelUtils.override(oreDetectorFrame(generator, Math.floorMod(i - 16, 32)), i - 0.5F));
        }
        frames.add(ItemModelUtils.override(north, 31.5F));
        ItemModel.Unbaked compass = ItemModelUtils.rangeSelect(
                // Ohne Nachschwingen (2026-10-02): OreDetectorGlint rechnet dasselbe Bild nach und laesst
                // den Auswahl-Schimmer genau auf der sichtbaren Nadel laufen.
                new net.minecraft.client.renderer.item.properties.numeric.CompassAngle(false,
                        net.minecraft.client.renderer.item.properties.numeric.CompassAngleState.CompassTarget.LODESTONE),
                32.0F, frames);
        generator.itemModelOutput.accept(detector, ItemModelUtils.conditional(
                        new net.minecraft.client.renderer.item.properties.conditional.HasComponent(DataComponents.LODESTONE_TRACKER, false),
                        compass, ItemModelUtils.plainModel(idle)),
                new ClientItem.Properties(false, false, 1.0F));
    }

    /** Nadelstellungen der Messuhr: 0 = Ruhe (unten links) bis GAUGE_FRAMES - 1 = Vollausschlag (unten rechts). */
    public static final int GAUGE_FRAMES = 17;

    /**
     * Messuhr (2026-09-29) wie ein Kompass: Ebene 0 das Zifferblatt ({@code item/velocity_gauge_dial}),
     * Ebene 1 die Nadel ({@code item/velocity_gauge_needle_NN}), gewaehlt ueber
     * {@code simplebuilding:gauge_needle} (Tempo des Halters, 0..1, {@code GaugeNeedleModelProperty}).
     * {@code item/velocity_gauge} bleibt als flaches Ruhebild fuer Rezeptanzeigen und das Wiki. Die
     * Eigenschaft muss dafuer schon hier am {@code ID_MAPPER} haengen - die Datagen schreibt sie ueber
     * deren Codec. Kein Ausholen der Hand, wenn die Nadel springt.
     */
    private static void generateGauge(ItemModelGenerators generator) {
        Item gauge = ModItems.VELOCITY_GAUGE;
        net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties.ID_MAPPER.put(
                com.simplebuilding.client.property.GaugeNeedleModelProperty.ID,
                com.simplebuilding.client.property.GaugeNeedleModelProperty.CODEC);
        ModelTemplates.FLAT_ITEM.create(gauge, TextureMapping.layer0(gauge), generator.modelOutput);
        java.util.List<net.minecraft.client.renderer.item.RangeSelectItemModel.Entry> frames = new java.util.ArrayList<>();
        for (int i = 0; i < GAUGE_FRAMES; i++) {
            String suffix = String.format(java.util.Locale.ROOT, "_%02d", i);
            Identifier model = ModelTemplates.TWO_LAYERED_ITEM.create(ModelLocationUtils.getModelLocation(gauge, suffix),
                    TextureMapping.layered(TextureMapping.getItemTexture(gauge, "_dial"),
                            TextureMapping.getItemTexture(gauge, "_needle" + suffix)), generator.modelOutput);
            float threshold = i == 0 ? 0.0F : (i - 0.5F) / (GAUGE_FRAMES - 1);
            frames.add(ItemModelUtils.override(ItemModelUtils.plainModel(model), threshold));
        }
        generator.itemModelOutput.accept(gauge, ItemModelUtils.rangeSelect(
                        new com.simplebuilding.client.property.GaugeNeedleModelProperty(), 1.0F, frames),
                new ClientItem.Properties(false, false, 1.0F));
    }

    private static ItemModel.Unbaked oreDetectorFrame(ItemModelGenerators generator, int index) {
        Item detector = ModItems.ORE_DETECTOR;
        String suffix = String.format(java.util.Locale.ROOT, "_%02d", index);
        Identifier model = ModelTemplates.TWO_LAYERED_ITEM.create(ModelLocationUtils.getModelLocation(detector, suffix),
                TextureMapping.layered(TextureMapping.getItemTexture(detector, "_dial"),
                        TextureMapping.getItemTexture(detector, "_needle" + suffix)), generator.modelOutput);
        return ItemModelUtils.tintedModel(model, new net.minecraft.client.color.item.Constant(-1),
                new net.minecraft.client.color.item.CustomModelDataSource(0,
                        com.simplebuilding.items.custom.OreDetectorItem.RESONANCE_COLORS[0]));
    }

    /**
     * Blaupause mit drei Texturen (Besitzer 2026-09-28): frisch gebaut die normale, bearbeitet eine
     * leicht veraenderte ({@code _edited}), signiert eine deutlich andere ({@code _signed}). Das Modell
     * fragt erst {@code minecraft:has_component} (ohne Komponente: frisch), dann
     * {@code simplebuilding:blueprint_state} ({@code BlueprintItem#modelState}). Die Eigenschaft muss
     * dafuer schon hier am {@code ID_MAPPER} haengen - die Datagen schreibt sie ueber deren Codec.
     */
    private static void generateBlueprint(ItemModelGenerators generator) {
        Item blueprint = ModItems.BLUEPRINT;
        net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties.ID_MAPPER.put(
                com.simplebuilding.client.property.BlueprintStateModelProperty.ID,
                com.simplebuilding.client.property.BlueprintStateModelProperty.PROPERTY_TYPE);
        Identifier plain = ModelTemplates.FLAT_ITEM.create(blueprint, TextureMapping.layer0(blueprint), generator.modelOutput);
        Identifier edited = ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(blueprint, "_edited"),
                TextureMapping.layer0(TextureMapping.getItemTexture(blueprint, "_edited")), generator.modelOutput);
        Identifier signed = ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(blueprint, "_signed"),
                TextureMapping.layer0(TextureMapping.getItemTexture(blueprint, "_signed")), generator.modelOutput);
        ItemModel.Unbaked byState = ItemModelUtils.select(new com.simplebuilding.client.property.BlueprintStateModelProperty(),
                ItemModelUtils.plainModel(plain),
                ItemModelUtils.when(com.simplebuilding.items.custom.BlueprintItem.STATE_EDITED, ItemModelUtils.plainModel(edited)),
                ItemModelUtils.when(com.simplebuilding.items.custom.BlueprintItem.STATE_SIGNED, ItemModelUtils.plainModel(signed)));
        generator.itemModelOutput.accept(blueprint, ItemModelUtils.conditional(
                new net.minecraft.client.renderer.item.properties.conditional.HasComponent(
                        com.simplebuilding.component.ModDataComponentTypes.BLUEPRINT, false),
                byState, ItemModelUtils.plainModel(plain)));
    }

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

    /**
     * Abgestelltes Buendel: je Stufe ein Modell und ein gefaerbtes Zwei-Ebenen-Modell, gedreht nach
     * der Blickrichtung (Vorderseite nach Norden im Modell).
     */
    private void registerPlacedBundle(BlockModelGenerators generator) {
        java.util.Map<com.simplebuilding.blocks.custom.PlacedBundleBlock.Tier, Identifier> plain = new java.util.EnumMap<>(com.simplebuilding.blocks.custom.PlacedBundleBlock.Tier.class);
        java.util.Map<com.simplebuilding.blocks.custom.PlacedBundleBlock.Tier, Identifier> dyed = new java.util.EnumMap<>(com.simplebuilding.blocks.custom.PlacedBundleBlock.Tier.class);
        for (com.simplebuilding.blocks.custom.PlacedBundleBlock.Tier tier : com.simplebuilding.blocks.custom.PlacedBundleBlock.Tier.values()) {
            String base = tier == com.simplebuilding.blocks.custom.PlacedBundleBlock.Tier.BUNDLE ? "placed_bundle" : "placed_" + tier.getSerializedName() + "_bundle";
            Identifier texture = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "block/" + base);
            Identifier dyedTexture = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "block/" + base + "_dyed");
            Identifier overlay = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "block/" + base + "_dyed_overlay");
            plain.put(tier, PLACED_BUNDLE_MODEL.create(texture,
                    new TextureMapping().put(PLACED_BUNDLE_TEXTURE, new Material(texture)).put(TextureSlot.PARTICLE, new Material(texture)), generator.modelOutput));
            dyed.put(tier, PLACED_BUNDLE_DYED_MODEL.create(dyedTexture,
                    new TextureMapping().put(PLACED_BUNDLE_TEXTURE, new Material(dyedTexture)).put(PLACED_BUNDLE_OVERLAY, new Material(overlay))
                            .put(TextureSlot.PARTICLE, new Material(texture)),
                    generator.modelOutput));
        }
        generator.blockStateOutput.accept(MultiVariantGenerator.dispatch(ModBlocks.PLACED_BUNDLE)
                .with(PropertyDispatch.initial(com.simplebuilding.blocks.custom.PlacedBundleBlock.FACING,
                                com.simplebuilding.blocks.custom.PlacedBundleBlock.TIER, com.simplebuilding.blocks.custom.PlacedBundleBlock.DYED)
                        .generate((facing, tier, isDyed) -> {
                            Identifier id = isDyed ? dyed.get(tier) : plain.get(tier);
                            return switch (facing) {
                                case EAST -> BlockModelGenerators.plainVariant(id).with(BlockModelGenerators.Y_ROT_90);
                                case SOUTH -> BlockModelGenerators.plainVariant(id).with(BlockModelGenerators.Y_ROT_180);
                                case WEST -> BlockModelGenerators.plainVariant(id).with(BlockModelGenerators.Y_ROT_270);
                                default -> BlockModelGenerators.plainVariant(id);
                            };
                        })));
    }

    private void registerTieredChest(BlockModelGenerators generator, Block chest, Block particle) {
        generator.createParticleOnlyBlock(chest, particle);
        String texture = ((com.simplebuilding.blocks.custom.TieredChestBlock) chest).textureName();
        generator.itemModelOutput.accept(chest.asItem(), ItemModelUtils.specialModel(Identifier.withDefaultNamespace("item/chest"),
                new net.minecraft.client.renderer.special.ChestSpecialRenderer.Unbaked(
                        Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, texture))));
    }

    private void registerTieredShulkerBox(BlockModelGenerators generator, Block box) {
        generator.createParticleOnlyBlock(box);
        Item item = box.asItem();
        com.simplebuilding.blocks.custom.ChestTier tier = ((com.simplebuilding.blocks.custom.TieredShulkerBoxBlock) box).tier();
        Identifier base = ModelTemplates.SHULKER_BOX_INVENTORY.create(item, TextureMapping.particle(box), generator.modelOutput);
        com.mojang.math.Transformation transformation = net.minecraft.client.renderer.blockentity.ShulkerBoxRenderer.modelTransform(Direction.UP);
        java.util.List<net.minecraft.client.renderer.item.SelectItemModel.SwitchCase<DyeColor>> cases = new java.util.ArrayList<>();
        for (DyeColor color : DyeColor.values()) {
            cases.add(ItemModelUtils.when(color, ItemModelUtils.specialModel(base, transformation,
                    new net.minecraft.client.renderer.special.ShulkerBoxSpecialRenderer.Unbaked(
                            com.simplebuilding.client.render.TieredShulkerBoxRenderer.textureId(tier, color), 0.0F))));
        }
        ItemModel.Unbaked undyed = ItemModelUtils.specialModel(base, transformation,
                new net.minecraft.client.renderer.special.ShulkerBoxSpecialRenderer.Unbaked(
                        com.simplebuilding.client.render.TieredShulkerBoxRenderer.textureId(tier, null), 0.0F));
        generator.itemModelOutput.accept(item, ItemModelUtils.select(
                new net.minecraft.client.renderer.item.properties.select.ComponentContents<>(DataComponents.BASE_COLOR), undyed, cases));
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
     * Der Netheritkolben mit Verschleiss ({@code NetheriteBreakerPistonBlock#WEAR}, 0-7): je zwei
     * Stufen teilen sich ein Modellpaar (eingefahren/ausgefahren), ab Stufe 2, 4 und 6 mit den
     * Seitentexturen {@code <kolben>_side_worn1..3} (immer tiefere Risse), sonst wie
     * {@link #registerCustomPiston}. Das Inventarmodell bleibt das unversehrte.
     */
    private void registerWearingPiston(BlockModelGenerators generator, Block block) {
        net.minecraft.client.data.models.MultiVariant[] retracted = new net.minecraft.client.data.models.MultiVariant[4];
        net.minecraft.client.data.models.MultiVariant[] extended = new net.minecraft.client.data.models.MultiVariant[4];
        for (int stage = 0; stage < 4; stage++) {
            String worn = stage == 0 ? "" : "_worn" + stage;
            TextureMapping textureMap = new TextureMapping()
                    .put(TextureSlot.BOTTOM, TextureMapping.getBlockTexture(block, "_bottom"))
                    .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(block, "_side" + worn))
                    .put(TextureSlot.PLATFORM, TextureMapping.getBlockTexture(block, "_top"))
                    .put(TextureSlot.INSIDE, TextureMapping.getBlockTexture(block, "_inner"));
            extended[stage] = BlockModelGenerators.plainVariant(
                    PISTON_BASE_MODEL.createWithSuffix(block, "_base" + worn, textureMap, generator.modelOutput));
            retracted[stage] = BlockModelGenerators.plainVariant(
                    ModelTemplates.PISTON.createWithSuffix(block, worn, textureMap, generator.modelOutput));
        }
        generator.blockStateOutput.accept(MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(BlockStateProperties.EXTENDED, com.simplebuilding.blocks.custom.NetheriteBreakerPistonBlock.WEAR)
                        .generate((isExtended, wear) -> isExtended ? extended[wear / 2] : retracted[wear / 2]))
                .with(PropertyDispatch.modify(BlockStateProperties.FACING)
                        .select(Direction.DOWN, BlockModelGenerators.X_ROT_90)
                        .select(Direction.UP, BlockModelGenerators.X_ROT_270)
                        .select(Direction.NORTH, BlockModelGenerators.NOP)
                        .select(Direction.SOUTH, BlockModelGenerators.Y_ROT_180)
                        .select(Direction.WEST, BlockModelGenerators.Y_ROT_270)
                        .select(Direction.EAST, BlockModelGenerators.Y_ROT_90)));
        TextureMapping inventoryMap = new TextureMapping()
                .put(TextureSlot.BOTTOM, TextureMapping.getBlockTexture(block, "_bottom"))
                .put(TextureSlot.TOP, TextureMapping.getBlockTexture(block, "_top"))
                .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(block, "_side"));
        Identifier inventoryModelId = ModelTemplates.CUBE_BOTTOM_TOP.createWithSuffix(block, "_inventory", inventoryMap, generator.modelOutput);
        generator.registerSimpleItemModel(block, inventoryModelId);
    }

    /**
     * Der Kopf einer Kolbenstufe, gebaut wie Vanillas {@code createPistonHeads}: lange und kurze
     * Form, je normal und klebrig, gedreht nach FACING. Vorderseite ({@code platform}) ist die
     * Schubplatte des Kolbens ({@code <basis>_top}, klebrig {@code <basis>_top_sticky}), der Rand
     * der Platte kommt aus {@code <basis>_side}; eigene Texturen sind die Rueckseite der Platte
     * ({@code <kopf>.png}) und die Stange ({@code <basis>_arm.png}). Stufen ohne klebrigen Kolben
     * zeigen fuer {@code type=sticky} (nie gesetzt) dieselben Modelle wie fuer {@code type=normal}.
     */
    private void registerPistonHead(BlockModelGenerators generator, Block head, Block base, boolean hasSticky) {
        TextureMapping normal = new TextureMapping()
                .put(TextureSlot.PLATFORM, TextureMapping.getBlockTexture(base, "_top"))
                .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(base, "_side"))
                .put(TextureSlot.UNSTICKY, TextureMapping.getBlockTexture(head))
                .put(ARM, TextureMapping.getBlockTexture(base, "_arm"));
        Identifier normalLong = PISTON_HEAD_MODEL.create(head, normal, generator.modelOutput);
        Identifier normalShort = PISTON_HEAD_SHORT_MODEL.createWithSuffix(head, "_short", normal, generator.modelOutput);
        Identifier stickyLong = normalLong;
        Identifier stickyShort = normalShort;
        if (hasSticky) {
            TextureMapping sticky = normal.copyAndUpdate(TextureSlot.PLATFORM, TextureMapping.getBlockTexture(base, "_top_sticky"));
            stickyLong = PISTON_HEAD_MODEL.createWithSuffix(head, "_sticky", sticky, generator.modelOutput);
            stickyShort = PISTON_HEAD_SHORT_MODEL.createWithSuffix(head, "_short_sticky", sticky, generator.modelOutput);
        }
        generator.blockStateOutput.accept(MultiVariantGenerator.dispatch(head)
                .with(PropertyDispatch.initial(BlockStateProperties.SHORT, BlockStateProperties.PISTON_TYPE)
                        .select(false, PistonType.DEFAULT, BlockModelGenerators.plainVariant(normalLong))
                        .select(false, PistonType.STICKY, BlockModelGenerators.plainVariant(stickyLong))
                        .select(true, PistonType.DEFAULT, BlockModelGenerators.plainVariant(normalShort))
                        .select(true, PistonType.STICKY, BlockModelGenerators.plainVariant(stickyShort)))
                .with(PropertyDispatch.modify(BlockStateProperties.FACING)
                        .select(Direction.DOWN, BlockModelGenerators.X_ROT_90)
                        .select(Direction.UP, BlockModelGenerators.X_ROT_270)
                        .select(Direction.NORTH, BlockModelGenerators.NOP)
                        .select(Direction.SOUTH, BlockModelGenerators.Y_ROT_180)
                        .select(Direction.WEST, BlockModelGenerators.Y_ROT_270)
                        .select(Direction.EAST, BlockModelGenerators.Y_ROT_90)));
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
        generateBlueprint(itemModelGenerator);
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

        // --- ENDERITE ARMOR (TRIM-AWARE) ---
        // Nur die Modelle (unbesetzt + je Material): die Item-Definition schreibt
        // ArmorTrimModelProvider, weil sie zusaetzlich nach dem Besatz-MUSTER waehlt und dieser
        // Auswahl als Rueckfall die Vanilla-Auswahl nach Material mitgibt.
        ItemModelGenerators trimModelsOnly = new ItemModelGenerators(new ItemModelOutput() {
            @Override
            public void accept(Item item, ItemModel.Unbaked model, ClientItem.Properties properties) {
            }

            @Override
            public void copy(Item donor, Item acceptor) {
            }
        }, itemModelGenerator.modelOutput);
        ModelGenCompat.trimmableArmorItem(trimModelsOnly, ModItems.ENDERITE_HELMET, ModArmorMaterials.ENDERITE_ASSET_KEY, ItemModelGenerators.TRIM_PREFIX_HELMET);
        ModelGenCompat.trimmableArmorItem(trimModelsOnly, ModItems.ENDERITE_CHESTPLATE, ModArmorMaterials.ENDERITE_ASSET_KEY, ItemModelGenerators.TRIM_PREFIX_CHESTPLATE);
        ModelGenCompat.trimmableArmorItem(trimModelsOnly, ModItems.ENDERITE_LEGGINGS, ModArmorMaterials.ENDERITE_ASSET_KEY, ItemModelGenerators.TRIM_PREFIX_LEGGINGS);
        ModelGenCompat.trimmableArmorItem(trimModelsOnly, ModItems.ENDERITE_BOOTS, ModArmorMaterials.ENDERITE_ASSET_KEY, ItemModelGenerators.TRIM_PREFIX_BOOTS);

        // --- NEW: ENDERITE MATERIALS (GENERATED) ---
        itemModelGenerator.generateFlatItem(ModItems.ENDERITE_CORE, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.RAW_ENDERITE, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.LAYERED_RAW_ENDERITE, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.ENDERITE_INGOT, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.ENDERITE_SCRAP, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.ENDERITE_HORSE_ARMOR, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.ENDERITE_NAUTILUS_ARMOR, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.NIHILITH_SHARD, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.ASTRALIT_DUST, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.ENDER_QUARTZ, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.ENDERITE_UPGRADE_TEMPLATE, ModelTemplates.FLAT_ITEM);


        // --- CORES & MISC ---
        generateGauge(itemModelGenerator);
        generateOreDetector(itemModelGenerator);
        itemModelGenerator.generateFlatItem(ModItems.MAGNET, ModelTemplates.FLAT_ITEM);
        generateRotator(itemModelGenerator);

        itemModelGenerator.generateFlatItem(ModItems.COPPER_CORE, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.IRON_CORE, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.GOLD_CORE, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.DIAMOND_CORE, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.NETHERITE_CORE, ModelTemplates.FLAT_ITEM);

        generateDyeableBundle(itemModelGenerator, ModItems.REINFORCED_BUNDLE);
        generateDyeableBundle(itemModelGenerator, ModItems.NETHERITE_BUNDLE);
        generateDyeableItem(itemModelGenerator, ModItems.QUIVER);
        generateDyeableItem(itemModelGenerator, ModItems.REINFORCED_QUIVER);
        generateDyeableItem(itemModelGenerator, ModItems.NETHERITE_QUIVER);
        itemModelGenerator.generateFlatItem(ModItems.LEATHER_SHEET, ModelTemplates.FLAT_ITEM);
        // Handbuecher (GuideBooks): flache Item-Modelle, je Buch eine eigene Textur.
        for (com.simplebuilding.guide.GuideBooks.Book book : com.simplebuilding.guide.GuideBooks.Book.values()) {
            if (!com.simplebuilding.version.McVersion.MEGA_GUIDES || book.isHub()) itemModelGenerator.generateFlatItem(com.simplebuilding.guide.GuideBooks.item(book), ModelTemplates.FLAT_ITEM);
        }
        // Layout-Platzhalter der Kreativ-Tabs: zeichnet nichts (minecraft:empty).
        itemModelGenerator.itemModelOutput.accept(ModItems.CREATIVE_SPACER, new net.minecraft.client.renderer.item.EmptyModel.Unbaked());
        itemModelGenerator.generateFlatItem(ModItems.DIAMOND_PEBBLE, ModelTemplates.FLAT_ITEM);
        if (com.simplebuilding.version.McVersion.SMALL_PLACEABLES) {
            // Splitter-Kleinteile (2026-10-05); Steinkiesel/Feuersteinsplitter haben handgeschriebene Modelle im Overlay.
            for (net.minecraft.world.item.Item chip : java.util.List.of(ModItems.FIRE_CHIP, ModItems.ICE_CHIP, ModItems.OBSIDIAN_CHIP)) {
                itemModelGenerator.generateFlatItem(chip, ModelTemplates.FLAT_ITEM);
            }
        }
        itemModelGenerator.generateFlatItem(ModItems.CRACKED_DIAMOND, ModelTemplates.FLAT_ITEM);

        // Hoppers hier auch, da Generated Item Model für Inventory
        itemModelGenerator.generateFlatItem(ModItems.REINFORCED_HOPPER, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.NETHERITE_HOPPER, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.ENDERITE_HOPPER, ModelTemplates.FLAT_ITEM);

        itemModelGenerator.generateFlatItem(ModItems.GLOWING_TRIM_TEMPLATE, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.EMITTING_TRIM_TEMPLATE, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.PULSATING_TRIM_TEMPLATE, ModelTemplates.FLAT_ITEM);

        itemModelGenerator.generateFlatItem(ModItems.BASIC_UPGRADE_TEMPLATE, ModelTemplates.FLAT_ITEM);

        itemModelGenerator.generateFlatItem(ModItems.NETHERITE_NUGGET, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.ENDERITE_NUGGET, ModelTemplates.FLAT_ITEM);
        if (ModItems.REINFORCED_SHULKER_SHELL != null) {
            itemModelGenerator.generateFlatItem(ModItems.REINFORCED_SHULKER_SHELL, ModelTemplates.FLAT_ITEM);
            itemModelGenerator.generateFlatItem(ModItems.NETHERITE_SHULKER_SHELL, ModelTemplates.FLAT_ITEM);
            itemModelGenerator.generateFlatItem(ModItems.ENDERITE_SHULKER_SHELL, ModelTemplates.FLAT_ITEM);
        }
        for (Item disc : com.simplebuilding.util.MusicDiscs.items()) {
            itemModelGenerator.generateFlatItem(disc, ModelTemplates.FLAT_ITEM);
        }
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
        generateDyeableItem(itemModelGenerator, ModItems.ENDERITE_QUIVER);
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

        // Aus Simple Tweaks: Spawn-Elytra, Laserpointer, Echo-Kompass
        com.simplebuilding.tweaks.datagen.TweaksModelGen.items(itemModelGenerator);
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

    /** Wuerfel mit der Textur eines Vanilla-Blocks (cube_all auf minecraft:block/<vanilla>). */
    private static void vanillaCube(BlockModelGenerators generator, Block block, Block texture) {
        Identifier model = ModelTemplates.CUBE_ALL.create(block, TextureMapping.cube(texture), generator.modelOutput);
        generator.blockStateOutput.accept(MultiVariantGenerator.dispatch(block, BlockModelGenerators.plainVariant(model)));
    }
}
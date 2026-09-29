package com.simplebuilding.datagen;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.tweaks.block.TweaksBlocks;
import com.simplebuilding.tweaks.item.TweaksItems;
import com.simplebuilding.recipe.BackpackUpgradeRecipe;
import com.simplebuilding.recipe.CountBasedSmithingRecipe;
import com.simplebuilding.recipe.ReinforcedBundleRecipe;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
// MC 26.2: Kriterien-Trigger wurden von net.minecraft.advancements.criterion nach
// net.minecraft.advancements.triggers verschoben (Criterion liegt jetzt ebenfalls dort).
import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.RecipeUnlockedTrigger;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.CustomCraftingRecipeBuilder;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.world.item.crafting.DyeRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeUnlockAdvancementBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SmithingTransformRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

// Extends RecipeProviderCompat (26.2: src/mc26_2/java, 26.3: mc26_3/fabric/src/main/java) so that
// this file compiles unchanged against both Minecraft lines.
public class ModRecipeProvider extends RecipeProviderCompat {

    // WICHTIG: Diesen Tag manuell definieren, da er in 1.21.2+ Code fehlt
    private static final TagKey<Item> TRIM_TEMPLATES = TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("trim_templates"));

    public ModRecipeProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected RecipeProvider createGenerator(Object first, Object second) {
        return new Generator(first, second) {
            @Override
            public void buildRecipes() {

                // ---------------------------------------------------------
                // WICHTIG: Registry Zugriff für Tags vorbereiten (für 1.21.2+)
                // ---------------------------------------------------------
                HolderGetter<Item> itemRegistry = items();

                // =================================================================
                // FIX: DUMMY REZEPT FÜR SCHMIEDETISCH (Glowing Ink)
                // =================================================================
                // Wir erstellen Ingredients über die Registry (ofTag statt fromTag)
                Ingredient armorIngredient = Ingredient.of(itemRegistry.getOrThrow(ItemTags.TRIMMABLE_ARMOR));

                SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(ModItems.GLOWING_TRIM_TEMPLATE), // Slot 1: nur die eigene Vorlage (sonst passt Glowing + Glowstone aufs Emitting-Rezept)
                        armorIngredient,                        // Slot 2: Rüstung
                        Ingredient.of(Items.GLOW_INK_SAC), // Slot 3: Leuchttinte
                        RecipeCategory.MISC,
                        ModItems.GLOWING_TRIM_TEMPLATE          // Dummy Output (wird vom Mixin überschrieben)
                )
                .unlocks("has_glowing_template", has(ModItems.GLOWING_TRIM_TEMPLATE))
                .save(output, "glowing_armor_upgrade_dummy");

                SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(ModItems.EMITTING_TRIM_TEMPLATE), // Slot 1: nur die eigene Vorlage
                        armorIngredient,                        // Slot 2: Rüstung
                        Ingredient.of(Items.GLOWSTONE_DUST), // Slot 3: Leuchttinte
                        RecipeCategory.MISC,
                        ModItems.EMITTING_TRIM_TEMPLATE          // Dummy Output (wird vom Mixin überschrieben)
                )
                .unlocks("has_emitting_template", has(ModItems.EMITTING_TRIM_TEMPLATE))
                .save(output, "emitting_armor_upgrade_dummy");

                // Pulsating Armor Trim (Besitzer 2026-09-28): Vorlage + Ruestung + Echoscherbe; das Ergebnis
                // (Ruestung mit pulsierendem Besatz) setzt SmithingScreenHandlerMixin / TrimUpgrades.
                SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(ModItems.PULSATING_TRIM_TEMPLATE),
                        armorIngredient,
                        Ingredient.of(Items.ECHO_SHARD),
                        RecipeCategory.MISC,
                        ModItems.PULSATING_TRIM_TEMPLATE          // Dummy Output (wird vom Mixin überschrieben)
                )
                .unlocks("has_pulsating_template", has(ModItems.PULSATING_TRIM_TEMPLATE))
                .save(output, "pulsating_armor_upgrade_dummy");

                // Die Vorlage selbst: Echoscherbe + beliebiger Vorschlaghammer an der Werkbank. Der Hammer
                // bleibt im Raster und verliert Haltbarkeit (ShapelessRecipeMixin / SledgehammerCrafting).
                shapeless(RecipeCategory.MISC, ModItems.PULSATING_TRIM_TEMPLATE)
                        .requires(Ingredient.of(itemRegistry.getOrThrow(com.simplebuilding.util.ModTags.Items.SLEDGEHAMMER_ENCHANTABLE)))
                        .requires(Items.ECHO_SHARD)
                        .unlockedBy(getHasName(Items.ECHO_SHARD), has(Items.ECHO_SHARD))
                        .save(output);


                // =================================================================
                // CHISELS (Stick + Material + Nugget/Shard) - DIAGONAL
                // =================================================================
                createChiselRecipe(ModItems.STONE_CHISEL, Items.COBBLESTONE, Items.COPPER_NUGGET);
                createChiselRecipe(ModItems.COPPER_CHISEL, Items.COPPER_INGOT, Items.COPPER_NUGGET);
                createChiselRecipe(ModItems.IRON_CHISEL, Items.IRON_INGOT, Items.COPPER_NUGGET);
                createChiselRecipe(ModItems.GOLD_CHISEL, Items.GOLD_INGOT, Items.COPPER_NUGGET);
                createChiselRecipe(ModItems.DIAMOND_CHISEL, Items.DIAMOND, Items.COPPER_NUGGET);
                createSmithing(ModItems.DIAMOND_CHISEL, ModItems.NETHERITE_CHISEL, RecipeCategory.TOOLS);


                // =================================================================
                // BUILDING CORES (Material + Nether Star)
                // =================================================================
                createCoreRecipe(ModItems.COPPER_CORE, Items.COPPER_INGOT);
                createCoreRecipe(ModItems.IRON_CORE, Items.IRON_INGOT);
                createCoreRecipe(ModItems.GOLD_CORE, Items.GOLD_INGOT);
                createCoreRecipe(ModItems.DIAMOND_CORE, Items.DIAMOND);

                SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                        Ingredient.of(ModItems.DIAMOND_CORE),
                        Ingredient.of(Items.NETHERITE_INGOT),
                        RecipeCategory.MISC,
                        ModItems.NETHERITE_CORE
                ).unlocks("has_netherite_ingot", has(Items.NETHERITE_INGOT))
                 .save(output, getItemName(ModItems.NETHERITE_CORE) + "_smithing");


                // =================================================================
                // BUILDING WANDS (Stick + Core + Material)
                // =================================================================
                createWandRecipe(ModItems.COPPER_BUILDING_WAND, ModItems.COPPER_CORE, Items.COPPER_INGOT);
                createWandRecipe(ModItems.IRON_BUILDING_WAND, ModItems.IRON_CORE, Items.IRON_INGOT);
                createWandRecipe(ModItems.GOLD_BUILDING_WAND, ModItems.GOLD_CORE, Items.GOLD_INGOT);
                createWandRecipe(ModItems.DIAMOND_BUILDING_WAND, ModItems.DIAMOND_CORE, Items.DIAMOND);
                createSmithing(ModItems.DIAMOND_BUILDING_WAND, ModItems.NETHERITE_BUILDING_WAND, RecipeCategory.TOOLS);

                // =================================================================
                // SLEDGEHAMMER
                // =================================================================
                createSledgehammerRecipe(ModItems.STONE_SLEDGEHAMMER, Items.COBBLESTONE, Items.IRON_INGOT);
                // MC 26.2: Items.COPPER_BLOCK ist jetzt eine WeatheringCopperCollection<Item>.
                // weathering().unaffected() liefert das unverwitterte, ungewachste minecraft:copper_block
                // (identisch zum frueheren Items.COPPER_BLOCK) - so macht es auch der VanillaRecipeProvider.
                createSledgehammerRecipe(ModItems.COPPER_SLEDGEHAMMER, Items.COPPER_INGOT, Items.COPPER_BLOCK.weathering().unaffected());
                createSledgehammerRecipe(ModItems.IRON_SLEDGEHAMMER, Items.IRON_INGOT, Items.IRON_BLOCK);
                createSledgehammerRecipe(ModItems.GOLD_SLEDGEHAMMER, Items.GOLD_INGOT, Items.GOLD_BLOCK);
                createSledgehammerRecipe(ModItems.DIAMOND_SLEDGEHAMMER, Items.DIAMOND, Items.DIAMOND_BLOCK);
                createSmithing(ModItems.DIAMOND_SLEDGEHAMMER, ModItems.NETHERITE_SLEDGEHAMMER, RecipeCategory.TOOLS);


                // =================================================================
                // RANGEFINDER (Antik / Octant Style)
                // =================================================================
                // Besitzer 2026-09-25: Goldbarren -> leichte Waegeplatten (Gold), Goldnugget ->
                // Blitzableiter (gleiche Felder), Kupferbarren unten -> schwere Waegeplatte (Eisen).
                // Besitzer 2026-09-29: statt der Waegeplatten Goldnuggets (leichte) und ein Goldkern (schwere).
                // Besitzer 2026-09-29 (Lauf HH): Goldnuggets auch an Stelle der beiden Blitzableiter, die Leine
                // nach unten rechts, oben rechts ein einzelner Blitzableiter; der Goldkern bleibt unten links.
                shaped(RecipeCategory.TOOLS, ModItems.OCTANT)
                        .pattern(" NR")
                        .pattern("NCN")
                        .pattern("GNL")
                        .define('N', Items.GOLD_NUGGET)
                        .define('R', Items.LIGHTNING_ROD.weathering().unaffected())
                        .define('G', ModItems.GOLD_CORE)
                        .define('C', Items.COMPASS)
                        .define('L', Items.LEAD)
                        .unlockedBy(getHasName(Items.COMPASS), has(Items.COMPASS))
                        .save(output);
                for (DyeColor color : DyeColor.values()) {
                    Item resultItem = ModItems.COLORED_OCTANT_ITEMS.get(color);
                    Item dyeItem = getDyeItem(color);

                    if (resultItem != null && dyeItem != null) {

                        ShapelessRecipeBuilder.shapeless(items(), RecipeCategory.TOOLS, resultItem)
                                .requires(ModItems.OCTANT)
                                .requires(dyeItem)
                                .unlockedBy(getHasName(dyeItem), has(dyeItem))
                                .save(output, getSimpleRecipeName(resultItem) + "_from_dye");
                    }
                }

                // =================================================================
                // GEFAERBTE RUCKSAECKE, BUENDEL UND KOECHER: Vanillas Farbstoff-Rezept wie fuer
                // Lederruestung (crafting_dye, <item>_dyed) - Item + ein oder mehrere Farbstoffe,
                // Farben mischen sich, alle anderen Komponenten (Inhalt, Name, Verzauberungen)
                // bleiben. Gewaschen wird im Kessel (Tag cauldron_can_remove_dye).
                // =================================================================
                // Vanillas dyedItem(...) speichert unter minecraft:<id>_dyed; hier dasselbe Rezept
                // unter simplebuilding:<id>_dyed.
                for (Item backpack : new Item[]{ModItems.BACKPACK, ModItems.REINFORCED_BACKPACK,
                        ModItems.NETHERITE_BACKPACK, ModItems.ENDERITE_BACKPACK}) {
                    modDyedItem(backpack, "dyed_backpack");
                }
                for (Item bundle : new Item[]{ModItems.REINFORCED_BUNDLE, ModItems.NETHERITE_BUNDLE, ModItems.ENDERITE_BUNDLE}) {
                    modDyedItem(bundle, "dyed_bundle");
                }
                for (Item quiver : new Item[]{ModItems.QUIVER, ModItems.REINFORCED_QUIVER, ModItems.NETHERITE_QUIVER,
                        ModItems.ENDERITE_QUIVER}) {
                    modDyedItem(quiver, "dyed_quiver");
                }

                // =================================================================
                // VELOCITY_GAUGE
                // =================================================================
                // Besitzer 2026-09-29 (Lauf HH): wie der Detector um 45 Grad gedreht - Amethystscherbe oben
                // rechts (vorher oben Mitte), Kupfer-Baukern unten links (vorher unten Mitte), Kupfernuggets
                // oben, unten, links und rechts neben dem Kompass; oben links und unten rechts frei, kein Quarz.
                shaped(RecipeCategory.TOOLS, ModItems.VELOCITY_GAUGE)
                        .pattern(" NA")
                        .pattern("NCN")
                        .pattern("KN ")
                        .define('C', Items.COMPASS)
                        .define('A', Items.AMETHYST_SHARD)
                        .define('N', Items.COPPER_NUGGET)
                        .define('K', ModItems.COPPER_CORE)
                        .unlockedBy(getHasName(Items.COMPASS), has(Items.COMPASS))
                        .save(output);

                // =================================================================
                // MAGNET
                // =================================================================
                // Besitzer 2026-09-28: Eisen-Baukern unten links, Eisenbarren unten Mitte und links Mitte,
                // Redstone oben Mitte, Lapislazuli rechts Mitte (vorher unten rechts); kein Leitstein.
                shaped(RecipeCategory.TOOLS, ModItems.MAGNET)
                        .pattern(" R ")
                        .pattern("I L")
                        .pattern("CI ")
                        .define('R', Items.REDSTONE)
                        .define('I', Items.IRON_INGOT)
                        .define('C', ModItems.IRON_CORE)
                        .define('L', Items.LAPIS_LAZULI)
                        .unlockedBy(getHasName(ModItems.IRON_CORE), has(ModItems.IRON_CORE))
                        .save(output);

                // Rotator (Besitzer 2026-09-27): Eisen-Baukern unten links, vier Eisenbarren im Kreuz, Enderperle in der Mitte
                shaped(RecipeCategory.TOOLS, ModItems.ROTATOR)
                        .pattern(" I ")
                        .pattern("IPI")
                        .pattern("CI ")
                        .define('I', Items.IRON_INGOT)
                        .define('P', Items.ENDER_PEARL)
                        .define('C', ModItems.IRON_CORE)
                        .unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT))
                        .save(output);

                // =================================================================
                // LEATHER SHEET (neun Leder, kein Rueckweg)
                // =================================================================
                shaped(RecipeCategory.MISC, ModItems.LEATHER_SHEET)
                        .pattern("LLL")
                        .pattern("LLL")
                        .pattern("LLL")
                        .define('L', Items.LEATHER)
                        .unlockedBy(getHasName(Items.LEATHER), has(Items.LEATHER))
                        .save(output);

                // =================================================================
                // REINFORCED BUNDLE
                // =================================================================
                // Faden oben, Buendel in der Mitte, Lederplatte unten und sechs Diamantkiesel in den
                // beiden Seitenspalten (Besitzer 2026-09-29: sechs statt einem). Als Aufwertung behaelt
                // das Ergebnis Inhalt, Verzauberungen und Namen des Vanilla-Buendels (ReinforcedBundleRecipe).
                createContainerUpgrade(ModItems.REINFORCED_BUNDLE, Items.BUNDLE,
                        Map.of('S', Items.STRING, 'D', ModItems.DIAMOND_PEBBLE, 'B', Items.BUNDLE, 'X', ModItems.LEATHER_SHEET),
                        "DSD",
                        "DBD",
                        "DXD");


                createSmithing(ModItems.REINFORCED_BUNDLE, ModItems.NETHERITE_BUNDLE, RecipeCategory.TOOLS);


                // =================================================================
                // QUIVER
                // =================================================================
                shaped(RecipeCategory.TOOLS, ModItems.QUIVER)
                        .pattern(" SL")
                        .pattern("SLN")
                        .pattern("B  ")
                        .define('S', Items.STRING)
                        .define('L', Items.LEATHER)
                        .define('N', Items.COPPER_NUGGET)
                        .define('B', Items.BUNDLE)
                        .unlockedBy(getHasName(Items.BUNDLE), has(Items.BUNDLE))
                        .save(output);

                // Dasselbe Muster wie das verstaerkte Buendel (Besitzer 2026-09-29): der Koecher an der
                // Stelle des Buendels, Faden oben, Lederplatte unten, sechs Diamantkiesel an den Seiten.
                // Behaelt Pfeile, Verzauberungen und Namen.
                createContainerUpgrade(ModItems.REINFORCED_QUIVER, ModItems.QUIVER,
                        Map.of('S', Items.STRING, 'D', ModItems.DIAMOND_PEBBLE, 'X', ModItems.LEATHER_SHEET,
                                'Q', ModItems.QUIVER),
                        "DSD",
                        "DQD",
                        "DXD");

                // Keine Stufe ueberspringen - wie bei den Buendeln wird nur der verstaerkte Koecher
                // zum Netherit-Koecher.
                createSmithing(ModItems.REINFORCED_QUIVER, ModItems.NETHERITE_QUIVER, RecipeCategory.TOOLS);

                // =================================================================
                // RUCKSAECKE (die Lederplatte steht oben bei den Buendeln)
                // =================================================================
                shaped(RecipeCategory.TOOLS, ModItems.BACKPACK)
                        .pattern("NSN")
                        .pattern("PPP")
                        .pattern("WWW")
                        .define('N', Items.COPPER_NUGGET)
                        .define('S', Items.STRING)
                        .define('P', ModItems.LEATHER_SHEET)
                        // Schwere Waegeplatten (Eisen) als steifer Boden
                        .define('W', Items.HEAVY_WEIGHTED_PRESSURE_PLATE)
                        .unlockedBy(getHasName(ModItems.LEATHER_SHEET), has(ModItems.LEATHER_SHEET))
                        .save(output);

                // Verstaerkter Rucksack: eigener Rezepttyp, der den Rucksack aus dem Raster mit
                // Inhalt, Verzauberungen und Name uebernimmt (ein crafting_shaped wuerde ihn leeren).
                ShapedRecipePattern reinforcedPattern = ShapedRecipePattern.of(java.util.Map.of(
                                'S', Ingredient.of(Items.STRING),
                                'D', Ingredient.of(ModItems.DIAMOND_PEBBLE),
                                'L', Ingredient.of(ModItems.LEATHER_SHEET),
                                'B', Ingredient.of(ModItems.BACKPACK)),
                        // Vier Diamantkiesel (Besitzer 2026-09-29): neben dem Faden und neben dem Rucksack.
                        "DSD",
                        "DBD",
                        "LLL");
                ResourceKey<Recipe<?>> reinforcedId = ResourceKey.create(Registries.RECIPE,
                        Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "reinforced_backpack"));
                RecipeUnlockAdvancementBuilder reinforcedUnlock = new RecipeUnlockAdvancementBuilder();
                reinforcedUnlock.unlockedBy(getHasName(ModItems.BACKPACK), has(ModItems.BACKPACK));
                output.accept(reinforcedId,
                        new BackpackUpgradeRecipe(
                                RecipeBuilder.createCraftingCommonInfo(true),
                                RecipeBuilder.createCraftingBookInfo(RecipeCategory.TOOLS, null),
                                reinforcedPattern,
                                new ItemStackTemplate(itemRegistry.getOrThrow(BuiltInRegistries.ITEM.getResourceKey(ModItems.REINFORCED_BACKPACK).orElseThrow()),
                                        1, DataComponentPatch.EMPTY)),
                        reinforcedUnlock.build(output, reinforcedId, RecipeCategory.TOOLS));

                // Netherit und Enderit am Schmiedetisch; smithing_transform behaelt alle Komponenten.
                createSmithing(ModItems.REINFORCED_BACKPACK, ModItems.NETHERITE_BACKPACK, RecipeCategory.TOOLS);
                createSmithingTransform(output, ModItems.ENDERITE_UPGRADE_TEMPLATE, ModItems.NETHERITE_BACKPACK, ModItems.ENDERITE_INGOT, RecipeCategory.TOOLS, ModItems.ENDERITE_BACKPACK);


                // =================================================================
                // DETECTOR
                // =================================================================
                // Besitzer 2026-09-29 (Lauf HH): das alte Rezept um 45 Grad gedreht und zwei Echoscherben
                // weniger - Kompass mittig, Echoscherben oben/unten/links/rechts, Goldkern unten links,
                // kalibrierter Sculk-Sensor oben rechts, die beiden uebrigen Ecken frei.
                shaped(RecipeCategory.TOOLS, ModItems.ORE_DETECTOR)
                        .pattern(" ES")
                        .pattern("ECE")
                        .pattern("GE ")
                        .define('E', Items.ECHO_SHARD)
                        .define('C', Items.COMPASS)
                        .define('G', ModItems.GOLD_CORE)
                        .define('S', Items.CALIBRATED_SCULK_SENSOR)
                        .unlockedBy(getHasName(Items.COMPASS), has(Items.COMPASS))
                        .save(output);

                // Aus Simple Tweaks: Druckplatten, Pads, Teleporter, Echo-Kompass
                buildTweaksRecipes();


                shaped(RecipeCategory.MISC, ModItems.CRACKED_DIAMOND)
                        .pattern("PPP")
                        .pattern("PPP")
                        .pattern("PPP")
                        .define('P', ModItems.DIAMOND_PEBBLE)
                        .unlockedBy(getHasName(ModItems.DIAMOND_PEBBLE), has(ModItems.DIAMOND_PEBBLE))
                        .save(output);

                oreBlasting(java.util.List.of(ModItems.CRACKED_DIAMOND), RecipeCategory.MISC, net.minecraft.world.item.crafting.CookingBookCategory.MISC, Items.DIAMOND, 1.0f, fastMachineTicks(100), "diamond_from_cracked");


                // Construction light recipe - lapis light
                shaped(RecipeCategory.MISC, ModItems.CONSTRUCTION_LIGHT)
                        .pattern("LGL")
                        .pattern("GTG")
                        .pattern("LGL")
                        .define('G', Items.GLASS)
                        .define('L', Items.LAPIS_LAZULI)
                        .define('T', Items.TORCH)
                        .unlockedBy(getHasName(Items.LAPIS_LAZULI), has(Items.LAPIS_LAZULI))
                        .save(output);


                shaped(RecipeCategory.BUILDING_BLOCKS, ModItems.CRACKED_DIAMOND_BLOCK)
                        .pattern("CCC")
                        .pattern("CCC")
                        .pattern("CCC")
                        .define('C', ModItems.CRACKED_DIAMOND)
                        .unlockedBy(getHasName(ModItems.CRACKED_DIAMOND), has(ModItems.CRACKED_DIAMOND)).save(output);
                oneToOneConversionRecipe(ModItems.CRACKED_DIAMOND, ModItems.CRACKED_DIAMOND_BLOCK, "cracked_diamond_from_block", 9);


                // =================================================================
                // HOPPER REINFORCED & NETHERITE
                // =================================================================
                // 1. Reinforced Hopper
                ShapedRecipeBuilder.shaped(items(), RecipeCategory.REDSTONE, ModItems.REINFORCED_HOPPER, 5)
                        .pattern("HNH")
                        .pattern("DDD")
                        .pattern("HHH")
                        .define('D', ModItems.CRACKED_DIAMOND)
                        .define('H', Items.HOPPER)
                        .define('N', Items.NAME_TAG)
                        .unlockedBy(getHasName(Items.HOPPER), has(Items.HOPPER))
                        .save(output, "reinforced_hopper_from_crafting");

                // Verstaerkte Truhe aus drei Kupfertruhen (jede Oxidationsstufe, gewachst oder nicht), wie
                // die verstaerkten Oefen. Mit Inhalt geht es in der Welt: Vorschlaghammer plus Rissiger
                // Diamant in der Nebenhand (SledgehammerUpgrades). Netherit- und Enderittruhe entstehen
                // nur so, wie die Maschinen.
                ShapedRecipeBuilder.shaped(items(), RecipeCategory.DECORATIONS, ModItems.REINFORCED_CHEST, 3)
                        .pattern("DDD")
                        .pattern("CCC")
                        .pattern("DDD")
                        .define('D', ModItems.CRACKED_DIAMOND)
                        .define('C', tag(com.simplebuilding.util.ModTags.Items.COPPER_CHESTS))
                        .unlockedBy(getHasName(ModItems.CRACKED_DIAMOND), has(ModItems.CRACKED_DIAMOND))
                        .save(output);

                // Netherit- und Enderit-Trichter (wie alle Netherit- und Enderit-Maschinen) haben kein
                // Werkbankrezept mehr: sie entstehen in der Welt, per Vorschlaghammer und Nugget
                // (SledgehammerUpgrades).


                // =================================================================
                // PISTON REINFORCED & NETHERITE
                // =================================================================
                ShapedRecipeBuilder.shaped(items(), RecipeCategory.REDSTONE, ModItems.REINFORCED_PISTON, 2)
                        .pattern("DDD")
                        .pattern("PIP")
                        .pattern("III")
                        .define('D', ModItems.CRACKED_DIAMOND)
                        .define('P', Items.PISTON)
                        .define('I', Items.IRON_INGOT)
                        .unlockedBy(getHasName(Items.PISTON), has(Items.PISTON))
                        .save(output);
                // Klebrig wie bei Vanilla: Schleimball ueber dem Kolben.
                ShapedRecipeBuilder.shaped(items(), RecipeCategory.REDSTONE, ModItems.REINFORCED_STICKY_PISTON)
                        .pattern("S")
                        .pattern("P")
                        .define('S', Items.SLIME_BALL)
                        .define('P', ModItems.REINFORCED_PISTON)
                        .unlockedBy(getHasName(ModItems.REINFORCED_PISTON), has(ModItems.REINFORCED_PISTON))
                        .save(output);


                // =================================================================
                // BLAST FURNACE REINFORCED & NETHERITE
                // =================================================================
                ShapedRecipeBuilder.shaped(items(), RecipeCategory.REDSTONE, ModItems.REINFORCED_BLAST_FURNACE, 3)
                        .pattern("DDD")
                        .pattern("BBB")
                        .pattern("DDD")
                        .define('D', ModItems.CRACKED_DIAMOND)
                        .define('B', Items.BLAST_FURNACE)
                        .unlockedBy(getHasName(Items.BLAST_FURNACE), has(Items.BLAST_FURNACE))
                        .save(output);


                // =================================================================
                // FURNACE REINFORCED & NETHERITE
                // =================================================================
                ShapedRecipeBuilder.shaped(items(), RecipeCategory.REDSTONE, ModItems.REINFORCED_FURNACE, 3)
                        .pattern("DDD")
                        .pattern("FFF")
                        .pattern("DDD")
                        .define('D', ModItems.CRACKED_DIAMOND)
                        .define('F', Items.FURNACE)
                        .unlockedBy(getHasName(Items.FURNACE), has(Items.FURNACE))
                        .save(output);


                // =================================================================
                // SMOKER REINFORCED & NETHERITE
                // =================================================================
                ShapedRecipeBuilder.shaped(items(), RecipeCategory.REDSTONE, ModItems.REINFORCED_SMOKER, 3)
                        .pattern("DDD")
                        .pattern("SSS")
                        .pattern("DDD")
                        .define('D', ModItems.CRACKED_DIAMOND)
                        .define('S', Items.SMOKER)
                        .unlockedBy(getHasName(Items.SMOKER), has(Items.SMOKER))
                        .save(output);

                shaped(RecipeCategory.MISC, ModItems.BASIC_UPGRADE_TEMPLATE, 2)
                        .pattern("ABA")
                        .pattern("ACA")
                        .pattern("AAA")
                        .define('A', Items.GOLD_INGOT) // 7 Gold
                        .define('B', ModItems.BASIC_UPGRADE_TEMPLATE) // Das Original
                        .define('C', Items.IRON_BLOCK) // Iron Block Core
                        .unlockedBy(getHasName(ModItems.BASIC_UPGRADE_TEMPLATE), has(ModItems.BASIC_UPGRADE_TEMPLATE)).save(output);


                // =================================================================
                // NETHERITE NUGGET <-> INGOT RECIPES
                // =================================================================
                shapeless(RecipeCategory.MISC, ModItems.NETHERITE_NUGGET, 9)
                        .requires(Items.NETHERITE_INGOT)
                        .unlockedBy(getHasName(Items.NETHERITE_INGOT), has(Items.NETHERITE_INGOT))
                        .unlockedBy(getHasName(ModItems.NETHERITE_NUGGET), has(ModItems.NETHERITE_NUGGET))
                        .save(output);
                shaped(RecipeCategory.MISC, Items.NETHERITE_INGOT)
                        .pattern("NNN")
                        .pattern("NNN")
                        .pattern("NNN")
                        .define('N', ModItems.NETHERITE_NUGGET)
                        .unlockedBy(getHasName(ModItems.NETHERITE_NUGGET), has(ModItems.NETHERITE_NUGGET))
                        .unlockedBy(getHasName(Items.NETHERITE_INGOT), has(Items.NETHERITE_INGOT))
                        .save(output);

                shapeless(RecipeCategory.MISC, ModItems.ENDERITE_NUGGET, 9)
                        .requires(ModItems.ENDERITE_INGOT)
                        .unlockedBy(getHasName(ModItems.ENDERITE_INGOT), has(ModItems.ENDERITE_INGOT))
                        .unlockedBy(getHasName(ModItems.ENDERITE_NUGGET), has(ModItems.ENDERITE_NUGGET))
                        .save(output, "enderite_nugget_from_ingot");
                shaped(RecipeCategory.MISC, ModItems.ENDERITE_INGOT)
                        .pattern("NNN")
                        .pattern("NNN")
                        .pattern("NNN")
                        .define('N', ModItems.ENDERITE_NUGGET)
                        .unlockedBy(getHasName(ModItems.ENDERITE_NUGGET), has(ModItems.ENDERITE_NUGGET))
                        .unlockedBy(getHasName(ModItems.ENDERITE_INGOT), has(ModItems.ENDERITE_INGOT))
                        .save(output, "enderite_ingot_from_nugget");


                // =================================================================
                // NETHERITE FOOD RECIPES
                // =================================================================
                shaped(RecipeCategory.FOOD, ModItems.NETHERITE_CARROT)
                        .pattern(" N ")
                        .pattern("NCN")
                        .pattern(" N ")
                        .define('N', ModItems.NETHERITE_NUGGET)
                        .define('C', Items.CARROT)
                        .unlockedBy(getHasName(ModItems.NETHERITE_NUGGET), has(ModItems.NETHERITE_NUGGET))
                        .save(output);
                shaped(RecipeCategory.FOOD, ModItems.NETHERITE_APPLE)
                        .pattern("NNN")
                        .pattern("NAN")
                        .pattern("NNN")
                        .define('N', ModItems.NETHERITE_NUGGET)
                        .define('A', Items.APPLE)
                        .unlockedBy(getHasName(ModItems.NETHERITE_NUGGET), has(ModItems.NETHERITE_NUGGET))
                        .save(output);

                shaped(RecipeCategory.FOOD, ModItems.ENDERITE_CARROT)
                        .pattern(" N ")
                        .pattern("NCN")
                        .pattern(" N ")
                        .define('N', ModItems.ENDERITE_NUGGET)
                        .define('C', Items.CARROT)
                        .unlockedBy(getHasName(ModItems.ENDERITE_NUGGET), has(ModItems.ENDERITE_NUGGET))
                        .save(output);
                shaped(RecipeCategory.FOOD, ModItems.ENDERITE_APPLE)
                        .pattern("NNN")
                        .pattern("NAN")
                        .pattern("NNN")
                        .define('N', ModItems.ENDERITE_NUGGET)
                        .define('A', Items.APPLE)
                        .unlockedBy(getHasName(ModItems.ENDERITE_NUGGET), has(ModItems.ENDERITE_NUGGET))
                        .save(output);



                // =================================================================
                // UPGRADE RECIPES FÜR WERKZEUGE
                // =================================================================

                // Kosten (Entscheidung des Besitzers): das Doppelte des Materials, das die Werkbank fuer das
                // Ziel-Werkzeug verlangt. Aufwerten behaelt Verzauberungen, Schaden und Namen - dafuer ist
                // es teurer als neu bauen.
                // Spitzhacken und Aexte (Werkbank: 3 -> Aufwerten: 6)
                createUpgradeRecipe(items(), output, this::unlockedRecipe, Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.COBBLESTONE, 6);
                createUpgradeRecipe(items(), output, this::unlockedRecipe, Items.STONE_PICKAXE, Items.IRON_PICKAXE, Items.IRON_INGOT, 6);
                createUpgradeRecipe(items(), output, this::unlockedRecipe, Items.IRON_PICKAXE, Items.GOLDEN_PICKAXE, Items.GOLD_INGOT, 6);
                createUpgradeRecipe(items(), output, this::unlockedRecipe, Items.GOLDEN_PICKAXE, Items.DIAMOND_PICKAXE, Items.DIAMOND, 6);
                createUpgradeRecipe(items(), output, this::unlockedRecipe, Items.COPPER_PICKAXE, Items.IRON_PICKAXE, Items.IRON_INGOT, 6);
                createUpgradeRecipe(items(), output, this::unlockedRecipe, Items.COPPER_AXE, Items.IRON_AXE, Items.IRON_INGOT, 6);
                createUpgradeRecipe(items(), output, this::unlockedRecipe, Items.WOODEN_AXE, Items.STONE_AXE, Items.COBBLESTONE, 6);
                createUpgradeRecipe(items(), output, this::unlockedRecipe, Items.STONE_AXE, Items.IRON_AXE, Items.IRON_INGOT, 6);
                createUpgradeRecipe(items(), output, this::unlockedRecipe, Items.IRON_AXE, Items.GOLDEN_AXE, Items.GOLD_INGOT, 6);
                createUpgradeRecipe(items(), output, this::unlockedRecipe, Items.GOLDEN_AXE, Items.DIAMOND_AXE, Items.DIAMOND, 6);

                // Schwerter und Hacken (Werkbank: 2 -> Aufwerten: 4)
                createUpgradeRecipe(items(), output, this::unlockedRecipe, Items.WOODEN_SWORD, Items.STONE_SWORD, Items.COBBLESTONE, 4);
                createUpgradeRecipe(items(), output, this::unlockedRecipe, Items.STONE_SWORD, Items.IRON_SWORD, Items.IRON_INGOT, 4);
                createUpgradeRecipe(items(), output, this::unlockedRecipe, Items.IRON_SWORD, Items.GOLDEN_SWORD, Items.GOLD_INGOT, 4);
                createUpgradeRecipe(items(), output, this::unlockedRecipe, Items.GOLDEN_SWORD, Items.DIAMOND_SWORD, Items.DIAMOND, 4);
                createUpgradeRecipe(items(), output, this::unlockedRecipe, Items.COPPER_SWORD, Items.IRON_SWORD, Items.IRON_INGOT, 4);
                createUpgradeRecipe(items(), output, this::unlockedRecipe, Items.WOODEN_HOE, Items.STONE_HOE, Items.COBBLESTONE, 4);
                createUpgradeRecipe(items(), output, this::unlockedRecipe, Items.STONE_HOE, Items.IRON_HOE, Items.IRON_INGOT, 4);
                createUpgradeRecipe(items(), output, this::unlockedRecipe, Items.IRON_HOE, Items.GOLDEN_HOE, Items.GOLD_INGOT, 4);
                createUpgradeRecipe(items(), output, this::unlockedRecipe, Items.GOLDEN_HOE, Items.DIAMOND_HOE, Items.DIAMOND, 4);
                createUpgradeRecipe(items(), output, this::unlockedRecipe, Items.COPPER_HOE, Items.IRON_HOE, Items.IRON_INGOT, 4);

                // Schaufeln (Werkbank: 1 -> Aufwerten: 2)
                createUpgradeRecipe(items(), output, this::unlockedRecipe, Items.WOODEN_SHOVEL, Items.STONE_SHOVEL, Items.COBBLESTONE, 2);
                createUpgradeRecipe(items(), output, this::unlockedRecipe, Items.STONE_SHOVEL, Items.IRON_SHOVEL, Items.IRON_INGOT, 2);
                createUpgradeRecipe(items(), output, this::unlockedRecipe, Items.IRON_SHOVEL, Items.GOLDEN_SHOVEL, Items.GOLD_INGOT, 2);
                createUpgradeRecipe(items(), output, this::unlockedRecipe, Items.GOLDEN_SHOVEL, Items.DIAMOND_SHOVEL, Items.DIAMOND, 2);
                createUpgradeRecipe(items(), output, this::unlockedRecipe, Items.COPPER_SHOVEL, Items.IRON_SHOVEL, Items.IRON_INGOT, 2);

                // Mod-Werkzeuge
                // Meissel (Werkbank: 1 Barren/Diamant -> Aufwerten: 2)
                createUpgradeRecipe(items(), output, this::unlockedRecipe, ModItems.COPPER_CHISEL, ModItems.IRON_CHISEL, Items.IRON_INGOT, 2);
                createUpgradeRecipe(items(), output, this::unlockedRecipe, ModItems.IRON_CHISEL, ModItems.GOLD_CHISEL, Items.GOLD_INGOT, 2);
                createUpgradeRecipe(items(), output, this::unlockedRecipe, ModItems.GOLD_CHISEL, ModItems.DIAMOND_CHISEL, Items.DIAMOND, 2);

                // Vorschlaghammer (Werkbank: 1 Block + 2 Barren = 11 Barren -> Aufwerten: 22)
                createUpgradeRecipe(items(), output, this::unlockedRecipe, ModItems.COPPER_SLEDGEHAMMER, ModItems.IRON_SLEDGEHAMMER, Items.IRON_INGOT, 22);
                createUpgradeRecipe(items(), output, this::unlockedRecipe, ModItems.IRON_SLEDGEHAMMER, ModItems.GOLD_SLEDGEHAMMER, Items.GOLD_INGOT, 22);
                createUpgradeRecipe(items(), output, this::unlockedRecipe, ModItems.GOLD_SLEDGEHAMMER, ModItems.DIAMOND_SLEDGEHAMMER, Items.DIAMOND, 22);

                // Baustab: kein Barren-Preis - ein Baustab braucht einen Kern (Late-Game), also kostet das
                // Aufwerten genau einen Kern der Zielstufe (Entscheidung des Besitzers, 2026-09-25).
                createUpgradeRecipe(items(), output, this::unlockedRecipe, ModItems.COPPER_BUILDING_WAND, ModItems.IRON_BUILDING_WAND, ModItems.IRON_CORE, 1);
                createUpgradeRecipe(items(), output, this::unlockedRecipe, ModItems.IRON_BUILDING_WAND, ModItems.GOLD_BUILDING_WAND, ModItems.GOLD_CORE, 1);
                createUpgradeRecipe(items(), output, this::unlockedRecipe, ModItems.GOLD_BUILDING_WAND, ModItems.DIAMOND_BUILDING_WAND, ModItems.DIAMOND_CORE, 1);



                // --- 1. Synthese: Raw Enderite (4 Shards + 4 Dust + 1 Pearl) ---
                // Shapeless Rezept
                shapeless(RecipeCategory.MISC, ModItems.RAW_ENDERITE)
                        .requires(ModItems.NIHILITH_SHARD, 4)
                        .requires(ModItems.ASTRALIT_DUST, 4)
                        .requires(Items.ENDER_PEARL)
                        .unlockedBy(getHasName(ModItems.NIHILITH_SHARD), has(ModItems.NIHILITH_SHARD))
                        .unlockedBy(getHasName(ModItems.ASTRALIT_DUST), has(ModItems.ASTRALIT_DUST))
                        .save(output, "raw_enderite_synthesis");

                // --- 2. Schichten: 3 Rohenderit uebereinander -> Geschichtetes Rohenderit ---
                // Besitzer 2026-09-29: Rohenderit schmilzt nicht mehr direkt zu Schrott.
                shaped(RecipeCategory.MISC, ModItems.LAYERED_RAW_ENDERITE)
                        .pattern("R")
                        .pattern("R")
                        .pattern("R")
                        .define('R', ModItems.RAW_ENDERITE)
                        .unlockedBy(getHasName(ModItems.RAW_ENDERITE), has(ModItems.RAW_ENDERITE))
                        .save(output);

                // --- 3. Schmelzen: Geschichtetes Rohenderit -> Enderitschrott ---
                // Doppelt so lange je Schrott wie das fruehere Direktschmelzen (72000 Ticks): 144000 Ticks = 2 h in
                // einem Vanilla-Schmelzofen (1 h verstaerkt, 30 min Netherit, 15 min Enderit), 10 Erfahrung.
                // Ein Schrott je Geschichtetem Rohenderit: Kochrezepte koennen auf 1.21.11 nur ein einzelnes Item
                // liefern (STRICT_SINGLE_ITEM_CODEC), also auf allen Linien 3 Rohenderit je Schrott. Zeiten ueber
                // 32767 Ticks ueberleben Speichern und Menue-Sync nur dank AbstractFurnaceBlockEntityMixin /
                // AbstractFurnaceMenuMixin. Kein Ofen- oder Raeucherofen-Rezept, wie beim Rohenderit zuvor.
                oreBlasting(List.of(ModItems.LAYERED_RAW_ENDERITE), RecipeCategory.MISC, net.minecraft.world.item.crafting.CookingBookCategory.MISC, ModItems.ENDERITE_SCRAP, 10.0f, fastMachineTicks(2 * 72000), "enderite_scrap");

                // --- 3. Barren: Enderite Ingot (4 Scrap + 4 Diamond) ---
                // Hinweis: Du wolltest Diamanten statt Netherite, um Netherite nicht zu entwerten.
                shaped(RecipeCategory.MISC, ModItems.ENDERITE_INGOT)
                        .pattern("SDS")
                        .pattern("DND")
                        .pattern("SDS")
                        .define('N', Items.NETHERITE_INGOT)
                        .define('S', ModItems.ENDERITE_SCRAP)
                        .define('D', Items.DIAMOND)
                        .unlockedBy(getHasName(ModItems.ENDERITE_SCRAP), has(ModItems.ENDERITE_SCRAP))
                        .save(output, "enderite_ingot_from_scrap");

                // --- 4. Enderite Block ---
                nineBlockStorageRecipes(RecipeCategory.BUILDING_BLOCKS, ModItems.ENDERITE_INGOT, RecipeCategory.DECORATIONS, ModBlocks.ENDERITE_BLOCK);

                // --- 5. Duplizierung des Templates ---
                shaped(RecipeCategory.MISC, ModItems.ENDERITE_UPGRADE_TEMPLATE, 2)
                        .pattern("ATA")
                        .pattern("AEA")
                        .pattern("AAA")
                        .define('A', Items.DIAMOND) // 7 Diamonds
                        .define('E', Items.END_STONE) // End Stone
                        .define('T', ModItems.ENDERITE_UPGRADE_TEMPLATE)
                        .unlockedBy(getHasName(ModItems.ENDERITE_UPGRADE_TEMPLATE), has(ModItems.ENDERITE_UPGRADE_TEMPLATE)).save(output);

                // --- 6. Smithing Upgrades (Netherite -> Enderite) ---
                List<Item> netheriteItems = List.of(
                        Items.NETHERITE_SWORD, Items.NETHERITE_PICKAXE, Items.NETHERITE_AXE, Items.NETHERITE_SHOVEL, Items.NETHERITE_HOE, Items.NETHERITE_SPEAR,
                        Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS,
                        Items.NETHERITE_HORSE_ARMOR, Items.NETHERITE_NAUTILUS_ARMOR,
                        ModItems.NETHERITE_CORE, ModItems.NETHERITE_CHISEL, ModItems.NETHERITE_BUILDING_WAND, ModItems.NETHERITE_SLEDGEHAMMER
                );
                List<Item> enderiteItems = List.of(
                        ModItems.ENDERITE_SWORD, ModItems.ENDERITE_PICKAXE, ModItems.ENDERITE_AXE, ModItems.ENDERITE_SHOVEL, ModItems.ENDERITE_HOE, ModItems.ENDERITE_SPEAR,
                        ModItems.ENDERITE_HELMET, ModItems.ENDERITE_CHESTPLATE, ModItems.ENDERITE_LEGGINGS, ModItems.ENDERITE_BOOTS,
                        ModItems.ENDERITE_HORSE_ARMOR, ModItems.ENDERITE_NAUTILUS_ARMOR,
                        ModItems.ENDERITE_CORE, ModItems.ENDERITE_CHISEL, ModItems.ENDERITE_BUILDING_WAND, ModItems.ENDERITE_SLEDGEHAMMER
                );

                for (int i = 0; i < netheriteItems.size(); i++) {
                    createSmithingTransform(output,
                            ModItems.ENDERITE_UPGRADE_TEMPLATE,
                            netheriteItems.get(i),
                            ModItems.ENDERITE_INGOT,
                            RecipeCategory.COMBAT, // Kategorie ggf. anpassen je nach Item
                            enderiteItems.get(i)
                    );
                }

                // --- POLISHED END STONE ---
                shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.POLISHED_END_STONE, 4)
                        .pattern("SS")
                        .pattern("SS")
                        .define('S', Items.END_STONE)
                        .unlockedBy(getHasName(Items.END_STONE), has(Items.END_STONE))
                        .save(output);

                // --- CHECKER BLOCKS ---
                createCheckerRecipe(output, ModBlocks.PURPUR_QUARTZ_CHECKER, Items.PURPUR_BLOCK);
                createCheckerRecipe(output, ModBlocks.LAPIS_QUARTZ_CHECKER, Items.LAPIS_BLOCK);
                createCheckerRecipe(output, ModBlocks.BLACKSTONE_QUARTZ_CHECKER, Items.BLACKSTONE);
                // Resin Placeholder (z.B. Red Nether Bricks)
                createCheckerRecipe(output, ModBlocks.RESIN_QUARTZ_CHECKER, Items.RED_NETHER_BRICKS);
                // End-Schachbretter: Splitter bzw. Staub stehen direkt fuer den farbigen Block
                createCheckerRecipe(output, ModBlocks.NIHILITH_QUARTZ_CHECKER, ModItems.NIHILITH_SHARD);
                createCheckerRecipe(output, ModBlocks.ASTRALIT_QUARTZ_CHECKER, ModItems.ASTRALIT_DUST);
                createCheckerRecipe(output, ModBlocks.ENDER_QUARTZ_CHECKER, ModItems.ENDER_QUARTZ);

                // --- ASTRAL / NIHIL BLOCKS (8 Block + 1 Powder/Shard) ---
                createCoatingRecipe(output, ModBlocks.ASTRAL_PURPUR_BLOCK, Items.PURPUR_BLOCK, ModItems.ASTRALIT_DUST);
                createCoatingRecipe(output, ModBlocks.NIHIL_PURPUR_BLOCK, Items.PURPUR_BLOCK, ModItems.NIHILITH_SHARD);
                createCoatingRecipe(output, ModBlocks.ASTRAL_END_STONE, ModBlocks.POLISHED_END_STONE, ModItems.ASTRALIT_DUST);
                createCoatingRecipe(output, ModBlocks.NIHIL_END_STONE, ModBlocks.POLISHED_END_STONE, ModItems.NIHILITH_SHARD);

                // --- ENDERQUARZ ---
                // Formlos 1 Astralitstaub + 1 Nihilithsplitter + 1 Quarz -> 2 Enderquarz.
                shapeless(RecipeCategory.MISC, ModItems.ENDER_QUARTZ, 2)
                        .requires(ModItems.ASTRALIT_DUST)
                        .requires(ModItems.NIHILITH_SHARD)
                        .requires(Items.QUARTZ)
                        .unlockedBy(getHasName(ModItems.ASTRALIT_DUST), has(ModItems.ASTRALIT_DUST))
                        .unlockedBy(getHasName(ModItems.NIHILITH_SHARD), has(ModItems.NIHILITH_SHARD))
                        .save(output);

                // --- HANDBUECHER (com.simplebuilding.guide.GuideBooks) ---
                // Einsteiger-Handbuch: Buch + Werkbank (falls das geschenkte verloren geht). Themenbuecher:
                // Buch oder Handbuch + Schluesselitem; das Handbuch ist sein eigener Handwerksrest und
                // bleibt liegen. Freigeschaltet durch das Schluesselitem oder schon durch das Handbuch.
                shapeless(RecipeCategory.MISC, ModItems.GUIDE_BOOK)
                        .requires(Items.BOOK)
                        .requires(com.simplebuilding.guide.GuideBooks.keyItem(com.simplebuilding.guide.GuideBooks.Book.GUIDE))
                        .unlockedBy(getHasName(Items.BOOK), has(Items.BOOK))
                        .save(output);
                for (com.simplebuilding.guide.GuideBooks.Book topic : com.simplebuilding.guide.GuideBooks.Book.topics()) {
                    ItemLike key = com.simplebuilding.guide.GuideBooks.keyItem(topic);
                    shapeless(RecipeCategory.MISC, com.simplebuilding.guide.GuideBooks.item(topic))
                            .requires(Ingredient.of(Items.BOOK, ModItems.GUIDE_BOOK))
                            .requires(key)
                            .unlockedBy(getHasName(key), has(key))
                            .unlockedBy(getHasName(ModItems.GUIDE_BOOK), has(ModItems.GUIDE_BOOK))
                            .save(output);
                }

                // --- BLAUPAUSE ---
                // Formlos 1 Enderquarz + 1 Papier + 1 Leuchttintenbeutel -> 1 leere Blaupause (spaet, aber leicht;
                // Besitzer 2026-09-29: Leuchttinte statt Tinte).
                shapeless(RecipeCategory.TOOLS, ModItems.BLUEPRINT)
                        .requires(ModItems.ENDER_QUARTZ)
                        .requires(Items.PAPER)
                        .requires(Items.GLOW_INK_SAC)
                        .unlockedBy(getHasName(ModItems.ENDER_QUARTZ), has(ModItems.ENDER_QUARTZ))
                        .save(output);

                // --- END-PALETTEN (Astralit, Nihilith, Enderquarz) ---
                // Werkbank wie Vanilla: 4 Material im Quadrat -> 1 Grundblock (wie Quarz- und
                // Amethystblock), Grundblock 2x2 -> 4 poliert, poliert 2x2 -> 4 Ziegel (Kette wie bei
                // Tiefenschiefer), 2 poliert uebereinander -> 2 Saeulen (wie Quarzsaeule), 2 Ziegelstufen
                // uebereinander -> 1 gemeisselte Ziegel; Treppe 6 -> 4, Stufe 3 -> 6, Mauer 6 -> 6.
                // Steinmetz: der Grundblock schneidet alles, der polierte Block seine Familie, die
                // Ziegelfamilie, Saeule und gemeisselte Ziegel, die Ziegel ihre Familie und gemeisselte.
                // Umfaerben: 8 Endstein-/Purpur-Variante um 1 Material -> 8 der passenden Palettenvariante.
                createEndPaletteRecipes(output, ModBlocks.ASTRALIT_PALETTE, ModItems.ASTRALIT_DUST, false);
                createEndPaletteRecipes(output, ModBlocks.NIHILITH_PALETTE, ModItems.NIHILITH_SHARD, false);
                createEndPaletteRecipes(output, ModBlocks.ENDER_QUARTZ_PALETTE, ModItems.ENDER_QUARTZ, true);

                // Der beschichtete Endstein (aeltere Grundlage der Ziegel) schneidet weiter in den ganzen
                // Ziegelbausatz und dazu in den Grundblock; der beschichtete Purpurblock schneidet in den
                // polierten Block. Letzteres ersetzt das Umfaerben von Purpurbloecken, das bei Astralit
                // und Nihilith schon als Beschichtungsrezept (-> Astral-/Nihil-Purpurblock) belegt ist.
                createCoatedStoneCuts(ModBlocks.ASTRAL_END_STONE, ModBlocks.ASTRAL_PURPUR_BLOCK, ModBlocks.ASTRALIT_PALETTE);
                createCoatedStoneCuts(ModBlocks.NIHIL_END_STONE, ModBlocks.NIHIL_PURPUR_BLOCK, ModBlocks.NIHILITH_PALETTE);

                // --- GRAVITY BLOCKS ---
                // Nihilith -> No Gravity (Suspended)
                createCoatingRecipe(output, ModBlocks.SUSPENDED_SAND, Items.SAND, ModItems.NIHILITH_SHARD);
                createCoatingRecipe(output, ModBlocks.SUSPENDED_GRAVEL, Items.GRAVEL, ModItems.NIHILITH_SHARD);

                // Astralit -> Reverse Gravity (Levitating/Upwards)
                createCoatingRecipe(output, ModBlocks.LEVITATING_SAND, Items.SAND, ModItems.ASTRALIT_DUST);
                createCoatingRecipe(output, ModBlocks.LEVITATING_GRAVEL, Items.GRAVEL, ModItems.ASTRALIT_DUST);

                // --- ENDERITE ITEMS (Smithing Upgrades) ---
                // Bundle
                createSmithingTransform(output, ModItems.ENDERITE_UPGRADE_TEMPLATE, ModItems.NETHERITE_BUNDLE, ModItems.ENDERITE_INGOT, RecipeCategory.TOOLS, ModItems.ENDERITE_BUNDLE);
                // Quiver
                createSmithingTransform(output, ModItems.ENDERITE_UPGRADE_TEMPLATE, ModItems.NETHERITE_QUIVER, ModItems.ENDERITE_INGOT, RecipeCategory.TOOLS, ModItems.ENDERITE_QUIVER);
            }

            /** Werkbank-, Steinmetz- und Umfaerbe-Rezepte einer End-Palette (siehe Kommentar am Aufruf). */
            private void createEndPaletteRecipes(RecipeOutput exporter, ModBlocks.EndPalette p, ItemLike material,
                                                 boolean recolourPurpurBlock) {
                shaped(RecipeCategory.BUILDING_BLOCKS, p.block())
                        .pattern("##")
                        .pattern("##")
                        .define('#', material)
                        .unlockedBy(getHasName(material), has(material))
                        .save(exporter);
                shaped(RecipeCategory.BUILDING_BLOCKS, p.polished(), 4)
                        .pattern("##")
                        .pattern("##")
                        .define('#', p.block())
                        .unlockedBy(getHasName(p.block()), has(p.block()))
                        .save(exporter);
                shaped(RecipeCategory.BUILDING_BLOCKS, p.bricks(), 4)
                        .pattern("##")
                        .pattern("##")
                        .define('#', p.polished())
                        .unlockedBy(getHasName(p.polished()), has(p.polished()))
                        .save(exporter);
                shaped(RecipeCategory.BUILDING_BLOCKS, p.pillar(), 2)
                        .pattern("#")
                        .pattern("#")
                        .define('#', p.polished())
                        .unlockedBy(getHasName(p.polished()), has(p.polished()))
                        .save(exporter);
                for (Block[] family : new Block[][]{
                        {p.bricks(), p.brickStairs(), p.brickSlab(), p.brickWall()},
                        {p.polished(), p.polishedStairs(), p.polishedSlab(), p.polishedWall()}}) {
                    Block full = family[0];
                    stairBuilder(family[1], Ingredient.of(full)).unlockedBy(getHasName(full), has(full)).save(exporter);
                    slabBuilder(RecipeCategory.BUILDING_BLOCKS, family[2], Ingredient.of(full)).unlockedBy(getHasName(full), has(full)).save(exporter);
                    wallBuilder(RecipeCategory.DECORATIONS, family[3], Ingredient.of(full)).unlockedBy(getHasName(full), has(full)).save(exporter);
                }
                chiseledBuilder(RecipeCategory.BUILDING_BLOCKS, p.chiseled(), Ingredient.of(p.brickSlab()))
                        .unlockedBy(getHasName(p.brickSlab()), has(p.brickSlab())).save(exporter);
                if (p.blockStairs() != null) {
                    // Wie quartz_stairs/quartz_slab: Treppe 6 -> 4, Stufe 3 -> 6, Steinmetz 1 -> 1 bzw. 1 -> 2
                    stairBuilder(p.blockStairs(), Ingredient.of(p.block())).unlockedBy(getHasName(p.block()), has(p.block())).save(exporter);
                    slabBuilder(RecipeCategory.BUILDING_BLOCKS, p.blockSlab(), Ingredient.of(p.block())).unlockedBy(getHasName(p.block()), has(p.block())).save(exporter);
                    stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, p.blockStairs(), p.block());
                    stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, p.blockSlab(), p.block(), 2);
                }

                // Steinmetz: Grundblock -> alles, poliert -> polierte Familie + Ziegelfamilie + Saeule +
                // gemeisselt, Ziegel -> Ziegelfamilie + gemeisselt
                for (Block base : List.of(p.block(), p.polished(), p.bricks())) {
                    if (base != p.bricks()) {
                        if (base != p.polished()) {
                            stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, p.polished(), base);
                        }
                        stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, p.polishedStairs(), base);
                        stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, p.polishedSlab(), base, 2);
                        stonecutterResultFromBase(RecipeCategory.DECORATIONS, p.polishedWall(), base);
                        stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, p.bricks(), base);
                        stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, p.pillar(), base);
                    }
                    stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, p.brickStairs(), base);
                    stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, p.brickSlab(), base, 2);
                    stonecutterResultFromBase(RecipeCategory.DECORATIONS, p.brickWall(), base);
                    stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, p.chiseled(), base);
                }

                // Umfaerben wie mit Farbstoff, nur wo es eine passende Vanilla-Variante gibt
                createRecolourRecipe(exporter, p.block(), Items.END_STONE, material);
                createRecolourRecipe(exporter, p.bricks(), Items.END_STONE_BRICKS, material);
                createRecolourRecipe(exporter, p.brickStairs(), Items.END_STONE_BRICK_STAIRS, material);
                createRecolourRecipe(exporter, p.brickSlab(), Items.END_STONE_BRICK_SLAB, material);
                createRecolourRecipe(exporter, p.brickWall(), Items.END_STONE_BRICK_WALL, material);
                if (recolourPurpurBlock) {
                    createRecolourRecipe(exporter, p.polished(), Items.PURPUR_BLOCK, material);
                }
                createRecolourRecipe(exporter, p.polishedStairs(), Items.PURPUR_STAIRS, material);
                createRecolourRecipe(exporter, p.polishedSlab(), Items.PURPUR_SLAB, material);
                createRecolourRecipe(exporter, p.pillar(), Items.PURPUR_PILLAR, material);
                if (recolourPurpurBlock) {
                    // Enderquarz auch aus Quarz: jede Quarz-Variante mit Gegenstueck in der Palette
                    createRecolourRecipe(exporter, p.block(), Items.QUARTZ_BLOCK, material);
                    createRecolourRecipe(exporter, p.bricks(), Items.QUARTZ_BRICKS, material);
                    createRecolourRecipe(exporter, p.pillar(), Items.QUARTZ_PILLAR, material);
                    createRecolourRecipe(exporter, p.chiseled(), Items.CHISELED_QUARTZ_BLOCK, material);
                    createRecolourRecipe(exporter, p.polished(), Items.SMOOTH_QUARTZ, material);
                    createRecolourRecipe(exporter, p.polishedStairs(), Items.SMOOTH_QUARTZ_STAIRS, material);
                    createRecolourRecipe(exporter, p.polishedSlab(), Items.SMOOTH_QUARTZ_SLAB, material);
                    createRecolourRecipe(exporter, p.blockStairs(), Items.QUARTZ_STAIRS, material);
                    createRecolourRecipe(exporter, p.blockSlab(), Items.QUARTZ_SLAB, material);
                }
            }

            private void createCoatedStoneCuts(Block coatedEndStone, Block coatedPurpur, ModBlocks.EndPalette p) {
                stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, p.block(), coatedEndStone);
                stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, p.bricks(), coatedEndStone);
                stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, p.brickStairs(), coatedEndStone);
                stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, p.brickSlab(), coatedEndStone, 2);
                stonecutterResultFromBase(RecipeCategory.DECORATIONS, p.brickWall(), coatedEndStone);
                stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, p.pillar(), coatedEndStone);
                stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, p.chiseled(), coatedEndStone);
                stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, p.polished(), coatedPurpur);
            }

            /** 8 einer Vanilla-Variante um 1 Material -> 8 der Palettenvariante, wie Farbstoff an Glas oder Ton. */
            private void createRecolourRecipe(RecipeOutput exporter, ItemLike result, ItemLike vanilla, ItemLike material) {
                shaped(RecipeCategory.BUILDING_BLOCKS, result, 8)
                        .pattern("###")
                        .pattern("#M#")
                        .pattern("###")
                        .define('#', vanilla)
                        .define('M', material)
                        .unlockedBy(getHasName(material), has(material))
                        .save(exporter, BuiltInRegistries.ITEM.getKey(result.asItem()).getPath() + "_from_"
                                + BuiltInRegistries.ITEM.getKey(vanilla.asItem()).getPath());
            }

            private void createCheckerRecipe(RecipeOutput exporter, ItemLike output, ItemLike base) {
                shaped(RecipeCategory.BUILDING_BLOCKS, output, 4)
                        .pattern("BQ")
                        .pattern("QB")
                        .define('B', base)
                        .define('Q', Items.QUARTZ_BLOCK)
                        .unlockedBy(getHasName(base), has(base))
                        .save(exporter);
            }

            // Helper für Coating (8 Base + 1 Material)
            private void createCoatingRecipe(RecipeOutput exporter, ItemLike output, ItemLike base, ItemLike material) {
                shaped(RecipeCategory.BUILDING_BLOCKS, output, 8)
                        .pattern("BBB")
                        .pattern("BMB")
                        .pattern("BBB")
                        .define('B', base)
                        .define('M', material)
                        .unlockedBy(getHasName(material), has(material))
                        .save(exporter);
            }

            /**
             * Rezepte aus Simple Tweaks (ModRecipeProvider dort) plus die Enderit-Stufen und das neue
             * Echo-Kompass-Rezept (docs/SIMPLETWEAKS-UEBERNAHME.md, Abschnitte 2 und 3). Rezept-IDs
             * wie in Simple Tweaks. Seit 2026-09-27 kosten die Aufwertungen der Pad-Familien die
             * Druckplatte des Zielmaterials (Diamant-/Netherit-/Enderit-Druckplatte) statt des Rohstoffs.
             */
            private void buildTweaksRecipes() {
                Ingredient anyTemplate = Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE,
                        Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE,
                        Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE,
                        Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE, Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE,
                        Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE, Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE,
                        Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE, Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE,
                        Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE,
                        Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE, Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE,
                        Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE, Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE,
                        Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE, Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE);
                Item netheriteTemplate = Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE;
                Item enderiteTemplate = ModItems.ENDERITE_UPGRADE_TEMPLATE;

                Item diamondPlate = TweaksBlocks.DIAMOND_PRESSURE_PLATE.asItem();
                Item netheritePlate = TweaksBlocks.NETHERITE_PRESSURE_PLATE.asItem();
                Item enderitePlate = TweaksBlocks.ENDERITE_PRESSURE_PLATE.asItem();

                // Stufe I jeder Pad-Familie (Besitzer 2026-09-28): Schmiede aus Vorlage + Druckplatte der Familie
                // (Basis) + Freischalt-Zutat - Kupfer: Kupfer-Druckplatte + Kupferkern (Chunk-Loader), Eisen:
                // schwere Waegeplatte + Eisenkern (Launchpad), Gold: leichte Waegeplatte + Endermankopf
                // (Spawn-Teleporter), Diamant: Diamant-Druckplatte + Elytra (Elytra-Pad), Netherit:
                // Netherit-Druckplatte + Lohenkopf (Trank-Pad), Enderit: Enderit-Druckplatte + Enderit-Kern
                // (Flypad). Die Aufwertungen zahlen weiter mit der Druckplatte des Zielmaterials.

                // Spawn-Teleporter I-III (Wartezeit 50/20/5 s; III = Enderit, eigener Wiedereinstiegspunkt)
                tweaksSmithing(anyTemplate, Items.LIGHT_WEIGHTED_PRESSURE_PLATE, TweaksItems.ENDERMAN_HEAD, TweaksBlocks.SPAWN_TELEPORTER, "spawn_teleporter_smithing");
                tweaksSmithing(Ingredient.of(netheriteTemplate), TweaksBlocks.SPAWN_TELEPORTER, netheritePlate, TweaksBlocks.SPAWN_TELEPORTER_TIER_2, "spawn_teleporter_tier2_smithing");
                tweaksSmithing(Ingredient.of(enderiteTemplate), TweaksBlocks.SPAWN_TELEPORTER_TIER_2, enderitePlate, TweaksBlocks.ENDERITE_SPAWN_TELEPORTER, "enderite_spawn_teleporter_smithing");

                // Launchpads I-III: I = beliebige Vorlage + schwere Waegeplatte + Eisenkern,
                // dann Netherit- und Enderit-Druckplatte
                tweaksSmithing(anyTemplate, Items.HEAVY_WEIGHTED_PRESSURE_PLATE, ModItems.IRON_CORE, TweaksBlocks.LAUNCHPAD, "launchpad_smithing");
                tweaksSmithing(Ingredient.of(netheriteTemplate), TweaksBlocks.LAUNCHPAD, netheritePlate, TweaksBlocks.NETHERITE_LAUNCHPAD, "netherite_launchpad_smithing");
                tweaksSmithing(Ingredient.of(enderiteTemplate), TweaksBlocks.NETHERITE_LAUNCHPAD, enderitePlate, TweaksBlocks.ENDERITE_LAUNCHPAD, "enderite_launchpad_smithing");

                // Druckplatten: Diamant -> Netherit -> Enderit
                shaped(RecipeCategory.REDSTONE, TweaksBlocks.DIAMOND_PRESSURE_PLATE)
                        .pattern("DD")
                        .define('D', Items.DIAMOND)
                        .unlockedBy(getHasName(Items.DIAMOND), has(Items.DIAMOND))
                        .save(output);
                tweaksSmithing(Ingredient.of(netheriteTemplate), TweaksBlocks.DIAMOND_PRESSURE_PLATE, Items.NETHERITE_INGOT, TweaksBlocks.NETHERITE_PRESSURE_PLATE, "netherite_pressure_plate_smithing");
                tweaksSmithing(Ingredient.of(enderiteTemplate), TweaksBlocks.NETHERITE_PRESSURE_PLATE, ModItems.ENDERITE_INGOT, TweaksBlocks.ENDERITE_PRESSURE_PLATE, "enderite_pressure_plate_smithing");

                // Elytra-Pads I-V: I = beliebige Vorlage + Diamant-Druckplatte + Elytra (2026-09-28; vorher
                // Elytra ohne dritte Zutat), dann Diamant-, Netherit-, Enderit-Druckplatte, V mit Netherstern
                tweaksSmithing(anyTemplate, diamondPlate, Items.ELYTRA, TweaksBlocks.ELYTRA_PAD, "elytra_pad_smithing");
                tweaksSmithing(anyTemplate, TweaksBlocks.ELYTRA_PAD, diamondPlate, TweaksBlocks.REINFORCED_ELYTRA_PAD, "reinforced_elytra_pad_smithing");
                tweaksSmithing(Ingredient.of(netheriteTemplate), TweaksBlocks.REINFORCED_ELYTRA_PAD, netheritePlate, TweaksBlocks.NETHERITE_ELYTRA_PAD, "netherite_elytra_pad_smithing");
                tweaksSmithing(Ingredient.of(enderiteTemplate), TweaksBlocks.NETHERITE_ELYTRA_PAD, enderitePlate, TweaksBlocks.ENDERITE_ELYTRA_PAD, "enderite_elytra_pad_smithing");
                tweaksSmithing(Ingredient.of(netheriteTemplate), TweaksBlocks.ENDERITE_ELYTRA_PAD, Items.NETHER_STAR, TweaksBlocks.FINE_ELYTRA_PAD, "fine_elytra_pad_smithing");

                // Flypads I-III aus Enderit (Besitzer 2026-09-27): I = Enderit-Vorlage + Enderit-Druckplatte +
                // Enderit-Kern, II = Enderit-Vorlage + Flypad I + Enderit-Druckplatte, III = Enderit-Vorlage +
                // zwei Flypads II (Schmiede, das zweite als Zutat). Die alten Rezepte (Netherit-Flypad,
                // Enderit-Flypad, Stellar aus der Werkbank) entfallen.
                tweaksSmithing(Ingredient.of(enderiteTemplate), enderitePlate, ModItems.ENDERITE_CORE, TweaksBlocks.FLYPAD, "flypad_tier1_smithing");
                tweaksSmithing(Ingredient.of(enderiteTemplate), TweaksBlocks.FLYPAD, enderitePlate, TweaksBlocks.REINFORCED_FLYPAD, "flypad_tier2_smithing");
                tweaksSmithing(Ingredient.of(enderiteTemplate), TweaksBlocks.REINFORCED_FLYPAD, TweaksBlocks.REINFORCED_FLYPAD, TweaksBlocks.STELLAR_FLYPAD, "stellar_flypad_smithing");

                // Trank-Pads I-III (Besitzer 2026-09-28): I = beliebige Vorlage + Netherit-Druckplatte + Lohenkopf
                // (Schmiede, vorher Werkbank formlos); II = Enderit-Vorlage + I + Enderit-Druckplatte (Aufwertungen
                // zahlen mit der Druckplatte des Zielmaterials); III = Enderit-Vorlage + II + Enderit-Kern (wie Flypad I).
                tweaksSmithing(anyTemplate, netheritePlate, TweaksItems.BLAZE_HEAD, TweaksBlocks.POTION_PAD, "potion_pad_smithing");
                tweaksSmithing(Ingredient.of(enderiteTemplate), TweaksBlocks.POTION_PAD, enderitePlate, TweaksBlocks.REINFORCED_POTION_PAD, "reinforced_potion_pad_smithing");
                tweaksSmithing(Ingredient.of(enderiteTemplate), TweaksBlocks.REINFORCED_POTION_PAD, ModItems.ENDERITE_CORE, TweaksBlocks.INFUSED_POTION_PAD, "infused_potion_pad_smithing");

                // Kupfer-Druckplatte (2 Kupferbloecke) und Chunk-Loader I-III (Kupferplatte + Kupferkern,
                // dann Netherit- und Enderit-Druckplatte)
                shaped(RecipeCategory.REDSTONE, TweaksBlocks.COPPER_PRESSURE_PLATE)
                        .pattern("CC")
                        .define('C', Items.COPPER_BLOCK.weathering().unaffected())
                        .unlockedBy("has_copper_block", has(Items.COPPER_BLOCK.weathering().unaffected()))
                        .save(output);
                // Gewachste Kupferplatten auch an der Werkbank, wie Vanilla-Kupfer: Platte + Honigwabe
                for (int i = 0; i < 4; i++) {
                    net.minecraft.world.level.block.Block unwaxed = com.simplebuilding.tweaks.block.CopperPressurePlateBlock.stages().get(i);
                    net.minecraft.world.level.block.Block waxed = com.simplebuilding.tweaks.block.CopperPressurePlateBlock.waxedStages().get(i);
                    shapeless(RecipeCategory.REDSTONE, waxed)
                            .requires(unwaxed)
                            .requires(Items.HONEYCOMB)
                            .group(getItemName(waxed))
                            .unlockedBy(getHasName(unwaxed), has(unwaxed))
                            .save(output, getConversionRecipeName(waxed, Items.HONEYCOMB));
                }
                tweaksSmithing(anyTemplate, TweaksBlocks.COPPER_PRESSURE_PLATE, ModItems.COPPER_CORE, TweaksBlocks.CHUNK_LOADER, "chunk_loader_smithing");
                tweaksSmithing(Ingredient.of(netheriteTemplate), TweaksBlocks.CHUNK_LOADER, netheritePlate, TweaksBlocks.NETHERITE_CHUNK_LOADER, "netherite_chunk_loader_smithing");
                tweaksSmithing(Ingredient.of(enderiteTemplate), TweaksBlocks.NETHERITE_CHUNK_LOADER, enderitePlate, TweaksBlocks.ENDERITE_CHUNK_LOADER, "enderite_chunk_loader_smithing");

                // Versteckt (Spoiler in docs/SIMPLETWEAKS-UEBERNAHME.md): Rezepte und Advancements ueber den Endstufen.
                com.simplebuilding.tweaks.datagen.EasterEggData.generate(output, items());

                // Echolot/Echo Sounder (Id echo_sounder; Besitzer 2026-09-27): Bergungskompass in der Mitte, Enderit-Kern unten mittig,
                // sieben Enderit-Nuggets aussen herum, oben mittig inzwischen auch ein Nugget
                shaped(RecipeCategory.TOOLS, TweaksItems.ECHO_COMPASS)
                        .pattern("NNN")
                        .pattern("NRN")
                        .pattern("NEN")
                        .define('N', ModItems.ENDERITE_NUGGET)
                        .define('E', ModItems.ENDERITE_CORE)
                        .define('R', Items.RECOVERY_COMPASS)
                        .unlockedBy(getHasName(Items.RECOVERY_COMPASS), has(Items.RECOVERY_COMPASS))
                        .save(output);

                // Amethystlinse (Id amethyst_lens; Simple Tweaks hatte kein Rezept): Eisen-Baukern in
                // der Mitte, Amethystsplitter als Linse darueber, Redstone links und rechts oben,
                // Eisenbarren als U darunter.
                shaped(RecipeCategory.TOOLS, TweaksItems.LASER_POINTER)
                        .pattern("RAR")
                        .pattern("ICI")
                        .pattern("III")
                        .define('A', Items.AMETHYST_SHARD)
                        .define('C', ModItems.IRON_CORE)
                        .define('I', Items.IRON_INGOT)
                        .define('R', Items.REDSTONE)
                        .unlockedBy(getHasName(Items.AMETHYST_SHARD), has(Items.AMETHYST_SHARD))
                        .save(output);
            }

            private void tweaksSmithing(Ingredient template, ItemLike base, ItemLike addition, ItemLike result, String name) {
                SmithingTransformRecipeBuilder.smithing(template, Ingredient.of(base), Ingredient.of(addition),
                                RecipeCategory.TOOLS, result.asItem())
                        .unlocks(getHasName(base), has(base))
                        .save(output, Simplebuilding.MOD_ID + ":" + name);
            }

            // --- Helpers ---
            private void createSmithingTransform(RecipeOutput exporter, Item template, Item base, Item addition, RecipeCategory category, Item result) {
                SmithingTransformRecipeBuilder.smithing(
                                Ingredient.of(template),
                                Ingredient.of(base),
                                Ingredient.of(addition),
                                category,
                                result
                        )
                        .unlocks(getHasName(addition), has(addition))
                        .save(exporter, getItemName(result) + "_smithing");
            }

            /** Wie Vanillas {@code dyedItem}, aber unter {@code simplebuilding:<id>_dyed}. */
            private void modDyedItem(Item target, String group) {
                CustomCraftingRecipeBuilder.customCrafting(RecipeCategory.MISC,
                                (commonInfo, bookInfo) -> new DyeRecipe(commonInfo, bookInfo, Ingredient.of(target),
                                        tag(ItemTags.DYES), new ItemStackTemplate(target)))
                        .unlockedBy(getHasName(target), has(target))
                        .group(group)
                        .save(output, Simplebuilding.MOD_ID + ":" + getItemName(target) + "_dyed");
            }

            private void createChiselRecipe(Item resultItem, Item material, Item nugget) {
                shaped(RecipeCategory.TOOLS, resultItem)
                        .pattern("   ")
                        .pattern("NM ")
                        .pattern("SN ")
                        .define('M', material)
                        .define('S', Items.STICK)
                        .define('N', nugget)
                        .unlockedBy(getHasName(material), has(material))
                        .save(output);
            }

            private void createCoreRecipe(Item resultItem, Item material) {
                shaped(RecipeCategory.MISC, resultItem)
                        .pattern(" M ")
                        .pattern("MNM")
                        .pattern(" M ")
                        .define('M', material)
                        .define('N', Items.NETHER_STAR)
                        .unlockedBy(getHasName(Items.NETHER_STAR), has(Items.NETHER_STAR))
                        .save(output, getItemName(resultItem) + "_plus");
            }

            private void createWandRecipe(Item resultItem, Item core, Item material) {
                shaped(RecipeCategory.TOOLS, resultItem)
                        .pattern("  C")
                        .pattern(" S ")
                        .pattern("S  ")
                        .define('C', core)
                        .define('S', Items.STICK)
                        .unlockedBy(getHasName(core), has(core))
                        .save(output);
            }

            private void createSledgehammerRecipe(Item resultItem, Item material, Item block) {
                shaped(RecipeCategory.TOOLS, resultItem)
                        .pattern("BMM")
                        .pattern(" S ")
                        .pattern(" S ")
                        .define('M', material)
                        .define('B', block)
                        .define('S', Items.STICK)
                        .unlockedBy(getHasName(material), has(material))
                        .save(output);
            }

            private void createSmithing(Item input, Item result, RecipeCategory category) {
                SmithingTransformRecipeBuilder.smithing(Ingredient.of(
                        Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                        Ingredient.of(input),
                        Ingredient.of(Items.NETHERITE_INGOT),
                        category,
                        result)
                        .unlocks("has_netherite_ingot", has(Items.NETHERITE_INGOT))
                        .save(output, getItemName(result) + "_smithing");
            }

            /**
             * Geformtes Werkbank-Rezept vom Typ simplebuilding:reinforced_bundle: wie ein
             * crafting_shaped, nur dass das Ergebnis den Komponenten-Patch des eingelegten Behaelters
             * uebernimmt (siehe ReinforcedBundleRecipe). ShapedRecipeBuilder kann nur Vanillas
             * ShapedRecipe schreiben; Rezept-Id, Buchkategorie und Freischalt-Fortschritt sind hier
             * deshalb genau so gebaut wie in ShapedRecipeBuilder#save.
             */
            private void createContainerUpgrade(Item result, Item unlockedBy, Map<Character, ItemLike> key, String... rows) {
                Map<Character, Ingredient> ingredients = new LinkedHashMap<>();
                key.forEach((symbol, item) -> ingredients.put(symbol, Ingredient.of(item)));
                ShapedRecipePattern pattern = ShapedRecipePattern.of(ingredients, rows);

                ResourceKey<Recipe<?>> recipeKey = ResourceKey.create(Registries.RECIPE,
                        Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, getItemName(result)));
                ReinforcedBundleRecipe recipe = new ReinforcedBundleRecipe("",
                        RecipeBuilder.determineCraftingBookCategory(RecipeCategory.TOOLS), pattern, new ItemStackTemplate(result));

                Advancement.Builder advancement = output.advancement()
                        .addCriterion("has_the_recipe", unlockedRecipe(recipeKey))
                        .rewards(AdvancementRewards.Builder.recipe(recipeKey))
                        .requirements(AdvancementRequirements.Strategy.OR)
                        .addCriterion(getHasName(unlockedBy), has(unlockedBy));
                output.accept(recipeKey, recipe, advancement.build(
                        recipeKey.identifier().withPrefix("recipes/" + RecipeCategory.TOOLS.getFolderName() + "/")));
            }

        };
    }

    private void createUpgradeRecipe(HolderGetter<Item> items, RecipeOutput exporter,
                                     java.util.function.Function<ResourceKey<Recipe<?>>, Criterion<RecipeUnlockedTrigger.TriggerInstance>> unlocked,
                                     Item base, Item result, Item material, int count) {
        Identifier recipeId = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "upgrade_" + getItemName(base) + "_to_" + getItemName(result));

        ResourceKey<Recipe<?>> recipeKey = ResourceKey.create(Registries.RECIPE, recipeId);

        ResourceKey<Item> resultKey = BuiltInRegistries.ITEM.getResourceKey(result).orElseThrow();
        ItemStackTemplate resultTemplate = new ItemStackTemplate(
                items.getOrThrow(resultKey),
                1,
                DataComponentPatch.EMPTY
        );

        CountBasedSmithingRecipe recipe = new CountBasedSmithingRecipe(
                Ingredient.of(ModItems.BASIC_UPGRADE_TEMPLATE),
                Ingredient.of(base),
                Ingredient.of(material),
                resultTemplate,
                count
        );

        // Wie Vanillas Rezept-Advancements: Freischalten schenkt das Rezept (vorher nur "has_template"
        // ohne Belohnung - die 34 Advancements schalteten nichts frei).
        exporter.accept(recipeKey, recipe, exporter.advancement()
                .addCriterion("has_the_recipe", unlocked.apply(recipeKey))
                .rewards(AdvancementRewards.Builder.recipe(recipeKey))
                .requirements(AdvancementRequirements.Strategy.OR)
                .addCriterion("has_template", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.BASIC_UPGRADE_TEMPLATE))
                .build(Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "recipes/misc/" + recipeId.getPath())));
    }

    private String getItemName(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath();
    }

    @Override
    public String getName() {
        return "SimpleBuilding Recipes";
    }

    // MC 26.2: Die 16 Einzelfelder Items.<COLOR>_DYE gibt es nicht mehr; die Farbvarianten
    // stecken jetzt in Items.DYE (ColorCollection<Item>). ColorCollection.pick(DyeColor) ist
    // exakt derselbe Switch ueber DyeColor wie zuvor (white()..black()), also verhaltensgleich.
    private Item getDyeItem(DyeColor color) {
        return Items.DYE.pick(color);
    }
}

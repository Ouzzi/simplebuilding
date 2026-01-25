package com.simplebuilding.datagen;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.recipe.CountBasedSmithingRecipe;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.advancement.criterion.InventoryChangedCriterion;
import net.minecraft.data.recipe.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends FabricRecipeProvider {

    // WICHTIG: Diesen Tag manuell definieren, da er in 1.21.2+ Code fehlt
    private static final TagKey<Item> TRIM_TEMPLATES = TagKey.of(RegistryKeys.ITEM, Identifier.ofVanilla("trim_templates"));

    public ModRecipeProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected RecipeGenerator getRecipeGenerator(RegistryWrapper.WrapperLookup wrapperLookup, RecipeExporter recipeExporter) {
        return new RecipeGenerator(wrapperLookup, recipeExporter) {
            @Override
            public void generate() {

                // ---------------------------------------------------------
                // WICHTIG: Registry Zugriff für Tags vorbereiten (für 1.21.2+)
                // ---------------------------------------------------------
                RegistryWrapper.Impl<Item> itemRegistry = registries.getOrThrow(RegistryKeys.ITEM);

                // =================================================================
                // FIX: DUMMY REZEPT FÜR SCHMIEDETISCH (Glowing Ink)
                // =================================================================
                // Wir erstellen Ingredients über die Registry (ofTag statt fromTag)
                Ingredient templateIngredient = Ingredient.ofTag(itemRegistry.getOrThrow(TRIM_TEMPLATES));
                Ingredient armorIngredient = Ingredient.ofTag(itemRegistry.getOrThrow(ItemTags.TRIMMABLE_ARMOR));

                SmithingTransformRecipeJsonBuilder.create(
                        templateIngredient,                     // Slot 1: Jedes Template (damit auch deins geht)
                        armorIngredient,                        // Slot 2: Rüstung
                        Ingredient.ofItems(Items.GLOW_INK_SAC), // Slot 3: Leuchttinte
                        RecipeCategory.MISC,
                        ModItems.GLOWING_TRIM_TEMPLATE          // Dummy Output (wird vom Mixin überschrieben)
                )
                .criterion("has_glowing_template", conditionsFromItem(ModItems.GLOWING_TRIM_TEMPLATE))
                .offerTo(exporter, "glowing_armor_upgrade_dummy");

                SmithingTransformRecipeJsonBuilder.create(
                        templateIngredient,                     // Slot 1: Jedes Template (damit auch deins geht)
                        armorIngredient,                        // Slot 2: Rüstung
                        Ingredient.ofItems(Items.GLOWSTONE_DUST), // Slot 3: Leuchttinte
                        RecipeCategory.MISC,
                        ModItems.EMITTING_TRIM_TEMPLATE          // Dummy Output (wird vom Mixin überschrieben)
                )
                .criterion("has_emitting_template", conditionsFromItem(ModItems.EMITTING_TRIM_TEMPLATE))
                .offerTo(exporter, "emitting_armor_upgrade_dummy");


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
                // SPATULAS (Stick + Material + Nugget/Shard) - ECKIG
                // =================================================================
                createSpatulaRecipe(ModItems.STONE_SPATULA, Items.COBBLESTONE, Items.COPPER_INGOT);
                createSpatulaRecipe(ModItems.COPPER_SPATULA, Items.COPPER_INGOT, Items.COPPER_INGOT);
                createSpatulaRecipe(ModItems.IRON_SPATULA, Items.IRON_INGOT, Items.COPPER_INGOT);
                createSpatulaRecipe(ModItems.GOLD_SPATULA, Items.GOLD_INGOT, Items.COPPER_INGOT);
                createSpatulaRecipe(ModItems.DIAMOND_SPATULA, Items.DIAMOND, Items.COPPER_INGOT);
                createSmithing(ModItems.DIAMOND_SPATULA, ModItems.NETHERITE_SPATULA, RecipeCategory.TOOLS);


                // =================================================================
                // BUILDING CORES (Material + Nether Star)
                // =================================================================
                createCoreRecipe(ModItems.COPPER_CORE, Items.COPPER_INGOT);
                createCoreRecipe(ModItems.IRON_CORE, Items.IRON_INGOT);
                createCoreRecipe(ModItems.GOLD_CORE, Items.GOLD_INGOT);
                createCoreRecipe(ModItems.DIAMOND_CORE, Items.DIAMOND);

                SmithingTransformRecipeJsonBuilder.create(
                        Ingredient.ofItems(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                        Ingredient.ofItems(ModItems.DIAMOND_CORE),
                        Ingredient.ofItems(Items.NETHERITE_INGOT),
                        RecipeCategory.MISC,
                        ModItems.NETHERITE_CORE
                ).criterion("has_netherite_ingot", conditionsFromItem(Items.NETHERITE_INGOT))
                 .offerTo(exporter, getItemPath(ModItems.NETHERITE_CORE) + "_smithing");


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
                createSledgehammerRecipe(ModItems.COPPER_SLEDGEHAMMER, Items.COPPER_INGOT, Items.COPPER_BLOCK);
                createSledgehammerRecipe(ModItems.IRON_SLEDGEHAMMER, Items.IRON_INGOT, Items.IRON_BLOCK);
                createSledgehammerRecipe(ModItems.GOLD_SLEDGEHAMMER, Items.GOLD_INGOT, Items.GOLD_BLOCK);
                createSledgehammerRecipe(ModItems.DIAMOND_SLEDGEHAMMER, Items.DIAMOND, Items.DIAMOND_BLOCK);
                createSmithing(ModItems.DIAMOND_SLEDGEHAMMER, ModItems.NETHERITE_SLEDGEHAMMER, RecipeCategory.TOOLS);


                // =================================================================
                // RANGEFINDER (Antik / Octant Style)
                // =================================================================
                createShaped(RecipeCategory.TOOLS, ModItems.OCTANT)
                        .pattern(" GL")
                        .pattern("IPG")
                        .pattern("CI ")
                        .input('I', Items.GOLD_INGOT)
                        .input('G', Items.GOLD_NUGGET)
                        .input('C', Items.COPPER_INGOT)
                        .input('P', Items.COMPASS)
                        .input('L', Items.LEAD)
                        .criterion(hasItem(Items.COPPER_INGOT), conditionsFromItem(Items.COPPER_INGOT))
                        .offerTo(exporter);
                for (DyeColor color : DyeColor.values()) {
                    Item resultItem = ModItems.COLORED_OCTANT_ITEMS.get(color);
                    Item dyeItem = getDyeItem(color);

                    if (resultItem != null && dyeItem != null) {

                        ShapelessRecipeJsonBuilder.create(registries.getOrThrow(RegistryKeys.ITEM), RecipeCategory.TOOLS, resultItem)
                                .input(ModItems.OCTANT)
                                .input(dyeItem)
                                .criterion(hasItem(dyeItem), conditionsFromItem(dyeItem))
                                .offerTo(exporter, getRecipeName(resultItem) + "_from_dye");
                    }
                }

                // =================================================================
                // VELOCITY_GAUGE
                // =================================================================
                createShaped(RecipeCategory.TOOLS, ModItems.VELOCITY_GAUGE)
                        .pattern(" A ")
                        .pattern("OCO")
                        .pattern("QQQ")
                        .input('C', Items.COMPASS)
                        .input('A', Items.AMETHYST_SHARD)
                        .input('O', Items.COPPER_INGOT)
                        .input('Q', Items.QUARTZ)
                        .criterion(hasItem(Items.COMPASS), conditionsFromItem(Items.COMPASS))
                        .offerTo(exporter);

                // =================================================================
                // MAGNET
                // =================================================================
                createShaped(RecipeCategory.TOOLS, ModItems.MAGNET)
                        .pattern(" IR")
                        .pattern("ILI")
                        .pattern("BI ")
                        .input('L', Items.LODESTONE)
                        .input('I', Items.IRON_INGOT)
                        .input('R', Items.REDSTONE)
                        .input('B', Items.LAPIS_LAZULI)
                        .criterion(hasItem(Items.LODESTONE), conditionsFromItem(Items.LODESTONE))
                        .offerTo(exporter);

                createShaped(RecipeCategory.TOOLS, ModItems.ROTATOR)
                        .pattern(" I ")
                        .pattern("IEI")
                        .pattern("II ")
                        .input('I', Items.IRON_INGOT)
                        .input('E', Items.ENDER_PEARL)
                        .criterion(hasItem(Items.IRON_INGOT), conditionsFromItem(Items.IRON_INGOT))
                        .offerTo(exporter);

                // =================================================================
                // REINFORCED BUNDLE
                // =================================================================
                createShaped(RecipeCategory.TOOLS, ModItems.REINFORCED_BUNDLE)
                        .pattern(" S ")
                        .pattern("NBN")
                        .pattern("LLL")
                        .input('S', Items.STRING)
                        .input('N', Items.COPPER_NUGGET)
                        .input('B', Items.BUNDLE)
                        .input('L', Items.LEATHER)
                        .criterion( hasItem(Items.BUNDLE), conditionsFromItem(Items.BUNDLE))
                        .offerTo(exporter);


                createSmithing(ModItems.REINFORCED_BUNDLE, ModItems.NETHERITE_BUNDLE, RecipeCategory.TOOLS);


                // =================================================================
                // QUIVER
                // =================================================================
                createShaped(RecipeCategory.TOOLS, ModItems.QUIVER)
                        .pattern(" SL")
                        .pattern("SLN")
                        .pattern("B  ")
                        .input('S', Items.STRING)
                        .input('L', Items.LEATHER)
                        .input('N', Items.COPPER_NUGGET)
                        .input('B', Items.BUNDLE)
                        .criterion(hasItem(Items.BUNDLE), conditionsFromItem(Items.BUNDLE))
                        .offerTo(exporter);

                createSmithing(ModItems.QUIVER, ModItems.NETHERITE_QUIVER, RecipeCategory.TOOLS);


                // =================================================================
                // ORE DETECTOR
                // =================================================================
                createShaped(RecipeCategory.TOOLS, ModItems.ORE_DETECTOR)
                        .pattern(" S ")
                        .pattern(" C ")
                        .pattern(" G ")
                        .input('C', Items.COMPASS)
                        .input('G', ModItems.GOLD_CORE)
                        .input('S', Items.CALIBRATED_SCULK_SENSOR)
                        .criterion(hasItem(Items.COMPASS), conditionsFromItem(Items.COMPASS))
                        .offerTo(exporter);


                createShaped(RecipeCategory.MISC, ModItems.CRACKED_DIAMOND)
                        .pattern("PPP")
                        .pattern("PPP")
                        .pattern("PPP")
                        .input('P', ModItems.DIAMOND_PEBBLE)
                        .criterion(hasItem(ModItems.DIAMOND_PEBBLE), conditionsFromItem(ModItems.DIAMOND_PEBBLE))
                        .offerTo(exporter);

                offerBlasting(java.util.List.of(ModItems.CRACKED_DIAMOND), RecipeCategory.MISC, Items.DIAMOND, 1.0f, 100, "diamond_from_cracked");


                // Construction light recipe - lapis light
                createShaped(RecipeCategory.MISC, ModItems.CONSTRUCTION_LIGHT)
                        .pattern("LGL")
                        .pattern("GTG")
                        .pattern("LGL")
                        .input('G', Items.GLASS)
                        .input('L', Items.LAPIS_LAZULI)
                        .input('T', Items.TORCH)
                        .criterion(hasItem(Items.LAPIS_LAZULI), conditionsFromItem(Items.LAPIS_LAZULI))
                        .offerTo(exporter);


                createShaped(RecipeCategory.BUILDING_BLOCKS, ModItems.CRACKED_DIAMOND_BLOCK)
                        .pattern("CCC")
                        .pattern("CCC")
                        .pattern("CCC")
                        .input('C', ModItems.CRACKED_DIAMOND)
                        .criterion(hasItem(ModItems.CRACKED_DIAMOND), conditionsFromItem(ModItems.CRACKED_DIAMOND)).offerTo(exporter);
                offerShapelessRecipe(ModItems.CRACKED_DIAMOND, ModItems.CRACKED_DIAMOND_BLOCK, "cracked_diamond_from_block", 9);


                // =================================================================
                // HOPPER REINFORCED & NETHERITE
                // =================================================================
                // 1. Reinforced Hopper
                ShapedRecipeJsonBuilder.create(registries.getOrThrow(RegistryKeys.ITEM), RecipeCategory.REDSTONE, ModItems.REINFORCED_HOPPER, 6)
                        .pattern("HNH")
                        .pattern("DDD")
                        .pattern("HHH")
                        .input('D', ModItems.CRACKED_DIAMOND)
                        .input('H', Items.HOPPER)
                        .input('N', Items.NAME_TAG)
                        .criterion(hasItem(Items.HOPPER), conditionsFromItem(Items.HOPPER))
                        .offerTo(exporter, "reinforced_hopper_from_crafting");

                ShapedRecipeJsonBuilder.create(registries.getOrThrow(RegistryKeys.ITEM), RecipeCategory.REDSTONE, ModItems.NETHERITE_HOPPER)
                        .pattern("H")
                        .pattern("N")
                        .pattern("H")
                        .input('H', ModItems.REINFORCED_HOPPER)
                        .input('N', ModItems.NETHERITE_NUGGET)
                        .criterion(hasItem(ModItems.REINFORCED_HOPPER), conditionsFromItem(ModItems.REINFORCED_HOPPER))
                        .offerTo(exporter, "netherite_hopper_from_crafting");


                // =================================================================
                // PISTON REINFORCED & NETHERITE
                // =================================================================
                ShapedRecipeJsonBuilder.create(registries.getOrThrow(RegistryKeys.ITEM), RecipeCategory.REDSTONE, ModItems.REINFORCED_PISTON, 3)
                        .pattern("DDD")
                        .pattern("PPP")
                        .pattern("III")
                        .input('D', ModItems.CRACKED_DIAMOND)
                        .input('P', Items.PISTON)
                        .input('I', Items.IRON_INGOT)
                        .criterion(hasItem(Items.PISTON), conditionsFromItem(Items.PISTON))
                        .offerTo(exporter);
                createBulkUpgrade(ModItems.REINFORCED_PISTON, ModItems.NETHERITE_PISTON, RecipeCategory.REDSTONE);


                // =================================================================
                // BLAST FURNACE REINFORCED & NETHERITE
                // =================================================================
                ShapedRecipeJsonBuilder.create(registries.getOrThrow(RegistryKeys.ITEM), RecipeCategory.REDSTONE, ModItems.REINFORCED_BLAST_FURNACE, 3)
                        .pattern("DDD")
                        .pattern("BBB")
                        .pattern("DDD")
                        .input('D', ModItems.CRACKED_DIAMOND)
                        .input('B', Items.BLAST_FURNACE)
                        .criterion(hasItem(Items.BLAST_FURNACE), conditionsFromItem(Items.BLAST_FURNACE))
                        .offerTo(exporter);
                createBulkUpgrade(ModItems.REINFORCED_BLAST_FURNACE, ModItems.NETHERITE_BLAST_FURNACE, RecipeCategory.DECORATIONS);


                // =================================================================
                // FURNACE REINFORCED & NETHERITE
                // =================================================================
                ShapedRecipeJsonBuilder.create(registries.getOrThrow(RegistryKeys.ITEM), RecipeCategory.REDSTONE, ModItems.REINFORCED_FURNACE, 3)
                        .pattern("DDD")
                        .pattern("FFF")
                        .pattern("DDD")
                        .input('D', ModItems.CRACKED_DIAMOND)
                        .input('F', Items.FURNACE)
                        .criterion(hasItem(Items.FURNACE), conditionsFromItem(Items.FURNACE))
                        .offerTo(exporter);
                createBulkUpgrade(ModItems.REINFORCED_FURNACE, ModItems.NETHERITE_FURNACE, RecipeCategory.DECORATIONS);


                // =================================================================
                // SMOKER REINFORCED & NETHERITE
                // =================================================================
                ShapedRecipeJsonBuilder.create(registries.getOrThrow(RegistryKeys.ITEM), RecipeCategory.REDSTONE, ModItems.REINFORCED_SMOKER, 3)
                        .pattern("DDD")
                        .pattern("SSS")
                        .pattern("DDD")
                        .input('D', ModItems.CRACKED_DIAMOND)
                        .input('S', Items.SMOKER)
                        .criterion(hasItem(Items.SMOKER), conditionsFromItem(Items.SMOKER))
                        .offerTo(exporter);
                createBulkUpgrade(ModItems.REINFORCED_SMOKER, ModItems.NETHERITE_SMOKER, RecipeCategory.DECORATIONS);

                createShaped(RecipeCategory.MISC, ModItems.BASIC_UPGRADE_TEMPLATE, 2)
                        .pattern("ABA")
                        .pattern("ACA")
                        .pattern("AAA")
                        .input('A', Items.GOLD_INGOT) // 7 Gold
                        .input('B', ModItems.BASIC_UPGRADE_TEMPLATE) // Das Original
                        .input('C', Items.IRON_BLOCK) // Iron Block Core
                        .criterion(hasItem(ModItems.BASIC_UPGRADE_TEMPLATE), conditionsFromItem(ModItems.BASIC_UPGRADE_TEMPLATE)).offerTo(exporter);


                // =================================================================
                // NETHERITE NUGGET <-> INGOT RECIPES
                // =================================================================
                createShapeless(RecipeCategory.MISC, ModItems.NETHERITE_NUGGET, 9)
                        .input(Items.NETHERITE_INGOT)
                        .criterion(hasItem(Items.NETHERITE_INGOT), conditionsFromItem(Items.NETHERITE_INGOT))
                        .criterion(hasItem(ModItems.NETHERITE_NUGGET), conditionsFromItem(ModItems.NETHERITE_NUGGET))
                        .offerTo(exporter);
                createShaped(RecipeCategory.MISC, Items.NETHERITE_INGOT)
                        .pattern("NNN")
                        .pattern("NNN")
                        .pattern("NNN")
                        .input('N', ModItems.NETHERITE_NUGGET)
                        .criterion(hasItem(ModItems.NETHERITE_NUGGET), conditionsFromItem(ModItems.NETHERITE_NUGGET))
                        .criterion(hasItem(Items.NETHERITE_INGOT), conditionsFromItem(Items.NETHERITE_INGOT))
                        .offerTo(exporter);


                // =================================================================
                // NETHERITE FOOD RECIPES
                // =================================================================
                createShaped(RecipeCategory.FOOD, ModItems.NETHERITE_CARROT)
                        .pattern(" N ")
                        .pattern("NCN")
                        .pattern(" N ")
                        .input('N', ModItems.NETHERITE_NUGGET)
                        .input('C', Items.CARROT)
                        .criterion(hasItem(ModItems.NETHERITE_NUGGET), conditionsFromItem(ModItems.NETHERITE_NUGGET))
                        .offerTo(exporter);
                createShaped(RecipeCategory.FOOD, ModItems.NETHERITE_APPLE)
                        .pattern("NNN")
                        .pattern("NAN")
                        .pattern("NNN")
                        .input('N', ModItems.NETHERITE_NUGGET)
                        .input('A', Items.APPLE)
                        .criterion(hasItem(ModItems.NETHERITE_NUGGET), conditionsFromItem(ModItems.NETHERITE_NUGGET))
                        .offerTo(exporter);



                // =================================================================
                // UPGRADE RECIPES FÜR WERKZEUGE
                // =================================================================

                // Spitzhacken (Crafting: 3 -> Upgrade: 4)
                createUpgradeRecipe(exporter, Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.COBBLESTONE, 4);
                createUpgradeRecipe(exporter, Items.STONE_PICKAXE, Items.IRON_PICKAXE, Items.IRON_INGOT, 4);
                createUpgradeRecipe(exporter, Items.IRON_PICKAXE, Items.GOLDEN_PICKAXE, Items.GOLD_INGOT, 4);
                createUpgradeRecipe(exporter, Items.GOLDEN_PICKAXE, Items.DIAMOND_PICKAXE, Items.DIAMOND, 4);
                createUpgradeRecipe(exporter, Items.COPPER_PICKAXE, Items.IRON_PICKAXE, Items.IRON_INGOT, 4);

                // Äxte (Crafting: 3 -> Upgrade: 4)
                createUpgradeRecipe(exporter, Items.WOODEN_AXE, Items.STONE_AXE, Items.COBBLESTONE, 4);
                createUpgradeRecipe(exporter, Items.STONE_AXE, Items.IRON_AXE, Items.IRON_INGOT, 4);
                createUpgradeRecipe(exporter, Items.IRON_AXE, Items.GOLDEN_AXE, Items.GOLD_INGOT, 4);
                createUpgradeRecipe(exporter, Items.GOLDEN_AXE, Items.DIAMOND_AXE, Items.DIAMOND, 4);

                // Schwerter / Hacken (Crafting: 2 -> Upgrade: 3)
                createUpgradeRecipe(exporter, Items.WOODEN_SWORD, Items.STONE_SWORD, Items.COBBLESTONE, 3);
                createUpgradeRecipe(exporter, Items.STONE_SWORD, Items.IRON_SWORD, Items.IRON_INGOT, 3);
                createUpgradeRecipe(exporter, Items.IRON_SWORD, Items.GOLDEN_SWORD, Items.GOLD_INGOT, 3);
                createUpgradeRecipe(exporter, Items.GOLDEN_SWORD, Items.DIAMOND_SWORD, Items.DIAMOND, 3);

                // Schaufeln (Crafting: 1 -> Upgrade: 2)
                createUpgradeRecipe(exporter, Items.WOODEN_SHOVEL, Items.STONE_SHOVEL, Items.COBBLESTONE, 2);
                createUpgradeRecipe(exporter, Items.STONE_SHOVEL, Items.IRON_SHOVEL, Items.IRON_INGOT, 2);
                createUpgradeRecipe(exporter, Items.IRON_SHOVEL, Items.GOLDEN_SHOVEL, Items.GOLD_INGOT, 2);
                createUpgradeRecipe(exporter, Items.GOLDEN_SHOVEL, Items.DIAMOND_SHOVEL, Items.DIAMOND, 2);

                // Mod Tools (Beispiele)
                // Chisel (Crafting: 1 -> Upgrade: 2)
                createUpgradeRecipe(exporter, ModItems.COPPER_CHISEL, ModItems.IRON_CHISEL, Items.IRON_INGOT, 2);
                createUpgradeRecipe(exporter, ModItems.IRON_CHISEL, ModItems.GOLD_CHISEL, Items.GOLD_INGOT, 2);
                createUpgradeRecipe(exporter, ModItems.GOLD_CHISEL, ModItems.DIAMOND_CHISEL, Items.DIAMOND, 2);

                // Sledgehammer (Crafting: 5 -> Upgrade: 6)
                createUpgradeRecipe(exporter, ModItems.COPPER_SLEDGEHAMMER, ModItems.IRON_SLEDGEHAMMER, Items.IRON_INGOT, 12);
                createUpgradeRecipe(exporter, ModItems.IRON_SLEDGEHAMMER, ModItems.GOLD_SLEDGEHAMMER, Items.GOLD_INGOT, 12);
                createUpgradeRecipe(exporter, ModItems.GOLD_SLEDGEHAMMER, ModItems.DIAMOND_SLEDGEHAMMER, Items.DIAMOND, 12);



                // --- 1. Synthese: Raw Enderite (4 Shards + 4 Dust + 1 Pearl) ---
                // Shapeless Rezept
                createShapeless(RecipeCategory.MISC, ModItems.RAW_ENDERITE)
                        .input(ModItems.NIHILITH_SHARD, 4)
                        .input(ModItems.ASTRALIT_DUST, 4)
                        .input(Items.ENDER_PEARL)
                        .criterion(hasItem(ModItems.NIHILITH_SHARD), conditionsFromItem(ModItems.NIHILITH_SHARD))
                        .criterion(hasItem(ModItems.ASTRALIT_DUST), conditionsFromItem(ModItems.ASTRALIT_DUST))
                        .offerTo(exporter, "raw_enderite_synthesis");

                // --- 2. Schmelzen: Raw -> Scrap ---
                offerBlasting(List.of(ModItems.RAW_ENDERITE), RecipeCategory.MISC, ModItems.ENDERITE_SCRAP, 2.0f, 200, "enderite_scrap");

                // --- 3. Barren: Enderite Ingot (4 Scrap + 4 Diamond) ---
                // Hinweis: Du wolltest Diamanten statt Netherite, um Netherite nicht zu entwerten.
                createShaped(RecipeCategory.MISC, ModItems.ENDERITE_INGOT)
                        .pattern("SDS")
                        .pattern("DSD")
                        .pattern("SDS") // Oder SSS / D D / SSS, hier ein Schachbrettmuster
                        .input('S', ModItems.ENDERITE_SCRAP)
                        .input('D', Items.DIAMOND)
                        .criterion(hasItem(ModItems.ENDERITE_SCRAP), conditionsFromItem(ModItems.ENDERITE_SCRAP))
                        .offerTo(exporter, "enderite_ingot_from_scrap");

                // --- 4. Enderite Block ---
                offerReversibleCompactingRecipes(RecipeCategory.BUILDING_BLOCKS, ModItems.ENDERITE_INGOT, RecipeCategory.DECORATIONS, ModBlocks.ENDERITE_BLOCK);

                // --- 5. Duplizierung des Templates ---
                createShaped(RecipeCategory.MISC, ModItems.ENDERITE_UPGRADE_TEMPLATE, 2)
                        .pattern("ATA")
                        .pattern("AEA")
                        .pattern("AAA")
                        .input('A', Items.DIAMOND) // 7 Diamonds
                        .input('E', Items.END_STONE) // End Stone
                        .input('T', ModItems.ENDERITE_UPGRADE_TEMPLATE)
                        .criterion(hasItem(ModItems.ENDERITE_UPGRADE_TEMPLATE), conditionsFromItem(ModItems.ENDERITE_UPGRADE_TEMPLATE)).offerTo(exporter);

                // --- 6. Smithing Upgrades (Netherite -> Enderite) ---
                List<Item> netheriteItems = List.of(
                        Items.NETHERITE_SWORD, Items.NETHERITE_PICKAXE, Items.NETHERITE_AXE, Items.NETHERITE_SHOVEL, Items.NETHERITE_HOE,
                        Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS,
                        ModItems.NETHERITE_CORE, ModItems.NETHERITE_CHISEL, ModItems.NETHERITE_BUILDING_WAND, ModItems.NETHERITE_SLEDGEHAMMER
                );
                List<Item> enderiteItems = List.of(
                        ModItems.ENDERITE_SWORD, ModItems.ENDERITE_PICKAXE, ModItems.ENDERITE_AXE, ModItems.ENDERITE_SHOVEL, ModItems.ENDERITE_HOE,
                        ModItems.ENDERITE_HELMET, ModItems.ENDERITE_CHESTPLATE, ModItems.ENDERITE_LEGGINGS, ModItems.ENDERITE_BOOTS,
                        ModItems.ENDERITE_CORE, ModItems.ENDERITE_CHISEL, ModItems.ENDERITE_BUILDING_WAND, ModItems.ENDERITE_SLEDGEHAMMER
                );

                for (int i = 0; i < netheriteItems.size(); i++) {
                    createSmithingTransform(exporter,
                            ModItems.ENDERITE_UPGRADE_TEMPLATE,
                            netheriteItems.get(i),
                            ModItems.ENDERITE_INGOT,
                            RecipeCategory.COMBAT, // Kategorie ggf. anpassen je nach Item
                            enderiteItems.get(i)
                    );
                }

                // --- POLISHED END STONE ---
                createShaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.POLISHED_END_STONE, 4)
                        .pattern("SS")
                        .pattern("SS")
                        .input('S', Items.END_STONE)
                        .criterion(hasItem(Items.END_STONE), conditionsFromItem(Items.END_STONE))
                        .offerTo(exporter);

                // --- CHECKER BLOCKS ---
                createCheckerRecipe(exporter, ModBlocks.PURPUR_QUARTZ_CHECKER, Items.PURPUR_BLOCK);
                createCheckerRecipe(exporter, ModBlocks.LAPIS_QUARTZ_CHECKER, Items.LAPIS_BLOCK);
                createCheckerRecipe(exporter, ModBlocks.BLACKSTONE_QUARTZ_CHECKER, Items.BLACKSTONE);
                // Resin Placeholder (z.B. Red Nether Bricks)
                createCheckerRecipe(exporter, ModBlocks.RESIN_QUARTZ_CHECKER, Items.RED_NETHER_BRICKS);

                // --- ASTRAL / NIHIL BLOCKS (8 Block + 1 Powder/Shard) ---
                createCoatingRecipe(exporter, ModBlocks.ASTRAL_PURPUR_BLOCK, Items.PURPUR_BLOCK, ModItems.ASTRALIT_DUST);
                createCoatingRecipe(exporter, ModBlocks.NIHIL_PURPUR_BLOCK, Items.PURPUR_BLOCK, ModItems.NIHILITH_SHARD);
                createCoatingRecipe(exporter, ModBlocks.ASTRAL_END_STONE, ModBlocks.POLISHED_END_STONE, ModItems.ASTRALIT_DUST);
                createCoatingRecipe(exporter, ModBlocks.NIHIL_END_STONE, ModBlocks.POLISHED_END_STONE, ModItems.NIHILITH_SHARD);

                // --- GRAVITY BLOCKS ---
                // Nihilith -> No Gravity (Suspended)
                createCoatingRecipe(exporter, ModBlocks.SUSPENDED_SAND, Items.SAND, ModItems.NIHILITH_SHARD);
                createCoatingRecipe(exporter, ModBlocks.SUSPENDED_GRAVEL, Items.GRAVEL, ModItems.NIHILITH_SHARD);

                // Astralit -> Reverse Gravity (Levitating/Upwards)
                createCoatingRecipe(exporter, ModBlocks.LEVITATING_SAND, Items.SAND, ModItems.ASTRALIT_DUST);
                createCoatingRecipe(exporter, ModBlocks.LEVITATING_GRAVEL, Items.GRAVEL, ModItems.ASTRALIT_DUST);

                // --- ENDERITE ITEMS (Smithing Upgrades) ---
                // Bundle
                createSmithingTransform(exporter, ModItems.ENDERITE_UPGRADE_TEMPLATE, ModItems.NETHERITE_BUNDLE, ModItems.ENDERITE_INGOT, RecipeCategory.TOOLS, ModItems.ENDERITE_BUNDLE);
                // Quiver
                createSmithingTransform(exporter, ModItems.ENDERITE_UPGRADE_TEMPLATE, ModItems.NETHERITE_QUIVER, ModItems.ENDERITE_INGOT, RecipeCategory.TOOLS, ModItems.ENDERITE_QUIVER);

                // Food (Crafting, da man Essen normalerweise nicht schmiedet, aber du wolltest "like Netheite Variants")
                // Netherite Apple ist meist Crafting (Gold Apple + Netherite Ingot).
                createShapeless(RecipeCategory.FOOD, ModItems.ENDERITE_APPLE)
                        .input(ModItems.NETHERITE_APPLE)
                        .input(ModItems.ENDERITE_INGOT)
                        .criterion(hasItem(ModItems.ENDERITE_INGOT), conditionsFromItem(ModItems.ENDERITE_INGOT))
                        .offerTo(exporter);

                createShapeless(RecipeCategory.FOOD, ModItems.ENDERITE_CARROT)
                        .input(ModItems.NETHERITE_CARROT)
                        .input(ModItems.ENDERITE_INGOT)
                        .criterion(hasItem(ModItems.ENDERITE_INGOT), conditionsFromItem(ModItems.ENDERITE_INGOT))
                        .offerTo(exporter);
            }

            // Helper für Checker (4 Base + 4 Quartz)
            private void createCheckerRecipe(RecipeExporter exporter, ItemConvertible output, ItemConvertible base) {
                createShaped(RecipeCategory.BUILDING_BLOCKS, output, 4)
                        .pattern("BQ")
                        .pattern("QB")
                        .input('B', base)
                        .input('Q', Items.QUARTZ_BLOCK)
                        .criterion(hasItem(base), conditionsFromItem(base))
                        .offerTo(exporter);
            }

            // Helper für Coating (8 Base + 1 Material)
            private void createCoatingRecipe(RecipeExporter exporter, ItemConvertible output, ItemConvertible base, ItemConvertible material) {
                createShaped(RecipeCategory.BUILDING_BLOCKS, output, 8)
                        .pattern("BBB")
                        .pattern("BMB")
                        .pattern("BBB")
                        .input('B', base)
                        .input('M', material)
                        .criterion(hasItem(material), conditionsFromItem(material))
                        .offerTo(exporter);
            }

            // --- Helpers ---
            private void createSmithingTransform(RecipeExporter exporter, Item template, Item base, Item addition, RecipeCategory category, Item result) {
                SmithingTransformRecipeJsonBuilder.create(
                                Ingredient.ofItems(template),
                                Ingredient.ofItems(base),
                                Ingredient.ofItems(addition),
                                category,
                                result
                        )
                        .criterion(hasItem(addition), conditionsFromItem(addition))
                        .offerTo(exporter, getItemPath(result) + "_smithing");
            }

            private void createChiselRecipe(Item output, Item material, Item nugget) {
                createShaped(RecipeCategory.TOOLS, output)
                        .pattern("   ")
                        .pattern("NM ")
                        .pattern("SN ")
                        .input('M', material)
                        .input('S', Items.STICK)
                        .input('N', nugget)
                        .criterion(hasItem(material), conditionsFromItem(material))
                        .offerTo(exporter);
            }

            private void createSpatulaRecipe(Item output, Item material, Item nugget) {
                createShaped(RecipeCategory.TOOLS, output)
                        .pattern("   ")
                        .pattern("SN ")
                        .pattern(" M ")
                        .input('M', material)
                        .input('S', Items.STICK)
                        .input('N', nugget)
                        .criterion(hasItem(material), conditionsFromItem(material))
                        .offerTo(exporter);
            }

            private void createCoreRecipe(Item output, Item material) {
                createShaped(RecipeCategory.MISC, output)
                        .pattern(" M ")
                        .pattern("MNM")
                        .pattern(" M ")
                        .input('M', material)
                        .input('N', Items.NETHER_STAR)
                        .criterion(hasItem(Items.NETHER_STAR), conditionsFromItem(Items.NETHER_STAR))
                        .offerTo(exporter, getItemPath(output) + "_plus");
            }

            private void createWandRecipe(Item output, Item core, Item material) {
                createShaped(RecipeCategory.TOOLS, output)
                        .pattern("  C")
                        .pattern(" S ")
                        .pattern("S  ")
                        .input('C', core)
                        .input('S', Items.STICK)
                        .criterion(hasItem(core), conditionsFromItem(core))
                        .offerTo(exporter);
            }

            private void createSledgehammerRecipe(Item output, Item material, Item block) {
                createShaped(RecipeCategory.TOOLS, output)
                        .pattern("BMM")
                        .pattern(" S ")
                        .pattern(" S ")
                        .input('M', material)
                        .input('B', block)
                        .input('S', Items.STICK)
                        .criterion(hasItem(material), conditionsFromItem(material))
                        .offerTo(exporter);
            }

            private void createSmithing(Item input, Item result, RecipeCategory category) {
                SmithingTransformRecipeJsonBuilder.create(Ingredient.ofItems(
                        Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                        Ingredient.ofItems(input),
                        Ingredient.ofItems(Items.NETHERITE_INGOT),
                        category,
                        result)
                        .criterion("has_netherite_ingot", conditionsFromItem(Items.NETHERITE_INGOT))
                        .offerTo(exporter, getItemPath(result) + "_smithing");
            }

            // NEU: Helper für Massen-Upgrade (8 Items + 1 Ingot -> 8 Items)
            private void createBulkUpgrade(Item input, Item result, RecipeCategory category) {
                ShapedRecipeJsonBuilder.create(registries.getOrThrow(RegistryKeys.ITEM), category, result, 8)
                        .pattern("NR")
                        .pattern("RR")
                        .input('R', input)
                        .input('N', ModItems.NETHERITE_NUGGET)
                        .criterion(hasItem(input), conditionsFromItem(input))
                        .offerTo(exporter, getItemPath(result) + "_bulk");
            }
        };
    }

    private void createUpgradeRecipe(RecipeExporter exporter, Item base, Item result, Item material, int count) {
        Identifier recipeId = Identifier.of(Simplebuilding.MOD_ID, "upgrade_" + getItemName(base) + "_to_" + getItemName(result));

        // In 1.21.2+ braucht 'accept' einen RegistryKey
        RegistryKey<Recipe<?>> recipeKey = RegistryKey.of(RegistryKeys.RECIPE, recipeId);

        CountBasedSmithingRecipe recipe = new CountBasedSmithingRecipe(
                Ingredient.ofItems(ModItems.BASIC_UPGRADE_TEMPLATE),
                Ingredient.ofItems(base),
                Ingredient.ofItems(material),
                new ItemStack(result),
                count
        );

        exporter.accept(recipeKey, recipe, exporter.getAdvancementBuilder()
                .criterion("has_template", InventoryChangedCriterion.Conditions.items(ModItems.BASIC_UPGRADE_TEMPLATE))
                .build(Identifier.of(Simplebuilding.MOD_ID, "recipes/misc/" + recipeId.getPath())));
    }

    private String getItemName(Item item) {
        return Registries.ITEM.getId(item).getPath();
    }

    @Override
    public String getName() {
        return "SimpleBuilding Recipes";
    }

    private Item getDyeItem(DyeColor color) {
        return switch (color) {
            case WHITE -> Items.WHITE_DYE;
            case ORANGE -> Items.ORANGE_DYE;
            case MAGENTA -> Items.MAGENTA_DYE;
            case LIGHT_BLUE -> Items.LIGHT_BLUE_DYE;
            case YELLOW -> Items.YELLOW_DYE;
            case LIME -> Items.LIME_DYE;
            case PINK -> Items.PINK_DYE;
            case GRAY -> Items.GRAY_DYE;
            case LIGHT_GRAY -> Items.LIGHT_GRAY_DYE;
            case CYAN -> Items.CYAN_DYE;
            case PURPLE -> Items.PURPLE_DYE;
            case BLUE -> Items.BLUE_DYE;
            case BROWN -> Items.BROWN_DYE;
            case GREEN -> Items.GREEN_DYE;
            case RED -> Items.RED_DYE;
            case BLACK -> Items.BLACK_DYE;
        };
    }
}

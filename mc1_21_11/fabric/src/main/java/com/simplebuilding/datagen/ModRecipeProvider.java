package com.simplebuilding.datagen;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.recipe.CountBasedSmithingRecipe;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
// MC 1.21.11: Die Kriterien-Trigger liegen noch im alten Paket
// net.minecraft.advancements.criterion (erst 26.2 verschiebt sie nach ...advancements.triggers).
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
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
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
// MC 1.21.11: Das Gegenstueck zu ItemStackTemplate (26.2) heisst hier TransmuteResult und
// traegt exakt dieselben drei Felder (Holder<Item>, count, DataComponentPatch).
import net.minecraft.world.item.crafting.TransmuteResult;
import net.minecraft.world.level.ItemLike;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends FabricRecipeProvider {

    // WICHTIG: Diesen Tag manuell definieren, da er in 1.21.2+ Code fehlt
    private static final TagKey<Item> TRIM_TEMPLATES = TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("trim_templates"));

    public ModRecipeProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider wrapperLookup, RecipeOutput recipeExporter) {
        return new RecipeProvider(wrapperLookup, recipeExporter) {
            @Override
            public void buildRecipes() {

                // ---------------------------------------------------------
                // WICHTIG: Registry Zugriff für Tags vorbereiten (für 1.21.2+)
                // ---------------------------------------------------------
                HolderLookup.RegistryLookup<Item> itemRegistry = registries.lookupOrThrow(Registries.ITEM);

                // =================================================================
                // FIX: DUMMY REZEPT FÜR SCHMIEDETISCH (Glowing Ink)
                // =================================================================
                // Wir erstellen Ingredients über die Registry (ofTag statt fromTag)
                Ingredient templateIngredient = Ingredient.of(itemRegistry.getOrThrow(TRIM_TEMPLATES));
                Ingredient armorIngredient = Ingredient.of(itemRegistry.getOrThrow(ItemTags.TRIMMABLE_ARMOR));

                SmithingTransformRecipeBuilder.smithing(
                        templateIngredient,                     // Slot 1: Jedes Template (damit auch deins geht)
                        armorIngredient,                        // Slot 2: Rüstung
                        Ingredient.of(Items.GLOW_INK_SAC), // Slot 3: Leuchttinte
                        RecipeCategory.MISC,
                        ModItems.GLOWING_TRIM_TEMPLATE          // Dummy Output (wird vom Mixin überschrieben)
                )
                .unlocks("has_glowing_template", has(ModItems.GLOWING_TRIM_TEMPLATE))
                .save(output, "glowing_armor_upgrade_dummy");

                SmithingTransformRecipeBuilder.smithing(
                        templateIngredient,                     // Slot 1: Jedes Template (damit auch deins geht)
                        armorIngredient,                        // Slot 2: Rüstung
                        Ingredient.of(Items.GLOWSTONE_DUST), // Slot 3: Leuchttinte
                        RecipeCategory.MISC,
                        ModItems.EMITTING_TRIM_TEMPLATE          // Dummy Output (wird vom Mixin überschrieben)
                )
                .unlocks("has_emitting_template", has(ModItems.EMITTING_TRIM_TEMPLATE))
                .save(output, "emitting_armor_upgrade_dummy");


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
                // MC 1.21.11: Items.COPPER_BLOCK ist noch ein einfaches Item (die
                // WeatheringCopperCollection kommt erst mit 26.2); es ist dasselbe
                // minecraft:copper_block wie dort weathering().unaffected().
                createSledgehammerRecipe(ModItems.COPPER_SLEDGEHAMMER, Items.COPPER_INGOT, Items.COPPER_BLOCK);
                createSledgehammerRecipe(ModItems.IRON_SLEDGEHAMMER, Items.IRON_INGOT, Items.IRON_BLOCK);
                createSledgehammerRecipe(ModItems.GOLD_SLEDGEHAMMER, Items.GOLD_INGOT, Items.GOLD_BLOCK);
                createSledgehammerRecipe(ModItems.DIAMOND_SLEDGEHAMMER, Items.DIAMOND, Items.DIAMOND_BLOCK);
                createSmithing(ModItems.DIAMOND_SLEDGEHAMMER, ModItems.NETHERITE_SLEDGEHAMMER, RecipeCategory.TOOLS);


                // =================================================================
                // RANGEFINDER (Antik / Octant Style)
                // =================================================================
                shaped(RecipeCategory.TOOLS, ModItems.OCTANT)
                        .pattern(" GL")
                        .pattern("IPG")
                        .pattern("CI ")
                        .define('I', Items.GOLD_INGOT)
                        .define('G', Items.GOLD_NUGGET)
                        .define('C', Items.COPPER_INGOT)
                        .define('P', Items.COMPASS)
                        .define('L', Items.LEAD)
                        .unlockedBy(getHasName(Items.COPPER_INGOT), has(Items.COPPER_INGOT))
                        .save(output);
                for (DyeColor color : DyeColor.values()) {
                    Item resultItem = ModItems.COLORED_OCTANT_ITEMS.get(color);
                    Item dyeItem = getDyeItem(color);

                    if (resultItem != null && dyeItem != null) {

                        ShapelessRecipeBuilder.shapeless(registries.lookupOrThrow(Registries.ITEM), RecipeCategory.TOOLS, resultItem)
                                .requires(ModItems.OCTANT)
                                .requires(dyeItem)
                                .unlockedBy(getHasName(dyeItem), has(dyeItem))
                                .save(output, getSimpleRecipeName(resultItem) + "_from_dye");
                    }
                }

                // =================================================================
                // VELOCITY_GAUGE
                // =================================================================
                shaped(RecipeCategory.TOOLS, ModItems.VELOCITY_GAUGE)
                        .pattern(" A ")
                        .pattern("OCO")
                        .pattern("QQQ")
                        .define('C', Items.COMPASS)
                        .define('A', Items.AMETHYST_SHARD)
                        .define('O', Items.COPPER_INGOT)
                        .define('Q', Items.QUARTZ)
                        .unlockedBy(getHasName(Items.COMPASS), has(Items.COMPASS))
                        .save(output);

                // =================================================================
                // MAGNET
                // =================================================================
                shaped(RecipeCategory.TOOLS, ModItems.MAGNET)
                        .pattern(" IR")
                        .pattern("ILI")
                        .pattern("BI ")
                        .define('L', Items.LODESTONE)
                        .define('I', Items.IRON_INGOT)
                        .define('R', Items.REDSTONE)
                        .define('B', Items.LAPIS_LAZULI)
                        .unlockedBy(getHasName(Items.LODESTONE), has(Items.LODESTONE))
                        .save(output);

                shaped(RecipeCategory.TOOLS, ModItems.ROTATOR)
                        .pattern(" I ")
                        .pattern("IEI")
                        .pattern("II ")
                        .define('I', Items.IRON_INGOT)
                        .define('E', Items.ENDER_PEARL)
                        .unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT))
                        .save(output);

                // =================================================================
                // REINFORCED BUNDLE
                // =================================================================
                shaped(RecipeCategory.TOOLS, ModItems.REINFORCED_BUNDLE)
                        .pattern(" S ")
                        .pattern("NBN")
                        .pattern("LLL")
                        .define('S', Items.STRING)
                        .define('N', Items.COPPER_NUGGET)
                        .define('B', Items.BUNDLE)
                        .define('L', Items.LEATHER)
                        .unlockedBy( getHasName(Items.BUNDLE), has(Items.BUNDLE))
                        .save(output);


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

                createSmithing(ModItems.QUIVER, ModItems.NETHERITE_QUIVER, RecipeCategory.TOOLS);


                // =================================================================
                // ORE DETECTOR
                // =================================================================
                shaped(RecipeCategory.TOOLS, ModItems.ORE_DETECTOR)
                        .pattern(" S ")
                        .pattern(" C ")
                        .pattern(" G ")
                        .define('C', Items.COMPASS)
                        .define('G', ModItems.GOLD_CORE)
                        .define('S', Items.CALIBRATED_SCULK_SENSOR)
                        .unlockedBy(getHasName(Items.COMPASS), has(Items.COMPASS))
                        .save(output);


                shaped(RecipeCategory.MISC, ModItems.CRACKED_DIAMOND)
                        .pattern("PPP")
                        .pattern("PPP")
                        .pattern("PPP")
                        .define('P', ModItems.DIAMOND_PEBBLE)
                        .unlockedBy(getHasName(ModItems.DIAMOND_PEBBLE), has(ModItems.DIAMOND_PEBBLE))
                        .save(output);

                // MC 1.21.11: oreBlasting nimmt noch keine CookingBookCategory entgegen; sie wird
                // intern per determineBlastingRecipeCategory(result) bestimmt und ist fuer ein
                // Nicht-BlockItem-Ergebnis (Items.DIAMOND) genau CookingBookCategory.MISC - also
                // dasselbe, was die 26.2-Fassung explizit uebergibt.
                oreBlasting(java.util.List.of(ModItems.CRACKED_DIAMOND), RecipeCategory.MISC, Items.DIAMOND, 1.0f, 100, "diamond_from_cracked");


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
                ShapedRecipeBuilder.shaped(registries.lookupOrThrow(Registries.ITEM), RecipeCategory.REDSTONE, ModItems.REINFORCED_HOPPER, 5)
                        .pattern("HNH")
                        .pattern("DDD")
                        .pattern("HHH")
                        .define('D', ModItems.CRACKED_DIAMOND)
                        .define('H', Items.HOPPER)
                        .define('N', Items.NAME_TAG)
                        .unlockedBy(getHasName(Items.HOPPER), has(Items.HOPPER))
                        .save(output, "reinforced_hopper_from_crafting");

                ShapedRecipeBuilder.shaped(registries.lookupOrThrow(Registries.ITEM), RecipeCategory.REDSTONE, ModItems.NETHERITE_HOPPER, 2)
                        .pattern("H")
                        .pattern("N")
                        .pattern("H")
                        .define('H', ModItems.REINFORCED_HOPPER)
                        .define('N', ModItems.NETHERITE_NUGGET)
                        .unlockedBy(getHasName(ModItems.REINFORCED_HOPPER), has(ModItems.REINFORCED_HOPPER))
                        .save(output, "netherite_hopper_from_crafting");


                // =================================================================
                // PISTON REINFORCED & NETHERITE
                // =================================================================
                ShapedRecipeBuilder.shaped(registries.lookupOrThrow(Registries.ITEM), RecipeCategory.REDSTONE, ModItems.REINFORCED_PISTON, 2)
                        .pattern("DDD")
                        .pattern("PIP")
                        .pattern("III")
                        .define('D', ModItems.CRACKED_DIAMOND)
                        .define('P', Items.PISTON)
                        .define('I', Items.IRON_INGOT)
                        .unlockedBy(getHasName(Items.PISTON), has(Items.PISTON))
                        .save(output);
                createBulkUpgrade(ModItems.REINFORCED_PISTON, ModItems.NETHERITE_PISTON, RecipeCategory.REDSTONE);


                // =================================================================
                // BLAST FURNACE REINFORCED & NETHERITE
                // =================================================================
                ShapedRecipeBuilder.shaped(registries.lookupOrThrow(Registries.ITEM), RecipeCategory.REDSTONE, ModItems.REINFORCED_BLAST_FURNACE, 3)
                        .pattern("DDD")
                        .pattern("BBB")
                        .pattern("DDD")
                        .define('D', ModItems.CRACKED_DIAMOND)
                        .define('B', Items.BLAST_FURNACE)
                        .unlockedBy(getHasName(Items.BLAST_FURNACE), has(Items.BLAST_FURNACE))
                        .save(output);
                createBulkUpgrade(ModItems.REINFORCED_BLAST_FURNACE, ModItems.NETHERITE_BLAST_FURNACE, RecipeCategory.DECORATIONS);


                // =================================================================
                // FURNACE REINFORCED & NETHERITE
                // =================================================================
                ShapedRecipeBuilder.shaped(registries.lookupOrThrow(Registries.ITEM), RecipeCategory.REDSTONE, ModItems.REINFORCED_FURNACE, 3)
                        .pattern("DDD")
                        .pattern("FFF")
                        .pattern("DDD")
                        .define('D', ModItems.CRACKED_DIAMOND)
                        .define('F', Items.FURNACE)
                        .unlockedBy(getHasName(Items.FURNACE), has(Items.FURNACE))
                        .save(output);
                createBulkUpgrade(ModItems.REINFORCED_FURNACE, ModItems.NETHERITE_FURNACE, RecipeCategory.DECORATIONS);


                // =================================================================
                // SMOKER REINFORCED & NETHERITE
                // =================================================================
                ShapedRecipeBuilder.shaped(registries.lookupOrThrow(Registries.ITEM), RecipeCategory.REDSTONE, ModItems.REINFORCED_SMOKER, 3)
                        .pattern("DDD")
                        .pattern("SSS")
                        .pattern("DDD")
                        .define('D', ModItems.CRACKED_DIAMOND)
                        .define('S', Items.SMOKER)
                        .unlockedBy(getHasName(Items.SMOKER), has(Items.SMOKER))
                        .save(output);
                createBulkUpgrade(ModItems.REINFORCED_SMOKER, ModItems.NETHERITE_SMOKER, RecipeCategory.DECORATIONS);

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

                // Spitzhacken (Crafting: 3 -> Upgrade: 4)
                createUpgradeRecipe(registries, output, Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.COBBLESTONE, 4);
                createUpgradeRecipe(registries, output, Items.STONE_PICKAXE, Items.IRON_PICKAXE, Items.IRON_INGOT, 4);
                createUpgradeRecipe(registries, output, Items.IRON_PICKAXE, Items.GOLDEN_PICKAXE, Items.GOLD_INGOT, 4);
                createUpgradeRecipe(registries, output, Items.GOLDEN_PICKAXE, Items.DIAMOND_PICKAXE, Items.DIAMOND, 4);
                createUpgradeRecipe(registries, output, Items.COPPER_PICKAXE, Items.IRON_PICKAXE, Items.IRON_INGOT, 4);

                // Äxte (Crafting: 3 -> Upgrade: 4)
                createUpgradeRecipe(registries, output, Items.WOODEN_AXE, Items.STONE_AXE, Items.COBBLESTONE, 4);
                createUpgradeRecipe(registries, output, Items.STONE_AXE, Items.IRON_AXE, Items.IRON_INGOT, 4);
                createUpgradeRecipe(registries, output, Items.IRON_AXE, Items.GOLDEN_AXE, Items.GOLD_INGOT, 4);
                createUpgradeRecipe(registries, output, Items.GOLDEN_AXE, Items.DIAMOND_AXE, Items.DIAMOND, 4);

                // Schwerter / Hacken (Crafting: 2 -> Upgrade: 3)
                createUpgradeRecipe(registries, output, Items.WOODEN_SWORD, Items.STONE_SWORD, Items.COBBLESTONE, 3);
                createUpgradeRecipe(registries, output, Items.STONE_SWORD, Items.IRON_SWORD, Items.IRON_INGOT, 3);
                createUpgradeRecipe(registries, output, Items.IRON_SWORD, Items.GOLDEN_SWORD, Items.GOLD_INGOT, 3);
                createUpgradeRecipe(registries, output, Items.GOLDEN_SWORD, Items.DIAMOND_SWORD, Items.DIAMOND, 3);

                // Schaufeln (Crafting: 1 -> Upgrade: 2)
                createUpgradeRecipe(registries, output, Items.WOODEN_SHOVEL, Items.STONE_SHOVEL, Items.COBBLESTONE, 2);
                createUpgradeRecipe(registries, output, Items.STONE_SHOVEL, Items.IRON_SHOVEL, Items.IRON_INGOT, 2);
                createUpgradeRecipe(registries, output, Items.IRON_SHOVEL, Items.GOLDEN_SHOVEL, Items.GOLD_INGOT, 2);
                createUpgradeRecipe(registries, output, Items.GOLDEN_SHOVEL, Items.DIAMOND_SHOVEL, Items.DIAMOND, 2);

                // Mod Tools (Beispiele)
                // Chisel (Crafting: 1 -> Upgrade: 2)
                createUpgradeRecipe(registries, output, ModItems.COPPER_CHISEL, ModItems.IRON_CHISEL, Items.IRON_INGOT, 2);
                createUpgradeRecipe(registries, output, ModItems.IRON_CHISEL, ModItems.GOLD_CHISEL, Items.GOLD_INGOT, 2);
                createUpgradeRecipe(registries, output, ModItems.GOLD_CHISEL, ModItems.DIAMOND_CHISEL, Items.DIAMOND, 2);

                // Sledgehammer (Crafting: 5 -> Upgrade: 6)
                createUpgradeRecipe(registries, output, ModItems.COPPER_SLEDGEHAMMER, ModItems.IRON_SLEDGEHAMMER, Items.IRON_INGOT, 12);
                createUpgradeRecipe(registries, output, ModItems.IRON_SLEDGEHAMMER, ModItems.GOLD_SLEDGEHAMMER, Items.GOLD_INGOT, 12);
                createUpgradeRecipe(registries, output, ModItems.GOLD_SLEDGEHAMMER, ModItems.DIAMOND_SLEDGEHAMMER, Items.DIAMOND, 12);



                // --- 1. Synthese: Raw Enderite (4 Shards + 4 Dust + 1 Pearl) ---
                // Shapeless Rezept
                shapeless(RecipeCategory.MISC, ModItems.RAW_ENDERITE)
                        .requires(ModItems.NIHILITH_SHARD, 4)
                        .requires(ModItems.ASTRALIT_DUST, 4)
                        .requires(Items.ENDER_PEARL)
                        .unlockedBy(getHasName(ModItems.NIHILITH_SHARD), has(ModItems.NIHILITH_SHARD))
                        .unlockedBy(getHasName(ModItems.ASTRALIT_DUST), has(ModItems.ASTRALIT_DUST))
                        .save(output, "raw_enderite_synthesis");

                // --- 2. Schmelzen: Raw -> Scrap ---
                // Siehe oben: ENDERITE_SCRAP ist ein einfaches Item, daher liefert
                // determineBlastingRecipeCategory(...) wieder CookingBookCategory.MISC.
                oreBlasting(List.of(ModItems.RAW_ENDERITE), RecipeCategory.MISC, ModItems.ENDERITE_SCRAP, 2.0f, 200, "enderite_scrap");

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

                // --- ASTRAL / NIHIL BLOCKS (8 Block + 1 Powder/Shard) ---
                createCoatingRecipe(output, ModBlocks.ASTRAL_PURPUR_BLOCK, Items.PURPUR_BLOCK, ModItems.ASTRALIT_DUST);
                createCoatingRecipe(output, ModBlocks.NIHIL_PURPUR_BLOCK, Items.PURPUR_BLOCK, ModItems.NIHILITH_SHARD);
                createCoatingRecipe(output, ModBlocks.ASTRAL_END_STONE, ModBlocks.POLISHED_END_STONE, ModItems.ASTRALIT_DUST);
                createCoatingRecipe(output, ModBlocks.NIHIL_END_STONE, ModBlocks.POLISHED_END_STONE, ModItems.NIHILITH_SHARD);

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

            // Helper für Checker (4 Base + 4 Quartz)
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

            // NEU: Helper für Massen-Upgrade (8 Items + 1 Ingot -> 8 Items)
            private void createBulkUpgrade(Item input, Item result, RecipeCategory category) {
                ShapedRecipeBuilder.shaped(registries.lookupOrThrow(Registries.ITEM), category, result, 3)
                        .pattern("NR")
                        .pattern("RR")
                        .define('R', input)
                        .define('N', ModItems.NETHERITE_NUGGET)
                        .unlockedBy(getHasName(input), has(input))
                        .save(output, getItemName(result) + "_bulk");
            }
        };
    }

    private void createUpgradeRecipe(HolderLookup.Provider registries, RecipeOutput exporter, Item base, Item result, Item material, int count) {
        Identifier recipeId = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "upgrade_" + getItemName(base) + "_to_" + getItemName(result));

        ResourceKey<Recipe<?>> recipeKey = ResourceKey.create(Registries.RECIPE, recipeId);

        ResourceKey<Item> resultKey = BuiltInRegistries.ITEM.getResourceKey(result).orElseThrow();
        TransmuteResult resultTemplate = new TransmuteResult(
                registries.lookupOrThrow(Registries.ITEM).getOrThrow(resultKey),
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

        exporter.accept(recipeKey, recipe, exporter.advancement()
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

    // MC 1.21.11: Items.DYE (ColorCollection) existiert noch nicht; die Farbvarianten sind
    // weiterhin 16 Einzel-Items. DyeItem.byColor(...) liefert exakt dieselbe Zuordnung wie
    // spaeter Items.DYE.pick(...), also verhaltensgleich.
    private Item getDyeItem(DyeColor color) {
        return DyeItem.byColor(color);
    }
}

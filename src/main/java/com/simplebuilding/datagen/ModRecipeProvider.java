package com.simplebuilding.datagen;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.items.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.data.recipe.*;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends FabricRecipeProvider {
    public ModRecipeProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected RecipeGenerator getRecipeGenerator(RegistryWrapper.WrapperLookup wrapperLookup, RecipeExporter recipeExporter) {
        return new RecipeGenerator(wrapperLookup, recipeExporter) {
            @Override
            public void generate() {

                // =================================================================
                // CHISELS (Stick + Material + Nugget/Shard) - DIAGONAL
                // =================================================================
                createChiselRecipe(ModItems.STONE_CHISEL, Items.STONE, Items.IRON_NUGGET);
                createChiselRecipe(ModItems.COPPER_CHISEL, Items.COPPER_INGOT, Items.COPPER_INGOT); // Kupfer hat keine Nuggets -> Ingot
                createChiselRecipe(ModItems.IRON_CHISEL, Items.IRON_INGOT, Items.IRON_NUGGET);
                createChiselRecipe(ModItems.GOLD_CHISEL, Items.GOLD_INGOT, Items.GOLD_NUGGET);
                createChiselRecipe(ModItems.DIAMOND_CHISEL, Items.DIAMOND, Items.DIAMOND); // Diamant hat keine Nuggets

                // Netherite Chisel -> Smithing Upgrade
                SmithingTransformRecipeJsonBuilder.create(
                        Ingredient.ofItems(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                        Ingredient.ofItems(ModItems.DIAMOND_CHISEL),
                        Ingredient.ofItems(Items.NETHERITE_INGOT),
                        RecipeCategory.TOOLS,
                        ModItems.NETHERITE_CHISEL
                ).criterion("has_netherite_ingot", conditionsFromItem(Items.NETHERITE_INGOT))
                 .offerTo(exporter, getItemPath(ModItems.NETHERITE_CHISEL) + "_smithing");


                // =================================================================
                // SPATULAS (Stick + Material + Nugget/Shard) - ECKIG
                // =================================================================
                createSpatulaRecipe(ModItems.STONE_SPATULA, Items.STONE, Items.IRON_NUGGET);
                createSpatulaRecipe(ModItems.COPPER_SPATULA, Items.COPPER_INGOT, Items.COPPER_INGOT);
                createSpatulaRecipe(ModItems.IRON_SPATULA, Items.IRON_INGOT, Items.IRON_NUGGET);
                createSpatulaRecipe(ModItems.GOLD_SPATULA, Items.GOLD_INGOT, Items.GOLD_NUGGET);
                createSpatulaRecipe(ModItems.DIAMOND_SPATULA, Items.DIAMOND, Items.DIAMOND);

                // Netherite Spatula -> Smithing Upgrade
                SmithingTransformRecipeJsonBuilder.create(
                        Ingredient.ofItems(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                        Ingredient.ofItems(ModItems.DIAMOND_SPATULA),
                        Ingredient.ofItems(Items.NETHERITE_INGOT),
                        RecipeCategory.TOOLS,
                        ModItems.NETHERITE_SPATULA
                ).criterion("has_netherite_ingot", conditionsFromItem(Items.NETHERITE_INGOT))
                 .offerTo(exporter, getItemPath(ModItems.NETHERITE_SPATULA) + "_smithing");


                // =================================================================
                // BUILDING CORES (Material + Nether Star)
                // =================================================================
                createCoreRecipe(ModItems.COPPER_BUILDING_CORE, Items.COPPER_INGOT);
                createCoreRecipe(ModItems.IRON_BUILDING_CORE, Items.IRON_INGOT);
                createCoreRecipe(ModItems.GOLD_BUILDING_CORE, Items.GOLD_INGOT);
                createCoreRecipe(ModItems.DIAMOND_BUILDING_CORE, Items.DIAMOND);

                // Netherite Core -> Smithing Upgrade (Diamond Core + Netherite Ingot)
                SmithingTransformRecipeJsonBuilder.create(
                        Ingredient.ofItems(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                        Ingredient.ofItems(ModItems.DIAMOND_BUILDING_CORE),
                        Ingredient.ofItems(Items.NETHERITE_INGOT),
                        RecipeCategory.MISC,
                        ModItems.NETHERITE_BUILDING_CORE
                ).criterion("has_netherite_ingot", conditionsFromItem(Items.NETHERITE_INGOT))
                 .offerTo(exporter, getItemPath(ModItems.NETHERITE_BUILDING_CORE) + "_smithing");


                // =================================================================
                // BUILDING WANDS (Stick + Core + Material)
                // =================================================================
                createWandRecipe(ModItems.COPPER_BUILDING_WAND, ModItems.COPPER_BUILDING_CORE, Items.COPPER_INGOT);
                createWandRecipe(ModItems.IRON_BUILDING_WAND, ModItems.IRON_BUILDING_CORE, Items.IRON_INGOT);
                createWandRecipe(ModItems.GOLD_BUILDING_WAND, ModItems.GOLD_BUILDING_CORE, Items.GOLD_INGOT);
                createWandRecipe(ModItems.DIAMOND_BUILDING_WAND, ModItems.DIAMOND_BUILDING_CORE, Items.DIAMOND);
                createWandRecipe(ModItems.NETHERITE_BUILDING_WAND, ModItems.NETHERITE_BUILDING_CORE, Items.DIAMOND);

                // Netherite Wand -> Smithing Upgrade (Diamond Wand + Netherite Ingot)
                SmithingTransformRecipeJsonBuilder.create(
                        Ingredient.ofItems(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                        Ingredient.ofItems(ModItems.DIAMOND_BUILDING_WAND),
                        Ingredient.ofItems(Items.NETHERITE_INGOT),
                        RecipeCategory.TOOLS,
                        ModItems.NETHERITE_BUILDING_WAND
                ).criterion("has_netherite_ingot", conditionsFromItem(Items.NETHERITE_INGOT))
                 .offerTo(exporter, getItemPath(ModItems.NETHERITE_BUILDING_WAND) + "_smithing");


                // =================================================================
                // RANGEFINDER (Antik / Sextant Style)
                // =================================================================
                createShaped(RecipeCategory.TOOLS, ModItems.RANGEFINDER_ITEM)
                        .pattern(" GC")
                        .pattern("IPG")
                        .pattern(" IL")
                        .input('I', Items.GOLD_INGOT)
                        .input('G', Items.GOLD_NUGGET)
                        .input('C', Items.COPPER_INGOT)
                        .input('P', Items.COMPASS)
                        .input('L', Items.LEAD)
                        .criterion(hasItem(Items.COPPER_INGOT), conditionsFromItem(Items.COPPER_INGOT))
                        .offerTo(exporter);

                // Rangefinder Färben
                for (DyeColor color : DyeColor.values()) {
                    Item resultItem = ModItems.COLORED_RANGEFINDERS.get(color);
                    Item dyeItem = getDyeItem(color);

                    if (resultItem != null && dyeItem != null) {

                        ShapelessRecipeJsonBuilder.create(registries.getOrThrow(RegistryKeys.ITEM), RecipeCategory.TOOLS, resultItem)
                                .input(ModItems.RANGEFINDER_ITEM)
                                .input(dyeItem)
                                .criterion(hasItem(dyeItem), conditionsFromItem(dyeItem))
                                .offerTo(exporter, getRecipeName(resultItem) + "_from_dye");
                    }
                }

                // =================================================================
                // REINFORCED BUNDLE
                // =================================================================
                createShaped(RecipeCategory.TOOLS, ModItems.REINFORCED_BUNDLE)
                        .pattern(" S ")
                        .pattern("NBN")
                        .pattern("LLL")
                        .input('L', Items.LEATHER)
                        .input('B', Items.BUNDLE)
                        .input('N', Items.COPPER_NUGGET)
                        .input('S', Items.STRING)
                        .criterion(hasItem(Items.BUNDLE), conditionsFromItem(Items.BUNDLE))
                        .offerTo(exporter);
            }

            // --- Helper Methods to keep generate() clean ---

            private void createChiselRecipe(Item output, Item material, Item nugget) {
                createShaped(RecipeCategory.TOOLS, output)
                        .pattern("   ")
                        .pattern("NM ")
                        .pattern("SN ")
                        .input('M', material) // Oben Rechts
                        .input('S', Items.STICK) // Mitte
                        .input('N', nugget)   // Unten Links
                        .criterion(hasItem(material), conditionsFromItem(material))
                        .offerTo(exporter);
            }

            private void createSpatulaRecipe(Item output, Item material, Item nugget) {
                createShaped(RecipeCategory.TOOLS, output)
                        .pattern("   ")
                        .pattern("SN ")
                        .pattern(" M ")
                        .input('M', material) // Oben Mitte
                        .input('S', Items.STICK) // Mitte
                        .input('N', nugget)   // Unten Mitte
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
                        .offerTo(exporter);
            }

            private void createWandRecipe(Item output, Item core, Item material) {
                createShaped(RecipeCategory.TOOLS, output)
                        .pattern(" MC")
                        .pattern(" SM")
                        .pattern("S  ")
                        .input('C', core)
                        .input('S', Items.STICK)
                        .input('M', material)
                        .criterion(hasItem(core), conditionsFromItem(core))
                        .offerTo(exporter);
            }
        };
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

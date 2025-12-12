package com.simplebuilding.datagen;

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
                createChiselRecipe(ModItems.STONE_CHISEL, Items.COBBLESTONE, Items.COPPER_NUGGET);
                createChiselRecipe(ModItems.COPPER_CHISEL, Items.COPPER_INGOT, Items.COPPER_NUGGET); // Kupfer hat keine Nuggets -> Ingot
                createChiselRecipe(ModItems.IRON_CHISEL, Items.IRON_INGOT, Items.COPPER_NUGGET);
                createChiselRecipe(ModItems.GOLD_CHISEL, Items.GOLD_INGOT, Items.COPPER_NUGGET);
                createChiselRecipe(ModItems.DIAMOND_CHISEL, Items.DIAMOND, Items.COPPER_NUGGET); // Diamant hat keine Nuggets

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
                createSpatulaRecipe(ModItems.STONE_SPATULA, Items.COBBLESTONE, Items.COPPER_INGOT);
                createSpatulaRecipe(ModItems.COPPER_SPATULA, Items.COPPER_INGOT, Items.COPPER_INGOT);
                createSpatulaRecipe(ModItems.IRON_SPATULA, Items.IRON_INGOT, Items.COPPER_INGOT);
                createSpatulaRecipe(ModItems.GOLD_SPATULA, Items.GOLD_INGOT, Items.COPPER_INGOT);
                createSpatulaRecipe(ModItems.DIAMOND_SPATULA, Items.DIAMOND, Items.COPPER_INGOT);
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
                SmithingTransformRecipeJsonBuilder.create(
                        Ingredient.ofItems(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                        Ingredient.ofItems(ModItems.DIAMOND_BUILDING_WAND),
                        Ingredient.ofItems(Items.NETHERITE_INGOT),
                        RecipeCategory.TOOLS,
                        ModItems.NETHERITE_BUILDING_WAND
                ).criterion("has_netherite_ingot", conditionsFromItem(Items.NETHERITE_INGOT))
                 .offerTo(exporter, getItemPath(ModItems.NETHERITE_BUILDING_WAND) + "_smithing");

                // =================================================================
                // SLEDGEHAMMER
                // =================================================================
                createSledgehammerRecipe(ModItems.STONE_SLEDGEHAMMER, Items.COBBLESTONE, Items.IRON_INGOT);
                createSledgehammerRecipe(ModItems.COPPER_SLEDGEHAMMER, Items.COPPER_INGOT, Items.COPPER_BLOCK);
                createSledgehammerRecipe(ModItems.IRON_SLEDGEHAMMER, Items.IRON_INGOT, Items.IRON_BLOCK);
                createSledgehammerRecipe(ModItems.GOLD_SLEDGEHAMMER, Items.GOLD_INGOT, Items.GOLD_BLOCK);
                createSledgehammerRecipe(ModItems.DIAMOND_SLEDGEHAMMER, Items.DIAMOND, Items.DIAMOND_BLOCK);
                SmithingTransformRecipeJsonBuilder.create(
                        Ingredient.ofItems(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                        Ingredient.ofItems(ModItems.DIAMOND_SLEDGEHAMMER),
                        Ingredient.ofItems(Items.NETHERITE_INGOT),
                        RecipeCategory.TOOLS,
                        ModItems.NETHERITE_SLEDGEHAMMER
                ).criterion("has_netherite_ingot", conditionsFromItem(Items.NETHERITE_INGOT))
                 .offerTo(exporter, getItemPath(ModItems.NETHERITE_SLEDGEHAMMER) + "_smithing");



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

                SmithingTransformRecipeJsonBuilder.create(
                                Ingredient.ofItems(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                                Ingredient.ofItems(ModItems.REINFORCED_BUNDLE),
                                Ingredient.ofItems(Items.NETHERITE_INGOT),
                                RecipeCategory.TOOLS,
                                ModItems.NETHERITE_BUNDLE
                        ).criterion("has_netherite_ingot", conditionsFromItem(Items.NETHERITE_INGOT))
                        .offerTo(exporter, getItemPath(ModItems.NETHERITE_BUNDLE) + "_smithing");

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

                SmithingTransformRecipeJsonBuilder.create(
                                Ingredient.ofItems(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                                Ingredient.ofItems(ModItems.QUIVER),
                                Ingredient.ofItems(Items.NETHERITE_INGOT),
                                RecipeCategory.TOOLS,
                                ModItems.NETHERITE_QUIVER
                        ).criterion("has_netherite_ingot", conditionsFromItem(Items.NETHERITE_INGOT))
                        .offerTo(exporter, getItemPath(ModItems.NETHERITE_QUIVER) + "_smithing");
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
                        .offerTo(exporter, getItemPath(output) + "_plus");
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

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
                // REINFORCED BUNDLE
                // =================================================================
                java.util.Map<Character, Ingredient> bundleKeys = java.util.Map.of(
                        'L', Ingredient.ofItems(Items.LEATHER),
                        'B', Ingredient.ofItems(Items.BUNDLE),
                        'N', Ingredient.ofItems(Items.COPPER_NUGGET),
                        'S', Ingredient.ofItems(Items.STRING)
                );

                java.util.List<String> bundlePattern = java.util.List.of(
                        " S ",
                        "NBN",
                        "LLL"
                );

                net.minecraft.recipe.RawShapedRecipe rawRecipe = net.minecraft.recipe.RawShapedRecipe.create(bundleKeys, bundlePattern);

                com.simplebuilding.recipe.ReinforcedBundleRecipe customRecipe = new com.simplebuilding.recipe.ReinforcedBundleRecipe(
                        "",
                        net.minecraft.recipe.book.CraftingRecipeCategory.EQUIPMENT,
                        rawRecipe,
                        new net.minecraft.item.ItemStack(ModItems.REINFORCED_BUNDLE)
                );

                net.minecraft.util.Identifier recipeId = net.minecraft.util.Identifier.of(com.simplebuilding.Simplebuilding.MOD_ID, getRecipeName(ModItems.REINFORCED_BUNDLE));
                net.minecraft.registry.RegistryKey<net.minecraft.recipe.Recipe<?>> recipeKey = net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.RECIPE, recipeId);

                exporter.accept(
                        recipeKey,
                        customRecipe,
                        exporter.getAdvancementBuilder()
                                .criterion("has_bundle", conditionsFromItem(Items.BUNDLE))
                                .build(recipeId.withPrefixedPath("recipes/" + RecipeCategory.TOOLS.getName() + "/"))
                );

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

                /* todo chest:

                    // 2. Reinforced Chest (Chest + 8 Diamonds surround)
                    createShaped(RecipeCategory.DECORATIONS, ModItems.REINFORCED_CHEST)
                            .pattern("DDD").pattern("DCD").pattern("DDD")
                            .input('D', Items.DIAMOND).input('C', Items.CHEST)
                            .criterion(hasItem(Items.CHEST), conditionsFromItem(Items.CHEST)).offerTo(exporter);
                    // Netherite Chest (Smithing)
                    createSmithing(ModItems.REINFORCED_CHEST, ModItems.NETHERITE_CHEST, RecipeCategory.DECORATIONS);
                */

                // 1. Reinforced Hopper (Hopper + 4 Diamonds)
                createShaped(RecipeCategory.REDSTONE, ModItems.REINFORCED_HOPPER)
                        .pattern("DDD")
                        .pattern("DHD")
                        .pattern(" D ")
                        .input('D', ModItems.CRACKED_DIAMOND)
                        .input('H', Items.HOPPER)
                        .criterion(hasItem(Items.HOPPER), conditionsFromItem(Items.HOPPER)).offerTo(exporter);
                // Netherite Hopper (Smithing)
                createSmithing(ModItems.REINFORCED_HOPPER, ModItems.NETHERITE_HOPPER, RecipeCategory.REDSTONE);


                // 3. Reinforced Piston (Piston + Diamond)
                createShaped(RecipeCategory.REDSTONE, ModItems.REINFORCED_PISTON)
                        .pattern("DDD")
                        .pattern("IPI")
                        .pattern("III")
                        .input('D', ModItems.CRACKED_DIAMOND)
                        .input('P', Items.PISTON)
                        .input('I', Items.IRON_INGOT)
                        .criterion(hasItem(Items.PISTON), conditionsFromItem(Items.PISTON)).offerTo(exporter);
                // Netherite Piston (Smithing)
                createSmithing(ModItems.REINFORCED_PISTON, ModItems.NETHERITE_PISTON, RecipeCategory.REDSTONE);

                // 4. Reinforced Blast Furnace (Blast Furnace + Diamonds)
                createShaped(RecipeCategory.DECORATIONS, ModItems.REINFORCED_BLAST_FURNACE)
                        .pattern("DDD")
                        .pattern("DBD")
                        .pattern("DDD")
                        .input('D', ModItems.CRACKED_DIAMOND)
                        .input('B', Items.BLAST_FURNACE)
                        .criterion(hasItem(Items.BLAST_FURNACE), conditionsFromItem(Items.BLAST_FURNACE)).offerTo(exporter);
                // Netherite Blast Furnace (Smithing)
                createSmithing(ModItems.REINFORCED_BLAST_FURNACE, ModItems.NETHERITE_BLAST_FURNACE, RecipeCategory.DECORATIONS);

                // 4. Furnace
                createShaped(RecipeCategory.DECORATIONS, ModItems.REINFORCED_FURNACE)
                        .pattern("DDD").pattern("DFD").pattern("DDD")
                        .input('D', ModItems.CRACKED_DIAMOND).input('F', Items.FURNACE)
                        .criterion(hasItem(Items.FURNACE), conditionsFromItem(Items.FURNACE)).offerTo(exporter);
                createSmithing(ModItems.REINFORCED_FURNACE, ModItems.NETHERITE_FURNACE, RecipeCategory.DECORATIONS);

                // 5. Smoker
                createShaped(RecipeCategory.DECORATIONS, ModItems.REINFORCED_SMOKER)
                        .pattern("DDD").pattern("DSD").pattern("DDD")
                        .input('D', ModItems.CRACKED_DIAMOND).input('S', Items.SMOKER)
                        .criterion(hasItem(Items.SMOKER), conditionsFromItem(Items.SMOKER)).offerTo(exporter);
                createSmithing(ModItems.REINFORCED_SMOKER, ModItems.NETHERITE_SMOKER, RecipeCategory.DECORATIONS);
            }

            // --- Helpers ---
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
                SmithingTransformRecipeJsonBuilder.create(Ingredient.ofItems(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), Ingredient.ofItems(input), Ingredient.ofItems(Items.NETHERITE_INGOT), category, result).criterion("has_netherite_ingot", conditionsFromItem(Items.NETHERITE_INGOT)).offerTo(exporter, getItemPath(result) + "_smithing");
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

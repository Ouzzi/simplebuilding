package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.version.McVersion;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

/**
 * SimpleBuilding's side of the enchanting building blocks (N27 / BS25-1 / LOHEN17): since N48 (14.11.2026) the blazewood planks, chiseled
 * shelves, the blaze book and Blazing/Nihil Obsidian are Simple Enchanting's and the plain blazewood bookshelves SimpleLib's - the
 * {@code ModBlocks}/{@code ModItems} fields point at them and the Nihilit Book (with the End shelves' recipes) stays here. The tests of the
 * blocks themselves moved to {@code com.simpleenchanting.test.EnchantingTests} (shelf, chiseled shelf, fire, migration tests).
 */
public final class EnchantingBlockTests {
    private EnchantingBlockTests() {
    }

    /** The fields point at the moved objects, their recipes are loaded in the new namespaces, the Nihilit Book (stays here) is craftable. */
    public static void recipesAreLoaded(GameTestHelper helper) {
        if (!McVersion.ASTRAL_ENCHANTING) { helper.succeed(); return; }
        var recipes = helper.getLevel().getServer().getRecipeManager();
        boolean enchanting = com.simplebuilding.platform.ModEnvironment.isModLoaded("simpleenchanting");
        if (enchanting) {
            // N48: the fields of ModBlocks/ModItems are Simple Enchanting's (SimpleLib's for the plain shelves), the old ids move there.
            for (Block block : List.of(ModBlocks.BLAZING_OBSIDIAN, ModBlocks.NIHIL_OBSIDIAN, ModBlocks.CRIMSON_BLAZEWOOD_PLANKS, ModBlocks.WARPED_BLAZEWOOD_PLANKS,
                    ModBlocks.CHISELED_CRIMSON_BLAZEWOOD_BOOKSHELF, ModBlocks.CHISELED_WARPED_BLAZEWOOD_BOOKSHELF)) {
                helper.assertTrue("simpleenchanting".equals(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getNamespace()), block + " is Simple Enchanting's");
            }
            for (Block shelf : ModBlocks.BLAZEWOOD_BOOKSHELVES) {
                helper.assertTrue("simplelib".equals(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(shelf).getNamespace()), shelf + " is SimpleLib's");
            }
            helper.assertTrue("simpleenchanting".equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(ModItems.BLAZE_BOOK).getNamespace()), "the blaze book is Simple Enchanting's");
            for (String id : List.of("crimson_blazewood_planks", "warped_blazewood_planks", "crimson_blazewood_bookshelf", "warped_blazewood_bookshelf",
                    "chiseled_crimson_blazewood_bookshelf", "chiseled_warped_blazewood_bookshelf", "blaze_book", "blazing_obsidian")) {
                helper.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("simpleenchanting", id))).isPresent(), "recipe " + id + " is not loaded");
                helper.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("simplebuilding", id))).isEmpty(), "recipe " + id + " is still SimpleBuilding's");
            }
        }
        helper.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("simplebuilding", "nihilith_book"))).isPresent(), "recipe nihilith_book is not loaded");
        // DW04C-3: Nihilit Book = 4 Nihilit Shards around a book (plus); both mod books go into chiseled bookshelves.
        var nihil = (net.minecraft.world.item.crafting.CraftingRecipe) recipes.byKey(ResourceKey.create(Registries.RECIPE,
                Identifier.fromNamespaceAndPath("simplebuilding", "nihilith_book"))).orElseThrow().value();
        ItemStack shard = new ItemStack(ModItems.NIHILITH_SHARD);
        var plus = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, List.of(ItemStack.EMPTY, shard.copy(), ItemStack.EMPTY,
                shard.copy(), new ItemStack(Items.BOOK), shard.copy(), ItemStack.EMPTY, shard.copy(), ItemStack.EMPTY));
        helper.assertTrue(nihil.matches(plus, helper.getLevel()) && nihil.assemble(plus).is(ModItems.NIHILITH_BOOK), "4 nihilit shards + book craft the nihilit book");
        helper.assertTrue(new ItemStack(ModItems.NIHILITH_BOOK).is(net.minecraft.tags.ItemTags.BOOKSHELF_BOOKS), "the nihilit book fits chiseled bookshelves");
        // With Simple End: its bookshelves are made of Nihilit Books here (Simple End's own book recipe is off).
        if (com.simplebuilding.platform.ModEnvironment.isModLoaded("simpleend")) {
            for (String wood : List.of("ashwood", "duskwood")) {
                helper.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("simplebuilding", wood + "_bookshelf"))).isPresent(),
                        "nihilit-book recipe of the " + wood + " bookshelf");
                helper.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("simpleend", wood + "_bookshelf"))).isEmpty(),
                        "Simple End's book recipe of the " + wood + " bookshelf is off with SimpleBuilding");
                // a-end07 (Wunsch 8): the shelf wants Nihilit planks (4 shards in a plus around an End plank = 2), not the plain End planks.
                // a-nihilplanks (12.11.): the planks are Simple End's (simpleend:nihilith_<wood>_planks); SB keeps the recipes.
                var nihilPlanks = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("simpleend", "nihilith_" + wood + "_planks"));
                helper.assertTrue(nihilPlanks != Items.AIR && "simpleend".equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(nihilPlanks).getNamespace()),
                        "Simple End registers the nihilit " + wood + " planks with SimpleBuilding");
                var endPlanks = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("simpleend", wood + "_planks"));
                var shelfItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("simpleend", wood + "_bookshelf"));
                var plankRecipe = (net.minecraft.world.item.crafting.CraftingRecipe) recipes.byKey(ResourceKey.create(Registries.RECIPE,
                        Identifier.fromNamespaceAndPath("simplebuilding", "nihilith_" + wood + "_planks"))).orElseThrow().value();
                var plankGrid = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, List.of(ItemStack.EMPTY, shard.copy(), ItemStack.EMPTY,
                        shard.copy(), new ItemStack(endPlanks), shard.copy(), ItemStack.EMPTY, shard.copy(), ItemStack.EMPTY));
                helper.assertTrue(plankRecipe.matches(plankGrid, helper.getLevel()) && plankRecipe.assemble(plankGrid).is(nihilPlanks)
                        && plankRecipe.assemble(plankGrid).getCount() == 2, "4 nihilit shards around an End plank craft 2 nihilit " + wood + " planks");
                var shelfRecipe = (net.minecraft.world.item.crafting.CraftingRecipe) recipes.byKey(ResourceKey.create(Registries.RECIPE,
                        Identifier.fromNamespaceAndPath("simplebuilding", wood + "_bookshelf"))).orElseThrow().value();
                ItemStack shelfBook = new ItemStack(ModItems.NIHILITH_BOOK);
                java.util.function.Function<net.minecraft.world.item.Item, net.minecraft.world.item.crafting.CraftingInput> grid = planks ->
                        net.minecraft.world.item.crafting.CraftingInput.of(3, 3, List.of(new ItemStack(planks), new ItemStack(planks), new ItemStack(planks),
                                shelfBook.copy(), shelfBook.copy(), shelfBook.copy(), new ItemStack(planks), new ItemStack(planks), new ItemStack(planks)));
                helper.assertTrue(shelfRecipe.matches(grid.apply(nihilPlanks), helper.getLevel()) && shelfRecipe.assemble(grid.apply(nihilPlanks)).is(shelfItem),
                        "6 nihilit " + wood + " planks + 3 nihilit books craft the " + wood + " bookshelf");
                helper.assertFalse(shelfRecipe.matches(grid.apply(endPlanks), helper.getLevel()), "plain " + wood + " planks no longer craft the nihilit shelf");
                // a-nihilplanks: the chiseled End shelf like chiseled blazewood - 6 Nihilit planks around 3 slabs of the wood (WWW/SSS/WWW);
                // Simple End's own one (slabs around plain planks) is off with SimpleBuilding.
                helper.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("simpleend", "chiseled_" + wood + "_bookshelf"))).isEmpty(),
                        "Simple End's plain chiseled " + wood + " bookshelf recipe is off with SimpleBuilding");
                var chiseledRecipe = (net.minecraft.world.item.crafting.CraftingRecipe) recipes.byKey(ResourceKey.create(Registries.RECIPE,
                        Identifier.fromNamespaceAndPath("simplebuilding", "chiseled_" + wood + "_bookshelf"))).orElseThrow().value();
                var slab = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("simpleend", wood + "_slab")));
                var chiseledItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("simpleend", "chiseled_" + wood + "_bookshelf"));
                java.util.function.Function<net.minecraft.world.item.Item, net.minecraft.world.item.crafting.CraftingInput> chiseledGrid = planks ->
                        net.minecraft.world.item.crafting.CraftingInput.of(3, 3, List.of(new ItemStack(planks), new ItemStack(planks), new ItemStack(planks),
                                slab.copy(), slab.copy(), slab.copy(), new ItemStack(planks), new ItemStack(planks), new ItemStack(planks)));
                helper.assertTrue(chiseledRecipe.matches(chiseledGrid.apply(nihilPlanks), helper.getLevel())
                        && chiseledRecipe.assemble(chiseledGrid.apply(nihilPlanks)).is(chiseledItem), "6 nihilit " + wood + " planks + 3 slabs craft the chiseled " + wood + " bookshelf");
            }
        }
        helper.succeed();
    }
}

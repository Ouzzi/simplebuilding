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
 * SimpleBuilding's enchanting building blocks (N27 / BS25-1 / LOHEN17; the tables themselves are Simple Enchanting's since
 * ENCH02 03.11.): blazewood planks and bookshelves, the blaze book, Blazing and Nihil Obsidian. Until 03.11. part of
 * AstralEnchantingTests, whose table tests moved to {@code com.simpleenchanting.test.EnchantingTests}.
 */
public final class EnchantingBlockTests {
    private EnchantingBlockTests() {
    }

    /** The recipes of the concept are loaded: blazewood per nether wood, blaze book, shelves, blazing obsidian. */
    public static void recipesAreLoaded(GameTestHelper helper) {
        if (!McVersion.ASTRAL_ENCHANTING) { helper.succeed(); return; }
        var recipes = helper.getLevel().getServer().getRecipeManager();
        for (String id : List.of("crimson_blazewood_planks", "warped_blazewood_planks", "crimson_blazewood_bookshelf",
                "warped_blazewood_bookshelf", "chiseled_crimson_blazewood_bookshelf", "chiseled_warped_blazewood_bookshelf", "blaze_book", "nihilith_book", "blazing_obsidian")) {
            helper.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("simplebuilding", id))).isPresent(),
                    "recipe " + id + " is not loaded");
        }
        // Nachtrag 60: 8 Blaze Powder around 1 Book; the old 3 Paper + 1 Blaze Powder is gone
        var book = (net.minecraft.world.item.crafting.CraftingRecipe) recipes.byKey(ResourceKey.create(Registries.RECIPE,
                Identifier.fromNamespaceAndPath("simplebuilding", "blaze_book"))).orElseThrow().value();
        ItemStack powder = new ItemStack(Items.BLAZE_POWDER);
        var ring = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, List.of(powder.copy(), powder.copy(), powder.copy(),
                powder.copy(), new ItemStack(Items.BOOK), powder.copy(), powder.copy(), powder.copy(), powder.copy()));
        helper.assertTrue(book.matches(ring, helper.getLevel()) && book.assemble(ring).is(ModItems.BLAZE_BOOK), "8 blaze powder + book craft the blaze book");
        var old = net.minecraft.world.item.crafting.CraftingInput.of(2, 2, List.of(new ItemStack(Items.PAPER), new ItemStack(Items.PAPER),
                new ItemStack(Items.PAPER), powder.copy()));
        helper.assertFalse(book.matches(old, helper.getLevel()), "the old paper recipe still crafts the blaze book");
        helper.assertTrue(ModBlocks.CRIMSON_BLAZEWOOD_BOOKSHELF.defaultBlockState().is(net.minecraft.tags.BlockTags.ENCHANTMENT_POWER_PROVIDER),
                "blazewood shelves do not count at the Vanilla table");
        helper.assertTrue(ModBlocks.BLAZING_OBSIDIAN.defaultBlockState().getLightEmission() > 0, "blazing obsidian does not glow");
        // DW04C-3: Nihilit Book = 4 Nihilit Shards around a book (plus); both mod books go into chiseled bookshelves.
        var nihil = (net.minecraft.world.item.crafting.CraftingRecipe) recipes.byKey(ResourceKey.create(Registries.RECIPE,
                Identifier.fromNamespaceAndPath("simplebuilding", "nihilith_book"))).orElseThrow().value();
        ItemStack shard = new ItemStack(ModItems.NIHILITH_SHARD);
        var plus = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, List.of(ItemStack.EMPTY, shard.copy(), ItemStack.EMPTY,
                shard.copy(), new ItemStack(Items.BOOK), shard.copy(), ItemStack.EMPTY, shard.copy(), ItemStack.EMPTY));
        helper.assertTrue(nihil.matches(plus, helper.getLevel()) && nihil.assemble(plus).is(ModItems.NIHILITH_BOOK), "4 nihilit shards + book craft the nihilit book");
        helper.assertTrue(new ItemStack(ModItems.BLAZE_BOOK).is(net.minecraft.tags.ItemTags.BOOKSHELF_BOOKS)
                && new ItemStack(ModItems.NIHILITH_BOOK).is(net.minecraft.tags.ItemTags.BOOKSHELF_BOOKS), "mod books fit chiseled bookshelves");
        // With Simple End: its bookshelves are made of Nihilit Books here (Simple End's own book recipe is off).
        if (com.simplebuilding.platform.ModEnvironment.isModLoaded("simpleend")) {
            for (String wood : List.of("ashwood", "duskwood")) {
                helper.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("simplebuilding", wood + "_bookshelf"))).isPresent(),
                        "nihilit-book recipe of the " + wood + " bookshelf");
                helper.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("simpleend", wood + "_bookshelf"))).isEmpty(),
                        "Simple End's book recipe of the " + wood + " bookshelf is off with SimpleBuilding");
                // a-end07 (Wunsch 8): the shelf wants Nihilit planks (4 shards in a plus around an End plank = 2), not the plain End planks.
                var nihilPlanks = wood.equals("ashwood") ? ModItems.NIHILITH_ASHWOOD_PLANKS : ModItems.NIHILITH_DUSKWOOD_PLANKS;
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
            }
        }
        helper.succeed();
    }

    /**
     * BS25-1: the chiseled blazewood bookshelves work like Vanilla's: the block entity (Vanilla's type, accepted by the
     * mixin) takes a book into slot 0 and the slot_0_occupied state follows; a non-book is refused.
     */
    public static void chiseledBookshelvesStoreBooks(GameTestHelper helper) {
        if (!McVersion.ASTRAL_ENCHANTING) { helper.succeed(); return; }
        int i = 0;
        for (Block shelf : ModBlocks.CHISELED_BLAZEWOOD_BOOKSHELVES) {
            BlockPos pos = new BlockPos(1 + 2 * i++, 1, 1);
            helper.setBlock(pos, shelf);
            var be = helper.getBlockEntity(pos, net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity.class);
            helper.assertTrue(be.acceptsItemType(new ItemStack(Items.BOOK)) && !be.acceptsItemType(new ItemStack(Items.STICK)),
                    "only books fit the chiseled blazewood bookshelf");
            be.setItem(0, new ItemStack(Items.BOOK));
            helper.assertTrue(helper.getBlockState(pos).getValue(net.minecraft.world.level.block.ChiseledBookShelfBlock.SLOT_0_OCCUPIED),
                    "slot 0 is not occupied after setting a book");
            helper.assertFalse(helper.getBlockState(pos).getValue(net.minecraft.world.level.block.ChiseledBookShelfBlock.SLOT_1_OCCUPIED),
                    "slot 1 occupied by itself");
        }
        helper.succeed();
    }

    /**
     * LOHEN17 V2 (owner 2026-10-21): blazing obsidian is in {@code minecraft:infiniburn_overworld}, so fire on it burns
     * forever. Twenty fire ticks at age 15 (the spread gamerule opened for the synchronous ticks only, no player is
     * near a test): the fire on blazing obsidian stays, the one on plain obsidian (control) goes out.
     */
    public static void blazingObsidianKeepsFireBurning(GameTestHelper helper) {
        if (ModBlocks.BLAZING_OBSIDIAN == null) { helper.succeed(); return; }
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        helper.assertTrue(ModBlocks.BLAZING_OBSIDIAN.defaultBlockState().is(net.minecraft.tags.BlockTags.INFINIBURN_OVERWORLD),
                "blazing obsidian is not in minecraft:infiniburn_overworld");
        BlockPos blazing = new BlockPos(1, 1, 1);
        BlockPos plain = new BlockPos(4, 1, 1);
        helper.setBlock(blazing, ModBlocks.BLAZING_OBSIDIAN);
        helper.setBlock(plain, net.minecraft.world.level.block.Blocks.OBSIDIAN);
        var fire = net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AGE_15, 15);
        helper.setBlock(blazing.above(), fire);
        helper.setBlock(plain.above(), fire);
        List<BlockPos> fires = List.of(helper.absolutePos(blazing.above()), helper.absolutePos(plain.above()));
        var rules = level.getGameRules();
        var rule = net.minecraft.world.level.gamerules.GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER;
        int radius = rules.get(rule);
        try {
            rules.set(rule, -1, level.getServer());
            for (int i = 0; i < 20; i++) {
                for (BlockPos pos : fires) {
                    var state = level.getBlockState(pos);
                    if (state.is(net.minecraft.world.level.block.Blocks.FIRE)) state.tick(level, pos, level.getRandom());
                }
            }
        } finally {
            rules.set(rule, radius, level.getServer());
        }
        helper.assertTrue(level.getBlockState(fires.get(0)).is(net.minecraft.world.level.block.Blocks.FIRE),
                "fire on blazing obsidian went out");
        helper.assertFalse(level.getBlockState(fires.get(1)).is(net.minecraft.world.level.block.Blocks.FIRE),
                "control: fire on plain obsidian should go out");
        helper.succeed();
    }

    /**
     * ENCH02 (owner 03.11.): Nihil Obsidian - Blazing Obsidian with Nihilith - is crafted from crying obsidian and eight Nihilith
     * shards, glows and is in Simple Enchanting's floor tags (Dark: any obsidian floor, Astral: the full bonus).
     */
    public static void nihilObsidianIsCraftedAndCountsAsFloor(GameTestHelper helper) {
        if (ModBlocks.NIHIL_OBSIDIAN == null) { helper.succeed(); return; }
        var recipes = helper.getLevel().getServer().getRecipeManager();
        var recipe = (net.minecraft.world.item.crafting.CraftingRecipe) recipes.byKey(ResourceKey.create(Registries.RECIPE,
                Identifier.fromNamespaceAndPath("simplebuilding", "nihil_obsidian"))).orElseThrow().value();
        ItemStack shard = new ItemStack(ModItems.NIHILITH_SHARD);
        var ring = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, List.of(shard.copy(), shard.copy(), shard.copy(),
                shard.copy(), new ItemStack(Items.CRYING_OBSIDIAN), shard.copy(), shard.copy(), shard.copy(), shard.copy()));
        helper.assertTrue(recipe.matches(ring, helper.getLevel()) && recipe.assemble(ring).is(ModItems.NIHIL_OBSIDIAN),
                "crying obsidian + 8 Nihilith shards craft Nihil Obsidian");
        helper.assertTrue(ModBlocks.NIHIL_OBSIDIAN.defaultBlockState().getLightEmission() > 0, "Nihil Obsidian does not glow");
        if (com.simplebuilding.platform.ModEnvironment.isModLoaded("simpleenchanting")) {
            var floor = net.minecraft.tags.TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("simpleenchanting", "nihil_floor"));
            var any = net.minecraft.tags.TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("simpleenchanting", "obsidian_floor"));
            helper.assertTrue(ModBlocks.NIHIL_OBSIDIAN.defaultBlockState().is(floor), "Nihil Obsidian is not a Nihil floor");
            helper.assertTrue(ModBlocks.BLAZING_OBSIDIAN.defaultBlockState().is(any) && !ModBlocks.BLAZING_OBSIDIAN.defaultBlockState().is(floor),
                    "Blazing Obsidian is an obsidian floor but no Nihil floor");
        }
        helper.succeed();
    }
}

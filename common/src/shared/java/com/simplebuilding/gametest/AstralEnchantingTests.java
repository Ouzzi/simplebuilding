package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.enchanting.AstralEnchanting;
import com.simplebuilding.enchanting.AstralEnchantingTableBlockEntity;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.screen.AstralEnchantingMenu;
import com.simplebuilding.util.SledgehammerUpgrades;
import com.simplebuilding.version.McVersion;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Astral Enchanting Table (owner 2026-10-09, queue N27, docs/ai/KONZEPT-ASTRAL-VERZAUBERUNG-2026-10-09.md): the pure
 * budget/cost rules, the shelf and floor detection, making it with the sledgehammer, what breaking it drops, the stock
 * that stays in the block, and that enchanting uses up exactly what the rules say.
 */
public final class AstralEnchantingTests {
    /** Tick budget of the hammer test: 20 blows of 20 ticks run inside one game tick, the rest is slack. */
    public static final int MAX_TICKS = 40;
    private static final BlockPos TABLE = new BlockPos(3, 2, 3);

    private AstralEnchantingTests() {
    }

    // ------------------------------------------------------------------ pure rules

    /**
     * The numbers of the concept: tier min(30, 2 x points), floor 40 from 20 and 50 from 30 points; budget 3 without
     * shelves, unlimited at 50; 3/6/10 points per level by rarity; 1-3 levels by tens of points, 4 at 40, 5 at 50;
     * lapis = levels, blaze powder twice that; the player needs the points spent in levels (at most 30).
     */
    public static void budgetAndCostRules(GameTestHelper helper) {
        if (!McVersion.ASTRAL_ENCHANTING) { helper.succeed(); return; }
        eq(helper, AstralEnchanting.tier(0, false), 0, "tier without shelves");
        eq(helper, AstralEnchanting.tier(15, false), 30, "tier of 15 bookshelves");
        eq(helper, AstralEnchanting.tier(30, false), 30, "tier of 15 blazewood shelves without floor");
        eq(helper, AstralEnchanting.tier(19, true), 30, "tier of 19 points on the floor (below 40 the floor adds nothing)");
        eq(helper, AstralEnchanting.tier(20, true), 40, "tier of 20 points on the floor");
        eq(helper, AstralEnchanting.tier(30, true), 50, "tier of 30 points on the floor");
        int[] fifteenBlaze = new int[32];
        java.util.Arrays.fill(fifteenBlaze, 0, 15, AstralEnchanting.BLAZE_SHELF);
        eq(helper, AstralEnchanting.shelfPoints(fifteenBlaze), 30, "points of 15 blazewood shelves");
        int[] mixed = new int[32];
        java.util.Arrays.fill(mixed, 1);
        mixed[0] = 2;
        mixed[1] = 2;
        eq(helper, AstralEnchanting.shelfPoints(mixed), 17, "best 15 of 2 blazewood + 30 bookshelves");
        eq(helper, AstralEnchanting.budget(0), 3, "budget without shelves");
        eq(helper, AstralEnchanting.budget(30), 30, "budget at 30");
        eq(helper, AstralEnchanting.budget(40), 40, "budget at 40");
        eq(helper, AstralEnchanting.budget(50), AstralEnchanting.UNLIMITED, "budget at 50");
        eq(helper, AstralEnchanting.pointsPerLevel(10), 3, "common");
        eq(helper, AstralEnchanting.pointsPerLevel(5), 3, "uncommon");
        eq(helper, AstralEnchanting.pointsPerLevel(2), 6, "rare");
        eq(helper, AstralEnchanting.pointsPerLevel(1), 10, "very rare");
        eq(helper, AstralEnchanting.levelCost(0, 30), 0, "nothing chosen");
        eq(helper, AstralEnchanting.levelCost(3, 0), 1, "one common level without shelves");
        eq(helper, AstralEnchanting.levelCost(15, 30), 2, "15 points");
        eq(helper, AstralEnchanting.levelCost(30, 30), 3, "30 points");
        eq(helper, AstralEnchanting.levelCost(9, 40), 4, "tier 40");
        eq(helper, AstralEnchanting.levelCost(90, 50), 5, "tier 50");
        eq(helper, AstralEnchanting.requiredLevel(15, 30), 15, "needs the points in levels");
        eq(helper, AstralEnchanting.requiredLevel(90, 50), 30, "needs at most 30");
        eq(helper, AstralEnchanting.requiredLevel(3, 50), 5, "never less than the levels used up");
        eq(helper, AstralEnchanting.lapisCost(3), 3, "lapis");
        eq(helper, AstralEnchanting.blazePowderCost(3), 6, "blaze powder twice the lapis");
        // Without shelves (budget 3): one level of a common enchantment, rare ones greyed out.
        int[] costs = {3, 6, 10};
        eq(helper, AstralEnchanting.maxAffordable(0, new int[3], costs, 5, 3), 1, "common slider without shelves");
        eq(helper, AstralEnchanting.maxAffordable(1, new int[3], costs, 3, 3), 0, "rare slider without shelves");
        // Budget 30: Efficiency V (15) + Unbreaking III (9) leaves 6, so a rare slider reaches 1.
        eq(helper, AstralEnchanting.maxAffordable(1, new int[] {5, 0, 0}, new int[] {3, 6, 3}, 3, 30), 2, "rare slider next to Efficiency V");
        eq(helper, AstralEnchanting.spent(new int[] {5, 3, 0}, new int[] {3, 3, 6}), 24, "Efficiency V + Unbreaking III");
        helper.succeed();
    }

    // ------------------------------------------------------------------ the world

    private static void floor(GameTestHelper helper, Block block) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                helper.setBlock(TABLE.offset(dx, -1, dz), block);
            }
        }
    }

    /** Fills Vanilla's 32 shelf spots: the first {@code blaze} with blazewood shelves, the rest with bookshelves. */
    private static void shelves(GameTestHelper helper, int count, int blaze) {
        List<BlockPos> offsets = EnchantingTableBlock.BOOKSHELF_OFFSETS;
        for (int i = 0; i < offsets.size(); i++) {
            Block block = i >= count ? Blocks.AIR : i < blaze ? ModBlocks.CRIMSON_BLAZEWOOD_BOOKSHELF : Blocks.BOOKSHELF;
            helper.setBlock(TABLE.offset(offsets.get(i)), block);
        }
    }

    private static int tier(GameTestHelper helper) {
        return AstralEnchanting.tierAt(helper.getLevel(), helper.absolutePos(TABLE));
    }

    /**
     * Shelves count like Vanilla's (blocked gap = no shelf), blazewood shelves twice; the 5x5 blazing obsidian floor
     * lifts the tier to 40 and 50, and only a complete floor counts.
     */
    public static void shelvesAndFloorSetTheTier(GameTestHelper helper) {
        if (!McVersion.ASTRAL_ENCHANTING) { helper.succeed(); return; }
        floor(helper, Blocks.STONE);
        helper.setBlock(TABLE, ModBlocks.ASTRAL_ENCHANTING_TABLE);
        eq(helper, tier(helper), 0, "tier without shelves");
        shelves(helper, 5, 0);
        eq(helper, tier(helper), 10, "tier of 5 bookshelves");
        shelves(helper, 15, 0);
        eq(helper, tier(helper), 30, "tier of 15 bookshelves");
        // A block in the gap between table and shelf hides the shelf (Vanilla's transmitter rule).
        BlockPos first = EnchantingTableBlock.BOOKSHELF_OFFSETS.get(0);
        shelves(helper, 1, 1);
        eq(helper, AstralEnchanting.shelfValue(helper.getLevel(), helper.absolutePos(TABLE), first), AstralEnchanting.BLAZE_SHELF,
                "value of a blazewood shelf");
        helper.setBlock(TABLE.offset(first.getX() / 2, first.getY(), first.getZ() / 2), Blocks.STONE);
        eq(helper, AstralEnchanting.shelfValue(helper.getLevel(), helper.absolutePos(TABLE), first), 0, "value of a shelf behind stone");
        helper.setBlock(TABLE.offset(first.getX() / 2, first.getY(), first.getZ() / 2), Blocks.AIR);
        shelves(helper, 10, 10);
        eq(helper, tier(helper), 30, "tier of 10 blazewood shelves without floor");
        floor(helper, ModBlocks.BLAZING_OBSIDIAN);
        eq(helper, tier(helper), 40, "tier of 10 blazewood shelves (20 points) on the floor");
        shelves(helper, 15, 15);
        eq(helper, tier(helper), 50, "tier of 15 blazewood shelves on the floor");
        helper.setBlock(TABLE.offset(2, -1, 2), Blocks.OBSIDIAN);
        eq(helper, tier(helper), 30, "tier with one floor block missing");
        helper.succeed();
    }

    /**
     * Made in the world: a Vanilla enchanting table, a Netherite sledgehammer and an Enderite nugget in the off hand -
     * four times the blows of a machine upgrade, the nugget is used up at the last one. A diamond hammer cannot.
     */
    public static void hammerTurnsTheEnchantingTableAstral(GameTestHelper helper) {
        if (!McVersion.ASTRAL_ENCHANTING) { helper.succeed(); return; }
        var upgrade = SledgehammerUpgrades.upgradeOf(Blocks.ENCHANTING_TABLE);
        helper.assertTrue(upgrade != null && upgrade.to() == ModBlocks.ASTRAL_ENCHANTING_TABLE && upgrade.nugget() == ModItems.ENDERITE_NUGGET
                && upgrade.minHammerRank() == SledgehammerUpgrades.RANK_NETHERITE && upgrade.materialCost() == 1
                && upgrade.durationFactor() == SledgehammerUpgrades.ASTRAL_TABLE_DURATION_FACTOR, "enchanting table upgrade: " + upgrade);
        eq(helper, SledgehammerUpgrades.blows(upgrade), SledgehammerUpgrades.blows() * 4, "blows for the astral table");

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        helper.runBeforeTestEnd(() -> {
            player.containerMenu = player.inventoryMenu;
            helper.getLevel().getServer().getPlayerList().remove(player);
        });
        helper.setBlock(TABLE.below(), Blocks.STONE);
        helper.setBlock(TABLE, Blocks.ENCHANTING_TABLE);
        BlockPos abs = helper.absolutePos(TABLE);
        Vec3 feet = Vec3.atCenterOf(abs).add(0.0, 0.5, 0.0);
        player.snapTo(feet.x, feet.y, feet.z, 0.0F, 90.0F);

        // A diamond hammer is too weak: the click opens nothing and starts nothing.
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.ENDERITE_NUGGET, 2));
        click(helper, player, abs);
        helper.assertFalse(SledgehammerUpgrades.hasJob(player), "a diamond hammer started the astral upgrade");
        player.stopUsingItem();
        player.containerMenu = player.inventoryMenu;

        ItemStack hammer = new ItemStack(ModItems.NETHERITE_SLEDGEHAMMER);
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        click(helper, player, abs);
        helper.assertTrue(player.containerMenu == player.inventoryMenu, "the click opened the enchanting menu instead of hammering");
        helper.assertTrue(SledgehammerUpgrades.hasJob(player), "the netherite hammer did not start the astral upgrade");
        int ticks = SledgehammerUpgrades.upgradeTicks(upgrade);
        for (int tick = 1; tick < ticks; tick++) {
            player.connection.tick();
            if (!player.isUsingItem()) helper.fail("the hammering stopped after " + tick + " of " + ticks + " ticks");
        }
        helper.assertBlockPresent(Blocks.ENCHANTING_TABLE, TABLE);
        player.connection.tick();
        helper.assertBlockPresent(ModBlocks.ASTRAL_ENCHANTING_TABLE, TABLE);
        eq(helper, player.getOffhandItem().getCount(), 1, "nuggets left after the upgrade");
        helper.assertTrue(helper.getLevel().getBlockEntity(abs) instanceof AstralEnchantingTableBlockEntity, "no astral block entity");
        helper.succeed();
    }

    private static void click(GameTestHelper helper, ServerPlayer player, BlockPos abs) {
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs).add(0.0, 0.25, 0.0), Direction.UP, abs, false);
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
    }

    /**
     * Twice as hard to break as the Vanilla table; it drops a Vanilla enchanting table, the Enderite nugget and the
     * stored lapis and blaze powder.
     */
    public static void breakingDropsTableNuggetAndStock(GameTestHelper helper) {
        if (!McVersion.ASTRAL_ENCHANTING) { helper.succeed(); return; }
        BlockPos abs = helper.absolutePos(TABLE);
        float vanilla = Blocks.ENCHANTING_TABLE.defaultBlockState().getDestroySpeed(helper.getLevel(), abs);
        float astral = ModBlocks.ASTRAL_ENCHANTING_TABLE.defaultBlockState().getDestroySpeed(helper.getLevel(), abs);
        helper.assertTrue(astral == 2 * vanilla, "hardness " + astral + " is not twice the enchanting table's " + vanilla);
        helper.setBlock(TABLE.below(), Blocks.STONE);
        helper.setBlock(TABLE, ModBlocks.ASTRAL_ENCHANTING_TABLE);
        var table = helper.getBlockEntity(TABLE, AstralEnchantingTableBlockEntity.class);
        table.setItem(AstralEnchantingTableBlockEntity.LAPIS, new ItemStack(Items.LAPIS_LAZULI, 10));
        table.setItem(AstralEnchantingTableBlockEntity.BLAZE, new ItemStack(Items.BLAZE_POWDER, 20));
        helper.getLevel().destroyBlock(abs, true);
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(abs).inflate(2.0));
        eq(helper, count(drops, Items.ENCHANTING_TABLE), 1, "enchanting tables dropped");
        eq(helper, count(drops, ModItems.ENDERITE_NUGGET), 1, "enderite nuggets dropped");
        eq(helper, count(drops, Items.LAPIS_LAZULI), 10, "lapis dropped");
        eq(helper, count(drops, Items.BLAZE_POWDER), 20, "blaze powder dropped");
        eq(helper, count(drops, ModItems.ASTRAL_ENCHANTING_TABLE), 0, "astral tables dropped");
        drops.forEach(ItemEntity::discard);
        helper.succeed();
    }

    private static int count(List<ItemEntity> drops, net.minecraft.world.item.Item item) {
        return drops.stream().filter(e -> e.getItem().is(item)).mapToInt(e -> e.getItem().getCount()).sum();
    }

    private static AstralEnchantingMenu open(GameTestHelper helper, ServerPlayer player) {
        var table = helper.getBlockEntity(TABLE, AstralEnchantingTableBlockEntity.class);
        AstralEnchantingMenu menu = new AstralEnchantingMenu(1, player.getInventory(), table,
                ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(TABLE)));
        player.containerMenu = menu;
        return menu;
    }

    private static ServerPlayer survivor(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        helper.runBeforeTestEnd(() -> {
            player.containerMenu = player.inventoryMenu;
            helper.getLevel().getServer().getPlayerList().remove(player);
        });
        return player;
    }

    /**
     * Lapis and blaze powder go into the table's own slots (shift-click) and stay there when the menu closes; the item
     * to enchant goes back to the player. Only lapis and blaze powder fit, at most one stack each.
     */
    public static void stockStaysInTheTable(GameTestHelper helper) {
        if (!McVersion.ASTRAL_ENCHANTING) { helper.succeed(); return; }
        helper.setBlock(TABLE, ModBlocks.ASTRAL_ENCHANTING_TABLE);
        ServerPlayer player = survivor(helper);
        player.getInventory().clearContent();
        player.getInventory().setItem(0, new ItemStack(Items.LAPIS_LAZULI, 64));
        player.getInventory().setItem(1, new ItemStack(Items.LAPIS_LAZULI, 10));
        player.getInventory().setItem(2, new ItemStack(Items.BLAZE_POWDER, 30));
        player.getInventory().setItem(3, new ItemStack(Items.DIAMOND_SWORD));
        AstralEnchantingMenu menu = open(helper, player);
        for (int i = 0; i < 4; i++) {
            int slot = slotOf(menu, player, i);
            menu.quickMoveStack(player, slot);
        }
        var table = helper.getBlockEntity(TABLE, AstralEnchantingTableBlockEntity.class);
        eq(helper, table.getItem(AstralEnchantingTableBlockEntity.LAPIS).getCount(), 64, "lapis in the table (one stack)");
        eq(helper, table.getItem(AstralEnchantingTableBlockEntity.BLAZE).getCount(), 30, "blaze powder in the table");
        helper.assertTrue(menu.getSlot(AstralEnchantingMenu.ITEM_SLOT).getItem().is(Items.DIAMOND_SWORD), "the sword is not in the item slot");
        helper.assertFalse(table.canPlaceItem(AstralEnchantingTableBlockEntity.LAPIS, new ItemStack(Items.BLAZE_POWDER)), "blaze powder fits the lapis slot");
        helper.assertFalse(table.canTakeItemThroughFace(AstralEnchantingTableBlockEntity.LAPIS, table.getItem(0), Direction.DOWN),
                "a hopper below could pull lapis out");
        menu.removed(player);
        player.containerMenu = player.inventoryMenu;
        eq(helper, table.getItem(AstralEnchantingTableBlockEntity.LAPIS).getCount(), 64, "lapis after closing");
        eq(helper, table.getItem(AstralEnchantingTableBlockEntity.BLAZE).getCount(), 30, "blaze powder after closing");
        helper.assertTrue(player.getInventory().contains(new ItemStack(Items.DIAMOND_SWORD)), "the sword did not go back to the player");
        // Reopened: the stock is there again.
        AstralEnchantingMenu again = open(helper, player);
        eq(helper, again.lapis(), 64, "lapis seen by a new menu");
        eq(helper, again.blazePowder(), 30, "blaze powder seen by a new menu");
        again.removed(player);
        helper.succeed();
    }

    private static int slotOf(AstralEnchantingMenu menu, ServerPlayer player, int inventoryIndex) {
        for (int i = AstralEnchantingMenu.PLAYER_START; i < menu.slots.size(); i++) {
            if (menu.getSlot(i).container == player.getInventory() && menu.getSlot(i).getContainerSlot() == inventoryIndex) return i;
        }
        throw new IllegalStateException("no menu slot for inventory slot " + inventoryIndex);
    }

    /**
     * With 15 bookshelves (tier 30) a pickaxe gets three fitting, compatible enchantments; a slider asked beyond the
     * budget stops at what the budget allows; enchanting applies exactly the chosen levels and uses up the levels, lapis
     * and twice the blaze powder the rules name, and gives the player a new enchantment seed.
     */
    public static void enchantingUsesUpWhatTheRulesSay(GameTestHelper helper) {
        if (!McVersion.ASTRAL_ENCHANTING) { helper.succeed(); return; }
        floor(helper, Blocks.STONE);
        helper.setBlock(TABLE, ModBlocks.ASTRAL_ENCHANTING_TABLE);
        shelves(helper, 15, 0);
        var table = helper.getBlockEntity(TABLE, AstralEnchantingTableBlockEntity.class);
        table.setItem(AstralEnchantingTableBlockEntity.LAPIS, new ItemStack(Items.LAPIS_LAZULI, 10));
        table.setItem(AstralEnchantingTableBlockEntity.BLAZE, new ItemStack(Items.BLAZE_POWDER, 10));
        ServerPlayer player = survivor(helper);
        player.giveExperienceLevels(40);
        AstralEnchantingMenu menu = open(helper, player);
        eq(helper, menu.tier(), 30, "tier seen by the menu");
        menu.getSlot(AstralEnchantingMenu.ITEM_SLOT).set(new ItemStack(Items.DIAMOND_PICKAXE));
        int rows = 0;
        for (int row = 0; row < 3; row++) {
            Holder<Enchantment> holder = menu.enchantment(helper.getLevel(), row);
            if (holder == null) continue;
            rows++;
            helper.assertTrue(holder.value().isPrimaryItem(new ItemStack(Items.DIAMOND_PICKAXE)), holder + " does not fit a pickaxe");
            for (int other = 0; other < row; other++) {
                Holder<Enchantment> o = menu.enchantment(helper.getLevel(), other);
                helper.assertTrue(o == null || Enchantment.areCompatible(o, holder), o + " and " + holder + " offered together");
            }
        }
        eq(helper, rows, 3, "enchantments offered for a diamond pickaxe");
        // Every slider to its maximum, in order: what the budget does not allow is cut off.
        for (int row = 0; row < 3; row++) {
            menu.clickMenuButton(player, row * AstralEnchantingMenu.LEVELS_PER_ROW + menu.maxLevel(row));
            helper.assertTrue(menu.chosen(row) <= menu.maxLevel(row), "slider beyond its maximum");
        }
        int spent = menu.spent();
        helper.assertTrue(spent > 0 && spent <= 30, "points spent " + spent + " outside 1..30");
        int[] chosen = menu.chosenLevels();
        List<Holder<Enchantment>> offered = new java.util.ArrayList<>();
        for (int row = 0; row < 3; row++) offered.add(menu.enchantment(helper.getLevel(), row));
        int levels = AstralEnchanting.levelCost(spent, 30);
        int seed = player.getEnchantmentSeed();
        int before = player.experienceLevel;
        helper.assertTrue(menu.clickMenuButton(player, AstralEnchantingMenu.BUTTON_ENCHANT), "enchanting refused");
        ItemStack result = menu.getSlot(AstralEnchantingMenu.ITEM_SLOT).getItem();
        for (int row = 0; row < 3; row++) {
            eq(helper, EnchantmentHelper.getItemEnchantmentLevel(offered.get(row), result), chosen[row], "level of " + offered.get(row));
        }
        eq(helper, total(result), sum(chosen), "enchantment levels on the pickaxe");
        eq(helper, player.experienceLevel, before - levels, "levels used up");
        eq(helper, table.getItem(AstralEnchantingTableBlockEntity.LAPIS).getCount(), 10 - levels, "lapis left");
        eq(helper, table.getItem(AstralEnchantingTableBlockEntity.BLAZE).getCount(), 10 - 2 * levels, "blaze powder left");
        helper.assertTrue(player.getEnchantmentSeed() != seed, "the enchantment seed did not change");
        eq(helper, menu.enchantId(0), -1, "offers for the already enchanted pickaxe");
        menu.removed(player);
        helper.succeed();
    }

    private static int total(ItemStack stack) {
        int sum = 0;
        for (var entry : EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()) sum += entry.getIntValue();
        return sum;
    }

    private static int sum(int[] values) {
        int s = 0;
        for (int v : values) s += v;
        return s;
    }

    /** The recipes of the concept are loaded: blazewood per nether wood, blaze book, shelves, blazing obsidian. */
    public static void recipesAreLoaded(GameTestHelper helper) {
        if (!McVersion.ASTRAL_ENCHANTING) { helper.succeed(); return; }
        var recipes = helper.getLevel().getServer().getRecipeManager();
        for (String id : List.of("crimson_blazewood_planks", "warped_blazewood_planks", "crimson_blazewood_bookshelf",
                "warped_blazewood_bookshelf", "blaze_book", "blazing_obsidian")) {
            helper.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("simplebuilding", id))).isPresent(),
                    "recipe " + id + " is not loaded");
        }
        helper.assertTrue(ModBlocks.CRIMSON_BLAZEWOOD_BOOKSHELF.defaultBlockState().is(net.minecraft.tags.BlockTags.ENCHANTMENT_POWER_PROVIDER),
                "blazewood shelves do not count at the Vanilla table");
        helper.assertTrue(ModBlocks.BLAZING_OBSIDIAN.defaultBlockState().getLightEmission() > 0, "blazing obsidian does not glow");
        helper.succeed();
    }

    private static void eq(GameTestHelper helper, int actual, int expected, String what) {
        helper.assertValueEqual(actual, expected, what);
    }
}

package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.TieredChestBlock;
import com.simplebuilding.blocks.entity.custom.TieredChestBlockEntity;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.screen.TieredChestMenu;
import com.simplebuilding.screen.TieredChestOpenData;
import com.simplebuilding.util.SledgehammerUpgrades;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The chest tiers: vanilla copper chest (27) -&gt; Reinforced (36) -&gt; Netherite (45, stacks x2)
 * -&gt; Enderite (54, stacks x4), climbed in the world with the sledgehammer and the tier material in
 * the off hand, keeping the contents.
 *
 * <p>The numbers are this file's own constants, not read from the mod: a test that read
 * {@code ChestTier#slots} would follow every edit of it. The upgrades drive the real click path
 * ({@code ServerPlayerGameMode#useItemOn}, which asks the chest first - a chest that opened its
 * menu instead would stop the hammer) and vanilla's own player tick, one tick at a time, like
 * {@code SledgehammerUpgradeTests}.
 */
public final class TieredChestTests {

    private TieredChestTests() {
    }

    /** Tick budget of the hopper rigs. */
    public static final int HOPPER_MAX_TICKS = 200;

    private static final int UPGRADE_TICKS = 100;
    private static final int COPPER_SLOTS = 27;
    private static final int REINFORCED_SLOTS = 36;
    private static final int NETHERITE_SLOTS = 45;
    private static final int ENDERITE_SLOTS = 54;

    // =====================================================================================
    // UPGRADES
    // =====================================================================================

    /**
     * A waxed exposed copper chest (any of the eight copper chests is a start) with items in its
     * first and last slot and a name climbs to reinforced with a stone sledgehammer and a cracked
     * diamond, to netherite with a diamond hammer and a netherite nugget, and to enderite with a
     * netherite hammer and an enderite nugget. After every step: the next tier, the same facing, the
     * same items in the same slots, the name kept, exactly one piece of material gone, and the
     * container has the tier's slot count.
     */
    public static void singleChestClimbsFromCopperToEnderiteKeepingItsContents(GameTestHelper helper) {
        ServerPlayer player = smith(helper);
        BlockPos pos = new BlockPos(1, 1, 1);
        Block copper = vanillaBlock("waxed_exposed_copper_chest");
        helper.setBlock(pos, copper.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST));
        BaseContainerBlockEntity start = container(helper, pos);
        // The name first: applying components also applies the (empty) container component.
        start.applyComponents(DataComponentMap.builder().set(DataComponents.CUSTOM_NAME, Component.literal("Hoard")).build(),
                DataComponentPatch.EMPTY);
        start.setItem(0, new ItemStack(Items.DIAMOND, 5));
        start.setItem(COPPER_SLOTS - 1, new ItemStack(Items.OAK_LOG, 64));

        hammer(helper, player, pos, ModItems.STONE_SLEDGEHAMMER, ModItems.CRACKED_DIAMOND, 2, "copper -> reinforced");
        assertTier(helper, pos, ModBlocks.REINFORCED_CHEST, REINFORCED_SLOTS, "copper -> reinforced");
        helper.assertTrue(player.getOffhandItem().getCount() == 1, "copper -> reinforced: cracked diamonds left: "
                + player.getOffhandItem().getCount() + ", expected 1");

        hammer(helper, player, pos, ModItems.DIAMOND_SLEDGEHAMMER, ModItems.NETHERITE_NUGGET, 2, "reinforced -> netherite");
        assertTier(helper, pos, ModBlocks.NETHERITE_CHEST, NETHERITE_SLOTS, "reinforced -> netherite");
        hammer(helper, player, pos, ModItems.NETHERITE_SLEDGEHAMMER, ModItems.ENDERITE_NUGGET, 2, "netherite -> enderite");
        assertTier(helper, pos, ModBlocks.ENDERITE_CHEST, ENDERITE_SLOTS, "netherite -> enderite");
        helper.assertTrue(player.getOffhandItem().getCount() == 1, "netherite -> enderite: nuggets left: "
                + player.getOffhandItem().getCount() + ", expected 1");

        BaseContainerBlockEntity end = container(helper, pos);
        String contents = describe(end.getItem(0)) + "|" + describe(end.getItem(COPPER_SLOTS - 1));
        helper.assertTrue(contents.equals("5 diamond|64 oak_log"), "contents after three upgrades: " + contents);
        helper.assertTrue(end.getCustomName() != null && end.getCustomName().getString().equals("Hoard"),
                "the name was lost on the way: " + end.getCustomName());
        helper.assertTrue(helper.getBlockState(pos).getValue(ChestBlock.FACING) == Direction.EAST, "the facing was lost");
        helper.succeed();
    }

    /**
     * A double copper chest is upgraded as a whole: with one cracked diamond the hammer refuses (the
     * chest just opens), with two both halves become reinforced chests that still form one double
     * chest, each half keeps its own items, and both diamonds are gone. The next step to netherite
     * does the same with two nuggets.
     */
    public static void doubleChestUpgradesBothHalvesTogether(GameTestHelper helper) {
        ServerPlayer player = smith(helper);
        BlockPos left = new BlockPos(1, 1, 1);
        BlockPos right = new BlockPos(2, 1, 1);
        Block copper = vanillaBlock("copper_chest");
        placeDouble(helper, copper, left, right);
        container(helper, left).setItem(3, new ItemStack(Items.IRON_INGOT, 12));
        container(helper, right).setItem(20, new ItemStack(Items.GOLD_INGOT, 7));

        // One diamond is not enough for two blocks.
        arm(player, new ItemStack(ModItems.STONE_SLEDGEHAMMER), new ItemStack(ModItems.CRACKED_DIAMOND, 1));
        standOn(helper, player, left);
        player.containerMenu = player.inventoryMenu;
        click(helper, player, left);
        helper.assertFalse(SledgehammerUpgrades.hasJob(player), "one cracked diamond started the upgrade of a double chest");
        helper.assertFalse(player.isUsingItem(), "one cracked diamond started the hammer on a double chest");
        player.containerMenu = player.inventoryMenu;

        hammer(helper, player, left, ModItems.STONE_SLEDGEHAMMER, ModItems.CRACKED_DIAMOND, 3, "double copper -> reinforced");
        helper.assertTrue(player.getOffhandItem().getCount() == 1,
                "double copper -> reinforced: diamonds left " + player.getOffhandItem().getCount() + ", expected 3 - 2 = 1");
        assertDouble(helper, ModBlocks.REINFORCED_CHEST, left, right, "double copper -> reinforced");
        hammer(helper, player, right, ModItems.DIAMOND_SLEDGEHAMMER, ModItems.NETHERITE_NUGGET, 2, "double reinforced -> netherite");
        helper.assertTrue(player.getOffhandItem().isEmpty(), "double reinforced -> netherite: both nuggets should be gone");
        assertDouble(helper, ModBlocks.NETHERITE_CHEST, left, right, "double reinforced -> netherite");

        String contents = describe(container(helper, left).getItem(3)) + "|" + describe(container(helper, right).getItem(20));
        helper.assertTrue(contents.equals("12 iron_ingot|7 gold_ingot"), "contents of the two halves: " + contents);
        Container both = ChestBlock.getContainer((ChestBlock) ModBlocks.NETHERITE_CHEST, helper.getBlockState(left),
                helper.getLevel(), helper.absolutePos(left), true);
        helper.assertTrue(both != null && both.getContainerSize() == 2 * NETHERITE_SLOTS,
                "the upgraded double chest is " + (both == null ? "no container" : both.getContainerSize() + " slots"));
        helper.succeed();
    }

    // =====================================================================================
    // SLOTS, STACKS, PAIRING
    // =====================================================================================

    /**
     * Slot counts 36/45/54 (double 72/90/108), stack limits x1/x2/x4 for stackable items (and still 1
     * for a sword), and a menu that shows every slot without scrolling: at most 18 columns and 6 rows,
     * 338 x 222 at most, which fits the default GUI scale 4 at 1080p (480 x 270).
     */
    public static void slotCountsAndStackLimitsFollowTheTier(GameTestHelper helper) {
        List<String> summary = new ArrayList<>();
        Block[] chests = {ModBlocks.REINFORCED_CHEST, ModBlocks.NETHERITE_CHEST, ModBlocks.ENDERITE_CHEST};
        for (int i = 0; i < chests.length; i++) {
            BlockPos pos = new BlockPos(1 + 2 * i, 1, 1);
            helper.setBlock(pos, chests[i].defaultBlockState());
            TieredChestBlockEntity chest = (TieredChestBlockEntity) container(helper, pos);
            summary.add(chest.getContainerSize() + "/" + chest.getMaxStackSize(new ItemStack(Items.COBBLESTONE))
                    + "/" + chest.getMaxStackSize(new ItemStack(Items.ENDER_PEARL))
                    + "/" + chest.getMaxStackSize(new ItemStack(Items.IRON_SWORD)));
            Inventory inventory = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL).getInventory();
            for (boolean isDouble : new boolean[]{false, true}) {
                TieredChestMenu menu = new TieredChestMenu(0, inventory, TieredChestOpenData.of(chest.tier(), isDouble));
                summary.add((isDouble ? "D" : "S") + menu.chestSlotCount() + ":" + menu.columns() + "x" + menu.rows()
                        + "@" + menu.imageWidth() + "x" + menu.imageHeight());
                helper.assertTrue(menu.imageWidth() <= 480 - 20 && menu.imageHeight() <= 270 - 20,
                        "the " + chest.tier() + " menu does not fit GUI scale 4 at 1080p: " + menu.imageWidth() + "x" + menu.imageHeight());
            }
        }
        String expected = "[36/64/16/1, S36:9x4@176x186, D72:12x6@230x222, "
                + "45/128/32/1, S45:9x5@176x204, D90:15x6@284x222, "
                + "54/256/64/1, S54:9x6@176x222, D108:18x6@338x222]";
        helper.assertTrue(summary.toString().equals(expected), "slots/stacks/menus: expected " + expected + " but were " + summary);
        helper.succeed();
    }

    /**
     * Double chests form only from two chests of the same tier, placed the way a player places them:
     * two reinforced chests side by side pair up, a netherite chest beside a reinforced one and a
     * reinforced chest beside a copper chest stay single.
     */
    public static void doubleChestsFormOnlyFromEqualTiers(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setShiftKeyDown(false);
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        place(helper, player, ModItems.REINFORCED_CHEST, new BlockPos(1, 1, 1));
        place(helper, player, ModItems.REINFORCED_CHEST, new BlockPos(2, 1, 1));
        place(helper, player, ModItems.REINFORCED_CHEST, new BlockPos(1, 1, 4));
        place(helper, player, ModItems.NETHERITE_CHEST, new BlockPos(2, 1, 4));
        place(helper, player, Items.COPPER_CHEST.weathering().unaffected(), new BlockPos(1, 1, 7));
        place(helper, player, ModItems.REINFORCED_CHEST, new BlockPos(2, 1, 7));
        String types = type(helper, 1, 1) + "," + type(helper, 2, 1) + "|" + type(helper, 1, 4) + "," + type(helper, 2, 4)
                + "|" + type(helper, 1, 7) + "," + type(helper, 2, 7);
        helper.assertTrue(types.equals("right/south,left/south|single/south,single/south|single/south,single/south"),
                "chest types after placing (same tier | netherite beside reinforced | reinforced beside copper): " + types);
        helper.succeed();
    }

    // =====================================================================================
    // AUTOMATION
    // =====================================================================================

    /**
     * Hoppers fill the oversized slots up to the tier limit, alone and in a double chest: vanilla's
     * own transfer ({@code HopperBlockEntity#addItem}) tops a netherite slot of 127 up to 128 and an
     * enderite slot of 255 up to 256 and then moves on to the next slot, and a real vanilla hopper
     * pushes into a netherite chest whose every slot holds 64 (vanilla would call it full) and pulls
     * single items out of a slot of 120.
     */
    public static void vanillaHoppersFillAndEmptyOversizedSlots(GameTestHelper helper) {
        BlockPos netherite = new BlockPos(1, 1, 1);
        helper.setBlock(netherite, ModBlocks.NETHERITE_CHEST.defaultBlockState());
        Container single = container(helper, netherite);
        single.setItem(0, new ItemStack(Items.COBBLESTONE, 127));
        ItemStack rest = HopperBlockEntity.addItem(null, single, new ItemStack(Items.COBBLESTONE, 2), Direction.UP);
        String netheriteLine = single.getItem(0).getCount() + "+" + single.getItem(1).getCount() + " rest " + rest.getCount();
        helper.assertTrue(netheriteLine.equals("128+1 rest 0"), "netherite slot 0 at 127 plus 2 cobblestone: " + netheriteLine);

        BlockPos left = new BlockPos(4, 1, 1);
        BlockPos right = new BlockPos(5, 1, 1);
        placeDouble(helper, ModBlocks.ENDERITE_CHEST, left, right);
        Container both = ChestBlock.getContainer((ChestBlock) ModBlocks.ENDERITE_CHEST, helper.getBlockState(left),
                helper.getLevel(), helper.absolutePos(left), true);
        helper.assertTrue(both != null && both.getContainerSize() == 2 * ENDERITE_SLOTS, "the enderite double chest is no double container");
        both.setItem(ENDERITE_SLOTS, new ItemStack(Items.STONE, 255));
        for (int i = 0; i < ENDERITE_SLOTS; i++) {
            both.setItem(i, new ItemStack(Items.DIRT, 256));
        }
        HopperBlockEntity.addItem(null, both, new ItemStack(Items.STONE, 2), Direction.UP);
        String doubleLine = both.getItem(ENDERITE_SLOTS).getCount() + "+" + both.getItem(ENDERITE_SLOTS + 1).getCount();
        helper.assertTrue(doubleLine.equals("256+1"), "enderite double chest, slot at 255 plus 2 stone: " + doubleLine);

        // Real hoppers: one pushes into a netherite chest full of 64s, one pulls out of a slot of 120.
        BlockPos fed = new BlockPos(1, 1, 4);
        helper.setBlock(fed, ModBlocks.NETHERITE_CHEST.defaultBlockState());
        Container fedChest = container(helper, fed);
        for (int i = 0; i < NETHERITE_SLOTS; i++) {
            fedChest.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        }
        helper.setBlock(fed.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        ((Container) helper.getLevel().getBlockEntity(helper.absolutePos(fed.above()))).setItem(0, new ItemStack(Items.COBBLESTONE, 2));

        BlockPos drained = new BlockPos(4, 2, 4);
        helper.setBlock(drained, ModBlocks.NETHERITE_CHEST.defaultBlockState());
        container(helper, drained).setItem(0, new ItemStack(Items.COBBLESTONE, 120));
        helper.setBlock(drained.below(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));

        helper.succeedWhen(() -> {
            int total = 0;
            for (int i = 0; i < NETHERITE_SLOTS; i++) {
                total += fedChest.getItem(i).getCount();
            }
            helper.assertTrue(total == NETHERITE_SLOTS * 64 + 2 && fedChest.getItem(0).getCount() == 66,
                    "the hopper did not push its 2 cobblestone into the netherite chest full of 64s (total " + total
                            + ", slot 0 " + fedChest.getItem(0).getCount() + ")");
            Container hopper = (Container) helper.getLevel().getBlockEntity(helper.absolutePos(drained.below()));
            int pulled = hopper.getItem(0).getCount();
            int remaining = container(helper, drained).getItem(0).getCount();
            helper.assertTrue(pulled >= 2 && pulled + remaining == 120,
                    "the hopper below pulled " + pulled + ", the slot of 120 holds " + remaining);
        });
    }

    /**
     * The comparator reads a tier chest against its own limit: a netherite chest with every slot at
     * 64 (half of 128) gives 8, at 128 gives 15. Vanilla's formula (64 is full) would give 15 for both.
     */
    public static void comparatorReadsOversizedSlotsAgainstTheTierLimit(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.NETHERITE_CHEST.defaultBlockState());
        Container chest = container(helper, pos);
        for (int i = 0; i < NETHERITE_SLOTS; i++) {
            chest.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        }
        BlockState state = helper.getBlockState(pos);
        int half = state.getAnalogOutputSignal(helper.getLevel(), helper.absolutePos(pos), Direction.NORTH);
        for (int i = 0; i < NETHERITE_SLOTS; i++) {
            chest.setItem(i, new ItemStack(Items.COBBLESTONE, 128));
        }
        int full = state.getAnalogOutputSignal(helper.getLevel(), helper.absolutePos(pos), Direction.NORTH);
        helper.assertTrue(half == 8 && full == 15, "comparator signal half/full: " + half + "/" + full + ", expected 8/15");
        helper.succeed();
    }

    /**
     * An enderite chest saves a slot of 256 (vanilla's item codec stops at 99) and loads it back
     * whole; the vanilla "Items" list alone still holds a readable stack of 99.
     */
    public static void oversizedStacksSurviveSavingAndLoading(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.ENDERITE_CHEST.defaultBlockState());
        TieredChestBlockEntity chest = (TieredChestBlockEntity) container(helper, pos);
        chest.setItem(5, new ItemStack(Items.COBBLESTONE, 256));
        chest.setItem(6, new ItemStack(Items.ENDER_PEARL, 40));
        CompoundTag tag = chest.saveWithoutMetadata(helper.getLevel().registryAccess());
        helper.assertTrue(tag.toString().contains("\"count\":99") || tag.toString().contains("count:99"),
                "the vanilla item list does not hold the capped stack of 99: " + tag);
        chest.clearContent();
        chest.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
        String loaded = describe(chest.getItem(5)) + "|" + describe(chest.getItem(6));
        helper.assertTrue(loaded.equals("256 cobblestone|40 ender_pearl"), "loaded back: " + loaded);
        helper.succeed();
    }

    // =====================================================================================
    // ITEMS
    // =====================================================================================

    /** Rarity COMMON/UNCOMMON/EPIC (docs/RARITAETEN.md), netherite and enderite fire resistant, ids and blocks. */
    public static void chestItemsFollowTheFamilyScheme(GameTestHelper helper) {
        List<String> summary = new ArrayList<>();
        for (Item item : List.of(ModItems.REINFORCED_CHEST, ModItems.NETHERITE_CHEST, ModItems.ENDERITE_CHEST)) {
            ItemStack stack = new ItemStack(item);
            summary.add(BuiltInRegistries.ITEM.getKey(item).getPath() + ":" + stack.getRarity()
                    + ":" + (stack.has(DataComponents.DAMAGE_RESISTANT) ? "fireproof" : "burns")
                    + ":" + (item instanceof net.minecraft.world.item.BlockItem block && block.getBlock() instanceof TieredChestBlock));
        }
        String expected = "[reinforced_chest:" + Rarity.COMMON + ":burns:true, netherite_chest:" + Rarity.UNCOMMON
                + ":fireproof:true, enderite_chest:" + Rarity.EPIC + ":fireproof:true]";
        helper.assertTrue(summary.toString().equals(expected), "chest items: expected " + expected + " but were " + summary);
        helper.succeed();
    }

    // =====================================================================================
    // TRAPPED CHESTS (26.3)
    // =====================================================================================

    public static void trappedCapacityComparatorAndPropertiesMatchNormalTiers(GameTestHelper helper) {
        if (!com.simplebuilding.version.McVersion.TRAPPED_TIERED_CHESTS) { helper.succeed(); return; }
        Block[] normal = {ModBlocks.REINFORCED_CHEST, ModBlocks.NETHERITE_CHEST, ModBlocks.ENDERITE_CHEST};
        Block[] trapped = trappedChests();
        int[] slots = {36, 45, 54};
        int[] limits = {64, 128, 256};
        for (int i = 0; i < trapped.length; i++) {
            BlockPos pos = new BlockPos(1 + i * 3, 1, 1);
            helper.setBlock(pos, trapped[i]);
            TieredChestBlockEntity chest = (TieredChestBlockEntity) container(helper, pos);
            helper.assertValueEqual(chest.getContainerSize(), slots[i], "single capacity");
            helper.assertValueEqual(chest.getMaxStackSize(new ItemStack(Items.STONE)), limits[i], "stone limit");
            helper.assertValueEqual(chest.getMaxStackSize(new ItemStack(Items.IRON_SWORD)), 1, "unstackable limit");
            BlockState state = helper.getBlockState(pos);
            BlockPos absolute = helper.absolutePos(pos);
            helper.assertTrue(state.getDestroySpeed(helper.getLevel(), absolute) == normal[i].defaultBlockState().getDestroySpeed(helper.getLevel(), absolute)
                    && trapped[i].getExplosionResistance() == normal[i].getExplosionResistance(), "tier hardness and blast resistance");
            ItemStack item = new ItemStack(trapped[i]);
            ItemStack base = new ItemStack(normal[i]);
            helper.assertTrue(item.getRarity() == base.getRarity()
                    && java.util.Objects.equals(item.get(DataComponents.DAMAGE_RESISTANT), base.get(DataComponents.DAMAGE_RESISTANT)),
                    "tier item rarity and fire resistance");
            helper.assertValueEqual(state.getAnalogOutputSignal(helper.getLevel(), absolute, Direction.NORTH), 0, "empty comparator");
            for (int slot = 0; slot < slots[i]; slot++) chest.setItem(slot, new ItemStack(Items.STONE, limits[i] / 2));
            helper.assertValueEqual(state.getAnalogOutputSignal(helper.getLevel(), absolute, Direction.NORTH), 8, "half comparator");
            for (int slot = 0; slot < slots[i]; slot++) chest.setItem(slot, new ItemStack(Items.STONE, limits[i]));
            helper.assertValueEqual(state.getAnalogOutputSignal(helper.getLevel(), absolute, Direction.NORTH), 15, "full comparator");
            BlockPos other = pos.east();
            placeDouble(helper, trapped[i], pos, other);
            Container both = ChestBlock.getContainer((ChestBlock) trapped[i], helper.getBlockState(pos), helper.getLevel(), absolute, true);
            helper.assertTrue(both != null && both.getContainerSize() == slots[i] * 2, "double capacity");
        }
        helper.succeed();
    }

    public static void trappedDoubleChestsRequireTheSameKind(GameTestHelper helper) {
        if (!com.simplebuilding.version.McVersion.TRAPPED_TIERED_CHESTS) { helper.succeed(); return; }
        ServerPlayer player = smith(helper);
        Block[] trapped = trappedChests();
        Block[] normal = {ModBlocks.REINFORCED_CHEST, ModBlocks.NETHERITE_CHEST, ModBlocks.ENDERITE_CHEST};
        for (int i = 0; i < trapped.length; i++) {
            for (Block candidate : new Block[]{trapped[i], normal[i], trapped[(i + 1) % 3], Blocks.TRAPPED_CHEST}) {
                BlockPos first = new BlockPos(1, 1, 1);
                BlockPos second = first.east();
                helper.setBlock(first, Blocks.AIR);
                helper.setBlock(second, Blocks.AIR);
                place(helper, player, trapped[i].asItem(), first);
                place(helper, player, candidate.asItem(), second);
                boolean same = candidate == trapped[i];
                helper.assertTrue((helper.getBlockState(first).getValue(ChestBlock.TYPE) != ChestType.SINGLE) == same
                        && (helper.getBlockState(second).getValue(ChestBlock.TYPE) != ChestType.SINGLE) == same,
                        "pairing " + trapped[i] + " with " + candidate);
            }
        }
        helper.succeed();
    }

    public static void trappedRecipesUseTheMatchingChestAndHook(GameTestHelper helper) {
        if (!com.simplebuilding.version.McVersion.TRAPPED_TIERED_CHESTS) { helper.succeed(); return; }
        Item[] normal = {ModItems.REINFORCED_CHEST, ModItems.NETHERITE_CHEST, ModItems.ENDERITE_CHEST};
        Block[] trapped = trappedChests();
        for (int i = 0; i < normal.length; i++) {
            for (boolean reverse : new boolean[]{false, true}) {
                var grid = net.minecraft.world.item.crafting.CraftingInput.of(2, 2, reverse
                        ? List.of(new ItemStack(Items.TRIPWIRE_HOOK), ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(normal[i]))
                        : List.of(new ItemStack(normal[i]), ItemStack.EMPTY, new ItemStack(Items.TRIPWIRE_HOOK), ItemStack.EMPTY));
                var recipe = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                        net.minecraft.world.item.crafting.RecipeType.CRAFTING, grid, helper.getLevel());
                helper.assertTrue(recipe.isPresent(), "chest + hook recipe exists in both arrangements");
                ItemStack result = recipe.orElseThrow().value().assemble(grid);
                helper.assertTrue(result.is(trapped[i].asItem()) && result.getCount() == 1, "matching trapped tier output");
            }
            var blockTag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
                    Identifier.fromNamespaceAndPath("c", "chests/trapped"));
            var itemTag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                    Identifier.fromNamespaceAndPath("c", "chests/trapped"));
            helper.assertTrue(trapped[i].defaultBlockState().is(blockTag) && new ItemStack(trapped[i]).is(itemTag), "common trapped tags");
            BlockPos pos = new BlockPos(1 + i * 2, 1, 1);
            helper.setBlock(pos, trapped[i]);
            BaseContainerBlockEntity chest = container(helper, pos);
            chest.applyComponents(DataComponentMap.builder().set(DataComponents.CUSTOM_NAME, Component.literal("Wired"))
                    .build(), DataComponentPatch.EMPTY);
            List<ItemStack> drops = Block.getDrops(helper.getBlockState(pos), helper.getLevel(), helper.absolutePos(pos), chest);
            helper.assertTrue(drops.size() == 1 && drops.getFirst().is(trapped[i].asItem())
                    && drops.getFirst().getHoverName().getString().equals("Wired"), "named chest loot");
        }
        helper.succeed();
    }

    public static void trappedSignalCountsViewersAndUpdatesNeighbors(GameTestHelper helper) {
        if (!com.simplebuilding.version.McVersion.TRAPPED_TIERED_CHESTS) { helper.succeed(); return; }
        ServerPlayer player = smith(helper);
        ServerPlayer second = smith(helper);
        BlockPos pos = new BlockPos(2, 2, 2);
        BlockPos wire = pos.east();
        BlockPos lowerWire = pos.below().west();
        helper.setBlock(wire.below(), Blocks.STONE);
        helper.setBlock(lowerWire.below(), Blocks.STONE);
        for (Block block : trappedChests()) {
            helper.setBlock(pos.below(), Blocks.STONE);
            helper.setBlock(pos, block);
            helper.setBlock(wire, Blocks.REDSTONE_WIRE);
            helper.setBlock(lowerWire, Blocks.REDSTONE_WIRE);
            TieredChestBlockEntity chest = (TieredChestBlockEntity) container(helper, pos);
            helper.assertTrue(chest.getType().isValid(block.defaultBlockState()), "entity type supports trapped block");
            standOn(helper, player, pos);
            openChestMenu(helper, player, pos);
            chest.recheckOpen();
            assertTrappedSignal(helper, pos, 1);
            helper.assertValueEqual(helper.getBlockState(wire).getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWER), 1, "neighbor wire updates on open");
            helper.assertValueEqual(helper.getBlockState(lowerWire).getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWER), 1, "block below powers its neighbor");
            standOn(helper, second, pos);
            openChestMenu(helper, second, pos);
            chest.recheckOpen();
            assertTrappedSignal(helper, pos, 2);
            second.closeContainer();
            assertTrappedSignal(helper, pos, 1);
            player.closeContainer();
            assertTrappedSignal(helper, pos, 0);
            helper.assertValueEqual(helper.getBlockState(wire).getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWER), 0, "neighbor wire updates on close");
            helper.assertValueEqual(helper.getBlockState(lowerWire).getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWER), 0, "block below updates on close");
            var spectator = helper.makeMockPlayer(net.minecraft.world.level.GameType.SPECTATOR);
            helper.assertTrue(spectator.isSpectator(), "test player is a spectator");
            chest.startOpen(spectator);
            assertTrappedSignal(helper, pos, 0);
            // Exercise the same counter callbacks as menus, beyond the vanilla redstone cap.
            for (int count = 1; count <= 16; count++) {
                chest.startOpen(player);
                assertTrappedSignal(helper, pos, Math.min(15, count));
            }
            for (int count = 15; count >= 0; count--) {
                chest.stopOpen(player);
                assertTrappedSignal(helper, pos, Math.min(15, count));
            }
        }
        // Both halves count every viewer of the shared menu.
        BlockPos other = pos.east();
        helper.setBlock(other, Blocks.AIR);
        placeDouble(helper, ModBlocks.ENDERITE_TRAPPED_CHEST, pos, other);
        standOn(helper, player, pos);
        openChestMenu(helper, player, pos);
        assertTrappedSignal(helper, pos, 1);
        assertTrappedSignal(helper, other, 1);
        player.closeContainer();
        assertTrappedSignal(helper, pos, 0);
        assertTrappedSignal(helper, other, 0);
        helper.succeed();
    }

    public static void trappedUpgradesKeepBothHalvesAndContents(GameTestHelper helper) {
        if (!com.simplebuilding.version.McVersion.TRAPPED_TIERED_CHESTS) { helper.succeed(); return; }
        ServerPlayer player = smith(helper);
        BlockPos left = new BlockPos(1, 1, 1);
        BlockPos right = left.east();
        placeDouble(helper, ModBlocks.REINFORCED_TRAPPED_CHEST, left, right);
        container(helper, left).applyComponents(DataComponentMap.builder()
                .set(DataComponents.CUSTOM_NAME, Component.literal("Redstone stock")).build(), DataComponentPatch.EMPTY);
        container(helper, left).setItem(3, new ItemStack(Items.IRON_INGOT, 12));
        container(helper, right).setItem(20, new ItemStack(Items.GOLD_INGOT, 7));
        hammer(helper, player, left, ModItems.DIAMOND_SLEDGEHAMMER, ModItems.NETHERITE_NUGGET, 2, "trapped -> netherite");
        assertDouble(helper, ModBlocks.NETHERITE_TRAPPED_CHEST, left, right, "trapped netherite");
        helper.assertTrue(player.getOffhandItem().isEmpty(), "two netherite nuggets consumed");
        hammer(helper, player, left, ModItems.NETHERITE_SLEDGEHAMMER, ModItems.ENDERITE_NUGGET, 2, "trapped -> enderite");
        assertDouble(helper, ModBlocks.ENDERITE_TRAPPED_CHEST, left, right, "trapped enderite");
        helper.assertTrue(player.getOffhandItem().isEmpty(), "two enderite nuggets consumed");
        helper.assertValueEqual(container(helper, left).getItem(3).getCount(), 12, "first half contents");
        helper.assertValueEqual(container(helper, right).getItem(20).getCount(), 7, "second half contents");
        helper.assertValueEqual(container(helper, left).getName().getString(), "Redstone stock", "custom name");
        helper.succeed();
    }

    public static void trappedTitlesMatchNormalTiers(GameTestHelper helper) {
        if (!com.simplebuilding.version.McVersion.TRAPPED_TIERED_CHESTS) { helper.succeed(); return; }
        Block[] normal = {ModBlocks.REINFORCED_CHEST, ModBlocks.NETHERITE_CHEST, ModBlocks.ENDERITE_CHEST};
        Block[] trapped = {ModBlocks.REINFORCED_TRAPPED_CHEST, ModBlocks.NETHERITE_TRAPPED_CHEST, ModBlocks.ENDERITE_TRAPPED_CHEST};
        for (int i = 0; i < normal.length; i++) {
            BlockPos normalPos = new BlockPos(1 + 2 * i, 1, 1);
            BlockPos trappedPos = normalPos.east();
            helper.setBlock(normalPos, normal[i].defaultBlockState());
            helper.setBlock(trappedPos, trapped[i].defaultBlockState());
            var normalProvider = helper.getBlockState(normalPos).getMenuProvider(helper.getLevel(), helper.absolutePos(normalPos));
            var trappedProvider = helper.getBlockState(trappedPos).getMenuProvider(helper.getLevel(), helper.absolutePos(trappedPos));
            helper.assertTrue(normalProvider != null && trappedProvider != null, "both chest kinds offer a menu");
            String normalTitle = normalProvider.getDisplayName().getString();
            String trappedTitle = trappedProvider.getDisplayName().getString();
            helper.assertTrue(normalTitle.equals(trappedTitle),
                    "tier " + i + " trapped title is '" + trappedTitle + "' instead of '" + normalTitle + "'");
        }
        helper.succeed();
    }

    private static Block[] trappedChests() {
        return new Block[]{ModBlocks.REINFORCED_TRAPPED_CHEST, ModBlocks.NETHERITE_TRAPPED_CHEST, ModBlocks.ENDERITE_TRAPPED_CHEST};
    }

    private static void openChestMenu(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
        // Like HopperTests: NeoForge's mock connection rejects advanced_open_screen. Exercise the
        // real block menu provider and its lifecycle without pretending to have a connected client.
        var provider = helper.getBlockState(pos).getMenuProvider(helper.getLevel(), helper.absolutePos(pos));
        helper.assertTrue(provider != null, "unblocked chest offers a menu");
        var menu = provider.createMenu(1, player.getInventory(), player);
        helper.assertTrue(menu instanceof TieredChestMenu, "chest offers the tier menu");
        player.containerMenu = menu;
    }

    private static void assertTrappedSignal(GameTestHelper helper, BlockPos pos, int expected) {
        BlockState state = helper.getBlockState(pos);
        for (Direction direction : Direction.values()) {
            helper.assertValueEqual(state.getSignal(helper.getLevel(), helper.absolutePos(pos), direction), expected, "weak signal " + direction);
            helper.assertValueEqual(state.getDirectSignal(helper.getLevel(), helper.absolutePos(pos), direction),
                    direction == Direction.UP ? expected : 0, "strong signal " + direction);
        }
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    private static Block vanillaBlock(String path) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(path));
    }

    private static BaseContainerBlockEntity container(GameTestHelper helper, BlockPos pos) {
        return (BaseContainerBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }

    private static String describe(ItemStack stack) {
        return stack.isEmpty() ? "empty" : stack.getCount() + " " + BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }

    /** A double chest facing north: the LEFT half connects east (clockwise), the RIGHT half west. */
    private static void placeDouble(GameTestHelper helper, Block chest, BlockPos left, BlockPos right) {
        helper.setBlock(left, chest.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.LEFT));
        helper.setBlock(right, chest.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.RIGHT));
    }

    private static void assertTier(GameTestHelper helper, BlockPos pos, Block tier, int slots, String what) {
        BlockState state = helper.getBlockState(pos);
        helper.assertTrue(state.is(tier), what + ": the chest is " + state + " after the upgrade");
        helper.assertTrue(container(helper, pos).getContainerSize() == slots,
                what + ": " + container(helper, pos).getContainerSize() + " slots, expected " + slots);
    }

    private static void assertDouble(GameTestHelper helper, Block tier, BlockPos left, BlockPos right, String what) {
        String line = helper.getBlockState(left).getBlock().getDescriptionId() + ":" + helper.getBlockState(left).getValue(ChestBlock.TYPE)
                + "," + helper.getBlockState(right).getBlock().getDescriptionId() + ":" + helper.getBlockState(right).getValue(ChestBlock.TYPE);
        String expected = tier.getDescriptionId() + ":" + ChestType.LEFT + "," + tier.getDescriptionId() + ":" + ChestType.RIGHT;
        helper.assertTrue(line.equals(expected), what + ": halves are " + line + ", expected " + expected);
    }

    private static String type(GameTestHelper helper, int x, int z) {
        BlockState state = helper.getBlockState(new BlockPos(x, 1, z));
        return state.hasProperty(ChestBlock.TYPE)
                ? state.getValue(ChestBlock.TYPE).getSerializedName() + "/" + state.getValue(ChestBlock.FACING).getSerializedName() : "none";
    }

    /**
     * Places a chest item the way a player standing south of it and facing north does, through
     * {@code BlockItem#place}: {@code getStateForPlacement} picks the partner exactly as in the game
     * (vanilla {@code ChestBlock#getChestType} / {@code candidatePartnerFacing}). The context names the
     * test level explicitly - the mock player's own {@code level()} is not the one the test builds in.
     */
    private static void place(GameTestHelper helper, ServerPlayer player, Item item, BlockPos pos) {
        ItemStack stack = new ItemStack(item);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        Vec3 feet = helper.absoluteVec(new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 3.5));
        player.snapTo(feet.x, feet.y, feet.z, 180.0F, 30.0F);
        player.setYHeadRot(180.0F);
        // A click into the (air, so replaceable) target cell itself: the context places right there.
        BlockPos target = helper.absolutePos(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atBottomCenterOf(target), Direction.UP, target, false);
        net.minecraft.world.InteractionResult result = ((net.minecraft.world.item.BlockItem) item).place(
                new net.minecraft.world.item.context.BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit));
        helper.assertTrue(result.consumesAction() && !helper.getBlockState(pos).isAir(),
                "placing " + item + " at " + pos + " failed: " + result);
    }

    private static ServerPlayer smith(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getAbilities().instabuild = false;
        player.setShiftKeyDown(false);
        helper.runBeforeTestEnd(() -> {
            player.containerMenu = player.inventoryMenu;
            helper.getLevel().getServer().getPlayerList().remove(player);
        });
        return player;
    }

    private static void arm(ServerPlayer player, ItemStack hammer, ItemStack material) {
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        player.setItemInHand(InteractionHand.OFF_HAND, material);
    }

    private static void standOn(GameTestHelper helper, ServerPlayer player, BlockPos chest) {
        Vec3 feet = helper.absoluteVec(new Vec3(chest.getX() + 0.5, chest.getY() + 1.0, chest.getZ() + 0.5));
        player.snapTo(feet.x, feet.y, feet.z, 0.0F, 90.0F);
    }

    private static void click(GameTestHelper helper, ServerPlayer player, BlockPos chest) {
        BlockPos absolute = helper.absolutePos(chest);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).add(0.0, 0.375, 0.0), Direction.UP, absolute, false);
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
    }

    /** One upgrade from click to swap: the click must reach the hammer, then 100 player ticks. */
    private static void hammer(GameTestHelper helper, ServerPlayer player, BlockPos chest, Item hammerItem, Item material,
                               int materialCount, String what) {
        ItemStack hammer = new ItemStack(hammerItem);
        arm(player, hammer, new ItemStack(material, materialCount));
        standOn(helper, player, chest);
        player.containerMenu = player.inventoryMenu;
        click(helper, player, chest);
        helper.assertTrue(player.containerMenu == player.inventoryMenu, what + ": the click opened the chest instead of the hammer");
        helper.assertTrue(player.isUsingItem() && SledgehammerUpgrades.hasJob(player), what + ": the click did not start the upgrade");
        for (int tick = 1; tick <= UPGRADE_TICKS; tick++) {
            player.connection.tick();
        }
        helper.assertFalse(SledgehammerUpgrades.hasJob(player), what + ": the job outlived the upgrade");
        player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(hammer));
    }
}

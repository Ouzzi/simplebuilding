package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.EndRailBlock;
import com.simplebuilding.blocks.custom.EndRailPhysics;
import com.simplebuilding.blocks.custom.EndSignalBlock;
import com.simplebuilding.config.ServerTuning;
import com.simplebuilding.util.AstralStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class EndSystemsTests {
    public static void redstoneRecipesYieldTwo(GameTestHelper helper) {
        if (!active(helper)) return;
        for (var entry : java.util.Map.of(com.simplebuilding.items.ModItems.ASTRALIT_DUST,
                com.simplebuilding.items.ModItems.ASTRAL_REDSTONE, com.simplebuilding.items.ModItems.NIHILITH_SHARD,
                com.simplebuilding.items.ModItems.NIHIL_REDSTONE).entrySet()) {
            var input = net.minecraft.world.item.crafting.CraftingInput.of(2, 1,
                    java.util.List.of(new ItemStack(entry.getKey()), new ItemStack(Items.REDSTONE)));
            var recipe = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                    net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow();
            ItemStack result = recipe.value().assemble(input);
            helper.assertTrue(result.is(entry.getValue()) && result.getCount() == 2, "redstone recipe must yield two: " + result);
        }
        helper.succeed();
    }

    public static void redstoneAliasesResolveItemsAndBlocks(GameTestHelper helper) {
        if (!active(helper)) return;
        var items = net.minecraft.core.registries.BuiltInRegistries.ITEM;
        var blocks = net.minecraft.core.registries.BuiltInRegistries.BLOCK;
        for (var entry : java.util.Map.of("astralit_powder", "astral_redstone",
                "nihilith_powder", "nihil_redstone").entrySet()) {
            var oldId = net.minecraft.resources.Identifier.fromNamespaceAndPath("simplebuilding", entry.getKey());
            var newId = net.minecraft.resources.Identifier.fromNamespaceAndPath("simplebuilding", entry.getValue());
            helper.assertTrue(items.get(newId).isPresent() && blocks.get(newId).isPresent(), "new redstone id missing: " + newId);
            assertAlias(helper, items, oldId, newId);
            assertAlias(helper, blocks, oldId, newId);
        }
        var itemOnly = net.minecraft.resources.Identifier.fromNamespaceAndPath("simplebuilding", "ore_detector");
        helper.assertTrue(com.simplebuilding.datafix.LegacyItemIds.renamedIn(blocks, itemOnly) == null,
                "item-only alias leaked into block registry");
        helper.succeed();
    }

    private static <T> void assertAlias(GameTestHelper helper, net.minecraft.core.Registry<T> registry,
            net.minecraft.resources.Identifier oldId, net.minecraft.resources.Identifier newId) {
        T expected = registry.getValue(newId);
        var key = net.minecraft.resources.ResourceKey.create(registry.key(), oldId);
        helper.assertTrue(registry.getValue(oldId) == expected && registry.getValue(key) == expected,
                "value alias missing in " + registry.key() + ": " + oldId);
        helper.assertTrue(registry.get(oldId).orElseThrow().value() == expected
                && registry.get(key).orElseThrow().value() == expected && registry.containsKey(oldId),
                "holder alias missing in " + registry.key() + ": " + oldId);
        helper.assertTrue(newId.equals(registry.getKey(expected)), "alias became canonical: " + oldId);
        helper.assertTrue(net.minecraft.resources.ResourceKey.create(registry.key(), newId).equals(
                com.simplebuilding.datafix.LegacyItemIds.renamedIn(registry, key)), "alias changed registry key");
    }

    private static boolean active(GameTestHelper helper) {
        if (com.simplebuilding.version.McVersion.END_SYSTEMS) return true;
        helper.succeed();
        return false;
    }

    public static void vaultSharesOnlyItsFirstHalfAndPersists(GameTestHelper helper) {
        if (!active(helper)) return;
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        var vanilla = player.getEnderChestInventory();
        var extra = ((AstralStorage) vanilla).simplebuilding$astralStorage();
        var vault = new CompoundContainer(vanilla, extra);
        helper.assertTrue(vanilla.getContainerSize() == 27 && vault.getContainerSize() == 54, "wrong slot counts");
        vault.setItem(0, new ItemStack(Items.DIAMOND, 3));
        vault.setItem(26, new ItemStack(Items.EMERALD, 4));
        vault.setItem(27, new ItemStack(Items.GOLD_INGOT, 5));
        vault.setItem(53, new ItemStack(Items.IRON_INGOT, 6));
        helper.assertTrue(vanilla.getItem(0).getCount() == 3 && vanilla.getItem(26).getCount() == 4, "first half is not shared");
        vanilla.removeItem(0, 1);
        helper.assertTrue(vault.getItem(0).getCount() == 2, "vanilla edits did not reach the vault");
        var small = ChestMenu.threeRows(1, player.getInventory(), vanilla);
        var large = ChestMenu.sixRows(2, player.getInventory(), vault);
        helper.assertTrue(small.slots.size() == 63 && large.slots.size() == 90, "vanilla exposes extra slots or vault lacks them");
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        player.saveWithoutId(output);
        var loaded = helper.makeMockServerPlayerInLevel();
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), output.buildResult()));
        var loadedExtra = ((AstralStorage) loaded.getEnderChestInventory()).simplebuilding$astralStorage();
        helper.assertTrue(loaded.getEnderChestInventory().getItem(0).getCount() == 2, "vanilla slots lost after load");
        helper.assertTrue(loadedExtra.getItem(0).getCount() == 5 && loadedExtra.getItem(26).getCount() == 6, "extra slots lost after load");
        var other = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(((AstralStorage) other.getEnderChestInventory()).simplebuilding$astralStorage().isEmpty(), "extra inventory leaks between players");
        helper.succeed();
    }

    public static void vaultOpensAndConfigPreservesContents(GameTestHelper helper) {
        if (!active(helper)) return;
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.ASTRAL_VAULT);
        var world = helper.getLevel();
        var absolute = helper.absolutePos(pos);
        var state = world.getBlockState(absolute);
        helper.assertTrue(BlockEntityTypes.ENDER_CHEST.isValid(state), "vault does not fit ender chest block entity");
        helper.assertTrue(world.getBlockEntity(absolute) instanceof EnderChestBlockEntity, "missing vanilla animated chest entity");
        var player = helper.makeMockServerPlayerInLevel();
        player.setPos(Vec3.atCenterOf(absolute));
        var extra = ((AstralStorage) player.getEnderChestInventory()).simplebuilding$astralStorage();
        extra.setItem(26, new ItemStack(Items.DIAMOND));
        var hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        boolean before = ServerTuning.get().features.astralVault;
        try {
            ServerTuning.get().features.astralVault = false;
            state.useWithoutItem(world, player, hit);
            helper.assertTrue(player.containerMenu == player.inventoryMenu, "disabled vault opened");
            helper.assertTrue(extra.getItem(26).is(Items.DIAMOND), "disabled vault erased contents");
            ServerTuning.get().features.astralVault = true;
            helper.setBlock(pos.above(), Blocks.STONE);
            state.useWithoutItem(world, player, hit);
            helper.assertTrue(player.containerMenu == player.inventoryMenu, "blocked lid opened");
            helper.setBlock(pos.above(), Blocks.AIR);
            state.useWithoutItem(world, player, hit);
            helper.assertTrue(player.containerMenu instanceof ChestMenu && player.containerMenu.slots.size() == 90, "vault did not open six-row menu");
            helper.assertTrue(player.getEnderChestInventory().isActiveChest((EnderChestBlockEntity) world.getBlockEntity(absolute)), "vanilla lid opener is not bound");
            player.closeContainer();
        } finally { ServerTuning.get().features.astralVault = before; }
        helper.succeed();
    }

    /** N24: Astral powder carries a signal one hop per tick, Nihil powder one hop per two ticks. */
    public static void astralSignalTravelsFasterThanNihil(GameTestHelper helper) {
        if (!active(helper)) return;
        for (int x = 0; x <= 9; x++) for (int z = 0; z <= 4; z += 2) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        // far end first, so each hop needs its own scheduled tick instead of one in-order sweep
        for (int x = 8; x >= 1; x--) {
            helper.setBlock(new BlockPos(x, 1, 0), ModBlocks.ASTRAL_REDSTONE);
            helper.setBlock(new BlockPos(x, 1, 2), ModBlocks.NIHIL_REDSTONE);
        }
        helper.setBlock(new BlockPos(0, 1, 0), ModBlocks.ASTRALIT_SWITCH.defaultBlockState().setValue(EndSignalBlock.ENABLED, true));
        helper.setBlock(new BlockPos(0, 1, 2), ModBlocks.NIHILITH_SWITCH.defaultBlockState().setValue(EndSignalBlock.ENABLED, true));
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(helper.getBlockState(new BlockPos(8, 1, 0)).getValue(EndSignalBlock.POWER) > 0, "astral signal not at the end after 12 ticks");
            helper.assertTrue(helper.getBlockState(new BlockPos(8, 1, 2)).getValue(EndSignalBlock.POWER) == 0, "nihil signal as fast as astral");
            helper.succeed();
        });
    }

    public static void channelsStayIsolatedAndStopAtFifteen(GameTestHelper helper) {
        if (!active(helper)) return;
        // Fold the 16-segment path into the loader's 8x8 room, with one-block gaps.
        var path = java.util.List.of(new BlockPos(1,1,1), new BlockPos(2,1,1), new BlockPos(3,1,1), new BlockPos(4,1,1), new BlockPos(5,1,1),
                new BlockPos(5,1,2), new BlockPos(5,1,3), new BlockPos(4,1,3), new BlockPos(3,1,3), new BlockPos(2,1,3), new BlockPos(1,1,3),
                new BlockPos(1,1,4), new BlockPos(1,1,5), new BlockPos(2,1,5), new BlockPos(3,1,5), new BlockPos(4,1,5));
        for (int x = 0; x <= 6; x++) for (int z = 0; z <= 6; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        helper.setBlock(new BlockPos(0, 1, 1), ModBlocks.NIHILITH_SWITCH.defaultBlockState().setValue(EndSignalBlock.ENABLED, true));
        for (var pos : path) helper.setBlock(pos, ModBlocks.NIHIL_REDSTONE);
        for (int x = 1; x <= 5; x++) helper.setBlock(new BlockPos(x, 1, 6), ModBlocks.ASTRAL_REDSTONE);
        helper.setBlock(new BlockPos(6, 1, 6), Blocks.REDSTONE_BLOCK);
        helper.setBlock(new BlockPos(5, 1, 5), ModBlocks.NIHILITH_LAMP);
        helper.runAfterDelay(90, () -> {
            helper.assertTrue(helper.getBlockState(path.get(14)).getValue(EndSignalBlock.POWER) == 1, "fifteenth segment not powered");
            helper.assertTrue(helper.getBlockState(path.get(15)).getValue(EndSignalBlock.POWER) == 0, "sixteenth segment powered");
            helper.assertTrue(helper.getBlockState(new BlockPos(5, 1, 5)).getValue(EndSignalBlock.POWER) == 0, "out-of-range lamp powered");
            for (int x = 1; x <= 5; x++) helper.assertTrue(helper.getBlockState(new BlockPos(x, 1, 6)).getValue(EndSignalBlock.POWER) == 0, "astralit took nihilith or vanilla power");
            BlockPos absolute = helper.absolutePos(path.get(0));
            helper.assertTrue(helper.getLevel().getBlockState(absolute).getSignal(helper.getLevel(), absolute, Direction.NORTH) == 0, "powder emits vanilla redstone");
            helper.setBlock(new BlockPos(0, 1, 1), ModBlocks.NIHILITH_SWITCH);
        });
        helper.runAfterDelay(180, () -> {
            for (var pos : path) helper.assertTrue(helper.getBlockState(pos).getValue(EndSignalBlock.POWER) == 0, "signal latched after source turned off");
            helper.succeed();
        });
    }

    public static void matchingLampsAndConfigLimits(GameTestHelper helper) {
        if (!active(helper)) return;
        var config = ServerTuning.get();
        int beforeRange = config.machines.endSignalRange;
        boolean before = config.features.endSignals;
        try {
            config.machines.endSignalRange = 999;
            helper.assertTrue(EndSignalBlock.range() == 15, "range cap bypassed");
            config.machines.endSignalRange = -1;
            helper.assertTrue(EndSignalBlock.range() == 1, "range floor bypassed");
            config.machines.endSignalRange = 3;
            for (int x = 0; x <= 5; x++) helper.setBlock(new BlockPos(x, 0, 1), Blocks.STONE);
            helper.setBlock(new BlockPos(0, 1, 1), ModBlocks.ASTRALIT_SWITCH.defaultBlockState().setValue(EndSignalBlock.ENABLED, true));
            for (int x = 1; x <= 4; x++) helper.setBlock(new BlockPos(x, 1, 1), ModBlocks.ASTRAL_REDSTONE);
            // Explicit scheduled tick calls avoid leaving a global config altered across asynchronous tests.
            for (int pass = 0; pass < 4; pass++) for (int x = 0; x <= 4; x++) {
                var pos = helper.absolutePos(new BlockPos(x, 1, 1));
                helper.getLevel().getBlockState(pos).tick(helper.getLevel(), pos, helper.getLevel().getRandom());
            }
            helper.assertTrue(helper.getBlockState(new BlockPos(3, 1, 1)).getValue(EndSignalBlock.POWER) == 1 && helper.getBlockState(new BlockPos(4, 1, 1)).getValue(EndSignalBlock.POWER) == 0, "lower server range ignored");
            helper.setBlock(new BlockPos(3, 1, 2), ModBlocks.ASTRALIT_LAMP);
            helper.setBlock(new BlockPos(2, 1, 2), ModBlocks.NIHILITH_LAMP);
            for (int x : new int[]{2, 3}) {
                var pos = helper.absolutePos(new BlockPos(x, 1, 2));
                helper.getLevel().getBlockState(pos).tick(helper.getLevel(), pos, helper.getLevel().getRandom());
            }
            helper.assertTrue(helper.getBlockState(new BlockPos(3, 1, 2)).getValue(EndSignalBlock.POWER) > 0 && helper.getBlockState(new BlockPos(2, 1, 2)).getValue(EndSignalBlock.POWER) == 0, "lamps ignore channel identity");
            config.features.endSignals = false;
            for (int x = 0; x <= 4; x++) {
                var pos = helper.absolutePos(new BlockPos(x, 1, 1));
                helper.getLevel().getBlockState(pos).tick(helper.getLevel(), pos, helper.getLevel().getRandom());
                helper.assertTrue(helper.getBlockState(new BlockPos(x, 1, 1)).getValue(EndSignalBlock.POWER) == 0, "disabled network still powered");
            }
        } finally { config.machines.endSignalRange = beforeRange; config.features.endSignals = before; }
        helper.succeed();
    }

    /**
     * Astral/Nihil Redstone lays out like vanilla dust (owner 2026-10-02): an untouched dot stays a dot,
     * two pieces form a straight line, powder on top of a block makes the lower one climb the wall
     * ("up") and the upper one reach down, the signal crosses that step losing one per piece, and
     * nothing connects to the other channel or to vanilla redstone dust.
     */
    public static void powderConnectsLikeRedstoneWire(GameTestHelper helper) {
        if (!active(helper)) return;
        for (int x = 0; x <= 6; x++) for (int z = 0; z <= 6; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        helper.setBlock(new BlockPos(1, 1, 5), ModBlocks.NIHIL_REDSTONE);
        helper.setBlock(new BlockPos(3, 1, 1), ModBlocks.NIHIL_REDSTONE);
        helper.setBlock(new BlockPos(4, 1, 1), ModBlocks.NIHIL_REDSTONE);
        helper.setBlock(new BlockPos(1, 1, 3), ModBlocks.ASTRAL_REDSTONE);
        helper.setBlock(new BlockPos(2, 1, 3), ModBlocks.NIHIL_REDSTONE);
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.REDSTONE_WIRE);
        helper.setBlock(new BlockPos(5, 1, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 1, 4), ModBlocks.NIHILITH_SWITCH.defaultBlockState().setValue(EndSignalBlock.ENABLED, true));
        helper.setBlock(new BlockPos(4, 1, 4), ModBlocks.NIHIL_REDSTONE);
        helper.setBlock(new BlockPos(5, 2, 4), ModBlocks.NIHIL_REDSTONE);
        helper.runAfterDelay(20, () -> {
            var dot = helper.getBlockState(new BlockPos(1, 1, 5));
            helper.assertTrue(sides(dot).equals("none none none none"), "lone powder is not a dot: " + sides(dot));
            helper.assertTrue(sides(helper.getBlockState(new BlockPos(3, 1, 1))).equals("none side none side"), "two pieces are no east-west line: " + sides(helper.getBlockState(new BlockPos(3, 1, 1))));
            helper.assertTrue(sides(helper.getBlockState(new BlockPos(2, 1, 3))).equals("none none none none"), "nihil powder connected to astral powder or vanilla dust: " + sides(helper.getBlockState(new BlockPos(2, 1, 3))));
            helper.assertTrue(helper.getBlockState(new BlockPos(1, 1, 3)).getValue(com.simplebuilding.blocks.custom.EndSignalPowderBlock.EAST)
                    == net.minecraft.world.level.block.state.properties.RedstoneSide.NONE, "astral powder connected to nihil powder");
            var lower = helper.getBlockState(new BlockPos(4, 1, 4));
            helper.assertTrue(sides(lower).equals("none up none side"), "powder does not climb the wall: " + sides(lower));
            var upper = helper.getBlockState(new BlockPos(5, 2, 4));
            helper.assertTrue(upper.getValue(com.simplebuilding.blocks.custom.EndSignalPowderBlock.WEST)
                    == net.minecraft.world.level.block.state.properties.RedstoneSide.SIDE, "upper powder does not reach down: " + sides(upper));
            int range = EndSignalBlock.range();
            helper.assertTrue(lower.getValue(EndSignalBlock.POWER) == range, "powder next to the switch has " + lower.getValue(EndSignalBlock.POWER));
            helper.assertTrue(upper.getValue(EndSignalBlock.POWER) == range - 1, "signal did not climb the step: " + upper.getValue(EndSignalBlock.POWER));
            helper.assertTrue(helper.getBlockState(new BlockPos(2, 1, 3)).getValue(EndSignalBlock.POWER) == 0, "nihil powder took vanilla or astral power");
            helper.succeed();
        });
    }

    // ----- Astral-/Nihil-Kolben (docs/ai/PLAN-ASTRAL-KOLBEN-2026-10-02.md, Abschnitt 9) -----

    private static final BlockPos PISTON = new BlockPos(3, 3, 3);

    private static int fire(GameTestHelper helper, BlockPos rel, boolean astral) {
        return com.simplebuilding.blocks.custom.EndPistonMoves.fire(helper.getLevel(), helper.absolutePos(rel), astral);
    }

    private static boolean isMoving(GameTestHelper helper, BlockPos rel) {
        return helper.getBlockState(rel).is(Blocks.MOVING_PISTON);
    }

    /** Stones and moving blocks carrying stone in the room: a move must neither add nor lose one. */
    private static int stones(GameTestHelper helper) {
        int count = 0;
        for (BlockPos rel : BlockPos.betweenClosed(0, 0, 0, 7, 7, 7)) {
            var state = helper.getBlockState(rel);
            if (state.is(Blocks.STONE)) count++;
            else if (state.is(Blocks.MOVING_PISTON)
                    && helper.getLevel().getBlockEntity(helper.absolutePos(rel)) instanceof net.minecraft.world.level.block.piston.PistonMovingBlockEntity moving
                    && moving.getMovedState().is(Blocks.STONE)) count++;
        }
        return count;
    }

    private static int droppedItems(GameTestHelper helper) {
        var corner = Vec3.atLowerCornerOf(helper.absolutePos(BlockPos.ZERO));
        return helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(corner.add(-2, -2, -2), corner.add(10, 10, 10))).size();
    }

    /** The Astral piston pushes all six neighbours one cell, in one call, and nothing further. */
    public static void pistonPushesAllSixAtOnce(GameTestHelper helper) {
        if (!active(helper)) return;
        helper.setBlock(PISTON, ModBlocks.ASTRAL_PISTON);
        for (Direction d : Direction.values()) helper.setBlock(PISTON.relative(d), Blocks.STONE);
        helper.assertTrue(fire(helper, PISTON, true) == 6, "astral piston did not move all six neighbours");
        for (Direction d : Direction.values()) {
            helper.assertTrue(isMoving(helper, PISTON.relative(d, 2)), "no moving block towards " + d);
            helper.assertTrue(helper.getBlockState(PISTON.relative(d)).isAir(), "source left behind towards " + d);
        }
        helper.runAfterDelay(8, () -> {
            for (Direction d : Direction.values()) {
                helper.assertTrue(helper.getBlockState(PISTON.relative(d, 2)).is(Blocks.STONE), "stone did not land towards " + d);
                helper.assertTrue(helper.getBlockState(PISTON.relative(d)).isAir(), "neighbour cell not empty towards " + d);
            }
            helper.assertTrue(helper.getBlockState(PISTON).is(ModBlocks.ASTRAL_PISTON), "the piston itself moved");
            helper.assertTrue(stones(helper) == 6 && droppedItems(helper) == 0, "stones were duplicated, lost or dropped");
            helper.succeed();
        });
    }

    /** A taken target stops that side: no chain, the block stays; a liquid source is never replaced; grass is. */
    public static void pistonNeverChains(GameTestHelper helper) {
        if (!active(helper)) return;
        helper.setBlock(PISTON, ModBlocks.ASTRAL_PISTON);
        helper.setBlock(new BlockPos(4, 3, 3), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 3, 3), Blocks.DIRT);
        helper.setBlock(new BlockPos(3, 4, 3), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 5, 3), Blocks.WATER);
        helper.setBlock(new BlockPos(1, 2, 3), Blocks.DIRT);
        helper.setBlock(new BlockPos(2, 3, 3), Blocks.STONE);
        helper.setBlock(new BlockPos(1, 3, 3), Blocks.SHORT_GRASS);
        helper.assertTrue(fire(helper, PISTON, true) == 1, "only the side towards the grass may move");
        helper.assertTrue(helper.getBlockState(new BlockPos(4, 3, 3)).is(Blocks.STONE)
                && helper.getBlockState(new BlockPos(5, 3, 3)).is(Blocks.DIRT)
                && helper.getBlockState(new BlockPos(6, 3, 3)).isAir(), "a row was pushed (chain)");
        helper.assertTrue(helper.getBlockState(new BlockPos(3, 4, 3)).is(Blocks.STONE)
                && helper.getBlockState(new BlockPos(3, 5, 3)).getFluidState().isSource(), "a water source was replaced");
        helper.assertTrue(isMoving(helper, new BlockPos(1, 3, 3)), "replaceable grass target was not taken");
        helper.succeed();
    }

    /** The Nihil piston pulls the block across a one-cell gap; a block already touching it stays. */
    public static void nihilPullsAcrossTheGap(GameTestHelper helper) {
        if (!active(helper)) return;
        helper.setBlock(PISTON, ModBlocks.NIHIL_PISTON);
        for (Direction d : Direction.values()) {
            if (d != Direction.EAST) helper.setBlock(PISTON.relative(d, 2), Blocks.STONE);
        }
        helper.setBlock(new BlockPos(4, 3, 3), Blocks.DIRT);
        helper.setBlock(new BlockPos(5, 3, 3), Blocks.STONE);
        helper.assertTrue(fire(helper, PISTON, false) == 5, "nihil piston did not pull the five gapped blocks");
        helper.runAfterDelay(8, () -> {
            for (Direction d : Direction.values()) {
                if (d == Direction.EAST) continue;
                helper.assertTrue(helper.getBlockState(PISTON.relative(d)).is(Blocks.STONE), "stone not pulled from " + d);
                helper.assertTrue(helper.getBlockState(PISTON.relative(d, 2)).isAir(), "stone left at distance two " + d);
            }
            helper.assertTrue(helper.getBlockState(new BlockPos(4, 3, 3)).is(Blocks.DIRT)
                    && helper.getBlockState(new BlockPos(5, 3, 3)).is(Blocks.STONE), "touching block moved or was passed");
            helper.assertTrue(stones(helper) == 6 && droppedItems(helper) == 0, "stones were duplicated, lost or dropped");
            helper.succeed();
        });
    }

    /**
     * Obsidian, bedrock, a chest (block entity), a door (two parts, DESTROY), a torch (DESTROY) and a
     * vanilla piston stay where they are, nothing is broken; glazed terracotta (PUSH_ONLY) is pushed by
     * the Astral piston but never pulled by the Nihil piston.
     */
    public static void pistonLeavesImmovablesAlone(GameTestHelper helper) {
        if (!active(helper)) return;
        helper.setBlock(PISTON, ModBlocks.ASTRAL_PISTON);
        helper.setBlock(new BlockPos(4, 3, 3), Blocks.OBSIDIAN);
        helper.setBlock(new BlockPos(2, 3, 3), Blocks.CHEST);
        helper.setBlock(new BlockPos(3, 4, 3), Blocks.BEDROCK);
        helper.setBlock(new BlockPos(3, 2, 3), Blocks.PISTON);
        helper.setBlock(new BlockPos(3, 3, 4), Blocks.OAK_DOOR.defaultBlockState());
        helper.setBlock(new BlockPos(3, 4, 4), Blocks.OAK_DOOR.defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF,
                        net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        helper.setBlock(new BlockPos(3, 2, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 3, 2), Blocks.TORCH);
        helper.assertTrue(fire(helper, PISTON, true) == 0, "an immovable block was moved");
        helper.assertTrue(helper.getBlockState(new BlockPos(4, 3, 3)).is(Blocks.OBSIDIAN)
                && helper.getBlockState(new BlockPos(2, 3, 3)).is(Blocks.CHEST)
                && helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(2, 3, 3))) != null
                && helper.getBlockState(new BlockPos(3, 4, 3)).is(Blocks.BEDROCK)
                && helper.getBlockState(new BlockPos(3, 2, 3)).is(Blocks.PISTON)
                && helper.getBlockState(new BlockPos(3, 3, 4)).is(Blocks.OAK_DOOR)
                && helper.getBlockState(new BlockPos(3, 4, 4)).is(Blocks.OAK_DOOR)
                && helper.getBlockState(new BlockPos(3, 3, 2)).is(Blocks.TORCH), "an immovable block changed");
        helper.assertTrue(droppedItems(helper) == 0, "an immovable block was broken");
        // PUSH_ONLY: Astral pushes it away, Nihil does not pull it.
        helper.setBlock(new BlockPos(4, 3, 3), Blocks.GLAZED_TERRACOTTA.white());
        helper.assertTrue(fire(helper, PISTON, true) == 1 && isMoving(helper, new BlockPos(5, 3, 3)), "astral did not push glazed terracotta");
        BlockPos nihil = new BlockPos(3, 3, 6);
        helper.setBlock(nihil, ModBlocks.NIHIL_PISTON);
        helper.setBlock(new BlockPos(1, 3, 6), Blocks.GLAZED_TERRACOTTA.white());
        helper.assertTrue(fire(helper, nihil, false) == 0, "nihil pulled glazed terracotta");
        helper.succeed();
    }

    /** Two pistons on one block, or on one target, in the same tick: exactly one move, nothing duplicated. */
    public static void twoPistonsMoveOneBlockOnce(GameTestHelper helper) {
        if (!active(helper)) return;
        // Same block: A pushes it east, B pushes it south.
        helper.setBlock(new BlockPos(1, 2, 2), ModBlocks.ASTRAL_PISTON);
        helper.setBlock(new BlockPos(2, 2, 1), ModBlocks.ASTRAL_PISTON);
        helper.setBlock(new BlockPos(2, 2, 2), Blocks.STONE);
        int first = fire(helper, new BlockPos(1, 2, 2), true) + fire(helper, new BlockPos(2, 2, 1), true);
        helper.assertTrue(first == 1, "one block moved " + first + " times in one tick");
        // Same target: A and B push towards (4, 5, 4) from both sides.
        helper.setBlock(new BlockPos(2, 5, 4), ModBlocks.ASTRAL_PISTON);
        helper.setBlock(new BlockPos(3, 5, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(6, 5, 4), ModBlocks.ASTRAL_PISTON);
        helper.setBlock(new BlockPos(5, 5, 4), Blocks.STONE);
        int second = fire(helper, new BlockPos(2, 5, 4), true) + fire(helper, new BlockPos(6, 5, 4), true);
        helper.assertTrue(second == 1, "two blocks moved into one cell: " + second);
        helper.assertTrue(stones(helper) == 3, "stones were duplicated or lost: " + stones(helper));
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(stones(helper) == 3 && droppedItems(helper) == 0, "stones were duplicated, lost or dropped after landing");
            helper.succeed();
        });
    }

    /** Rising edge only: a held signal fires once; the cooldown is capped 4..100; switched off, nothing moves. */
    public static void pistonFiresOnRisingEdgeOnly(GameTestHelper helper) {
        if (!active(helper)) return;
        var config = ServerTuning.get();
        int beforeCooldown = config.machines.endPistonCooldownTicks;
        boolean before = config.features.endPistons;
        try {
            config.machines.endPistonCooldownTicks = 1;
            helper.assertTrue(com.simplebuilding.blocks.custom.EndPistonBlock.cooldown() == 4, "cooldown floor bypassed");
            config.machines.endPistonCooldownTicks = 999;
            helper.assertTrue(com.simplebuilding.blocks.custom.EndPistonBlock.cooldown() == 100, "cooldown cap bypassed");
            config.machines.endPistonCooldownTicks = 8;
            for (int x = 0; x <= 7; x++) for (int z = 0; z <= 7; z++) helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            helper.setBlock(new BlockPos(3, 0, 3), Blocks.STONE);
            BlockPos piston = new BlockPos(3, 2, 3), sw = new BlockPos(2, 2, 3);
            helper.setBlock(piston, ModBlocks.ASTRAL_PISTON);
            helper.setBlock(new BlockPos(4, 2, 3), Blocks.STONE);
            Runnable tick = () -> {
                var pos = helper.absolutePos(piston);
                helper.getLevel().getBlockState(pos).tick(helper.getLevel(), pos, helper.getLevel().getRandom());
            };
            helper.setBlock(sw, ModBlocks.ASTRALIT_SWITCH.defaultBlockState().setValue(EndSignalBlock.ENABLED, true));
            tick.run();
            helper.assertTrue(helper.getBlockState(piston).getValue(EndSignalBlock.POWER) > 0, "piston does not receive its switch");
            helper.assertTrue(isMoving(helper, new BlockPos(5, 2, 3)), "rising edge did not fire");
            helper.setBlock(new BlockPos(3, 3, 3), Blocks.STONE);
            tick.run();
            helper.assertTrue(helper.getBlockState(new BlockPos(3, 3, 3)).is(Blocks.STONE), "held signal fired again");
            helper.setBlock(sw, ModBlocks.ASTRALIT_SWITCH);
            tick.run();
            helper.assertTrue(helper.getBlockState(piston).getValue(EndSignalBlock.POWER) == 0, "signal latched after switch off");
            helper.setBlock(sw, ModBlocks.ASTRALIT_SWITCH.defaultBlockState().setValue(EndSignalBlock.ENABLED, true));
            tick.run();
            helper.assertTrue(isMoving(helper, new BlockPos(3, 4, 3)), "second rising edge did not fire");
            config.features.endPistons = false;
            helper.setBlock(sw, ModBlocks.ASTRALIT_SWITCH);
            tick.run();
            helper.setBlock(new BlockPos(3, 2, 4), Blocks.STONE);
            helper.setBlock(sw, ModBlocks.ASTRALIT_SWITCH.defaultBlockState().setValue(EndSignalBlock.ENABLED, true));
            tick.run();
            helper.assertTrue(helper.getBlockState(piston).getValue(EndSignalBlock.POWER) == 0
                    && helper.getBlockState(new BlockPos(3, 2, 4)).is(Blocks.STONE), "disabled piston moved or took power");
        } finally { config.machines.endPistonCooldownTicks = beforeCooldown; config.features.endPistons = before; }
        helper.succeed();
    }

    /** Vanilla redstone and the other channel never trigger; the channel's powder visibly connects. */
    public static void pistonIgnoresVanillaAndOtherChannel(GameTestHelper helper) {
        if (!active(helper)) return;
        for (int x = 0; x <= 7; x++) for (int z = 0; z <= 7; z++) helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        BlockPos astral = new BlockPos(2, 2, 2), nihil = new BlockPos(5, 2, 5);
        helper.setBlock(astral, ModBlocks.ASTRAL_PISTON);
        helper.setBlock(astral.west(), Blocks.REDSTONE_BLOCK);
        helper.setBlock(astral.east(), ModBlocks.NIHILITH_SWITCH.defaultBlockState().setValue(EndSignalBlock.ENABLED, true));
        helper.setBlock(astral.above(), Blocks.STONE);
        helper.setBlock(nihil, ModBlocks.NIHIL_PISTON);
        helper.setBlock(nihil.west(), ModBlocks.ASTRALIT_SWITCH.defaultBlockState().setValue(EndSignalBlock.ENABLED, true));
        helper.setBlock(nihil.above(2), Blocks.STONE);
        helper.setBlock(astral.north(), ModBlocks.ASTRAL_REDSTONE);
        for (BlockPos rel : List.of(astral, nihil, astral.north())) {
            var pos = helper.absolutePos(rel);
            helper.getLevel().getBlockState(pos).tick(helper.getLevel(), pos, helper.getLevel().getRandom());
        }
        helper.assertTrue(helper.getBlockState(astral).getValue(EndSignalBlock.POWER) == 0
                && helper.getBlockState(astral.above()).is(Blocks.STONE), "astral piston took vanilla or nihil power");
        helper.assertTrue(helper.getBlockState(nihil).getValue(EndSignalBlock.POWER) == 0
                && helper.getBlockState(nihil.above(2)).is(Blocks.STONE), "nihil piston took astral power");
        helper.assertTrue(helper.getBlockState(astral.north()).getValue(com.simplebuilding.blocks.custom.EndSignalPowderBlock.SOUTH)
                .isConnected(), "astral powder does not connect to the astral piston");
        helper.assertTrue(helper.getBlockState(astral.north()).getValue(EndSignalBlock.POWER) == 0, "piston passed a signal on");
        helper.succeed();
    }

    private static String sides(net.minecraft.world.level.block.state.BlockState state) {
        return state.getValue(com.simplebuilding.blocks.custom.EndSignalPowderBlock.NORTH).getSerializedName() + " "
                + state.getValue(com.simplebuilding.blocks.custom.EndSignalPowderBlock.EAST).getSerializedName() + " "
                + state.getValue(com.simplebuilding.blocks.custom.EndSignalPowderBlock.SOUTH).getSerializedName() + " "
                + state.getValue(com.simplebuilding.blocks.custom.EndSignalPowderBlock.WEST).getSerializedName();
    }

    // --- Nihil-Gewoelbe (2026-10-04): weltweit geteilter Inhalt. Alle Tests teilen den Weltspeicher und laufen
    // parallel; jeder nutzt deshalb nur seinen eigenen Slot und raeumt ihn danach wieder.

    private static ServerPlayer openNihil(GameTestHelper helper, BlockPos pos) {
        var absolute = helper.absolutePos(pos);
        var player = helper.makeMockServerPlayerInLevel();
        player.setPos(Vec3.atCenterOf(absolute));
        var hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        helper.getLevel().getBlockState(absolute).useWithoutItem(helper.getLevel(), player, hit);
        return player;
    }

    private static net.minecraft.world.SimpleContainer nihilShared(GameTestHelper helper) {
        return com.simplebuilding.util.NihilVaultStorage.get(helper.getLevel().getServer()).items();
    }

    /** Two vaults, two players: both menus show the one shared container; the lid counts each opener. */
    public static void nihilVaultSharesBetweenVaultsAndPlayers(GameTestHelper helper) {
        if (!active(helper)) return;
        int slot = 1;
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.NIHIL_VAULT);
        helper.setBlock(new BlockPos(4, 1, 1), ModBlocks.NIHIL_VAULT);
        var world = helper.getLevel();
        helper.assertTrue(BlockEntityTypes.ENDER_CHEST.isValid(helper.getBlockState(new BlockPos(1, 1, 1))), "nihil vault does not fit ender chest block entity");
        var first = openNihil(helper, new BlockPos(1, 1, 1));
        var second = openNihil(helper, new BlockPos(4, 1, 1));
        try {
            helper.assertTrue(first.containerMenu instanceof ChestMenu a && a.getRowCount() == 6 && a.slots.size() == 90, "first vault did not open a six-row menu (owner N16)");
            helper.assertTrue(second.containerMenu instanceof ChestMenu b && b.getRowCount() == 6, "second vault did not open a six-row menu");
            var chestA = (EnderChestBlockEntity) world.getBlockEntity(helper.absolutePos(new BlockPos(1, 1, 1)));
            helper.assertTrue(first.getEnderChestInventory().isActiveChest(chestA), "vanilla lid opener is not bound");
            first.containerMenu.getSlot(slot).set(new ItemStack(Items.DIAMOND, 7));
            helper.assertTrue(second.containerMenu.getSlot(slot).getItem().is(Items.DIAMOND)
                    && second.containerMenu.getSlot(slot).getItem().getCount() == 7, "second player at another vault does not see the item");
            helper.assertTrue(nihilShared(helper).getItem(slot).getCount() == 7, "item did not reach world storage");
            helper.assertTrue(first.getEnderChestInventory().getItem(slot).isEmpty(), "nihil vault wrote into the personal ender inventory");
            first.closeContainer();
            helper.assertTrue(!first.getEnderChestInventory().isActiveChest(chestA), "lid binding survived closing");
            second.closeContainer();
        } finally { nihilShared(helper).setItem(slot, ItemStack.EMPTY); }
        helper.succeed();
    }

    /** Two open menus on the same stack: only the first shift-click gets it, nothing is duplicated. */
    public static void nihilVaultNoDupeWithConcurrentMenus(GameTestHelper helper) {
        if (!active(helper)) return;
        int slot = 2;
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.NIHIL_VAULT);
        helper.setBlock(new BlockPos(4, 1, 1), ModBlocks.NIHIL_VAULT);
        var first = openNihil(helper, new BlockPos(1, 1, 1));
        var second = openNihil(helper, new BlockPos(4, 1, 1));
        try {
            nihilShared(helper).setItem(slot, new ItemStack(Items.EMERALD, 10));
            first.containerMenu.broadcastChanges();
            second.containerMenu.broadcastChanges();
            first.containerMenu.quickMoveStack(first, slot);
            second.containerMenu.quickMoveStack(second, slot);
            second.containerMenu.clicked(slot, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, second);
            int total = first.getInventory().countItem(Items.EMERALD) + second.getInventory().countItem(Items.EMERALD)
                    + nihilShared(helper).countItem(Items.EMERALD) + second.containerMenu.getCarried().getCount();
            helper.assertTrue(first.getInventory().countItem(Items.EMERALD) == 10, "first player did not get the stack");
            helper.assertTrue(total == 10, "emeralds duplicated or lost: " + total);
            first.closeContainer();
            second.closeContainer();
        } finally { nihilShared(helper).setItem(slot, ItemStack.EMPTY); }
        helper.succeed();
    }

    /** World storage round-trips through its codec; breaking a vault drops the vault and keeps the contents. */
    public static void nihilVaultPersistsAndBreakKeepsContents(GameTestHelper helper) {
        if (!active(helper)) return;
        int slot = 3;
        var server = helper.getLevel().getServer();
        var storage = com.simplebuilding.util.NihilVaultStorage.get(server);
        try {
            storage.setDirty(false);
            storage.items().setItem(slot, new ItemStack(Items.GOLD_INGOT, 5));
            helper.assertTrue(storage.isDirty(), "change does not mark the world storage dirty");
            var ops = helper.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
            var tag = com.simplebuilding.util.NihilVaultStorage.CODEC.encodeStart(ops, storage).getOrThrow();
            var loaded = com.simplebuilding.util.NihilVaultStorage.CODEC.parse(ops, tag).getOrThrow();
            helper.assertTrue(loaded.items().getItem(slot).is(Items.GOLD_INGOT) && loaded.items().getItem(slot).getCount() == 5, "contents lost after save/load");
            helper.assertTrue(server.overworld().getDataStorage().computeIfAbsent(com.simplebuilding.util.NihilVaultStorage.TYPE) == storage, "storage is not one per world");

            var pos = new BlockPos(1, 1, 1);
            helper.setBlock(pos, ModBlocks.NIHIL_VAULT);
            helper.getLevel().destroyBlock(helper.absolutePos(pos), true);
            helper.assertBlockNotPresent(ModBlocks.NIHIL_VAULT, pos);
            helper.assertItemEntityPresent(com.simplebuilding.items.ModItems.NIHIL_VAULT, pos, 2.0);
            helper.assertTrue(storage.items().getItem(slot).getCount() == 5, "breaking the vault touched the shared contents");
            helper.setBlock(new BlockPos(4, 1, 1), ModBlocks.NIHIL_VAULT);
            var player = openNihil(helper, new BlockPos(4, 1, 1));
            helper.assertTrue(player.containerMenu.getSlot(slot).getItem().is(Items.GOLD_INGOT), "a new vault does not show the kept contents");
            player.closeContainer();
        } finally { storage.items().setItem(slot, ItemStack.EMPTY); }
        helper.succeed();
    }

    /** Switch off: no opening, open menus become invalid, contents stay; a solid block above blocks the lid. */
    public static void nihilVaultConfigAndBlockedLid(GameTestHelper helper) {
        if (!active(helper)) return;
        int slot = 4;
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.NIHIL_VAULT);
        boolean before = ServerTuning.get().features.nihilVault;
        try {
            nihilShared(helper).setItem(slot, new ItemStack(Items.IRON_INGOT));
            var open = openNihil(helper, pos);
            helper.assertTrue(open.containerMenu instanceof ChestMenu, "vault did not open");
            ServerTuning.get().features.nihilVault = false;
            helper.assertTrue(!open.containerMenu.stillValid(open), "disabled vault keeps open menus valid");
            open.closeContainer();
            var player = openNihil(helper, pos);
            helper.assertTrue(player.containerMenu == player.inventoryMenu, "disabled vault opened");
            helper.assertTrue(nihilShared(helper).getItem(slot).is(Items.IRON_INGOT), "disabled vault erased contents");
            ServerTuning.get().features.nihilVault = true;
            helper.setBlock(pos.above(), Blocks.STONE);
            player = openNihil(helper, pos);
            helper.assertTrue(player.containerMenu == player.inventoryMenu, "blocked lid opened");
        } finally {
            ServerTuning.get().features.nihilVault = before;
            nihilShared(helper).setItem(slot, ItemStack.EMPTY);
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------
    // Astral-/Nihil-Schienen (2026-10-04, docs/ai/PLAN-ASTRAL-NIHIL-SCHIENEN-2026-10-02.md)
    // ------------------------------------------------------------------------------------------------

    private static boolean railsActive(GameTestHelper helper) {
        if (com.simplebuilding.version.McVersion.END_RAILS) return true;
        helper.succeed();
        return false;
    }

    private static net.minecraft.world.level.block.state.BlockState eastWest(net.minecraft.world.level.block.Block rail) {
        return rail.defaultBlockState().setValue(EndRailBlock.SHAPE, RailShape.EAST_WEST);
    }

    private static boolean powered(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).getValue(EndRailBlock.POWERED);
    }

    /**
     * The boost formula: speed rises every tick, never passes the cap (stays strictly below it until the gap is below
     * double precision) and gets within 2 % of it; every tick adds
     * less the faster the cart already is ("the faster, the more boost you need"); a stronger boost is faster after
     * the same number of ticks; momentum above the cap is cut to the cap.
     */
    public static void astralBoostApproachesTopSpeedWithoutPassingIt(GameTestHelper helper) {
        if (!railsActive(helper)) return;
        double[] caps = {0.4, 0.8, 1.0, 1.0 / 0.75};
        double[] boosts = {EndRailPhysics.MIN_BOOST, 0.12, EndRailPhysics.MAX_BOOST};
        for (double cap : caps) {
            double previousAfterTen = -1;
            for (double boost : boosts) {
                double v = 0, gain = Double.MAX_VALUE, afterTen = 0;
                for (int tick = 1; tick <= 2000; tick++) {
                    double next = EndRailPhysics.boosted(v, cap, boost);
                    // Never above the cap; strictly below it until the remaining gap is beyond double precision.
                    helper.assertTrue(next <= cap && (next < cap || cap - v < 1e-9),
                            "speed reached or passed the cap " + cap + " (boost " + boost + ", tick " + tick + ")");
                    helper.assertTrue(next > v || v > cap * 0.999, "speed did not rise at " + v + " (cap " + cap + ")");
                    helper.assertTrue(next - v <= gain + 1e-12, "gain grew with speed at " + v + " (cap " + cap + ")");
                    gain = next - v;
                    v = next;
                    if (tick == 10) afterTen = v;
                }
                helper.assertTrue(v > cap * 0.98, "speed stalled at " + v + " below the cap " + cap);
                helper.assertTrue(afterTen >= previousAfterTen, "a stronger boost was not faster (cap " + cap + ", boost " + boost + ")");
                previousAfterTen = afterTen;
            }
        }
        helper.assertTrue(EndRailPhysics.boosted(2.0, 0.8, 0.12) == 0.8, "momentum above the cap was not cut");
        helper.succeed();
    }

    /** Astral pushes harder than vanilla's powered rail (0.06 per tick) at every vanilla speed, even at the lowest config. */
    public static void astralBoostsMoreThanAPoweredRail(GameTestHelper helper) {
        if (!railsActive(helper)) return;
        double cap = EndRailPhysics.maxSpeedPerTick();
        double boost = EndRailPhysics.boost();
        for (double v = 0; v <= EndRailPhysics.VANILLA_MAX_SPEED + 1e-9; v += 0.05) {
            helper.assertTrue(EndRailPhysics.boosted(v, cap, boost) - v > EndRailPhysics.VANILLA_POWERED_RAIL_BOOST,
                    "default Astral boost at " + v + " is not above the powered rail");
        }
        double minCap = EndRailPhysics.MIN_MAX_SPEED / 20.0;
        helper.assertTrue(EndRailPhysics.boosted(0, minCap, EndRailPhysics.MIN_BOOST) > EndRailPhysics.VANILLA_POWERED_RAIL_BOOST,
                "lowest config Astral boost from standstill is not above the powered rail");
        helper.assertTrue(cap > EndRailPhysics.VANILLA_MAX_SPEED, "default top speed is not above vanilla");
        helper.succeed();
    }

    /** Powered Nihil: the speed only falls, never below zero, not in one tick, and 16 blocks/s stop within 10 ticks. */
    public static void nihilBrakeStopsSmoothly(GameTestHelper helper) {
        if (!railsActive(helper)) return;
        double v = 0.8;
        int ticks = 0;
        while (v > 0 && ticks < 50) {
            double next = EndRailPhysics.braked(v, EndRailPhysics.brake());
            helper.assertTrue(next < v && next >= 0, "Nihil brake did not slow down at " + v);
            helper.assertTrue(ticks > 0 || next > 0, "Nihil brake stopped a full-speed cart in one tick (not smooth)");
            v = next;
            ticks++;
        }
        helper.assertTrue(v == 0 && ticks <= 10, "Nihil brake needed " + ticks + " ticks");
        helper.assertTrue(EndRailPhysics.braked(0.02, EndRailPhysics.MIN_BRAKE) == 0, "slow cart not held");
        helper.succeed();
    }

    /** Hand-edited or commanded values are clamped at the runtime access and by validate(). */
    public static void endRailConfigIsClamped(GameTestHelper helper) {
        if (!railsActive(helper)) return;
        var machines = ServerTuning.get().machines;
        int speed = machines.astralRailMaxSpeed;
        double boost = machines.astralRailBoost, brake = machines.nihilRailBrake;
        try {
            machines.astralRailMaxSpeed = 999;
            machines.astralRailBoost = 50;
            machines.nihilRailBrake = 50;
            helper.assertTrue(EndRailPhysics.maxSpeedPerTick() == 1.0, "top speed cap bypassed");
            helper.assertTrue(EndRailPhysics.boost() == EndRailPhysics.MAX_BOOST, "boost cap bypassed");
            helper.assertTrue(EndRailPhysics.brake() == EndRailPhysics.MAX_BRAKE, "brake cap bypassed");
            machines.astralRailMaxSpeed = -1;
            machines.astralRailBoost = -1;
            machines.nihilRailBrake = Double.NaN;
            helper.assertTrue(EndRailPhysics.maxSpeedPerTick() == 0.4, "top speed floor bypassed");
            helper.assertTrue(EndRailPhysics.boost() == EndRailPhysics.MIN_BOOST, "boost floor bypassed");
            helper.assertTrue(EndRailPhysics.brake() == 0.08, "NaN brake not replaced");
            var copy = new com.simplebuilding.config.ServerTuningConfig();
            copy.machines.astralRailMaxSpeed = 999;
            copy.machines.astralRailBoost = 9;
            copy.machines.nihilRailBrake = -9;
            copy.validate();
            helper.assertTrue(copy.machines.astralRailMaxSpeed == 20 && copy.machines.astralRailBoost == 0.25
                    && copy.machines.nihilRailBrake == 0.02, "validate() kept out-of-range rail values");
        } finally {
            machines.astralRailMaxSpeed = speed;
            machines.astralRailBoost = boost;
            machines.nihilRailBrake = brake;
        }
        helper.succeed();
    }

    /** Only a powder or switch of the rail's own channel powers it; vanilla redstone and the other channel never do. */
    public static void endRailIsFedOnlyByItsOwnChannel(GameTestHelper helper) {
        if (!railsActive(helper)) return;
        for (int x = 0; x <= 7; x++) for (int z = 0; z <= 6; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        BlockPos astral = new BlockPos(1, 1, 1), nihil = new BlockPos(4, 1, 1), vanilla = new BlockPos(1, 1, 4), viaPowder = new BlockPos(5, 1, 4);
        helper.setBlock(astral, eastWest(ModBlocks.ASTRAL_RAIL));
        helper.setBlock(astral.north(), ModBlocks.ASTRALIT_SWITCH.defaultBlockState().setValue(EndSignalBlock.ENABLED, true));
        helper.setBlock(nihil, eastWest(ModBlocks.NIHIL_RAIL));
        helper.setBlock(nihil.north(), ModBlocks.ASTRALIT_SWITCH.defaultBlockState().setValue(EndSignalBlock.ENABLED, true));
        helper.setBlock(vanilla, eastWest(ModBlocks.ASTRAL_RAIL));
        helper.setBlock(vanilla.north(), Blocks.REDSTONE_BLOCK);
        helper.setBlock(viaPowder, eastWest(ModBlocks.NIHIL_RAIL));
        helper.setBlock(new BlockPos(5, 1, 5), ModBlocks.NIHIL_REDSTONE);
        helper.setBlock(new BlockPos(6, 1, 5), ModBlocks.NIHILITH_SWITCH.defaultBlockState().setValue(EndSignalBlock.ENABLED, true));
        // One sequence: scheduling new delays from inside a delayed callback breaks the test ticker.
        helper.startSequence().thenIdle(20).thenExecute(() -> {
            helper.assertTrue(powered(helper, astral), "astral switch did not power the astral rail");
            helper.assertFalse(powered(helper, nihil), "astral switch powered a nihil rail");
            helper.assertFalse(powered(helper, vanilla), "vanilla redstone powered an astral rail");
            helper.assertTrue(powered(helper, viaPowder), "nihil powder did not power the nihil rail");
            helper.setBlock(astral.north(), Blocks.AIR);
        }).thenIdle(6).thenExecute(() ->
                helper.assertFalse(powered(helper, astral), "astral rail stayed powered without its switch")).thenSucceed();
    }

    /**
     * In the real game (default minecart behaviour): a cart standing on powered Astral rails next to a stone is pushed
     * off and moves faster than vanilla's 0.4 blocks per tick within seven rails - the raised top speed and the boost
     * reach the cart through both mixins - without ever exceeding the configured top speed.
     */
    public static void astralRailLaunchesACartPastVanillaSpeed(GameTestHelper helper) {
        if (!railsActive(helper)) return;
        for (int x = 0; x <= 7; x++) for (int z = 1; z <= 2; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        helper.setBlock(new BlockPos(0, 1, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(0, 1, 2), ModBlocks.ASTRALIT_SWITCH.defaultBlockState().setValue(EndSignalBlock.ENABLED, true));
        for (int x = 1; x <= 7; x++) {
            helper.setBlock(new BlockPos(x, 1, 1), eastWest(ModBlocks.ASTRAL_RAIL));
            helper.setBlock(new BlockPos(x, 1, 2), ModBlocks.ASTRAL_REDSTONE);
        }
        double cap = EndRailPhysics.maxSpeedPerTick();
        net.minecraft.world.entity.vehicle.minecart.AbstractMinecart[] cart = {null};
        double[] startX = {0}, last = {0}, fastest = {0};
        helper.onEachTick(() -> {
            if (cart[0] == null || cart[0].isRemoved()) return;
            fastest[0] = Math.max(fastest[0], Math.abs(cart[0].getX() - last[0]));
            last[0] = cart[0].getX();
        });
        helper.startSequence().thenIdle(24).thenExecute(() -> {
            for (int x = 1; x <= 7; x++) helper.assertTrue(powered(helper, new BlockPos(x, 1, 1)), "rail " + x + " not powered");
            cart[0] = helper.spawn(net.minecraft.world.entity.EntityTypes.MINECART, new BlockPos(1, 1, 1));
            startX[0] = last[0] = cart[0].getX();
        }).thenWaitUntil(() -> {
            helper.assertTrue(fastest[0] <= cap + 1e-6, "cart moved " + fastest[0] + " in a tick, above the top speed " + cap);
            helper.assertTrue(cart[0].getX() - startX[0] > 4 && fastest[0] > EndRailPhysics.VANILLA_MAX_SPEED + 0.05,
                    "cart not past vanilla speed yet: fastest step " + fastest[0] + ", travelled " + (cart[0].getX() - startX[0]));
        }).thenExecute(() -> cart[0].discard()).thenSucceed();
    }

    /**
     * Powered Nihil rails stop a fast cart and hold it; unpowered ones let it roll through. A cart sent at the Astral
     * top speed onto a plain curve stays on the rails (the raised limit ends with the Astral rail).
     */
    public static void nihilRailStopsAndFastCartTakesACurve(GameTestHelper helper) {
        if (!railsActive(helper)) return;
        for (int x = 0; x <= 7; x++) for (int z = 0; z <= 7; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        java.util.Map<BlockPos, net.minecraft.world.level.block.state.BlockState> rails = new java.util.LinkedHashMap<>();
        // Lane z=1: powered Nihil rails x=1..6 (powder north of them, switch at x=0). Lane z=3: unpowered Nihil rails.
        helper.setBlock(new BlockPos(0, 1, 0), ModBlocks.NIHILITH_SWITCH.defaultBlockState().setValue(EndSignalBlock.ENABLED, true));
        for (int x = 1; x <= 6; x++) {
            rails.put(new BlockPos(x, 1, 1), eastWest(ModBlocks.NIHIL_RAIL));
            helper.setBlock(new BlockPos(x, 1, 0), ModBlocks.NIHIL_REDSTONE);
            rails.put(new BlockPos(x, 1, 3), eastWest(ModBlocks.NIHIL_RAIL));
        }
        // Lane z=5: Astral rails x=1..4, a plain curve at x=5 turning south onto powered Nihil rails z=6..7 (the stop).
        for (int x = 1; x <= 4; x++) rails.put(new BlockPos(x, 1, 5), eastWest(ModBlocks.ASTRAL_RAIL));
        rails.put(new BlockPos(5, 1, 5), Blocks.RAIL.defaultBlockState().setValue(net.minecraft.world.level.block.RailBlock.SHAPE, RailShape.SOUTH_WEST));
        for (int z = 6; z <= 7; z++) {
            rails.put(new BlockPos(5, 1, z), ModBlocks.NIHIL_RAIL.defaultBlockState().setValue(EndRailBlock.SHAPE, RailShape.NORTH_SOUTH));
            helper.setBlock(new BlockPos(6, 1, z), ModBlocks.NIHIL_REDSTONE);
        }
        helper.setBlock(new BlockPos(7, 1, 6), ModBlocks.NIHILITH_SWITCH.defaultBlockState().setValue(EndSignalBlock.ENABLED, true));
        // Placing a rail lets it reconnect to its neighbours; setting the same block again keeps the given shape.
        for (int pass = 0; pass < 2; pass++) rails.forEach(helper::setBlock);
        double cap = EndRailPhysics.maxSpeedPerTick();
        net.minecraft.world.entity.vehicle.minecart.AbstractMinecart[] carts = new net.minecraft.world.entity.vehicle.minecart.AbstractMinecart[3];
        double[] starts = new double[2];
        helper.startSequence().thenIdle(20).thenExecute(() -> {
            helper.assertTrue(powered(helper, new BlockPos(6, 1, 1)) && !powered(helper, new BlockPos(1, 1, 3))
                    && powered(helper, new BlockPos(5, 1, 7)), "nihil lanes not set up");
            var stopped = carts[0] = helper.spawn(net.minecraft.world.entity.EntityTypes.MINECART, new BlockPos(1, 1, 1));
            var rolling = carts[1] = helper.spawn(net.minecraft.world.entity.EntityTypes.MINECART, new BlockPos(1, 1, 3));
            var curving = carts[2] = helper.spawn(net.minecraft.world.entity.EntityTypes.MINECART, new BlockPos(1, 1, 5));
            stopped.setDeltaMovement(cap, 0, 0);
            rolling.setDeltaMovement(0.3, 0, 0);
            curving.setDeltaMovement(cap, 0, 0);
            starts[0] = stopped.getX();
            starts[1] = rolling.getX();
        }).thenIdle(30).thenExecute(() -> {
                var stopped = carts[0];
                var rolling = carts[1];
                var curving = carts[2];
                double stoppedX = starts[0], rollingX = starts[1];
                helper.assertTrue(stopped.getDeltaMovement().horizontalDistance() == 0, "powered Nihil rail did not stop the cart: " + stopped.getDeltaMovement());
                helper.assertTrue(stopped.getX() - stoppedX < 4, "cart braked too late: " + (stopped.getX() - stoppedX));
                helper.assertTrue(rolling.getX() - rollingX > 2, "unpowered Nihil rail braked the cart");
                BlockPos curve = helper.absolutePos(new BlockPos(5, 1, 5));
                helper.assertTrue(net.minecraft.world.level.block.BaseRailBlock.isRail(helper.getLevel().getBlockState(curving.getCurrentBlockPosOrRailBelow()))
                        && curving.getZ() > curve.getZ() + 1 && Math.abs(curving.getX() - (curve.getX() + 0.5)) < 0.1
                        && curving.getDeltaMovement().horizontalDistance() == 0,
                        "fast cart did not follow the curve onto the stop: at " + curving.position() + " moving " + curving.getDeltaMovement());
                stopped.discard();
                rolling.discard();
                curving.discard();
        }).thenSucceed();
    }
}

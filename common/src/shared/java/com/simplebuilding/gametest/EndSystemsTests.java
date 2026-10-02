package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
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
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

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

    private static String sides(net.minecraft.world.level.block.state.BlockState state) {
        return state.getValue(com.simplebuilding.blocks.custom.EndSignalPowderBlock.NORTH).getSerializedName() + " "
                + state.getValue(com.simplebuilding.blocks.custom.EndSignalPowderBlock.EAST).getSerializedName() + " "
                + state.getValue(com.simplebuilding.blocks.custom.EndSignalPowderBlock.SOUTH).getSerializedName() + " "
                + state.getValue(com.simplebuilding.blocks.custom.EndSignalPowderBlock.WEST).getSerializedName();
    }
}

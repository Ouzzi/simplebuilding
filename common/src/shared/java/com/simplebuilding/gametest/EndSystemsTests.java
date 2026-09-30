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
        for (var pos : path) helper.setBlock(pos, ModBlocks.NIHILITH_POWDER);
        for (int x = 1; x <= 5; x++) helper.setBlock(new BlockPos(x, 1, 6), ModBlocks.ASTRALIT_POWDER);
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
            for (int x = 1; x <= 4; x++) helper.setBlock(new BlockPos(x, 1, 1), ModBlocks.ASTRALIT_POWDER);
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
}

package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
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

import java.util.List;

public final class EndSystemsTests {
    public static void redstoneAliasesResolveItemsAndBlocks(GameTestHelper helper) {
        if (!active(helper)) return;
        var items = net.minecraft.core.registries.BuiltInRegistries.ITEM;
        var blocks = net.minecraft.core.registries.BuiltInRegistries.BLOCK;
        for (var entry : java.util.Map.of("astralit_powder", "astral_redstone",
                "nihilith_powder", "nihil_redstone").entrySet()) {
            var oldId = net.minecraft.resources.Identifier.fromNamespaceAndPath("simplebuilding", entry.getKey());
            // CIRC46: the redstone lines live in Simple Ores; both old SimpleBuilding ids resolve to the Simple Ores id.
            var newId = net.minecraft.resources.Identifier.fromNamespaceAndPath("simpleores", entry.getValue());
            var movedId = net.minecraft.resources.Identifier.fromNamespaceAndPath("simplebuilding", entry.getValue());
            helper.assertTrue(items.get(newId).isPresent() && blocks.get(newId).isPresent(), "new redstone id missing: " + newId);
            assertAlias(helper, items, oldId, newId);
            assertAlias(helper, blocks, oldId, newId);
            assertAlias(helper, items, movedId, newId);
            assertAlias(helper, blocks, movedId, newId);
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

    /**
     * simplebuilding-7 (PLAN40 A): with claims in a shared world the vault opens only on protected land where the player may
     * use containers. The rule is checked through {@link com.simplebuilding.blocks.custom.NihilVaultBlock#decide} with a
     * local provider - registering one on the test server would lock the parallel vault tests - and the test server itself
     * (no shared world) keeps opening it as before.
     */
    public static void nihilVaultOnlyInOwnClaims(GameTestHelper helper) {
        if (!active(helper)) return;
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        java.util.UUID owner = java.util.UUID.randomUUID(), stranger = java.util.UUID.randomUUID();
        String dim = helper.getLevel().dimension().identifier().toString();
        // One claimed column at pos: its owner (and an administrator, like Simple Tweaks' opBypass) may use containers there;
        // everywhere else is wilderness where everybody may.
        java.util.function.Predicate<com.simplebuilding.blocks.custom.NihilVaultBlock.Access> claim = t ->
                t.x() != pos.getX() || t.z() != pos.getZ() || owner.equals(t.actor()) || t.administrator();
        helper.assertTrue(com.simplebuilding.blocks.custom.NihilVaultBlock.decide(claim, dim, pos, owner, false), "owner cannot open the vault in his claim");
        helper.assertFalse(com.simplebuilding.blocks.custom.NihilVaultBlock.decide(claim, dim, pos, stranger, false), "a stranger opened the vault in a foreign claim");
        helper.assertTrue(com.simplebuilding.blocks.custom.NihilVaultBlock.decide(claim, dim, pos, stranger, true), "the provider's administrator bypass was ignored");
        helper.assertFalse(com.simplebuilding.blocks.custom.NihilVaultBlock.decide(claim, dim, pos.east(3), owner, false), "the vault opened on unclaimed land of a claims server");
        helper.assertFalse(com.simplebuilding.blocks.custom.NihilVaultBlock.decide(t -> false, dim, pos, owner, false), "a claim without container rights opened the vault");
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.NIHIL_VAULT);
        var player = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(com.simplebuilding.blocks.custom.NihilVaultBlock.mayOpen(helper.getLevel(), player, pos), "the vault is locked in a world nobody shares");
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

    /**
     * W24-6: the lid event reaches a vault like a Vanilla ender chest (control). Vanilla's opener counter sends it as
     * {@code Blocks.ENDER_CHEST}, which the level drops at a vault; {@code EnderChestOpenerMixin} resends it.
     */
    public static void vaultLidOpensLikeEnderChest(GameTestHelper helper) {
        if (!active(helper)) return;
        var blocks = List.of(Blocks.ENDER_CHEST, ModBlocks.ASTRAL_VAULT, ModBlocks.NIHIL_VAULT);
        var world = helper.getLevel();
        var players = new java.util.ArrayList<ServerPlayer>();
        boolean astral = ServerTuning.get().features.astralVault, nihil = ServerTuning.get().features.nihilVault;
        try {
            ServerTuning.get().features.astralVault = true;
            ServerTuning.get().features.nihilVault = true;
            for (int i = 0; i < blocks.size(); i++) {
                var pos = new BlockPos(1 + 2 * i, 1, 1);
                helper.setBlock(pos, blocks.get(i));
                var absolute = helper.absolutePos(pos);
                var player = helper.makeMockServerPlayerInLevel();
                player.setPos(Vec3.atCenterOf(absolute));
                world.getBlockState(absolute).useWithoutItem(world, player,
                        new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false));
                helper.assertTrue(player.containerMenu instanceof ChestMenu, blocks.get(i) + " did not open");
                players.add(player);
            }
        } finally {
            ServerTuning.get().features.astralVault = astral;
            ServerTuning.get().features.nihilVault = nihil;
        }
        helper.runAfterDelay(2, () -> {
            for (int i = 0; i < blocks.size(); i++) {
                var absolute = helper.absolutePos(new BlockPos(1 + 2 * i, 1, 1));
                var chest = (EnderChestBlockEntity) world.getBlockEntity(absolute);
                for (int t = 0; t < 12; t++) EnderChestBlockEntity.lidAnimateTick(world, absolute, world.getBlockState(absolute), chest);
                helper.assertTrue(chest.getOpenNess(1.0F) > 0.9F, blocks.get(i) + " lid did not open");
                players.get(i).closeContainer();
            }
            helper.runAfterDelay(2, () -> {
                for (int i = 0; i < blocks.size(); i++) {
                    var absolute = helper.absolutePos(new BlockPos(1 + 2 * i, 1, 1));
                    var chest = (EnderChestBlockEntity) world.getBlockEntity(absolute);
                    for (int t = 0; t < 12; t++) EnderChestBlockEntity.lidAnimateTick(world, absolute, world.getBlockState(absolute), chest);
                    helper.assertTrue(chest.getOpenNess(1.0F) < 0.1F, blocks.get(i) + " lid did not close");
                }
                helper.succeed();
            });
        });
    }

}

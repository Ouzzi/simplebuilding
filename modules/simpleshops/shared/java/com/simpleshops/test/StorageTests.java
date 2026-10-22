package com.simpleshops.test;

import static com.simpleshops.test.ShopsTests.buy;
import static com.simpleshops.test.ShopsTests.count;
import static com.simpleshops.test.ShopsTests.player;
import static com.simpleshops.test.ShopsTests.trade;

import com.simpleshops.ShopsRegistry;
import com.simpleshops.block.DisplayCaseBlockEntity;
import com.simpleshops.block.EarningsStore;
import com.simpleshops.block.SalesBlockEntity;
import com.simpleshops.block.SalesStorageBlock;
import com.simpleshops.block.SalesStorageBlockEntity;
import com.simpleshops.logic.ShopActions;
import com.simpleshops.logic.ShopGuard;
import com.simpleshops.logic.ShopLedger;
import com.simpleshops.logic.ShopStorage;
import com.simpleshops.logic.ShopTrade;
import com.simpleshops.menu.SalesOwnerMenu;
import com.simpleshops.menu.ShopBuyerMenu;
import com.simpleshops.menu.StockMenu;
import com.simpleshops.menu.StorageMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Tests of the shop rework (queue N66): the one-screen owner menu, the developer buyer view, the full-earnings rule and the
 * Sales Storage chain with its abuse cases (duplication, foreign storages, owner change, explosion, pistons, hoppers, chunks).
 * Column on the arena: sales block (7,6,7), display case above it, storages below it at y 5, 4, 3 ... (facing up, like the
 * chains of older worlds; SP22-7 storages attach to any side of the sales block, see storage_placement_rules).
 */
final class StorageTests {
    static void register(Map<String, Consumer<GameTestHelper>> all) {
        all.put("storage_registered_protected", h -> {
            h.assertTrue(BuiltInRegistries.ITEM.containsKey(com.simpleshops.SimpleShops.id("sales_storage")), "item");
            h.assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(com.simpleshops.SimpleShops.id("sales_storage")), "block entity type");
            for (String menu : List.of("shop", "storage")) h.assertTrue(BuiltInRegistries.MENU.containsKey(com.simpleshops.SimpleShops.id(menu)), "menu " + menu);
            h.assertTrue(h.getLevel().getServer().getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                    com.simpleshops.SimpleShops.id("sales_storage"))).isPresent(), "recipe");
            var state = ShopsRegistry.SALES_STORAGE.defaultBlockState();
            h.assertTrue(ShopsRegistry.SALES_STORAGE.getExplosionResistance() >= 3_600_000.0F, "blast resistance");
            h.assertTrue(state.getPistonPushReaction() == net.minecraft.world.level.material.PushReaction.IMMOVEABLE, "pistons never move it");
            h.assertTrue(state.is(BlockTags.WITHER_IMMUNE) && state.is(BlockTags.DRAGON_IMMUNE), "wither and dragon immune");
            h.assertTrue(ShopGuard.isShop(state), "covered by the shop protection");
            h.assertTrue(SalesStorageBlockEntity.EARNINGS_SIZE == 27 && SalesBlockEntity.EARNINGS_SIZE == 27, "chest size");
            h.succeed();
        });
        all.put("storage_placement_rules", h -> {
            // SP22-7: only sneak + right click on one's own sales block (any side) places it; everything else places nothing and keeps the item.
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            ServerPlayer other = player(h, GameType.SURVIVAL);
            Column c = column(h, owner, 0);
            ServerLevel level = h.getLevel();
            BlockPos below = c.sales.below();
            level.setBlock(below, Blocks.AIR.defaultBlockState(), 3);
            h.assertTrue(attach(owner, c.sales, Direction.DOWN, false).getCount() == 2 && level.getBlockState(below).isAir(), "without sneaking: no storage, item kept");
            BlockPos stone = c.sales.east(3);
            level.setBlock(stone, Blocks.STONE.defaultBlockState(), 3);
            h.assertTrue(attach(owner, stone, Direction.UP, true).getCount() == 2 && level.getBlockState(stone.above()).isAir(), "not on a plain block, item kept");
            h.assertTrue(attach(other, c.sales, Direction.DOWN, true).getCount() == 2 && level.getBlockState(below).isAir(), "a stranger cannot attach to my sales block");
            h.assertTrue(attach(owner, c.sales, Direction.DOWN, true).getCount() == 1 && level.getBlockState(below).is(ShopsRegistry.SALES_STORAGE), "owner attached it below");
            h.assertTrue(level.getBlockState(below).getValue(SalesStorageBlock.FACING) == Direction.UP, "it faces the sales block");
            SalesStorageBlockEntity st = (SalesStorageBlockEntity) level.getBlockEntity(below);
            h.assertTrue(st != null && st.isOwner(owner.getUUID()) && st.earnings().stream().allMatch(ItemStack::isEmpty), "owner from the placer, empty");
            h.assertTrue(attach(owner, c.sales, Direction.EAST, true).getCount() == 1 && level.getBlockState(c.sales.east()).getValue(SalesStorageBlock.FACING) == Direction.WEST,
                    "any side: east, facing west");
            // Not on a storage (no chains any more), not where the case stands.
            h.assertTrue(attach(owner, below, Direction.DOWN, true).getCount() == 2 && level.getBlockState(below.below()).isAir(), "no storage on a storage");
            h.assertTrue(attach(owner, c.sales, Direction.UP, true).getCount() == 2 && level.getBlockState(c.box).is(ShopsRegistry.DISPLAY_CASE), "the case keeps its place");
            h.assertTrue(ShopStorage.chain(c.sales()).size() == 3, "both storages are in the chain");
            // Without a player (dispenser) nothing places.
            var ctx = new net.minecraft.world.item.context.BlockPlaceContext(level, null, net.minecraft.world.InteractionHand.MAIN_HAND,
                    new ItemStack(ShopsRegistry.SALES_STORAGE_ITEM), new BlockHitResult(Vec3.atCenterOf(c.sales), Direction.WEST, c.sales, false));
            h.assertTrue(ShopsRegistry.SALES_STORAGE.getStateForPlacement(ctx) == null, "no placement without a player");
            h.succeed();
        });
        all.put("storage_side_pull", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            ServerPlayer other = player(h, GameType.SURVIVAL);
            Column c = column(h, owner, 0);
            ServerLevel level = h.getLevel();
            attach(owner, c.sales, Direction.NORTH, true);
            h.assertTrue(level.getBlockEntity(c.sales.north()) instanceof SalesStorageBlockEntity, "attached on the north side");
            SalesStorageBlockEntity side = (SalesStorageBlockEntity) level.getBlockEntity(c.sales.north());
            // A stranger's storage facing the sales block and my storage facing away take nothing.
            level.setBlock(c.sales.west(), ShopsRegistry.SALES_STORAGE.defaultBlockState().setValue(SalesStorageBlock.FACING, Direction.EAST), 3);
            SalesStorageBlockEntity foreign = (SalesStorageBlockEntity) level.getBlockEntity(c.sales.west());
            foreign.initFor(other);
            level.setBlock(c.sales.south(), ShopsRegistry.SALES_STORAGE.defaultBlockState().setValue(SalesStorageBlock.FACING, Direction.SOUTH), 3);
            SalesStorageBlockEntity away = (SalesStorageBlockEntity) level.getBlockEntity(c.sales.south());
            away.initFor(owner);
            List<EarningsStore> chain = ShopStorage.chain(c.sales());
            h.assertTrue(chain.size() == 2 && chain.contains(side), "only the storage facing the sales block with the same owner is in the chain");
            c.sales().earnings().set(0, new ItemStack(Items.EMERALD, 20));
            c.sales().earnings().set(4, new ItemStack(Items.GOLD_INGOT, 3));
            h.assertTrue(!ShopStorage.pull(foreign) && !ShopStorage.pull(away), "foreign or turned away: no pull");
            for (int i = 0; i < 10; i++) ShopStorage.pull(side);
            h.assertTrue(c.sales().earnings().stream().allMatch(ItemStack::isEmpty), "the sales block was emptied sideways");
            h.assertTrue(count(side.earningsView(), Items.EMERALD) == 20 && count(side.earningsView(), Items.GOLD_INGOT) == 3, "all in the side storage");
            h.assertTrue(foreign.earnings().stream().allMatch(ItemStack::isEmpty) && away.earnings().stream().allMatch(ItemStack::isEmpty), "the others got nothing");
            h.succeed();
        });
        all.put("storage_legacy_facing_migrates", h -> {
            // A storage of an older world: horizontal look-only facing, hung below the sales block, no "aimed" flag yet.
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            Column c = column(h, owner, 0);
            ServerLevel level = h.getLevel();
            BlockPos below = c.sales.below();
            level.setBlock(below.below(), Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(below, ShopsRegistry.SALES_STORAGE.defaultBlockState().setValue(SalesStorageBlock.FACING, Direction.NORTH), 3);
            SalesStorageBlockEntity old = (SalesStorageBlockEntity) level.getBlockEntity(below);
            old.initFor(owner);
            old.forgetAim();
            // A new storage keeps its facing even when it points at nothing (no guessing for placed blocks).
            BlockPos east = c.sales.east();
            level.setBlock(east, ShopsRegistry.SALES_STORAGE.defaultBlockState().setValue(SalesStorageBlock.FACING, Direction.EAST), 3);
            ((SalesStorageBlockEntity) level.getBlockEntity(east)).initFor(owner);
            c.sales().earnings().set(0, new ItemStack(Items.DIAMOND, 4));
            h.runAfterDelay(40, () -> {
                h.assertTrue(level.getBlockState(below).getValue(SalesStorageBlock.FACING) == Direction.UP, "the old storage turned to face up");
                h.assertTrue(level.getBlockEntity(below) instanceof SalesStorageBlockEntity st && count(st.earningsView(), Items.DIAMOND) == 4, "and pulls from the sales block again");
                h.assertTrue(level.getBlockState(east).getValue(SalesStorageBlock.FACING) == Direction.EAST, "a new storage is never turned");
                h.succeed();
            });
        });
        MAX_TICKS_PUT("storage_legacy_facing_migrates", 100);
        all.put("storage_chain_flow", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            Column c = column(h, owner, 3);
            c.sales().earnings().set(0, new ItemStack(Items.EMERALD, 40));
            c.sales().earnings().set(5, new ItemStack(Items.GOLD_INGOT, 7));
            int before = total(h, c, Items.EMERALD) + total(h, c, Items.GOLD_INGOT);
            for (int i = 0; i < 100; i++) for (SalesStorageBlockEntity s : c.storages()) ShopStorage.pull(s);
            h.assertTrue(c.sales().earnings().stream().allMatch(ItemStack::isEmpty), "the sales block was emptied");
            h.assertTrue(c.storages().get(0).earnings().stream().allMatch(ItemStack::isEmpty) && c.storages().get(1).earnings().stream().allMatch(ItemStack::isEmpty),
                    "items passed down like a hopper chain");
            h.assertTrue(count(c.storages().get(2).earningsView(), Items.EMERALD) == 40 && count(c.storages().get(2).earningsView(), Items.GOLD_INGOT) == 7, "all at the bottom");
            h.assertTrue(total(h, c, Items.EMERALD) + total(h, c, Items.GOLD_INGOT) == before, "nothing lost or made");
            // The world tick does it too (every 8 ticks), without any call from here.
            c.sales().earnings().set(1, new ItemStack(Items.DIAMOND, 5));
            c.sales().changed();
            h.runAfterDelay(60, () -> {
                h.assertTrue(c.sales().earnings().get(1).isEmpty() && count(c.storages().get(2).earningsView(), Items.DIAMOND) == 5, "the block ticker moved it by itself");
                h.succeed();
            });
        });
        MAX_TICKS_PUT("storage_chain_flow", 120);
        all.put("storage_full_chain_loses_nothing", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            Column c = column(h, owner, 2);
            List<Item> junk = junk(80);
            int placed = 0;
            for (int i = 0; i < 27; i++) {
                c.sales().earnings().set(i, new ItemStack(junk.get(i), junk.get(i).getDefaultMaxStackSize()));
                placed++;
            }
            for (int i = 0; i < 27; i++) c.storages().get(0).earnings().set(i, new ItemStack(junk.get(27 + i), junk.get(27 + i).getDefaultMaxStackSize()));
            for (int i = 0; i < 27; i++) c.storages().get(1).earnings().set(i, new ItemStack(junk.get(54 + i % 26), junk.get(54 + i % 26).getDefaultMaxStackSize()));
            // Distinct items per slot are not needed here: a full stack of an item merges nowhere. Count everything before and after.
            Map<String, Integer> before = census(c);
            for (int i = 0; i < 50; i++) for (SalesStorageBlockEntity s : c.storages()) ShopStorage.pull(s);
            h.assertTrue(census(c).equals(before), "a full chain moves nothing and loses nothing");
            c.sales().prices().set(2, new ItemStack(Items.EMERALD, 1));
            h.assertTrue(!ShopTrade.fits(c.sales()), "full chain: the price does not fit");
            h.succeed();
        });
        all.put("storage_trade_deposits_into_chain", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            ServerPlayer buyer = player(h, GameType.SURVIVAL);
            Column c = column(h, owner, 1);
            c.box().goods().set(0, new ItemStack(Items.DIAMOND, 3));
            c.sales().prices().set(2, new ItemStack(Items.EMERALD, 4));
            List<Item> junk = junk(60);
            for (int i = 0; i < 27; i++) c.sales().earnings().set(i, new ItemStack(junk.get(i), junk.get(i).getDefaultMaxStackSize()));
            ShopBuyerMenu menu = buyOpen(h, buyer, c);
            menu.pay().setItem(2, new ItemStack(Items.EMERALD, 4));
            menu.broadcastChanges();
            h.assertTrue(!menu.earningsFull(), "the storage below takes the price: not full");
            h.assertTrue(trade(menu, buyer) == ShopTrade.Result.OK, "trade goes through");
            h.assertTrue(count(c.storages().get(0).earningsView(), Items.EMERALD) == 4, "payment landed in the storage");
            h.assertTrue(count(buyer.getInventory(), Items.DIAMOND) == 1 && c.box().goods().get(0).getCount() == 2, "one piece sold");
            // Fill the storage too: now the price does not fit, the buy is off and nothing happens even if the client insists.
            for (int i = 0; i < 27; i++) c.storages().get(0).earnings().set(i, new ItemStack(junk.get(30 + i), junk.get(30 + i).getDefaultMaxStackSize()));
            menu.pay().setItem(2, new ItemStack(Items.EMERALD, 4));
            menu.broadcastChanges();
            h.assertTrue(menu.earningsFull() && !menu.canTradeClient(), "earnings full: button off");
            Map<String, Integer> before = census(c);
            int pay = menu.pay().getItem(2).getCount();
            for (int action : new int[] {ShopBuyerMenu.TRADE, ShopBuyerMenu.TRADE_MAX}) {
                menu.clickMenuButton(buyer, ShopBuyerMenu.button(action, menu.revisionSeen()));
                h.assertTrue(menu.lastResult == ShopTrade.Result.EARNINGS_FULL, "server refuses " + action);
            }
            h.assertTrue(census(c).equals(before) && menu.pay().getItem(2).getCount() == pay && count(buyer.getInventory(), Items.DIAMOND) == 1, "nothing changed");
            // A foreign storage below does not count as space.
            for (int i = 0; i < 27; i++) c.storages().get(0).earnings().set(i, ItemStack.EMPTY);
            c.storages().get(0).initFor(player(h, GameType.SURVIVAL));
            h.assertTrue(trade(menu, buyer) == ShopTrade.Result.EARNINGS_FULL, "foreign storage is no space");
            h.assertTrue(count(c.storages().get(0).earningsView(), Items.EMERALD) == 0, "nothing went into the foreign storage");
            h.succeed();
        });
        all.put("storage_owner_change_breaks_chain", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            ServerPlayer other = player(h, GameType.SURVIVAL);
            Column c = column(h, owner, 3);
            h.assertTrue(ShopStorage.chain(c.sales()).size() == 4, "chain of four");
            c.sales().earnings().set(0, new ItemStack(Items.EMERALD, 9));
            // Middle storage changes hands: nothing passes it in either direction, the chain ends above it.
            c.storages().get(1).initFor(other);
            h.assertTrue(ShopStorage.chain(c.sales()).size() == 2, "chain ends above the foreign storage");
            h.assertTrue(ShopStorage.pull(c.storages().get(0)) && !ShopStorage.pull(c.storages().get(1)) && !ShopStorage.pull(c.storages().get(2)), "only the owner's part moves");
            c.storages().get(1).earnings().set(0, new ItemStack(Items.GOLD_INGOT, 2));
            for (int i = 0; i < 50; i++) for (SalesStorageBlockEntity s : c.storages()) ShopStorage.pull(s);
            h.assertTrue(count(c.storages().get(1).earningsView(), Items.GOLD_INGOT) == 2 && count(c.storages().get(2).earningsView(), Items.GOLD_INGOT) == 0,
                    "the foreign storage keeps its items and gives nothing down");
            // A sales block that becomes a server shop (no owner) feeds nobody.
            c.sales().initServer("s999");
            h.assertTrue(ShopStorage.chain(c.sales()).size() == 1 && !ShopStorage.pull(c.storages().get(0)), "no owner, no chain");
            h.succeed();
        });
        all.put("storage_unloaded_or_removed", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            Column c = column(h, owner, 2);
            BlockPos far = new BlockPos(30_000_000 - 16, 64, 30_000_000 - 16);
            h.assertTrue(ShopStorage.storeAt(h.getLevel(), far) == null && ShopStorage.storeAt(h.getLevel(), far.below()) == null, "an unloaded chunk is no store");
            c.sales().earnings().set(0, new ItemStack(Items.EMERALD, 5));
            SalesStorageBlockEntity top = c.storages().get(0);
            top.setRemoved();
            h.assertTrue(ShopStorage.chain(c.sales()).size() == 1 && !ShopStorage.pull(top), "a removed storage is not used and pulls nothing");
            h.assertTrue(c.sales().earnings().get(0).getCount() == 5, "nothing taken from the sales block");
            h.succeed();
        });
        all.put("storage_roundtrip_no_dupe", h -> {
            // Chunk save/load in the middle of a transfer chain: every block entity writes its state at once, nothing exists twice.
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            Column c = column(h, owner, 2);
            c.sales().earnings().set(0, new ItemStack(Items.EMERALD, 30));
            ShopStorage.pull(c.storages().get(0));
            ShopStorage.pull(c.storages().get(1));
            var registries = h.getLevel().registryAccess();
            int sum = 0;
            for (EarningsStore store : ShopStorage.chain(c.sales())) {
                BlockEntity be = (BlockEntity) store;
                var tag = be.saveWithoutMetadata(registries);
                BlockEntity back = be instanceof SalesBlockEntity ? new SalesBlockEntity(be.getBlockPos(), be.getBlockState())
                        : new SalesStorageBlockEntity(be.getBlockPos(), be.getBlockState());
                back.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, registries, tag));
                // Ender shops: the chunk keeps only the entry id, the items stay in the world data (so a reload cannot double them).
                h.assertTrue(tag.contains("ender_id") && !tag.contains("earnings"), "no items in the chunk data");
                back.setLevel(h.getLevel());
                for (ItemStack s : ((EarningsStore) back).earnings()) if (s.is(Items.EMERALD)) sum += s.getCount();
                h.assertTrue(((EarningsStore) back).owner() != null && ((EarningsStore) back).owner().equals(owner.getUUID()), "owner survives");
            }
            h.assertTrue(sum == 30, "exactly the 30 emeralds after save and load, got " + sum);
            h.succeed();
        });
        all.put("storage_break_and_menu_rules", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            ServerPlayer stranger = player(h, GameType.SURVIVAL);
            ServerPlayer creative = player(h, GameType.CREATIVE);
            Column c = column(h, owner, 1);
            BlockPos at = c.storagePos(0);
            SalesStorageBlockEntity st = c.storages().get(0);
            st.earnings().set(3, new ItemStack(Items.DIAMOND, 6));
            h.assertTrue(!stranger.gameMode.destroyBlock(at) && !creative.gameMode.destroyBlock(at), "strangers cannot break it");
            h.assertTrue(h.getLevel().getBlockState(at).getDestroyProgress(stranger, h.getLevel(), at) == 0.0F, "no breaking animation for strangers");
            // Menu: owner only, take out only.
            h.assertTrue(ShopActions.useStorage(stranger, h.getLevel(), at) == net.minecraft.world.InteractionResult.CONSUME && !(stranger.containerMenu instanceof StorageMenu), "stranger gets no menu");
            ShopActions.useStorage(owner, h.getLevel(), at);
            h.assertTrue(owner.containerMenu instanceof StorageMenu, "owner gets the menu");
            StorageMenu menu = (StorageMenu) owner.containerMenu;
            h.assertTrue(!menu.stillValid(stranger) && menu.stillValid(owner), "menu is the owner's");
            owner.getInventory().clearContent();
            menu.clicked(3, 0, ContainerInput.QUICK_MOVE, owner);
            h.assertTrue(count(owner.getInventory(), Items.DIAMOND) == 6 && st.earnings().get(3).isEmpty(), "taken out");
            menu.setCarried(new ItemStack(Items.STICK, 2));
            menu.clicked(0, 0, ContainerInput.PICKUP, owner);
            h.assertTrue(st.earnings().get(0).isEmpty(), "nothing can be put in");
            // Break drops the contents exactly once.
            st.earnings().set(5, new ItemStack(Items.GOLD_INGOT, 4));
            h.assertTrue(owner.gameMode.destroyBlock(at), "owner breaks it");
            int dropped = 0;
            for (ItemEntity e : h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(3))) if (e.getItem().is(Items.GOLD_INGOT)) dropped += e.getItem().getCount();
            h.assertTrue(dropped == 4 && st.earnings().stream().allMatch(ItemStack::isEmpty), "contents dropped once, got " + dropped);
            h.succeed();
        });
        all.put("storage_explosion_piston_hopper", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            Column c = column(h, owner, 2);
            BlockPos at = c.storagePos(0);
            c.storages().get(0).earnings().set(0, new ItemStack(Items.EMERALD, 12));
            ServerLevel level = h.getLevel();
            Vec3 v = Vec3.atCenterOf(at).add(1.5, 0.0, 0.0);
            level.explode(null, v.x, v.y, v.z, 12.0F, Level.ExplosionInteraction.TNT);
            level.explode(null, v.x, v.y + 1, v.z, 8.0F, Level.ExplosionInteraction.MOB);
            level.explode(null, v.x, v.y, v.z, 6.0F, Level.ExplosionInteraction.BLOCK);
            for (BlockPos p : List.of(c.sales, at, c.storagePos(1))) h.assertTrue(!level.getBlockState(p).isAir(), "explosions leave " + p);
            h.assertTrue(count(c.storages().get(0).earningsView(), Items.EMERALD) == 12 && level.getBlockEntity(at) == c.storages().get(0), "contents intact");
            for (Direction dir : Direction.values()) {
                h.assertTrue(!PistonBaseBlock.isPushable(level.getBlockState(at), level, at, dir, false, dir), "not pushable " + dir);
            }
            h.assertTrue(!((Object) level.getBlockEntity(at) instanceof net.minecraft.world.Container), "no container");
            h.assertTrue(HopperBlockEntity.getContainerAt(level, at) == null, "hoppers (and hopper minecarts) find no container");
            // A hopper under the lowest storage and one beside it cannot pull or push anything.
            BlockPos lowest = c.storagePos(1);
            level.setBlock(lowest.below(), Blocks.HOPPER.defaultBlockState(), 3);
            c.storages().get(1).earnings().set(0, new ItemStack(Items.GOLD_INGOT, 3));
            for (int i = 0; i < 40; i++) {
                if (level.getBlockEntity(lowest.below()) instanceof HopperBlockEntity hop) HopperBlockEntity.suckInItems(level, hop);
            }
            HopperBlockEntity hop = (HopperBlockEntity) level.getBlockEntity(lowest.below());
            h.assertTrue(hop.isEmpty() && count(c.storages().get(1).earningsView(), Items.GOLD_INGOT) == 3, "a hopper takes nothing out");
            for (BlockPos pos : List.of(at, c.sales)) {
                for (Direction dir : new Direction[] {Direction.UP, Direction.DOWN, Direction.NORTH}) {
                    try {
                        Object storage = Class.forName("net.fabricmc.fabric.api.transfer.v1.item.ItemStorage").getField("SIDED").get(null);
                        for (var m : storage.getClass().getMethods()) {
                            if (m.getName().equals("find") && m.getParameterCount() == 3) {
                                h.assertTrue(m.invoke(storage, level, pos, dir) == null, "Fabric item storage at " + pos + " " + dir);
                                break;
                            }
                        }
                    } catch (ClassNotFoundException | NoSuchFieldException ignored) {
                        // not Fabric
                    } catch (ReflectiveOperationException e) {
                        throw h.assertionException("reflection: " + e);
                    }
                }
            }
            h.succeed();
        });
        all.put("dev_buyer_view", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            ServerPlayer stranger = player(h, GameType.SURVIVAL);
            ServerPlayer op = player(h, GameType.SURVIVAL);
            h.getLevel().getServer().getPlayerList().op(op.nameAndId(), java.util.Optional.of(net.minecraft.server.permissions.LevelBasedPermissionSet.GAMEMASTER), java.util.Optional.empty());
            h.runBeforeTestEnd(() -> h.getLevel().getServer().getPlayerList().deop(op.nameAndId()));
            Column c = column(h, owner, 0);
            c.box().goods().set(0, new ItemStack(Items.DIAMOND, 3));
            c.sales().prices().set(2, new ItemStack(Items.EMERALD, 2));
            // Owner: one shared screen, whichever block is clicked.
            ShopActions.useSales(owner, h.getLevel(), c.sales);
            h.assertTrue(owner.containerMenu instanceof SalesOwnerMenu m && m.isCombined(), "sales click: shared owner screen");
            owner.closeContainer();
            ShopActions.useCase(owner, h.getLevel(), c.box);
            h.assertTrue(owner.containerMenu instanceof SalesOwnerMenu m && m.isCombined(), "case click: shared owner screen");
            SalesOwnerMenu ownerMenu = (SalesOwnerMenu) owner.containerMenu;
            // A stranger cannot drive the owner's button, not even with the owner's menu object.
            stranger.setPos(Vec3.atCenterOf(c.sales).add(0, 0, -2));
            h.assertTrue(!ownerMenu.clickMenuButton(stranger, SalesOwnerMenu.DEV_BUYER), "stranger cannot use the developer button");
            // Owner switches to the buyer view and makes a test purchase: a normal trade (net zero for the owner).
            owner.getInventory().clearContent();
            owner.getInventory().setItem(0, new ItemStack(Items.EMERALD, 2));
            h.assertTrue(ownerMenu.clickMenuButton(owner, SalesOwnerMenu.DEV_BUYER), "switch");
            h.assertTrue(owner.containerMenu instanceof ShopBuyerMenu, "buyer screen");
            ShopBuyerMenu buyer = (ShopBuyerMenu) owner.containerMenu;
            buyer.broadcastChanges();
            h.assertTrue(buyer.canManage(), "owner sees the way back");
            int before = total(h, c, Items.EMERALD) + count(owner.getInventory(), Items.EMERALD) + total(h, c, Items.DIAMOND) + count(owner.getInventory(), Items.DIAMOND);
            buyer.pay().setItem(2, owner.getInventory().removeItem(0, 2));
            h.assertTrue(trade(buyer, owner) == ShopTrade.Result.OK, "test purchase");
            int after = total(h, c, Items.EMERALD) + count(owner.getInventory(), Items.EMERALD) + total(h, c, Items.DIAMOND) + count(owner.getInventory(), Items.DIAMOND);
            h.assertTrue(before == after, "the test purchase creates and destroys nothing");
            // A second player is told "busy" while the owner holds the shop in either view.
            ShopActions.useSales(stranger, h.getLevel(), c.sales);
            h.assertTrue(!(stranger.containerMenu instanceof ShopBuyerMenu), "busy");
            // Back to the owner screen; the old menu released the lock, the new one holds it.
            h.assertTrue(buyer.clickMenuButton(owner, ShopBuyerMenu.TO_OWNER), "back");
            h.assertTrue(owner.containerMenu instanceof SalesOwnerMenu m && m.isCombined() && owner.containerMenu != ownerMenu, "owner screen again");
            h.assertTrue(c.sales().lockHolder() == owner, "the lock follows the screen");
            owner.closeContainer();
            // Strangers and operators on a player shop see the buyer screen without the developer way back.
            for (ServerPlayer p : List.of(stranger, op)) {
                p.setPos(Vec3.atCenterOf(c.sales).add(0, 0, -2));
                ShopActions.useSales(p, h.getLevel(), c.sales);
                h.assertTrue(p.containerMenu instanceof ShopBuyerMenu, "buyer screen for " + p.getGameProfile().name());
                ShopBuyerMenu b = (ShopBuyerMenu) p.containerMenu;
                b.broadcastChanges();
                h.assertTrue(!b.canManage() && !b.clickMenuButton(p, ShopBuyerMenu.TO_OWNER) && p.containerMenu == b, "no way to the owner screen");
                p.closeContainer();
            }
            h.succeed();
        });
        all.put("owner_screens_layouts", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            Column c = column(h, owner, 0);
            ShopActions.useSales(owner, h.getLevel(), c.sales);
            SalesOwnerMenu both = (SalesOwnerMenu) owner.containerMenu;
            h.assertTrue(both.isCombined() && both.slots.size() == 5 + 5 + 27 + 36, "combined: goods, price, earnings, inventory");
            h.assertTrue(SalesOwnerMenu.C_HEIGHT == 114 + 6 * 18, "double chest height");
            // Earnings take nothing from outside (only the shop's own transfer fills them); goods slots are real.
            h.assertTrue(!both.slots.get(both.earningsStart()).mayPlace(new ItemStack(Items.STICK)) && both.slots.get(0).mayPlace(new ItemStack(Items.STICK)), "slot rules");
            both.setCarried(new ItemStack(Items.EMERALD, 5));
            both.clicked(both.priceStart() + 2, 1, ContainerInput.PICKUP, owner);
            h.assertTrue(c.sales().prices().get(2).is(Items.EMERALD), "price set in the shared screen");
            both.setCarried(ItemStack.EMPTY);
            owner.closeContainer();
            // Case removed: the sales block has its own screen (price + 27 earnings).
            h.getLevel().setBlock(c.box, Blocks.AIR.defaultBlockState(), 3);
            ShopActions.useSales(owner, h.getLevel(), c.sales);
            SalesOwnerMenu alone = (SalesOwnerMenu) owner.containerMenu;
            h.assertTrue(!alone.isCombined() && alone.slots.size() == 5 + 27 + 36, "sales block alone: own screen");
            owner.closeContainer();
            // Sales block removed instead: the case has its own goods screen.
            h.getLevel().setBlock(c.box, ShopsRegistry.DISPLAY_CASE.defaultBlockState(), 3);
            ((DisplayCaseBlockEntity) h.getLevel().getBlockEntity(c.box)).initFor(owner);
            h.getLevel().setBlock(c.sales, Blocks.AIR.defaultBlockState(), 3);
            ShopActions.useCase(owner, h.getLevel(), c.box);
            h.assertTrue(owner.containerMenu instanceof StockMenu, "display case alone: own screen");
            h.succeed();
        });
        all.put("storage_empty_goods_and_disconnect", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            ServerPlayer buyer = player(h, GameType.SURVIVAL);
            Column c = column(h, owner, 1);
            c.sales().prices().set(2, new ItemStack(Items.EMERALD, 2));
            ShopBuyerMenu menu = buyOpen(h, buyer, c);
            menu.pay().setItem(2, new ItemStack(Items.EMERALD, 2));
            Map<String, Integer> before = census(c);
            h.assertTrue(trade(menu, buyer) == ShopTrade.Result.NO_STOCK, "empty goods: no trade");
            h.assertTrue(census(c).equals(before), "no payment taken for nothing");
            // Disconnect with items in the pay slots: they come back exactly once, the lock is released.
            h.getLevel().getServer().getPlayerList().remove(buyer);
            h.assertTrue(menu.pay().isEmpty() && c.sales().lockFree(), "pay slots returned, lock free after a disconnect");
            h.succeed();
        });
        all.put("storage_dupe_fuzz", h -> {
            // Random trades, pulls, owner take-outs and owner changes: every item exists exactly once, always.
            for (long seed = 1; seed <= 8; seed++) fuzz(h, seed);
            h.succeed();
        });
        MAX_TICKS_PUT("storage_dupe_fuzz", 400);
    }

    private static void MAX_TICKS_PUT(String name, int ticks) {
        ShopsTests.MAX_TICKS.put(name, ticks);
    }

    private static final Item[] POOL = {Items.EMERALD, Items.DIAMOND, Items.GOLD_INGOT, Items.COAL, Items.STICK};

    private static void fuzz(GameTestHelper h, long seed) {
        Random rnd = new Random(seed);
        ServerPlayer owner = player(h, GameType.SURVIVAL);
        ServerPlayer buyer = player(h, GameType.SURVIVAL);
        Column c = column(h, owner, 3);
        c.box().goods().set(0, new ItemStack(Items.DIAMOND, 30));
        c.box().goods().set(1, new ItemStack(Items.STICK, 30));
        c.sales().prices().set(2, new ItemStack(Items.EMERALD, 1 + rnd.nextInt(3)));
        c.sales().prices().set(3, new ItemStack(Items.GOLD_INGOT, 1 + rnd.nextInt(2)));
        buyer.getInventory().setItem(0, new ItemStack(Items.EMERALD, 64));
        buyer.getInventory().setItem(1, new ItemStack(Items.GOLD_INGOT, 64));
        buyer.getInventory().setItem(2, new ItemStack(Items.COAL, 20));
        List<Item> junk = junk(30);
        ShopBuyerMenu menu = buyOpen(h, buyer, c);
        Map<Item, Integer> reference = universe(h, c, buyer, menu);
        for (int step = 0; step < 150; step++) {
            switch (rnd.nextInt(7)) {
                case 0, 1, 2 -> {
                    for (int p : new int[] {2, 3}) {
                        ItemStack price = c.sales().prices().get(p);
                        if (price.isEmpty() || !menu.pay().getItem(p).isEmpty()) continue;
                        for (int i = 0; i < buyer.getInventory().getContainerSize(); i++) {
                            ItemStack at = buyer.getInventory().getItem(i);
                            if (at.is(price.getItem()) && at.getCount() >= price.getCount()) {
                                menu.pay().setItem(p, at.split(price.getCount()));
                                break;
                            }
                        }
                    }
                    menu.clickMenuButton(buyer, ShopBuyerMenu.button(rnd.nextBoolean() ? ShopBuyerMenu.TRADE : ShopBuyerMenu.TRADE_MAX, menu.revisionSeen()));
                }
                case 3, 4 -> ShopStorage.pull(c.storages().get(rnd.nextInt(3)));
                case 5 -> {
                    // Owner action: add a junk stack to the sales block (fills the chain), or take one out of a random store.
                    List<EarningsStore> chain = new ArrayList<>(ShopStorage.chain(c.sales()));
                    EarningsStore s = chain.get(rnd.nextInt(chain.size()));
                    int slot = rnd.nextInt(27);
                    if (s.earnings().get(slot).isEmpty() && rnd.nextBoolean()) {
                        // Junk is outside the counted pool: it only makes the chain fill up.
                        s.earnings().set(slot, new ItemStack(junk.get(rnd.nextInt(junk.size())), 1));
                    } else if (!s.earnings().get(slot).isEmpty() && java.util.Arrays.asList(POOL).contains(s.earnings().get(slot).getItem())) {
                        ItemStack taken = s.earnings().get(slot);
                        s.earnings().set(slot, ItemStack.EMPTY);
                        owner.getInventory().add(taken);
                    }
                }
                default -> {
                    // A storage changes owner and back (a takeover attempt).
                    SalesStorageBlockEntity st = c.storages().get(rnd.nextInt(3));
                    List<ItemStack> keep = new ArrayList<>();
                    for (ItemStack k : st.earnings()) keep.add(k.copy());
                    st.initFor(rnd.nextBoolean() ? buyer : owner);
                    for (int i = 0; i < keep.size(); i++) st.earnings().set(i, keep.get(i));
                }
            }
            Map<Item, Integer> now = universe(h, c, buyer, menu);
            if (!now.equals(reference)) throw h.assertionException("seed " + seed + " step " + step + ": " + reference + " became " + now);
        }
    }

    // --- helpers -------------------------------------------------------------------------------------------------------------

    /** Sales block with the case above and {@code storages} storages below, all owned by {@code owner}. */
    static final class Column {
        final GameTestHelper h;
        final BlockPos sales, box;
        final int storages;

        Column(GameTestHelper h, BlockPos sales, BlockPos box, int storages) {
            this.h = h;
            this.sales = sales;
            this.box = box;
            this.storages = storages;
        }

        BlockPos storagePos(int i) {
            return this.sales.below(i + 1);
        }

        SalesBlockEntity sales() {
            return (SalesBlockEntity) this.h.getLevel().getBlockEntity(this.sales);
        }

        DisplayCaseBlockEntity box() {
            return (DisplayCaseBlockEntity) this.h.getLevel().getBlockEntity(this.box);
        }

        List<SalesStorageBlockEntity> storages() {
            List<SalesStorageBlockEntity> out = new ArrayList<>();
            for (int i = 0; i < this.storages; i++) out.add((SalesStorageBlockEntity) this.h.getLevel().getBlockEntity(storagePos(i)));
            return out;
        }
    }

    static Column column(GameTestHelper h, ServerPlayer owner, int storages) {
        ServerLevel level = h.getLevel();
        BlockPos s = h.absolutePos(new BlockPos(7, 6, 7));
        BlockPos c = s.above();
        level.setBlock(s.below(storages + 1), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(s, ShopsRegistry.SALES_BLOCK.defaultBlockState(), 3);
        level.setBlock(c, ShopsRegistry.DISPLAY_CASE.defaultBlockState(), 3);
        ((SalesBlockEntity) level.getBlockEntity(s)).initFor(owner);
        ((DisplayCaseBlockEntity) level.getBlockEntity(c)).initFor(owner);
        ShopLedger.record(level, s, owner.getUUID(), true);
        ShopLedger.record(level, c, owner.getUUID(), false);
        for (int i = 0; i < storages; i++) {
            level.setBlock(s.below(i + 1), ShopsRegistry.SALES_STORAGE.defaultBlockState(), 3);
            ((SalesStorageBlockEntity) level.getBlockEntity(s.below(i + 1))).initFor(owner);
        }
        return new Column(h, s, c, storages);
    }

    static ShopBuyerMenu buyOpen(GameTestHelper h, ServerPlayer buyer, Column c) {
        buyer.setPos(Vec3.atCenterOf(c.sales).add(0.0, 0.0, -2.0));
        ShopBuyerMenu menu = new ShopBuyerMenu(7, buyer.getInventory(), c.sales(), c.box(), buyer);
        buyer.containerMenu = menu;
        return menu;
    }

    /** Right click with two sales storages on the {@code face} side of {@code clicked}, sneaking or not; returns what is left in hand. */
    static ItemStack attach(ServerPlayer player, BlockPos clicked, Direction face, boolean sneak) {
        ItemStack stack = new ItemStack(ShopsRegistry.SALES_STORAGE_ITEM, 2);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
        player.setPos(Vec3.atCenterOf(clicked.relative(face, 3)));
        player.setShiftKeyDown(sneak);
        Vec3 at = Vec3.atCenterOf(clicked).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        player.gameMode.useItemOn(player, player.level(), stack, net.minecraft.world.InteractionHand.MAIN_HAND, new BlockHitResult(at, face, clicked, false));
        player.setShiftKeyDown(false);
        player.closeContainer();
        ItemStack left = player.getMainHandItem();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        return left;
    }

    static void place(ServerPlayer player, ItemStack stack, BlockPos clicked) {
        ShopsTests.place(player, stack, clicked);
    }

    /** Items that are never merged with the pool items of the tests; {@code n} different ones. */
    static List<Item> junk(int n) {
        List<Item> out = new ArrayList<>();
        for (Item it : BuiltInRegistries.ITEM) {
            if (it == Items.AIR || it.getDefaultMaxStackSize() < 2 || java.util.Arrays.asList(POOL).contains(it)) continue;
            out.add(it);
            if (out.size() >= n) break;
        }
        return out;
    }

    static int total(GameTestHelper h, Column c, Item item) {
        int n = 0;
        for (EarningsStore s : allStores(c)) for (ItemStack st : s.earnings()) if (st.is(item)) n += st.getCount();
        for (ItemStack st : c.box().goods()) if (st.is(item)) n += st.getCount();
        return n;
    }

    private static List<EarningsStore> allStores(Column c) {
        List<EarningsStore> out = new ArrayList<>();
        out.add(c.sales());
        out.addAll(c.storages());
        return out;
    }

    static Map<String, Integer> census(Column c) {
        Map<String, Integer> m = new java.util.TreeMap<>();
        for (EarningsStore s : allStores(c)) for (ItemStack st : s.earnings()) if (!st.isEmpty()) m.merge(st.getItem().toString(), st.getCount(), Integer::sum);
        for (ItemStack st : c.box().goods()) if (!st.isEmpty()) m.merge(st.getItem().toString(), st.getCount(), Integer::sum);
        return m;
    }

    private static Map<Item, Integer> universe(GameTestHelper h, Column c, ServerPlayer buyer, ShopBuyerMenu menu) {
        Map<Item, Integer> out = new java.util.LinkedHashMap<>();
        for (Item it : POOL) out.put(it, 0);
        java.util.function.Consumer<ItemStack> add = s -> {
            if (!s.isEmpty() && out.containsKey(s.getItem())) out.merge(s.getItem(), s.getCount(), Integer::sum);
        };
        for (ServerPlayer p : h.getLevel().players()) for (int i = 0; i < p.getInventory().getContainerSize(); i++) add.accept(p.getInventory().getItem(i));
        for (int i = 0; i < 5; i++) add.accept(menu.pay().getItem(i));
        for (EarningsStore s : allStores(c)) s.earnings().forEach(add);
        c.box().goods().forEach(add);
        for (ItemEntity e : h.getLevel().getEntitiesOfClass(ItemEntity.class, AABB.ofSize(Vec3.atCenterOf(c.sales), 24, 24, 24))) add.accept(e.getItem());
        return out;
    }

    private StorageTests() {}
}

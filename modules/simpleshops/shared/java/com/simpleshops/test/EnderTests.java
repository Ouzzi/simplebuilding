package com.simpleshops.test;

import static com.simpleshops.test.ShopsTests.player;

import com.simpleshops.ShopsRegistry;
import com.simpleshops.block.DisplayCaseBlockEntity;
import com.simpleshops.block.SalesBlockEntity;
import com.simpleshops.block.SalesStorageBlockEntity;
import com.simpleshops.logic.EnderShops;
import com.simpleshops.logic.ServerShopCommands;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Shops rework stage 2 (KONZ20 S1 A): the ender data. Old blocks move their contents into the world data without loss, a copied
 * block entity never shares an entry, save/load keeps everything exactly once, breaking deletes the entry, a block removed
 * without side effects leaves an orphan with all its items that an operator can clear.
 */
final class EnderTests {
    static void register(Map<String, Consumer<GameTestHelper>> all) {
        all.put("ender_migration_lossless", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            StorageTests.Column c = StorageTests.column(h, owner, 1);
            ServerLevel level = h.getLevel();
            var registries = level.registryAccess();
            // Old format: the items inside the block entities (what a world from before the rework has in its chunks).
            SalesBlockEntity oldSales = new SalesBlockEntity(c.sales, level.getBlockState(c.sales));
            oldSales.earnings().set(0, new ItemStack(Items.EMERALD, 17));
            oldSales.earnings().set(26, new ItemStack(Items.GOLD_INGOT, 3));
            oldSales.prices().set(2, new ItemStack(Items.EMERALD, 2));
            DisplayCaseBlockEntity oldCase = new DisplayCaseBlockEntity(c.box, level.getBlockState(c.box));
            oldCase.goods().set(1, new ItemStack(Items.DIAMOND, 9));
            SalesStorageBlockEntity oldStorage = new SalesStorageBlockEntity(c.storagePos(0), level.getBlockState(c.storagePos(0)));
            oldStorage.earnings().set(4, new ItemStack(Items.COAL, 33));
            CompoundTag salesTag = withOwner(oldSales.saveWithFullMetadata(registries), owner);
            CompoundTag caseTag = withOwner(oldCase.saveWithFullMetadata(registries), owner);
            CompoundTag storageTag = withOwner(oldStorage.saveWithFullMetadata(registries), owner);
            h.assertTrue(salesTag.contains("earnings") && caseTag.contains("Items") && !salesTag.contains("ender_id"), "old format carries the items");
            // Load them like a chunk from disk would.
            BlockPos[] at = {c.sales, c.box, c.storagePos(0)};
            CompoundTag[] tags = {salesTag, caseTag, storageTag};
            for (int i = 0; i < 3; i++) {
                BlockPos pos = at[i];
                CompoundTag tag = tags[i];
                BlockEntity be = BlockEntity.loadStatic(pos, level.getBlockState(pos), tag, registries);
                h.assertTrue(be != null, "loads " + pos);
                level.setBlockEntity(be);
            }
            // First use moves the items into the world data (the server tick does it too).
            SalesBlockEntity sales = c.sales();
            h.assertTrue(sales != oldSales && sales.vault() != null, "migrated sales block");
            h.assertTrue(count(sales.earnings(), Items.EMERALD) == 17 && count(sales.earnings(), Items.GOLD_INGOT) == 3, "earnings moved");
            h.assertTrue(sales.prices().get(2).is(Items.EMERALD) && sales.prices().get(2).getCount() == 2, "price moved");
            h.assertTrue(count(c.box().goods(), Items.DIAMOND) == 9, "goods moved");
            h.assertTrue(count(c.storages().get(0).earnings(), Items.COAL) == 33, "storage moved");
            // Saved again: only the id, no items in the chunk any more (nothing exists twice).
            CompoundTag after = sales.saveWithFullMetadata(registries);
            CompoundTag afterCase = c.box().saveWithFullMetadata(registries);
            h.assertTrue(after.contains("ender_id") && !after.contains("earnings") && !afterCase.contains("Items"), "new format: id only " + after);
            // Load the new format again: the same entry, the same items, exactly once.
            String id = sales.vault().id;
            BlockEntity again = BlockEntity.loadStatic(c.sales, level.getBlockState(c.sales), after, registries);
            level.setBlockEntity(again);
            SalesBlockEntity reloaded = c.sales();
            h.assertTrue(reloaded == again && reloaded.vault() != null && reloaded.vault().id.equals(id), "same entry after reload");
            h.assertTrue(count(reloaded.earnings(), Items.EMERALD) == 17, "still 17 emeralds, not 34");
            h.succeed();
        });
        all.put("ender_copied_data_is_empty", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            StorageTests.Column c = StorageTests.column(h, owner, 0);
            c.box().goods().set(0, new ItemStack(Items.DIAMOND, 5));
            c.sales().earnings().set(0, new ItemStack(Items.EMERALD, 8));
            ServerLevel level = h.getLevel();
            var registries = level.registryAccess();
            // Full block entity data (with the entry id) copied onto another place (structure, clone, edit tool).
            BlockPos elsewhere = c.sales.east(3);
            level.setBlock(elsewhere, ShopsRegistry.SALES_BLOCK.defaultBlockState(), 3);
            level.setBlock(elsewhere.above(), ShopsRegistry.DISPLAY_CASE.defaultBlockState(), 3);
            CompoundTag salesTag = c.sales().saveWithoutMetadata(registries);
            CompoundTag caseTag = c.box().saveWithoutMetadata(registries);
            level.setBlockEntity(BlockEntity.loadStatic(elsewhere, level.getBlockState(elsewhere), withId(salesTag, "simpleshops:sales_block"), registries));
            level.setBlockEntity(BlockEntity.loadStatic(elsewhere.above(), level.getBlockState(elsewhere.above()), withId(caseTag, "simpleshops:display_case"), registries));
            SalesBlockEntity copy = (SalesBlockEntity) level.getBlockEntity(elsewhere);
            DisplayCaseBlockEntity copyCase = (DisplayCaseBlockEntity) level.getBlockEntity(elsewhere.above());
            h.assertTrue(copy.vault() != null && copy.vault() != c.sales().vault() && copyCase.vault() != c.box().vault(), "the copy gets its own entry");
            h.assertTrue(copy.earnings().stream().allMatch(ItemStack::isEmpty) && copyCase.goods().stream().allMatch(ItemStack::isEmpty), "and it is empty");
            h.assertTrue(count(c.box().goods(), Items.DIAMOND) == 5 && count(c.sales().earnings(), Items.EMERALD) == 8, "the original keeps its items");
            // The pick-block data (client part) never carries the entry id or the earnings.
            CompoundTag custom = c.sales().saveCustomOnly(registries);
            h.assertTrue(!custom.contains("ender_id") && !custom.contains("earnings"), "item data has no id and no earnings");
            h.succeed();
        });
        all.put("ender_break_deletes_entry", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            StorageTests.Column c = StorageTests.column(h, owner, 1);
            c.box().goods().set(0, new ItemStack(Items.DIAMOND, 2));
            c.sales().earnings().set(0, new ItemStack(Items.EMERALD, 3));
            c.storages().get(0).earnings().set(0, new ItemStack(Items.COAL, 4));
            EnderShops data = EnderShops.get(h.getLevel().getServer());
            String s = c.sales().vault().id, b = c.box().vault().id, t = c.storages().get(0).vault().id;
            BlockPos storage = c.storagePos(0);
            h.assertTrue(owner.gameMode.destroyBlock(c.box) && owner.gameMode.destroyBlock(c.sales) && owner.gameMode.destroyBlock(storage), "owner breaks all three");
            h.assertTrue(data.find(s) == null && data.find(b) == null && data.find(t) == null, "entries deleted");
            int d = 0, e = 0, k = 0;
            for (ItemEntity it : h.getLevel().getEntitiesOfClass(ItemEntity.class, AABB.ofSize(Vec3.atCenterOf(c.sales), 8, 8, 8))) {
                if (it.getItem().is(Items.DIAMOND)) d += it.getItem().getCount();
                if (it.getItem().is(Items.EMERALD)) e += it.getItem().getCount();
                if (it.getItem().is(Items.COAL)) k += it.getItem().getCount();
            }
            h.assertTrue(d == 2 && e == 3 && k == 4, "dropped exactly once: " + d + "/" + e + "/" + k);
            h.succeed();
        });
        all.put("ender_orphan_kept_and_cleared", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            StorageTests.Column c = StorageTests.column(h, owner, 0);
            c.sales().earnings().set(3, new ItemStack(Items.EMERALD, 6));
            c.box().goods().set(2, new ItemStack(Items.DIAMOND, 4));
            EnderShops data = EnderShops.get(h.getLevel().getServer());
            EnderShops.Vault sales = c.sales().vault(), box = c.box().vault();
            // Removed without block entity side effects (as some commands, structures and mods do): no drop, the entry stays.
            int flags = Block.UPDATE_ALL | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;
            h.getLevel().setBlock(c.box, Blocks.AIR.defaultBlockState(), flags);
            h.getLevel().setBlock(c.sales, Blocks.AIR.defaultBlockState(), flags);
            h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class, AABB.ofSize(Vec3.atCenterOf(c.sales), 8, 8, 8)).isEmpty(), "nothing dropped");
            h.assertTrue(data.find(sales.id) == sales && data.find(box.id) == box && count(sales.items, Items.EMERALD) == 6, "entries kept with their items");
            var orphans = data.orphans(h.getLevel().getServer());
            h.assertTrue(orphans.contains(sales) && orphans.contains(box), "listed as orphans");
            // A stranger's player shop entries are never handed out by a non-operator: the command tree needs level 2 (checked by the
            // registration); here the operator path runs directly.
            ServerPlayer op = player(h, GameType.SURVIVAL);
            op.getInventory().clearContent();
            try {
                ServerShopCommands.orphans(op.createCommandSourceStack(), false);
                ServerShopCommands.orphans(op.createCommandSourceStack(), true);
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                throw h.assertionException("command: " + e.getMessage());
            }
            h.assertTrue(data.find(sales.id) == null && data.find(box.id) == null, "cleared");
            h.assertTrue(ShopsTests.count(op.getInventory(), Items.EMERALD) >= 6 && ShopsTests.count(op.getInventory(), Items.DIAMOND) >= 4, "the items went to the operator");
            h.succeed();
        });
        all.put("ender_world_data_roundtrip", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            StorageTests.Column c = StorageTests.column(h, owner, 1);
            c.box().goods().set(0, new ItemStack(Items.DIAMOND, 7));
            c.sales().prices().set(2, new ItemStack(Items.EMERALD, 2));
            c.sales().earnings().set(5, new ItemStack(Items.GOLD_INGOT, 12));
            EnderShops.link(c.sales());
            EnderShops data = EnderShops.get(h.getLevel().getServer());
            EnderShops.Vault box = c.box().vault();
            // Goods on their way to a trade terminal are saved and come back on load.
            ItemStack reserved = box.items.get(0).split(3);
            box.transit().add(reserved);
            var ops = h.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
            Tag tag = EnderShops.CODEC.encodeStart(ops, data).getOrThrow();
            EnderShops back = EnderShops.CODEC.parse(ops, tag).getOrThrow();
            EnderShops.Vault s2 = back.find(c.sales().vault().id), b2 = back.find(box.id);
            h.assertTrue(s2 != null && b2 != null, "entries survive");
            h.assertTrue(count(s2.items, Items.GOLD_INGOT) == 12 && s2.prices.get(2).getCount() == 2 && s2.owner().equals(owner.getUUID()), "sales entry");
            h.assertTrue(s2.caseId().equals(box.id) && s2.storages().size() == 1, "links survive");
            h.assertTrue(count(b2.items, Items.DIAMOND) == 7 && b2.transit().isEmpty(), "reserved goods are back in the goods after a load: " + b2.items);
            box.transit().remove(reserved);
            box.items.get(0).grow(3);
            h.succeed();
        });
    }

    private static CompoundTag withOwner(CompoundTag tag, ServerPlayer owner) {
        tag.store("owner", net.minecraft.core.UUIDUtil.CODEC, owner.getUUID());
        tag.putString("owner_name", owner.getGameProfile().name());
        return tag;
    }

    private static CompoundTag withId(CompoundTag tag, String id) {
        tag.putString("id", id);
        return tag;
    }

    static int count(Iterable<ItemStack> list, Item item) {
        int n = 0;
        for (ItemStack s : list) if (s.is(item)) n += s.getCount();
        return n;
    }

    private EnderTests() {}
}

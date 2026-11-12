package com.simpleshops.test;

import static com.simpleshops.test.ShopsTests.player;
import static com.simpleshops.test.ShopsTests.shop;

import com.simpleshops.ShopsRegistry;
import com.simpleshops.block.SalesBlockEntity;
import com.simpleshops.config.ShopsConfig;
import com.simpleshops.logic.ShopLedger;
import com.simpleshops.logic.ShopMusic;
import com.simpleshops.menu.SalesOwnerMenu;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Shop music (a-shopmusic37, plan docs/ai/PLAN-A-SHOPMUSIC37-2026-11-12.md): the disc slot of the sales block, the owner's
 * right click, the client update (disc, start tick, reach - nothing else), the shared start tick of shops with the same disc,
 * the drop on removal, the server switch and the step per piece, so per disc side (a-shopmusic37b). Which shop a client hears is plain logic ({@code ShopMusicPickTest}, JUnit).
 * <b>What breaks these tests:</b> a slot that takes non-discs or more than one, a disc that does not reach the client update or
 * reaches it while the switch is off, a second shop with the same disc that does not start in step, a disc that vanishes with
 * the block, or a stranger who can put a disc in.
 */
final class MusicTests {
    private MusicTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> all) {
        all.put("shop_music_slot", h -> {
            ShopsConfig.reset();
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            ServerPlayer stranger = player(h, GameType.SURVIVAL);
            ShopsTests.Shop shop = shop(h, owner);
            SalesBlockEntity sales = shop.sales();
            SalesOwnerMenu menu = new SalesOwnerMenu(1, owner.getInventory(), sales, owner);
            owner.containerMenu = menu;
            h.assertTrue(menu.slots.size() == 5 + 27 + 1 + 36 && menu.discSlot() == SalesOwnerMenu.EARNINGS_END
                    && menu.inventoryStart() == SalesOwnerMenu.EARNINGS_END + 1, "price, earnings, disc slot, inventory");
            Slot slot = menu.slots.get(menu.discSlot());
            h.assertTrue(slot.x == SalesOwnerMenu.DISC_X && slot.y == SalesOwnerMenu.DISC_Y, "ninth column of the first row");
            h.assertTrue(slot.mayPlace(new ItemStack(Items.MUSIC_DISC_CAT)) && !slot.mayPlace(new ItemStack(Items.STICK))
                    && !slot.mayPlace(new ItemStack(Items.JUKEBOX)) && slot.getMaxStackSize() == 1, "only a music disc, one at a time");
            // Shift click (no display case in this menu): the disc goes from the hotbar into the slot and plays from now on. Every test
            // uses its own discs: parallel test arenas lie within the 64-block sync range of each other.
            owner.getInventory().setItem(0, new ItemStack(Items.MUSIC_DISC_WAIT));
            menu.quickMoveStack(owner, menu.inventoryStart() + 27);
            h.assertTrue(sales.disc().is(Items.MUSIC_DISC_WAIT) && owner.getInventory().getItem(0).isEmpty(), "shift click puts the disc in");
            h.assertTrue(sales.discStart() == h.getLevel().getGameTime(), "a lone shop starts now: " + sales.discStart());
            CompoundTag tag = sales.getUpdateTag(h.getLevel().registryAccess());
            h.assertTrue(tag.contains("disc") && tag.getLongOr("disc_start", -1L) == sales.discStart()
                    && tag.getIntOr("music_range", 0) == ShopsConfig.MUSIC_RANGE_DEFAULT, "the client gets disc, start and reach: " + tag);
            h.assertTrue(!tag.contains("earnings"), "and still no earnings");
            // Taken out: silence (no disc in the update).
            menu.quickMoveStack(owner, menu.discSlot());
            h.assertTrue(sales.disc().isEmpty() && ShopsTests.count(owner.getInventory(), Items.MUSIC_DISC_WAIT) == 1, "the disc comes back out");
            h.assertTrue(!sales.getUpdateTag(h.getLevel().registryAccess()).contains("disc"), "no disc, no music");
            owner.closeContainer();
            // Jukebox logic with the block: the owner's right click puts a disc in; strangers and a full slot do nothing.
            stranger.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.MUSIC_DISC_13));
            h.assertTrue(!ShopMusic.tryInsert(stranger, h.getLevel(), shop.sales, InteractionHand.MAIN_HAND) && sales.disc().isEmpty(), "a stranger cannot");
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.MUSIC_DISC_13));
            h.assertTrue(ShopMusic.tryInsert(owner, h.getLevel(), shop.sales, InteractionHand.MAIN_HAND) && sales.disc().is(Items.MUSIC_DISC_13)
                    && owner.getMainHandItem().isEmpty(), "the owner's right click puts the disc in");
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.MUSIC_DISC_CAT));
            h.assertTrue(!ShopMusic.tryInsert(owner, h.getLevel(), shop.sales, InteractionHand.MAIN_HAND) && sales.disc().is(Items.MUSIC_DISC_13), "a full slot keeps its disc");
            h.succeed();
        });
        all.put("shop_music_sync_start", h -> {
            ShopsConfig.reset();
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            ShopsTests.Shop first = shop(h, owner);
            first.sales().setDisc(new ItemStack(Items.MUSIC_DISC_MELLOHI));
            long start = first.sales().discStart();
            h.runAfterDelay(7, () -> {
                SalesBlockEntity same = extra(h, owner, new BlockPos(3, 1, 3));
                SalesBlockEntity other = extra(h, owner, new BlockPos(11, 1, 11));
                same.setDisc(new ItemStack(Items.MUSIC_DISC_MELLOHI));
                other.setDisc(new ItemStack(Items.MUSIC_DISC_WARD));
                long now = h.getLevel().getGameTime();
                h.assertTrue(now > start, "time went on");
                h.assertTrue(same.discStart() == start, "same disc nearby: in step with the first shop (" + same.discStart() + " vs " + start + ")");
                h.assertTrue(other.discStart() == now, "another disc starts now");
                // Out and in again: still in step; a different disc in the first shop starts anew.
                same.setDisc(ItemStack.EMPTY);
                same.setDisc(new ItemStack(Items.MUSIC_DISC_MELLOHI));
                h.assertTrue(same.discStart() == start, "re-inserted: in step again");
                h.assertTrue(ShopMusic.song(new ItemStack(Items.MUSIC_DISC_CAT)).equals("minecraft:music_disc.cat")
                        && ShopMusic.seconds(new ItemStack(Items.MUSIC_DISC_CAT)) > 100.0F, "song id and length come from the disc's jukebox song");
                h.succeed();
            });
        });
        // a-shopmusic37b: shops play in step per piece (song), not per item - so the two sides of a disc (Simple Music: one item per
        // side) are two pieces. Here with Vanilla discs: the same item carrying another song starts on its own, another item
        // carrying the same song joins in step.
        all.put("shop_music_per_side", h -> {
            ShopsConfig.reset();
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            ShopsTests.Shop first = shop(h, owner);
            first.sales().setDisc(new ItemStack(Items.MUSIC_DISC_FAR));
            long start = first.sales().discStart();
            h.runAfterDelay(5, () -> {
                var songs = h.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.JUKEBOX_SONG);
                ItemStack otherSide = new ItemStack(Items.MUSIC_DISC_FAR);
                otherSide.set(net.minecraft.core.component.DataComponents.JUKEBOX_PLAYABLE,
                        new net.minecraft.world.item.JukeboxPlayable(songs.getOrThrow(net.minecraft.world.item.JukeboxSongs.MALL)));
                ItemStack sameSong = new ItemStack(Items.MUSIC_DISC_STAL);
                sameSong.set(net.minecraft.core.component.DataComponents.JUKEBOX_PLAYABLE,
                        new net.minecraft.world.item.JukeboxPlayable(songs.getOrThrow(net.minecraft.world.item.JukeboxSongs.FAR)));
                SalesBlockEntity flipped = extra(h, owner, new BlockPos(3, 1, 3));
                SalesBlockEntity joined = extra(h, owner, new BlockPos(11, 1, 11));
                flipped.setDisc(otherSide);
                joined.setDisc(sameSong);
                long now = h.getLevel().getGameTime();
                h.assertTrue(now > start, "time went on");
                h.assertTrue("minecraft:music_disc.mall".equals(ShopMusic.song(otherSide)) && flipped.discStart() == now,
                        "same item, other song (the other side): its own start (" + flipped.discStart() + ")");
                h.assertTrue("minecraft:music_disc.far".equals(ShopMusic.song(sameSong)) && joined.discStart() == start,
                        "other item, same song: in step with the first shop (" + joined.discStart() + " vs " + start + ")");
                h.succeed();
            });
        });
        all.put("shop_music_drops_disc", h -> {
            ShopsConfig.reset();
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            ShopsTests.Shop shop = shop(h, owner);
            shop.sales().setDisc(new ItemStack(Items.MUSIC_DISC_PIGSTEP));
            h.assertTrue(owner.gameMode.destroyBlock(shop.box) && owner.gameMode.destroyBlock(shop.sales), "owner breaks the shop");
            int discs = 0;
            for (ItemEntity e : h.getLevel().getEntitiesOfClass(ItemEntity.class, AABB.ofSize(Vec3.atCenterOf(shop.sales), 6, 6, 6))) {
                if (e.getItem().is(Items.MUSIC_DISC_PIGSTEP)) discs += e.getItem().getCount();
            }
            h.assertTrue(discs == 1, "the disc drops exactly once: " + discs);
            h.succeed();
        });
        all.put("shop_music_switch_off", h -> {
            ShopsConfig.reset();
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            ShopsTests.Shop shop = shop(h, owner);
            shop.sales().setDisc(new ItemStack(Items.MUSIC_DISC_CAT));
            try {
                ShopsConfig.shopMusic = false;
                h.assertTrue(!shop.sales().getUpdateTag(h.getLevel().registryAccess()).contains("disc"), "switch off: clients get no disc, nobody hears it");
                SalesOwnerMenu menu = new SalesOwnerMenu(1, owner.getInventory(), shop.sales(), owner);
                owner.containerMenu = menu;
                h.assertTrue(!menu.slots.get(menu.discSlot()).mayPlace(new ItemStack(Items.MUSIC_DISC_13)), "switch off: the slot takes nothing");
                h.assertTrue(menu.slots.get(menu.discSlot()).mayPickup(owner), "but the disc can be taken out");
                owner.closeContainer();
                owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.MUSIC_DISC_13));
                shop.sales().setDisc(ItemStack.EMPTY);
                h.assertTrue(!ShopMusic.tryInsert(owner, h.getLevel(), shop.sales, InteractionHand.MAIN_HAND), "switch off: no right click insert");
                ShopsConfig.shopMusicRange = 999;
                ShopsConfig.apply(ShopsConfig.toJson());
                h.assertTrue(ShopsConfig.shopMusicRange == ShopsConfig.MUSIC_RANGE_MAX, "reach is clamped");
            } finally {
                ShopsConfig.reset();
            }
            h.assertTrue(ShopsConfig.shopMusic && ShopsConfig.shopMusicRange == 24, "defaults: on, 24 blocks");
            h.succeed();
        });
    }

    /** One more sales block (without a case) on the arena floor at {@code rel}, owned by {@code owner}. */
    private static SalesBlockEntity extra(GameTestHelper h, ServerPlayer owner, BlockPos rel) {
        BlockPos pos = h.absolutePos(rel);
        h.getLevel().setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        h.getLevel().setBlock(pos, ShopsRegistry.SALES_BLOCK.defaultBlockState(), 3);
        SalesBlockEntity sales = (SalesBlockEntity) h.getLevel().getBlockEntity(pos);
        sales.initFor(owner);
        ShopLedger.record(h.getLevel(), pos, owner.getUUID(), true);
        return sales;
    }
}

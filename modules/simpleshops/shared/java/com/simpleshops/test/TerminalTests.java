package com.simpleshops.test;

import static com.simpleshops.test.ShopsTests.count;
import static com.simpleshops.test.ShopsTests.player;

import com.simpleshops.ShopsRegistry;
import com.simpleshops.block.SalesBlockEntity;
import com.simpleshops.config.ShopsConfig;
import com.simpleshops.logic.EnderShops;
import com.simpleshops.logic.ShopActions;
import com.simpleshops.logic.ShopGuard;
import com.simpleshops.logic.TradeTerminal;
import com.simpleshops.menu.ShopBuyerMenu;
import com.simpleshops.menu.TradeTerminalMenu;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.Direction;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Shops rework stages 3 and 4 (KONZ20 S2 W2, S3 d, S5 k): the trade terminal. Remote buying books exactly like buying on site,
 * never loads a chunk, shares the one-player lock with the shop on site, respects private shops and the dimension rule, is rate
 * limited and refuses stale or foreign requests; goods only go out (with a delivery time, back into the shop when the delivery
 * fails); earnings are collected only into the inventory; random clicks never create or destroy an item.
 */
final class TerminalTests {
    static final BlockPos TERMINAL = new BlockPos(4, 1, 4);

    static void register(Map<String, Consumer<GameTestHelper>> all) {
        all.put("terminal_registered", h -> {
            h.assertTrue(ShopsRegistry.TRADE_TERMINAL != null && ShopsRegistry.TERMINAL_MENU != null, "block and menu");
            var recipe = h.getLevel().getServer().getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                    com.simpleshops.SimpleShops.id("trade_terminal")));
            h.assertTrue(recipe.isPresent() && recipe.get().value().placementInfo().ingredients().stream().anyMatch(i -> i.test(new ItemStack(Items.ENDER_EYE))), "recipe with an ender eye");
            h.assertTrue(!ShopGuard.isShop(ShopsRegistry.TRADE_TERMINAL.defaultBlockState()) && ShopsRegistry.TRADE_TERMINAL.getExplosionResistance() < 10.0F,
                    "an ordinary block: it holds nothing");
            ShopsConfig.reset();
            h.assertTrue(ShopsConfig.remoteTake() && ShopsConfig.remoteDelaySeconds == 2 && !ShopsConfig.remoteCrossDimension, "KONZ20 defaults");
            h.succeed();
        });
        all.put("terminal_drops_lectern_without_silk_touch", h -> {
            var level = h.getLevel();
            BlockPos pos = h.absolutePos(new BlockPos(2, 1, 2));
            BlockState state = ShopsRegistry.TRADE_TERMINAL.defaultBlockState();
            ItemStack silk = new ItemStack(Items.IRON_PICKAXE);
            silk.enchant(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
            h.assertTrue(dropsOnly(Block.getDrops(state, level, pos, null, null, silk), ShopsRegistry.TRADE_TERMINAL_ITEM), "silk touch keeps the terminal");
            var axeDrops = Block.getDrops(state, level, pos, null, null, new ItemStack(Items.IRON_AXE));
            h.assertTrue(dropsOnly(axeDrops, Items.LECTERN), "an axe yields only a lectern, got " + axeDrops);
            h.assertTrue(dropsOnly(Block.getDrops(state, level, pos, null, null, ItemStack.EMPTY), Items.LECTERN), "bare hands yield only a lectern");
            h.succeed();
        });
        all.put("shops_recipes_na24", h -> {
            var recipes = h.getLevel().getServer().getRecipeManager();
            h.assertTrue(recipeHas(recipes, "sales_block", Items.HOPPER, Items.ENDER_PEARL, Items.CHEST), "sales block: hopper on top");
            h.assertTrue(recipeHas(recipes, "sales_storage", Items.HOPPER, Items.ENDER_PEARL), "sales storage: hoppers");
            h.assertTrue(recipeHas(recipes, "display_case", Items.OAK_PRESSURE_PLATE, Items.GLASS, Items.IRON_NUGGET), "display case: plate, glass block, nuggets");
            h.succeed();
        });
        all.put("display_tint_red", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            ServerPlayer stranger = player(h, GameType.SURVIVAL);
            StorageTests.Column c = StorageTests.column(h, owner, 0);
            var level = h.getLevel();
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(c.box), Direction.UP, c.box, false);
            ItemStack dye = new ItemStack(Items.DYE.red(), 2);
            stranger.setShiftKeyDown(true);
            stranger.gameMode.useItemOn(stranger, level, dye, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
            h.assertTrue(level.getBlockState(c.box).getValue(com.simpleshops.block.DisplayCaseBlock.TINT) == com.simpleshops.block.DisplayCaseBlock.Tint.NONE && dye.getCount() == 2, "a stranger cannot tint");
            owner.setShiftKeyDown(false);
            owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, dye);
            owner.gameMode.useItemOn(owner, level, dye, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
            h.assertTrue(level.getBlockState(c.box).getValue(com.simpleshops.block.DisplayCaseBlock.TINT) == com.simpleshops.block.DisplayCaseBlock.Tint.NONE, "without sneaking the dye is goods");
            h.assertTrue(c.box().goods().stream().anyMatch(g -> g.is(Items.DYE.red())), "dye stocked as goods");
            c.box().goods().clear();
            ItemStack dye2 = new ItemStack(Items.DYE.red(), 2);
            owner.setShiftKeyDown(true);
            owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, dye2);
            owner.gameMode.useItemOn(owner, level, dye2, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
            h.assertTrue(level.getBlockState(c.box).getValue(com.simpleshops.block.DisplayCaseBlock.TINT) == com.simpleshops.block.DisplayCaseBlock.Tint.RED && dye2.getCount() == 1, "owner tints, one dye used");
            h.assertTrue(level.getBlockEntity(c.box) instanceof com.simpleshops.block.DisplayCaseBlockEntity, "the case keeps its entity");
            h.succeed();
        });
        all.put("terminal_remote_buy_equals_on_site", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            ServerPlayer remote = player(h, GameType.SURVIVAL);
            ServerPlayer local = player(h, GameType.SURVIVAL);
            StorageTests.Column c = StorageTests.column(h, owner, 0);
            c.box().goods().set(0, new ItemStack(Items.DIAMOND, 4));
            c.sales().prices().set(2, new ItemStack(Items.EMERALD, 2));
            EnderShops.link(c.sales());
            for (ServerPlayer p : List.of(remote, local)) {
                p.getInventory().clearContent();
                p.getInventory().setItem(0, new ItemStack(Items.EMERALD, 10));
            }
            // On site.
            ShopBuyerMenu onSite = StorageTests.buyOpen(h, local, c);
            onSite.pay().setItem(2, local.getInventory().removeItem(0, 2));
            h.assertTrue(ShopsTests.trade(onSite, local) == com.simpleshops.logic.ShopTrade.Result.OK, "on-site trade");
            local.closeContainer();
            // From the terminal.
            TradeTerminalMenu menu = open(h, remote);
            browse(menu, remote, owner.getGameProfile().name());
            h.assertTrue(select(menu, remote, c.sales().vault().id, 0), "offer listed and selected");
            menu.pay().setItem(2, remote.getInventory().removeItem(0, 2));
            menu.broadcastChanges();
            h.assertTrue(menu.canBuyClient(), "button on");
            menu.clickMenuButton(remote, TradeTerminalMenu.BUY | menu.revisionSeen());
            h.assertTrue(menu.lastStatus == TradeTerminalMenu.S_DONE, "remote trade: " + menu.lastStatus);
            h.assertTrue(count(remote.getInventory(), Items.DIAMOND) == count(local.getInventory(), Items.DIAMOND) && count(remote.getInventory(), Items.DIAMOND) == 1,
                    "the same piece");
            h.assertTrue(count(remote.getInventory(), Items.EMERALD) == count(local.getInventory(), Items.EMERALD), "the same price");
            h.assertTrue(c.box().goods().get(0).getCount() == 2 && EnderTests.count(c.sales().earnings(), Items.EMERALD) == 4, "both landed in the same shop");
            // Shift: up to 64 times, stops when the goods run out.
            menu.pay().setItem(2, remote.getInventory().removeItem(0, 8));
            menu.clickMenuButton(remote, TradeTerminalMenu.BUY_MAX | menu.revisionSeen());
            h.assertTrue(c.box().goods().get(0).isEmpty() && count(remote.getInventory(), Items.DIAMOND) == 3 && menu.pay().getItem(2).getCount() == 4, "bought the rest");
            remote.closeContainer();
            h.assertTrue(count(remote.getInventory(), Items.EMERALD) == 4, "pay slots returned on close");
            h.succeed();
        });
        all.put("terminal_never_loads_chunks", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            ServerPlayer buyer = player(h, GameType.SURVIVAL);
            ServerLevel level = h.getLevel();
            BlockPos far = new BlockPos(28_000_000 + (int) (level.getGameTime() % 1000) * 16, 70, 28_000_000);
            h.assertTrue(!level.hasChunkAt(far), "far chunk unloaded");
            EnderShops data = EnderShops.get(level.getServer());
            EnderShops.Vault sales = data.create(EnderShops.Kind.SALES, level, far);
            EnderShops.Vault box = data.create(EnderShops.Kind.CASE, level, far.above());
            sales.setOwner(owner.getUUID(), owner.getGameProfile().name());
            box.setOwner(owner.getUUID(), owner.getGameProfile().name());
            data.linkCase(sales, box);
            box.items.set(0, new ItemStack(Items.NAUTILUS_SHELL, 3));
            sales.prices.set(0, new ItemStack(Items.EMERALD, 1));
            buyer.getInventory().clearContent();
            buyer.getInventory().setItem(0, new ItemStack(Items.EMERALD, 5));
            TradeTerminalMenu menu = open(h, buyer);
            browse(menu, buyer, owner.getGameProfile().name());
            h.assertTrue(select(menu, buyer, sales.id, 0), "the far shop is listed");
            menu.pay().setItem(0, buyer.getInventory().removeItem(0, 1));
            menu.clickMenuButton(buyer, TradeTerminalMenu.BUY | menu.revisionSeen());
            h.assertTrue(menu.lastStatus == TradeTerminalMenu.S_DONE && count(buyer.getInventory(), Items.NAUTILUS_SHELL) == 1, "bought from the far shop");
            h.assertTrue(box.items.get(0).getCount() == 2 && EnderTests.count(sales.items, Items.EMERALD) == 1, "booked in the world data");
            h.assertTrue(!level.hasChunkAt(far) && !level.hasChunkAt(far.above()), "the far chunk was never loaded");
            buyer.closeContainer();
            data.delete(sales);
            data.delete(box);
            h.succeed();
        });
        all.put("map_shows_unloaded_public_shops", h -> {
            // KONZ20 stage 4: the map layer reads shops in unloaded chunks from the world data, public ones only, never loading them.
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            ServerLevel level = h.getLevel();
            BlockPos far = new BlockPos(27_000_000 + (int) (level.getGameTime() % 1000) * 16, 70, 27_000_000);
            h.assertTrue(!level.hasChunkAt(far), "far chunk unloaded");
            EnderShops data = EnderShops.get(level.getServer());
            EnderShops.Vault sales = data.create(EnderShops.Kind.SALES, level, far);
            EnderShops.Vault box = data.create(EnderShops.Kind.CASE, level, far.above());
            sales.setOwner(owner.getUUID(), owner.getGameProfile().name());
            box.setOwner(owner.getUUID(), owner.getGameProfile().name());
            data.linkCase(sales, box);
            sales.prices.set(0, new ItemStack(Items.EMERALD, 2));
            var layer = com.simplebuilding.framework.api.MapLayerSources.get("shops");
            h.assertTrue(layer != null, "shops map layer registered");
            java.util.function.Supplier<List<com.simplebuilding.framework.api.MapLayerSources.Marker>> markers =
                    () -> layer.collector().collect(level, far.getX() - 8, far.getZ() - 8, far.getX() + 8, far.getZ() + 8);
            h.assertTrue(markers.get().isEmpty(), "sold out: no marker");
            box.items.set(0, new ItemStack(Items.NAUTILUS_SHELL, 3));
            var seen = markers.get();
            h.assertTrue(seen.size() == 1 && seen.get(0).x() == far.getX() && seen.get(0).z() == far.getZ()
                    && seen.get(0).lines().get(0).contains(owner.getGameProfile().name()) && seen.get(0).lines().get(2).contains("2x"),
                    "public unloaded shop on the map: " + seen);
            sales.setListed(false);
            h.assertTrue(markers.get().isEmpty(), "private shop: no marker");
            sales.setListed(true);
            h.assertTrue(layer.collector().collect(level, far.getX() + 16, far.getZ(), far.getX() + 32, far.getZ() + 8).isEmpty(), "outside the rectangle");
            h.assertTrue(!level.hasChunkAt(far) && !level.hasChunkAt(far.above()), "the map loaded the far chunk");
            data.delete(sales);
            data.delete(box);
            h.succeed();
        });
        all.put("terminal_lock_shared_with_site", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            ServerPlayer remote = player(h, GameType.SURVIVAL);
            ServerPlayer local = player(h, GameType.SURVIVAL);
            StorageTests.Column c = StorageTests.column(h, owner, 0);
            c.box().goods().set(0, new ItemStack(Items.DIAMOND, 4));
            c.sales().prices().set(2, new ItemStack(Items.EMERALD, 2));
            EnderShops.link(c.sales());
            String id = c.sales().vault().id;
            TradeTerminalMenu menu = open(h, remote);
            browse(menu, remote, owner.getGameProfile().name());
            h.assertTrue(select(menu, remote, id, 0) && !c.sales().lockFree(), "remote selection holds the shop");
            local.setPos(Vec3.atCenterOf(c.sales).add(0, 0, -2));
            ShopActions.useSales(local, h.getLevel(), c.sales);
            h.assertTrue(!(local.containerMenu instanceof ShopBuyerMenu), "on site: busy");
            remote.closeContainer();
            h.assertTrue(c.sales().lockFree(), "closing the terminal frees it");
            ShopActions.useSales(local, h.getLevel(), c.sales);
            h.assertTrue(local.containerMenu instanceof ShopBuyerMenu, "on site again");
            TradeTerminalMenu second = open(h, remote);
            browse(second, remote, owner.getGameProfile().name());
            h.assertTrue(!select(second, remote, id, 0) && second.lastStatus == TradeTerminalMenu.S_BUSY && second.selectedShopForTest() == null, "terminal waits for the site");
            // A remote buy without the lock is refused even if a client insists.
            second.pay().setItem(2, new ItemStack(Items.EMERALD, 2));
            second.clickMenuButton(remote, TradeTerminalMenu.BUY | 0);
            h.assertTrue(c.box().goods().get(0).getCount() == 4, "nothing sold without the lock");
            remote.closeContainer();
            local.closeContainer();
            h.succeed();
        });
        all.put("terminal_take_delivery", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            StorageTests.Column c = StorageTests.column(h, owner, 0);
            c.box().goods().set(0, new ItemStack(Items.DIAMOND, 5));
            c.box().goods().set(1, new ItemStack(Items.GOLD_INGOT, 7));
            c.sales().prices().set(2, new ItemStack(Items.EMERALD, 2));
            EnderShops.link(c.sales());
            EnderShops.Vault sales = c.sales().vault(), box = c.box().vault();
            owner.getInventory().clearContent();
            TradeTerminalMenu menu = open(h, owner);
            h.assertTrue(select(menu, owner, sales.id, -1), "own shop selected");
            // Private: no take.
            sales.setListed(false);
            menu.clicked(TradeTerminalMenu.ROW_A, 0, ContainerInput.PICKUP, owner);
            h.assertTrue(menu.lastStatus == TradeTerminalMenu.S_PRIVATE && box.items.get(0).getCount() == 5, "private shop: no take from afar");
            sales.setListed(true);
            // Config none: look only.
            ShopsConfig.remoteStock = "none";
            menu.clicked(TradeTerminalMenu.ROW_A, 0, ContainerInput.PICKUP, owner);
            h.assertTrue(menu.lastStatus == TradeTerminalMenu.S_OFF && box.items.get(0).getCount() == 5, "remoteStock none");
            ShopsConfig.reset();
            // Order: reserved at once, delivered after 2 s.
            menu.clicked(TradeTerminalMenu.ROW_A, 0, ContainerInput.PICKUP, owner);
            h.assertTrue(menu.lastStatus == TradeTerminalMenu.S_DELIVERING && box.items.get(0).isEmpty() && box.transit().size() == 1, "reserved");
            h.assertTrue(count(owner.getInventory(), Items.DIAMOND) == 0, "not yet delivered");
            // Putting goods in from afar is impossible: row clicks with an item on the cursor change nothing.
            menu.setCarried(new ItemStack(Items.STICK, 3));
            for (int s = TradeTerminalMenu.ROW_A; s < TradeTerminalMenu.PAY; s++) for (ContainerInput in : ContainerInput.values()) menu.clicked(s, 0, in, owner);
            h.assertTrue(box.items.stream().noneMatch(st -> st.is(Items.STICK)) && menu.getCarried().is(Items.STICK), "no stick went into the shop");
            menu.setCarried(ItemStack.EMPTY);
            h.runAfterDelay(30, () -> {
                menu.broadcastChanges();
                h.assertTrue(count(owner.getInventory(), Items.DIAMOND) == 0, "1.5 s: still on the way");
            });
            h.runAfterDelay(45, () -> {
                menu.broadcastChanges();
                h.assertTrue(count(owner.getInventory(), Items.DIAMOND) == 5 && box.transit().isEmpty(), "delivered after 2 s");
                // A second order, then the terminal closes before the delivery: the goods go back into the shop.
                menu.clicked(TradeTerminalMenu.ROW_A + 1, 0, ContainerInput.PICKUP, owner);
                h.assertTrue(box.items.get(1).isEmpty() && box.transit().size() == 1, "second order reserved");
                owner.closeContainer();
                menu.removed(owner);
                h.assertTrue(box.transit().isEmpty() && box.items.stream().anyMatch(st -> st.is(Items.GOLD_INGOT) && st.getCount() == 7), "back in the shop");
                h.assertTrue(count(owner.getInventory(), Items.GOLD_INGOT) == 0 && c.sales().lockFree(), "not delivered, lock free");
                // No room: no order.
                TradeTerminalMenu again = open(h, owner);
                select(again, owner, sales.id, -1);
                for (int i = 0; i < 36; i++) owner.getInventory().setItem(i, new ItemStack(Items.STONE_SWORD));
                int slot = box.items.get(0).isEmpty() ? 1 : 0;
                again.clicked(TradeTerminalMenu.ROW_A + slot, 0, ContainerInput.PICKUP, owner);
                h.assertTrue(again.lastStatus == TradeTerminalMenu.S_NO_SPACE && box.transit().isEmpty(), "full inventory: no delivery");
                owner.closeContainer();
                h.succeed();
            });
        });
        ShopsTests.MAX_TICKS.put("terminal_take_delivery", 120);
        all.put("terminal_private_and_dimension", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            ServerPlayer buyer = player(h, GameType.SURVIVAL);
            StorageTests.Column c = StorageTests.column(h, owner, 0);
            c.box().goods().set(0, new ItemStack(Items.DIAMOND, 4));
            c.sales().prices().set(2, new ItemStack(Items.EMERALD, 2));
            EnderShops.link(c.sales());
            EnderShops.Vault sales = c.sales().vault();
            EnderShops data = EnderShops.get(h.getLevel().getServer());
            String q = owner.getGameProfile().name();
            h.assertTrue(listed(data, buyer, q, sales.id), "public: listed");
            sales.setListed(false);
            h.assertTrue(!listed(data, buyer, q, sales.id), "private: not in the catalog");
            sales.setListed(true);
            // Other dimension: only with the server switch.
            ServerLevel nether = h.getLevel().getServer().getLevel(Level.NETHER);
            if (nether != null) {
                EnderShops.Vault ns = data.create(EnderShops.Kind.SALES, nether, new BlockPos(100, 70, 100));
                EnderShops.Vault nb = data.create(EnderShops.Kind.CASE, nether, new BlockPos(100, 71, 100));
                ns.setOwner(owner.getUUID(), owner.getGameProfile().name());
                nb.setOwner(owner.getUUID(), owner.getGameProfile().name());
                data.linkCase(ns, nb);
                nb.items.set(0, new ItemStack(Items.QUARTZ, 3));
                ns.prices.set(0, new ItemStack(Items.EMERALD));
                try {
                    h.assertTrue(!listed(data, buyer, q, ns.id) && TradeTerminal.mayBuy(buyer, ns) == TradeTerminal.Why.DIMENSION, "nether shop out of reach");
                    ShopsConfig.remoteCrossDimension = true;
                    h.assertTrue(listed(data, buyer, q, ns.id) && TradeTerminal.mayBuy(buyer, ns) == TradeTerminal.Why.OK, "with remoteCrossDimension");
                } finally {
                    ShopsConfig.reset();
                    data.delete(ns);
                    data.delete(nb);
                }
            }
            // Own shops: listed, not buyable.
            h.assertTrue(TradeTerminal.mayBuy(owner, sales) == TradeTerminal.Why.OWN, "own shop not buyable");
            h.succeed();
        });
        all.put("terminal_rate_limit_and_bad_requests", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            ServerPlayer buyer = player(h, GameType.SURVIVAL);
            ServerPlayer stranger = player(h, GameType.SURVIVAL);
            StorageTests.Column c = StorageTests.column(h, owner, 0);
            c.box().goods().set(0, new ItemStack(Items.DIAMOND, 4));
            c.sales().prices().set(2, new ItemStack(Items.EMERALD, 2));
            EnderShops.link(c.sales());
            TradeTerminalMenu menu = open(h, buyer);
            menu.clickMenuButton(buyer, TradeTerminalMenu.B_TAB_BROWSE);
            menu.clickMenuButton(buyer, TradeTerminalMenu.B_APPLY);
            h.assertTrue(menu.lastStatus == TradeTerminalMenu.S_SLOW, "two queries in one tick: the second is refused");
            // Query text is capped at 32 characters.
            menu.clickMenuButton(buyer, TradeTerminalMenu.B_CLEAR);
            for (int i = 0; i < 100; i++) menu.clickMenuButton(buyer, TradeTerminalMenu.CHAR + 'x');
            menu.coolDownForTest();
            h.assertTrue(menu.clickMenuButton(buyer, TradeTerminalMenu.B_APPLY) && menu.rowsForTest().isEmpty(), "capped query finds nothing");
            h.assertTrue(!menu.clickMenuButton(buyer, 99) && !menu.clickMenuButton(buyer, -5), "unknown buttons");
            h.assertTrue(!menu.clickMenuButton(stranger, TradeTerminalMenu.B_COLLECT), "someone else's menu");
            menu.coolDownForTest();
            browse(menu, buyer, owner.getGameProfile().name());
            select(menu, buyer, c.sales().vault().id, 0);
            buyer.getInventory().clearContent();
            menu.pay().setItem(2, new ItemStack(Items.EMERALD, 2));
            menu.clickMenuButton(buyer, TradeTerminalMenu.BUY | ((menu.revisionSeen() + 1) & 0xFFFF));
            h.assertTrue(menu.lastStatus == TradeTerminalMenu.S_STALE && c.box().goods().get(0).getCount() == 4, "stale revision refused");
            // A buyer cannot act on the owner's tab functions.
            menu.coolDownForTest();
            menu.clickMenuButton(buyer, TradeTerminalMenu.B_TAB_MINE);
            h.assertTrue(menu.rowsForTest().isEmpty(), "the buyer has no shops");
            menu.clickMenuButton(buyer, TradeTerminalMenu.B_COLLECT);
            menu.clickMenuButton(buyer, TradeTerminalMenu.B_PUBLIC);
            h.assertTrue(c.sales().vault().listed(), "nothing happened to the owner's shop");
            // Idle: the server closes the terminal.
            menu.ageForTest(ShopsConfig.idleTimeoutSeconds * 20L + 20);
            h.assertTrue(!menu.stillValid(buyer), "idle timeout");
            buyer.closeContainer();
            h.assertTrue(count(buyer.getInventory(), Items.EMERALD) == 2, "pay slots returned");
            h.succeed();
        });
        all.put("terminal_collect_and_prices", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            StorageTests.Column c = StorageTests.column(h, owner, 1);
            c.sales().prices().set(2, new ItemStack(Items.EMERALD, 2));
            c.sales().earnings().set(0, new ItemStack(Items.EMERALD, 30));
            c.storages().get(0).earnings().set(0, new ItemStack(Items.GOLD_INGOT, 9));
            EnderShops.link(c.sales());
            EnderShops.Vault sales = c.sales().vault();
            owner.getInventory().clearContent();
            for (int i = 1; i < 36; i++) owner.getInventory().setItem(i, new ItemStack(Items.STONE_SWORD));
            TradeTerminalMenu menu = open(h, owner);
            h.assertTrue(select(menu, owner, sales.id, -1), "own shop");
            menu.clickMenuButton(owner, TradeTerminalMenu.B_COLLECT);
            // One free slot: 30 emeralds fit, the gold stays (nothing drops, nothing is lost).
            h.assertTrue(count(owner.getInventory(), Items.EMERALD) == 30 && EnderTests.count(c.storages().get(0).earnings(), Items.GOLD_INGOT) == 9, "only what fits");
            owner.getInventory().setItem(5, ItemStack.EMPTY);
            menu.clickMenuButton(owner, TradeTerminalMenu.B_COLLECT);
            h.assertTrue(count(owner.getInventory(), Items.GOLD_INGOT) == 9 && c.storages().get(0).earnings().stream().allMatch(ItemStack::isEmpty), "the storage too");
            h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class, AABB.ofSize(owner.position(), 8, 8, 8)).isEmpty(), "nothing dropped");
            // Price templates: the same clicks as at the sales block.
            menu.setCarried(new ItemStack(Items.DIAMOND, 3));
            menu.clicked(TradeTerminalMenu.ROW_B + 3, 0, ContainerInput.PICKUP, owner);
            h.assertTrue(sales.prices.get(3).is(Items.DIAMOND) && menu.getCarried().getCount() == 3, "template set, cursor kept");
            menu.setCarried(ItemStack.EMPTY);
            menu.clickMenuButton(owner, TradeTerminalMenu.B_PUBLIC);
            h.assertTrue(!sales.listed(), "now private");
            owner.closeContainer();
            h.succeed();
        });
        all.put("terminal_dupe_fuzz", h -> {
            for (long seed = 1; seed <= 6; seed++) fuzz(h, seed);
            h.succeed();
        });
        ShopsTests.MAX_TICKS.put("terminal_dupe_fuzz", 400);
    }

    private static boolean dropsOnly(List<ItemStack> drops, Item item) {
        return drops.size() == 1 && drops.get(0).is(item) && drops.get(0).getCount() == 1;
    }

    private static boolean recipeHas(net.minecraft.world.item.crafting.RecipeManager recipes, String name, Item... items) {
        var r = recipes.byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, com.simpleshops.SimpleShops.id(name)));
        if (r.isEmpty()) return false;
        var ings = r.get().value().placementInfo().ingredients();
        for (Item item : items) if (ings.stream().noneMatch(i -> i.test(new ItemStack(item)))) return false;
        return true;
    }

    static TradeTerminalMenu open(GameTestHelper h, ServerPlayer p) {
        BlockPos at = h.absolutePos(TERMINAL);
        if (!h.getLevel().getBlockState(at).is(ShopsRegistry.TRADE_TERMINAL)) h.getLevel().setBlock(at, ShopsRegistry.TRADE_TERMINAL.defaultBlockState(), 3);
        p.setPos(Vec3.atCenterOf(at).add(0, 0, 2));
        TradeTerminalMenu menu = new TradeTerminalMenu(9, p.getInventory(), ContainerLevelAccess.create(h.getLevel(), at), p);
        p.containerMenu = menu;
        return menu;
    }

    /** Browse tab with a search text, through the real buttons (rate limit stepped over). */
    static void browse(TradeTerminalMenu menu, ServerPlayer p, String text) {
        menu.coolDownForTest();
        menu.clickMenuButton(p, TradeTerminalMenu.B_TAB_BROWSE);
        menu.clickMenuButton(p, TradeTerminalMenu.B_CLEAR);
        text.codePoints().forEach(cp -> menu.clickMenuButton(p, TradeTerminalMenu.CHAR + cp));
        menu.coolDownForTest();
        menu.clickMenuButton(p, TradeTerminalMenu.B_APPLY);
    }

    /** Clicks the row of {@code shop}/{@code ware} (pages forward as needed); true when it is selected afterwards. */
    static boolean select(TradeTerminalMenu menu, ServerPlayer p, String shop, int ware) {
        for (int guard = 0; guard < 50; guard++) {
            List<TradeTerminal.Row> rows = menu.rowsForTest();
            for (int i = 0; i < rows.size(); i++) {
                TradeTerminal.Row r = rows.get(i);
                if (!r.shop().equals(shop) || r.ware() != ware) continue;
                int pageOf = i / TradeTerminalMenu.LIST;
                while (menu.page() < pageOf) {
                    menu.coolDownForTest();
                    menu.clickMenuButton(p, TradeTerminalMenu.B_NEXT);
                }
                menu.clicked(i % TradeTerminalMenu.LIST, 0, ContainerInput.PICKUP, p);
                menu.broadcastChanges();
                return shop.equals(menu.selectedShopForTest());
            }
            return false;
        }
        return false;
    }

    private static boolean listed(EnderShops data, ServerPlayer p, String q, String id) {
        return TradeTerminal.browse(data, p, q, false, TradeTerminal.SORT_PRICE).stream().anyMatch(r -> r.shop().equals(id));
    }

    private static final Item[] POOL = {Items.EMERALD, Items.DIAMOND, Items.GOLD_INGOT, Items.COAL};

    private static Map<Item, Integer> universe(GameTestHelper h, List<ServerPlayer> players, List<TradeTerminalMenu> menus, StorageTests.Column c,
                                               EnderShops.Vault sales, EnderShops.Vault box) {
        Map<Item, Integer> out = new LinkedHashMap<>();
        for (Item it : POOL) out.put(it, 0);
        Consumer<ItemStack> add = s -> {
            if (!s.isEmpty() && out.containsKey(s.getItem())) out.merge(s.getItem(), s.getCount(), Integer::sum);
        };
        for (ServerPlayer p : players) for (int i = 0; i < p.getInventory().getContainerSize(); i++) add.accept(p.getInventory().getItem(i));
        for (TradeTerminalMenu m : menus) {
            add.accept(m.getCarried());
            for (int i = 0; i < 5; i++) add.accept(m.pay().getItem(i));
        }
        sales.items.forEach(add);
        box.items.forEach(add);
        box.transit().forEach(add);
        for (EnderShops.Vault v : List.of(c.storages().get(0).vault())) v.items.forEach(add);
        for (ItemEntity e : h.getLevel().getEntitiesOfClass(ItemEntity.class, AABB.ofSize(Vec3.atCenterOf(c.sales), 40, 40, 40))) add.accept(e.getItem());
        return out;
    }

    private static void fuzz(GameTestHelper h, long seed) {
        Random rnd = new Random(seed);
        ServerPlayer owner = player(h, GameType.SURVIVAL);
        ServerPlayer buyer = player(h, GameType.SURVIVAL);
        StorageTests.Column c = StorageTests.column(h, owner, 1);
        c.box().goods().set(0, new ItemStack(Items.DIAMOND, 12));
        c.box().goods().set(1, new ItemStack(Items.GOLD_INGOT, 8));
        c.sales().prices().set(2, new ItemStack(Items.EMERALD, 2));
        c.sales().prices().set(3, new ItemStack(Items.COAL, 1));
        c.sales().earnings().set(0, new ItemStack(Items.EMERALD, 6));
        EnderShops.link(c.sales());
        EnderShops.Vault sales = c.sales().vault(), box = c.box().vault();
        for (ServerPlayer p : List.of(owner, buyer)) p.getInventory().clearContent();
        buyer.getInventory().setItem(0, new ItemStack(Items.EMERALD, 40));
        buyer.getInventory().setItem(1, new ItemStack(Items.COAL, 20));
        TradeTerminalMenu bm = open(h, buyer), om = open(h, owner);
        List<ServerPlayer> players = List.of(owner, buyer);
        List<TradeTerminalMenu> menus = List.of(bm, om);
        Map<Item, Integer> start = universe(h, players, menus, c, sales, box);
        for (int step = 0; step < 120; step++) {
            int op = rnd.nextInt(10);
            TradeTerminalMenu m = rnd.nextBoolean() ? bm : om;
            ServerPlayer p = m == bm ? buyer : owner;
            try {
                switch (op) {
                    case 0 -> {
                        browse(m, p, owner.getGameProfile().name());
                        select(m, p, sales.id, rnd.nextInt(2));
                    }
                    case 1 -> {
                        m.coolDownForTest();
                        m.clickMenuButton(p, TradeTerminalMenu.B_TAB_MINE);
                        select(m, p, sales.id, -1);
                    }
                    case 2 -> m.clickMenuButton(p, (rnd.nextBoolean() ? TradeTerminalMenu.BUY : TradeTerminalMenu.BUY_MAX)
                            | (rnd.nextInt(6) == 0 ? rnd.nextInt(65536) : m.revisionSeen()));
                    case 3 -> m.clickMenuButton(p, TradeTerminalMenu.B_COLLECT);
                    case 4 -> m.clicked(TradeTerminalMenu.ROW_A + rnd.nextInt(5), 0, ContainerInput.PICKUP, p);
                    case 5 -> m.clicked(TradeTerminalMenu.INVENTORY_START + rnd.nextInt(36), 0, ContainerInput.QUICK_MOVE, p);
                    case 6 -> m.clicked(TradeTerminalMenu.PAY + rnd.nextInt(5), rnd.nextInt(2), ContainerInput.values()[rnd.nextInt(ContainerInput.values().length - 1)], p);
                    case 7 -> m.clicked(rnd.nextInt(TradeTerminalMenu.INVENTORY_START + 36), rnd.nextInt(2), ContainerInput.PICKUP, p);
                    case 8 -> m.broadcastChanges();
                    default -> m.clicked(rnd.nextInt(TradeTerminalMenu.PAY), rnd.nextInt(9), ContainerInput.values()[rnd.nextInt(ContainerInput.values().length)], p);
                }
            } catch (RuntimeException e) {
                throw h.assertionException("seed " + seed + " step " + step + " op " + op + " threw " + e);
            }
            Map<Item, Integer> now = universe(h, players, menus, c, sales, box);
            h.assertTrue(now.equals(start), "seed " + seed + " step " + step + " op " + op + ": " + start + " -> " + now);
        }
        // Closing returns pay slots and pending deliveries; the totals stay.
        for (TradeTerminalMenu m : menus) {
            ServerPlayer p = m == bm ? buyer : owner;
            p.containerMenu = m;
            m.removed(p);
            p.containerMenu = p.inventoryMenu;
            if (!m.getCarried().isEmpty()) {
                p.getInventory().add(m.getCarried());
                m.setCarried(ItemStack.EMPTY);
            }
        }
        Map<Item, Integer> end = universe(h, players, List.of(), c, sales, box);
        h.assertTrue(end.equals(start), "seed " + seed + " after closing: " + start + " -> " + end);
        h.assertTrue(sales.lockFree(), "lock free at the end");
    }

    private TerminalTests() {}
}

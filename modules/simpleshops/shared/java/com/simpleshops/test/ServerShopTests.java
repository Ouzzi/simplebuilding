package com.simpleshops.test;

import static com.simpleshops.test.ShopsTests.buy;
import static com.simpleshops.test.ShopsTests.count;
import static com.simpleshops.test.ShopsTests.place;
import static com.simpleshops.test.ShopsTests.player;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.simplebuilding.framework.api.Protection;
import com.simpleshops.ShopsRegistry;
import com.simpleshops.block.DisplayCaseBlockEntity;
import com.simpleshops.block.SalesBlockEntity;
import com.simpleshops.config.ShopsConfig;
import com.simpleshops.logic.PriceRow;
import com.simpleshops.logic.ServerShops;
import com.simpleshops.logic.ShopActions;
import com.simpleshops.logic.ShopGuard;
import com.simpleshops.logic.ShopTrade;
import com.simpleshops.menu.SalesOwnerMenu;
import com.simpleshops.menu.ShopBuyerMenu;
import com.simpleshops.menu.StockMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Tests of the server (unlimited) shop: operator commands, rights, buying without a limit, protection, persistence. */
final class ServerShopTests {
    static void register(Map<String, Consumer<GameTestHelper>> all) {
        all.put("servershop_commands_rights", h -> {
            ServerPlayer op = op(h, true);
            ServerPlayer moderator = op(h, false);
            ServerPlayer stranger = player(h, GameType.SURVIVAL);
            BlockPos at = h.absolutePos(new BlockPos(3, 1, 3));
            h.getLevel().setBlock(h.absolutePos(new BlockPos(3, 0, 3)), Blocks.STONE.defaultBlockState(), 3);
            String where = at.getX() + " " + at.getY() + " " + at.getZ();
            // Only permission level 2 may use the command at all.
            for (ServerPlayer nobody : List.of(stranger, moderator)) {
                h.assertTrue(!runs(nobody, "servershop create " + where) && !runs(nobody, "servershop list") && !runs(nobody, "servershop cash")
                        && !runs(nobody, "servershop price s001 add minecraft:emerald") && !runs(nobody, "servershop goods s001 clear"), "no rights: " + nobody.getName().getString());
            }
            h.assertTrue(h.getLevel().getBlockState(at).isAir() && shopAt(h, at) == null, "a refused command creates nothing");
            h.assertTrue(runs(op, "servershop create " + where), "operator creates");
            ServerShops.Shop shop = shopAt(h, at);
            h.assertTrue(shop != null && shop.id().matches("s\\d{3,}"), "id like s001: " + shop);
            h.assertTrue(h.getLevel().getBlockState(at).is(ShopsRegistry.SALES_BLOCK) && h.getLevel().getBlockState(at.above()).is(ShopsRegistry.DISPLAY_CASE), "both blocks stand");
            SalesBlockEntity sales = (SalesBlockEntity) h.getLevel().getBlockEntity(at);
            DisplayCaseBlockEntity box = (DisplayCaseBlockEntity) h.getLevel().getBlockEntity(at.above());
            h.assertTrue(sales.isServerShop() && box.isServerShop() && sales.owner() == null && box.owner() == null && sales.serverId().equals(shop.id()), "flagged, no owner");
            // The same place is no longer free.
            h.assertTrue(!runs(op, "servershop create " + where), "occupied place refused");
            // Configure by command.
            String id = shop.id();
            h.assertTrue(runs(op, "servershop goods " + id + " add minecraft:diamond") && runs(op, "servershop goods " + id + " add minecraft:stick 3"), "goods");
            h.assertTrue(runs(op, "servershop price " + id + " add minecraft:emerald 4") && runs(op, "servershop price " + id + " add minecraft:coal 2"), "prices");
            h.assertTrue(box.goods().get(0).is(Items.DIAMOND) && box.goods().get(1).is(Items.STICK) && box.goods().get(1).getCount() == 3, "goods set");
            h.assertTrue(sales.prices().get(2).is(Items.EMERALD) && sales.prices().get(2).getCount() == 4 && sales.prices().get(3).is(Items.COAL) && PriceRow.valid(sales.prices()), "price run valid");
            h.assertTrue(!runs(op, "servershop price " + id + " add minecraft:air"), "air is no price");
            h.assertTrue(!runs(op, "servershop price " + id + " add minecraft:diamond_sword 5"), "above the stack size is refused");
            for (int i = 0; i < 3; i++) runs(op, "servershop price " + id + " add minecraft:gold_ingot");
            h.assertTrue(PriceRow.filled(sales.prices()) == 5 && PriceRow.valid(sales.prices()) && !runs(op, "servershop price " + id + " add minecraft:gold_ingot"), "five prices at most, run kept");
            h.assertTrue(runs(op, "servershop price " + id + " remove 1") && PriceRow.filled(sales.prices()) == 4 && PriceRow.valid(sales.prices()), "remove keeps the run");
            h.assertTrue(runs(op, "servershop price " + id + " clear") && PriceRow.filled(sales.prices()) == 0, "clear prices");
            h.assertTrue(runs(op, "servershop goods " + id + " remove 2") && box.goods().get(1).isEmpty() && !runs(op, "servershop goods " + id + " remove 2"), "remove goods");
            // list / info / tp / edit work for the operator.
            h.assertTrue(listed(op, "servershop list") >= 1 && listed(op, "servershop list coords") >= 1 && listed(op, "servershop list items") >= 1, "list modes");
            h.assertTrue(runs(op, "servershop info " + id) && runs(op, "servershop tp " + id), "info and tp");
            h.assertTrue(!runs(op, "servershop info s99999") && !runs(op, "servershop remove s99999"), "unknown id");
            op.setPos(Vec3.atCenterOf(at).add(0.0, 0.0, -3.0));
            h.assertTrue(runs(op, "servershop edit " + id + " price") && op.containerMenu instanceof SalesOwnerMenu, "edit prices opens the owner menu");
            op.closeContainer();
            op.setPos(Vec3.atCenterOf(at).add(0.0, 0.0, -3.0));
            h.assertTrue(runs(op, "servershop edit " + id + " goods") && op.containerMenu instanceof StockMenu, "edit goods opens the stock menu");
            op.closeContainer();
            // Ids are never reused: remove, create again on the same place.
            h.assertTrue(runs(op, "servershop remove " + id), "remove");
            h.assertTrue(h.getLevel().getBlockState(at).isAir() && h.getLevel().getBlockState(at.above()).isAir() && shopAt(h, at) == null
                    && ServerShops.get(h.getLevel().getServer()).find(id) == null, "both blocks and the entry are gone");
            h.assertTrue(runs(op, "servershop create " + where), "create again");
            ServerShops.Shop second = shopAt(h, at);
            h.assertTrue(second != null && !second.id().equals(id), "the id is not reused: " + id + " -> " + second);
            ServerShops.remove(h.getLevel().getServer(), second);
            h.succeed();
        });
        all.put("servershop_buy_unlimited", h -> {
            ServerPlayer op = op(h, true);
            ServerPlayer buyer = player(h, GameType.SURVIVAL);
            ServerPlayer second = player(h, GameType.SURVIVAL);
            BlockPos at = h.absolutePos(new BlockPos(7, 1, 7));
            h.getLevel().setBlock(h.absolutePos(new BlockPos(7, 0, 7)), Blocks.STONE.defaultBlockState(), 3);
            ServerShops.CreateResult made = ServerShops.create(h.getLevel(), at, Direction.NORTH);
            h.assertTrue(made.status() == ServerShops.Created.OK, "created " + made);
            SalesBlockEntity sales = (SalesBlockEntity) h.getLevel().getBlockEntity(at);
            DisplayCaseBlockEntity box = (DisplayCaseBlockEntity) h.getLevel().getBlockEntity(at.above());
            ItemStack named = new ItemStack(Items.DIAMOND);
            named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("Shiny"));
            box.goods().set(0, named.copy());
            sales.prices().set(2, new ItemStack(Items.EMERALD, 4));
            ShopsTests.Shop shop = new ShopsTests.Shop(at, at.above(), h);
            // Everybody buys, an operator too: the click opens the buyer menu, never the owner menu.
            for (ServerPlayer p : List.of(buyer, op, second)) {
                p.setPos(Vec3.atCenterOf(at).add(0.0, 0.0, -2.0));
                h.assertTrue(ShopActions.useSales(p, h.getLevel(), at).consumesAction() && p.containerMenu instanceof ShopBuyerMenu, "buyer menu for " + p.getName().getString());
            }
            h.assertTrue(sales.lockFree(), "a server shop is never locked: several buyers at once");
            for (ServerPlayer p : List.of(buyer, op, second)) p.closeContainer();
            // Unlimited: 12 purchases of a stack that holds one piece, goods never shrink, the buyer gets exact copies.
            for (int i = 0; i < buyer.getInventory().getContainerSize(); i++) buyer.getInventory().setItem(i, ItemStack.EMPTY);
            buyer.getInventory().setItem(0, new ItemStack(Items.EMERALD, 50));
            ShopBuyerMenu menu = buy(h, buyer, shop);
            int revision = sales.revision();
            menu.pay().setItem(2, new ItemStack(Items.EMERALD, 3));
            h.assertTrue(ShopsTests.trade(menu, buyer) == ShopTrade.Result.WRONG_PAYMENT, "too little is refused");
            h.assertTrue(box.goods().get(0).getCount() == 1 && count(buyer.getInventory(), Items.DIAMOND) == 0 && menu.pay().getItem(2).getCount() == 3, "a refused trade changes nothing");
            menu.pay().setItem(2, new ItemStack(Items.EMERALD, 48));
            menu.clickMenuButton(buyer, ShopBuyerMenu.button(ShopBuyerMenu.TRADE_MAX, menu.revisionSeen()));
            h.assertTrue(menu.lastResult == ShopTrade.Result.WRONG_PAYMENT || menu.lastResult == ShopTrade.Result.OK, "last result " + menu.lastResult);
            h.assertTrue(count(buyer.getInventory(), Items.DIAMOND) == 12 && menu.pay().getItem(2).isEmpty(), "12 purchases from one piece of stock");
            for (int i = 0; i < buyer.getInventory().getContainerSize(); i++) {
                ItemStack s = buyer.getInventory().getItem(i);
                if (s.is(Items.DIAMOND)) h.assertTrue(ItemStack.isSameItemSameComponents(s, named) && s.getCount() <= 64, "an exact copy with the name");
            }
            h.assertTrue(box.goods().get(0).getCount() == 1 && ItemStack.isSameItemSameComponents(box.goods().get(0), named), "goods untouched");
            h.assertTrue(sales.earnings().stream().allMatch(ItemStack::isEmpty), "no earnings store");
            h.assertTrue(sales.revision() == revision, "trading does not change the shop revision");
            h.assertTrue(count(buyer.getInventory(), Items.EMERALD) == 50, "the inventory was not touched (the test filled the pay slots directly)");
            AABB area = AABB.ofSize(Vec3.atCenterOf(at), 10, 10, 10);
            for (ItemEntity e : h.getLevel().getEntitiesOfClass(ItemEntity.class, area)) h.assertTrue(!e.getItem().is(Items.EMERALD), "no payment on the ground");
            h.assertTrue(ServerShops.get(h.getLevel().getServer()).cash().isEmpty() || !ShopsConfig.collectServerShopIncome, "income vanishes by default");
            menu.removed(buyer);
            // Optional admin cash: the price items are summed instead of vanishing.
            ShopsConfig.collectServerShopIncome = true;
            try {
                ServerShops registry = ServerShops.get(h.getLevel().getServer());
                registry.clearCash();
                ShopBuyerMenu again = buy(h, second, shop);
                second.getInventory().setItem(0, ItemStack.EMPTY);
                again.pay().setItem(2, new ItemStack(Items.EMERALD, 8));
                again.clickMenuButton(second, ShopBuyerMenu.button(ShopBuyerMenu.TRADE_MAX, again.revisionSeen()));
                long cash = registry.cash().stream().filter(c -> c.stack().is(Items.EMERALD)).mapToLong(ServerShops.Cash::count).sum();
                h.assertTrue(cash == 8 && count(second.getInventory(), Items.DIAMOND) == 2, "admin cash holds 8 emeralds for 2 purchases: " + cash);
                again.removed(second);
                registry.clearCash();
            } finally {
                ShopsConfig.reset();
            }
            // A player's case never sells from a server sales block (and the other way round).
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            box.goods().set(1, new ItemStack(Items.GOLD_INGOT, 5));
            DisplayCaseBlockEntity mixed = new DisplayCaseBlockEntity(at.above(), ShopsRegistry.DISPLAY_CASE.defaultBlockState());
            mixed.setLevel(h.getLevel());
            mixed.initFor(owner);
            mixed.goods().set(0, new ItemStack(Items.DIAMOND, 9));
            buyer.getInventory().setItem(0, new ItemStack(Items.EMERALD, 4));
            ShopBuyerMenu fake = buy(h, buyer, shop);
            fake.pay().setItem(2, new ItemStack(Items.EMERALD, 4));
            h.assertTrue(ShopTrade.execute(buyer, sales, mixed, 0, -1, fake.pay()) == ShopTrade.Result.INVALID && mixed.goods().get(0).getCount() == 9, "mixed halves are refused");
            fake.removed(buyer);
            ServerShops.remove(h.getLevel().getServer(), ServerShops.get(h.getLevel().getServer()).find(made.id()));
            h.succeed();
        });
        all.put("servershop_protection", h -> {
            ServerPlayer op = op(h, true);
            ServerPlayer stranger = player(h, GameType.SURVIVAL);
            ServerPlayer creative = player(h, GameType.CREATIVE);
            BlockPos at = h.absolutePos(new BlockPos(7, 1, 7));
            h.getLevel().setBlock(h.absolutePos(new BlockPos(7, 0, 7)), Blocks.STONE.defaultBlockState(), 3);
            ServerShops.CreateResult made = ServerShops.create(h.getLevel(), at, Direction.NORTH);
            SalesBlockEntity sales = (SalesBlockEntity) h.getLevel().getBlockEntity(at);
            DisplayCaseBlockEntity box = (DisplayCaseBlockEntity) h.getLevel().getBlockEntity(at.above());
            box.goods().set(0, new ItemStack(Items.DIAMOND, 2));
            sales.prices().set(2, new ItemStack(Items.EMERALD, 1));
            // Nobody but an operator breaks it: not survival, not creative.
            for (BlockPos pos : List.of(at, at.above())) {
                h.assertTrue(!stranger.gameMode.destroyBlock(pos) && !creative.gameMode.destroyBlock(pos), "stranger break refused at " + pos);
                h.assertTrue(h.getLevel().getBlockState(pos).getDestroyProgress(stranger, h.getLevel(), pos) == 0.0F, "no breaking animation");
            }
            h.assertTrue(ShopGuard.mayBreak(op, h.getLevel(), at) && !ShopGuard.mayBreak(stranger, h.getLevel(), at), "mayBreak: operator only");
            ShopGuard.register(h.getLevel().getServer());
            try {
                String dim = h.getLevel().dimension().identifier().toString();
                for (Protection.Action action : List.of(Protection.Action.DESTROY, Protection.Action.BUILD, Protection.Action.ANY)) {
                    h.assertTrue(!Protection.allows(h.getLevel().getServer(), new Protection.Target(stranger.getUUID(), dim, at.getX(), at.getY(), at.getZ(), false), action), "stranger " + action);
                    h.assertTrue(Protection.allows(h.getLevel().getServer(), new Protection.Target(op.getUUID(), dim, at.getX(), at.getY(), at.getZ(), true), action), "admin " + action);
                }
            } finally {
                ShopGuard.unregister(h.getLevel().getServer());
            }
            // Edit menus are for operators only.
            SalesOwnerMenu priceMenu = new SalesOwnerMenu(5, stranger.getInventory(), sales, null);
            StockMenu goodsMenu = new StockMenu(6, stranger.getInventory(), box, sales, null);
            h.assertTrue(!priceMenu.stillValid(stranger) && !goodsMenu.stillValid(stranger), "a stranger cannot edit through a menu");
            op.setPos(Vec3.atCenterOf(at).add(0.0, 0.0, -2.0));
            h.assertTrue(priceMenu.stillValid(op) && goodsMenu.stillValid(op), "an operator can");
            // Placing a case onto a server sales block is refused, even for an operator (the command builds it).
            ServerShops.Shop shop = ServerShops.get(h.getLevel().getServer()).find(made.id());
            h.assertTrue(op.gameMode.destroyBlock(at.above()), "operator breaks the case");
            AABB area = AABB.ofSize(Vec3.atCenterOf(at), 8, 8, 8);
            for (ItemEntity e : h.getLevel().getEntitiesOfClass(ItemEntity.class, area)) h.assertTrue(!e.getItem().is(Items.DIAMOND) && !e.getItem().is(Items.EMERALD), "goods and prices never drop: " + e.getItem());
            h.assertTrue(ServerShops.locate(h.getLevel().getServer(), shop, false) != null && !ServerShops.locate(h.getLevel().getServer(), shop, false).complete(), "half a shop is reported as damaged");
            place(op, new ItemStack(ShopsRegistry.DISPLAY_CASE_ITEM), at);
            h.assertTrue(h.getLevel().getBlockState(at.above()).isAir(), "no case on a server sales block");
            // A damaged shop can still be removed; nothing drops.
            ServerShops.remove(h.getLevel().getServer(), shop);
            h.assertTrue(h.getLevel().getBlockState(at).isAir() && ServerShops.get(h.getLevel().getServer()).find(made.id()) == null, "removed");
            for (ItemEntity e : h.getLevel().getEntitiesOfClass(ItemEntity.class, area)) h.assertTrue(!e.getItem().is(Items.DIAMOND) && !e.getItem().is(Items.EMERALD), "nothing dropped on remove");
            // Breaking the sales block by hand also forgets the entry.
            ServerShops.CreateResult again = ServerShops.create(h.getLevel(), at, Direction.EAST);
            h.assertTrue(again.status() == ServerShops.Created.OK && !again.id().equals(made.id()), "new id " + again);
            h.assertTrue(op.gameMode.destroyBlock(at.above()) && op.gameMode.destroyBlock(at), "operator breaks both");
            h.assertTrue(ServerShops.get(h.getLevel().getServer()).find(again.id()) == null, "the entry goes with the block");
            h.succeed();
        });
        all.put("servershop_persistence", h -> {
            ServerPlayer op = op(h, true);
            BlockPos at = h.absolutePos(new BlockPos(7, 1, 7));
            h.getLevel().setBlock(h.absolutePos(new BlockPos(7, 0, 7)), Blocks.STONE.defaultBlockState(), 3);
            ServerShops.CreateResult made = ServerShops.create(h.getLevel(), at, Direction.SOUTH);
            SalesBlockEntity sales = (SalesBlockEntity) h.getLevel().getBlockEntity(at);
            DisplayCaseBlockEntity box = (DisplayCaseBlockEntity) h.getLevel().getBlockEntity(at.above());
            box.goods().set(0, new ItemStack(Items.DIAMOND));
            sales.prices().set(2, new ItemStack(Items.EMERALD, 4));
            var registries = h.getLevel().registryAccess();
            // Block entities: flag and data survive a save and load; the client part never carries the id.
            var salesTag = sales.saveWithoutMetadata(registries);
            var boxTag = box.saveWithoutMetadata(registries);
            SalesBlockEntity salesBack = new SalesBlockEntity(at, sales.getBlockState());
            salesBack.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, registries, salesTag));
            DisplayCaseBlockEntity boxBack = new DisplayCaseBlockEntity(at.above(), box.getBlockState());
            boxBack.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, registries, boxTag));
            // Ender shops: prices and goods stay in the world data; the reloaded block entities find them through their saved id.
            salesBack.setLevel(h.getLevel());
            boxBack.setLevel(h.getLevel());
            h.assertTrue(salesBack.serverId().equals(made.id()) && salesBack.prices().get(2).is(Items.EMERALD) && boxBack.isServerShop() && boxBack.goods().get(0).is(Items.DIAMOND), "block entities reload as server shop");
            h.assertTrue(!sales.getUpdateTag(registries).toString().contains("server_id"), "the id is not sent to clients");
            // Registry: ids, counter and cash survive the codec.
            ServerShops registry = ServerShops.get(h.getLevel().getServer());
            registry.collect(List.of(new ItemStack(Items.EMERALD, 4), new ItemStack(Items.EMERALD, 2), new ItemStack(Items.COAL, 1)));
            var encoded = ServerShops.CODEC.encodeStart(NbtOps.INSTANCE, registry).getOrThrow();
            ServerShops back = ServerShops.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
            h.assertTrue(back.shops().equals(registry.shops()) && back.find(made.id()) != null, "shops survive");
            h.assertTrue(back.cash().stream().anyMatch(c -> c.stack().is(Items.EMERALD) && c.count() == 6) && back.cash().size() == registry.cash().size(), "cash survives and merges");
            h.assertTrue(ServerShops.formatId(1).equals("s001") && ServerShops.formatId(12).equals("s012") && ServerShops.formatId(1234).equals("s1234"), "id format");
            // The next id continues after a reload.
            List<String> before = new ArrayList<>();
            for (var s : back.shops()) before.add(s.id());
            ServerShops.remove(h.getLevel().getServer(), registry.find(made.id()));
            registry.clearCash();
            ServerShops.CreateResult next = ServerShops.create(h.getLevel(), at, Direction.NORTH);
            h.assertTrue(next.status() == ServerShops.Created.OK && !before.contains(next.id()), "never reused: " + next.id());
            ServerShops.remove(h.getLevel().getServer(), registry.find(next.id()));
            h.succeed();
        });
    }

    // --- helpers ------------------------------------------------------------------------------------------------------------------

    /** A player with permission level 2 (or 1 when {@code gamemaster} is false), removed from the operator list when the test ends. */
    static ServerPlayer op(GameTestHelper h, boolean gamemaster) {
        ServerPlayer p = player(h, GameType.SURVIVAL);
        var list = h.getLevel().getServer().getPlayerList();
        list.op(p.nameAndId(), Optional.of(gamemaster ? LevelBasedPermissionSet.GAMEMASTER : LevelBasedPermissionSet.MODERATOR), Optional.empty());
        h.runBeforeTestEnd(() -> list.deop(p.nameAndId()));
        return p;
    }

    /** Runs a command line as {@code p} through the real dispatcher; false when it is refused, unknown or fails. */
    static boolean runs(ServerPlayer p, String command) {
        try {
            return p.level().getServer().getCommands().getDispatcher().execute(command, p.createCommandSourceStack()) > 0;
        } catch (CommandSyntaxException e) {
            return false;
        }
    }

    static int listed(ServerPlayer p, String command) {
        try {
            return p.level().getServer().getCommands().getDispatcher().execute(command, p.createCommandSourceStack());
        } catch (CommandSyntaxException e) {
            return -1;
        }
    }

    static ServerShops.Shop shopAt(GameTestHelper h, BlockPos pos) {
        ServerLevel level = h.getLevel();
        String dim = level.dimension().identifier().toString();
        for (ServerShops.Shop s : ServerShops.get(level.getServer()).shops()) if (s.pos() == pos.asLong() && s.dimension().equals(dim)) return s;
        return null;
    }


    private ServerShopTests() {}
}

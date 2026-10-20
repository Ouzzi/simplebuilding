package com.simpleshops.logic;

import com.simpleshops.block.DisplayCaseBlockEntity;
import com.simpleshops.block.EarningsStore;
import com.simpleshops.block.SalesBlockEntity;
import com.simpleshops.config.ShopsConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * The one place where a trade happens (server thread only). First everything is checked and simulated, then everything is
 * changed in one go, with nothing in between that could wait for the network or a file: price items leave the buyer's pay
 * slots, land in the earnings store, one piece leaves the case, and the buyer receives an exact one-piece copy (on site a full
 * inventory drops it in front of the buyer, like an overflowing villager trade; from a trade terminal a full inventory means no
 * trade). Quantities, prices and items come from the shop's world data, never from a packet. Any failure before the commit
 * changes nothing. On site and from the trade terminal it is the same path ({@link #core}).
 */
public final class ShopTrade {
    public enum Result { OK, STALE, NO_STOCK, NO_PRICE, WRONG_PAYMENT, EARNINGS_FULL, INVALID, NO_SPACE, BUSY, FAR }

    /**
     * @param ware           goods slot to buy from (0..4)
     * @param clientRevision the revision the buyer saw, low 16 bits ({@code -1} skips the check, for the server side logic)
     * @param pay            the buyer's five pay slots; slot {@code p} pays price position {@code p} only (nothing is counted twice)
     */
    public static Result execute(ServerPlayer buyer, SalesBlockEntity sales, DisplayCaseBlockEntity shopCase, int ware,
                                 int clientRevision, Container pay) {
        if (sales.isRemoved() || shopCase.isRemoved() || ware < 0 || ware >= DisplayCaseBlockEntity.SIZE || pay.getContainerSize() < PriceRow.SIZE) {
            return Result.INVALID;
        }
        boolean server = sales.isServerShop();
        // Both halves must be the same kind (a player's case never sells from a server sales block and the other way round).
        if (server != shopCase.isServerShop()) return Result.INVALID;
        if (clientRevision >= 0 && (sales.revision() & 0xFFFF) != clientRevision) return Result.STALE;
        // The earnings go into the whole chain (sales block, then the storages below it with the same owner); a server shop has none.
        List<EarningsStore> stores = server ? List.of() : ShopStorage.chain(sales);
        List<NonNullList<ItemStack>> lists = new ArrayList<>();
        for (EarningsStore s : stores) lists.add(s.earnings());
        Result result = core(buyer, server, sales.prices(), shopCase.goods(), ware, lists, pay, false);
        if (result != Result.OK) return result;
        if (server) {
            collect(buyer, sales.prices());
        } else {
            sales.bumpRevision();
            for (EarningsStore store : stores) store.changed();
            shopCase.changed();
        }
        pay.setChanged();
        return Result.OK;
    }

    /**
     * The same trade from a trade terminal, against the shop's world data only: the shop's chunk is never loaded. The buyer
     * must hold the shop's lock (taken when the offer was selected) and have room for the piece.
     */
    public static Result executeRemote(ServerPlayer buyer, EnderShops.Vault sales, int ware, int clientRevision, Container pay) {
        var server = buyer.level().getServer();
        EnderShops data = EnderShops.get(server);
        if (sales.deleted() || sales.kind != EnderShops.Kind.SALES || data.find(sales.id) != sales || ware < 0 || ware >= DisplayCaseBlockEntity.SIZE
                || pay.getContainerSize() < PriceRow.SIZE) return Result.INVALID;
        EnderShops.Vault box = data.caseOf(sales);
        if (box == null || box.isServer() != sales.isServer()) return Result.INVALID;
        if (sales.lockHolder() != buyer) return Result.BUSY;
        if (clientRevision >= 0 && (sales.revision() & 0xFFFF) != clientRevision) return Result.STALE;
        List<NonNullList<ItemStack>> lists = sales.isServer() ? List.of() : data.earningsChain(sales);
        Result result = core(buyer, sales.isServer(), sales.prices, box.items, ware, lists, pay, true);
        if (result != Result.OK) return result;
        if (sales.isServer()) {
            collect(buyer, sales.prices);
        } else {
            sales.bumpRevision();
            EnderShops.touch(server, sales);
            EnderShops.touch(server, box);
            for (String id : sales.storages()) {
                EnderShops.Vault s = data.find(id);
                if (s != null) EnderShops.touch(server, s);
            }
        }
        pay.setChanged();
        return Result.OK;
    }

    private static void collect(ServerPlayer buyer, NonNullList<ItemStack> prices) {
        // Nothing in the shop changed (prices and goods stay), so no revision bump: several buyers can use it at once.
        if (!ShopsConfig.collectServerShopIncome) return;
        List<ItemStack> paid = new ArrayList<>();
        for (ItemStack price : prices) if (!price.isEmpty()) paid.add(price);
        ServerShops.get(buyer.level().getServer()).collect(paid);
    }

    /** Check, simulate, commit (with rollback). {@code server}: goods are templates and the price is a sink. */
    private static Result core(ServerPlayer buyer, boolean server, NonNullList<ItemStack> prices, NonNullList<ItemStack> goods, int ware,
                               List<NonNullList<ItemStack>> stores, Container pay, boolean needSpace) {
        ItemStack stock = goods.get(ware);
        if (stock.isEmpty()) return Result.NO_STOCK;
        if (PriceRow.filled(prices) == 0) return Result.NO_PRICE;
        for (int p = 0; p < PriceRow.SIZE; p++) {
            ItemStack price = prices.get(p);
            if (price.isEmpty()) continue;
            ItemStack paid = pay.getItem(p);
            if (paid.isEmpty() || !ItemStack.isSameItemSameComponents(price, paid) || paid.getCount() < price.getCount()) return Result.WRONG_PAYMENT;
        }
        List<NonNullList<ItemStack>> simulated = server ? List.of() : simulate(stores, prices);
        if (simulated == null) return Result.EARNINGS_FULL;
        ItemStack delivered = stock.copyWithCount(1);
        if (needSpace && buyer.getInventory().getSlotWithRemainingSpace(delivered) < 0 && buyer.getInventory().getFreeSlot() < 0) return Result.NO_SPACE;
        // Commit. Snapshots make a failure in here undo everything.
        ItemStack[] paySnapshot = new ItemStack[PriceRow.SIZE];
        for (int p = 0; p < PriceRow.SIZE; p++) paySnapshot[p] = pay.getItem(p).copy();
        List<List<ItemStack>> earningsSnapshot = new ArrayList<>();
        for (NonNullList<ItemStack> store : stores) earningsSnapshot.add(copy(store));
        ItemStack stockSnapshot = stock.copy();
        try {
            for (int p = 0; p < PriceRow.SIZE; p++) {
                ItemStack price = prices.get(p);
                if (price.isEmpty()) continue;
                ItemStack left = pay.getItem(p).copy();
                left.shrink(price.getCount());
                pay.setItem(p, left.isEmpty() ? ItemStack.EMPTY : left);
            }
            if (!server) {
                for (int n = 0; n < stores.size(); n++) {
                    for (int i = 0; i < simulated.get(n).size(); i++) stores.get(n).set(i, simulated.get(n).get(i));
                }
                ItemStack rest = stock.copy();
                rest.shrink(1);
                goods.set(ware, rest.isEmpty() ? ItemStack.EMPTY : rest);
            }
            buyer.getInventory().placeItemBackInInventory(delivered, true, net.minecraft.util.Prediction.SERVER_ONLY);
        } catch (RuntimeException e) {
            for (int p = 0; p < PriceRow.SIZE; p++) pay.setItem(p, paySnapshot[p]);
            for (int n = 0; n < stores.size(); n++) {
                for (int i = 0; i < earningsSnapshot.get(n).size(); i++) stores.get(n).set(i, earningsSnapshot.get(n).get(i));
            }
            goods.set(ware, stockSnapshot);
            com.simpleshops.SimpleShops.LOG.error("Trade rolled back", e);
            return Result.INVALID;
        }
        return Result.OK;
    }

    private static List<ItemStack> copy(NonNullList<ItemStack> list) {
        List<ItemStack> out = new ArrayList<>(list.size());
        for (ItemStack s : list) out.add(s.copy());
        return out;
    }

    /** Copies of the stores' slots with every price item merged in, or {@code null} when the whole price does not fit. */
    private static @org.jspecify.annotations.Nullable List<NonNullList<ItemStack>> simulate(List<NonNullList<ItemStack>> stores, NonNullList<ItemStack> prices) {
        List<NonNullList<ItemStack>> lists = new ArrayList<>();
        for (NonNullList<ItemStack> store : stores) {
            NonNullList<ItemStack> c = NonNullList.withSize(store.size(), ItemStack.EMPTY);
            for (int i = 0; i < c.size(); i++) c.set(i, store.get(i).copy());
            lists.add(c);
        }
        for (ItemStack price : prices) {
            if (price.isEmpty()) continue;
            if (!ShopStorage.insert(lists, price.copy()).isEmpty()) return null;
        }
        return lists;
    }

    /** Whether one trade's price still fits into the shop's earnings (a server shop always does). Used to disable the buy button. */
    public static boolean fits(SalesBlockEntity sales) {
        if (sales.isServerShop()) return true;
        List<NonNullList<ItemStack>> lists = new ArrayList<>();
        for (EarningsStore s : ShopStorage.chain(sales)) lists.add(s.earnings());
        return simulate(lists, sales.prices()) != null;
    }

    /** The same check against the world data (trade terminal). */
    public static boolean fitsRemote(EnderShops data, EnderShops.Vault sales) {
        if (sales.isServer()) return true;
        return simulate(data.earningsChain(sales), sales.prices) != null;
    }

    private ShopTrade() {}
}

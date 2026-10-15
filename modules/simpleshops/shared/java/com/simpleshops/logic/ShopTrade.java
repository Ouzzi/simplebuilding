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
 * slots, land in the earnings store, one piece leaves the case, and the buyer receives an exact one-piece copy (a full
 * inventory drops it in front of the buyer, like an overflowing villager trade). Quantities, prices and items come from the
 * block entities, never from a packet. Any failure before the commit changes nothing.
 */
public final class ShopTrade {
    public enum Result { OK, STALE, NO_STOCK, NO_PRICE, WRONG_PAYMENT, EARNINGS_FULL, INVALID }

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
        ItemStack stock = shopCase.goods().get(ware);
        if (stock.isEmpty()) return Result.NO_STOCK;
        NonNullList<ItemStack> prices = sales.prices();
        if (PriceRow.filled(prices) == 0) return Result.NO_PRICE;
        for (int p = 0; p < PriceRow.SIZE; p++) {
            ItemStack price = prices.get(p);
            if (price.isEmpty()) continue;
            ItemStack paid = pay.getItem(p);
            if (paid.isEmpty() || !ItemStack.isSameItemSameComponents(price, paid) || paid.getCount() < price.getCount()) return Result.WRONG_PAYMENT;
        }
        // The earnings go into the whole chain (sales block, then the storages below it with the same owner); a server shop has none.
        List<EarningsStore> stores = server ? List.of() : ShopStorage.chain(sales);
        List<NonNullList<ItemStack>> simulated = server ? List.of() : simulate(stores, prices);
        if (simulated == null) return Result.EARNINGS_FULL;
        // Commit. Snapshots make a failure in here undo everything.
        ItemStack[] paySnapshot = new ItemStack[PriceRow.SIZE];
        for (int p = 0; p < PriceRow.SIZE; p++) paySnapshot[p] = pay.getItem(p).copy();
        List<List<ItemStack>> earningsSnapshot = new ArrayList<>();
        for (EarningsStore store : stores) earningsSnapshot.add(copy(store.earnings()));
        ItemStack stockSnapshot = stock.copy();
        ItemStack delivered = stock.copyWithCount(1);
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
                    for (int i = 0; i < simulated.get(n).size(); i++) stores.get(n).earnings().set(i, simulated.get(n).get(i));
                }
                ItemStack rest = stock.copy();
                rest.shrink(1);
                shopCase.goods().set(ware, rest.isEmpty() ? ItemStack.EMPTY : rest);
            }
            buyer.getInventory().placeItemBackInInventory(delivered, true, net.minecraft.util.Prediction.SERVER_ONLY);
        } catch (RuntimeException e) {
            for (int p = 0; p < PriceRow.SIZE; p++) pay.setItem(p, paySnapshot[p]);
            for (int n = 0; n < stores.size(); n++) {
                for (int i = 0; i < earningsSnapshot.get(n).size(); i++) stores.get(n).earnings().set(i, earningsSnapshot.get(n).get(i));
            }
            shopCase.goods().set(ware, stockSnapshot);
            com.simpleshops.SimpleShops.LOG.error("Trade rolled back", e);
            return Result.INVALID;
        }
        if (server) {
            // Nothing in the shop changed (prices and goods stay), so no revision bump: several buyers can use it at once.
            if (ShopsConfig.collectServerShopIncome) {
                List<ItemStack> paid = new ArrayList<>();
                for (ItemStack price : prices) if (!price.isEmpty()) paid.add(price);
                ServerShops.get(buyer.level().getServer()).collect(paid);
            }
        } else {
            sales.bumpRevision();
            for (EarningsStore store : stores) store.changed();
            shopCase.changed();
        }
        pay.setChanged();
        return Result.OK;
    }

    private static List<ItemStack> copy(NonNullList<ItemStack> list) {
        List<ItemStack> out = new ArrayList<>(list.size());
        for (ItemStack s : list) out.add(s.copy());
        return out;
    }

    /** Copies of the stores' slots with every price item merged in, or {@code null} when the whole price does not fit. */
    private static @org.jspecify.annotations.Nullable List<NonNullList<ItemStack>> simulate(List<EarningsStore> stores, NonNullList<ItemStack> prices) {
        List<NonNullList<ItemStack>> lists = new ArrayList<>();
        for (EarningsStore store : stores) {
            NonNullList<ItemStack> c = NonNullList.withSize(store.earnings().size(), ItemStack.EMPTY);
            for (int i = 0; i < c.size(); i++) c.set(i, store.earnings().get(i).copy());
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
        return simulate(ShopStorage.chain(sales), sales.prices()) != null;
    }

    private ShopTrade() {}
}

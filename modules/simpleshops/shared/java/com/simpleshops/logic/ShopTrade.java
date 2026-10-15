package com.simpleshops.logic;

import com.simpleshops.block.DisplayCaseBlockEntity;
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
        NonNullList<ItemStack> simulated = NonNullList.withSize(sales.earnings().size(), ItemStack.EMPTY);
        for (int i = 0; i < simulated.size(); i++) simulated.set(i, sales.earnings().get(i).copy());
        // A server shop has no earnings store: the payment is a sink (or goes to the admin cash below), the stock is unlimited.
        if (!server) {
            for (int p = 0; p < PriceRow.SIZE; p++) {
                ItemStack price = prices.get(p);
                if (price.isEmpty()) continue;
                if (!insert(simulated, price.copy()).isEmpty()) return Result.EARNINGS_FULL;
            }
        }
        // Commit. Snapshots make a failure in here undo everything.
        ItemStack[] paySnapshot = new ItemStack[PriceRow.SIZE];
        for (int p = 0; p < PriceRow.SIZE; p++) paySnapshot[p] = pay.getItem(p).copy();
        ItemStack[] earningsSnapshot = new ItemStack[sales.earnings().size()];
        for (int i = 0; i < earningsSnapshot.length; i++) earningsSnapshot[i] = sales.earnings().get(i).copy();
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
                for (int i = 0; i < simulated.size(); i++) sales.earnings().set(i, simulated.get(i));
                ItemStack rest = stock.copy();
                rest.shrink(1);
                shopCase.goods().set(ware, rest.isEmpty() ? ItemStack.EMPTY : rest);
            }
            buyer.getInventory().placeItemBackInInventory(delivered, true, net.minecraft.util.Prediction.SERVER_ONLY);
        } catch (RuntimeException e) {
            for (int p = 0; p < PriceRow.SIZE; p++) pay.setItem(p, paySnapshot[p]);
            for (int i = 0; i < earningsSnapshot.length; i++) sales.earnings().set(i, earningsSnapshot[i]);
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
            sales.changed();
            shopCase.changed();
        }
        pay.setChanged();
        return Result.OK;
    }

    /** Merges {@code stack} into {@code list} (same item and components first, then empty slots); returns what did not fit. */
    public static ItemStack insert(NonNullList<ItemStack> list, ItemStack stack) {
        ItemStack rest = stack.copy();
        int max = rest.getMaxStackSize();
        for (int i = 0; i < list.size() && !rest.isEmpty(); i++) {
            ItemStack at = list.get(i);
            if (at.isEmpty() || !ItemStack.isSameItemSameComponents(at, rest) || at.getCount() >= max) continue;
            int moved = Math.min(max - at.getCount(), rest.getCount());
            at.grow(moved);
            rest.shrink(moved);
        }
        for (int i = 0; i < list.size() && !rest.isEmpty(); i++) {
            if (!list.get(i).isEmpty()) continue;
            int moved = Math.min(max, rest.getCount());
            list.set(i, rest.copyWithCount(moved));
            rest.shrink(moved);
        }
        return rest;
    }

    private ShopTrade() {}
}

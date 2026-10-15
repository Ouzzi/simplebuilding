package com.simplebuilding.modules.simpleshops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.simpleshops.menu.SalesOwnerMenu;
import com.simpleshops.menu.ShopBuyerMenu;
import com.simpleshops.menu.StockMenu;
import com.simpleshops.menu.StorageMenu;
import org.junit.jupiter.api.Test;

/**
 * Geometry of the shop screens (menu constants are compile-time values, no game classes load). <b>What breaks this test:</b> the
 * shared owner screen no longer being exactly as high as a double chest, the earnings not being three chest rows, rows that overlap
 * or leave the box, no room left for the developer button, or the inventory not starting below the block area with its seam.
 */
class ShopLayoutTest {
    private static final int SLOT = 18, FRAME = 5, SEAM = 3;

    @Test
    void sharedOwnerScreenIsDoubleChestHigh() {
        assertEquals(114 + 6 * SLOT, SalesOwnerMenu.C_HEIGHT, "double chest: 222");
        assertEquals(SalesOwnerMenu.C_HEIGHT - 82, SalesOwnerMenu.C_INVENTORY_Y, "inventory rows sit where a chest's do");
    }

    @Test
    void sharedOwnerScreenRowsAreOrderedAndInside() {
        int goods = SalesOwnerMenu.C_GOODS_Y, price = SalesOwnerMenu.C_PRICE_Y, earn = SalesOwnerMenu.C_EARNINGS_Y;
        assertTrue(goods >= FRAME + 8, "goods row below the title");
        assertTrue(goods + SLOT <= price && price + SLOT <= earn, "goods, price, earnings do not overlap, in this order");
        int earningsEnd = earn + 3 * SLOT;
        assertEquals(27, SalesOwnerMenu.C_EARNINGS_END - SalesOwnerMenu.C_EARNINGS_START, "chest size earnings");
        // Developer button strip: 4 px below the earnings, 14 px high, still above the inventory seam.
        assertTrue(earningsEnd + 4 + 14 <= SalesOwnerMenu.C_INVENTORY_Y - SEAM, "room for the developer button");
        assertTrue(SalesOwnerMenu.EARNINGS_X + 9 * SLOT <= SalesOwnerMenu.WIDTH - FRAME, "nine columns fit the box");
        assertTrue(SalesOwnerMenu.PRICE_X + 5 * SLOT <= SalesOwnerMenu.WIDTH - FRAME, "five price slots fit the box");
        assertEquals(SalesOwnerMenu.PRICE_START + 5, SalesOwnerMenu.EARNINGS_START, "alone: price then earnings");
        assertEquals(SalesOwnerMenu.C_GOODS_END + 5, SalesOwnerMenu.C_EARNINGS_START, "combined: goods, price, earnings");
    }

    @Test
    void aloneScreensKeepTheirOwnLayout() {
        assertTrue(SalesOwnerMenu.PRICE_Y + SLOT <= SalesOwnerMenu.EARNINGS_Y, "sales block alone: price above earnings");
        assertTrue(SalesOwnerMenu.EARNINGS_Y + 3 * SLOT <= SalesOwnerMenu.INVENTORY_Y - SEAM, "sales block alone: earnings end above the seam");
        assertEquals(SalesOwnerMenu.HEIGHT - 82, SalesOwnerMenu.INVENTORY_Y, "sales block alone: inventory at the bottom");
        assertEquals(StockMenu.HEIGHT - 82, StockMenu.INVENTORY_Y, "display case alone: inventory at the bottom");
        assertEquals(StorageMenu.HEIGHT, 114 + 3 * SLOT, "storage: chest height");
        assertEquals(StorageMenu.HEIGHT - 82, StorageMenu.INVENTORY_Y);
        assertEquals(ShopBuyerMenu.HEIGHT - 82, ShopBuyerMenu.INVENTORY_Y, "buyer: inventory at the bottom");
        assertTrue(ShopBuyerMenu.PAY_Y + SLOT + 4 + 18 <= ShopBuyerMenu.INVENTORY_Y - SEAM, "buyer: the trade button fits above the seam");
    }
}

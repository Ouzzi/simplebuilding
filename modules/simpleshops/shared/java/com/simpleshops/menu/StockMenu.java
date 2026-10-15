package com.simpleshops.menu;

import com.simpleshops.ShopsRegistry;
import com.simpleshops.block.DisplayCaseBlockEntity;
import com.simpleshops.block.SalesBlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Owner menu of a display case: five real goods slots (an item in = for sale, out = not any more) over the inventory. */
public class StockMenu extends ShopMenu {
    public static final int GOODS_END = DisplayCaseBlockEntity.SIZE;
    public static final int WIDTH = 176, HEIGHT = 133, ROW_X = 60, ROW_Y = 20, INVENTORY_Y = 51;
    private final @Nullable DisplayCaseBlockEntity shopCase;
    private final @Nullable SalesBlockEntity sales;

    /** Client side (menu type factory). */
    public StockMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null, null, null);
    }

    public StockMenu(int containerId, Inventory inventory, @Nullable DisplayCaseBlockEntity shopCase, @Nullable SalesBlockEntity sales,
                     @Nullable ServerPlayer player) {
        super(ShopsRegistry.STOCK_MENU, containerId, shopCase);
        this.shopCase = shopCase;
        this.sales = sales;
        Container goods = shopCase != null ? shopCase.goodsView() : new SimpleContainer(DisplayCaseBlockEntity.SIZE);
        for (int i = 0; i < DisplayCaseBlockEntity.SIZE; i++) addSlot(new Slot(goods, i, ROW_X + i * 18, ROW_Y));
        addInventory(inventory, 8, INVENTORY_Y);
        if (sales != null && player != null) sales.lock(player, this);
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.shopCase != null && !this.shopCase.isOwner(player.getUUID())) return false;
        return super.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack copy = stack.copy();
        boolean moved = index < GOODS_END ? moveItemStackTo(stack, GOODS_END, this.slots.size(), true)
                : moveItemStackTo(stack, 0, GOODS_END, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    protected void onClosed(Player player) {
        if (this.sales != null) this.sales.unlock(this);
    }
}

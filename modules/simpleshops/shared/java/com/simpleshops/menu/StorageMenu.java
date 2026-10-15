package com.simpleshops.menu;

import com.simpleshops.ShopsRegistry;
import com.simpleshops.block.SalesStorageBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Owner menu of a sales storage: its 27 earnings slots (take out only) over the inventory, chest size. */
public class StorageMenu extends ShopMenu {
    public static final int SLOTS = SalesStorageBlockEntity.EARNINGS_SIZE;
    public static final int WIDTH = 176, HEIGHT = 168, ROWS_Y = 18, INVENTORY_Y = 86;
    private final @Nullable SalesStorageBlockEntity storage;

    /** Client side (menu type factory). */
    public StorageMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null);
    }

    public StorageMenu(int containerId, Inventory inventory, @Nullable SalesStorageBlockEntity storage) {
        super(ShopsRegistry.STORAGE_MENU, containerId, storage);
        this.storage = storage;
        Container earnings = storage != null ? storage.earningsView() : new SimpleContainer(SLOTS);
        for (int i = 0; i < SLOTS; i++) {
            addSlot(new Slot(earnings, i, 8 + (i % 9) * 18, ROWS_Y + (i / 9) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        addInventory(inventory, 8, INVENTORY_Y);
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.storage != null && !this.storage.isOwner(player.getUUID())) return false;
        return super.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < SLOTS) return moveToInventory(this.slots.get(index), SLOTS);
        return ItemStack.EMPTY;
    }
}

package com.simpleshops.menu;

import com.simpleshops.ShopsRegistry;
import com.simpleshops.block.SalesBlockEntity;
import com.simpleshops.logic.PriceRow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Owner menu of a sales block: the price row (five template slots, filter principle) above the nine earnings slots.
 * The template slots show copies; a click only edits the template from the cursor stack and never moves a real item
 * (the slots accept and give nothing, so shift-click, drag, hotbar keys, double-click collect and drop never reach them).
 */
public class SalesOwnerMenu extends ShopMenu {
    public static final int PRICE_START = 0, EARNINGS_START = PriceRow.SIZE, EARNINGS_END = EARNINGS_START + SalesBlockEntity.EARNINGS_SIZE;
    public static final int WIDTH = 176, HEIGHT = 166, PRICE_X = 60, PRICE_Y = 20, EARNINGS_X = 8, EARNINGS_Y = 52, INVENTORY_Y = 84;
    private final @Nullable SalesBlockEntity sales;
    private final SimpleContainer priceView = new SimpleContainer(PriceRow.SIZE);

    /** Client side (menu type factory). */
    public SalesOwnerMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null, null);
    }

    public SalesOwnerMenu(int containerId, Inventory inventory, @Nullable SalesBlockEntity sales, @Nullable ServerPlayer player) {
        super(ShopsRegistry.OWNER_MENU, containerId, sales);
        this.sales = sales;
        for (int p = 0; p < PriceRow.SIZE; p++) {
            addSlot(new ViewSlot(this.priceView, p, PRICE_X + p * 18, PRICE_Y, i -> shown(i)));
        }
        Container earnings = sales != null ? sales.earningsView() : new SimpleContainer(SalesBlockEntity.EARNINGS_SIZE);
        for (int i = 0; i < SalesBlockEntity.EARNINGS_SIZE; i++) {
            addSlot(new Slot(earnings, i, EARNINGS_X + i * 18, EARNINGS_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        addInventory(inventory, 8, INVENTORY_Y);
        if (sales != null && player != null) {
            sales.lock(player, this);
            refresh();
        }
    }

    /** The template slot is drawn when it holds one or sits next to the run (its own empty slot, see {@link PriceRow#visible}). */
    private boolean shown(int p) {
        return PriceRow.visible(priceRow(), p);
    }

    private void refresh() {
        if (this.sales == null) return;
        for (int p = 0; p < PriceRow.SIZE; p++) {
            ItemStack template = this.sales.prices().get(p);
            if (!ItemStack.matches(this.priceView.getItem(p), template)) this.priceView.setItem(p, template.copy());
        }
    }

    @Override
    public void broadcastChanges() {
        refresh();
        super.broadcastChanges();
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (slotIndex >= PRICE_START && slotIndex < EARNINGS_START) {
            touch();
            if (input == ContainerInput.PICKUP && this.sales != null && player instanceof ServerPlayer sp && stillValid(sp)
                    && this.sales.isOwner(sp.getUUID())) {
                if (PriceRow.click(this.sales.prices(), slotIndex - PRICE_START, getCarried(), button == 1)) {
                    this.sales.bumpRevision();
                    this.sales.changed();
                    refresh();
                }
            }
            return;
        }
        super.clicked(slotIndex, button, input, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index >= EARNINGS_START && index < EARNINGS_END) return moveToInventory(this.slots.get(index), EARNINGS_END);
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.sales != null && !this.sales.isOwner(player.getUUID())) return false;
        return super.stillValid(player);
    }

    @Override
    protected void onClosed(Player player) {
        if (this.sales != null) this.sales.unlock(this);
    }

    /** The price row as this side sees it (synced template slots on the client). */
    public net.minecraft.core.NonNullList<ItemStack> priceRow() {
        net.minecraft.core.NonNullList<ItemStack> row = PriceRow.empty();
        for (int i = 0; i < PriceRow.SIZE; i++) row.set(i, this.priceView.getItem(i));
        return row;
    }
}

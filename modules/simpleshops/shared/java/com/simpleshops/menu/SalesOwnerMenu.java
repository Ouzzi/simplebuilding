package com.simpleshops.menu;

import com.simpleshops.ShopsRegistry;
import com.simpleshops.block.DisplayCaseBlockEntity;
import com.simpleshops.block.SalesBlockEntity;
import com.simpleshops.logic.PriceRow;
import com.simpleshops.logic.ShopActions;
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
 * Owner menu of a sales block. Alone (no display case above): the price row (five template slots, filter principle) above the
 * 27 earnings slots. Combined (case above, same owner): goods row, price row and earnings in one double-chest-high screen. The
 * template slots show copies; a click only edits the template from the cursor stack and never moves a real item (the slots accept
 * and give nothing, so shift-click, drag, hotbar keys, double-click collect and drop never reach them).
 */
public class SalesOwnerMenu extends ShopMenu {
    /** Layout alone: price row, then the earnings. */
    public static final int PRICE_START = 0, EARNINGS_START = PriceRow.SIZE, EARNINGS_END = EARNINGS_START + SalesBlockEntity.EARNINGS_SIZE;
    public static final int WIDTH = 176, HEIGHT = 196, PRICE_X = 60, PRICE_Y = 18, EARNINGS_X = 8, EARNINGS_Y = 40, INVENTORY_Y = 114;
    /** Layout combined: goods row, price row, earnings (a double chest is 222 high). */
    public static final int C_GOODS_END = DisplayCaseBlockEntity.SIZE, C_PRICE_START = C_GOODS_END, C_EARNINGS_START = C_PRICE_START + PriceRow.SIZE,
            C_EARNINGS_END = C_EARNINGS_START + SalesBlockEntity.EARNINGS_SIZE;
    public static final int C_HEIGHT = 222, C_GOODS_Y = 18, C_PRICE_Y = 40, C_EARNINGS_Y = 62, C_INVENTORY_Y = 140;
    /** Menu button: switch to the buyer's screen (owner only, combined layout). */
    public static final int DEV_BUYER = 20;

    private final @Nullable SalesBlockEntity sales;
    private final @Nullable DisplayCaseBlockEntity shopCase;
    private final boolean combined;
    private final SimpleContainer priceView = new SimpleContainer(PriceRow.SIZE);

    /** Client side (menu type factory). */
    public SalesOwnerMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, false, null, null, null);
    }

    /** Client side of the combined type. */
    public static SalesOwnerMenu combinedClient(int containerId, Inventory inventory) {
        return new SalesOwnerMenu(containerId, inventory, true, null, null, null);
    }

    public SalesOwnerMenu(int containerId, Inventory inventory, @Nullable SalesBlockEntity sales, @Nullable ServerPlayer player) {
        this(containerId, inventory, false, sales, null, player);
    }

    public static SalesOwnerMenu combined(int containerId, Inventory inventory, SalesBlockEntity sales, DisplayCaseBlockEntity shopCase, ServerPlayer player) {
        return new SalesOwnerMenu(containerId, inventory, true, sales, shopCase, player);
    }

    private SalesOwnerMenu(int containerId, Inventory inventory, boolean combined, @Nullable SalesBlockEntity sales,
                           @Nullable DisplayCaseBlockEntity shopCase, @Nullable ServerPlayer player) {
        super(combined ? ShopsRegistry.SHOP_MENU : ShopsRegistry.OWNER_MENU, containerId, sales);
        this.sales = sales;
        this.shopCase = shopCase;
        this.combined = combined;
        if (combined) {
            Container goods = shopCase != null ? shopCase.goodsView() : new SimpleContainer(DisplayCaseBlockEntity.SIZE);
            for (int i = 0; i < DisplayCaseBlockEntity.SIZE; i++) addSlot(new Slot(goods, i, PRICE_X + i * 18, C_GOODS_Y));
        }
        for (int p = 0; p < PriceRow.SIZE; p++) {
            addSlot(new ViewSlot(this.priceView, p, PRICE_X + p * 18, combined ? C_PRICE_Y : PRICE_Y, i -> shown(i)));
        }
        Container earnings = sales != null ? sales.earningsView() : new SimpleContainer(SalesBlockEntity.EARNINGS_SIZE);
        for (int i = 0; i < SalesBlockEntity.EARNINGS_SIZE; i++) {
            addSlot(new Slot(earnings, i, EARNINGS_X + (i % 9) * 18, (combined ? C_EARNINGS_Y : EARNINGS_Y) + (i / 9) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        addInventory(inventory, 8, combined ? C_INVENTORY_Y : INVENTORY_Y);
        if (sales != null && player != null) {
            sales.lock(player, this);
            refresh();
        }
    }

    public boolean isCombined() {
        return this.combined;
    }

    public int priceStart() {
        return this.combined ? C_PRICE_START : PRICE_START;
    }

    public int earningsStart() {
        return this.combined ? C_EARNINGS_START : EARNINGS_START;
    }

    public int earningsEnd() {
        return this.combined ? C_EARNINGS_END : EARNINGS_END;
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
        int start = priceStart();
        if (slotIndex >= start && slotIndex < start + PriceRow.SIZE) {
            touch();
            if (input == ContainerInput.PICKUP && this.sales != null && player instanceof ServerPlayer sp && stillValid(sp)
                    && this.sales.mayEdit(sp)) {
                if (PriceRow.click(this.sales.prices(), slotIndex - start, getCarried(), button == 1)) {
                    this.sales.bumpRevision();
                    this.sales.changed();
                    refresh();
                }
            }
            return;
        }
        super.clicked(slotIndex, button, input, player);
    }

    /** Developer button: the owner sees and uses the buyer's screen of his own shop. Everything is decided on the server. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != DEV_BUYER || !this.combined || this.sales == null || this.shopCase == null || !(player instanceof ServerPlayer sp)) return false;
        if (!stillValid(sp) || !this.sales.mayEdit(sp)) return false;
        touch();
        return ShopActions.switchToBuyer(sp, this.sales);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        int es = earningsStart(), ee = earningsEnd();
        if (index >= es && index < ee) return moveToInventory(this.slots.get(index), ee);
        if (this.combined && index < C_GOODS_END) return moveToInventory(this.slots.get(index), ee);
        if (this.combined && index >= ee) {
            Slot slot = this.slots.get(index);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) return ItemStack.EMPTY;
            ItemStack copy = stack.copy();
            if (!moveItemStackTo(stack, 0, C_GOODS_END, false)) return ItemStack.EMPTY;
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
            return copy;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.sales != null && !this.sales.mayEdit(player)) return false;
        if (this.shopCase != null && (this.shopCase.isRemoved() || !this.shopCase.mayEdit(player))) return false;
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

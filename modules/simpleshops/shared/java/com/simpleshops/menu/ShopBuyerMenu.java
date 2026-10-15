package com.simpleshops.menu;

import com.simpleshops.ShopsRegistry;
import com.simpleshops.block.DisplayCaseBlockEntity;
import com.simpleshops.block.SalesBlockEntity;
import com.simpleshops.logic.PriceRow;
import com.simpleshops.logic.ShopTrade;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Buyer menu: the goods row (pick one), the price row (templates, display only), five pay slots under it (same positions as
 * the prices, each takes only its exact price item) and the trade button. All real decisions run in {@link ShopTrade};
 * the client sends only "select ware i" and "trade" (with the revision it saw). Leftover pay items go back on close.
 */
public class ShopBuyerMenu extends ShopMenu {
    public static final int GOODS_START = 0, PRICE_START = 5, PAY_START = 10, PAY_END = 15, INVENTORY_START = 15;
    public static final int WIDTH = 176, HEIGHT = 192, ROW_X = 60, GOODS_Y = 18, PRICE_Y = 40, PAY_Y = 62, INVENTORY_Y = 110;
    /** Menu button ids: {@code action | revision << 8}. */
    public static final int SELECT = 0, TRADE = 10, TRADE_MAX = 11, MAX_REPEAT = 64;

    private final @Nullable SalesBlockEntity sales;
    private final @Nullable DisplayCaseBlockEntity shopCase;
    private final SimpleContainer goodsView = new SimpleContainer(DisplayCaseBlockEntity.SIZE);
    private final SimpleContainer priceView = new SimpleContainer(PriceRow.SIZE);
    private final SimpleContainer pay = new SimpleContainer(PriceRow.SIZE);
    private final DataSlot selected = DataSlot.standalone();
    private final DataSlot revision = DataSlot.standalone();
    /** Last outcome for tests: ordinal of {@link ShopTrade.Result}, -1 none (server only). */
    public ShopTrade.@Nullable Result lastResult;

    /** Client side (menu type factory). */
    public ShopBuyerMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null, null, null);
    }

    public ShopBuyerMenu(int containerId, Inventory inventory, @Nullable SalesBlockEntity sales, @Nullable DisplayCaseBlockEntity shopCase,
                         @Nullable ServerPlayer player) {
        super(ShopsRegistry.BUYER_MENU, containerId, sales);
        this.sales = sales;
        this.shopCase = shopCase;
        for (int i = 0; i < DisplayCaseBlockEntity.SIZE; i++) {
            addSlot(new ViewSlot(this.goodsView, i, ROW_X + i * 18, GOODS_Y, i2 -> true));
        }
        for (int p = 0; p < PriceRow.SIZE; p++) {
            addSlot(new ViewSlot(this.priceView, p, ROW_X + p * 18, PRICE_Y, i -> !this.priceView.getItem(i).isEmpty()));
        }
        for (int p = 0; p < PriceRow.SIZE; p++) {
            final int position = p;
            addSlot(new Slot(this.pay, p, ROW_X + p * 18, PAY_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    ItemStack price = ShopBuyerMenu.this.priceView.getItem(position);
                    return !price.isEmpty() && ItemStack.isSameItemSameComponents(price, stack);
                }

                @Override
                public boolean isActive() {
                    return !ShopBuyerMenu.this.priceView.getItem(position).isEmpty();
                }
            });
        }
        addInventory(inventory, 8, INVENTORY_Y);
        addDataSlot(this.selected);
        addDataSlot(this.revision);
        if (sales != null && player != null) {
            sales.lock(player, this);
            refresh();
        }
    }

    public int selectedWare() {
        return this.selected.get();
    }

    public int revisionSeen() {
        return this.revision.get();
    }

    public SimpleContainer pay() {
        return this.pay;
    }

    public net.minecraft.world.Container goodsView() {
        return this.goodsView;
    }

    public net.minecraft.world.Container priceView() {
        return this.priceView;
    }

    private void refresh() {
        if (this.sales == null || this.shopCase == null) return;
        for (int p = 0; p < PriceRow.SIZE; p++) {
            ItemStack template = this.sales.prices().get(p);
            if (!ItemStack.matches(this.priceView.getItem(p), template)) this.priceView.setItem(p, template.copy());
        }
        for (int i = 0; i < DisplayCaseBlockEntity.SIZE; i++) {
            ItemStack goods = this.shopCase.goods().get(i);
            if (!ItemStack.matches(this.goodsView.getItem(i), goods)) this.goodsView.setItem(i, goods.copy());
        }
        if (this.shopCase.goods().get(Math.max(0, Math.min(this.selected.get(), DisplayCaseBlockEntity.SIZE - 1))).isEmpty()) {
            int first = 0;
            for (int i = 0; i < DisplayCaseBlockEntity.SIZE; i++) {
                if (!this.shopCase.goods().get(i).isEmpty()) {
                    first = i;
                    break;
                }
            }
            this.selected.set(first);
        }
        this.revision.set(this.sales.revision() & 0xFFFF);
    }

    @Override
    public void broadcastChanges() {
        refresh();
        super.broadcastChanges();
    }

    /** Client only: whether the pay slots hold the exact prices and the chosen ware exists (the button is enabled). */
    public boolean canTradeClient() {
        int ware = this.selected.get();
        if (ware < 0 || ware >= DisplayCaseBlockEntity.SIZE || this.goodsView.getItem(ware).isEmpty()) return false;
        boolean any = false;
        for (int p = 0; p < PriceRow.SIZE; p++) {
            ItemStack price = this.priceView.getItem(p);
            if (price.isEmpty()) continue;
            any = true;
            ItemStack paid = this.pay.getItem(p);
            if (paid.isEmpty() || !ItemStack.isSameItemSameComponents(price, paid) || paid.getCount() < price.getCount()) return false;
        }
        return any;
    }

    /** Button id for the client: the action plus the revision it sees. */
    public static int button(int action, int revision) {
        return action | ((revision & 0xFFFF) << 8);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer sp) || this.sales == null || this.shopCase == null || id < 0) return false;
        if (!stillValid(sp)) return false;
        touch();
        int action = id & 0xFF;
        int seen = (id >>> 8) & 0xFFFF;
        if (action < DisplayCaseBlockEntity.SIZE) {
            if (!this.shopCase.goods().get(action).isEmpty()) this.selected.set(action);
            return true;
        }
        if (action != TRADE && action != TRADE_MAX) return false;
        int times = action == TRADE_MAX ? MAX_REPEAT : 1;
        ServerLevel level = sp.level();
        for (int i = 0; i < times; i++) {
            // The repeat sees the new revision it just produced.
            int expected = i == 0 ? seen : this.sales.revision() & 0xFFFF;
            ShopTrade.Result result = ShopTrade.execute(sp, this.sales, this.shopCase, this.selected.get(), expected, this.pay);
            this.lastResult = result;
            if (result == ShopTrade.Result.OK) {
                feedback(level, true);
                refresh();
            } else {
                if (i == 0) feedback(level, false);
                break;
            }
        }
        broadcastChanges();
        return true;
    }

    private void feedback(ServerLevel level, boolean ok) {
        if (this.sales == null) return;
        var pos = this.sales.getBlockPos();
        level.playSound(null, pos, ok ? SoundEvents.VILLAGER_YES : SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.6F, ok ? 1.2F : 0.9F);
        level.sendParticles(ok ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                ok ? 3 : 4, 0.25, 0.2, 0.25, 0.0);
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        // The goods and price rows are display only: no click type ever touches them.
        if (slotIndex >= GOODS_START && slotIndex < PAY_START) {
            touch();
            return;
        }
        super.clicked(slotIndex, button, input, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index >= PAY_START && index < PAY_END) return moveToInventory(this.slots.get(index), INVENTORY_START);
        if (index >= INVENTORY_START) {
            Slot slot = this.slots.get(index);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) return ItemStack.EMPTY;
            ItemStack copy = stack.copy();
            if (!moveItemStackTo(stack, PAY_START, PAY_END, false)) return ItemStack.EMPTY;
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
            return copy;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.shopCase != null && this.shopCase.isRemoved()) return false;
        return super.stillValid(player);
    }

    @Override
    protected void onClosed(Player player) {
        // Everything the buyer left in the pay slots goes back (what does not fit drops, also after a disconnect).
        clearContainer(player, this.pay);
        if (this.sales != null) this.sales.unlock(this);
    }
}

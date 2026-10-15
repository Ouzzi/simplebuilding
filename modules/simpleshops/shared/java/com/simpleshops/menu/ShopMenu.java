package com.simpleshops.menu;

import com.simpleshops.config.ShopsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * Common part of the three shop menus: the anchor block entity (server side only), the idle timeout and the validity
 * rule (anchor alive, chunk loaded, within reach). The one-player lock is held by the sales block entity and released
 * in {@link #removed}, which runs on close, disconnect, death, dimension change and server stop.
 */
public abstract class ShopMenu extends AbstractContainerMenu {
    protected final @Nullable BlockEntity anchor;
    private long lastAction;

    protected ShopMenu(MenuType<?> type, int containerId, @Nullable BlockEntity anchor) {
        super(type, containerId);
        this.anchor = anchor;
        touch();
    }

    protected final void touch() {
        this.lastAction = this.anchor != null && this.anchor.getLevel() != null ? this.anchor.getLevel().getGameTime() : 0L;
    }

    /** Tests: pretends the last click was {@code ticks} ago. */
    public final void ageForTest(long ticks) {
        this.lastAction -= ticks;
    }

    /** The whole player inventory below a menu: three rows and the hotbar. */
    protected final void addInventory(Inventory inventory, int left, int top) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, left + col * 18, top + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, left + col * 18, top + 58));
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.anchor == null) return true;
        Level level = this.anchor.getLevel();
        if (level == null || this.anchor.isRemoved()) return false;
        BlockPos pos = this.anchor.getBlockPos();
        if (!level.hasChunkAt(pos) || level.getBlockEntity(pos) != this.anchor) return false;
        if (player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) > 64.0) return false;
        return level.getGameTime() - this.lastAction <= ShopsConfig.idleTimeoutSeconds * 20L;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer) onClosed(player);
    }

    /** Server side clean-up: return buyer items, release the lock. */
    protected void onClosed(Player player) {}

    @Override
    public void clicked(int slotIndex, int buttonNum, net.minecraft.world.inventory.ContainerInput input, Player player) {
        touch();
        super.clicked(slotIndex, buttonNum, input, player);
    }

    /** Moves into the player inventory (all slots from {@code inventoryStart}). */
    protected final ItemStack moveToInventory(Slot slot, int inventoryStart) {
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack copy = stack.copy();
        if (!moveItemStackTo(stack, inventoryStart, this.slots.size(), true)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    /** A read-only slot that shows a stack: never accepts, never gives. */
    public static final class ViewSlot extends Slot {
        private final java.util.function.IntPredicate shown;

        public ViewSlot(Container container, int index, int x, int y, java.util.function.IntPredicate shown) {
            super(container, index, x, y);
            this.shown = shown;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean isActive() {
            return this.shown.test(getContainerSlot());
        }
    }
}

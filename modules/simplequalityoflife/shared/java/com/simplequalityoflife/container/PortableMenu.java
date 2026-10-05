package com.simplequalityoflife.container;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Server menu of a shulker box or ender chest opened from the inventory. It carries a Vanilla menu type
 * ({@code SHULKER_BOX}, {@code GENERIC_9xN}), so the client shows the Vanilla screen with the same slot
 * layout; the server keeps the rules: the opened item is locked in its slot (no pickup, swap, throw,
 * drag or double-click), and the menu closes as soon as that exact stack leaves the slot.
 */
public final class PortableMenu extends AbstractContainerMenu {
    private final Container container;
    private final Player player;
    private final int lockedSlot;
    private final ItemStack locked;
    private final Runnable onClose;

    PortableMenu(MenuType<?> type, int id, Inventory inventory, Container container, int rows, int lockedSlot, ItemStack locked, Runnable onClose) {
        super(type, id);
        this.container = container;
        this.player = inventory.player;
        this.lockedSlot = lockedSlot;
        this.locked = locked;
        this.onClose = onClose;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new ContentSlot(container, column + row * 9, 8 + column * 18, 18 + row * 18));
            }
        }
        int top = 18 + rows * 18 + 13;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new PlayerSlot(inventory, column + (row + 1) * 9, 8 + column * 18, top + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) this.addSlot(new PlayerSlot(inventory, column, 8 + column * 18, top + 58));
    }

    public int lockedSlot() {
        return this.lockedSlot;
    }

    /** The opened item is still the very same stack in its slot. */
    public boolean holdsItem() {
        return this.player.isAlive() && !this.player.isRemoved() && this.player.getInventory().getItem(this.lockedSlot) == this.locked
                && PortableContainers.openable(this.locked, this.player.level());
    }

    @Override
    public boolean stillValid(Player player) {
        return player == this.player && this.holdsItem();
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        // Number keys / the offhand key would swap straight out of the locked slot.
        if (input == ContainerInput.SWAP && (button == this.lockedSlot || button == Inventory.SLOT_OFFHAND && this.lockedSlot == Inventory.SLOT_OFFHAND)) return;
        if (!this.holdsItem()) return;
        super.clicked(slotIndex, button, input, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        int size = this.container.getContainerSize();
        boolean moved = slotIndex < size ? this.moveItemStackTo(stack, size, this.slots.size(), true) : this.moveItemStackTo(stack, 0, size, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return before;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.onClose.run();
    }

    private final class ContentSlot extends Slot {
        ContentSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return PortableMenu.this.holdsItem() && this.container.canPlaceItem(this.getContainerSlot(), stack);
        }

        @Override
        public boolean mayPickup(Player player) {
            return PortableMenu.this.holdsItem() && super.mayPickup(player);
        }
    }

    private final class PlayerSlot extends Slot {
        PlayerSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        private boolean isLocked() {
            return this.getContainerSlot() == PortableMenu.this.lockedSlot;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !this.isLocked() && super.mayPlace(stack);
        }

        @Override
        public boolean mayPickup(Player player) {
            return !this.isLocked() && super.mayPickup(player);
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return this.isLocked() ? 0 : super.getMaxStackSize(stack);
        }
    }
}

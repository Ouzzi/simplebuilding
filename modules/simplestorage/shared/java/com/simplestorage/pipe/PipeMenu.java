package com.simplestorage.pipe;

import com.simplestorage.registry.StorageMenus;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The window of the pipe (ST27-1): laid out like the Vanilla hopper window (slot row at y 20, inventory at y 51, height 133) with its one slot, the filter key (button 0) and the output key (button 1), above the inventory.
 * The server menu is wired to the block entity; filter mode, output side and tier reach the client as three data slots.
 */
public class PipeMenu extends AbstractContainerMenu {
    public static final int BUTTON_FILTER = 0, BUTTON_OUTPUT = 1;
    /** Window size and the place of the pipe's slot (the screen and the menu use the same numbers). */
    public static final int IMAGE_WIDTH = 176, SLOT_X = 80, SLOT_Y = 20, INVENTORY_Y = 51;

    private final Container container;
    private final ContainerData data;
    private final @Nullable ItemPipeBlockEntity pipe;

    /** Client. */
    public PipeMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(1), new SimpleContainerData(3), null);
    }

    public static PipeMenu server(int containerId, Inventory inventory, ItemPipeBlockEntity pipe) {
        return new PipeMenu(containerId, inventory, pipe, pipe.data(), pipe);
    }

    private PipeMenu(int containerId, Inventory inventory, Container container, ContainerData data, @Nullable ItemPipeBlockEntity pipe) {
        super(StorageMenus.PIPE, containerId);
        checkContainerSize(container, 1);
        checkContainerDataCount(data, 3);
        this.container = container;
        this.data = data;
        this.pipe = pipe;
        container.startOpen(inventory.player);
        addSlot(new Slot(container, 0, SLOT_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return super.mayPlace(stack) && (PipeMenu.this.pipe == null || PipeMenu.this.pipe.mayPlayerPlace(stack));
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                // moveItemStackTo stacks onto a filled slot without asking mayPlace: a refused stack has no room.
                return mayPlace(stack) ? super.getMaxStackSize(stack) : 0;
            }
        });
        addStandardInventorySlots(inventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    public int filterMode() {
        return this.data.get(ItemPipeBlockEntity.FILTER_MODE);
    }

    public Direction output() {
        return Direction.values()[Math.floorMod(this.data.get(ItemPipeBlockEntity.OUTPUT), 6)];
    }

    public PipeTier tier() {
        return PipeTier.values()[Math.floorMod(this.data.get(ItemPipeBlockEntity.TIER), PipeTier.values().length)];
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.pipe == null || this.pipe.isRemoved()) return false;
        if (id == BUTTON_FILTER) {
            this.pipe.cycleFilterMode();
            return true;
        }
        if (id == BUTTON_OUTPUT && this.pipe.getLevel() != null) {
            ItemPipeBlock.turnOutput(this.pipe.getLevel(), this.pipe.getBlockPos(), this.pipe.getBlockState(), null);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (slotIndex == 0) {
            if (!moveItemStackTo(stack, 1, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }
}

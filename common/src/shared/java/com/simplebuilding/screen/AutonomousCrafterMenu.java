package com.simplebuilding.screen;

import com.simplebuilding.blocks.entity.custom.AutonomousCrafterBlockEntity;
import com.simplebuilding.util.HopperFilterMode;
import com.simplebuilding.util.ItemFilter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.NonInteractiveResultSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.CrafterBlock;

/**
 * The Autonomous Crafter's menu: the crafter's 3x3 grid (slots switch off like the crafter's), the recipe result as a
 * preview, the redstone sign and the filter key. Slot switching and the filter key go through Vanilla's menu button
 * packet ({@link #clickMenuButton}: 0..8 toggle a slot, {@link #BUTTON_FILTER} cycles the filter) - no own payload.
 */
public class AutonomousCrafterMenu extends AbstractContainerMenu implements ContainerListener {
    public static final int BUTTON_FILTER = 9;
    public static final int INV_START = 9, INV_END = 36, HOTBAR_END = 45, RESULT_SLOT = 45;
    /** Filter key (relative to the image): under the arrow, caption in front of it. */
    public static final int FILTER_BUTTON_X = 110, FILTER_BUTTON_Y = 52;

    private final Container container;
    private final ContainerData data;
    private final Player player;
    private final ResultContainer resultContainer = new ResultContainer();

    public AutonomousCrafterMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(AutonomousCrafterBlockEntity.SIZE),
                new SimpleContainerData(AutonomousCrafterBlockEntity.DATA_COUNT));
    }

    public AutonomousCrafterMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(ModScreenHandlers.AUTONOMOUS_CRAFTER_MENU, containerId);
        checkContainerSize(container, AutonomousCrafterBlockEntity.SIZE);
        checkContainerDataCount(data, AutonomousCrafterBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.player = inventory.player;
        container.startOpen(inventory.player);
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                this.addSlot(new GridSlot(container, x + y * 3, 26 + x * 18, 17 + y * 18));
            }
        }
        this.addStandardInventorySlots(inventory, 8, 84);
        this.addSlot(new NonInteractiveResultSlot(this.resultContainer, 0, 134, 35));
        this.addDataSlots(data);
        this.addSlotListener(this);
        refreshResult();
    }

    public boolean isSlotDisabled(int slot) {
        return slot >= 0 && slot < AutonomousCrafterBlockEntity.SIZE && this.data.get(slot) == 1;
    }

    public boolean isPowered() {
        return this.data.get(AutonomousCrafterBlockEntity.DATA_TRIGGERED) == 1;
    }

    public HopperFilterMode filterMode() {
        int ordinal = this.data.get(AutonomousCrafterBlockEntity.DATA_FILTER);
        return ordinal >= 0 && ordinal < HopperFilterMode.values().length ? HopperFilterMode.values()[ordinal] : HopperFilterMode.NONE;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(this.container instanceof AutonomousCrafterBlockEntity crafter)) {
            return false;
        }
        if (id >= 0 && id < AutonomousCrafterBlockEntity.SIZE) {
            crafter.setSlotDisabled(id, !crafter.isSlotDisabled(id));
            return true;
        }
        if (id == BUTTON_FILTER) {
            crafter.cycleFilterMode();
            return true;
        }
        return false;
    }

    private void refreshResult() {
        if (this.player instanceof ServerPlayer serverPlayer && this.container instanceof CraftingContainer crafting) {
            CraftingInput input = crafting.asCraftInput();
            ItemStack result = CrafterBlock.getPotentialResults(serverPlayer.level(), input)
                    .map(recipe -> recipe.value().assemble(input)).orElse(ItemStack.EMPTY);
            this.resultContainer.setItem(0, result);
        }
    }

    @Override
    public void slotChanged(AbstractContainerMenu menu, int slotIndex, ItemStack stack) {
        refreshResult();
    }

    @Override
    public void dataChanged(AbstractContainerMenu menu, int id, int value) {
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

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (slot == null || !slot.hasItem() || slotIndex == RESULT_SLOT) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack moved = stack.copy();
        if (slotIndex < INV_START) {
            if (!this.moveItemStackTo(stack, INV_START, HOTBAR_END, true)) return ItemStack.EMPTY;
        } else if (!this.moveItemStackTo(stack, 0, INV_START, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == moved.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return moved;
    }

    /**
     * A grid slot: never while switched off; with the filter on (filter principle) an empty slot takes anything - that
     * is its recipe item - and a filled one only a match. Read from the synced data, so the client predicts the same.
     */
    private final class GridSlot extends Slot {
        GridSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            HopperFilterMode mode = filterMode();
            return !isSlotDisabled(getContainerSlot()) && super.mayPlace(stack)
                    && (mode == HopperFilterMode.NONE || !hasItem() || ItemFilter.matches(mode, getItem(), stack));
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return mayPlace(stack) ? super.getMaxStackSize(stack) : 0;
        }
    }
}

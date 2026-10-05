package com.simplebuilding.screen;

import com.simplebuilding.blocks.entity.custom.AutoSmitherBlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.level.Level;

/**
 * Crafter-style layout with three inputs and a take-only slot for finished results.
 * Recipe errors are computed by the server, independently of stored output.
 */
public class AutoSmitherMenu extends AbstractContainerMenu implements ContainerListener {
    public static final int RESULT_SLOT = AutoSmitherBlockEntity.RESULT_SLOT;
    private static final int INV_START = RESULT_SLOT + 1;
    private static final int INV_END = INV_START + 27;
    private static final int HOTBAR_END = INV_END + 9;

    private final Container container;
    private final ContainerData data;
    private final Player player;
    private final DataSlot recipeError = DataSlot.standalone();

    public AutoSmitherMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(AutoSmitherBlockEntity.SIZE), new SimpleContainerData(1));
    }

    public AutoSmitherMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(ModScreenHandlers.AUTO_SMITHER_MENU, containerId);
        checkContainerSize(container, AutoSmitherBlockEntity.SIZE);
        this.container = container;
        this.data = data;
        this.player = inventory.player;
        container.startOpen(inventory.player);
        Level level = inventory.player.level();
        this.addSlot(new InputSlot(container, AutoSmitherBlockEntity.TEMPLATE_SLOT, 26, 35, level, RecipePropertySet.SMITHING_TEMPLATE));
        this.addSlot(new InputSlot(container, AutoSmitherBlockEntity.BASE_SLOT, 44, 35, level, RecipePropertySet.SMITHING_BASE));
        this.addSlot(new InputSlot(container, AutoSmitherBlockEntity.ADDITION_SLOT, 62, 35, level, RecipePropertySet.SMITHING_ADDITION));
        this.addSlot(new Slot(container, RESULT_SLOT, 134, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addStandardInventorySlots(inventory, 8, 84);
        this.addDataSlots(data);
        this.addDataSlot(this.recipeError);
        this.addSlotListener(this);
        refreshResult();
    }

    /** Ob gerade ein Redstone-Signal anliegt (Anzeige wie beim Crafter). */
    public boolean isPowered() {
        return this.data.get(0) == 1;
    }

    private void refreshResult() {
        if (this.player instanceof ServerPlayer serverPlayer) {
            AutoSmitherBlockEntity.Outcome outcome = AutoSmitherBlockEntity.outcome(serverPlayer.level(),
                    this.container.getItem(0), this.container.getItem(1), this.container.getItem(2));
            this.recipeError.set(outcome == null && !this.container.getItem(0).isEmpty()
                    && !this.container.getItem(1).isEmpty() && !this.container.getItem(2).isEmpty() ? 1 : 0);
        }
    }

    public boolean hasRecipeError() {
        return this.recipeError.get() != 0;
    }

    @Override
    public void slotChanged(AbstractContainerMenu menu, int slotIndex, ItemStack stack) {
        if (slotIndex < RESULT_SLOT) {
            refreshResult();
        }
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
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack moved = stack.copy();
        if (slotIndex <= RESULT_SLOT) {
            if (!this.moveItemStackTo(stack, INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, 0, RESULT_SLOT, false)) {
            if (slotIndex < INV_END) {
                if (!this.moveItemStackTo(stack, INV_END, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, INV_START, INV_END, false)) {
                return ItemStack.EMPTY;
            }
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

    /** Eingang: nimmt nur, was am Schmiedetisch in denselben Slot passen wuerde. */
    private static final class InputSlot extends Slot {
        private final Level level;
        private final net.minecraft.resources.ResourceKey<RecipePropertySet> set;

        InputSlot(Container container, int index, int x, int y, Level level, net.minecraft.resources.ResourceKey<RecipePropertySet> set) {
            super(container, index, x, y);
            this.level = level;
            this.set = set;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.level.recipeAccess().propertySet(this.set).test(stack);
        }
    }
}

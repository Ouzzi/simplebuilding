package com.simplemusic.press;

import com.simplemusic.registry.MusicRegistry;
import com.simplemusic.sheet.SheetMusicItem;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Menu of the disc press: sheet and blank disc side by side on the left (owner 23.10., DP23: "nebeneinander statt
 * übereinander"), the progress arrow, the pressed disc on the right. Shift-click sends sheets and blanks to their slots.
 */
public class DiscPressMenu extends AbstractContainerMenu {
    public static final int SHEET_X = 38, SHEET_Y = 35, BLANK_X = 56, BLANK_Y = 35, OUTPUT_X = 116, OUTPUT_Y = 35;
    private final Container press;
    private final ContainerData data;

    public DiscPressMenu(int id, Inventory inventory, Container press, ContainerData data) {
        super(MusicRegistry.DISC_PRESS_MENU, id);
        checkContainerSize(press, 3);
        checkContainerDataCount(data, 2);
        this.press = press;
        this.data = data;
        press.startOpen(inventory.player);
        addSlot(new Slot(press, DiscPressBlockEntity.SHEET, SHEET_X, SHEET_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof SheetMusicItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(press, DiscPressBlockEntity.BLANK, BLANK_X, BLANK_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(MusicRegistry.BLANK_DISC);
            }
        });
        addSlot(new Slot(press, DiscPressBlockEntity.OUTPUT, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addStandardInventorySlots(inventory, 8, 84);
        addDataSlots(data);
    }

    /** Client side: an empty mirror, filled by the server's slot and data updates. */
    public static DiscPressMenu client(int id, Inventory inventory) {
        return new DiscPressMenu(id, inventory, new SimpleContainer(3), new SimpleContainerData(2));
    }

    /** Pressing progress 0..1 for the arrow. */
    public float progress() {
        int total = data.get(1);
        return total <= 0 ? 0F : Math.min(1F, data.get(0) / (float) total);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        if (index < 3) {
            if (!moveItemStackTo(stack, 3, slots.size(), true)) return ItemStack.EMPTY;
            if (index == DiscPressBlockEntity.OUTPUT) slot.onQuickCraft(stack, before);
        } else if (stack.getItem() instanceof SheetMusicItem) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (stack.is(MusicRegistry.BLANK_DISC)) {
            if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == before.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return before;
    }

    @Override
    public boolean stillValid(Player player) {
        return press.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        press.stopOpen(player);
    }
}

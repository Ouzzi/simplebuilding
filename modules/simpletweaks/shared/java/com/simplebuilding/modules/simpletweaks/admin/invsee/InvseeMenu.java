package com.simplebuilding.modules.simpletweaks.admin.invsee;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/**
 * T13/T11 screen menu: the target's inventory (armor, offhand, 27 + hotbar), ender chest (27) or a death backup (inventory
 * layout + buttons), above the viewer's own inventory. The top container is the live target container (online), a
 * container written back to the player file (offline) or a backup copy. Server-side behavior comes from {@link Invsee.Target}.
 */
public class InvseeMenu extends AbstractContainerMenu {
    public enum Kind { INVENTORY, ENDER, DEATHS }
    public static final int WIDTH = 176;
    /** Inventory layout: armor row y, main rows y, hotbar y, own inventory y. */
    public static final int ARMOR_Y = 18, DEATH_SHIFT = 26, MAIN_Y = 40, HOTBAR_Y = 98, OWN_Y = 124;
    public static final int ENDER_Y = 18, ENDER_OWN_Y = 86;
    /** Container slot numbers of the top row, left to right: head, chest, legs, feet, offhand. */
    static final int[] ARMOR_ROW = {39, 38, 37, 36, 40};
    static final EquipmentSlot[] ARMOR_SLOT = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, null};
    public static final int PREV = 0, NEXT = 1, RESTORE = 2;
    /** Death backups: info line and its own button row between the title and the slots (DEATH_SHIFT). */
    public static final int INFO_Y = 18, BUTTONS_Y = 28, BUTTON_H = 14;

    public final Kind kind;
    final int top;
    final Invsee.Target target;
    final boolean editable;

    public static int ownY(Kind kind) { return kind == Kind.ENDER ? ENDER_OWN_Y : OWN_Y + (kind == Kind.DEATHS ? DEATH_SHIFT : 0); }
    public static int height(Kind kind) { return ownY(kind) + 58 + 18 + 7; }

    /** Client side: an empty container of the right size, filled by the server's slot updates. */
    public InvseeMenu(MenuType<?> type, int id, Inventory own, Kind kind) {
        this(type, id, own, kind, new SimpleContainer(kind == Kind.ENDER ? 27 : 41), null, true);
    }
    public InvseeMenu(MenuType<?> type, int id, Inventory own, Kind kind, Container container, Invsee.Target target, boolean editable) {
        super(type, id);
        this.kind = kind; this.target = target; this.editable = editable;
        int shift = kind == Kind.DEATHS ? DEATH_SHIFT : 0;
        if (kind == Kind.ENDER) {
            for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Guarded(container, c + r * 9, 8 + c * 18, ENDER_Y + r * 18, null));
        } else {
            for (int i = 0; i < ARMOR_ROW.length; i++) addSlot(new Guarded(container, ARMOR_ROW[i], 8 + i * 18 + (i == 4 ? 6 : 0), ARMOR_Y + shift, ARMOR_SLOT[i]));
            for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Guarded(container, 9 + c + r * 9, 8 + c * 18, MAIN_Y + shift + r * 18, null));
            for (int c = 0; c < 9; c++) addSlot(new Guarded(container, c, 8 + c * 18, HOTBAR_Y + shift, null));
        }
        top = slots.size();
        int y = ownY(kind);
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(own, c + r * 9 + 9, 8 + c * 18, y + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(own, c, 8 + c * 18, y + 58));
    }

    /** A target slot: read-only without the edit right, armor slots only take fitting equipment. */
    final class Guarded extends Slot {
        private final EquipmentSlot equipment;
        Guarded(Container c, int index, int x, int y, EquipmentSlot equipment) { super(c, index, x, y); this.equipment = equipment; }
        @Override public boolean mayPlace(ItemStack stack) {
            if (!editable || kind == Kind.DEATHS) return false;
            if (equipment == null) return true;
            var eq = stack.get(DataComponents.EQUIPPABLE);
            return eq != null && eq.slot() == equipment;
        }
        @Override public boolean mayPickup(Player player) { return editable; }
        @Override public int getMaxStackSize() { return equipment == null ? super.getMaxStackSize() : 1; }
    }

    @Override public boolean stillValid(Player player) { return target == null || target.valid(player); }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        if (index < top) { if (!moveItemStackTo(stack, top, slots.size(), true)) return ItemStack.EMPTY; }
        else {
            if (kind == Kind.DEATHS || !editable) return ItemStack.EMPTY;
            int from = kind == Kind.ENDER ? 0 : ARMOR_ROW.length;
            if (!moveItemStackTo(stack, from, top, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override public boolean clickMenuButton(Player player, int id) {
        return target != null && target.button(player, id);
    }

    @Override public void broadcastChanges() {
        super.broadcastChanges();
        if (target != null) target.changed();
    }

    @Override public void removed(Player player) {
        super.removed(player);
        if (target != null) target.closed();
    }
}

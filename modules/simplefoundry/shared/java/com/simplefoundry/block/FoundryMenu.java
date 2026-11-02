package com.simplefoundry.block;

import com.simplefoundry.Fuels;
import com.simplefoundry.Melts;
import com.simplefoundry.Metal;
import com.simplefoundry.Tier;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The desk window (FOUNDRY32-UI = A): places on the left (3 / 6 / 9 / 12 by tier), the section of the tank in the middle, the stack of
 * melts on the right with the fuel tank. Plain Vanilla slots; numbers arrive through {@link ContainerData} ({@link DeskBlockEntity#data}).
 * Buttons: 0 = arrow (one layer down, wraps), 100 + n = select level n.
 */
public class FoundryMenu extends AbstractContainerMenu {
    public static MenuType<FoundryMenu> TYPE;
    public static final int WIDTH = 176, HEIGHT = 208;
    public static final int SLOT_X = 8, SLOT_Y = 22, COLUMNS = 3;
    public static final int FUEL_X = 112, FUEL_Y = 84;
    public static final int INVENTORY_Y = 126;
    public static final int ARROW_BUTTON = 0, LEVEL_BUTTON = 100;
    private static final int INV_START = DeskBlockEntity.INPUT_SLOTS + 1;

    private final ContainerData data;
    private final @Nullable DeskBlockEntity desk;

    public static FoundryMenu client(int id, Inventory inventory) {
        return new FoundryMenu(id, inventory, new SimpleContainer(DeskBlockEntity.INPUT_SLOTS + 1), new SimpleContainerData(DeskBlockEntity.DATA_COUNT), null);
    }

    /** Client menu with given window data (client smoke screenshots of the window in a known state). */
    public static FoundryMenu preview(int id, Inventory inventory, ContainerData data) {
        return new FoundryMenu(id, inventory, new SimpleContainer(DeskBlockEntity.INPUT_SLOTS + 1), data, null);
    }

    public FoundryMenu(int id, Inventory inventory, DeskBlockEntity desk) {
        this(id, inventory, desk.items, desk.data(), desk);
    }

    private FoundryMenu(int id, Inventory inventory, Container container, ContainerData data, @Nullable DeskBlockEntity desk) {
        super(TYPE, id);
        this.data = data;
        this.desk = desk;
        for (int i = 0; i < DeskBlockEntity.INPUT_SLOTS; i++) addSlot(new InputSlot(container, i, SLOT_X + (i % COLUMNS) * 18, SLOT_Y + (i / COLUMNS) * 18));
        addSlot(new Slot(container, DeskBlockEntity.FUEL_SLOT, FUEL_X, FUEL_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return Fuels.of(stack) != null;
            }
        });
        addStandardInventorySlots(inventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    /** A work place: only as many as the tier gives are active, the others show as locked. */
    private final class InputSlot extends Slot {
        private final int index;

        InputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
            this.index = index;
        }

        @Override
        public boolean isActive() {
            return index < tier().slots;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isActive() && Melts.of(stack) != null;
        }
    }

    public Tier tier() {
        return Tier.byIndex(data.get(0));
    }

    public int selected() {
        return data.get(1);
    }

    public int levels() {
        return Math.max(1, data.get(2));
    }

    public String status() {
        return DeskBlockEntity.statusId(data.get(3));
    }

    public boolean lit() {
        return data.get(4) != 0;
    }

    /** Burn ticks in the fuel tank (rounded to 16). */
    public int fuel() {
        return data.get(5) * 16;
    }

    public int capacity() {
        return data.get(6) | data.get(7) << 15;
    }

    private int pair(int group) {
        return data.get(8 + 2 * group) | data.get(9 + 2 * group) << 15;
    }

    /** Units melted per {@link Metal} ordinal. */
    public int[] melt() {
        int[] out = new int[Metal.values().length];
        for (int i = 0; i < out.length; i++) out[i] = pair(i);
        return out;
    }

    /** Units waiting to melt per {@link Metal} ordinal. */
    public int[] waiting() {
        int[] out = new int[Metal.values().length];
        for (int i = 0; i < out.length; i++) out[i] = pair(3 + i);
        return out;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (desk != null) {
            if (id == ARROW_BUTTON) desk.cycleLevel();
            else if (id >= LEVEL_BUTTON && id < LEVEL_BUTTON + 64) desk.selectLevel(id - LEVEL_BUTTON);
            else return false;
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return desk == null || !desk.isRemoved() && player.distanceToSqr(Vec3.atCenterOf(desk.getBlockPos())) <= 64;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < INV_START) {
            if (!moveItemStackTo(stack, INV_START, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean moved = Fuels.of(stack) != null && moveItemStackTo(stack, DeskBlockEntity.FUEL_SLOT, INV_START, false);
            if (!moved && Melts.of(stack) != null) moved = moveItemStackTo(stack, 0, DeskBlockEntity.INPUT_SLOTS, false);
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}

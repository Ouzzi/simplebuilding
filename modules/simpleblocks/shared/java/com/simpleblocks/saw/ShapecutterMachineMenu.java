package com.simpleblocks.saw;

import java.util.List;
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
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Menu of the Pulse and Auto Shapecutter (N62): the Shapecutter's screen and the Vanilla stonecutter's slots. Slot 0 is the block's
 * real input slot, slot 1 ({@link ShapecutterMenu#RESULT_SLOT}) a preview of what one cut makes (cannot be taken or filled), the cuts
 * in the middle choose the shape - a click stores the cut's recipe id in the block ({@link ShapecutterMachineBlockEntity#select}).
 * The chosen index comes from the block as container data; client and server build the same list ({@link Sawing#entries}).
 */
public class ShapecutterMachineMenu extends AbstractContainerMenu implements SawMenu {
    public static final int INPUT_SLOT = 0, PREVIEW_SLOT = ShapecutterMenu.RESULT_SLOT;
    private static final int INV_START = 2, INV_END = 29, HOTBAR_END = 38;

    private final @Nullable ShapecutterMachineBlockEntity machine;
    private final Container input;
    private final SimpleContainer preview = new SimpleContainer(1);
    private final ContainerData data;
    private final Level level;
    private ItemStack listedFor = ItemStack.EMPTY;
    private List<Sawing.Entry> cuts = List.of();
    private Runnable listener = () -> {};

    /** Client: a stand-in input filled by the slot sync; the listener runs when the kind of input changes. */
    public ShapecutterMachineMenu(int id, Inventory inventory) {
        this(id, inventory, null, new SimpleContainerData(ShapecutterMachineBlockEntity.DATA_COUNT));
    }

    public ShapecutterMachineMenu(int id, Inventory inventory, ShapecutterMachineBlockEntity machine) {
        this(id, inventory, machine, machine.data());
    }

    private ShapecutterMachineMenu(int id, Inventory inventory, @Nullable ShapecutterMachineBlockEntity machine, ContainerData data) {
        super(Sawing.MACHINE_MENU, id);
        checkContainerDataCount(data, ShapecutterMachineBlockEntity.DATA_COUNT);
        this.machine = machine;
        this.data = data;
        this.level = inventory.player.level();
        this.input = machine != null ? machine : new SimpleContainer(1) {
            @Override
            public void setChanged() {
                super.setChanged();
                if (ShapecutterMachineMenu.this.refreshCuts()) ShapecutterMachineMenu.this.listener.run();
            }
        };
        this.addSlot(new Slot(this.input, 0, ShapecutterMenu.INPUT_X, ShapecutterMenu.INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return Sawing.accepts(ShapecutterMachineMenu.this.level, stack);
            }
        });
        this.addSlot(new Slot(this.preview, 0, ShapecutterMenu.RESULT_X, ShapecutterMenu.RESULT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }
        });
        this.addStandardInventorySlots(inventory, 8, ShapecutterMenu.INVENTORY_Y);
        this.addDataSlots(data);
    }

    /** Rebuilds the cut list when the kind of input changed; true then. */
    private boolean refreshCuts() {
        ItemStack now = this.input.getItem(0);
        if (now.is(this.listedFor.getItem()) && now.isEmpty() == this.listedFor.isEmpty()) return false;
        this.listedFor = now.copyWithCount(1);
        this.cuts = Sawing.forInput(Sawing.entries(this.level), now);
        return true;
    }

    @Override
    public List<Sawing.Entry> cuts() {
        refreshCuts();
        return this.cuts;
    }

    @Override
    public int selectedIndex() {
        return this.data.get(ShapecutterMachineBlockEntity.DATA_SELECTED);
    }

    @Override
    public boolean hasInput() {
        return !this.input.getItem(0).isEmpty() && !cuts().isEmpty();
    }

    @Override
    public void registerUpdateListener(Runnable listener) {
        this.listener = listener;
    }

    /** The block behind this menu (server side), null on the client. */
    public @Nullable ShapecutterMachineBlockEntity machine() {
        return this.machine;
    }

    @Override
    public MenuType<?> getType() {
        return Sawing.MACHINE_MENU;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.input.stillValid(player);
    }

    /** A click on a cut chooses it for every following cut (server: stored in the block). */
    @Override
    public boolean clickMenuButton(Player player, int button) {
        List<Sawing.Entry> list = cuts();
        if (button < 0 || button >= list.size() || selectedIndex() == button) return false;
        if (this.machine != null) this.machine.select(list.get(button).id());
        return true;
    }

    /** Server: the preview slot follows the block (input, chosen cut) before every sync. */
    @Override
    public void broadcastChanges() {
        if (this.machine != null) {
            ItemStack next = this.machine.previewResult();
            if (!ItemStack.matches(next, this.preview.getItem(0))) this.preview.setItem(0, next);
        }
        super.broadcastChanges();
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return target.container != this.preview && super.canTakeItemForPickAll(carried, target);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem() || index == PREVIEW_SLOT) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack clicked = stack.copy();
        if (index == INPUT_SLOT) {
            if (!this.moveItemStackTo(stack, INV_START, HOTBAR_END, true)) return ItemStack.EMPTY;
        } else if (Sawing.accepts(this.level, stack)) {
            if (!this.moveItemStackTo(stack, INPUT_SLOT, INPUT_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (index < INV_END) {
            if (!this.moveItemStackTo(stack, INV_END, HOTBAR_END, false)) return ItemStack.EMPTY;
        } else if (!this.moveItemStackTo(stack, INV_START, INV_END, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == clicked.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return clicked;
    }
}

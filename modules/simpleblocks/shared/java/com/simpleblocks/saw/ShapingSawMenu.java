package com.simpleblocks.saw;

import java.util.List;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Menu of the Shaping Saw, built like Vanilla's StonecutterMenu: one input, a list of cuts for it ({@link Sawing#forInput}),
 * a result slot. A click on a cut is a menu button with the index in that list; the result is always made on the server.
 * The list is longer than the stonecutter's, so the screen shows 6 x 4 cuts with scrolling and a search field.
 */
public class ShapingSawMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176;
    public static final int HEIGHT = 200;
    public static final int INPUT_X = 16, INPUT_Y = 34;
    public static final int RESULT_X = 16, RESULT_Y = 80;
    public static final int INVENTORY_Y = 118;
    public static final int INPUT_SLOT = 0;
    public static final int RESULT_SLOT = 1;
    private static final int INV_START = 2, INV_END = 29, HOTBAR_END = 38;

    private final ContainerLevelAccess access;
    private final Level level;
    private final DataSlot selected = DataSlot.standalone();
    private List<Sawing.Entry> cuts = List.of();
    private ItemStack input = ItemStack.EMPTY;
    private long lastSoundTime;
    private final Slot inputSlot;
    private final Slot resultSlot;
    private Runnable listener = () -> {};
    public final Container container = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            ShapingSawMenu.this.slotsChanged(this);
            ShapingSawMenu.this.listener.run();
        }
    };
    private final ResultContainer result = new ResultContainer();

    public ShapingSawMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public ShapingSawMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(Sawing.MENU, id);
        this.access = access;
        this.level = inventory.player.level();
        this.inputSlot = this.addSlot(new Slot(this.container, 0, INPUT_X, INPUT_Y));
        this.resultSlot = this.addSlot(new Slot(this.result, 1, RESULT_X, RESULT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack carried) {
                carried.onCraftedBy(player, carried.getCount());
                ItemStack remaining = ShapingSawMenu.this.inputSlot.remove(1);
                if (!remaining.isEmpty()) ShapingSawMenu.this.setupResult(ShapingSawMenu.this.selected.get());
                access.execute((level, pos) -> {
                    long time = level.getGameTime();
                    if (ShapingSawMenu.this.lastSoundTime != time) {
                        level.playSound(null, pos, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.0F);
                        ShapingSawMenu.this.lastSoundTime = time;
                    }
                });
                super.onTake(player, carried);
            }
        });
        this.addStandardInventorySlots(inventory, 8, INVENTORY_Y);
        this.addDataSlot(this.selected);
    }

    public int selectedIndex() {
        return this.selected.get();
    }

    /** The cuts for the current input, in button order. */
    public List<Sawing.Entry> cuts() {
        return this.cuts;
    }

    public boolean hasInput() {
        return this.inputSlot.hasItem() && !this.cuts.isEmpty();
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, Sawing.SAW);
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (this.selected.get() == button) return false;
        if (button >= 0 && button < this.cuts.size()) {
            this.selected.set(button);
            this.setupResult(button);
        }
        return true;
    }

    @Override
    public void slotsChanged(Container changed) {
        ItemStack now = this.inputSlot.getItem();
        if (!now.is(this.input.getItem())) {
            this.input = now.copy();
            this.selected.set(-1);
            this.resultSlot.set(ItemStack.EMPTY);
            this.cuts = Sawing.forInput(Sawing.entries(this.level), now);
        }
    }

    private void setupResult(int index) {
        if (index >= 0 && index < this.cuts.size() && this.inputSlot.hasItem()) {
            this.resultSlot.set(this.cuts.get(index).resultStack());
        } else {
            this.resultSlot.set(ItemStack.EMPTY);
        }
        this.broadcastChanges();
    }

    public void registerUpdateListener(Runnable listener) {
        this.listener = listener;
    }

    @Override
    public MenuType<?> getType() {
        return Sawing.MENU;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return target.container != this.result && super.canTakeItemForPickAll(carried, target);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return clicked;
        ItemStack stack = slot.getItem();
        clicked = stack.copy();
        if (index == RESULT_SLOT) {
            stack.getItem().onCraftedBy(stack, player);
            if (!this.moveItemStackTo(stack, INV_START, HOTBAR_END, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, clicked);
        } else if (index == INPUT_SLOT) {
            if (!this.moveItemStackTo(stack, INV_START, HOTBAR_END, false)) return ItemStack.EMPTY;
        } else if (Sawing.accepts(this.level, stack)) {
            if (!this.moveItemStackTo(stack, INPUT_SLOT, INPUT_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (index < INV_END) {
            if (!this.moveItemStackTo(stack, INV_END, HOTBAR_END, false)) return ItemStack.EMPTY;
        } else if (!this.moveItemStackTo(stack, INV_START, INV_END, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        slot.setChanged();
        if (stack.getCount() == clicked.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        if (index == RESULT_SLOT) player.drop(stack, false, Prediction.PREDICTED);
        this.broadcastChanges();
        return clicked;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.result.removeItemNoUpdate(1);
        this.access.execute((level, pos) -> this.clearContainer(player, this.container));
    }
}

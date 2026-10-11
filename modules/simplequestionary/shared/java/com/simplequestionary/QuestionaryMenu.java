package com.simplequestionary;

import com.simplequestionary.net.FactsPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * One item slot (one piece, like the enchanting table) and the player inventory. Putting an item in makes the server
 * send the recipe facts for the "Use" page ({@link QuestionaryFacts}); closing gives the item back.
 */
public class QuestionaryMenu extends AbstractContainerMenu {
    /** Layout shared with the screen: the left box is a Vanilla-wide container, the book sits to its right. The whole
     *  width must stay under 427 (an 854 px window at GUI scale 2), the height under 240. */
    public static final int LEFT_WIDTH = 176, LEFT_HEIGHT = 184, WIDTH = 424, HEIGHT = 212;
    public static final int SLOT_X = 80, SLOT_Y = 26, INVENTORY_X = 8, INVENTORY_Y = 102;
    public static final int ITEM_SLOT = 0, INVENTORY_START = 1, INVENTORY_END = 37;

    private final ContainerLevelAccess access;
    private final Player player;
    private final Container item = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
        }
    };

    /** Client side (menu type factory). */
    public QuestionaryMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public QuestionaryMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(QuestionaryRegistry.MENU, id);
        this.access = access;
        this.player = inventory.player;
        addSlot(new Slot(item, 0, SLOT_X, SLOT_Y) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addStandardInventorySlots(inventory, INVENTORY_X, INVENTORY_Y);
    }

    public ItemStack shown() {
        return item.getItem(0);
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container != item) return;
        access.execute((level, pos) -> {
            if (player instanceof ServerPlayer server && QuestionaryConfig.autoFacts) {
                SimpleQuestionary.toClient.accept(server, FactsPayload.of(QuestionaryFacts.compute(level, item.getItem(0))));
            }
        });
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        if (index == ITEM_SLOT) {
            if (!moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, true)) return ItemStack.EMPTY;
        } else {
            if (slots.get(ITEM_SLOT).hasItem() || stack.isEmpty()) return ItemStack.EMPTY;
            slots.get(ITEM_SLOT).setByPlayer(stack.split(1));
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == before.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return before;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> clearContainer(player, item));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, QuestionaryRegistry.TABLE);
    }
}

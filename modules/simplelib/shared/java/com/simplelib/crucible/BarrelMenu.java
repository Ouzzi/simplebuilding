package com.simplelib.crucible;

import com.simplelib.api.StackLimits;
import com.simplelib.registry.LibMenus;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A loose barrel's chest menu (owner N15): Vanilla's layout and screen, but the barrel's slots keep its raised
 * stack limit (Enderite 128) on the server and in the client mirror, so display, clicks and shift-clicks agree.
 * One menu type per barrel tier tells the client the size and the multiplier.
 */
public class BarrelMenu extends ChestMenu {
    public BarrelMenu(BarrelTier tier, int id, Inventory inventory, Container barrel) {
        super(LibMenus.forBarrel(tier), id, inventory, barrel, tier.rows());
        // ChestMenu adds plain slots first (they cap at the item's normal stack); swap in limited ones at the same place.
        for (int i = 0; i < tier.slots(); i++) {
            Slot plain = slots.get(i);
            Slot limited = new StackLimits.LimitedSlot(barrel, plain.getContainerSlot(), plain.x, plain.y);
            limited.index = plain.index;
            slots.set(i, limited);
        }
    }

    /** Client constructor: a mirror with the tier's stack limit. */
    public static BarrelMenu client(BarrelTier tier, int id, Inventory inventory) {
        return new BarrelMenu(tier, id, inventory, StackLimits.mirror(tier.slots(), tier::stackMultiplier));
    }

    /**
     * Forge's moveItemStackTo caps a merge into an existing stack at the item's normal size; top up matching
     * stacks to the slot's raised limit first, then let the loader's own code place the rest.
     */
    @Override
    protected boolean moveItemStackTo(ItemStack stack, int start, int end, boolean reverse) {
        boolean moved = false;
        if (stack.getMaxStackSize() > 1) {
            for (int i = reverse ? end - 1 : start; reverse ? i >= start : i < end; i += reverse ? -1 : 1) {
                if (stack.isEmpty()) break;
                Slot slot = slots.get(i);
                ItemStack target = slot.getItem();
                if (target.isEmpty() || !ItemStack.isSameItemSameComponents(stack, target)) continue;
                int n = Math.min(slot.getMaxStackSize(target) - target.getCount(), stack.getCount());
                if (n > 0) {
                    stack.shrink(n);
                    target.grow(n);
                    slot.setChanged();
                    moved = true;
                }
            }
        }
        return stack.isEmpty() ? moved : super.moveItemStackTo(stack, start, end, reverse) || moved;
    }
}

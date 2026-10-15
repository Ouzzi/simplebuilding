package com.simpleshops.block;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A {@link Container} view on a block entity's item list, used only by the shop menus. The block entities themselves are
 * deliberately NOT containers: Vanilla hoppers, droppers, golems and item transfer APIs must not see a shop.
 */
public final class ListContainer implements Container {
    private final NonNullList<ItemStack> items;
    private final Runnable changed;

    public ListContainer(NonNullList<ItemStack> items, Runnable changed) {
        this.items = items;
        this.changed = changed;
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        return this.items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack stack = this.items.get(slot);
        if (stack.isEmpty() || count <= 0) return ItemStack.EMPTY;
        ItemStack out = stack.split(count);
        if (!out.isEmpty()) setChanged();
        return out;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        setChanged();
    }

    @Override
    public void setChanged() {
        this.changed.run();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        this.items.clear();
        for (int i = 0; i < this.items.size(); i++) this.items.set(i, ItemStack.EMPTY);
        setChanged();
    }
}

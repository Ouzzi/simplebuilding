package com.simplequalityoflife.container;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Duck interface on {@code AbstractContainerMenu} (see {@code ContainerMenuLinkedMixin}). */
public interface LinkedMenu {
    /** Appends a slot behind every slot the menu created itself, so the menu's own indices stay unchanged. */
    void qol$addSlot(Slot slot);

    /** Vanilla {@code moveItemStackTo} of this menu. */
    boolean qol$move(ItemStack stack, int start, int end, boolean backwards);

    /** Drops every slot from {@code from} on (slots, last and remote copies). */
    void qol$truncate(int from);

    /** Server: the linked container session of this menu, or null. */
    @Nullable LinkedContainers.Session qol$session();

    void qol$session(@Nullable LinkedContainers.Session session);

    /** Client: the panel state (layout, scroll) of the linked container, or null. */
    @Nullable Object qol$panel();

    void qol$panel(@Nullable Object panel);
}

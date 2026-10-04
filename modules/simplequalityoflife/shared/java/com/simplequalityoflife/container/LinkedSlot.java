package com.simplequalityoflife.container;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A slot of the marked container, appended to a second menu. On the server every access checks the
 * link (range, dimension, loaded chunk, block entity still in place), so a broken or far container can
 * never hand out or take items. While the second menu's own shift-click runs, the slot takes nothing
 * ({@link #SUPPRESSED}), because many menus move "to the player" over {@code slots.size()}.
 */
public final class LinkedSlot extends Slot {
    static final ThreadLocal<Boolean> SUPPRESSED = ThreadLocal.withInitial(() -> false);

    /** Server only; null on the client, whose container is a plain mirror. */
    private final @Nullable LinkedContainers.Session session;
    /** Client only: false while the row is scrolled out of the panel. */
    public boolean shown = true;

    public LinkedSlot(Container container, int slot, @Nullable LinkedContainers.Session session) {
        super(container, slot, 0, 0);
        this.session = session;
    }

    private boolean usable() {
        return !SUPPRESSED.get() && (this.session == null || this.session.valid());
    }

    @Override
    public boolean mayPickup(Player player) {
        return this.usable() && super.mayPickup(player);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return this.usable() && this.container.canPlaceItem(this.getContainerSlot(), stack);
    }

    @Override
    public int getMaxStackSize() {
        return this.usable() ? super.getMaxStackSize() : 0;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return this.usable() ? super.getMaxStackSize(stack) : 0;
    }

    @Override
    public boolean isActive() {
        return this.shown;
    }
}

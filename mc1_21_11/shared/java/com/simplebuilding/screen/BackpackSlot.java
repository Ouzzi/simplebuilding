package com.simplebuilding.screen;

import com.simplebuilding.items.custom.BackpackItem;
import java.util.Optional;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Ein Rucksack-Slot. Mit Tiefe Taschen haelt er mehr als einen normalen Stapel - aber nichts, was
 * ihn verlaesst, darf groesser sein als ein normaler Stapel: der Cursor, die Hotbar und ein
 * geworfenes Item-Entity kennen nur Vanilla-Grenzen (und ein Item-Entity ueber 99 laesst sich nicht
 * einmal speichern).
 *
 * <p>Deshalb nimmt {@link #tryRemove} hoechstens einen normalen Stapel auf einmal heraus. Das deckt
 * Aufnehmen (Links- und Rechtsklick), Werfen (Q, Strg+Q in Stapeln) und Doppelklick-Sammeln ab;
 * den Tausch per Zifferntaste und den Tausch mit einem anderen Cursor-Stapel faengt
 * {@link BackpackMenu#clicked} ab.
 */
public class BackpackSlot extends Slot {
    private final BackpackContainer backpack;

    public BackpackSlot(BackpackContainer container, int slot, int x, int y) {
        super(container, slot, x, y);
        this.backpack = container;
    }

    public boolean isExtraColumn() {
        return this.backpack.tier().isColumnSlot(getContainerSlot());
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return BackpackItem.mayStore(stack);
    }

    @Override
    public int getMaxStackSize() {
        return this.backpack.getMaxStackSize();
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return BackpackItem.maxStackSizeIn(stack, this.backpack.multiplier());
    }

    @Override
    public Optional<ItemStack> tryRemove(int amount, int maxAmount, Player player) {
        ItemStack current = getItem();
        int limit = current.isEmpty() ? amount : Math.min(amount, Math.max(1, current.getMaxStackSize()));
        return super.tryRemove(limit, maxAmount, player);
    }
}

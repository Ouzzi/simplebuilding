package com.simplebuilding.screen;

import com.simplebuilding.items.custom.BackpackItem;
import java.util.Optional;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Ein Platz einer Mod-Truhe. Ab Netherit haelt er mehr als einen normalen Stapel (x2/x4), aber
 * nichts, was ihn verlaesst, ist groesser als ein normaler Stapel - genau wie {@link BackpackSlot}:
 * der Cursor, das Inventar und ein geworfenes Item kennen nur Vanilla-Grenzen. Die Grenze kommt
 * aus dem Menue (der Stufe), nicht aus dem Container: eine Doppeltruhe ist ein Vanilla-
 * {@code CompoundContainer}, und der kennt nur die normale Stapelgroesse.
 */
public class TieredChestSlot extends Slot {
    private final int multiplier;
    private final boolean shulkerBox;

    public TieredChestSlot(Container container, int slot, int x, int y, int multiplier) {
        this(container, slot, x, y, multiplier, false);
    }

    /** {@code shulkerBox}: a slot of a tier shulker box, which refuses what vanilla's {@code ShulkerBoxSlot} refuses. */
    public TieredChestSlot(Container container, int slot, int x, int y, int multiplier, boolean shulkerBox) {
        super(container, slot, x, y);
        this.multiplier = multiplier;
        this.shulkerBox = shulkerBox;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return !this.shulkerBox || com.simplebuilding.blocks.entity.custom.TieredShulkerBoxBlockEntity.canHold(stack);
    }

    @Override
    public int getMaxStackSize() {
        return 99 * this.multiplier;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return BackpackItem.maxStackSizeIn(stack, this.multiplier);
    }

    @Override
    public Optional<ItemStack> tryRemove(int amount, int maxAmount, Player player) {
        ItemStack current = getItem();
        int limit = current.isEmpty() ? amount : Math.min(amount, Math.max(1, current.getMaxStackSize()));
        return super.tryRemove(limit, maxAmount, player);
    }
}

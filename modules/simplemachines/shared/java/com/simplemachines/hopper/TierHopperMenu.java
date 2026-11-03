package com.simplemachines.hopper;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The mod hopper menu (SimpleBuilding's {@code ModHopperScreenHandler} + {@code NetheriteHopperScreenHandler}, stage 3) for a hopper
 * block or a hopper cart. One menu type per tier ({@link Hoppers#MENUS}), so the client knows the tier without opening data. The
 * filter key is menu button {@value #FILTER_BUTTON} (Vanilla's {@code ServerboundContainerButtonClickPacket}, like the stonecutter).
 * Layout of the 26.3 container style (owner N23: the five slots, the filter caption and the key centred; 6 px more room under the row).
 */
public class TierHopperMenu extends HopperMenu {
    /** Menu button id of the filter key. */
    public static final int FILTER_BUTTON = 0;
    /** x of the first hopper slot (the row of five, the gap with the caption and the key centred). */
    public static final int FIRST_SLOT_X = 24;
    /** x of the filter key: after the five slots and a gap one slot wider than Vanilla's (4 + 18 px). */
    public static final int FILTER_BUTTON_X = FIRST_SLOT_X - 1 + 5 * 18 + 22;
    public static final int FILTER_BUTTON_Y = 19;
    /** The player's slots sit 6 px lower than in Vanilla's hopper (owner 2026-10-20: more padding under the row). */
    public static final int INVENTORY_SHIFT = 6;
    /** Height of the screen image: Vanilla's 133 plus {@link #INVENTORY_SHIFT}. */
    public static final int IMAGE_HEIGHT = 133 + INVENTORY_SHIFT;

    private final HopperTier tier;
    /** The hopper behind the menu (block or cart); null on the client. */
    private final @Nullable FilterHopper hopper;
    private final ContainerData data;

    /** Client. */
    public TierHopperMenu(HopperTier tier, int syncId, Inventory playerInventory) {
        this(tier, syncId, playerInventory, new SimpleContainer(5), null, new SimpleContainerData(1));
    }

    /** Server: {@code container} and {@code hopper} are the same block entity or cart. */
    public TierHopperMenu(HopperTier tier, int syncId, Inventory playerInventory, Container container, FilterHopper hopper) {
        this(tier, syncId, playerInventory, container, hopper, hopper.getPropertyDelegate());
    }

    private TierHopperMenu(HopperTier tier, int syncId, Inventory playerInventory, Container container, @Nullable FilterHopper hopper, ContainerData data) {
        super(syncId, playerInventory, container);
        this.tier = tier;
        this.hopper = hopper;
        this.data = data;
        // HopperMenu adds five plain slots whose mayPlace is always true; they are swapped for filtering ones, so the filter holds
        // for every kind of click Vanilla knows (click, hotbar swap, drag, shift click).
        for (int index = 0; index < HopperMenu.CONTAINER_SIZE; index++) {
            Slot vanilla = this.slots.get(index);
            Slot filtered = new FilterSlot(hopper, vanilla.container, vanilla.getContainerSlot(), FIRST_SLOT_X + index * 18, vanilla.y);
            filtered.index = vanilla.index;
            this.slots.set(index, filtered);
        }
        // The player's slots (same index, same inventory) INVENTORY_SHIFT lower: room under the hopper row.
        for (int index = HopperMenu.CONTAINER_SIZE; index < this.slots.size(); index++) {
            Slot vanilla = this.slots.get(index);
            Slot moved = new Slot(vanilla.container, vanilla.getContainerSlot(), vanilla.x, vanilla.y + INVENTORY_SHIFT);
            moved.index = vanilla.index;
            this.slots.set(index, moved);
        }
        this.addDataSlots(data);
    }

    public HopperTier tier() {
        return tier;
    }

    /** The hopper whose filter this menu shows (null on the client). */
    public @Nullable FilterHopper filterHopper() {
        return hopper;
    }

    public HopperFilterMode getSyncedFilterMode() {
        return HopperFilterMode.of(this.data.get(0));
    }

    @Override
    public MenuType<?> getType() {
        return Hoppers.MENUS.get(tier);
    }

    /** The filter key; the server checks that this menu is the player's open one. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != FILTER_BUTTON || hopper == null) return false;
        hopper.toggleFilterMode();
        return true;
    }

    /**
     * One of the five hopper slots. With a filter on (docs/ai/PRINZIPIEN-FILTER.md) an empty slot takes anything - that sets the
     * filter - and a filled one only what matches. Taking out stays free.
     */
    private static final class FilterSlot extends Slot {
        private final @Nullable FilterHopper hopper;

        private FilterSlot(@Nullable FilterHopper hopper, Container container, int slot, int x, int y) {
            super(container, slot, x, y);
            this.hopper = hopper;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            // super first: slot mixins of other mods hang there.
            return super.mayPlace(stack) && (hopper == null || hopper.mayPlayerPlace(getContainerSlot(), stack));
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            // moveItemStackTo stacks onto a filled slot without asking mayPlace; a slot whose content the filter refuses has no room.
            return mayPlace(stack) ? super.getMaxStackSize(stack) : 0;
        }
    }
}

package com.simplebuilding.screen;

import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.version.McVersion;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ModHopperScreenHandler extends HopperMenu {

    /**
     * x of the first hopper slot. The 26.3 container style (ModScreenStyle, the line with SimpleLib) centres the row
     * of five slots, the gap with the filter caption and the filter key (owner N23: "Gesamtblock mittig"); 26.2 keeps
     * Vanilla's 44, which its hopper.png background has the slot frames for.
     */
    public static final int FIRST_SLOT_X = McVersion.CRUCIBLE ? 24 : 44;
    /** x of the filter key: after the five slots and a gap one slot wider than before (4 + 18 px). */
    public static final int FILTER_BUTTON_X = McVersion.CRUCIBLE ? FIRST_SLOT_X - 1 + 5 * 18 + 22 : 44 + 5 * 18 + 4;
    public static final int FILTER_BUTTON_Y = 19;

    private final ModHopperBlockEntity blockEntity;

    // Client
    public ModHopperScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(5), null);
    }

    // Server
    public ModHopperScreenHandler(int syncId, Inventory playerInventory, Container inventory, ModHopperBlockEntity blockEntity) {
        super(syncId, playerInventory, inventory);
        this.blockEntity = blockEntity;
        // HopperMenu legt seine fuenf Slots als schlichte Vanilla-Slots an, deren mayPlace konstant true sagt - den
        // Filter des Blocks fragt das Menue damit nie. Shift-Klick, Hotbar-Tausch und Zieh-Verteilung legten deshalb
        // filterfremdes Material in die Trichterslots. Da HopperMenu die Slots schon im eigenen Konstruktor addiert,
        // werden sie hier nachtraeglich gegen filternde ausgetauscht (auf dem Client ohne Blockentitaet: nur die neue
        // Lage); damit gilt der Filter fuer jede Klickart, die Vanilla kennt.
        for (int index = 0; index < HopperMenu.CONTAINER_SIZE; index++) {
            Slot vanilla = this.slots.get(index);
            Slot filtered = new FilterSlot(blockEntity, vanilla.container, vanilla.getContainerSlot(),
                    FIRST_SLOT_X + index * 18, vanilla.y);
            filtered.index = vanilla.index;
            this.slots.set(index, filtered);
        }
    }

    public ModHopperBlockEntity getBlockEntity() {
        return blockEntity;
    }

    /**
     * One of the five hopper slots. With a filter on (filter principle, docs/ai/PRINZIPIEN-FILTER.md) the real item in
     * a slot is its filter: an empty slot takes anything - that sets the filter - and a filled one only what matches.
     * Taking out stays free: the last item may be taken as well, which clears that slot's filter.
     *
     * <p>Vanilla asks {@code mayPlace} in every branch that puts an item into a slot - click, hotbar swap, drag and the
     * shift click that fills an empty slot.
     */
    private static final class FilterSlot extends Slot {

        private final @Nullable ModHopperBlockEntity hopper;

        private FilterSlot(@Nullable ModHopperBlockEntity hopper, Container container, int slot, int x, int y) {
            super(container, slot, x, y);
            this.hopper = hopper;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            // super zuerst: dort haengen Slot-Mixins wie SpawnElytraSlotMixin (keine Spawn-Elytra in
            // Container-Slots) - ohne den Aufruf nahm der Mod-Trichter sie an (Audit N6).
            return super.mayPlace(stack) && (hopper == null || hopper.mayPlayerPlace(getContainerSlot(), stack));
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            // moveItemStackTo stapelt beim Shift-Klick auf einen belegten Slot, ohne mayPlace zu
            // fragen - es liest nur, wie viel dort noch hineinpasst. Ein Slot, dessen Inhalt der
            // Filter ablehnt, hat also keinen Platz mehr.
            return mayPlace(stack) ? super.getMaxStackSize(stack) : 0;
        }
    }
}

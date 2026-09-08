package com.simplebuilding.screen;

import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.util.HopperFilterMode;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ModHopperScreenHandler extends HopperMenu {
    
    private final ModHopperBlockEntity blockEntity;

    // Client
    public ModHopperScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(5), null);
    }

    // Server
    public ModHopperScreenHandler(int syncId, Inventory playerInventory, Container inventory, ModHopperBlockEntity blockEntity) {
        super(syncId, playerInventory, inventory);
        this.blockEntity = blockEntity;
        // Falls du einen eigenen ScreenHandlerType hast, hier überschreiben, sonst Vanilla Typ nutzen

        // HopperMenu legt seine fünf Slots als schlichte Vanilla-Slots an, deren mayPlace
        // konstant true sagt - den Filter des Blocks fragt das Menü damit nie. Shift-Klick,
        // Hotbar-Tausch und Zieh-Verteilung legten deshalb filterfremdes Material in die
        // Trichterslots, obwohl der Einzelklick unten abgefangen wird. Da HopperMenu die Slots
        // schon im eigenen Konstruktor addiert, werden sie hier nachträglich gegen filternde
        // ausgetauscht; damit gilt der Filter für jede Klickart, die Vanilla kennt.
        if (blockEntity != null) {
            for (int index = 0; index < HopperMenu.CONTAINER_SIZE; index++) {
                Slot vanilla = this.slots.get(index);
                Slot filtered = new FilterSlot(blockEntity, vanilla.container,
                        vanilla.getContainerSlot(), vanilla.x, vanilla.y);
                filtered.index = vanilla.index;
                this.slots.set(index, filtered);
            }
        }
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput actionType, Player player) {
        // Prüfen ob Klick im Hopper Inventar (Slots 0-4)
        if (slotIndex >= 0 && slotIndex < 5 && blockEntity != null) {
            HopperFilterMode mode = blockEntity.getFilterMode();

            // Nur wenn Filter NICHT 'None' ist, greifen wir ein
            if (mode != HopperFilterMode.NONE) {
                ItemStack cursor = getCarried();
                
                // Klick mit Item -> Setze Ghost
                // Klick ohne Item -> Lösche Ghost
                if (actionType == ContainerInput.PICKUP) {
                    blockEntity.setGhostItem(slotIndex, cursor.isEmpty() ? ItemStack.EMPTY : cursor);
                    // Abbrechen, damit Item nicht wirklich reingelegt wird
                    return; 
                }
            }
        }
        super.clicked(slotIndex, button, actionType, player);
    }
    
    public ModHopperBlockEntity getBlockEntity() {
        return blockEntity;
    }

    /**
     * Einer der fünf Trichterslots: er fragt den Filter des Blocks, bevor er etwas annimmt.
     *
     * <p>Vanilla prüft {@code mayPlace} in jedem Zweig, der ein Item in einen Slot legt -
     * Hotbar-Tausch, Zieh-Verteilung und der Shift-Klick, der einen leeren Slot füllt.
     * Herausnehmen bleibt unberührt: ein Trichterslot gibt seinen Inhalt weiter her, auch
     * wenn der Filter ihn heute nicht mehr annehmen würde.
     */
    private static final class FilterSlot extends Slot {

        private final ModHopperBlockEntity hopper;

        private FilterSlot(ModHopperBlockEntity hopper, Container container, int slot, int x, int y) {
            super(container, slot, x, y);
            this.hopper = hopper;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return hopper.canPlaceItem(getContainerSlot(), stack);
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
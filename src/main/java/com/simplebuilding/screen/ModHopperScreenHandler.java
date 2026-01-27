package com.simplebuilding.screen;

import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.util.HopperFilterMode;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.HopperScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;

public class ModHopperScreenHandler extends HopperScreenHandler {
    
    private final ModHopperBlockEntity blockEntity;

    // Client
    public ModHopperScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(5), null);
    }

    // Server
    public ModHopperScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, ModHopperBlockEntity blockEntity) {
        super(syncId, playerInventory, inventory);
        this.blockEntity = blockEntity;
        // Falls du einen eigenen ScreenHandlerType hast, hier überschreiben, sonst Vanilla Typ nutzen
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        // Prüfen ob Klick im Hopper Inventar (Slots 0-4)
        if (slotIndex >= 0 && slotIndex < 5 && blockEntity != null) {
            HopperFilterMode mode = blockEntity.getFilterMode();

            // Nur wenn Filter NICHT 'None' ist, greifen wir ein
            if (mode != HopperFilterMode.NONE) {
                ItemStack cursor = getCursorStack();
                
                // Klick mit Item -> Setze Ghost
                // Klick ohne Item -> Lösche Ghost
                if (actionType == SlotActionType.PICKUP) {
                    blockEntity.setGhostItem(slotIndex, cursor.isEmpty() ? ItemStack.EMPTY : cursor);
                    // Abbrechen, damit Item nicht wirklich reingelegt wird
                    return; 
                }
            }
        }
        super.onSlotClick(slotIndex, button, actionType, player);
    }
    
    public ModHopperBlockEntity getBlockEntity() {
        return blockEntity;
    }
}
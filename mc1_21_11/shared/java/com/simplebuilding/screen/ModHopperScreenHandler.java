package com.simplebuilding.screen;

import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.util.HopperFilterMode;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.HopperMenu;
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
    }

    @Override
    public void clicked(int slotIndex, int button, ClickType actionType, Player player) {
        // Prüfen ob Klick im Hopper Inventar (Slots 0-4)
        if (slotIndex >= 0 && slotIndex < 5 && blockEntity != null) {
            HopperFilterMode mode = blockEntity.getFilterMode();

            // Nur wenn Filter NICHT 'None' ist, greifen wir ein
            if (mode != HopperFilterMode.NONE) {
                ItemStack cursor = getCarried();
                
                // Klick mit Item -> Setze Ghost
                // Klick ohne Item -> Lösche Ghost
                if (actionType == ClickType.PICKUP) {
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
}
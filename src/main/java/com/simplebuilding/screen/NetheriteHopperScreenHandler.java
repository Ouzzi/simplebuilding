package com.simplebuilding.screen;

import com.simplebuilding.blocks.entity.custom.NetheriteHopperBlockEntity;
import com.simplebuilding.util.HopperFilterMode;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public class NetheriteHopperScreenHandler extends ModHopperScreenHandler {

    private final NetheriteHopperBlockEntity blockEntity;

    public NetheriteHopperScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, NetheriteHopperBlockEntity blockEntity) {
        super(syncId, playerInventory, inventory); // Ich nehme an, dein ModHopperScreenHandler hat diesen Super-Konstruktor
        this.blockEntity = blockEntity;
    }
    
    // Konstruktor für Client (wird vom ScreenFactory aufgerufen)
    // Du musst sicherstellen, dass dieser übergeben wird oder Client-seitig gecastet wird
    // Oft ist inventory hier ein SimpleInventory auf dem Client.

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        // Prüfen, ob wir auf einen der 5 Hopper Slots klicken
        if (slotIndex >= 0 && slotIndex < 5) {
            HopperFilterMode mode = blockEntity.getFilterMode(slotIndex);

            // Wenn Filter aktiv ist (EXACT oder TYPE)
            if (mode != HopperFilterMode.NONE) {
                // Zugriff auf Ghost Item Logik
                // Client und Server führen das aus. Server speichert.
                
                ItemStack cursorStack = getCursorStack();
                ItemStack currentGhost = blockEntity.getGhostItem(slotIndex);
                
                // Logik:
                // 1. Wenn Cursor Item hat -> Setze als Ghost (überschreiben)
                // 2. Wenn Cursor leer -> Lösche Ghost (oder nimm Ghost auf? User sagte "Klick setzt Ghost")
                // User requirement: "drauf klickt werden nicht hereingelegt, sondern da wird ein Ghost item erscheinen"
                // "shift klickt auf ein Item, was bereits gefiltert ist im Hopper soll es auch reingelegt werden können"

                if (actionType == SlotActionType.PICKUP) {
                    if (!cursorStack.isEmpty()) {
                        // Setze Ghost Item
                        blockEntity.setGhostItem(slotIndex, cursorStack);
                        return; // Abbruch, kein echtes Item reinlegen
                    } else {
                        // Cursor leer -> Ghost löschen?
                        // Oder Ghost "nehmen"? User sagte "Tooltips... beim ersten Klick Ghost item erscheinen".
                        // Machen wir: Leer Klick löscht Ghost.
                        blockEntity.setGhostItem(slotIndex, ItemStack.EMPTY);
                        return; 
                    }
                } 
                
                // Spezialfall: Shift-Click aus Inventar (Quick Move) wird von `quickMove` (transferSlot) behandelt, nicht hier.
                // Aber wenn man im Hopper-Slot shift-klickt (um Ghost zu löschen?), handhaben wir das oben.
            }
        }

        super.onSlotClick(slotIndex, button, actionType, player);
    }
    
    // Für GUI Rendering Zugriff
    public NetheriteHopperBlockEntity getBlockEntity() {
        return blockEntity;
    }
}
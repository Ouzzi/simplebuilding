package com.simplebuilding.screen;

import com.simplebuilding.blocks.entity.custom.NetheriteHopperBlockEntity;
import com.simplebuilding.util.HopperFilterMode;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.HopperScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;

public class NetheriteHopperScreenHandler extends HopperScreenHandler {

    private final NetheriteHopperBlockEntity blockEntity;

    public NetheriteHopperScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(5), null);
    }

    public NetheriteHopperScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, NetheriteHopperBlockEntity blockEntity) {
        super(syncId, playerInventory, inventory);
        this.blockEntity = blockEntity;
    }

    @Override
    public ScreenHandlerType<?> getType() {
        // FIX: ModScreenHandlers Importieren und sicherstellen!
        return com.simplebuilding.screen.ModScreenHandlers.NETHERITE_HOPPER_SCREEN_HANDLER;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (blockEntity != null && slotIndex >= 0 && slotIndex < 5) {
            HopperFilterMode mode = blockEntity.getFilterMode(slotIndex);

            if (mode != HopperFilterMode.NONE) {
                // FIX: getCursorStack() kommt vom ScreenHandler (this)
                ItemStack cursorStack = getCursorStack();

                if (actionType == SlotActionType.PICKUP) {
                    if (!cursorStack.isEmpty()) {
                        blockEntity.setGhostItem(slotIndex, cursorStack);
                        return;
                    } else {
                        blockEntity.setGhostItem(slotIndex, ItemStack.EMPTY);
                        return;
                    }
                }
            }
        }
        super.onSlotClick(slotIndex, button, actionType, player);
    }

    public NetheriteHopperBlockEntity getBlockEntity() {
        return blockEntity;
    }
}
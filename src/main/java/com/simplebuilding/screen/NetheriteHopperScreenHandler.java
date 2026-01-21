package com.simplebuilding.screen;

import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.screen.ScreenHandlerType;

public class NetheriteHopperScreenHandler extends ModHopperScreenHandler {

    // Client Constructor
    public NetheriteHopperScreenHandler(int syncId, PlayerInventory playerInventory) {
        super(syncId, playerInventory, new SimpleInventory(5), null);
    }

    // Server Constructor
    public NetheriteHopperScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, ModHopperBlockEntity blockEntity) {
        super(syncId, playerInventory, inventory, blockEntity);
    }

    @Override
    public ScreenHandlerType<?> getType() {
        return ModScreenHandlers.NETHERITE_HOPPER_SCREEN_HANDLER;
    }
}
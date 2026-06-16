package com.simplebuilding.platform;

import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class HopperMenus {
    private HopperMenus() {
    }

    public static void openMenu(ServerPlayer player, ModHopperBlockEntity blockEntity) {
        player.openMenu(new ExtendedMenuProvider<>() {
            @Override
            public Component getDisplayName() {
                return blockEntity.getDisplayName();
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player ignored) {
                return blockEntity.createScreenMenu(syncId, playerInventory);
            }

            @Override
            public BlockPos getScreenOpeningData(ServerPlayer serverPlayer) {
                return blockEntity.getBlockPos();
            }
        });
    }
}

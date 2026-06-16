package com.simplebuilding.platform;

import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.common.extensions.IForgeServerPlayer;

public final class HopperMenus {
    private HopperMenus() {
    }

    public static void openMenu(ServerPlayer player, ModHopperBlockEntity blockEntity) {
        ((IForgeServerPlayer) player).openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return blockEntity.getDisplayName();
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player ignored) {
                return blockEntity.createScreenMenu(syncId, playerInventory);
            }
        }, (FriendlyByteBuf buffer) -> buffer.writeBlockPos(blockEntity.getBlockPos()));
    }
}

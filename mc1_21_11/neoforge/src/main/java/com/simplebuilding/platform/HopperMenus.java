package com.simplebuilding.platform;

import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.common.extensions.IPlayerExtension;

public final class HopperMenus {
    private HopperMenus() {
    }

    public static void openMenu(ServerPlayer player, ModHopperBlockEntity blockEntity) {
        ((IPlayerExtension) player).openMenu(new net.minecraft.world.MenuProvider() {
            @Override
            public Component getDisplayName() {
                return blockEntity.getDisplayName();
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player ignored) {
                return blockEntity.createScreenMenu(syncId, playerInventory);
            }
        }, (RegistryFriendlyByteBuf buffer) -> buffer.writeBlockPos(blockEntity.getBlockPos()));
    }
}

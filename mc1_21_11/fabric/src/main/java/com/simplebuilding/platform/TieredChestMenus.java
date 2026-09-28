package com.simplebuilding.platform;

import com.simplebuilding.screen.TieredChestOpenData;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Fabric (MC 1.21.11): oeffnet das Menue einer Mod-Truhe samt Oeffnungsdaten (Stufe, einzeln oder doppelt).
 * Was geoeffnet wird, entscheidet {@code TieredChests#opening}.
 */
public final class TieredChestMenus {
    private TieredChestMenus() {
    }

    public static void open(ServerPlayer player, MenuProvider provider, TieredChestOpenData data) {
        player.openMenu(new ExtendedScreenHandlerFactory<TieredChestOpenData>() {
            @Override
            public Component getDisplayName() {
                return provider.getDisplayName();
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player menuPlayer) {
                return provider.createMenu(syncId, playerInventory, menuPlayer);
            }

            @Override
            public TieredChestOpenData getScreenOpeningData(ServerPlayer serverPlayer) {
                return data;
            }
        });
    }
}

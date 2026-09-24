package com.simplebuilding.platform;

import com.simplebuilding.blocks.entity.custom.BackpackBlockEntity;
import com.simplebuilding.screen.BackpackMenuProviders;
import com.simplebuilding.screen.BackpackOpenData;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Fabric: schickt ein Rucksack-Menue ab. Was geoeffnet wird, entscheidet
 * {@link BackpackMenuProviders}; hier kommt nur der Fabric-Weg fuer die Oeffnungsdaten dazu.
 */
public final class BackpackMenus {
    private BackpackMenus() {
    }

    public static void openWorn(ServerPlayer player) {
        open(player, BackpackMenuProviders.worn(player));
    }

    public static void openPlaced(ServerPlayer player, BackpackBlockEntity backpack) {
        open(player, BackpackMenuProviders.placed(backpack));
    }

    private static void open(ServerPlayer player, BackpackMenuProviders.Opening opening) {
        player.openMenu(new ExtendedMenuProvider<BackpackOpenData>() {
            @Override
            public Component getDisplayName() {
                return opening.provider().getDisplayName();
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player menuPlayer) {
                return opening.provider().createMenu(syncId, playerInventory, menuPlayer);
            }

            @Override
            public BackpackOpenData getScreenOpeningData(ServerPlayer serverPlayer) {
                return opening.data();
            }
        });
    }
}

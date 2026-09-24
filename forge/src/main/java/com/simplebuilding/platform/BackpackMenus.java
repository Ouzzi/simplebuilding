package com.simplebuilding.platform;

import com.simplebuilding.blocks.entity.custom.BackpackBlockEntity;
import com.simplebuilding.screen.BackpackMenuProviders;
import com.simplebuilding.screen.BackpackOpenData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.extensions.IForgeServerPlayer;

/**
 * Forge (geparkt, nur Kompilier-Paritaet): schickt ein Rucksack-Menue ab.
 * Was geoeffnet wird, entscheidet {@link BackpackMenuProviders}.
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
        ((IForgeServerPlayer) player).openMenu(opening.provider(),
                (FriendlyByteBuf buffer) -> BackpackOpenData.STREAM_CODEC.encode(buffer, opening.data()));
    }
}

package com.simplebuilding.platform;

import com.simplebuilding.blocks.entity.custom.BackpackBlockEntity;
import com.simplebuilding.screen.BackpackMenuProviders;
import com.simplebuilding.screen.BackpackOpenData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.extensions.IPlayerExtension;

/**
 * NeoForge: schickt ein Rucksack-Menue ab; die Oeffnungsdaten reisen im Puffer von
 * {@code openMenu(provider, writer)}. Was geoeffnet wird, entscheidet {@link BackpackMenuProviders}.
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
        ((IPlayerExtension) player).openMenu(opening.provider(),
                (RegistryFriendlyByteBuf buffer) -> BackpackOpenData.STREAM_CODEC.encode(buffer, opening.data()));
    }
}

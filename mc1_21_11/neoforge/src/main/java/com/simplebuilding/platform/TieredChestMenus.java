package com.simplebuilding.platform;

import com.simplebuilding.screen.TieredChestOpenData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.neoforged.neoforge.common.extensions.IPlayerExtension;

/**
 * NeoForge: oeffnet das Menue einer Mod-Truhe; die Oeffnungsdaten reisen im Puffer von
 * {@code openMenu(provider, writer)}. Was geoeffnet wird, entscheidet {@code TieredChests#opening}.
 */
public final class TieredChestMenus {
    private TieredChestMenus() {
    }

    public static void open(ServerPlayer player, MenuProvider provider, TieredChestOpenData data) {
        ((IPlayerExtension) player).openMenu(provider,
                (RegistryFriendlyByteBuf buffer) -> TieredChestOpenData.STREAM_CODEC.encode(buffer, data));
    }
}

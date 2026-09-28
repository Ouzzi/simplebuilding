package com.simplebuilding.platform;

import com.simplebuilding.screen.TieredChestOpenData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraftforge.common.extensions.IForgeServerPlayer;

/**
 * Forge (geparkt, nur Kompilier-Paritaet): oeffnet das Menue einer Mod-Truhe; die Oeffnungsdaten reisen im Puffer von
 * {@code openMenu(provider, writer)}. Was geoeffnet wird, entscheidet {@code TieredChests#opening}.
 */
public final class TieredChestMenus {
    private TieredChestMenus() {
    }

    public static void open(ServerPlayer player, MenuProvider provider, TieredChestOpenData data) {
        ((IForgeServerPlayer) player).openMenu(provider,
                (FriendlyByteBuf buffer) -> TieredChestOpenData.STREAM_CODEC.encode(buffer, data));
    }
}

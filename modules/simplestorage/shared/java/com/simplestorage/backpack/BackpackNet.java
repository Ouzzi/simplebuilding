package com.simplestorage.backpack;

import com.simplestorage.SimpleStorage;
import java.util.function.Consumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Client -> server: the backpack key was pressed while a backpack is worn or carried (split stage 8c-2, from SimpleBuilding's
 * {@code OpenBackpackPayload}). The server checks again and picks the backpack itself
 * ({@link BackpackMenuProviders#openCarriedIfAllowed}); every loader registers it, the client sends through {@link #toServer}.
 */
public final class BackpackNet {
    /** Client send hook (set by the loader's client entry); a no-op on a server. */
    public static Consumer<CustomPacketPayload> toServer = payload -> {};

    public record OpenPayload() implements CustomPacketPayload {
        public static final Type<OpenPayload> TYPE = new Type<>(SimpleStorage.id("open_backpack"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenPayload> CODEC = StreamCodec.unit(new OpenPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private BackpackNet() {}

    public static void handle(OpenPayload payload, ServerPlayer player) {
        BackpackMenuProviders.openCarriedIfAllowed(player);
    }
}

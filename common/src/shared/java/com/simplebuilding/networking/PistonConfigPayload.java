package com.simplebuilding.networking;

import com.simplebuilding.platform.PlatformServices;
import com.simplebuilding.util.PistonBreach;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * The server's piston options for the client (audit N16): a client replaying a reinforced piston
 * asks {@code PistonBreach#isBreachable} in its own {@code PistonStructureResolver}, and with a
 * config of its own it would push (or refuse) a block the server did not. Sent at login on every
 * loader; the client keeps the values in {@link PistonBreach#setClientRules}.
 */
public record PistonConfigPayload(boolean endPortalFrames, boolean moddedUnbreakables) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PistonConfigPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("simplebuilding", "piston_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PistonConfigPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, PistonConfigPayload::endPortalFrames,
            ByteBufCodecs.BOOL, PistonConfigPayload::moddedUnbreakables,
            PistonConfigPayload::new
    );

    /** The server's own values. */
    public static PistonConfigPayload fromServer() {
        PistonBreach.ClientRules rules = PistonBreach.serverRules();
        return new PistonConfigPayload(rules.endPortalFrames(), rules.moddedUnbreakables());
    }

    /** At login: the server's values to that player, if its client can take them. */
    public static void sendTo(ServerPlayer player) {
        if (player.connection != null && PlatformServices.canSendToPlayer(player, ID)) {
            PlatformServices.sendToPlayer(player, fromServer());
        }
    }

    /** Client side, on the client thread. */
    public void apply() {
        PistonBreach.setClientRules(new PistonBreach.ClientRules(endPortalFrames, moddedUnbreakables));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}

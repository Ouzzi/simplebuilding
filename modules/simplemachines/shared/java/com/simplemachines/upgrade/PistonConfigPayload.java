package com.simplemachines.upgrade;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * The server's piston options for the client (audit N16): a client replaying a Reinforced Piston asks
 * {@link PistonBreach#isBreachable} in its own {@code PistonStructureResolver}; with a config of its own it would push (or refuse) a
 * block the server did not. Sent at login; the client keeps the values in {@link PistonBreach#setClientRules}.
 */
public record PistonConfigPayload(boolean endPortalFrames, boolean moddedUnbreakables) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PistonConfigPayload> ID =
            new CustomPacketPayload.Type<>(com.simplemachines.SimpleMachines.id("piston_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PistonConfigPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, PistonConfigPayload::endPortalFrames,
            ByteBufCodecs.BOOL, PistonConfigPayload::moddedUnbreakables,
            PistonConfigPayload::new);

    /** The server's own values. */
    public static PistonConfigPayload fromServer() {
        PistonBreach.ClientRules rules = PistonBreach.serverRules();
        return new PistonConfigPayload(rules.endPortalFrames(), rules.moddedUnbreakables());
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

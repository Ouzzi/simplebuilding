package com.simplebuilding.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Server to client: the head in the extra head slot of the player with {@code entityId} (owner N49, {@code ExtraHead}),
 * sent to the player and to every client that sees them, so the head is drawn on the player everywhere. A payload
 * instead of synced entity data: NeoForge forbids adding synced data to a foreign entity (Player).
 */
public record ExtraHeadPayload(int entityId, ItemStack head) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ExtraHeadPayload> ID = new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath("simplebuilding", "extra_head"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ExtraHeadPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ExtraHeadPayload::entityId,
            ItemStack.OPTIONAL_STREAM_CODEC, ExtraHeadPayload::head,
            ExtraHeadPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    /** Client side: stores the head on that player in the client level (ignored when the player is not there). */
    public static void receive(ExtraHeadPayload payload) {
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        if (client.level != null && client.level.getEntity(payload.entityId()) instanceof net.minecraft.world.entity.player.Player player) {
            com.simplebuilding.util.ExtraHead.set(player, payload.head());
        }
    }
}

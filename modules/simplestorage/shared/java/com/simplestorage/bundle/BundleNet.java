package com.simplestorage.bundle;

import com.simplestorage.SimpleStorage;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;

/**
 * The two client-to-server packets of the bundles (split stage 8c-1, from SimpleBuilding's {@code ReinforcedBundleSelectionPayload}
 * and {@code PlacedBundleScrollPayload}); every loader registers them and calls the handlers here on the server thread,
 * the client sends through {@link #toServer} (set by the loader's client entry).
 */
public final class BundleNet {
    /** Client send hook (set by the loader's client entry); a no-op on a server. */
    public static Consumer<CustomPacketPayload> toServer = payload -> {};

    /** Mouse wheel over a bundle in a slot: the entry it selects ({@code -1} closes it). */
    public record SelectPayload(int slotId, int selectedIndex) implements CustomPacketPayload {
        public static final Type<SelectPayload> TYPE = new Type<>(SimpleStorage.id("bundle_selection"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SelectPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, SelectPayload::slotId, ByteBufCodecs.INT, SelectPayload::selectedIndex, SelectPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Sneak + mouse wheel on a placed bundle: which one and which way ({@code step} &gt; 0 = next item). The server owns
     * the shown index and checks range and sneaking itself ({@link PlacedBundles#scroll}); the block entity update brings
     * the result back.
     */
    public record ScrollPayload(BlockPos pos, int step) implements CustomPacketPayload {
        public static final Type<ScrollPayload> TYPE = new Type<>(SimpleStorage.id("placed_bundle_scroll"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ScrollPayload> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, ScrollPayload::pos, ByteBufCodecs.VAR_INT, ScrollPayload::step, ScrollPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private BundleNet() {}

    /** Only a bundle in a slot of the player's own open menu. */
    public static void handle(SelectPayload payload, ServerPlayer player) {
        if (player.containerMenu == null) return;
        int slotId = payload.slotId();
        if (slotId >= 0 && slotId < player.containerMenu.slots.size()) {
            Slot slot = player.containerMenu.getSlot(slotId);
            if (slot != null && slot.hasItem() && slot.getItem().getItem() instanceof ReinforcedBundleItem) {
                ReinforcedBundleItem.setBundleSelectedItem(slot.getItem(), payload.selectedIndex());
            }
        }
    }

    public static void handle(ScrollPayload payload, ServerPlayer player) {
        PlacedBundles.scroll(player, payload.pos(), payload.step());
    }
}

package com.simplebuilding.modules.simpleinterfaces.network;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Server to client only: the menu {@code containerId} gets the slots of every marked container appended, one
 * {@link Entry} per container in marking order (N65: up to {@code linkedContainerMax}). Sent after the open-screen packet
 * and before the menu's first content packet. Container id 0 is the player's own inventory menu (kept while a container
 * is marked); no entries remove the slots again (mark ended). Container id {@link #HUD} carries only the HUD tiles (one
 * per entry, none = hidden); it does not touch any menu.
 */
public record LinkedOpenPayload(int containerId, List<Entry> entries) implements CustomPacketPayload {
    /** Pseudo container id: the payload only updates the HUD tiles. */
    public static final int HUD = -1;
    /** Most entries a payload carries (the config allows 5). */
    public static final int MAX_ENTRIES = 8;

    /** One marked container: its slot count, its name and the block's item (HUD tile, tab icon). */
    public record Entry(int size, Component title, ItemStack icon) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Entry::size,
                ComponentSerialization.STREAM_CODEC, Entry::title,
                ItemStack.OPTIONAL_STREAM_CODEC, Entry::icon,
                Entry::new);
    }

    public static final Type<LinkedOpenPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("simpleinterfaces", "linked_open"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LinkedOpenPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LinkedOpenPayload::containerId,
            Entry.CODEC.apply(ByteBufCodecs.list(MAX_ENTRIES)), LinkedOpenPayload::entries,
            LinkedOpenPayload::new);

    public LinkedOpenPayload {
        entries = List.copyOf(entries);
    }

    /** All slots of all entries. */
    public int size() {
        int n = 0;
        for (Entry e : this.entries) n += e.size();
        return n;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

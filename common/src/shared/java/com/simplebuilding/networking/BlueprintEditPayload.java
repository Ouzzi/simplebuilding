package com.simplebuilding.networking;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blueprint.BlueprintCode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Editor -> Server: neuer Bau-Code fuer die Blaupause im Inventar-Slot {@code slot} (Hotbar 0-8
 * oder {@code Inventory.SLOT_OFFHAND}), optional signiert mit Titel. Der Server prueft alles
 * noch einmal ({@code ModMessageHandlers#handleBlueprintEdit}), wie beim Buch.
 */
public record BlueprintEditPayload(int slot, String code, boolean sign, String title) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BlueprintEditPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "blueprint_edit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BlueprintEditPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BlueprintEditPayload::slot,
            ByteBufCodecs.stringUtf8(BlueprintCode.MAX_CODE_LENGTH), BlueprintEditPayload::code,
            ByteBufCodecs.BOOL, BlueprintEditPayload::sign,
            ByteBufCodecs.stringUtf8(BlueprintCode.MAX_TITLE_LENGTH * 4), BlueprintEditPayload::title,
            BlueprintEditPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}

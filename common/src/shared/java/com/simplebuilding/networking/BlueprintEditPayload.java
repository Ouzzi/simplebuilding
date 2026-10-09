package com.simplebuilding.networking;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blueprint.BlueprintCode;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Editor -> Server: neuer Bau-Code fuer die Blaupause im Inventar-Slot {@code slot} (Hotbar 0-8
 * oder {@code Inventory.SLOT_OFFHAND}) oder - mit {@code placed} - fuer die abgelegte Blaupause an
 * dieser Blockposition (Queue N23), optional signiert mit Titel. Der Server prueft alles noch
 * einmal ({@code ModMessageHandlers#handleBlueprintEdit}), wie beim Buch.
 */
public record BlueprintEditPayload(int slot, String code, boolean sign, String title, Optional<BlockPos> placed)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BlueprintEditPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "blueprint_edit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BlueprintEditPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BlueprintEditPayload::slot,
            ByteBufCodecs.stringUtf8(BlueprintCode.MAX_CODE_LENGTH), BlueprintEditPayload::code,
            ByteBufCodecs.BOOL, BlueprintEditPayload::sign,
            ByteBufCodecs.stringUtf8(BlueprintCode.MAX_TITLE_LENGTH * 4), BlueprintEditPayload::title,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), BlueprintEditPayload::placed,
            BlueprintEditPayload::new
    );

    /** Fuer die Blaupause im Slot. */
    public BlueprintEditPayload(int slot, String code, boolean sign, String title) {
        this(slot, code, sign, title, Optional.empty());
    }

    /** Fuer die abgelegte Blaupause an {@code pos}. */
    public static BlueprintEditPayload placed(BlockPos pos, String code, boolean sign, String title) {
        return new BlueprintEditPayload(-1, code, sign, title, Optional.of(pos.immutable()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}

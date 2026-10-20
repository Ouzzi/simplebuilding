package com.simplemusic.net;

import com.simplemusic.SimpleMusic;
import com.simplemusic.sheet.NoteCode;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: save the sheet in {@code hand} (0 main, 1 off); {@code sign} with {@code title} makes it read-only. */
public record SheetEditPayload(int hand, String code, String title, boolean sign) implements CustomPacketPayload {
    public static final Type<SheetEditPayload> TYPE = new Type<>(SimpleMusic.id("sheet_edit"));
    public static final StreamCodec<ByteBuf, SheetEditPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SheetEditPayload::hand,
            ByteBufCodecs.stringUtf8(NoteCode.MAX_CODE_LENGTH), SheetEditPayload::code,
            ByteBufCodecs.stringUtf8(NoteCode.MAX_TITLE_LENGTH * 4), SheetEditPayload::title,
            ByteBufCodecs.BOOL, SheetEditPayload::sign,
            SheetEditPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package com.simplemusic.sheet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Content of a sheet of music (data component {@code simplemusic:sheet}, like the blueprint): the note code and, as on a
 * written book, title, author and whether it is signed (= read-only).
 */
public record SheetContent(String code, String title, String author, boolean signed) {
    public static final SheetContent EMPTY = new SheetContent("", "", "", false);

    public static final Codec<SheetContent> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("code", "").forGetter(SheetContent::code),
            Codec.STRING.optionalFieldOf("title", "").forGetter(SheetContent::title),
            Codec.STRING.optionalFieldOf("author", "").forGetter(SheetContent::author),
            Codec.BOOL.optionalFieldOf("signed", false).forGetter(SheetContent::signed)
    ).apply(i, SheetContent::new));

    public static final StreamCodec<ByteBuf, SheetContent> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(NoteCode.MAX_CODE_LENGTH), SheetContent::code,
            ByteBufCodecs.stringUtf8(NoteCode.MAX_TITLE_LENGTH * 4), SheetContent::title,
            ByteBufCodecs.stringUtf8(64), SheetContent::author,
            ByteBufCodecs.BOOL, SheetContent::signed,
            SheetContent::new);

    public boolean isBlank() {
        return code.isBlank();
    }
}

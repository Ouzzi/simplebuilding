package com.simplebuilding.blueprint;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Inhalt einer Blaupause (Datenkomponente {@code simplebuilding:blueprint}): der Bau-Code und, wie
 * beim beschriebenen Buch, Titel, Autor und ob sie signiert (= schreibgeschuetzt) ist.
 */
public record BlueprintContent(String code, String title, String author, boolean signed) {

    public static final BlueprintContent EMPTY = new BlueprintContent("", "", "", false);

    public static final Codec<BlueprintContent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("code", "").forGetter(BlueprintContent::code),
            Codec.STRING.optionalFieldOf("title", "").forGetter(BlueprintContent::title),
            Codec.STRING.optionalFieldOf("author", "").forGetter(BlueprintContent::author),
            Codec.BOOL.optionalFieldOf("signed", false).forGetter(BlueprintContent::signed)
    ).apply(instance, BlueprintContent::new));

    public static final StreamCodec<ByteBuf, BlueprintContent> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(BlueprintCode.MAX_CODE_LENGTH), BlueprintContent::code,
            ByteBufCodecs.stringUtf8(BlueprintCode.MAX_TITLE_LENGTH * 4), BlueprintContent::title,
            ByteBufCodecs.stringUtf8(64), BlueprintContent::author,
            ByteBufCodecs.BOOL, BlueprintContent::signed,
            BlueprintContent::new
    );

    public boolean isBlank() {
        return code.isBlank();
    }

    public BlueprintContent withCode(String newCode) {
        return new BlueprintContent(newCode, title, author, signed);
    }
}

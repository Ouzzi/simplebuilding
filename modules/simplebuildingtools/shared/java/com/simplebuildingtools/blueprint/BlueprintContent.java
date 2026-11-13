package com.simplebuildingtools.blueprint;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Inhalt einer Blaupause (Datenkomponente {@code simplebuilding:blueprint}): der Bau-Code und, wie
 * beim beschriebenen Buch, Titel, Autor und ob sie signiert (= schreibgeschuetzt) ist.
 */
public record BlueprintContent(String code, String title, String author, boolean signed) {

    public static final BlueprintContent EMPTY = new BlueprintContent("", "", "", false);

    /**
     * Characters per stored string. NBT strings hold at most 65 535 bytes (modified UTF-8, up to 3 bytes a char), so a
     * longer code (a scan of a varied selection, {@link BlueprintCode#MAX_STORED_CODE_LENGTH}) is saved in pieces: the
     * first in {@code code}, the rest in the list {@code more}. Every code up to one piece stays exactly as before.
     */
    public static final int CHUNK = 20_000;

    public static final Codec<BlueprintContent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("code", "").forGetter(c -> chunks(c.code()).get(0)),
            Codec.STRING.listOf().optionalFieldOf("more", List.of()).forGetter(c -> {
                List<String> all = chunks(c.code());
                return all.subList(1, all.size());
            }),
            Codec.STRING.optionalFieldOf("title", "").forGetter(BlueprintContent::title),
            Codec.STRING.optionalFieldOf("author", "").forGetter(BlueprintContent::author),
            Codec.BOOL.optionalFieldOf("signed", false).forGetter(BlueprintContent::signed)
    ).apply(instance, (head, more, title, author, signed) ->
            new BlueprintContent(more.isEmpty() ? head : head + String.join("", more), title, author, signed)));

    public static final StreamCodec<ByteBuf, BlueprintContent> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(BlueprintCode.MAX_STORED_CODE_LENGTH), BlueprintContent::code,
            ByteBufCodecs.stringUtf8(BlueprintCode.MAX_TITLE_LENGTH * 4), BlueprintContent::title,
            ByteBufCodecs.stringUtf8(64), BlueprintContent::author,
            ByteBufCodecs.BOOL, BlueprintContent::signed,
            BlueprintContent::new
    );

    /** The code cut into pieces of at most {@link #CHUNK} chars (never inside a surrogate pair); always at least one. */
    static List<String> chunks(String code) {
        if (code.length() <= CHUNK) {
            return List.of(code);
        }
        List<String> out = new ArrayList<>(code.length() / CHUNK + 1);
        for (int start = 0; start < code.length(); ) {
            int end = Math.min(code.length(), start + CHUNK);
            if (end < code.length() && Character.isHighSurrogate(code.charAt(end - 1))) {
                end--;
            }
            out.add(code.substring(start, end));
            start = end;
        }
        return out;
    }

    public boolean isBlank() {
        return code.isBlank();
    }

    public BlueprintContent withCode(String newCode) {
        return new BlueprintContent(newCode, title, author, signed);
    }
}

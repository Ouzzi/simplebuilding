package com.simplelib.api;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;

/**
 * Codec helpers for saved data (REV2-B-11). {@code codec.listOf()} fails the whole list for one broken element - an item or
 * block of a removed mod would drop every other entry of a block entity or {@code SavedData} with it.
 */
public final class SafeCodecs {
    private static final Logger LOGGER = LogUtils.getLogger();

    private SafeCodecs() {}

    /**
     * Like {@code element.listOf()}, but a broken element is logged ({@code what}) and skipped; the other elements stay.
     * Encoding is unchanged, so old saves still load and the next save writes only the readable elements.
     */
    public static <T> Codec<List<T>> listLenient(Codec<T> element, String what) {
        return Codec.of(element.listOf(), new Decoder<List<T>>() {
            @Override
            public <O> DataResult<Pair<List<T>, O>> decode(DynamicOps<O> ops, O input) {
                return ops.getStream(input).map(stream -> {
                    List<T> out = new ArrayList<>();
                    stream.forEach(item -> element.parse(ops, item).resultOrPartial(
                            err -> LOGGER.error("Skipping broken {}: {}", what, err)).ifPresent(out::add));
                    return Pair.of(out, input);
                });
            }
        });
    }
}

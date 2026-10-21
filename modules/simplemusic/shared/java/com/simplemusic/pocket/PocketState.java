package com.simplemusic.pocket;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Where a Pocket Jukebox stands (data component {@code simplemusic:pocket}): its session {@code id} (0 = never played),
 * the disc counted from the oldest one ({@code fromEnd}, the order discs went in; new discs go to the front of the
 * bundle list, so this stays put when one is added) and the paused position in ticks. Written on start, pause and
 * track change only; the running time lives in {@link PocketPlayback}.
 */
public record PocketState(long id, int fromEnd, int ticks) {
    public static final PocketState NEW = new PocketState(0L, 0, 0);

    public static final Codec<PocketState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.optionalFieldOf("id", 0L).forGetter(PocketState::id),
            Codec.INT.optionalFieldOf("disc", 0).forGetter(PocketState::fromEnd),
            Codec.INT.optionalFieldOf("ticks", 0).forGetter(PocketState::ticks)
    ).apply(i, PocketState::new));

    public static final StreamCodec<ByteBuf, PocketState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, PocketState::id,
            ByteBufCodecs.VAR_INT, PocketState::fromEnd,
            ByteBufCodecs.VAR_INT, PocketState::ticks,
            PocketState::new);
}

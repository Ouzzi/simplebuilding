package com.simplemusic.disc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What a custom disc carries (data component {@code simplemusic:track}, concept §2.3): the server's SHA-256 of the
 * uploaded file, title, length and uploader - never audio data.
 */
public record TrackRef(String sha256, String title, int seconds, String uploader) {
    public static final Codec<TrackRef> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("sha256").forGetter(TrackRef::sha256),
            Codec.STRING.optionalFieldOf("title", "").forGetter(TrackRef::title),
            Codec.INT.optionalFieldOf("seconds", 0).forGetter(TrackRef::seconds),
            Codec.STRING.optionalFieldOf("uploader", "").forGetter(TrackRef::uploader)
    ).apply(i, TrackRef::new));

    public static final StreamCodec<ByteBuf, TrackRef> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), TrackRef::sha256,
            ByteBufCodecs.stringUtf8(128), TrackRef::title,
            ByteBufCodecs.VAR_INT, TrackRef::seconds,
            ByteBufCodecs.stringUtf8(64), TrackRef::uploader,
            TrackRef::new);
}

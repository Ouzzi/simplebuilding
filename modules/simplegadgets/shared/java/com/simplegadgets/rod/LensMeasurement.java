package com.simplegadgets.rod;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Last measurement of a Resonance Rod with Constructor's Touch (owner 2026-09-28; moved from SimpleBuilding, OW24-2 G2): distance
 * from the eye to the hit point in blocks (rounded to 0.1), height difference target Y minus the player's foot Y (whole blocks)
 * and the translation key of the target (block or creature), so the tooltip shows it in the reader's language. The component
 * type {@code simplegadgets:lens_measurement} takes over the old {@code simplebuilding:lens_measurement} through SimpleLib's id moves.
 */
public record LensMeasurement(float distance, int heightDifference, String target) {

    public static final Codec<LensMeasurement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("distance").forGetter(LensMeasurement::distance),
            Codec.INT.fieldOf("height_difference").forGetter(LensMeasurement::heightDifference),
            Codec.STRING.fieldOf("target").forGetter(LensMeasurement::target)
    ).apply(instance, LensMeasurement::new));

    public static final StreamCodec<ByteBuf, LensMeasurement> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, LensMeasurement::distance,
            ByteBufCodecs.VAR_INT, LensMeasurement::heightDifference,
            ByteBufCodecs.STRING_UTF8, LensMeasurement::target,
            LensMeasurement::new);

    /** Distance to one decimal, as HUD and tooltip show it. */
    public static float round(double distance) {
        return Math.round(distance * 10.0) / 10.0f;
    }
}

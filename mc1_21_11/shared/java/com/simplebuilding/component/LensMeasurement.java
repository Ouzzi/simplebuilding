package com.simplebuilding.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Letzte Messung einer Amethystlinse mit Beruehrung des Konstrukteurs (Besitzer 2026-09-28):
 * Entfernung vom Auge zum Trefferpunkt in Bloecken (auf 0,1 gerundet), Hoehenunterschied
 * Ziel-Y minus Fuss-Y des Spielers (ganze Bloecke) und der Uebersetzungsschluessel des Ziels
 * (Block oder Lebewesen), damit der Tooltip ihn in der Sprache des Lesers zeigt.
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

    /** Entfernung auf eine Nachkommastelle, wie HUD und Tooltip sie zeigen. */
    public static float round(double distance) {
        return Math.round(distance * 10.0) / 10.0f;
    }
}

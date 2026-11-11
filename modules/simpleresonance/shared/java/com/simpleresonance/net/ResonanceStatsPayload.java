package com.simpleresonance.net;

import com.simpleresonance.ResonanceMath;
import com.simpleresonance.SimpleResonance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client once a second (MOB27-3 R2, docs/ai/PLAN-B-TRIMSTATS27-2026-11-11.md): what the player gathered since his last death -
 * experience points, metres, active ticks, hostile and passive kills, damage taken - and the configured base of the trim resonance.
 * The client computes the three factors from it with {@link ResonanceMath}, exactly like the server. Replaces SimpleBuilding's old
 * {@code TrimDataPayload} and {@code SurvivalSyncPayload} on 26.3.
 */
public record ResonanceStatsPayload(int xp, int metres, int activeTicks, int hostile, int passive, int damage, double base)
        implements CustomPacketPayload {
    public static final Type<ResonanceStatsPayload> TYPE = new Type<>(SimpleResonance.id("stats"));
    public static final StreamCodec<FriendlyByteBuf, ResonanceStatsPayload> CODEC =
            CustomPacketPayload.codec(ResonanceStatsPayload::write, ResonanceStatsPayload::read);
    /** A fresh player: nothing gathered, the default base. */
    public static final ResonanceStatsPayload FRESH = new ResonanceStatsPayload(0, 0, 0, 0, 0, 0, 2.0);

    private static ResonanceStatsPayload read(FriendlyByteBuf buf) {
        return new ResonanceStatsPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt(), buf.readDouble());
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeVarInt(Math.max(0, xp));
        buf.writeVarInt(Math.max(0, metres));
        buf.writeVarInt(Math.max(0, activeTicks));
        buf.writeVarInt(Math.max(0, hostile));
        buf.writeVarInt(Math.max(0, passive));
        buf.writeVarInt(Math.max(0, damage));
        buf.writeDouble(base);
    }

    /** {xp, survival, combat}, each 0.1..1.0. */
    public double[] factors() {
        return new double[] {ResonanceMath.xpFactor(xp), ResonanceMath.survivalFactor(metres, activeTicks), ResonanceMath.combatFactor(hostile, passive, damage)};
    }

    /** The trim resonance: base x mean of the three factors (0.2..2.0 with the default base). */
    public double multiplier() {
        double[] f = factors();
        return base * ResonanceMath.meanFactor(f[0], f[1], f[2]);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package com.simpleresonance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;

/**
 * Everything Simple Resonance remembers about one player (saved in {@link ResonanceData}): the long-term value points {@code points},
 * the totals over all lives and the baselines of the last death (the resonance share F counts what came after them, exactly like the
 * SimpleBuilding core's {@code SurvivalTracerMixin}), the farm guard window for kill points and the last award per boss.
 */
public final class PlayerRecord {
    /** Long-term value points V (1 per 30 active minutes, kills, bosses); x deathKeep at every death. */
    public double points;
    /** Active ticks, hostile and passive kills over all lives. */
    public int active, hostile, passive;
    /** Baselines at the last death: distance in blocks, active ticks, kills, damage taken (stat) and total experience. */
    public int baseDistance, baseActive, baseHostile, basePassive, baseDamage, baseXp;
    /** Kill point window: play time (ticks) when it began and the points given inside it. */
    public long hourStart;
    public double hourPoints;
    /** Last award per boss/source: play time (ticks) of the award. */
    public final Map<String, Long> bosses = new HashMap<>();
    /** Whether the old SimpleBuilding block {@code SimpleBuildingData} was imported (once). */
    public boolean legacyImported;

    public static final Codec<PlayerRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.optionalFieldOf("points", 0.0).forGetter(r -> r.points),
            Codec.INT.optionalFieldOf("active", 0).forGetter(r -> r.active),
            Codec.INT.optionalFieldOf("hostile", 0).forGetter(r -> r.hostile),
            Codec.INT.optionalFieldOf("passive", 0).forGetter(r -> r.passive),
            Codec.INT.optionalFieldOf("baseDistance", 0).forGetter(r -> r.baseDistance),
            Codec.INT.optionalFieldOf("baseActive", 0).forGetter(r -> r.baseActive),
            Codec.INT.optionalFieldOf("baseHostile", 0).forGetter(r -> r.baseHostile),
            Codec.INT.optionalFieldOf("basePassive", 0).forGetter(r -> r.basePassive),
            Codec.INT.optionalFieldOf("baseDamage", 0).forGetter(r -> r.baseDamage),
            Codec.INT.optionalFieldOf("baseXp", 0).forGetter(r -> r.baseXp),
            Codec.LONG.optionalFieldOf("hourStart", 0L).forGetter(r -> r.hourStart),
            Codec.DOUBLE.optionalFieldOf("hourPoints", 0.0).forGetter(r -> r.hourPoints),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("bosses", Map.of()).forGetter(r -> r.bosses),
            Codec.BOOL.optionalFieldOf("legacyImported", false).forGetter(r -> r.legacyImported)).apply(i, PlayerRecord::new));

    public PlayerRecord() {}

    private PlayerRecord(double points, int active, int hostile, int passive, int baseDistance, int baseActive, int baseHostile,
                         int basePassive, int baseDamage, int baseXp, long hourStart, double hourPoints, Map<String, Long> bosses,
                         boolean legacyImported) {
        this.points = points;
        this.active = active;
        this.hostile = hostile;
        this.passive = passive;
        this.baseDistance = baseDistance;
        this.baseActive = baseActive;
        this.baseHostile = baseHostile;
        this.basePassive = basePassive;
        this.baseDamage = baseDamage;
        this.baseXp = baseXp;
        this.hourStart = hourStart;
        this.hourPoints = hourPoints;
        this.bosses.putAll(bosses);
        this.legacyImported = legacyImported;
    }
}

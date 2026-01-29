package com.simplebuilding.util;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.config.SimplebuildingConfig;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stat;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class TrimMultiplierLogic {

    // Nimmt jetzt PlayerEntity statt ServerPlayerEntity
    public static double getMultiplier(PlayerEntity player) {
        double xpMult = calculateXPMultiplier(player);
        double survivalMult = calculateSurvivalMultiplier(player);
        double globalMult = SimplebuildingConfig.trimBenefitBaseMultiplier;

        return globalMult * xpMult * survivalMult;
    }

    public static double calculateXPMultiplier(PlayerEntity player) {
        int level = player.experienceLevel;
        double result = 0.1d + ((double) level / 100.0d) * 0.9d;
        return MathHelper.clamp(result, 0.1d, 1.0d);
    }

    public static double calculateSurvivalMultiplier(PlayerEntity player) {
        int baseDist = 0;
        int baseKills = 0;
        int baseTime = 0;

        int currentDist = 0;
        int currentKills = 0;
        int currentTime = 0;

        // Versuche, Daten via Accessor zu holen (funktioniert auf Client und Server, wenn Mixins greifen)
        if (player instanceof SurvivalTracerAccessor accessor) {
            baseDist = accessor.simplebuilding$getBaseDistance();
            baseKills = accessor.simplebuilding$getBaseKills();
            baseTime = accessor.simplebuilding$getBaseTime();

            // FIX: Auf dem Client lesen wir die gesyncten Werte, auf dem Server berechnen wir sie frisch
            if (player.getEntityWorld().isClient()) {
                currentDist = accessor.simplebuilding$getCurrentDistance();
                currentKills = accessor.simplebuilding$getCurrentKills();
                currentTime = accessor.simplebuilding$getCurrentTime();
            } else {
                // Server-Logik
                currentDist = getStatTotalDistance(player);
                currentKills = 0; // Platzhalter
                currentTime = getStat(player, Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME));
            }
        } else {
            // Fallback, falls Mixin fehlschlägt (sollte nicht passieren)
            if (player.getEntityWorld().isClient()) {
                currentDist = getStatTotalDistance(player);
                currentTime = getStat(player, Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME));
            }
        }

        int distSinceDeath = Math.max(0, currentDist - baseDist);
        int killsSinceDeath = Math.max(0, currentKills - baseKills);
        int timeSinceDeath = Math.max(0, currentTime - baseTime);

        // Debugging im Tooltip oder Logger könnte hier helfen, wenn es immer noch 0 ist
        // System.out.println("Dist: " + distSinceDeath + " Time: " + timeSinceDeath);

        double distFactor = calculateCurve(distSinceDeath, 4000.0);
        double killFactor = calculateCurve(killsSinceDeath, 250.0);
        double timeFactor = calculateCurve(timeSinceDeath, 72000.0);

        return Math.max(distFactor, Math.max(killFactor, timeFactor));
    }

    private static double calculateCurve(double input, double scale) {
        return 0.1d + 0.9d * (1.0d - Math.exp(-input / scale));
    }

    private static int getStatTotalDistance(PlayerEntity player) {
        System.out.println("Total Distance Stats for player " + player.getName().getString() + ": Walked: " +
                getStat(player, Stats.CUSTOM.getOrCreateStat(Stats.WALK_ONE_CM)) + ", Sprinted: " +
                getStat(player, Stats.CUSTOM.getOrCreateStat(Stats.SPRINT_ONE_CM)) + ", Crouched: " +
                getStat(player, Stats.CUSTOM.getOrCreateStat(Stats.CROUCH_ONE_CM)) + ", Flown: " +
                getStat(player, Stats.CUSTOM.getOrCreateStat(Stats.FLY_ONE_CM)) + ", Climbed: " +
                getStat(player, Stats.CUSTOM.getOrCreateStat(Stats.CLIMB_ONE_CM))
        );
        return getStat(player, Stats.CUSTOM.getOrCreateStat(Stats.WALK_ONE_CM)) / 100
                + getStat(player, Stats.CUSTOM.getOrCreateStat(Stats.SPRINT_ONE_CM)) / 100
                + getStat(player, Stats.CUSTOM.getOrCreateStat(Stats.CROUCH_ONE_CM)) / 100
                + getStat(player, Stats.CUSTOM.getOrCreateStat(Stats.FLY_ONE_CM)) / 100
                + getStat(player, Stats.CUSTOM.getOrCreateStat(Stats.CLIMB_ONE_CM)) / 100;
    }

    private static int getStat(PlayerEntity player, Stat<?> stat) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            return serverPlayer.getStatHandler().getStat(stat);
        }
        return 0;
    }
}
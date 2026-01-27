package com.simplebuilding.util;

import com.simplebuilding.config.SimplebuildingConfig;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.util.math.MathHelper;

public class TrimMultiplierLogic {

    public static double getMultiplier(ServerPlayerEntity player) {
        double xpMult = calculateXPMultiplier(player);
        double survivalMult = calculateSurvivalMultiplier(player);
        double globalMult = SimplebuildingConfig.trimBenefitBaseMultiplier;

        return globalMult * xpMult * survivalMult;
    }

    private static double calculateXPMultiplier(ServerPlayerEntity player) {
        int level = player.experienceLevel;
        double result = 0.1d + ((double) level / 100.0d) * 0.9d;
        return MathHelper.clamp(result, 0.1d, 1.0d);
    }

    private static double calculateSurvivalMultiplier(ServerPlayerEntity player) {
        int baseDist = 0;
        int baseKills = 0;
        int baseTime = 0;

        // Daten aus dem Mixin holen
        if (player instanceof SurvivalTracerAccessor accessor) {
            baseDist = accessor.simplebuilding$getBaseDistance();
            baseKills = accessor.simplebuilding$getBaseKills();
            baseTime = accessor.simplebuilding$getBaseTime();
        }

        int currentDist = getStatTotalDistance(player);

        // FIX: Auch hier Nutzung von Stats statt Scoreboard
        int currentKills = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.MOB_KILLS));
        int currentTime = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME));

        int distSinceDeath = Math.max(0, currentDist - baseDist);
        int killsSinceDeath = Math.max(0, currentKills - baseKills);
        int timeSinceDeath = Math.max(0, currentTime - baseTime);

        double distFactor = calculateCurve(distSinceDeath, 4000.0);
        double killFactor = calculateCurve(killsSinceDeath, 250.0);
        double timeFactor = calculateCurve(timeSinceDeath, 72000.0);

        return Math.max(distFactor, Math.max(killFactor, timeFactor));
    }

    private static double calculateCurve(double input, double scale) {
        return 0.1d + 0.9d * (1.0d - Math.exp(-input / scale));
    }

    private static int getStatTotalDistance(ServerPlayerEntity player) {
        return player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.WALK_ONE_CM)) / 100
                + player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.SPRINT_ONE_CM)) / 100
                + player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.CROUCH_ONE_CM)) / 100
                + player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.FLY_ONE_CM)) / 100
                + player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.CLIMB_ONE_CM)) / 100;
    }
}
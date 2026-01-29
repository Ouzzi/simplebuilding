package com.simplebuilding.util;

import com.simplebuilding.config.SimplebuildingConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.util.math.MathHelper;

public class TrimMultiplierLogic {

    public static double getMultiplier(PlayerEntity player) {
        double xpMult = calculateXPMultiplier(player);
        double survivalMult = calculateSurvivalMultiplier(player);
        double combatMult = calculateCombatMultiplier(player);
        double globalMult = SimplebuildingConfig.trimBenefitBaseMultiplier;

        return globalMult * xpMult * survivalMult * combatMult;
    }

    public static double calculateXPMultiplier(PlayerEntity player) {
        int level = player.experienceLevel;
        double result = 0.1d + ((double) level / 100.0d) * 0.9d;
        return MathHelper.clamp(result, 0.1d, 1.0d);
    }

    public static double calculateSurvivalMultiplier(PlayerEntity player) {
        int baseDist = 0;
        int baseTime = 0;
        int currentDist = 0;
        int currentTime = 0;

        if (player instanceof SurvivalTracerAccessor accessor) {
            baseDist = accessor.simplebuilding$getBaseDistance();
            baseTime = accessor.simplebuilding$getBaseTime();

            if (player.getEntityWorld().isClient()) {
                currentDist = accessor.simplebuilding$getCurrentDistance();
                currentTime = accessor.simplebuilding$getCurrentTime();
            } else {
                currentDist = getStatTotalDistance(player);
                currentTime = getStat(player, Stats.PLAY_TIME);
            }
        }

        int distSinceDeath = Math.max(0, currentDist - baseDist);
        int timeSinceDeath = Math.max(0, currentTime - baseTime);

        double distFactor = calculateCurve(distSinceDeath, 4000.0);
        double timeFactor = calculateCurve(timeSinceDeath, 72000.0);

        return Math.max(distFactor, timeFactor);
    }

    public static double calculateCombatMultiplier(PlayerEntity player) {
        int baseHostile = 0;
        int basePassive = 0;
        int baseDamage = 0;
        int curHostile = 0;
        int curPassive = 0;
        int curDamage = 0;

        // FIX: Nur Accessor Interface nutzen, kein illegaler Mixin-Cast!
        if (player instanceof SurvivalTracerAccessor accessor) {
            baseHostile = accessor.simplebuilding$getBaseHostileKills();
            basePassive = accessor.simplebuilding$getBasePassiveKills();
            baseDamage = accessor.simplebuilding$getBaseDamageTaken();

            if (player.getEntityWorld().isClient()) {
                curHostile = accessor.simplebuilding$getCurrentHostileKills();
                curPassive = accessor.simplebuilding$getCurrentPassiveKills();
                curDamage = accessor.simplebuilding$getCurrentDamageTaken();
            } else if (player instanceof ServerPlayerEntity serverPlayer) {
                curHostile = accessor.simplebuilding$getCurrentHostileKills();
                curPassive = accessor.simplebuilding$getCurrentPassiveKills();
                curDamage = serverPlayer.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.DAMAGE_TAKEN));
            }
        }

        int hostileSinceDeath = Math.max(0, curHostile - baseHostile);
        int passiveSinceDeath = Math.max(0, curPassive - basePassive);
        int damageSinceDeath = Math.max(0, curDamage - baseDamage);

        // Score: Kills + (Damage Taken / 10). Damage taken gibt Punkte (Kampferfahrung)
        // Damage Taken Stat ist meist x10 (also 20 = 2 Herzen?), hier nehmen wir den rohen Wert.
        // Ein bisschen Schaden einzustecken hilft dem Multiplikator.
        double combatScore = (hostileSinceDeath * 1.0) + (passiveSinceDeath * 0.2) + (damageSinceDeath * 0.05);

        return calculateCurve(combatScore, 100.0);
    }

    private static double calculateCurve(double input, double scale) {
        return 0.1d + 0.9d * (1.0d - Math.exp(-input / scale));
    }

    private static int getStatTotalDistance(PlayerEntity player) {
        return getStat(player, Stats.WALK_ONE_CM) / 100
             + getStat(player, Stats.SPRINT_ONE_CM) / 100
             + getStat(player, Stats.CROUCH_ONE_CM) / 100
             + getStat(player, Stats.FLY_ONE_CM) / 100
             + getStat(player, Stats.CLIMB_ONE_CM) / 100;
    }

    private static int getStat(PlayerEntity player, net.minecraft.util.Identifier stat) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            return serverPlayer.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(stat));
        }
        return 0;
    }
}
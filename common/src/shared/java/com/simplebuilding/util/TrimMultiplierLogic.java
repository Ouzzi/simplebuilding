package com.simplebuilding.util;

import com.simplebuilding.config.SimplebuildingConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Die "Resonanz" der Besatz-Boni: waechst mit Erfahrungsstufe, Ueberleben (Weg und Zeit seit dem
 * letzten Tod) und Kampf (Kills und eingesteckter Schaden seit dem letzten Tod).
 *
 * <p>Jeder der drei Faktoren liegt in 0,1..1,0; die Resonanz ist ihr MITTELWERT mal der
 * konfigurierten Basis (Standard 2,0), also 0,2..2,0. Frueher war es das Produkt - ein frischer
 * Spieler lag dann bei 0,002 und selbst ein ordentlich gespielter (Stufe 30, eine Stunde am Leben,
 * 50 Kills) bei rund 0,23, die Boni waren praktisch unsichtbar. Siehe docs/TRIM-BALANCE.md.
 */
public class TrimMultiplierLogic {

    /** Ab dieser Stufe ist der Erfahrungsfaktor voll - die hoechste, die Vanilla je verlangt (Zaubertisch). */
    public static final int XP_LEVEL_FOR_FULL_FACTOR = 30;

    public static double getMultiplier(Player player) {
        double xpMult = calculateXPMultiplier(player);
        double survivalMult = calculateSurvivalMultiplier(player);
        double combatMult = calculateCombatMultiplier(player);
        double globalMult = SimplebuildingConfig.trimBenefitBaseMultiplier;

        return globalMult * (xpMult + survivalMult + combatMult) / 3.0d;
    }

    /** Der Anteil an der vollen Resonanz (0,1..1,0), ohne die konfigurierte Basis. */
    public static double getResonanceFraction(Player player) {
        return (calculateXPMultiplier(player) + calculateSurvivalMultiplier(player) + calculateCombatMultiplier(player)) / 3.0d;
    }

    public static double calculateXPMultiplier(Player player) {
        int level = player.experienceLevel;
        double result = 0.1d + ((double) level / XP_LEVEL_FOR_FULL_FACTOR) * 0.9d;
        return Mth.clamp(result, 0.1d, 1.0d);
    }

    public static double calculateSurvivalMultiplier(Player player) {
        int baseDist = 0;
        int baseTime = 0;
        int currentDist = 0;
        int currentTime = 0;

        if (player instanceof SurvivalTracerAccessor accessor) {
            baseDist = accessor.simplebuilding$getBaseDistance();
            baseTime = accessor.simplebuilding$getBaseTime();

            if (player.level().isClientSide()) {
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

    public static double calculateCombatMultiplier(Player player) {
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

            if (player.level().isClientSide()) {
                curHostile = accessor.simplebuilding$getCurrentHostileKills();
                curPassive = accessor.simplebuilding$getCurrentPassiveKills();
                curDamage = accessor.simplebuilding$getCurrentDamageTaken();
            } else if (player instanceof ServerPlayer serverPlayer) {
                curHostile = accessor.simplebuilding$getCurrentHostileKills();
                curPassive = accessor.simplebuilding$getCurrentPassiveKills();
                curDamage = serverPlayer.getStats().getValue(Stats.CUSTOM.get(Stats.DAMAGE_TAKEN));
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

    private static int getStatTotalDistance(Player player) {
        return getStat(player, Stats.WALK_ONE_CM) / 100
             + getStat(player, Stats.SPRINT_ONE_CM) / 100
             + getStat(player, Stats.CROUCH_ONE_CM) / 100
             + getStat(player, Stats.FLY_ONE_CM) / 100
             + getStat(player, Stats.CLIMB_ONE_CM) / 100;
    }

    private static int getStat(Player player, net.minecraft.resources.Identifier stat) {
        if (player instanceof ServerPlayer serverPlayer) {
            return serverPlayer.getStats().getValue(Stats.CUSTOM.get(stat));
        }
        return 0;
    }
}
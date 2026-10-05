package com.simplequalityoflife.config;

import com.simplequalityoflife.Simplequalityoflife;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Config(name = Simplequalityoflife.MOD_ID)
public class SimplequalityoflifeConfig implements ConfigData {

    public static final double MAX_CLIMB_SPEED = 0.4;
    public static final double MAX_SLIDE_SPEED = 0.8;
    public static final double MAX_BONUS = 1.5;
    public static final double DEFAULT_CLIMB_SPEED = 0.4;
    public static final double DEFAULT_SLIDE_SPEED = 0.8;
    public static final double DEFAULT_DURABILITY_THRESHOLD = 0.8;
    public static final double DEFAULT_DURABILITY_BONUS_MULTIPLIER = 1.0;
    public static final int DEFAULT_VAULT_COOLDOWN_DAYS = 100;
    public static final int DEFAULT_RAIN_PARTICLE_DENSITY = 20;
    public static final int DEFAULT_LINKED_RANGE = 64;
    public static final int MIN_LINKED_RANGE = 8;
    public static final int MAX_LINKED_RANGE = 128;
    public static double bounded(double v, double min, double max, double fallback) {
        return Double.isFinite(v) ? Math.clamp(v, min, max) : fallback;
    }
    public void normalizeNumbers() {
        if (qOL == null) qOL = new QOL();
        qOL.ladderClimbingSpeed = bounded(qOL.ladderClimbingSpeed, 0.2, MAX_CLIMB_SPEED, 0.4);
        qOL.ladderSlideSpeed = bounded(qOL.ladderSlideSpeed, 0.15, MAX_SLIDE_SPEED, 0.8);
        qOL.fullDurabilityThreshold = bounded(qOL.fullDurabilityThreshold, 0.8, 1, 0.8);
        qOL.fullDurabilityBonusMultiplier = bounded(qOL.fullDurabilityBonusMultiplier, 1, MAX_BONUS, DEFAULT_DURABILITY_BONUS_MULTIPLIER);
        qOL.vaultCooldownDays = Math.clamp(qOL.vaultCooldownDays, 1, 36500);
        qOL.clientRainParticleDensity = Math.clamp(qOL.clientRainParticleDensity, 0, 100);
        qOL.linkedContainerRange = Math.clamp(qOL.linkedContainerRange, MIN_LINKED_RANGE, MAX_LINKED_RANGE);
        if (qOL.ladderSlideActivation == null) qOL.ladderSlideActivation = SlideActivationMode.CAMERA;
    }
    public void normalize() {
        normalizeNumbers();
        qOL.mutedEntities = boundedList(qOL.mutedEntities);
        qOL.nametagMuteSuffixes = boundedList(qOL.nametagMuteSuffixes);
        qOL.nametagBabySuffixes = boundedList(qOL.nametagBabySuffixes);
    }
    private static List<String> boundedList(List<String> values) {
        if (values == null) return new ArrayList<>();
        return new ArrayList<>(values.stream().filter(v -> v != null && !v.isBlank() && v.length() <= 64).distinct().limit(64).toList());
    }
    // Diese Enum muss public sein, damit Cloth Config sie lesen kann
    public enum SlideActivationMode {
        CAMERA, ALWAYS
    }

    // Option außerhalb der QOL-Kategorie (Global)
    @ConfigEntry.Gui.Tooltip
    public boolean frostWalkerWalkOnPowderSnow = true;

    @ConfigEntry.Gui.CollapsibleObject
    public QOL qOL = new QOL();

    public static class QOL {
        @ConfigEntry.Gui.Tooltip
        public boolean enableManualCrawl = true;

        @ConfigEntry.Gui.Tooltip
        public boolean enableVaultCooldown = true;
        // --- Movement & AutoWalk ---
        @ConfigEntry.Gui.Tooltip
        public boolean enableAutowalk = false;

        // --- Sound & Visuals ---
        @ConfigEntry.Gui.Tooltip
        public List<String> mutedEntities = new ArrayList<>();

        @ConfigEntry.Gui.Tooltip
        public List<String> nametagMuteSuffixes = new ArrayList<>(Arrays.asList("_mute", "_shhh"));

        @ConfigEntry.Gui.Tooltip
        public List<String> nametagBabySuffixes = new ArrayList<>(Arrays.asList("_baby", "_small"));

        // --- Farming & World Interaction ---
        @ConfigEntry.Gui.Tooltip
        public boolean preventFarmlandTrampleWithFeatherFalling = true;

        @ConfigEntry.Gui.Tooltip
        public boolean sharpnessCutsGrass = true;

        @ConfigEntry.Gui.Tooltip
        public boolean enableHoeHarvest = true;

        @ConfigEntry.Gui.Tooltip
        public boolean enableFurnaceLavaFill = true;

        // --- Ladder Mechanics ---
        @ConfigEntry.Gui.Tooltip
        public double ladderClimbingSpeed = DEFAULT_CLIMB_SPEED; // Vanilla default is ~0.2

        @ConfigEntry.Gui.Tooltip
        public boolean enableFastLadderSlide = true;

        @ConfigEntry.Gui.Tooltip
        public double ladderSlideSpeed = DEFAULT_SLIDE_SPEED;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public SlideActivationMode ladderSlideActivation = SlideActivationMode.CAMERA;

        // --- Vaults ---
        @ConfigEntry.Gui.Tooltip
        public int vaultCooldownDays = DEFAULT_VAULT_COOLDOWN_DAYS;

        // --- Durability Mechanics ---
        @ConfigEntry.Gui.Tooltip
        public boolean enableFullDurabilityBonus = true;

        @ConfigEntry.Gui.Tooltip
        public double fullDurabilityThreshold = DEFAULT_DURABILITY_THRESHOLD;

        @ConfigEntry.Gui.Tooltip
        public double fullDurabilityBonusMultiplier = DEFAULT_DURABILITY_BONUS_MULTIPLIER;

        // --- Amboss ---
        /** Reparieren ohne neue Verzauberung erhoeht die Arbeitskosten am Amboss nicht (2026-10-02). */
        @ConfigEntry.Gui.Tooltip
        public boolean anvilRepairKeepsCost = true;

        // --- Piglins ---
        @ConfigEntry.Gui.Tooltip
        public boolean piglinsIgnoreGoldTrims = true;

        @ConfigEntry.Gui.Tooltip
        public boolean piglinsIgnoreGoldTools = true;

        // --- Weather ---
        @ConfigEntry.Gui.Tooltip
        public boolean disableWeather = false;

        @ConfigEntry.Gui.Tooltip
        public int clientRainParticleDensity = DEFAULT_RAIN_PARTICLE_DENSITY;

        // --- Containers (2026-10-04) ---
        @ConfigEntry.Gui.Tooltip
        public boolean enableLinkedContainers = true;

        @ConfigEntry.Gui.Tooltip
        public int linkedContainerRange = DEFAULT_LINKED_RANGE;

        @ConfigEntry.Gui.Tooltip
        public boolean enableEasyShulkers = true;

        @ConfigEntry.Gui.Tooltip
        public boolean enableEasyEnderChests = true;
    }
}

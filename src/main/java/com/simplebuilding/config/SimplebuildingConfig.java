package com.simplebuilding.config;

import com.simplebuilding.Simplebuilding;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = Simplebuilding.MOD_ID)
public class SimplebuildingConfig implements ConfigData {

    @ConfigEntry.Gui.CollapsibleObject
    public Tools tools = new Tools();

    @ConfigEntry.Gui.CollapsibleObject
    public WorldGen worldGen = new WorldGen();

    @ConfigEntry.Gui.Tooltip
    public boolean enableDoubleJump = true;

    @ConfigEntry.Gui.Tooltip
    public boolean enableArmorTrimBenefits = true;

    @ConfigEntry.Gui.Tooltip
    public static double trimBenefitBaseMultiplier = 2.0;
    @ConfigEntry.Gui.Tooltip
    public static double maxMultiplierLimit = 10.0;

    public static class Tools {
        @ConfigEntry.Gui.Tooltip
        public boolean invertOctantSneak = false; // Constructor's Touch Invertierung
        public int buildingHighlightOpacity = 40;

        @ConfigEntry.Gui.Tooltip
        public boolean enableToolAnimations = true; // Hauptschalter
        @ConfigEntry.Gui.Tooltip
        public boolean enableChiselAnimation = true;

        @ConfigEntry.Gui.Tooltip
        public boolean invertBundleInteractions = false;
    }

    public static class WorldGen {
        @ConfigEntry.Gui.Tooltip
        public boolean enableVillagerTrades = true;

        @ConfigEntry.Gui.Tooltip
        public boolean enableWanderingTrades = true;

        @ConfigEntry.Gui.Tooltip
        public boolean enableLootTableChanges = true;
    }


}
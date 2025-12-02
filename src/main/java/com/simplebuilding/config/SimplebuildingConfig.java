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

    public static class Tools {
        @ConfigEntry.Gui.Tooltip
        public boolean invertOctantSneak = false; // Constructor's Touch Invertierung
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
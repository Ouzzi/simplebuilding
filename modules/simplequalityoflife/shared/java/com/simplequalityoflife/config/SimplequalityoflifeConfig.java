package com.simplequalityoflife.config;

import com.simplequalityoflife.Simplequalityoflife;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

/**
 * Super-mod config ({@code config/simplequalityoflife.json}): one switch per bundled sub-mod
 * (docs/ai/KONZEPT-SUPERMOD-SUBMOD-2026-10-06.md). The former {@code qOL.*} options live in the sub-mods' own configs;
 * each sub-mod takes its old values over once from this file, unknown old keys are ignored here.
 */
@Config(name = Simplequalityoflife.MOD_ID)
public class SimplequalityoflifeConfig implements ConfigData {

    @ConfigEntry.Gui.Tooltip
    public boolean enableSimpleWeather = true;

    @ConfigEntry.Gui.Tooltip
    public boolean enableSimpleInterfaces = true;

    @ConfigEntry.Gui.Tooltip
    public boolean enableSimpleLoot = true;

    @ConfigEntry.Gui.Tooltip
    public boolean enableSimpleMobManners = true;

    @ConfigEntry.Gui.Tooltip
    public boolean enableSimpleFarming = true;

    @ConfigEntry.Gui.Tooltip
    public boolean enableSimpleTools = true;

    @ConfigEntry.Gui.Tooltip
    public boolean enableSimpleMovement = true;

    public void normalizeNumbers() {
    }

    public void normalize() {
        normalizeNumbers();
    }
}

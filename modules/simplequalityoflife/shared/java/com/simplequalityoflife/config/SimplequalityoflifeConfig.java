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

    public static final int DEFAULT_LINKED_RANGE = 64;
    public static final int MIN_LINKED_RANGE = 8;
    public static final int MAX_LINKED_RANGE = 128;
    public static double bounded(double v, double min, double max, double fallback) {
        return Double.isFinite(v) ? Math.clamp(v, min, max) : fallback;
    }
    public void normalizeNumbers() {
        if (qOL == null) qOL = new QOL();
        qOL.linkedContainerRange = Math.clamp(qOL.linkedContainerRange, MIN_LINKED_RANGE, MAX_LINKED_RANGE);
    }
    public void normalize() {
        normalizeNumbers();
    }
    private static List<String> boundedList(List<String> values) {
        if (values == null) return new ArrayList<>();
        return new ArrayList<>(values.stream().filter(v -> v != null && !v.isBlank() && v.length() <= 64).distinct().limit(64).toList());
    }

    // --- Sub-Mods (Super-Mod, docs/ai/KONZEPT-SUPERMOD-SUBMOD-2026-10-06.md): one switch per bundled sub-mod ---
    @ConfigEntry.Gui.Tooltip
    public boolean enableSimpleWeather = true;

    @ConfigEntry.Gui.Tooltip
    public boolean enableSimpleInterfaces = true;

    @ConfigEntry.Gui.Tooltip
    public boolean enableSimpleMovement = true;

    @ConfigEntry.Gui.Tooltip
    public boolean enableSimpleTools = true;

    @ConfigEntry.Gui.Tooltip
    public boolean enableSimpleFarming = true;

    @ConfigEntry.Gui.Tooltip
    public boolean enableSimpleCreatures = true;

    @ConfigEntry.Gui.Tooltip
    public boolean enableSimpleLoot = true;

    @ConfigEntry.Gui.CollapsibleObject
    public QOL qOL = new QOL();

    public static class QOL {
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

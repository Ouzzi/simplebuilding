package com.simplelib.crucible;

import com.simplelib.config.LibConfig;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Heat levels (owner answer 51 = A): no "low" level; medium = campfire/magma, high = lava (furnace
 * level), extreme = soul lava (SimpleBuilding). Torches, candles and plain fire give no heat.
 */
public enum HeatLevel {
    NONE, MEDIUM, HIGH, EXTREME;

    /** Speed factor of this heat (owner F16, adapted to three levels: 0.5 / 0.75 / 1.0). */
    public double factor() {
        return switch (this) {
            case NONE -> 0.0;
            case MEDIUM -> LibConfig.factorMedium;
            case HIGH -> LibConfig.factorHigh;
            case EXTREME -> LibConfig.factorExtreme;
        };
    }

    /** Lowest heat at which a cooking recipe type may run (plan section 6, table A). */
    public static HeatLevel required(RecipeType<?> type) {
        if (type == RecipeType.SMELTING || type == RecipeType.BLASTING) return HIGH;
        return MEDIUM;
    }

    public boolean atLeast(HeatLevel other) {
        return ordinal() >= other.ordinal();
    }

    public HeatLevel up() {
        return this == NONE || this == EXTREME ? this : values()[ordinal() + 1];
    }

    public HeatLevel down() {
        return this == NONE ? NONE : values()[ordinal() - 1];
    }

    public static HeatLevel byId(int id) {
        HeatLevel[] all = values();
        return all[Math.max(0, Math.min(all.length - 1, id))];
    }
}

package com.simpleovens;

import com.simpleovens.config.OvensConfig;
import net.minecraft.world.item.Rarity;

/** The three furnace tiers; speeds come from {@link OvensConfig} (default 2x, 4x, 8x the Vanilla machine). */
public enum OvenTier {
    REINFORCED("reinforced", Rarity.COMMON, false),
    NETHERITE("netherite", Rarity.UNCOMMON, true),
    ENDERITE("enderite", Rarity.EPIC, true);

    private final String prefix;
    private final Rarity rarity;
    private final boolean fireResistant;

    OvenTier(String prefix, Rarity rarity, boolean fireResistant) {
        this.prefix = prefix;
        this.rarity = rarity;
        this.fireResistant = fireResistant;
    }

    public String prefix() {
        return this.prefix;
    }

    public Rarity rarity() {
        return this.rarity;
    }

    /** Netherite and Enderite items survive fire and lava, like SimpleBuilding's other upper tiers. */
    public boolean fireResistant() {
        return this.fireResistant;
    }

    /** Netherite and Enderite pay double experience (see {@link OvenPerks}). */
    public boolean upper() {
        return this != REINFORCED;
    }

    /** Speed against the Vanilla machine of the same family (config, 1..8). */
    public int speed() {
        return switch (this) {
            case REINFORCED -> OvensConfig.reinforcedSpeed;
            case NETHERITE -> OvensConfig.netheriteSpeed;
            case ENDERITE -> OvensConfig.enderiteSpeed;
        };
    }

    /** Extra cooking ticks per game tick: speed - 1. */
    public int extraCookTicks() {
        return Math.max(0, speed() - 1);
    }

    /** The speed of the shipped defaults (2, 4, 8), for displays that must not depend on a server's config. */
    public int defaultSpeed() {
        return switch (this) {
            case REINFORCED -> OvensConfig.REINFORCED_DEFAULT;
            case NETHERITE -> OvensConfig.NETHERITE_DEFAULT;
            case ENDERITE -> OvensConfig.ENDERITE_DEFAULT;
        };
    }
}

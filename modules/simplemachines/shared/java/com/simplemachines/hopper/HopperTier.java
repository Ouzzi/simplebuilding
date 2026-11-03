package com.simplemachines.hopper;

/** The three mod hopper tiers (MACH32-MOD stage 3): block, item, cart and menu of each carry its tier. */
public enum HopperTier {
    REINFORCED("reinforced"),
    NETHERITE("netherite"),
    ENDERITE("enderite");

    private final String prefix;

    HopperTier(String prefix) {
        this.prefix = prefix;
    }

    /** {@code reinforced}, {@code netherite}, {@code enderite}. */
    public String prefix() {
        return prefix;
    }

    /** 1 Reinforced, 2 Netherite, 3 Enderite. */
    public int level() {
        return ordinal() + 1;
    }

    public String hopperPath() {
        return prefix + "_hopper";
    }

    public String cartPath() {
        return prefix + "_hopper_minecart";
    }
}

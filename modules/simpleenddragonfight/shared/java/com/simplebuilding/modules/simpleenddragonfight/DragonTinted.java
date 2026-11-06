package com.simplebuilding.modules.simpleenddragonfight;

/** Duck interface of the End dragon (EnderDragonMixin): synced flag "chorus colour", set by the fight session, read by the client renderer. */
public interface DragonTinted {
    /** 0 = plain, 1 = chorus violet (from tintFromDragon), 2 = soul blue and chorus violet together (from tintJewelFromDragon). */
    int simpleenddragonfight$tintLevel();

    void simpleenddragonfight$setTintLevel(int level);

    default boolean simpleenddragonfight$tinted() { return simpleenddragonfight$tintLevel() > 0; }

    default void simpleenddragonfight$setTinted(boolean tinted) { simpleenddragonfight$setTintLevel(tinted ? 1 : 0); }
}

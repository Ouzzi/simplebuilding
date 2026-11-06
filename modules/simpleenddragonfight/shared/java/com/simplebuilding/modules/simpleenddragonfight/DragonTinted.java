package com.simplebuilding.modules.simpleenddragonfight;

/** Duck interface of the End dragon (EnderDragonMixin): synced flag "chorus colour", set by the fight session, read by the client renderer. */
public interface DragonTinted {
    boolean simpleenddragonfight$tinted();

    void simpleenddragonfight$setTinted(boolean tinted);
}

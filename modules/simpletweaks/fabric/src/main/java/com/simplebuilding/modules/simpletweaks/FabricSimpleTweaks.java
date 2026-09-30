package com.simplebuilding.modules.simpletweaks;
import net.fabricmc.api.ModInitializer;
public final class FabricSimpleTweaks implements ModInitializer {
    @Override public void onInitialize() { LegacyDeed.register(); }
}

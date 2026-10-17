package com.simplebuilding.modules.wiringexample;
import net.fabricmc.api.ModInitializer;
public final class WiringExampleFabric implements ModInitializer {
    @Override public void onInitialize() { ExampleItems.register(); }
}

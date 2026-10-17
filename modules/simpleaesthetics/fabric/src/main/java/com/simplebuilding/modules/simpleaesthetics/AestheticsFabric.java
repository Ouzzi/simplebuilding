package com.simplebuilding.modules.simpleaesthetics;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
/** Super-mod entry: no content, only the sub-mod switches (config/simpleaesthetics.json). */
public final class AestheticsFabric implements ModInitializer {
    @Override public void onInitialize() { SimpleAesthetics.load(FabricLoader.getInstance().getConfigDir()); }
}

package com.simplebuilding.modules.simpleaesthetics;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
/** Super-mod entry: no content, only the sub-mod switches (config/simpleaesthetics.json). */
public final class AestheticsFabric implements ModInitializer {
    @Override public void onInitialize() {
        SimpleAesthetics.load(FabricLoader.getInstance().getConfigDir());
        // CMD24: /simpleaesthetics config; the sub-mods read their switch while the game starts, so a change needs a restart.
        com.simplelib.api.config.ConfigCommands.server(SimpleAesthetics.MOD_ID).gson(SimpleAesthetics.Config.class, SimpleAesthetics::config, SimpleAesthetics::restore, SimpleAesthetics.Config::new)
                .file(FabricLoader.getInstance().getConfigDir(), SimpleAesthetics.FILE).restart("enableSimpleVisuals", "enableSimpleSounds", "enableSimpleModels").register();
    }
}

package com.simplebuilding.modules.simplelib;

/** Mod Menu entry: the config screen, only while Cloth Config is loaded (optional for SimpleLib). */
public final class SimpleLibModMenu implements com.terraformersmc.modmenu.api.ModMenuApi {
    @Override
    public com.terraformersmc.modmenu.api.ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("cloth-config")
                ? com.simplelib.config.LibConfigScreen.create(parent) : null;
    }
}

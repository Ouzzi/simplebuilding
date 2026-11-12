package com.simplebuilding.modules.simplelib;

/**
 * Mod Menu entry: SimpleLib's own screen (Cloth; without Cloth the generic one), and (L13, RELSB31 f2=A) the generic config
 * screen for every loaded mod that announced a config with {@code ConfigCommands}. Mod Menu puts the provided screens in only
 * where a mod has none of its own (its own Cloth screen stays; without Cloth those return no screen, so this one shows).
 */
public final class SimpleLibModMenu implements com.terraformersmc.modmenu.api.ModMenuApi {
    @Override
    public com.terraformersmc.modmenu.api.ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("cloth-config")
                ? com.simplelib.config.LibConfigScreen.create(parent) : com.simplelib.config.screen.GenericConfigScreen.create("simplelib", parent);
    }

    @Override
    public java.util.Map<String, com.terraformersmc.modmenu.api.ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
        java.util.Map<String, com.terraformersmc.modmenu.api.ConfigScreenFactory<?>> out = new java.util.LinkedHashMap<>();
        for (String id : com.simplelib.config.screen.GenericConfigScreen.modIds()) {
            if (!id.equals("simplelib")) out.put(id, parent -> com.simplelib.config.screen.GenericConfigScreen.create(id, parent));
        }
        return out;
    }
}

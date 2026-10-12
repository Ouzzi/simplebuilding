package com.simplebuilding.modules.simpletools;

/** Mod Menu entry: opens the config screen. */
public final class ToolsModMenu implements com.terraformersmc.modmenu.api.ModMenuApi {
    @Override
    public com.terraformersmc.modmenu.api.ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ToolsScreen::create;
    }
}

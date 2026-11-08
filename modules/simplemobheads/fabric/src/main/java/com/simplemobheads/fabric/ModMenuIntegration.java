package com.simplemobheads.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Mod Menu: the config screen. */
public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("cloth-config")) return com.terraformersmc.modmenu.api.ModMenuApi.super.getModConfigScreenFactory(); // Q-B1: the screen needs Cloth Config, the mod does not
        return com.simplemobheads.client.MobHeadsConfigScreen::build;
    }
}

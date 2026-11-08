package com.simplevisuals.fabric;
public final class VisualsModMenu implements com.terraformersmc.modmenu.api.ModMenuApi { public com.terraformersmc.modmenu.api.ConfigScreenFactory<?> getModConfigScreenFactory(){if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("cloth-config")) return com.terraformersmc.modmenu.api.ModMenuApi.super.getModConfigScreenFactory(); // Q-B1: the screen needs Cloth Config, the mod does not
        return com.simplevisuals.client.VisualsConfigScreen::create;} }

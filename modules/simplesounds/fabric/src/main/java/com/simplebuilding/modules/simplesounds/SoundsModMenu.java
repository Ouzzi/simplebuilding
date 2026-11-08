package com.simplebuilding.modules.simplesounds;
public final class SoundsModMenu implements com.terraformersmc.modmenu.api.ModMenuApi {public com.terraformersmc.modmenu.api.ConfigScreenFactory<?> getModConfigScreenFactory(){if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("cloth-config")) return com.terraformersmc.modmenu.api.ModMenuApi.super.getModConfigScreenFactory(); // Q-B1: the screen needs Cloth Config, the mod does not
        return SoundScreen::create;}}

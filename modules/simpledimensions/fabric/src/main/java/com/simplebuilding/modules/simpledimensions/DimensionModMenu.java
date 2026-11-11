package com.simplebuilding.modules.simpledimensions;
import com.terraformersmc.modmenu.api.*;
public final class DimensionModMenu implements ModMenuApi {
 public ConfigScreenFactory<?> getModConfigScreenFactory(){
  if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("cloth-config")) return com.terraformersmc.modmenu.api.ModMenuApi.super.getModConfigScreenFactory(); // Q-B1: the screen needs Cloth Config, the mod does not
  return com.simplebuilding.modules.simpledimensions.client.DimensionConfigScreen::create;}
}

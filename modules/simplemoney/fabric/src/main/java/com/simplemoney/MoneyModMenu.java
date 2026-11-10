package com.simplemoney;
import com.terraformersmc.modmenu.api.*;
import net.fabricmc.loader.api.FabricLoader;
public final class MoneyModMenu implements ModMenuApi {
 public ConfigScreenFactory<?> getModConfigScreenFactory() { 
  if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("cloth-config")) return com.terraformersmc.modmenu.api.ModMenuApi.super.getModConfigScreenFactory(); // Q-B1: the screen needs Cloth Config, the mod does not
  return parent->com.simplemoney.client.MoneyConfigScreen.create(parent,FabricLoader.getInstance().getConfigDir()); }
}

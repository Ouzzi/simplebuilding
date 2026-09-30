package com.simplemoney;
import com.terraformersmc.modmenu.api.*;
import net.fabricmc.loader.api.FabricLoader;
public final class MoneyModMenu implements ModMenuApi {
 public ConfigScreenFactory<?> getModConfigScreenFactory() { return parent->com.simplemoney.client.MoneyConfigScreen.create(parent,FabricLoader.getInstance().getConfigDir()); }
}

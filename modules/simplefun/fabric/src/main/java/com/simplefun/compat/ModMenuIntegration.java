package com.simplefun.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ModMenuIntegration implements ModMenuApi {

  @Override
  @SuppressWarnings("removal")
  public ConfigScreenFactory<?> getModConfigScreenFactory() {
    return parent -> com.simplefun.client.FunConfigScreen.build(parent);
  }
}

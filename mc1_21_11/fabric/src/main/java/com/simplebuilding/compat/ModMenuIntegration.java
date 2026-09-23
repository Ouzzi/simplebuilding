package com.simplebuilding.compat;

import com.simplebuilding.config.SimplebuildingConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfigClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ModMenuIntegration implements ModMenuApi {

    // Direkter Aufruf statt Reflection: Cloth Config 26.2 hat getConfigScreen von AutoConfig nach
    // AutoConfigClient verschoben, und die reflektive Suche auf AutoConfig warf seitdem beim Druck
    // auf den ModMenu-Knopf. So wird ein kuenftiger Umzug zum Compile-Fehler statt zum Absturz.
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> AutoConfigClient.getConfigScreen(SimplebuildingConfig.class, parent).get();
    }
}

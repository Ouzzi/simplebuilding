package com.simplebuilding.compat;

import com.simplebuilding.config.SimplebuildingConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // Erzeugt das GUI automatisch basierend auf deiner Config-Klasse
        return parent -> AutoConfig.getConfigScreen(SimplebuildingConfig.class, parent).get();
    }
}
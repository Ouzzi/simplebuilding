package com.simplebuilding.compat;

import com.simplebuilding.config.SimplebuildingConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Method;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class ModMenuIntegration implements ModMenuApi {

    @Override
    @SuppressWarnings("removal")
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> openConfigScreen(parent);
    }

    @SuppressWarnings("unchecked")
    private static Screen openConfigScreen(Screen parent) {
        try {
            Method method = AutoConfig.class.getMethod("getConfigScreen", Class.class, Screen.class);
            Supplier<Screen> supplier = (Supplier<Screen>) method.invoke(null, SimplebuildingConfig.class, parent);
            return supplier.get();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to open Simplebuilding config screen", e);
        }
    }
}

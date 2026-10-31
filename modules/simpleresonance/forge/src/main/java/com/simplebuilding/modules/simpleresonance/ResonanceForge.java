package com.simplebuilding.modules.simpleresonance;

import com.simpleresonance.SimpleResonance;
import com.simpleresonance.guide.ResonanceGuide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegisterEvent;

/** Forge 26.3 entry: config and guide book like NeoForge (events and tests follow with the Forge port, not requested yet). */
@Mod(SimpleResonance.MOD_ID)
public final class ResonanceForge {
    public ResonanceForge(FMLJavaModLoadingContext context) {
        SimpleResonance.init(FMLPaths.CONFIGDIR.get());
        RegisterEvent.getBus(context.getModBusGroup()).addListener(event -> {
            if (event.getRegistryKey().equals(net.minecraft.core.registries.Registries.ITEM)) ResonanceGuide.register();
        });
    }
}

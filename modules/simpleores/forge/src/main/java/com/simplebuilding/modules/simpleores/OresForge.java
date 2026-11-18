package com.simplebuilding.modules.simpleores;

import com.simpleores.OresRegistry;
import com.simpleores.SimpleOres;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegisterEvent;

/** Forge 26.3 entry: same registration as Fabric/NeoForge (tests and creative tabs follow with the Forge port, not requested yet). */
@Mod(SimpleOres.MOD_ID)
public final class OresForge {
    public OresForge(FMLJavaModLoadingContext context) {
        SimpleOres.init(FMLPaths.CONFIGDIR.get());
        RegisterEvent.getBus(context.getModBusGroup()).addListener(event -> {
            event.register(Registries.SOUND_EVENT, r -> com.simpleores.OresSounds.events(r::register));
            event.register(Registries.BLOCK, r -> OresRegistry.blocks(r::register));
            event.register(Registries.ITEM, r -> {
                OresRegistry.items(r::register);
                SimpleOres.registerStation();
            });
            // N92-2: the two own feature types (Fabric: onInitialize), as on NeoForge.
            if (event.getRegistryKey().equals(Registries.FEATURE_TYPE)) {
                com.simpleores.RoseGoldVeinFeature.register();
                com.simpleores.MiniGeodeFeature.register();
            }
        });
    }
}

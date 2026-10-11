package com.simplebuilding.modules.simpletrims;

import com.simpletrims.SimpleTrims;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

/** Forge 26.3 entry. */
@Mod(SimpleTrims.MOD_ID)
public final class TrimsForge {
    public TrimsForge(FMLJavaModLoadingContext context) { com.simplebuilding.modules.simpletrims.forge.ModuleForgeTests.register(context.getModBusGroup());
        SimpleTrims.init(FMLPaths.CONFIGDIR.get());
    }
}

package com.simplebuilding.modules.simpleinterfaces;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Forge entry: nothing to register; the style is client mixins (manifest MixinConfigs) plus a lazily loaded config. */
@Mod("simpleinterfaces")
public final class ContainersForge {
    public ContainersForge(FMLJavaModLoadingContext context) { com.simplebuilding.modules.simpleinterfaces.forge.ModuleForgeTests.register(context.getModBusGroup());}
}

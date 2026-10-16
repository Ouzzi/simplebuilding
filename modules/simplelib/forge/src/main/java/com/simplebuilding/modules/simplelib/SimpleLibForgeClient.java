package com.simplebuilding.modules.simplelib;

import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Client-only Forge setup (dist guard in the entry point). Nothing to register since the crucible screens moved to Simple Crucibles (split stage 5). */
final class SimpleLibForgeClient {
    static void init(FMLJavaModLoadingContext context) {
    }

    private SimpleLibForgeClient() {}
}

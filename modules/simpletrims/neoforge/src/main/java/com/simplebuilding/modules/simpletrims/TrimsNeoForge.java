package com.simplebuilding.modules.simpletrims;

import com.simpletrims.SimpleTrims;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;

/** NeoForge entry. */
@Mod(SimpleTrims.MOD_ID)
public final class TrimsNeoForge {
    public TrimsNeoForge(IEventBus bus) {
        SimpleTrims.init(FMLPaths.CONFIGDIR.get());
        ModuleNeoTests.register(bus);
    }
}

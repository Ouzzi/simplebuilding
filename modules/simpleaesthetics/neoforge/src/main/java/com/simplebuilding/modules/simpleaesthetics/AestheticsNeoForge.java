package com.simplebuilding.modules.simpleaesthetics;
import net.neoforged.fml.common.Mod;
/** Super-mod entry: no content, only the sub-mod switches (config/simpleaesthetics.json). */
@Mod("simpleaesthetics")
public final class AestheticsNeoForge {
    public AestheticsNeoForge(net.neoforged.bus.api.IEventBus bus) {
        SimpleAesthetics.load(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get());
        ModuleNeoTests.register(bus);
    }
}

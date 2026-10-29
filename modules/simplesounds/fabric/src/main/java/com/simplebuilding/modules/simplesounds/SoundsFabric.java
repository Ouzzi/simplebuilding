package com.simplebuilding.modules.simplesounds;
import net.fabricmc.api.ModInitializer;
public final class SoundsFabric implements ModInitializer {
    @Override public void onInitialize() {
        ExampleItems.register();
        // CMD24: /simpleclient simplesounds (client command; announced here like the other client-only modules, SimpleLib is bundled in the Fabric entry).
        com.simplelib.api.config.ConfigCommands.client("simplesounds").gson(SoundConfig.class, () -> SoundClient.CONFIG, c -> { c.normalize(); SoundClient.CONFIG = c; },
                () -> { SoundConfig d = new SoundConfig(); d.normalize(); return d; })
                .save(SoundClient::save).file(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir(), "simplesounds.json").register();
    }
}

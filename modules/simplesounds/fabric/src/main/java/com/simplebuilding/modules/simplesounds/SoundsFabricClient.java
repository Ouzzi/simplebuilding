package com.simplebuilding.modules.simplesounds;

/** Fabric client entry: the local {@code /simplesounds guide} command. */
public final class SoundsFabricClient implements net.fabricmc.api.ClientModInitializer {
    @Override public void onInitializeClient() {
        net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registry) -> com.simplebuilding.modules.simplesounds.client.SoundsGuideCommand.register(dispatcher));
    }
}

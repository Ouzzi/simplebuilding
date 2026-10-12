package com.simplebuilding.modules.simplerespawn;

import com.simplebuilding.modules.simplerespawn.network.DownedListPayload;
import com.simplebuilding.modules.simplerespawn.network.RespawnStatePayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

public final class RespawnFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        RespawnConfig.load(FabricLoader.getInstance().getConfigDir());
        com.simplebuilding.modules.simplerespawn.guide.RespawnGuide.register();
        if (FabricLoader.getInstance().isModLoaded("ftbquests")) com.simplebuilding.modules.simplerespawn.guide.RespawnGuide.installQuests(FabricLoader.getInstance().getConfigDir());
        net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> {
            if (!SimpleRespawn.on()) return;
            var book = new net.minecraft.world.item.ItemStack(com.simplebuilding.modules.simplerespawn.guide.RespawnGuide.book());
            // Before the writable book: a tail append clashes with other mods' insertAfter at the tail.
            if (out.getDisplayStacks().stream().anyMatch(st -> st.is(net.minecraft.world.item.Items.WRITABLE_BOOK)))
                out.insertBefore(net.minecraft.world.item.Items.WRITABLE_BOOK, java.util.List.of(book), net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else out.accept(book);
        });
        PayloadTypeRegistry.clientboundPlay().register(DownedListPayload.TYPE, DownedListPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RespawnStatePayload.TYPE, RespawnStatePayload.CODEC);
        SimpleRespawn.send = (player, payload) -> {
            if (player.connection != null && ServerPlayNetworking.canSend(player, payload.type())) ServerPlayNetworking.send(player, payload);
        };
        ServerTickEvents.END_SERVER_TICK.register(RespawnServer::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> RespawnServer.reset());
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> RespawnCommands.register(dispatcher));
    }
}

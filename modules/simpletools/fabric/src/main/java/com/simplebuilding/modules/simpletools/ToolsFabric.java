package com.simplebuilding.modules.simpletools;

import net.fabricmc.api.ModInitializer;

public final class ToolsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleTools.configDir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
        SimpleTools.load();
        ToolsRegistry.registerBlocks();
        ToolsRegistry.registerItems();
        ToolsRegistry.registerMenus();
        net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out ->
                out.insertAfter(net.minecraft.world.item.Items.LODESTONE, ToolsRegistry.tabStacks(), net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        net.fabricmc.fabric.api.loot.v3.LootTableEvents.MODIFY.register((key, table, source, registries) -> ToolsLoot.apply(key, table::withPool, registries));
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((d, r, e) -> ToolsCommands.register(d));
        InteractionGuard.permission = (p, pos) -> net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.invoker()
                .beforeBlockBreak(p.level(), p, pos, p.level().getBlockState(pos), p.level().getBlockEntity(pos));
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((p, w, hand, hit) ->
                FurnaceLavaFillHandler.onRightClickBlock(p, hand, hit.getBlockPos(), hit.getDirection()));
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(com.simplebuilding.modules.simpletools.network.ConfigSyncPayload.TYPE, com.simplebuilding.modules.simpletools.network.ConfigSyncPayload.STREAM_CODEC);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(s -> SimpleTools.onChange = () -> s.getPlayerList().getPlayers().forEach(ToolsFabric::sync));
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((h, sender, s) -> sync(h.player));
    }
    private static void sync(net.minecraft.server.level.ServerPlayer p) {
        if (net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(p, com.simplebuilding.modules.simpletools.network.ConfigSyncPayload.TYPE))
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(p, new com.simplebuilding.modules.simpletools.network.ConfigSyncPayload(SimpleTools.syncJson()));
    }
}

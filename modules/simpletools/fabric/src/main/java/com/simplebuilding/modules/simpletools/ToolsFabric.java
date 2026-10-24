package com.simplebuilding.modules.simpletools;
import com.simplelib.api.InteractionGuard;

import net.fabricmc.api.ModInitializer;

public final class ToolsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleTools.registerMoves();
        SimpleTools.configDir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
        SimpleTools.load();
        com.simplebuilding.modules.simpletools.guide.ToolsGuide.register();
        net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> {
            var book = java.util.List.of(com.simplebuilding.modules.simpletools.guide.ToolsGuide.stack());
            // Before the writable book: a tail append clashes with other mods' insertAfter at the tail.
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(net.minecraft.world.item.Items.WRITABLE_BOOK))) out.insertBefore(net.minecraft.world.item.Items.WRITABLE_BOOK, book, net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else book.forEach(s -> out.accept(s, net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("ftbquests")) com.simplebuilding.modules.simpletools.guide.ToolsGuide.installQuests(SimpleTools.configDir);
        ToolsRegistry.registerBlocks();
        ToolsRegistry.registerItems();
        ToolsRegistry.registerMenus();
        net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out ->
                out.insertAfter(net.minecraft.world.item.Items.LODESTONE, ToolsRegistry.tabStacks(), net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES).register(out ->
                out.insertAfter(net.minecraft.world.item.Items.BRUSH, ToolsRegistry.brushStacks(), net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        net.fabricmc.fabric.api.loot.v3.LootTableEvents.MODIFY.register((key, table, source, registries) -> ToolsLoot.apply(key, table::withPool, registries));
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((d, r, e) -> ToolsCommands.register(d));
        // The probe fires the real BEFORE handlers; area hammers must not take it for a break (HammerActions veto).
        com.simplelib.api.HammerActions.registerAreaVeto(() -> InteractionGuard.probing > 0);
        InteractionGuard.permission = (p, pos) -> {
            InteractionGuard.probing++;
            try {
                return net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.invoker()
                        .beforeBlockBreak(p.level(), p, pos, p.level().getBlockState(pos), p.level().getBlockEntity(pos));
            } finally {
                InteractionGuard.probing--;
            }
        };
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((p, w, hand, hit) ->
                FurnaceLavaFillHandler.onRightClickBlock(p, hand, hit.getBlockPos(), hit.getDirection()));
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(com.simplebuilding.modules.simpletools.network.ConfigSyncPayload.TYPE, com.simplebuilding.modules.simpletools.network.ConfigSyncPayload.STREAM_CODEC);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(s -> SimpleTools.onChange = () -> s.getPlayerList().getPlayers().forEach(ToolsFabric::sync));
        // REV2-B-1 (done with group A): do not keep the stopped server referenced.
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s -> SimpleTools.onChange = () -> {});
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((h, sender, s) -> sync(h.player));
    }
    private static void sync(net.minecraft.server.level.ServerPlayer p) {
        if (net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(p, com.simplebuilding.modules.simpletools.network.ConfigSyncPayload.TYPE))
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(p, new com.simplebuilding.modules.simpletools.network.ConfigSyncPayload(SimpleTools.syncJson()));
    }
}

package com.simplebuilding.modules.simpletools;
import com.simplelib.api.InteractionGuard;

import net.neoforged.fml.common.Mod;

@Mod("simpletools")
public final class ToolsNeoForge {
    public ToolsNeoForge(net.neoforged.bus.api.IEventBus bus) {
        SimpleTools.registerMoves();
        SimpleTools.configDir = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get();
        SimpleTools.load();
        // Death Vault: the despawn timer follows the entity's own lifespan (Item#getEntityLifespan, saved "Lifespan")
        com.simplebuilding.modules.simpletools.vault.DeathLedger.lifespanOf = e -> e.lifespan;
        bus.addListener((net.neoforged.neoforge.registries.RegisterEvent e) -> {
            if (e.getRegistryKey().equals(net.minecraft.core.registries.Registries.ITEM)) com.simplebuilding.modules.simpletools.guide.ToolsGuide.register();
        });
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES)) e.accept(com.simplebuilding.modules.simpletools.guide.ToolsGuide.stack(), net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        });
        if (net.neoforged.fml.ModList.get().isLoaded("ftbquests")) com.simplebuilding.modules.simpletools.guide.ToolsGuide.installQuests(SimpleTools.configDir);
        ModuleNeoTests.register(bus);
        bus.addListener((net.neoforged.neoforge.registries.RegisterEvent e) -> {
            var key = e.getRegistryKey();
            if (key.equals(net.minecraft.core.registries.Registries.BLOCK)) ToolsRegistry.registerBlocks();
            if (key.equals(net.minecraft.core.registries.Registries.ITEM)) ToolsRegistry.registerItems();
            if (key.equals(net.minecraft.core.registries.Registries.MENU)) ToolsRegistry.registerMenus();
        });
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS))
                ToolsRegistry.tabStacks().forEach(st -> e.accept(st, net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
            if (e.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES))
                ToolsRegistry.brushStacks().forEach(st -> {
                    var brush = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BRUSH);
                    if (e.getParentEntries().contains(brush)) e.insertAfter(brush, st, net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                    else e.accept(st, net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                });
        });
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.LootTableLoadEvent e) -> ToolsLoot.apply(
                net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, e.getName()), pool -> e.getTable().addPool(pool.build()), e.getRegistries()));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent e) -> ToolsCommands.register(e.getDispatcher()));
        var game = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        InteractionGuard.permission = (p, pos) -> !game.post(new net.neoforged.neoforge.event.level.block.BreakBlockEvent(p.level(), pos, p.level().getBlockState(pos), p)).isCanceled();
        game.addListener(net.neoforged.bus.api.EventPriority.LOWEST, (net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock e) -> {
            if (e.isCanceled()) return;
            var a = FurnaceLavaFillHandler.onRightClickBlock(e.getEntity(), e.getHand(), e.getPos(), e.getFace());
            if (a != net.minecraft.world.InteractionResult.PASS) {
                e.setCancellationResult(a);
                e.setCanceled(true);
            }
        });
        bus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent e) -> e.registrar("1").optional().playToClient(com.simplebuilding.modules.simpletools.network.ConfigSyncPayload.TYPE,
                com.simplebuilding.modules.simpletools.network.ConfigSyncPayload.STREAM_CODEC, (p, c) -> c.enqueueWork(() -> SimpleTools.receiveSync(p.json()))));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStartedEvent e) -> SimpleTools.onChange = () -> e.getServer().getPlayerList().getPlayers().forEach(ToolsNeoForge::sync));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p) sync(p);
        });
    }
    private static void sync(net.minecraft.server.level.ServerPlayer p) {
        if (p.connection.hasChannel(com.simplebuilding.modules.simpletools.network.ConfigSyncPayload.TYPE))
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, new com.simplebuilding.modules.simpletools.network.ConfigSyncPayload(SimpleTools.syncJson()));
    }
}

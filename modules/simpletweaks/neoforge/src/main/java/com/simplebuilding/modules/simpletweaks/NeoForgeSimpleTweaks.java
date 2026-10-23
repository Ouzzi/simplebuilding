package com.simplebuilding.modules.simpletweaks;
import net.neoforged.fml.common.Mod;
@Mod("simpletweaks")
public final class NeoForgeSimpleTweaks {
    public NeoForgeSimpleTweaks(net.neoforged.bus.api.IEventBus bus, net.neoforged.api.distmarker.Dist dist) {
        ModuleNeoTests.register(bus);
        AdminNeo.init(bus, dist); // admin and protection features (T10 T11 T13-T18 T34)
        PlayNeo.init(bus); // player, world and event features (T4-T9 T19-T28 T30-T33)
        ProtectNeo.init(bus); // protection: anti-x-ray, Freecam rule, seed hash, hidden health, action checks
  if(net.neoforged.fml.ModList.get().isLoaded("ftbquests"))com.simplebuilding.modules.simpletweaks.guide.TweaksGuide.installQuests(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get());
  bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent e)->{if(!e.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES)||!com.simplebuilding.framework.api.SubMods.enabled("simpletweaks"))return;var book=new net.minecraft.world.item.ItemStack(com.simplebuilding.modules.simpletweaks.guide.TweaksGuide.book());var deed=new net.minecraft.world.item.ItemStack(com.simplebuilding.modules.simpletweaks.deed.ClaimDeedItem.item());boolean vt=com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get(),"simpletweaks");var anchor=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK);/* before the writable book: a tail append clashes with other mods' insertAfter at the tail */if(e.getParentEntries().contains(anchor)){e.insertBefore(anchor,book,net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);if(vt)e.insertAfter(book,deed,net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);}else{e.accept(book);if(vt)e.accept(deed);}});
        var events=net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        events.addListener((net.neoforged.neoforge.event.server.ServerStartedEvent e)->com.simplebuilding.modules.simpletweaks.claims.Claims.start(e.getServer()));
        events.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent e)->com.simplebuilding.modules.simpletweaks.claims.Claims.stop(e.getServer()));
        events.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent e)->com.simplebuilding.modules.simpletweaks.claims.ClaimCommands.register(e.getDispatcher()));
        bus.addListener((net.neoforged.neoforge.registries.RegisterEvent event) -> {
            if (event.getRegistryKey().equals(net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE)) LegacyDeed.registerComponents();
            if (event.getRegistryKey().equals(net.minecraft.core.registries.Registries.BLOCK)) LegacyDeed.registerBlocks();
            if (event.getRegistryKey().equals(net.minecraft.core.registries.Registries.ITEM)) LegacyDeed.register();
            if (event.getRegistryKey().equals(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB)) LegacyDeed.registerTab();
            if (event.getRegistryKey().equals(net.minecraft.core.registries.Registries.RECIPE_SERIALIZER)) LegacyDeed.registerRecipes();
        });
        bus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) -> {
            var registrar = event.registrar("1").optional();
            registrar.playToServer(com.simplebuilding.modules.simpletweaks.deed.net.DeedEditPayload.TYPE, com.simplebuilding.modules.simpletweaks.deed.net.DeedEditPayload.CODEC,
                    (payload, context) -> context.enqueueWork(() -> com.simplebuilding.modules.simpletweaks.deed.DeedService.handleEdit(payload, (net.minecraft.server.level.ServerPlayer) context.player())));
            registrar.playToServer(com.simplebuilding.modules.simpletweaks.deed.net.DeedClaimPayload.TYPE, com.simplebuilding.modules.simpletweaks.deed.net.DeedClaimPayload.CODEC,
                    (payload, context) -> context.enqueueWork(() -> com.simplebuilding.modules.simpletweaks.deed.DeedService.handleClaim(payload, (net.minecraft.server.level.ServerPlayer) context.player())));
            registrar.playToClient(com.simplebuilding.modules.simpletweaks.deed.net.DeedOpenPayload.TYPE, com.simplebuilding.modules.simpletweaks.deed.net.DeedOpenPayload.CODEC,
                    (payload, context) -> context.enqueueWork(() -> com.simplebuilding.modules.simpletweaks.deed.client.DeedClient.open(payload)));
            registrar.playToClient(com.simplebuilding.modules.simpletweaks.deed.net.DeedResultPayload.TYPE, com.simplebuilding.modules.simpletweaks.deed.net.DeedResultPayload.CODEC,
                    (payload, context) -> context.enqueueWork(() -> com.simplebuilding.modules.simpletweaks.deed.client.DeedClient.result(payload)));
        });
        com.simplebuilding.modules.simpletweaks.deed.net.DeedNet.toClient = (player, payload) -> {
            if (player.connection == null || !player.connection.hasChannel(payload.type())) return false;
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, payload);
            return true;
        };
        if (dist == net.neoforged.api.distmarker.Dist.CLIENT) com.simplebuilding.modules.simpletweaks.NeoTweaksClient.init();
    }
}

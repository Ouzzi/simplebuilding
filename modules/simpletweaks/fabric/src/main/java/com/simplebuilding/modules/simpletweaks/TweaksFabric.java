package com.simplebuilding.modules.simpletweaks;
import net.fabricmc.api.ModInitializer;
public final class TweaksFabric implements ModInitializer {
    @Override public void onInitialize() {
        LegacyDeed.registerComponents();
        LegacyDeed.registerBlocks();
        LegacyDeed.register();
        LegacyDeed.registerTab();
        LegacyDeed.registerRecipes();
        registerDeedNetwork();
        TweaksConfigCommand.announce(); // CMD24-3: /simpletweaks config
        AdminFabric.init(); // admin and protection features (T10 T11 T13-T18 T34)
        PlayFabric.init(); // player, world and event features (T4-T9 T19-T28 T30-T33)
        ProtectFabric.init(); // protection: anti-x-ray, Freecam rule, seed hash, hidden health, action checks
  if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("ftbquests"))com.simplebuilding.modules.simpletweaks.guide.TweaksGuide.installQuests(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir());
  net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES).register(out->{if(!com.simplebuilding.framework.api.SubMods.enabled("simpletweaks"))return;var book=new net.minecraft.world.item.ItemStack(com.simplebuilding.modules.simpletweaks.guide.TweaksGuide.book());var deed=new net.minecraft.world.item.ItemStack(com.simplebuilding.modules.simpletweaks.deed.ClaimDeedItem.item());var items=com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir(),"simpletweaks")?java.util.List.of(book,deed):java.util.List.of(book);/* before the writable book: a tail append clashes with other mods' insertAfter at the tail */if(out.getDisplayStacks().stream().anyMatch(s->s.is(net.minecraft.world.item.Items.WRITABLE_BOOK)))out.insertBefore(net.minecraft.world.item.Items.WRITABLE_BOOK,items,net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);else items.forEach(out::accept);});
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(com.simplebuilding.modules.simpletweaks.claims.Claims::start);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(com.simplebuilding.modules.simpletweaks.claims.Claims::stop);
        com.simplelib.api.PlayerState.onLeave("simpletweaks:roof", com.simplebuilding.modules.simpletweaks.play.roof.NetherRoof::forget);
        com.simplelib.api.PlayerState.onStop("simpletweaks:roof", com.simplebuilding.modules.simpletweaks.play.roof.NetherRoof::forgetAll);
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((d,r,e)->com.simplebuilding.modules.simpletweaks.claims.ClaimCommands.register(d));
    }
    /** Deed packets: edit/claim go to the server, open/result come back. The client side is {@link TweaksFabricClient}. */
    private static void registerDeedNetwork() {
        var registry=net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.serverboundPlay();
        registry.register(com.simplebuilding.modules.simpletweaks.deed.net.DeedEditPayload.TYPE,com.simplebuilding.modules.simpletweaks.deed.net.DeedEditPayload.CODEC);
        registry.register(com.simplebuilding.modules.simpletweaks.deed.net.DeedClaimPayload.TYPE,com.simplebuilding.modules.simpletweaks.deed.net.DeedClaimPayload.CODEC);
        var back=net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay();
        back.register(com.simplebuilding.modules.simpletweaks.deed.net.DeedOpenPayload.TYPE,com.simplebuilding.modules.simpletweaks.deed.net.DeedOpenPayload.CODEC);
        back.register(com.simplebuilding.modules.simpletweaks.deed.net.DeedResultPayload.TYPE,com.simplebuilding.modules.simpletweaks.deed.net.DeedResultPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(com.simplebuilding.modules.simpletweaks.deed.net.DeedEditPayload.TYPE,
                (payload,context)->context.server().execute(()->com.simplebuilding.modules.simpletweaks.deed.DeedService.handleEdit(payload,context.player())));
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(com.simplebuilding.modules.simpletweaks.deed.net.DeedClaimPayload.TYPE,
                (payload,context)->context.server().execute(()->com.simplebuilding.modules.simpletweaks.deed.DeedService.handleClaim(payload,context.player())));
        // map view of the deed (SP22-8 branch 2): chunk tiles with pictures, and its buttons
        registry.register(com.simplebuilding.modules.simpletweaks.deed.net.DeedChunksRequest.TYPE,com.simplebuilding.modules.simpletweaks.deed.net.DeedChunksRequest.CODEC);
        registry.register(com.simplebuilding.modules.simpletweaks.deed.net.DeedActionPayload.TYPE,com.simplebuilding.modules.simpletweaks.deed.net.DeedActionPayload.CODEC);
        back.register(com.simplebuilding.modules.simpletweaks.deed.net.DeedChunksPayload.TYPE,com.simplebuilding.modules.simpletweaks.deed.net.DeedChunksPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(com.simplebuilding.modules.simpletweaks.deed.net.DeedChunksRequest.TYPE,
                (payload,context)->context.server().execute(()->com.simplebuilding.modules.simpletweaks.deed.DeedService.handleChunks(payload,context.player())));
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(com.simplebuilding.modules.simpletweaks.deed.net.DeedActionPayload.TYPE,
                (payload,context)->context.server().execute(()->com.simplebuilding.modules.simpletweaks.deed.DeedService.handleAction(payload,context.player())));
        com.simplebuilding.modules.simpletweaks.deed.net.DeedNet.toClient=(player,payload)->{
            if(!net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player,payload.type()))return false;
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,payload);return true;
        };
    }
}

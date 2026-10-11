package com.simplebuilding.modules.simplemodels;
import net.fabricmc.api.ModInitializer;
public final class FabricExample implements ModInitializer {
    @Override public void onInitialize() {
  com.simplebuilding.modules.simplemodels.guide.ModelsGuide.register();
  if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("ftbquests"))com.simplebuilding.modules.simplemodels.guide.ModelsGuide.installQuests(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir());
  net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES).register(out->{if(!com.simplebuilding.framework.api.SubMods.enabled("simplemodels"))return;var book=com.simplebuilding.modules.simplemodels.guide.ModelsGuide.book();/* before the writable book: a tail append clashes with other mods' insertAfter at the tail */if(out.getDisplayStacks().stream().anyMatch(s->s.is(net.minecraft.world.item.Items.WRITABLE_BOOK)))out.insertBefore(net.minecraft.world.item.Items.WRITABLE_BOOK,java.util.List.of(book),net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);else out.accept(book);});
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(CataloguePayload.ID, CataloguePayload.CODEC);
        Models.root = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("simplemodels");
        Models.send = player -> { if (net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player, CataloguePayload.ID))
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, CataloguePayload.current()); };
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING.register(server -> Models.reload());
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> Models.commands(dispatcher));
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> Models.send.accept(handler.player));
    }
}

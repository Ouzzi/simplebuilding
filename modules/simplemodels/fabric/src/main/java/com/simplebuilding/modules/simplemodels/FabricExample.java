package com.simplebuilding.modules.simplemodels;
import net.fabricmc.api.ModInitializer;
public final class FabricExample implements ModInitializer {
    @Override public void onInitialize() {
  com.simplebuilding.modules.simplemodels.guide.ModelsGuide.register();
  if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("ftbquests"))com.simplebuilding.modules.simplemodels.guide.ModelsGuide.installQuests(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir());
  net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES).register(out->out.accept(com.simplebuilding.modules.simplemodels.guide.ModelsGuide.book()));
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(CataloguePayload.ID, CataloguePayload.CODEC);
        Models.root = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("simplemodels");
        Models.send = player -> { if (net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player, CataloguePayload.ID))
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, CataloguePayload.current()); };
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING.register(server -> Models.reload());
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> Models.commands(dispatcher));
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> Models.send.accept(handler.player));
    }
}

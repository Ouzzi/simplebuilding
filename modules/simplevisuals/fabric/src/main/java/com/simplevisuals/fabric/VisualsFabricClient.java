package com.simplevisuals.fabric;
public final class VisualsFabricClient implements net.fabricmc.api.ClientModInitializer {
 public void onInitializeClient(){
  com.simplevisuals.client.RenamedModels.modelsLoaded=net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simplemodels");
  net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT.register((dispatcher,registry)->com.simplevisuals.client.LocalCommands.register(dispatcher,(source,text)->source.sendFeedback(text)));
 }
}

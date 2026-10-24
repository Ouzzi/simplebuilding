package com.simplevisuals.fabric;
public final class VisualsFabricClient implements net.fabricmc.api.ClientModInitializer {
 public void onInitializeClient(){
  com.simplevisuals.client.RenamedModels.modelsLoaded=net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simplemodels");
  net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT.register((dispatcher,registry)->{
   com.simplevisuals.client.LocalCommands.register(dispatcher,(source,text)->source.sendFeedback(text));
   // CMD24: the client root /simplevisuals hides the server's /simplevisuals config visuals enableAnvilFormatting: hand it on.
   String anvil="simplevisuals config visuals enableAnvilFormatting";
   dispatcher.register(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("simplevisuals")
    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("config").then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("visuals")
     .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("enableAnvilFormatting").executes(c->{com.simplelib.api.client.ServerCommand.send(anvil);return 1;})
      .then(com.simplelib.api.config.ConfigCommands.<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource>forwardToServer(anvil,com.simplelib.api.client.ServerCommand::send))))));
  });
 }
}

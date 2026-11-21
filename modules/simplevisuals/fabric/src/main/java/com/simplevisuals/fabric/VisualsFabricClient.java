package com.simplevisuals.fabric;
public final class VisualsFabricClient implements net.fabricmc.api.ClientModInitializer {
 public void onInitializeClient(){
  com.simplevisuals.client.RenamedModels.modelsLoaded=net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simplemodels");
  // IG31-21: stand under a cauldron on a campfire (render add-on, option visuals.cauldronStand).
  CauldronStandModel.register();
  net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT.register((dispatcher,registry)->{
   com.simplevisuals.client.LocalCommands.register(dispatcher,(source,text)->source.sendFeedback(text));
   // CMD24: the client root /simplevisuals hides the server's /simplevisuals config visuals <policy>: hand both server policies on
   // (enableAnvilFormatting, W3B allowAnvilObfuscation).
   var visuals=net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("visuals");
   for(String policy:new String[]{"enableAnvilFormatting","allowAnvilObfuscation"}){
    String server="simplevisuals config visuals "+policy;
    visuals.then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal(policy).executes(c->{com.simplelib.api.client.ServerCommand.send(server);return 1;})
     .then(com.simplelib.api.config.ConfigCommands.<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource>forwardToServer(server,com.simplelib.api.client.ServerCommand::send)));
   }
   // N136 (pattern N134/N135): any other /simplevisuals ... goes to the server too - the client root shares the name and Fabric
   // does not pass an unknown sub-command on (today the server has only the two policies above; this keeps future ones reachable).
   dispatcher.register(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("simplevisuals")
    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("config").then(visuals))
    .then(com.simplelib.api.config.ConfigCommands.<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource>forwardToServer("simplevisuals",com.simplelib.api.client.ServerCommand::send)));
  });
 }
}

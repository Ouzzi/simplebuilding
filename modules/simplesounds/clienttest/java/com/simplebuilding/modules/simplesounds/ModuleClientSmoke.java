package com.simplebuilding.modules.simplesounds;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
public final class ModuleClientSmoke implements FabricClientGameTest {
 @Override public void runTest(ClientGameTestContext context) {
  if(!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simplesounds"))throw new AssertionError("Module did not boot");
  context.takeScreenshot("simplesounds-title");
  try(var world=context.worldBuilder().create()){
   world.getConnection().waitForClientboundPackets();world.getConnection().waitForChunksRender();
   context.runOnClient(client->call("world",client));context.waitTicks(3);context.takeScreenshot("simplesounds-world");
   context.runOnClient(client->call("config",client));context.waitTicks(3);context.takeScreenshot("simplesounds-config");
   context.runOnClient(client->call("restore",client));
  }
 }
 private static void call(String name,net.minecraft.client.Minecraft client){try{Class.forName("com.simplebuilding.modules.simplesounds.SoundClientAssertions").getMethod(name,net.minecraft.client.Minecraft.class).invoke(null,client);}catch(java.lang.reflect.InvocationTargetException e){throw new AssertionError(name,e.getCause());}catch(Exception e){throw new AssertionError(name,e);}}
}

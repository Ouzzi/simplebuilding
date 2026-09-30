package com.simplebuilding.modules.simplevisuals;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
public final class ModuleClientSmoke implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simplevisuals")) throw new AssertionError("Module did not boot");
        context.runOnClient(client->call("title",client));
        context.takeScreenshot("simplevisuals-title");
        try(var world=context.worldBuilder().create()){
            world.getConnection().waitForClientboundPackets();world.getConnection().waitForChunksRender();
            context.runOnClient(client->call("world",client));context.waitTicks(3);
            context.takeScreenshot("simplevisuals-world");
            context.runOnClient(client->call("prepareModels",client));context.waitTicks(100);
            context.waitFor(client->client.gui.overlay()==null,1200);context.waitTicks(5);
            context.runOnClient(client->call("models",client));context.waitTicks(3);
            context.takeScreenshot("simplevisuals-models");
            context.runOnClient(client->call("anvil",client));context.waitTicks(3);
            context.takeScreenshot("simplevisuals-anvil");
            context.runOnClient(client->call("config",client));context.waitTicks(3);
            context.takeScreenshot("simplevisuals-config");
            context.runOnClient(client->call("restore",client));
        }
    }
    private static void call(String name,net.minecraft.client.Minecraft client){try{Class.forName("com.simplevisuals.client.ClientAssertions").getMethod(name,net.minecraft.client.Minecraft.class).invoke(null,client);}catch(java.lang.reflect.InvocationTargetException e){throw new AssertionError(name,e.getCause());}catch(Exception e){throw new AssertionError(name,e);}}
}

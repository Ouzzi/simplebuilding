package com.simplemoney.clienttest;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
public final class MoneyClientSmoke implements FabricClientGameTest {
 public void runTest(ClientGameTestContext context) {
  if(!FabricLoader.getInstance().isModLoaded("simplemoney") || !FabricLoader.getInstance().isModLoaded("simplebuilding")) throw new AssertionError("Both mods must boot");
  context.takeScreenshot("simplemoney_title");
  try(var world=context.worldBuilder().create()) {
   world.getConnection().waitForClientboundPackets();world.getConnection().waitForChunksRender();
   context.runOnClient(client-> {
    if(client.player==null || client.level==null) throw new AssertionError("Title-to-world transition failed");
    for(String name:java.util.List.of("special_paper","special_fiber","resin_fiber","blank_note","refined_blank_note","raw_bill","money_bill")) {
     var id=Identifier.fromNamespaceAndPath("simplemoney",name);if(!BuiltInRegistries.ITEM.containsKey(id)) throw new AssertionError("Missing item "+id);
    }
    int slot=0;
    for(String name:java.util.List.of("money_bill","special_paper","special_fiber","resin_fiber","blank_note","refined_blank_note","raw_bill")) client.player.getInventory().setItem(slot++,new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("simplemoney",name))));
   });
   context.waitTicks(20);context.takeScreenshot("simplemoney_world");
   context.runOnClient(client-> {
    try {
     var type=Class.forName("com.simplemoney.client.MoneyConfigScreen");
     var screen=(net.minecraft.client.gui.screens.Screen)type.getMethod("create",net.minecraft.client.gui.screens.Screen.class,java.nio.file.Path.class).invoke(null,null,FabricLoader.getInstance().getConfigDir());
     client.gui.setScreen(screen);
    } catch(ReflectiveOperationException e) { throw new AssertionError("Module config screen failed",e); }
   });
   context.waitTicks(5);context.takeScreenshot("simplemoney_config");
   context.runOnClient(client->client.gui.setScreen(null));
  }
 }
}

package com.simplebuilding.integration;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.fabricmc.loader.api.FabricLoader;
public final class RidingClientGameTest implements FabricClientGameTest {
 public void runTest(ClientGameTestContext context) {
  context.runOnClient(client->{
   if(!(client.gui.screen() instanceof TitleScreen))throw new AssertionError("Client reached title screen");
   if(!FabricLoader.getInstance().isModLoaded("simpleriding")||!FabricLoader.getInstance().isModLoaded("simplebuilding"))throw new AssertionError("Both mods boot");
  });
  context.takeScreenshot("riding-title");
  try(var world=context.worldBuilder().create()){
   world.getConnection().waitForClientboundPackets();world.getConnection().waitForChunksRender();
   world.getServer().runOnServer(server->{
    if(server.getResourceManager().listPacks().anyMatch(p->p.packId().equals("trade_rebalance")))throw new AssertionError("Normal world has Trade Rebalance disabled");
    var trades=server.registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE);
    for(int level=2;level<=4;level++){
     var trade=trades.getOrThrow(net.minecraft.resources.ResourceKey.create(Registries.VILLAGER_TRADE,Identifier.fromNamespaceAndPath("simpleriding","librarian/"+level+"/riding_book")));
     var pool=trades.getOrThrow(net.minecraft.tags.TagKey.create(Registries.VILLAGER_TRADE,Identifier.withDefaultNamespace("librarian/level_"+level)));
     if(!pool.contains(trade))throw new AssertionError("Normal world librarian offers riding book at level "+level);
    }
   });
   context.runOnClient(client->{
    if(client.level==null||client.player==null)throw new AssertionError("Title-to-world joins");
    for(String id:new String[]{"tailwind","leaping"})if(client.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getValue(Identifier.fromNamespaceAndPath("simpleriding",id))==null)throw new AssertionError("Enchantment reaches client");
    if(BuiltInRegistries.CREATIVE_MODE_TAB.getValue(Identifier.fromNamespaceAndPath("simpleriding","riding_items"))==null)throw new AssertionError("Creative tab reaches client");
   });
   context.takeScreenshot("riding-world");
   context.runOnClient(client->{try{
    var cls=Class.forName("com.simpleriding.client.RidingConfigScreen");client.setScreenAndShow((Screen)cls.getMethod("create",Screen.class).invoke(null,client.gui.screen()));
   }catch(Exception e){throw new AssertionError("Module config screen opens",e);}});
   context.waitTicks(5);
   context.takeScreenshot("riding-config");
   context.runOnClient(client->client.setScreenAndShow(null));
  }
 }
}

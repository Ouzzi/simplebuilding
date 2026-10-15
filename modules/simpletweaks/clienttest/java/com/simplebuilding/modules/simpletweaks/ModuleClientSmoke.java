package com.simplebuilding.modules.simpletweaks;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
public final class ModuleClientSmoke implements FabricClientGameTest {
 public void runTest(ClientGameTestContext context) {
  context.runOnClient(client->{if(!FabricLoader.getInstance().isModLoaded("simpletweaks") || !FabricLoader.getInstance().isModLoaded("simplebuilding"))throw new AssertionError("Both mods boot");});
  context.takeScreenshot("simpletweaks-title");
  try(var world=context.worldBuilder().create()) {
   world.getConnection().waitForClientboundPackets();world.getConnection().waitForChunksRender();
   world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("simpletweaks","claim_deed"))));});
   context.runOnClient(client->{if(client.level==null || client.player==null)throw new AssertionError("Title to world");if(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("simpletweaks","elytra_pad"))!=BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("simplebuilding","elytra_pad")))throw new AssertionError("Client aliases");});
   context.waitTicks(20);context.takeScreenshot("simpletweaks-world");
   context.runOnClient(client->{client.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(client.player));});context.waitTicks(10);context.takeScreenshot("simpletweaks-deed");
   deedEditor(context,world);
  }
 }
 /** Claim deed (phase 2) end to end on a real client: sheet, claim, editor, error line, valid code, Done, ledger. */
 private static void deedEditor(ClientGameTestContext context,net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext world) {
  var input=context.getInput();
  var item=BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("simpletweaks","claim_deed"));
  context.runOnClient(client->client.gui.setScreen(null));
  world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.teleportTo(4000.5,120,4000.5);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item));});
  world.getConnection().waitForClientboundPackets();world.getConnection().waitForChunksRender();context.waitTicks(20);
  world.getServer().runOnServer(server->{if(!com.simplebuilding.modules.simpletweaks.deed.DeedService.open(server.getPlayerList().getPlayers().getFirst(),0))throw new AssertionError("Deed screen is sent to a client with the mod");});
  context.waitForScreen(com.simplebuilding.modules.simpletweaks.deed.client.DeedScreen.class);
  context.takeScreenshot("simpletweaks-deed-claim");
  context.clickScreenButton("simpletweaks.deed.claim");
  context.waitTicks(10);
  world.getServer().runOnServer(server->{
   var p=server.getPlayerList().getPlayers().getFirst();var claims=com.simplebuilding.modules.simpletweaks.claims.Claims.get(server);
   var data=p.getMainHandItem().get(com.simplebuilding.modules.simpletweaks.deed.DeedData.TYPE);
   if(claims.view().size()!=1||data==null||!data.owner().equals(p.getUUID()))throw new AssertionError("The sheet claims the chunk and binds the deed: "+claims.view().size()+" "+data);
   com.simplebuilding.modules.simpletweaks.deed.DeedService.open(p,0);
  });
  context.waitForScreen(com.simplebuilding.modules.simpletweaks.deed.client.DeedScreen.class);
  context.waitTicks(5);context.takeScreenshot("simpletweaks-deed-editor");
  input.pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN);input.typeChars("*: U");input.pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN);input.typeChars("Zed: Q");
  context.waitTicks(5);context.takeScreenshot("simpletweaks-deed-error");
  for(int i=0;i<6;i++)input.pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_BACKSPACE);
  input.typeChars("069a79f4-44e9-4726-a5be-fca90e38aaf5: BDU");
  context.waitTicks(5);context.takeScreenshot("simpletweaks-deed-valid");
  // Right after the claim the shared cooldown (100 ticks) still runs: the editor stays open and says so.
  context.clickScreenButton("gui.done");
  context.waitTicks(10);context.takeScreenshot("simpletweaks-deed-cooldown");
  context.runOnClient(client->{if(!(client.gui.screen() instanceof com.simplebuilding.modules.simpletweaks.deed.client.DeedScreen))throw new AssertionError("A refused save keeps the editor open");});
  context.waitTicks(110);
  context.clickScreenButton("gui.done");
  context.waitFor(client->client.gui.screen()==null,200);
  world.getServer().runOnServer(server->{
   var p=server.getPlayerList().getPlayers().getFirst();var land=com.simplebuilding.modules.simpletweaks.claims.Claims.get(server).lands().get(p.getUUID());
   if(land==null||land.top().everyone()!=com.simplebuilding.modules.simpletweaks.claims.ClaimRights.USE||land.top().members().size()!=1||!land.code().contains("Claim rights"))throw new AssertionError("Done saved the code on the server: "+land);
   // somebody else's deed: reader sheet with a note
   p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item));
   var other=new ItemStack(item);other.set(com.simplebuilding.modules.simpletweaks.deed.DeedData.TYPE,new com.simplebuilding.modules.simpletweaks.deed.DeedData(java.util.UUID.randomUUID(),"Nobody",1));
   p.setItemInHand(InteractionHand.MAIN_HAND,other);
   com.simplebuilding.modules.simpletweaks.deed.DeedService.open(p,0);
  });
  context.waitForScreen(com.simplebuilding.modules.simpletweaks.deed.client.DeedScreen.class);
  context.waitTicks(3);context.takeScreenshot("simpletweaks-deed-read");
  context.runOnClient(client->client.gui.setScreen(null));
 }
}

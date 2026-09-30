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
  }
 }
}

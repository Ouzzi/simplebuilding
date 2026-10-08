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
   world.getServer().runOnServer(server->{
    var player=server.getPlayerList().getPlayers().getFirst();var level=player.level();
    var mount=net.minecraft.world.entity.EntityTypes.NAUTILUS.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);
    mount.snapTo(player.getX()+2,player.getY(),player.getZ(),0,0);mount.tame(player);
    var lookup=level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
    var saddle=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SADDLE);
    saddle.enchant(lookup.getOrThrow(net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT,Identifier.parse("simpleriding:tailwind"))),3);
    var armor=new net.minecraft.world.item.ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("simplebuilding:enderite_nautilus_armor")));
    armor.enchant(lookup.getOrThrow(net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT,Identifier.parse("simpleriding:leaping"))),3);
    mount.setItemSlot(net.minecraft.world.entity.EquipmentSlot.SADDLE,saddle);mount.setItemSlot(net.minecraft.world.entity.EquipmentSlot.BODY,armor);
    level.addFreshEntity(mount);player.startRiding(mount,true,true);
   });
   context.waitTicks(10);world.getConnection().waitForClientboundPackets();
   context.runOnClient(client->{
    if(!(client.player.getVehicle() instanceof net.minecraft.world.entity.animal.nautilus.AbstractNautilus mount))throw new AssertionError("Nautilus riding reaches client");
    var bonus=mount.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getModifier(Identifier.parse("simpleriding:tailwind_boost"));
    if(bonus==null||Math.abs(bonus.amount()-.6)>1e-5)throw new AssertionError("Server Tailwind attribute reaches client");
    client.getConnection().send(new net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket(client.player,net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.START_RIDING_JUMP,Integer.MAX_VALUE));
   });
   context.waitTicks(2);
   world.getServer().runOnServer(server->{var mount=(net.minecraft.world.entity.animal.nautilus.AbstractNautilus)server.getPlayerList().getPlayers().getFirst().getVehicle();if(mount.getJumpCooldown()!=0)throw new AssertionError("Server refuses forged client dash charge");});
   context.runOnClient(client->client.getConnection().send(new net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket(client.player,net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.START_RIDING_JUMP,100)));
   context.waitTicks(2);
   world.getServer().runOnServer(server->{var player=server.getPlayerList().getPlayers().getFirst();var mount=(net.minecraft.world.entity.animal.nautilus.AbstractNautilus)player.getVehicle();if(mount.getJumpCooldown()<=0)throw new AssertionError("Server executes valid client dash");player.stopRiding();mount.discard();});
   double[] ghastStart=new double[2];
   world.getServer().runOnServer(server->{
    var player=server.getPlayerList().getPlayers().getFirst();var mount=net.minecraft.world.entity.EntityTypes.HAPPY_GHAST.create(player.level(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);
    mount.snapTo(player.getX(),player.getY()+6,player.getZ(),0,0);
    var harness=new net.minecraft.world.item.ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("white_harness")));
    harness.enchant(player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT,Identifier.parse("simpleriding:tailwind"))),3);
    mount.setItemSlot(net.minecraft.world.entity.EquipmentSlot.BODY,harness);player.level().addFreshEntity(mount);player.startRiding(mount,true,true);
    ghastStart[0]=mount.getX();ghastStart[1]=mount.getZ();
   });
   context.waitTicks(10);world.getConnection().waitForClientboundPackets();
   context.runOnClient(client->client.options.keyUp.setDown(true));
   context.waitTicks(60);
   context.runOnClient(client->{
    client.options.keyUp.setDown(false);var mount=(net.minecraft.world.entity.animal.happyghast.HappyGhast)client.player.getVehicle();
    double velocity=mount.getDeltaMovement().length();if(velocity<.4||velocity>1)throw new AssertionError("Happy Ghast flight remains useful and physically capped: "+velocity);
   });
   context.runOnClient(client->{client.player.setXRot(-60);client.options.keyUp.setDown(true);client.options.keyLeft.setDown(true);client.options.keyJump.setDown(true);});
   context.waitTicks(40);
   context.runOnClient(client->{
    client.options.keyUp.setDown(false);client.options.keyLeft.setDown(false);client.options.keyJump.setDown(false);
    double velocity=client.player.getVehicle().getDeltaMovement().length();if(velocity<.4||velocity>1.4)throw new AssertionError("Combined Ghast steering/ascending remains bounded: "+velocity);
   });
   world.getServer().runOnServer(server->{
    var player=server.getPlayerList().getPlayers().getFirst();var mount=player.getVehicle();
    double travel=Math.hypot(mount.getX()-ghastStart[0],mount.getZ()-ghastStart[1]);if(travel<5||travel>70)throw new AssertionError("Server accepts bounded Ghast flight: "+travel);
    player.stopRiding();mount.discard();
   });
   context.takeScreenshot("riding-world");
   // Hoof panel in the container style (SimpleLib style blocks): open a tamed horse's inventory.
   world.getServer().runOnServer(server->{
    var player=server.getPlayerList().getPlayers().getFirst();
    var horse=net.minecraft.world.entity.EntityTypes.HORSE.create(player.level(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);
    horse.snapTo(player.getX()+2,player.getY(),player.getZ(),0,0);horse.tameWithName(player);
    player.level().addFreshEntity(horse);
   });
   context.waitTicks(5);world.getConnection().waitForClientboundPackets();
   world.getServer().runOnServer(server->{
    var player=server.getPlayerList().getPlayers().getFirst();
    player.level().getEntities((net.minecraft.world.entity.Entity)null,player.getBoundingBox().inflate(8),e->e instanceof net.minecraft.world.entity.animal.equine.Horse)
     .forEach(e->((net.minecraft.world.entity.animal.equine.Horse)e).openCustomInventoryScreen(player));
   });
   context.waitTicks(10);world.getConnection().waitForClientboundPackets();
   context.runOnClient(client->{
    if(!(client.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.HorseInventoryScreen screen))throw new AssertionError("Horse inventory opens: "+client.gui.screen());
    if(!com.simpleriding.HorseshoePanel.hasShoeSlots(screen.getMenu()))throw new AssertionError("Horse inventory has the hoof slots");
   });
   context.takeScreenshot("riding-horse-panel");
   context.runOnClient(client->client.setScreenAndShow(null));
   world.getServer().runOnServer(server->server.getPlayerList().getPlayers().getFirst().level().getEntities((net.minecraft.world.entity.Entity)null,new net.minecraft.world.phys.AABB(-1e4,-64,-1e4,1e4,320,1e4),e->e instanceof net.minecraft.world.entity.animal.equine.Horse).forEach(net.minecraft.world.entity.Entity::discard));
   context.runOnClient(client->{try{
    var cls=Class.forName("com.simpleriding.client.RidingConfigScreen");var screen=(Screen)cls.getMethod("create",Screen.class).invoke(null,client.gui.screen());
    var categories=(java.util.Map<?,?>)screen.getClass().getMethod("getCategorizedEntries").invoke(screen);
    if(categories.size()!=3||categories.values().stream().mapToInt(v->((java.util.List<?>)v).size()).sum()!=18)throw new AssertionError("All server options appear in three config tabs");
    client.setScreenAndShow(screen);
   }catch(Exception e){throw new AssertionError("Module config screen opens",e);}});
   context.waitTicks(5);
   context.takeScreenshot("riding-config");
   context.runOnClient(client->client.setScreenAndShow(null));
  }
 }
}

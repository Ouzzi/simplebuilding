package com.simplebuilding.modules.simpledimensions;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
import net.minecraft.world.level.block.Blocks;
public final class ModuleClientSmoke implements FabricClientGameTest {
 public void runTest(ClientGameTestContext context){
  context.runOnClient(client->{if(!(client.gui.screen() instanceof TitleScreen)||!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simpledimension"))throw new AssertionError("Module title boot");});
  context.takeScreenshot("simpledimensions-title");
  try(var world=context.worldBuilder().create()){
   world.getConnection().waitForClientboundPackets();world.getConnection().waitForChunksRender();
   world.getServer().runOnServer(server->{
    for(String name:java.util.List.of("skyblock","mining","travel"))if(server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,DimensionRegistry.id(name)))==null)throw new AssertionError("Normal world dimension "+name);
    var p=server.getPlayerList().getPlayers().getFirst();var l=p.level();var base=p.blockPosition().offset(-2,0,-5);
    for(int x=0;x<4;x++)for(int y=0;y<4;y++){
     var q=base.offset(x,y,0);boolean inside=x>0&&x<3&&y<3;
     l.setBlock(q,inside?DimensionRegistry.PORTAL.defaultBlockState():((y<3||x==1||x==2)?Blocks.GLOWSTONE:Blocks.AIR).defaultBlockState(),3);
     if(inside&&l.getBlockEntity(q) instanceof SkyPortalBlockEntity b){b.configure(0x66D9FF,"simpledimension:skyblock");l.sendBlockUpdated(q,l.getBlockState(q),l.getBlockState(q),3);}
    }
    l.setBlock(base.offset(0,0,2),Blocks.SEA_LANTERN.defaultBlockState(),3);
    p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,net.minecraft.world.phys.Vec3.atCenterOf(base.offset(2,1,0)));
   });
   world.getConnection().waitForClientboundPackets();world.getConnection().waitForChunksRender();context.waitTicks(20);
   context.runOnClient(client->{if(client.level==null||client.player==null)throw new AssertionError("Title to world");});
   context.takeScreenshot("simpledimensions-portal");
   context.runOnClient(client->client.setScreenAndShow(com.simplebuilding.modules.simpledimensions.client.DimensionConfigScreen.create(client.gui.screen())));
   context.waitTicks(5);context.takeScreenshot("simpledimensions-config");
   context.runOnClient(client->client.setScreenAndShow(null));
  }
 }
}

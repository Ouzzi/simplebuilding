package com.simplebuilding.modules.simpledimensions;
import dev.simpledimension.common.portal.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
public record PortalWorld(ServerLevel level,DimensionPortalConfig config) implements PortalWorldView {
 public boolean isFrame(int x,int y,int z){return isBlock(x,y,z,config.frameBlock);}
 public boolean isBlock(int x,int y,int z,String id){var key=Identifier.tryParse(id);return key!=null&&BuiltInRegistries.BLOCK.containsKey(key)&&level.getBlockState(new BlockPos(x,y,z)).is(BuiltInRegistries.BLOCK.getValue(key));}
 public boolean isEmpty(int x,int y,int z){var s=level.getBlockState(new BlockPos(x,y,z));return s.isAir()||s.is(Blocks.FIRE)||s.is(Blocks.SOUL_FIRE);}
 public boolean isPortal(int x,int y,int z){return DimensionRegistry.portal(level.getBlockState(new BlockPos(x,y,z)));}
 public boolean separateLight(MatchedPortal shape){
  for(var c:shape.interior())for(int dx=-3;dx<=3;dx++)for(int dy=-3;dy<=3;dy++)for(int dz=-3;dz<=3;dz++) {
   var s=level.getBlockState(new BlockPos(c.x()+dx,c.y()+dy,c.z()+dz));
   if(!s.is(Blocks.GLOWSTONE)&&!s.is(Blocks.FIRE)&&!s.is(Blocks.SOUL_FIRE)&&!s.is(Blocks.NETHER_PORTAL)&&!s.is(Blocks.END_PORTAL)&&!s.is(Blocks.END_GATEWAY)&&!DimensionRegistry.portal(s)&&s.getLightEmission()>0)return true;
  }
  return false;
 }
}

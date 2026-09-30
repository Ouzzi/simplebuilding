package com.simplebuilding.modules.simpledimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;
/** Rounded, immutable arrival island. Bedrock has no survival drop or mining reward. */
public final class DestinationPlatform {
 public static Map<BlockPos,BlockState> plan(BlockPos feet,int diameter){
  if(diameter<7||diameter>27)throw new IllegalArgumentException("Island diameter");
  Map<BlockPos,BlockState> out=new LinkedHashMap<>();double center=(diameter-1)/2.0,radius=diameter/2.0;
  for(int x=0;x<diameter;x++)for(int z=0;z<diameter;z++){
   double d=(x-center)*(x-center)+(z-center)*(z-center);if(d>=radius*radius)continue;
   int depth=Math.max(1,(int)Math.ceil(Math.sqrt(radius*radius-d)/2));
   for(int y=1;y<=depth;y++)out.put(feet.offset(x-diameter/2,-y,z-diameter/2),Blocks.BEDROCK.defaultBlockState());
  }
  return out;
 }
}

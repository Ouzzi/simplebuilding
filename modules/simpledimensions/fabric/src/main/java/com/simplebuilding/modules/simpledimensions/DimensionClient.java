package com.simplebuilding.modules.simpledimensions;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
public final class DimensionClient implements ClientModInitializer {
 public void onInitializeClient(){BlockColorRegistry.register((state,getter,pos,tints)->{
  int color=0x66D9FF;if(getter!=null&&pos!=null&&getter.getBlockEntity(pos) instanceof SkyPortalBlockEntity b)color=b.getColor();tints.add(0xFF000000|color);
 },DimensionRegistry.PORTAL,DimensionRegistry.LEGACY);}
}

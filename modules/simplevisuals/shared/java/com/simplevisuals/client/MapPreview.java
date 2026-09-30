package com.simplevisuals.client;
public record MapPreview(net.minecraft.world.level.saveddata.maps.MapId id,net.minecraft.world.level.saveddata.maps.MapItemSavedData data)
 implements net.minecraft.world.inventory.tooltip.TooltipComponent,net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent {
 public int getWidth(net.minecraft.client.gui.Font f){return 128;}
 public int getHeight(net.minecraft.client.gui.Font f){return 152;}
 public void extractImage(net.minecraft.client.gui.Font f,int x,int y,int w,int h,net.minecraft.client.gui.GuiGraphicsExtractor g){
  var mc=net.minecraft.client.Minecraft.getInstance();var texture=mc.getMapTextureManager().prepareMapTexture(id,data);
  g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,texture,x,y,0,0,128,128,128,128);
  g.text(f,net.minecraft.network.chat.Component.translatable("simplevisuals.map_center",data.centerX,data.centerZ),x,y+132,0xffffffff);
  g.text(f,data.dimension.identifier().toString(),x,y+142,0xffaaaaaa);
 }
}

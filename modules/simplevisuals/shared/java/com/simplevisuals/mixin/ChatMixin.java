package com.simplevisuals.mixin;
import com.simplevisuals.Visuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

@Mixin(ChatComponent.class)
public abstract class ChatMixin {
 @Shadow @Final private List<GuiMessage.Line> trimmedMessages;
 @Shadow private int chatScrollbarPos;
 @Shadow protected abstract double getScale();
 @Shadow protected abstract int getLineHeight();
 @ModifyVariable(method="addMessage",at=@At("HEAD"),argsOnly=true)
 private Component visuals$death(Component message){
  var c=Visuals.CONFIG.visuals; var mc=Minecraft.getInstance();
  if(!c.enhanceDeathMessages||c.deathCoordsMode==com.simplevisuals.config.SimplevisualsConfig.Visuals.DeathCoordsMode.DISABLED||mc.player==null)return message;
  if(message.getContents() instanceof TranslatableContents t&&t.getKey().startsWith("death.")&&t.getArgs().length>0&&t.getArgs()[0] instanceof Component victim&&victim.getString().equals(mc.player.getName().getString())){
   var pos=mc.player.blockPosition();var coords=Component.translatable("simplevisuals.death_location",pos.getX(),pos.getY(),pos.getZ()).withStyle(net.minecraft.ChatFormatting.GRAY);
   // Local only. Never disclose another player's coordinates or send a chat packet.
   return message.copy().append(c.deathCoordsMode==com.simplevisuals.config.SimplevisualsConfig.Visuals.DeathCoordsMode.SEPARATE?"\n":" ").append(coords);
  }return message;
 }
 @Inject(method="extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",at=@At("TAIL"))
 private void visuals$heads(GuiGraphicsExtractor g,net.minecraft.client.gui.Font font,int time,int mx,int my,ChatComponent.DisplayMode mode,boolean bool,CallbackInfo ci){
  var mc=Minecraft.getInstance();if(!Visuals.CONFIG.visuals.enableChatHeads||mc.getConnection()==null||mc.gui.hud.isHidden())return;
  double scale=getScale();g.pose().pushMatrix();g.pose().scale((float)scale);int bottom=(int)((g.guiHeight()-40)/scale);
  int n=Math.min(20,trimmedMessages.size()-chatScrollbarPos);
  for(int i=0;i<n;i++){var line=trimmedMessages.get(i+chatScrollbarPos);if(time-line.addedTime()>200&&!((ChatComponent)(Object)this).isChatFocused())continue;
   var message=line.parent().content();if(!(message.getContents() instanceof TranslatableContents t)||!t.getKey().startsWith("chat.type.")||t.getArgs().length==0||!(t.getArgs()[0] instanceof Component name))continue;
   var player=mc.getConnection().getPlayerInfo(name.getString());if(player==null)continue;
   var skin=player.getSkin().body().texturePath();int y=bottom-i*getLineHeight()-9;
   // Keep vanilla text and link hitboxes intact; heads occupy the chat's right margin.
   int x=(int)(ChatComponent.getWidth(mc.options.chatWidth().get())/scale)+6;
   g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,skin,x,y,8,8,8,8,8,8,64,64);
   g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,skin,x,y,40,8,8,8,8,8,64,64);
  }g.pose().popMatrix();
 }
}

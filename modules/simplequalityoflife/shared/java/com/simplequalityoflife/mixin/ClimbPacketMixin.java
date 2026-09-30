package com.simplequalityoflife.mixin;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ClimbPacketMixin implements com.simplequalityoflife.util.ClimbAudit {
 @Shadow public ServerPlayer player;
 @Unique private int qol$baseTick=-1;
 @Unique private double qol$baseY;
 @Unique private int qol$budgetTicks=1;
 @Unique private long qol$rejectedPackets;
 public long qol$rejectedMovementPackets(){return qol$rejectedPackets;}
 @Inject(method="handleMovePlayer",at=@At("HEAD"),cancellable=true)
 private void qol$validate(ServerboundMovePlayerPacket packet,CallbackInfo ci){
  if(!player.level().getServer().isSameThread())return;
  if(!player.onClimbable()||player.getAbilities().flying||player.isPassenger()){qol$baseTick=-1;return;}
  int tick=player.tickCount;
  if(qol$baseTick!=tick){qol$budgetTicks=qol$baseTick<0?1:Math.clamp(tick-qol$baseTick,1,3);qol$baseY=player.getY();qol$baseTick=tick;}
  if(!com.simplequalityoflife.util.ClimbSecurity.allowed(packet.getY(player.getY())-qol$baseY,qol$budgetTicks,com.simplequalityoflife.Simplequalityoflife.getConfig())){
   qol$rejectedPackets++;
   player.connection.teleport(player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot());ci.cancel();
  }
 }
}

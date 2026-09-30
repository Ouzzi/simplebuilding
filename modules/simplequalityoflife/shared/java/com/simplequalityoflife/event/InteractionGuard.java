package com.simplequalityoflife.event;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
public final class InteractionGuard {
 public static final double MAX_REACH=4.5;
 public static java.util.function.BiPredicate<Player,BlockPos> permission=(p,pos)->true;
 public static boolean mayChange(Player p,BlockPos pos){return allow(p,pos)&&permission.test(p,pos);}
 private static final java.util.Map<Player,Integer> LAST=new java.util.WeakHashMap<>();
 public static boolean action(Player p){Integer last=LAST.get(p);if(last!=null&&p.tickCount-last<4)return false;LAST.put(p,p.tickCount);return true;}
 public static boolean allow(Player p,BlockPos pos){
  var world=p.level();
  return !p.isSpectator() && p.mayBuild() && world.hasChunkAt(pos) && world.getWorldBorder().isWithinBounds(pos)
   && p.getEyePosition().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos))<=MAX_REACH*MAX_REACH && world.mayInteract(p,pos)
   && p.mayUseItemAt(pos,net.minecraft.core.Direction.UP,p.getMainHandItem());
 }
}

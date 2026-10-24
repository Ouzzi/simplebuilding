package com.simplebuilding.modules.simplemovement;
import com.simplebuilding.modules.simplemovement.MovementConfig;
/** Server packet budget. Multiple packets cannot buy additional distance in one tick. */
public final class ClimbSecurity {
 public static final double POSITION_TOLERANCE=0.05;
 public static boolean allowed(double delta,int elapsed,MovementConfig c){return allowed(delta,elapsed,c,0);}
 /** {@code extraUp}: upward distance per tick the server knows a reason for ({@link #extraUp}). */
 public static boolean allowed(double delta,int elapsed,MovementConfig c,double extraUp){
  c.normalizeNumbers();int ticks=Math.clamp(elapsed,1,3);
  return Double.isFinite(delta)&&delta<=(c.ladderClimbingSpeed+POSITION_TOLERANCE+Math.max(0,extraUp))*ticks
   && delta>=-(c.enableFastLadderSlide?c.ladderSlideSpeed+POSITION_TOLERANCE:0.2)*ticks;
 }
 /** Jump Boost lifts a jump off the ladder's foot by 0.1 per level (Vanilla jump 0.42 fits the climb budget on its own). */
 public static double extraUp(net.minecraft.world.entity.player.Player p){
  var boost=p.getEffect(net.minecraft.world.effect.MobEffects.JUMP_BOOST);
  return boost==null?0:0.1*(boost.getAmplifier()+1);
 }
 /** Vanilla moves the player up on its own (Levitation, Riptide, Elytra): no ladder budget applies. */
 public static boolean launched(net.minecraft.world.entity.player.Player p){
  return p.hasEffect(net.minecraft.world.effect.MobEffects.LEVITATION)||p.isAutoSpinAttack()||p.isFallFlying();
 }
}

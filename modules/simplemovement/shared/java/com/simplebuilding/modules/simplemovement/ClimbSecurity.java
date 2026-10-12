package com.simplebuilding.modules.simplemovement;
import com.simplebuilding.modules.simplemovement.MovementConfig;
/** Server packet budget. Multiple packets cannot buy additional distance in one tick. */
public final class ClimbSecurity {
 public static final double POSITION_TOLERANCE=0.05;
 public static boolean allowed(double delta,int elapsed,MovementConfig c){
  c.normalizeNumbers();int ticks=Math.clamp(elapsed,1,3);
  return Double.isFinite(delta)&&delta<=(c.ladderClimbingSpeed+POSITION_TOLERANCE)*ticks
   && delta>=-(c.enableFastLadderSlide?c.ladderSlideSpeed+POSITION_TOLERANCE:0.2)*ticks;
 }
}

package com.simplevisuals;
/** Pure speed-to-look mapping for the speed lines (blocks per tick in, factors out); no client classes so it is testable headless. */
public final class SpeedLinesMath {
 /** Speed (blocks/tick) at which tempo and density equal the configured base values (about sprinting/elytra start). */
 public static final double REFERENCE=.5;
 public static final float MIN_TEMPO=.4f,MAX_TEMPO=8f,MIN_DENSITY=.6f,MAX_DENSITY=1.6f;
 private SpeedLinesMath(){}
 /** Animation tempo factor: proportional to speed, clamped so slow stays visible and very fast stays readable. */
 public static float tempo(double speed){return clamp(speed/REFERENCE,MIN_TEMPO,MAX_TEMPO);}
 /** Line count factor: grows with the square root of speed so density rises slower than tempo. */
 public static float density(double speed){return clamp(Math.sqrt(Math.max(0,speed)/REFERENCE),MIN_DENSITY,MAX_DENSITY);}
 private static float clamp(double v,float lo,float hi){return Double.isNaN(v)?lo:(float)Math.max(lo,Math.min(hi,v));}
}

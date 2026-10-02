package com.simplevisuals.effects;
import com.simplebuilding.framework.api.CosmeticIntensity;
public enum Intensity { OFF, SUBTLE, NORMAL, STRONG, MAXIMUM;
 public int count(){return ordinal();}
 /** Explicit mapping to the shared framework level (exhaustive switch: a new constant fails to compile instead of failing at runtime). */
 public CosmeticIntensity.Level shared(){
  return switch(this){
   case OFF -> CosmeticIntensity.Level.OFF;
   case SUBTLE -> CosmeticIntensity.Level.SUBTLE;
   case NORMAL -> CosmeticIntensity.Level.NORMAL;
   case STRONG -> CosmeticIntensity.Level.STRONG;
   case MAXIMUM -> CosmeticIntensity.Level.MAXIMUM;
  };
 }
}

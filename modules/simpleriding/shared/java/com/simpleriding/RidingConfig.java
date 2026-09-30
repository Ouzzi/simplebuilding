package com.simpleriding;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.*;
/** Server-owned settings; original JSON paths remain compatible. */
public final class RidingConfig {
 public WorldGen worldGen = new WorldGen();
 public Enchantments enchantments = new Enchantments();
 public static class WorldGen { public boolean enableVillagerTrades=true, enableLootTableChanges=true; }
 public static class Enchantments { public SwiftRide swiftRide=new SwiftRide(); public HorseJump horseJump=new HorseJump(); }
 public static class SwiftRide { public float ghastSpeedMultiplier=.85f, horseSpeedMultiplier=.3f, otherSpeedMultiplier=.2f; }
 public static class HorseJump { public float jumpStrengthMultiplier=.2f; }
 public static final float MAX_SPEED_PER_LEVEL=1f, MAX_JUMP_PER_LEVEL=.5f;
 public static float bounded(float value,float fallback,float max) { return Float.isFinite(value)?Math.max(0,Math.min(max,value)):fallback; }
 public void normalize() {
  if(worldGen==null) worldGen=new WorldGen(); if(enchantments==null) enchantments=new Enchantments();
  if(enchantments.swiftRide==null) enchantments.swiftRide=new SwiftRide(); if(enchantments.horseJump==null) enchantments.horseJump=new HorseJump();
  var s=enchantments.swiftRide; s.ghastSpeedMultiplier=bounded(s.ghastSpeedMultiplier,.85f,MAX_SPEED_PER_LEVEL);
  s.horseSpeedMultiplier=bounded(s.horseSpeedMultiplier,.3f,MAX_SPEED_PER_LEVEL); s.otherSpeedMultiplier=bounded(s.otherSpeedMultiplier,.2f,MAX_SPEED_PER_LEVEL);
  enchantments.horseJump.jumpStrengthMultiplier=bounded(enchantments.horseJump.jumpStrengthMultiplier,.2f,MAX_JUMP_PER_LEVEL);
 }
 public static RidingConfig load(Path directory) {
  var gson=new GsonBuilder().setPrettyPrinting().create(); var path=directory.resolve("simpleriding.json");
  try { RidingConfig c=Files.exists(path)?gson.fromJson(Files.readString(path),RidingConfig.class):new RidingConfig();
   if(c==null)c=new RidingConfig(); c.normalize(); if(!Files.exists(path)){ Files.createDirectories(directory); Files.writeString(path,gson.toJson(c)); } return c;
  } catch(Exception e) { throw new IllegalStateException("Cannot read Simple Riding server config "+path,e); }
 }
}

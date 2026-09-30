package com.simpleriding;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.*;
/** Server-owned settings; original JSON paths remain compatible. */
public final class RidingConfig {
 public WorldGen worldGen = new WorldGen();
 public Safety safety = new Safety();
 public Enchantments enchantments = new Enchantments();
 public static class WorldGen { public boolean enableVillagerTrades=true, enableLootTableChanges=true; }
 public static class Enchantments { public SwiftRide swiftRide=new SwiftRide(); public HorseJump horseJump=new HorseJump(); }
 public static class Safety { public boolean enableTailwind=true, enableLeaping=true, enableArmorUtilities=true, enableNautilus=true; public float maximumSpeedBonus=3f, maximumJumpBonus=1.5f, movementDistancePerTick=4f; public int movementPacketsPerTick=20; }
 public static class SwiftRide { public float nautilusSpeedMultiplier=.2f, ghastSpeedMultiplier=.85f, horseSpeedMultiplier=.3f, otherSpeedMultiplier=.2f; }
 public static class HorseJump { public float nautilusDashMultiplier=.2f, featherFallingReduction=.12f; public int armorEnchantability=15; public float jumpStrengthMultiplier=.2f; }
 public static final float MAX_SPEED_PER_LEVEL=1f, MAX_JUMP_PER_LEVEL=.5f;
 public static float bounded(float value,float fallback,float max) { return Float.isFinite(value)?Math.max(0,Math.min(max,value)):fallback; }
 public void normalize() {
  if(safety==null)safety=new Safety();
  safety.maximumSpeedBonus=bounded(safety.maximumSpeedBonus,3,3); safety.maximumJumpBonus=bounded(safety.maximumJumpBonus,1.5f,1.5f);
  safety.movementDistancePerTick=Math.max(.5f,bounded(safety.movementDistancePerTick,4,4)); safety.movementPacketsPerTick=Math.max(1,Math.min(20,safety.movementPacketsPerTick));
  if(worldGen==null) worldGen=new WorldGen(); if(enchantments==null) enchantments=new Enchantments();
  if(enchantments.swiftRide==null) enchantments.swiftRide=new SwiftRide(); if(enchantments.horseJump==null) enchantments.horseJump=new HorseJump();
  var s=enchantments.swiftRide; s.nautilusSpeedMultiplier=bounded(s.nautilusSpeedMultiplier,.2f,MAX_SPEED_PER_LEVEL);
  var j=enchantments.horseJump; j.nautilusDashMultiplier=bounded(j.nautilusDashMultiplier,.2f,MAX_JUMP_PER_LEVEL); j.featherFallingReduction=bounded(j.featherFallingReduction,.12f,.12f); j.armorEnchantability=Math.max(0,Math.min(15,j.armorEnchantability)); s.ghastSpeedMultiplier=bounded(s.ghastSpeedMultiplier,.85f,MAX_SPEED_PER_LEVEL);
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

package com.simpleriding;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.*;
/** Server-owned settings; original JSON paths remain compatible. */
public final class RidingConfig {
 public WorldGen worldGen = new WorldGen();
 public Safety safety = new Safety();
 public Enchantments enchantments = new Enchantments();
 public HorseshoeSettings horseshoes = new HorseshoeSettings();
 public static class WorldGen { public boolean enableVillagerTrades=true, enableLootTableChanges=true; }
 public static class Enchantments { public SwiftRide swiftRide=new SwiftRide(); public HorseJump horseJump=new HorseJump(); }
 public static class Safety { public boolean enableTailwind=true, enableLeaping=true, enableArmorUtilities=true, enableNautilus=true; public float maximumSpeedBonus=3f, maximumJumpBonus=1.5f, movementDistancePerTick=4f; public int movementPacketsPerTick=20; }
 /** R1 horseshoes; every value is server-owned and hard-capped in {@link #normalize()}. */
 public static class HorseshoeSettings { public boolean enableHorseshoes=true; public float terrainBonus=.5f, handlingBonus=.3f, fullSetSpeedBonus=.05f, fullSetJumpBonus=.05f, fullSetFallDamageIncrease=.1f; public int blocksPerDurability=40; }
 public static class SwiftRide { public float nautilusSpeedMultiplier=.2f, ghastSpeedMultiplier=.85f, horseSpeedMultiplier=.3f, otherSpeedMultiplier=.2f; }
 public static class HorseJump { public float nautilusDashMultiplier=.2f, featherFallingReduction=.12f; public int armorEnchantability=15; public float jumpStrengthMultiplier=.2f; }
 public static final float MAX_SPEED_PER_LEVEL=1f, MAX_JUMP_PER_LEVEL=.5f;
 public static final float MAX_HANDLING=.5f, MAX_FULL_SET=.1f, MAX_FALL_INCREASE=.25f;
 public static final int MIN_BLOCKS_PER_DURABILITY=8, MAX_BLOCKS_PER_DURABILITY=400;
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
  if(horseshoes==null) horseshoes=new HorseshoeSettings();
  var h=horseshoes; h.terrainBonus=bounded(h.terrainBonus,.5f,1); h.handlingBonus=bounded(h.handlingBonus,.3f,MAX_HANDLING);
  h.fullSetSpeedBonus=bounded(h.fullSetSpeedBonus,.05f,MAX_FULL_SET); h.fullSetJumpBonus=bounded(h.fullSetJumpBonus,.05f,MAX_FULL_SET);
  h.fullSetFallDamageIncrease=bounded(h.fullSetFallDamageIncrease,.1f,MAX_FALL_INCREASE);
  h.blocksPerDurability=Math.max(MIN_BLOCKS_PER_DURABILITY,Math.min(MAX_BLOCKS_PER_DURABILITY,h.blocksPerDurability));
 }
 public static RidingConfig load(Path directory) {
  var gson=new GsonBuilder().setPrettyPrinting().create(); var path=directory.resolve("simpleriding.json");
  RidingConfig c=com.simplebuilding.framework.api.ConfigFiles.readOrQuarantine(path,t->gson.fromJson(t,RidingConfig.class),org.slf4j.LoggerFactory.getLogger("SimpleRiding")::warn);
  if(c==null)c=new RidingConfig();
  try { c.normalize(); if(!Files.exists(path)){ Files.createDirectories(directory); Files.writeString(path,gson.toJson(c)); } return c;
  } catch(java.io.IOException e) { throw new IllegalStateException("Cannot write Simple Riding server config "+path,e); }
 }
}

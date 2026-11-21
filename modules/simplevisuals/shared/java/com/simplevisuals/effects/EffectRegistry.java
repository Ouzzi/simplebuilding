package com.simplevisuals.effects;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import com.google.gson.*;
public final class EffectRegistry {
 public record Effect(String id,String category,String particle,int interval) {}
 public static final List<Effect> ALL = load();
 private static List<Effect> load(){
  try(var stream=EffectRegistry.class.getResourceAsStream("/assets/simplevisuals/effects.json")){
   if(stream==null)throw new IllegalStateException("Effect registry missing");
   var result=List.of(new Gson().fromJson(new InputStreamReader(stream,StandardCharsets.UTF_8),Effect[].class));
   var ids=new HashSet<String>();for(var e:result)if(!ids.add(e.id())||e.interval()<1||!e.particle().startsWith("minecraft:"))throw new IllegalStateException("Invalid effect registry");
   return result;
  }catch(IOException e){throw new IllegalStateException(e);}
 }
 public static Intensity level(com.simplevisuals.config.SimplevisualsConfig c,String id){
  return c.particles.overrides.getOrDefault(id,c.particles.globalLevel);
 }
 /** N136: 26.3 split grass, podzol and mycelium out of the dirt tag into grass_blocks; the fireflies stand on both (by id, so older versions without the tag still compile). */
 public static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> GRASS_BLOCKS=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,net.minecraft.resources.Identifier.withDefaultNamespace("grass_blocks"));
 public static boolean firefliesGround(net.minecraft.world.level.block.state.BlockState state){return state.is(net.minecraft.tags.BlockTags.DIRT)||state.is(GRASS_BLOCKS);}
 private EffectRegistry(){}
}

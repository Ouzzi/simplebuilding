package com.simplebuilding.modules.simplesounds;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import java.util.Set;
/**
 * N136: when each of the twelve sounds is due. Shared by the client tick ({@link SoundClient}) and the gametests: the probe client
 * has no audio device, so this is where the sound behaviour is tested. Server-safe (no client classes).
 */
public final class SoundTriggers {
 /** Every effect id of effects.json has a condition here (gametest soundTriggers compares both lists). */
 public static final Set<String> IDS=Set.of("footstep_dust","cold_breath","fireflies","pollen","fire_sparks","water_ripples","water_droplets","leaf_fall","enchanted_items","beacon_aura","damage_feedback","healing_feedback");
 private SoundTriggers(){}
 /** @param day time of day 0..23999 of the player's level, -1 without a day and night cycle; damage/healing: health fell/rose this tick. */
 public static boolean active(String id,Player p,Level level,long day,boolean damage,boolean healing){
  if(p.isSpectator())return false;
  BlockPos pos=p.blockPosition();
  return switch(id){
   case "footstep_dust"->p.onGround()&&!p.isInWater()&&p.getDeltaMovement().horizontalDistanceSqr()>.015;
   case "cold_breath"->!p.isUnderWater()&&level.getBiome(pos).value().coldEnoughToSnow(pos,level.getSeaLevel());
   case "fireflies"->day>=13000&&day<=23000&&firefliesGround(level.getBlockState(pos.below()));
   case "pollen"->day>=0&&day<12000&&nearby(level,pos,BlockTags.FLOWERS,null);
   case "fire_sparks"->nearby(level,pos,null,Blocks.FIRE)||nearby(level,pos,null,Blocks.LAVA)||nearby(level,pos,null,Blocks.CAMPFIRE);
   case "water_ripples"->p.isInWater()&&!p.isUnderWater()&&p.getDeltaMovement().horizontalDistanceSqr()>.005;
   case "water_droplets"->p.isInWater()&&p.getDeltaMovement().lengthSqr()>.01;
   case "leaf_fall"->nearby(level,pos.above(2),BlockTags.LEAVES,null);
   case "enchanted_items"->p.getMainHandItem().isEnchanted()||p.getOffhandItem().isEnchanted();
   case "beacon_aura"->nearby(level,pos,null,Blocks.BEACON);
   case "damage_feedback"->damage;
   case "healing_feedback"->healing;
   default->false;
  };
 }
 /** 26.3 split grass, podzol and mycelium out of the dirt tag into grass_blocks (by id: older versions compile too). */
 private static final TagKey<Block> GRASS_BLOCKS=TagKey.create(net.minecraft.core.registries.Registries.BLOCK,net.minecraft.resources.Identifier.withDefaultNamespace("grass_blocks"));
 public static boolean firefliesGround(net.minecraft.world.level.block.state.BlockState state){return state.is(BlockTags.DIRT)||state.is(GRASS_BLOCKS);}
 private static boolean nearby(Level level,BlockPos pos,TagKey<Block> tag,Block block){
  for(var d:net.minecraft.core.Direction.values()){var sample=pos.relative(d);if(!level.hasChunkAt(sample))continue;var state=level.getBlockState(sample);if(tag!=null?state.is(tag):state.is(block))return true;}return false;
 }
}

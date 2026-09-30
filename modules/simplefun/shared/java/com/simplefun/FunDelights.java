package com.simplefun;
import com.simplefun.heads.*;import com.simplefun.config.SimplefunConfig;import net.minecraft.server.level.*;import net.minecraft.core.particles.*;import net.minecraft.sounds.*;import net.minecraft.world.item.*;import net.minecraft.world.entity.EquipmentSlot;
/** Only cosmetic server effects: no item rewards, buffs, entities or chunk access. */
public final class FunDelights {
 private static final java.util.Map<ServerPlayer,Long> NEXT=new java.util.WeakHashMap<>();
 public static String food(ItemStack s){if(s.is(Items.COOKIE))return "cookieCrumbs";if(s.is(Items.APPLE))return "appleSparkle";if(s.is(Items.CARROT))return "carrotCrunch";if(s.is(Items.MELON_SLICE))return "melonSplash";if(s.is(Items.HONEY_BOTTLE))return "honeyBubbles";if(s.is(Items.BREAD))return "breadCrumbs";if(s.is(Items.SWEET_BERRIES)||s.is(Items.GLOW_BERRIES))return "berryBlush";return null;}
 public static boolean enabled(String key){if(key==null)return false;try{return SimplefunCommon.getConfig().fun.getClass().getField(key).getBoolean(SimplefunCommon.getConfig().fun);}catch(Exception e){return false;}}
 public static boolean emit(ServerPlayer p,String key,SoundEvent sound){if(!enabled(key)||p.isSpectator()||!p.isAlive())return false;long now=p.level().getGameTime();if(now<NEXT.getOrDefault(p,Long.MIN_VALUE))return false;NEXT.put(p,now+SimplefunConfig.COSMETIC_COOLDOWN);var l=(ServerLevel)p.level();l.sendParticles(ParticleTypes.HAPPY_VILLAGER,p.getX(),p.getEyeY(),p.getZ(),SimplefunConfig.MAX_PARTICLES,.15,.1,.15,.01);l.playSound(null,p.blockPosition(),sound,SoundSource.PLAYERS,.2f,1);return true;}
 public static void food(ServerPlayer p,ItemStack s){emit(p,food(s),SoundEvents.GENERIC_EAT.value());}
 public static void tick(ServerPlayer p){if(!p.isShiftKeyDown())return;var head=p.getItemBySlot(EquipmentSlot.HEAD);for(var t:AnimalHead.values())if(head.is(AnimalHeads.ITEMS.get(t))){emit(p,t.name().toLowerCase(java.util.Locale.ROOT)+"HeadGreeting",t.sound);return;}if(net.minecraft.world.level.block.Block.byItem(p.getMainHandItem().getItem()) instanceof net.minecraft.world.level.block.FlowerBlock)emit(p,"flowerSniff",SoundEvents.FOX_SNIFF);}
}

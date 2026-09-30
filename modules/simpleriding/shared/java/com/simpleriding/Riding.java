package com.simpleriding;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.tags.TagKey;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
public final class Riding {
 public static RidingConfig CONFIG=new RidingConfig();
 public static Identifier id(String p){return Identifier.fromNamespaceAndPath("simpleriding",p);}
 public static final ResourceKey<Enchantment> TAILWIND=ResourceKey.create(Registries.ENCHANTMENT,id("tailwind")), LEAPING=ResourceKey.create(Registries.ENCHANTMENT,id("leaping"));
 public static final TagKey<Item> ARMOR=TagKey.create(Registries.ITEM,id("horse_armor_enchantable")), SADDLE=TagKey.create(Registries.ITEM,id("saddle_enchantable"));
 public static final DataComponentType<BlockPos> COORDINATES=DataComponentType.<BlockPos>builder().persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC).build();
 public static final CreativeModeTab TAB=CreativeModeTab.builder(CreativeModeTab.Row.TOP,0).title(Component.translatable("itemgroup.simpleriding.riding_items")).icon(()->new ItemStack(Items.SADDLE)).displayItems((params,out)->{
  var lookup=params.holders().lookupOrThrow(Registries.ENCHANTMENT);
  for(var key:java.util.List.of(TAILWIND,LEAPING))out.accept(EnchantmentHelper.createBook(new EnchantmentInstance(lookup.getOrThrow(key),3)));
 }).build();
 public static void components(){Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,id("coordinates"),COORDINATES);}
 public static void tab(){Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,id("riding_items"),TAB);}
 public static boolean allowed(Enchantment e) {
  if(!(e.description().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t))return false;
  return java.util.Set.of("enchantment.minecraft.protection","enchantment.minecraft.fire_protection","enchantment.minecraft.blast_protection","enchantment.minecraft.projectile_protection","enchantment.minecraft.feather_falling","enchantment.simpleriding.leaping").contains(t.getKey());
 }
}

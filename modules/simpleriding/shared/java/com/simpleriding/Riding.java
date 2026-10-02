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
 public static final TagKey<Item> NAUTILUS_ARMOR=TagKey.create(Registries.ITEM,id("nautilus_armor_enchantable"));
 public static boolean armor(ItemStack s){return s.is(ARMOR)||s.is(NAUTILUS_ARMOR);}
 public static final DataComponentType<BlockPos> COORDINATES=DataComponentType.<BlockPos>builder().persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC).build();
 public static final CreativeModeTab TAB=CreativeModeTab.builder(CreativeModeTab.Row.TOP,0).title(Component.translatable("itemgroup.simpleriding.riding_items")).icon(()->new ItemStack(Items.SADDLE)).displayItems((params,out)->{
  var lookup=params.holders().lookupOrThrow(Registries.ENCHANTMENT);
  for(var key:java.util.List.of(TAILWIND,LEAPING))out.accept(EnchantmentHelper.createBook(new EnchantmentInstance(lookup.getOrThrow(key),3)));
  if(Horseshoes.TEMPLATE!=null)out.accept(Horseshoes.TEMPLATE);
  for(var item:Horseshoes.ITEMS.values())out.accept(item);
 }).build();
 /** Set by each loader before registration: the Enderite horseshoe only exists alongside SimpleBuilding. */
 public static boolean SIMPLEBUILDING=false;
 public static void items(){Horseshoes.register(SIMPLEBUILDING);}
 public static void components(){Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,id("coordinates"),COORDINATES);}
 public static void tab(){Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,id("riding_items"),TAB);}
 /**
  * Vanilla tabs too (owner 2026-10-02): the horseshoes stand in Combat right before the wolf armor, i.e. after every
  * horse armor tier (SimpleBuilding's Enderite one included), and the template in Ingredients right before the bottle
  * o' enchanting, after every smithing template. "Before" anchors keep the order independent of the mod load order.
  * Each loader inserts {@link #vanillaTabStacks} before {@link #vanillaTabAnchor} of that tab.
  */
 public static Item vanillaTabAnchor(ResourceKey<CreativeModeTab> tab){
  if(tab.equals(CreativeModeTabs.COMBAT))return Items.WOLF_ARMOR;
  if(tab.equals(CreativeModeTabs.INGREDIENTS))return Items.EXPERIENCE_BOTTLE;
  return null;
 }
 public static java.util.List<ItemStack> vanillaTabStacks(ResourceKey<CreativeModeTab> tab){
  if(tab.equals(CreativeModeTabs.COMBAT))return Horseshoes.ITEMS.values().stream().map(ItemStack::new).toList();
  if(tab.equals(CreativeModeTabs.INGREDIENTS)&&Horseshoes.TEMPLATE!=null)return java.util.List.of(new ItemStack(Horseshoes.TEMPLATE));
  return java.util.List.of();
 }
 public static boolean allowed(Enchantment e) {
  if(!(e.description().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t))return false;
  if(!CONFIG.safety.enableArmorUtilities)return t.getKey().equals("enchantment.simpleriding.leaping");
  return java.util.Set.of("enchantment.minecraft.protection","enchantment.minecraft.fire_protection","enchantment.minecraft.blast_protection","enchantment.minecraft.projectile_protection","enchantment.minecraft.feather_falling","enchantment.simpleriding.leaping").contains(t.getKey());
 }
}

package com.simpleriding;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.entries.*;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
public final class RidingLoot {
 public static final int BASTION_MAX_ROLLS=2, BASTION_EMPTY_WEIGHT=30;
 public static final int FORTRESS_MAX_ROLLS=1, FORTRESS_EMPTY_WEIGHT=20;
 public static final int TRIAL_MAX_ROLLS=2, TRIAL_EMPTY_WEIGHT=60;
 public static final String[] TABLES={"chests/bastion_treasure","chests/bastion_other","chests/nether_bridge","chests/trial_chambers/reward_common","chests/trial_chambers/reward_rare"};
 public static final int[][] BASTION={{0,2,20},{0,3,10},{1,2,10},{1,3,5}}, FORTRESS={{0,2,5},{0,3,10}}, TRIAL={{1,2,10},{1,3,3},{0,2,10},{0,3,3},{2,4,10}};
 public static void apply(ResourceKey<LootTable> key,Consumer<LootPool.Builder> add,HolderGetter.Provider registries){
  if(!Riding.CONFIG.worldGen.enableLootTableChanges)return;
  String name=key.identifier().toString(); int[][] entries; int empty,rolls;
  for(String village:VILLAGE_TABLES)if(name.equals("minecraft:"+village)){add.accept(villagePool());return;}
  if(name.equals("minecraft:"+TABLES[0])||name.equals("minecraft:"+TABLES[1])){entries=BASTION;empty=BASTION_EMPTY_WEIGHT;rolls=BASTION_MAX_ROLLS;}
  else if(name.equals("minecraft:"+TABLES[2])){entries=FORTRESS;empty=FORTRESS_EMPTY_WEIGHT;rolls=FORTRESS_MAX_ROLLS;}
  else if(name.equals("minecraft:"+TABLES[3])||name.equals("minecraft:"+TABLES[4])){entries=TRIAL;empty=TRIAL_EMPTY_WEIGHT;rolls=TRIAL_MAX_ROLLS;}
  else return;
  var ench=registries.lookupOrThrow(Registries.ENCHANTMENT); var pool=LootPool.lootPool().setRolls(ContextIntProviders.between(0,rolls));
  for(var e:entries){var keyE=e[0]==0?Riding.TAILWIND:e[0]==1?Riding.LEAPING:Enchantments.PROTECTION;
   var builder=new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);builder.upgrade(ench.getOrThrow(keyE),e[1]);
   pool.add(LootItem.lootTableItem(Items.ENCHANTED_BOOK).setWeight(e[2]).apply(SetComponentsFunction.setComponent(DataComponents.STORED_ENCHANTMENTS,builder.toImmutable())));
  }
  add.accept(pool.add(EmptyLootItem.emptyItem().setWeight(empty)));
 }

 // ---- R1 horseshoes: village stable/smithy chests and trail-ruins archaeology ----
 /** Weaponsmith (saddles, horse armor) and tannery (saddles) are the village "stable" chests. */
 public static final String[] VILLAGE_TABLES={"chests/village/village_weaponsmith","chests/village/village_tannery"};
 /** One roll: template 3, copper and iron horseshoe 1 each (worn), empty 15 -> 15 % / 5 % / 5 %. */
 public static final int VILLAGE_TEMPLATE_WEIGHT=3, VILLAGE_SHOE_WEIGHT=1, VILLAGE_EMPTY_WEIGHT=15;
 public static final String TRAIL_RARE="archaeology/trail_ruins_rare", TRAIL_COMMON="archaeology/trail_ruins_common";
 /** Trail ruins: like one more weight-1 entry in the rare table (12 entries) and in the common table (weight 45). */
 public static final int TRAIL_RARE_ONE_IN=13, TRAIL_COMMON_ONE_IN=46;
 public static final float WORN_MIN=.15f, WORN_MAX=.8f;
 private static LootPool.Builder villagePool(){
  var pool=LootPool.lootPool().setRolls(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.exactly(1));
  pool.add(LootItem.lootTableItem(Horseshoes.TEMPLATE).setWeight(VILLAGE_TEMPLATE_WEIGHT));
  for(var t:java.util.List.of(Horseshoes.Tier.COPPER,Horseshoes.Tier.IRON))pool.add(LootItem.lootTableItem(Horseshoes.ITEMS.get(t)).setWeight(VILLAGE_SHOE_WEIGHT)
   .apply(net.minecraft.world.level.storage.loot.functions.SetItemDamageFunction.setDamage(net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders.between(WORN_MIN,WORN_MAX))));
  return pool.add(EmptyLootItem.emptyItem().setWeight(VILLAGE_EMPTY_WEIGHT));
 }
 /** Replaces a brushed find (deterministic per block seed); returns the original when not selected. */
 public static ItemStack archaeology(ResourceKey<LootTable> table,long seed,ItemStack found,HolderLookup.Provider registries){
  if(!Riding.CONFIG.worldGen.enableLootTableChanges||table==null||Horseshoes.TEMPLATE==null)return found;
  String name=table.identifier().toString();
  var random=net.minecraft.util.RandomSource.create(seed^0x5EED_4005E5L);
  if(name.equals("minecraft:"+TRAIL_RARE))return random.nextInt(TRAIL_RARE_ONE_IN)==0?new ItemStack(Horseshoes.TEMPLATE):found;
  if(name.equals("minecraft:"+TRAIL_COMMON)&&random.nextInt(TRAIL_COMMON_ONE_IN)==0){
   var shoe=new ItemStack(Horseshoes.ITEMS.get(random.nextBoolean()?Horseshoes.Tier.COPPER:Horseshoes.Tier.IRON));
   shoe.setDamageValue((int)(shoe.getMaxDamage()*(1-(WORN_MIN+random.nextFloat()*(WORN_MAX-WORN_MIN)))));
   return shoe;
  }
  return found;
 }
}

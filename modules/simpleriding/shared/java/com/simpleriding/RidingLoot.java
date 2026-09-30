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
}

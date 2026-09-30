package com.simplemoney;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import java.util.*;
import java.util.function.Consumer;
public final class MoneyLoot {
 public record Entry(String table,float chance,int min,int max) {}
 public static final List<Entry> ENTRIES=List.of(new Entry("igloo_chest",.30f,1,1),new Entry("simple_dungeon",.20f,2,4),new Entry("end_city_treasure",.20f,1,6),new Entry("abandoned_mineshaft",.35f,1,2),new Entry("shipwreck_treasure",.30f,1,2),new Entry("stronghold_library",.25f,8,16),new Entry("buried_treasure",.30f,1,4));
 public static void inject(String id,Consumer<LootPool.Builder> add) {
  for(var e:ENTRIES) if(id.equals("minecraft:chests/"+e.table())) add.accept(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1)).when(LootItemRandomChanceCondition.randomChance(e.chance())).add(LootItem.lootTableItem(MoneyItems.ITEMS.get("money_bill"))).apply(SetItemCountFunction.setCount(ContextIntProviders.between(e.min(),e.max()))));
 }
}

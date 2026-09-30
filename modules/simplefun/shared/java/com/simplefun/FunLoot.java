package com.simplefun;
import com.simplefun.heads.*;
import net.minecraft.core.*;import net.minecraft.core.registries.*;import net.minecraft.resources.*;import net.minecraft.advancements.predicates.entity.*;import net.minecraft.world.level.storage.loot.*;import net.minecraft.world.level.storage.loot.entries.*;import net.minecraft.world.level.storage.loot.predicates.*;import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
public final class FunLoot {
 public static void apply(ResourceKey<LootTable> key,java.util.function.Consumer<LootPool.Builder> add,HolderGetter.Provider registry){
  if(!key.equals(BuiltInLootTables.CHARGED_CREEPER)||!SimplefunCommon.getConfig().fun.enableAnimalHeads)return;
  for(var t:AnimalHead.values())add.accept(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1)).add(LootItem.lootTableItem(AnimalHeads.ITEMS.get(t))).when(LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS,EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(registry.lookupOrThrow(Registries.ENTITY_TYPE),t.source)))));
 }
}

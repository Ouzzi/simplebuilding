package com.simpleriding.mixin;

import com.simpleriding.RidingLoot;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Archaeology yields exactly one item, so trail-ruins finds are replaced instead of adding a pool. */
@Mixin(BrushableBlockEntity.class)
public abstract class HorseshoeBrushMixin {
 @Shadow private ItemStack item;
 @Shadow private ResourceKey<LootTable> lootTable;
 @Shadow private long lootTableSeed;
 @Unique private ResourceKey<LootTable> simpleriding$table;
 @Unique private long simpleriding$seed;
 @Inject(method="unpackLootTable",at=@At("HEAD"))
 private void simpleriding$remember(ServerLevel level,LivingEntity user,ItemInstance brush,CallbackInfo ci){simpleriding$table=lootTable;simpleriding$seed=lootTableSeed;}
 @Inject(method="unpackLootTable",at=@At("TAIL"))
 private void simpleriding$replace(ServerLevel level,LivingEntity user,ItemInstance brush,CallbackInfo ci){
  if(simpleriding$table!=null&&lootTable==null)item=RidingLoot.archaeology(simpleriding$table,simpleriding$seed,item,level.registryAccess());
  simpleriding$table=null;
 }
}

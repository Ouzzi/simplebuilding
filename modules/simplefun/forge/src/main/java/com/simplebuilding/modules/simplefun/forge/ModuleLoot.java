package com.simplebuilding.modules.simplefun.forge;
public final class ModuleLoot {
 public static void inject(net.minecraft.core.HolderLookup.Provider context,net.minecraft.core.RegistryAccess.Frozen loaded){
  var lookup=net.minecraft.core.HolderLookup.Provider.create(java.util.stream.Stream.concat(context.listRegistries(),loaded.listRegistries()));
  loaded.lookupOrThrow(net.minecraft.core.registries.Registries.LOOT_TABLE).listElements().forEach(holder->{com.simplefun.FunLoot.apply(holder.key(),pool->holder.value().addPool(pool.build()),lookup);});
 }
}

package com.simplebuilding.modules.simpletweaks.forge;
import com.simplebuilding.modules.simpletweaks.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
@net.minecraftforge.fml.common.Mod("simpletweaks")
public final class TweaksForge {
 public TweaksForge(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext context){
  ModuleForgeTests.register(context.getModBusGroup());
  net.minecraftforge.registries.RegisterEvent.getBus(context.getModBusGroup()).addListener(event->{
   if(event.getRegistryKey().equals(net.minecraft.core.registries.Registries.ITEM))LegacyDeed.register();
   if(!(event.getForgeRegistry() instanceof net.minecraftforge.registries.ForgeRegistry<?> registry))return;
   for(var vanilla:java.util.List.of(BuiltInRegistries.ITEM,BuiltInRegistries.BLOCK,BuiltInRegistries.BLOCK_ENTITY_TYPE,BuiltInRegistries.DATA_COMPONENT_TYPE)){
    if(!vanilla.key().equals(event.getRegistryKey()))continue;
    var paths=new java.util.HashSet<>(LegacyAliases.BLOCKS);paths.addAll(LegacyAliases.BLOCK_ENTITIES);paths.addAll(LegacyAliases.COMPONENTS);paths.add("laser_pointer");paths.add("spawn_elytra");
    for(String path:paths){var old=Identifier.fromNamespaceAndPath("simpletweaks",path);var target=LegacyAliases.target(vanilla,old);if(target!=null)registry.addAlias(old,target);}
   }
  });
 }
}

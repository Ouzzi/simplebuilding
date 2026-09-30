package com.simplebuilding.modules.simpledimensions;
import net.neoforged.fml.common.Mod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
@Mod("simpledimension")
public final class NeoForgeExample {
 public NeoForgeExample(net.neoforged.bus.api.IEventBus bus) {
  ModuleNeoTests.register(bus);
  bus.addListener((net.neoforged.neoforge.registries.RegisterEvent e)-> {
   if(e.getRegistryKey().equals(Registries.BLOCK)) DimensionRegistry.blocks();
   if(e.getRegistryKey().equals(Registries.ITEM)) DimensionRegistry.items();
   if(e.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) DimensionRegistry.PORTAL_ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,DimensionRegistry.id("sky_portal"),new BlockEntityType<>(SkyPortalBlockEntity::new,java.util.Set.of(DimensionRegistry.PORTAL,DimensionRegistry.LEGACY)));
  });
 }
}

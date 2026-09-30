package com.simplebuilding.modules.simpledimensions;
import net.neoforged.fml.common.Mod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
@Mod("simpledimension")
public final class NeoForgeExample {
 public NeoForgeExample(net.neoforged.bus.api.IEventBus bus) {
  ModuleNeoTests.register(bus);
  var events=net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
  events.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post e)->DimensionRuntime.get(e.getServer()).tick());
  events.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent e)->DimensionRuntime.stop(e.getServer()));
  events.addListener((net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock e)->{
   if(e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p){var result=PortalActivation.ignite(p,e.getHand(),e.getHitVec());if(result!=net.minecraft.world.InteractionResult.PASS){e.setCanceled(true);e.setCancellationResult(result);}}
  });
  DimensionRuntime.claimIntegrationRequired=java.util.List.of("flan","ftbchunks","openpartiesandclaims","griefdefender","claimchunk").stream().anyMatch(net.neoforged.fml.ModList.get()::isLoaded);
  bus.addListener((net.neoforged.neoforge.registries.RegisterEvent e)-> {
   if(e.getRegistryKey().equals(Registries.BLOCK)) DimensionRegistry.blocks();
   if(e.getRegistryKey().equals(Registries.ITEM)) DimensionRegistry.items();
   if(e.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) DimensionRegistry.PORTAL_ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,DimensionRegistry.id("sky_portal"),new BlockEntityType<>(SkyPortalBlockEntity::new,java.util.Set.of(DimensionRegistry.PORTAL,DimensionRegistry.LEGACY),true));
   if(e.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) DimensionRegistry.LEGACY_ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,DimensionRegistry.id("light_blue_portal"),new BlockEntityType<>(SkyPortalBlockEntity::new,java.util.Set.of(DimensionRegistry.LEGACY),true));
  });
 }
}

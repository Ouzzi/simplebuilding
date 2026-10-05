package com.simplebuilding.modules.simpledimensions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
public final class FabricExample implements ModInitializer {
 public void onInitialize() {
  net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(s->DimensionRuntime.get(s).tick());
  net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(DimensionRuntime::stop);
  net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((p,l,h,hit)->p instanceof net.minecraft.server.level.ServerPlayer sp?PortalActivation.ignite(sp,h,hit):net.minecraft.world.InteractionResult.PASS);
  DimensionRuntime.claimIntegrationRequired=java.util.List.of("flan","ftbchunks","openpartiesandclaims","griefdefender","claimchunk").stream().anyMatch(net.fabricmc.loader.api.FabricLoader.getInstance()::isModLoaded);
  DimensionRegistry.blocks(); DimensionRegistry.items();
  if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("ftbquests"))com.simplebuilding.modules.simpledimensions.guide.DimensionsGuide.installQuests(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir());
  net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES).register(out->out.accept(com.simplebuilding.modules.simpledimensions.guide.DimensionsGuide.book()));
  DimensionRegistry.PORTAL_ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,DimensionRegistry.id("sky_portal"),FabricBlockEntityTypeBuilder.create(SkyPortalBlockEntity::new,DimensionRegistry.PORTAL,DimensionRegistry.LEGACY).canPotentiallyExecuteCommands(true).build());
  DimensionRegistry.LEGACY_ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,DimensionRegistry.id("light_blue_portal"),FabricBlockEntityTypeBuilder.create(SkyPortalBlockEntity::new,DimensionRegistry.LEGACY).canPotentiallyExecuteCommands(true).build());
 }
}

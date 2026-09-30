package com.simplebuilding.modules.simpledimensions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
public final class FabricExample implements ModInitializer {
 public void onInitialize() {
  DimensionRegistry.blocks(); DimensionRegistry.items();
  DimensionRegistry.PORTAL_ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,DimensionRegistry.id("sky_portal"),FabricBlockEntityTypeBuilder.create(SkyPortalBlockEntity::new,DimensionRegistry.PORTAL,DimensionRegistry.LEGACY).build());
 }
}

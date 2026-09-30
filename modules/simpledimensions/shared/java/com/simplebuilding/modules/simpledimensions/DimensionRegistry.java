package com.simplebuilding.modules.simpledimensions;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.item.*;
public final class DimensionRegistry {
 public static SkyPortalBlock PORTAL, LEGACY;
 public static BlockEntityType<SkyPortalBlockEntity> PORTAL_ENTITY;
 public static Identifier id(String path) { return Identifier.fromNamespaceAndPath("simpledimension", path); }
 public static void blocks() {
  PORTAL=block("sky_portal"); LEGACY=block("light_blue_portal");
 }
 private static SkyPortalBlock block(String path) {
  return Registry.register(BuiltInRegistries.BLOCK,id(path),new SkyPortalBlock(BlockBehaviour.Properties.of()
   .strength(-1,3600000).noCollision().noLootTable().lightLevel(s->11).pushReaction(PushReaction.POPPED)
   .setId(ResourceKey.create(Registries.BLOCK,id(path)))));
 }
 public static void items() {
  Registry.register(BuiltInRegistries.ITEM,id("light_blue_portal"),new BlockItem(LEGACY,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id("light_blue_portal")))));
 }
 public static boolean portal(net.minecraft.world.level.block.state.BlockState state) { return state.is(PORTAL)||state.is(LEGACY); }
}

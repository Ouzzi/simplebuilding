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
 public static BlockEntityType<SkyPortalBlockEntity> PORTAL_ENTITY, LEGACY_ENTITY;
 /** N33 nether portal in arch shape; registered by the Fabric entry only so far (NeoForge/Forge port open), null elsewhere. */
 public static ShapedNetherPortalBlock SHAPED;
 /** Super-mod switch "Enable Simple Dimensions" (framework SubMods): off = blocks stay registered but portals neither ignite nor teleport. */
 public static boolean on(){return com.simplebuilding.framework.api.SubMods.enabled("simpledimensions");}
 public static Identifier id(String path) { return Identifier.fromNamespaceAndPath("simpledimensions", path); }
 public static void blocks() {
  PORTAL=block("sky_portal"); LEGACY=block("light_blue_portal");
 }
 private static SkyPortalBlock block(String path) {
  return Registry.register(BuiltInRegistries.BLOCK,id(path),new SkyPortalBlock(BlockBehaviour.Properties.of()
   .strength(-1,3600000).noCollision().noLootTable().lightLevel(s->11).pushReaction(PushReaction.POPPED)
   .setId(ResourceKey.create(Registries.BLOCK,id(path)))));
 }
 /** Vanilla's nether portal properties (no collision, random ticks for piglins, light 11, glass sound, unbreakable), no drop, no item. */
 public static void shapedPortal() {
  SHAPED=Registry.register(BuiltInRegistries.BLOCK,id("shaped_nether_portal"),new ShapedNetherPortalBlock(BlockBehaviour.Properties.of()
   .noCollision().randomTicks().strength(-1).sound(SoundType.GLASS).lightLevel(s->11).pushReaction(PushReaction.IMMOVEABLE).noLootTable()
   .setId(ResourceKey.create(Registries.BLOCK,id("shaped_nether_portal")))));
 }
 /** Legacy block item only: keeps old stacks loadable. Deliberately in no creative tab (audit 2026-10-02) - no recipe, drop or trade; /give only. */
 public static void items() {
  com.simplebuilding.modules.simpledimensions.guide.DimensionsGuide.register();
  Registry.register(BuiltInRegistries.ITEM,id("light_blue_portal"),new BlockItem(LEGACY,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id("light_blue_portal")))));
 }
 public static boolean portal(net.minecraft.world.level.block.state.BlockState state) { return state.is(PORTAL)||state.is(LEGACY); }
}

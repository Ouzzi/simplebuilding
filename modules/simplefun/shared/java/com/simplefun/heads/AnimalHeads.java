package com.simplefun.heads;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

public final class AnimalHeads {
  public static final Map<AnimalHead, Block> STANDING = new EnumMap<>(AnimalHead.class),
      WALL = new EnumMap<>(AnimalHead.class);
  public static final Map<AnimalHead, Item> ITEMS = new EnumMap<>(AnimalHead.class);

  public static Identifier id(String s) {
    return Identifier.fromNamespaceAndPath("simplefun", s);
  }

  public static void blocks() {
    for (var t : AnimalHead.values()) {
      var key = ResourceKey.create(Registries.BLOCK, id(t.path()));
      var b =
          Registry.register(
              BuiltInRegistries.BLOCK,
              key,
              new SkullBlock(
                  t,
                  BlockBehaviour.Properties.of()
                      .setId(key)
                      .strength(1)
                      .noOcclusion()
                      .instrument(NoteBlockInstrument.CUSTOM_HEAD)));
      STANDING.put(t, b);
      var wk = ResourceKey.create(Registries.BLOCK, id(t.wall()));
      WALL.put(
          t,
          Registry.register(
              BuiltInRegistries.BLOCK,
              wk,
              new WallSkullBlock(
                  t,
                  BlockBehaviour.Properties.of()
                      .setId(wk)
                      .strength(1)
                      .noOcclusion()
                      .overrideLootTable(b.getLootTable())
                      .overrideDescription(b.getDescriptionId()))));
    }
  }

  public static void items() {
    for (var t : AnimalHead.values()) {
      var k = ResourceKey.create(Registries.ITEM, id(t.path()));
      ITEMS.put(
          t,
          Registry.register(
              BuiltInRegistries.ITEM,
              k,
              new StandingAndWallBlockItem(
                  STANDING.get(t),
                  WALL.get(t),
                  Direction.DOWN,
                  new Item.Properties()
                      .setId(k)
                      .useBlockDescriptionPrefix()
                      .equippableUnswappable(EquipmentSlot.HEAD)
                      .component(DataComponents.NOTE_BLOCK_SOUND, t.sound.location()))));
    }
  }

  /**
   * Vanilla neighbours: every loader also puts the brick snowball right after the snowball in the
   * vanilla Combat tab and the heads right after the dragon head (the last vanilla head) in
   * Functional Blocks, so the search tab lists them beside their vanilla models instead of at its
   * end. SimpleBuilding's own mob heads follow the piglin head, so the two never compete.
   */
  public static final Item SNOWBALL_ANCHOR = Items.SNOWBALL, HEAD_ANCHOR = Items.DRAGON_HEAD;

  public static List<ItemStack> headStacks() {
    var out = new ArrayList<ItemStack>();
    for (var t : AnimalHead.values()) out.add(new ItemStack(ITEMS.get(t)));
    return List.copyOf(out);
  }

  /**
   * Only the mod's own items: the brick snowball, then the heads. The vanilla ingredients (bricks,
   * snowball, feather, ...) stay in their vanilla tabs.
   */
  public static List<ItemStack> tabStacks() {
    var out = new ArrayList<ItemStack>();
    out.add(new ItemStack(com.simplefun.registry.ModItems.BRICK_SNOWBALL));
    out.addAll(headStacks());
    return List.copyOf(out);
  }

  public static void tab() {
    Registry.register(
        BuiltInRegistries.CREATIVE_MODE_TAB,
        id("fun"),
        CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
            .title(net.minecraft.network.chat.Component.translatable("itemgroup.simplefun.fun"))
            .icon(() -> new ItemStack(com.simplefun.registry.ModItems.BRICK_SNOWBALL))
            .displayItems((p, out) -> tabStacks().forEach(out::accept))
            .build());
  }
}

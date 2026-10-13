package com.simplefun.registry;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Simple Fun's tab and id helper. The animal heads left for Simple Mobheads with version 1.4.0
 * (KONZEPT-SIMPLE-MOBHEADS-2026-10-13); Fun keeps the brick snowball, the piggy effect and its guide.
 */
public final class FunContent {
  /** The brick snowball follows the snowball in the vanilla Combat tab, so the search tab lists it there too. */
  public static final Item SNOWBALL_ANCHOR = Items.SNOWBALL;

  private FunContent() {}

  public static Identifier id(String s) {
    return Identifier.fromNamespaceAndPath("simplefun", s);
  }

  /** Item registration step after the brick snowball: the guide book. */
  public static void items() {
    com.simplefun.guide.FunGuide.register();
  }

  /** Only the mod's own items: the guide, then the brick snowball. */
  public static List<ItemStack> tabStacks() {
    var out = new ArrayList<ItemStack>();
    if (com.simplefun.guide.FunGuide.book() != null) out.add(com.simplefun.guide.FunGuide.stack());
    out.add(new ItemStack(ModItems.BRICK_SNOWBALL));
    return List.copyOf(out);
  }

  public static void tab() {
    Registry.register(
        BuiltInRegistries.CREATIVE_MODE_TAB,
        id("fun"),
        CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
            .title(net.minecraft.network.chat.Component.translatable("itemgroup.simplefun.fun"))
            .icon(() -> new ItemStack(ModItems.BRICK_SNOWBALL))
            .displayItems(
                (p, out) -> {
                  if (!com.simplefun.SimplefunCommon.on()) return;
                  tabStacks().forEach(out::accept);
                })
            .build());
  }
}

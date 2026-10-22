package com.simplemaps;

import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.dimension.DimensionType;

/** The three wayfinder maps (owner F8: one map per dimension) and the module's tags. */
public final class MapsItems {
    /** Dimension types the Nether / End map accepts; the plain map accepts every type in neither tag (F8, datapack-extensible). */
    public static final TagKey<DimensionType> NETHER_DIMENSIONS = TagKey.create(Registries.DIMENSION_TYPE, SimpleMaps.id("nether_wayfinder"));
    public static final TagKey<DimensionType> END_DIMENSIONS = TagKey.create(Registries.DIMENSION_TYPE, SimpleMaps.id("end_wayfinder"));
    /** Mob heads a waypoint may use as icon (owner F10: every mob head). */
    public static final TagKey<Item> WAYPOINT_HEADS = TagKey.create(Registries.ITEM, SimpleMaps.id("waypoint_heads"));
    public static final TagKey<Item> WAYFINDER_MAPS = TagKey.create(Registries.ITEM, SimpleMaps.id("wayfinder_maps"));

    public static Item WAYFINDER_MAP, NETHER_WAYFINDER_MAP, END_WAYFINDER_MAP;

    /** Owner 21.10.: a wayfinder map does not stack (each one is a single map with its own waypoints and view). */
    public static final int STACK_SIZE = 1;

    public static void register() {
        WAYFINDER_MAP = register("wayfinder_map", p -> new WayfinderMapItem(WayfinderMapItem.Kind.OVERWORLD, p.rarity(Rarity.UNCOMMON).stacksTo(STACK_SIZE)));
        NETHER_WAYFINDER_MAP = register("nether_wayfinder_map", p -> new WayfinderMapItem(WayfinderMapItem.Kind.NETHER, p.rarity(Rarity.RARE).stacksTo(STACK_SIZE)));
        END_WAYFINDER_MAP = register("end_wayfinder_map", p -> new WayfinderMapItem(WayfinderMapItem.Kind.END, p.rarity(Rarity.EPIC).stacksTo(STACK_SIZE)));
    }

    public static List<ItemStack> tabStacks() {
        if (!MapsConfig.enabled) return List.of();
        // The guide book joins only the own tab (registerTab); the Vanilla tab gets it from the loader hook.
        return List.of(new ItemStack(WAYFINDER_MAP), new ItemStack(NETHER_WAYFINDER_MAP), new ItemStack(END_WAYFINDER_MAP));
    }

    public static boolean isWayfinder(ItemStack stack) {
        return MapsConfig.enabled && stack.getItem() instanceof WayfinderMapItem;
    }

    private static Item register(String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleMaps.id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
    }

    private MapsItems() {}

    /** The module's own creative tab (items are also sorted into the Vanilla tabs, see the loader hooks). */
    public static void registerTab() {
        net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB,
                net.minecraft.resources.Identifier.fromNamespaceAndPath("simplemaps", "maps"),
                net.minecraft.world.item.CreativeModeTab.builder(net.minecraft.world.item.CreativeModeTab.Row.TOP, 0)
                        .title(net.minecraft.network.chat.Component.translatable("itemgroup.simplemaps.maps"))
                        .icon(() -> new net.minecraft.world.item.ItemStack(WAYFINDER_MAP))
                        .displayItems((params, out) -> {
                            List<ItemStack> stacks = tabStacks();
                            stacks.forEach(out::accept);
                            // The guide belongs in the own tab too, not only in Tools & Utilities.
                            net.minecraft.world.item.Item guide = com.simplebuilding.modules.simplemaps.guide.MapsGuide.book();
                            if (guide != null && stacks.stream().noneMatch(s -> s.is(guide))) out.accept(new ItemStack(guide));
                        }).build());
    }
}

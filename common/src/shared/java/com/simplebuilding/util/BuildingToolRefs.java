package com.simplebuilding.util;

import com.simplebuilding.version.McVersion;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jspecify.annotations.Nullable;

/**
 * The building tools by id (b-btools07, like {@link HammerRefs}): on 26.3 the Building Wands, Octants and Blueprints belong to the
 * required, bundled sub-mod Simple Building Tools ({@code simplebuildingtools:*}) and SimpleBuilding registers none of them
 * ({@link McVersion#BTOOLS_IN_SB}); tabs, loot, the test centre, datagen and the tests find them in the registry. On 26.2 they are
 * SimpleBuilding's own items under {@code simplebuilding:*}, so the same calls work on both lines. SimpleBuilding imports no class of
 * the module. Call after registration; an unregistered id gives air.
 */
public final class BuildingToolRefs {
    /** Wand tiers in tab order; the ids are {@code <tier>_building_wand}. */
    public static final List<String> WAND_TIERS = List.of("copper", "iron", "gold", "diamond", "netherite", "enderite");

    private BuildingToolRefs() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(McVersion.BTOOLS_NAMESPACE, path);
    }

    public static Item item(String path) {
        return BuiltInRegistries.ITEM.getValue(id(path));
    }

    /** The wand of {@code tier} ({@code "iron"}, ..., {@code "creative"}). */
    public static Item wand(String tier) {
        return item(tier + "_building_wand");
    }

    /** The six crafted wands, copper to Enderite. */
    public static List<Item> wands() {
        List<Item> out = new ArrayList<>();
        for (String tier : WAND_TIERS) out.add(wand(tier));
        return out;
    }

    /** The plain octant. */
    public static Item octant() {
        return item("octant");
    }

    /** The dyed octant of {@code color}. */
    public static Item octant(DyeColor color) {
        return item("octant_" + color.getName());
    }

    /** The sixteen dyed octants in dye order. */
    public static List<Item> dyedOctants() {
        List<Item> out = new ArrayList<>();
        for (DyeColor color : DyeColor.values()) out.add(octant(color));
        return out;
    }

    public static Item blueprint() {
        return item("blueprint");
    }

    public static Item creativeBlueprint() {
        return item("creative_blueprint");
    }

    /** A wand enchantment that moved with the wands ({@code "cover"}, {@code "bridge"}, {@code "linear"}). */
    public static ResourceKey<Enchantment> enchantment(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT, id(path));
    }

    private static @Nullable String toolPath(Item item) {
        if (item == null || item == Items.AIR) return null;
        Identifier key = BuiltInRegistries.ITEM.getKey(item);
        return McVersion.BTOOLS_NAMESPACE.equals(key.getNamespace()) ? key.getPath() : null;
    }

    public static boolean isWand(Item item) {
        String path = toolPath(item);
        return path != null && path.endsWith("_building_wand");
    }

    public static boolean isOctant(Item item) {
        String path = toolPath(item);
        return path != null && (path.equals("octant") || path.startsWith("octant_"));
    }

    public static boolean isOctant(ItemStack stack) {
        return !stack.isEmpty() && isOctant(stack.getItem());
    }

    public static boolean isBlueprint(Item item) {
        String path = toolPath(item);
        return path != null && (path.equals("blueprint") || path.equals("creative_blueprint"));
    }

    public static boolean isBlueprint(ItemStack stack) {
        return !stack.isEmpty() && isBlueprint(stack.getItem());
    }

    /** The dye of an octant ({@code null} for the plain one and for anything else). */
    public static @Nullable DyeColor octantColor(Item item) {
        String path = toolPath(item);
        if (path == null || !path.startsWith("octant_")) return null;
        return DyeColor.byName(path.substring("octant_".length()), null);
    }
}

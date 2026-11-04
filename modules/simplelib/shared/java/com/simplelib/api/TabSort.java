package com.simplelib.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * QW11 (owner 04.11.): the order of block families inside a creative tab, one rule for every Simple mod. A family is the base block
 * and its shapes - material key from the id path (shape ending and variant words removed), block variant by variant (plain,
 * polished ... chiseled, pillar), each followed by its shapes: stairs, slab, wall, fence, fence gate, pane, trapdoor, door, octet. The test centre's
 * {@code ModBlockGallery} uses the same material key and ranks, so the gallery and the tabs agree. Pure functions on id paths; no world.
 *
 * <p>{@link #sort} keeps everything that is not a family where it stands (tools, food, single items) and puts each family at the place
 * of its first member, so a new item is placed by its id alone - nothing is listed by hand.
 */
public final class TabSort {
    private TabSort() {}

    /** Shape endings in tab order after the block variants (octet last: the eighth pieces of the family). */
    public static final List<String> SHAPES = List.of("_stairs", "_slab", "_wall", "_fence_gate", "_fence", "_pane", "_trapdoor", "_door", "_octet");
    /** Variant words, order = rank (plain first, then polished ... chiseled, pillar). */
    public static final List<String> VARIANT_WORDS = List.of("planks", "block", "log", "stem", "wood", "hyphae", "stripped",
            "cobbled", "polished", "smooth", "cut", "bricks", "brick", "tiles", "tile", "cracked", "mossy", "chiseled", "pillar",
            "waxed", "exposed", "weathered", "oxidized");

    /** Index of the shape ending of the path, -1 = none (a plain block or a non-block item). */
    public static int shapeIndex(String path) {
        for (int i = 0; i < SHAPES.size(); i++) {
            String suffix = SHAPES.get(i);
            if (path.endsWith(suffix) && path.length() > suffix.length()) return i;
        }
        return -1;
    }

    /** Material key of an id path: shape ending and variant words removed ({@code mossy_stone_brick_wall} -> {@code stone}). */
    public static String materialKey(String path) {
        String p = path;
        int shape = shapeIndex(p);
        if (shape >= 0) p = p.substring(0, p.length() - SHAPES.get(shape).length());
        List<String> kept = new ArrayList<>();
        for (String word : p.split("_")) {
            if (!VARIANT_WORDS.contains(word)) kept.add(word);
        }
        if (kept.isEmpty()) return p.replace("bricks", "brick").replace("tiles", "tile"); // "bricks", "brick_stairs"
        return String.join("_", kept);
    }

    /** Rank of the variant: the latest variant word counts ({@code mossy_stone_bricks} after {@code stone_bricks}). */
    public static int variantRank(String path) {
        int rank = -1;
        for (String word : path.split("_")) rank = Math.max(rank, VARIANT_WORDS.indexOf(word));
        return rank;
    }

    private static String path(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }

    /**
     * Sorts the family blocks of a tab (stable). A family is a material key shared by at least two entries of one namespace of which at
     * least one has a shape ending; its members move together to the place of the first member, ordered by variant rank, then shape.
     */
    public static List<ItemStack> sort(List<ItemStack> stacks) {
        Map<String, List<Integer>> groups = new LinkedHashMap<>();
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (CreativeTabPolicy.isVanilla(id) || !stack.getComponentsPatch().isEmpty()) continue;
            groups.computeIfAbsent(id.getNamespace() + ":" + materialKey(id.getPath()), k -> new ArrayList<>()).add(i);
        }
        Map<Integer, List<ItemStack>> atFirst = new LinkedHashMap<>();
        java.util.Set<Integer> moved = new java.util.HashSet<>();
        for (List<Integer> members : groups.values()) {
            if (members.size() < 2 || members.stream().noneMatch(i -> shapeIndex(path(stacks.get(i))) >= 0)) continue;
            List<ItemStack> family = new ArrayList<>();
            for (int i : members) family.add(stacks.get(i));
            family.sort(Comparator.<ItemStack>comparingInt(s -> variantRank(path(s))).thenComparingInt(s -> shapeIndex(path(s))));
            atFirst.put(members.get(0), family);
            moved.addAll(members);
        }
        List<ItemStack> out = new ArrayList<>(stacks.size());
        for (int i = 0; i < stacks.size(); i++) {
            if (atFirst.containsKey(i)) out.addAll(atFirst.get(i));
            else if (!moved.contains(i)) out.add(stacks.get(i));
        }
        return out;
    }
}

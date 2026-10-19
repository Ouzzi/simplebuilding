package com.simplebuilding.util;

import com.simplebuilding.version.McVersion;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * The sledgehammers by id (split stage 7, like {@code OvenRefs}). On 26.3 they belong to the required, bundled sub-mod
 * Simple Hammers ({@code simplehammers:*}) and SimpleBuilding registers none of them ({@link McVersion#HAMMERS_IN_SB});
 * tabs, the test centre, datagen and the tests find them in the registry. On 26.2 they are SimpleBuilding's own items
 * under {@code simplebuilding:*}, so the same calls work on both lines. Call after registration; an unregistered id
 * gives air.
 */
public final class HammerRefs {
    /** Tiers in tab order; the item ids are {@code <tier>_sledgehammer}. */
    public static final List<String> TIERS = List.of("stone", "copper", "iron", "gold", "diamond", "netherite", "enderite");

    private HammerRefs() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(McVersion.HAMMERS_NAMESPACE, path);
    }

    /** The sledgehammer of {@code tier} ({@code "iron"}). */
    public static Item item(String tier) {
        return BuiltInRegistries.ITEM.getValue(id(tier + "_sledgehammer"));
    }

    /** All seven, stone to Enderite. */
    public static List<Item> all() {
        List<Item> out = new ArrayList<>();
        for (String tier : TIERS) out.add(item(tier));
        return out;
    }

    /** A hammer enchantment ({@code "radius"}, {@code "override"}, {@code "break_through"}). */
    public static ResourceKey<Enchantment> enchantment(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT, id(path));
    }
}

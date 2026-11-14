package com.simplelib.material;

import com.simplelib.SimpleLib;
import com.simplelib.api.IdMoves;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;

/**
 * N48 (b-circ46): the ids of the material pressure plates that moved here. Simple Pads' {@code simplepads:{diamond,netherite,enderite}_pressure_plate}
 * and Simple Ores' {@code simpleores:enderite_pressure_plate} (the second Enderite plate, merged into the first) resolve to
 * {@code simplelib:}: blocks, items, the block entity type, the recipes and their advancements. Pads' own moves from
 * {@code simplebuilding:} to {@code simplepads:} chain onto these. Always registered (SimpleLib is the one mod that is there
 * whichever of the two is missing).
 */
public final class PlateMoves {
    private PlateMoves() {}

    public static void register() {
        for (String metal : new String[] {"diamond", "netherite", "enderite"}) {
            String plate = metal + "_pressure_plate";
            move(Registries.BLOCK, "simplepads", plate, plate);
            move(Registries.ITEM, "simplepads", plate, plate);
        }
        move(Registries.BLOCK, "simpleores", "enderite_pressure_plate", "enderite_pressure_plate");
        move(Registries.ITEM, "simpleores", "enderite_pressure_plate", "enderite_pressure_plate");
        // Simple Pads' netherite_pressure_plate_be (shared by the Netherite and Enderite plate) is now simplelib:filter_pressure_plate_be.
        move(Registries.BLOCK_ENTITY_TYPE, "simplepads", "netherite_pressure_plate_be", "filter_pressure_plate_be");
        move(Registries.RECIPE, "simplepads", "diamond_pressure_plate", "diamond_pressure_plate");
        move(Registries.RECIPE, "simplepads", "netherite_pressure_plate_smithing", "netherite_pressure_plate_smithing");
        move(Registries.RECIPE, "simplepads", "enderite_pressure_plate_smithing", "enderite_pressure_plate_smithing");
        move(Registries.RECIPE, "simpleores", "enderite_pressure_plate", "enderite_pressure_plate");
        move(Registries.ADVANCEMENT, "simplepads", "recipes/redstone/diamond_pressure_plate", "recipes/redstone/diamond_pressure_plate");
        move(Registries.ADVANCEMENT, "simplepads", "recipes/tools/netherite_pressure_plate_smithing", "recipes/tools/netherite_pressure_plate_smithing");
        move(Registries.ADVANCEMENT, "simplepads", "recipes/tools/enderite_pressure_plate_smithing", "recipes/tools/enderite_pressure_plate_smithing");
        move(Registries.ADVANCEMENT, "simpleores", "recipes/redstone/enderite_pressure_plate", "recipes/redstone/enderite_pressure_plate");
    }

    private static void move(net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<?>> registry, String namespace, String from, String to) {
        IdMoves.move(registry, Identifier.fromNamespaceAndPath(namespace, from), SimpleLib.id(to));
    }
}

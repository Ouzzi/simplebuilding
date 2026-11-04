package com.simplepads.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Parts of the required mods by id (no class import, principle 3): the building cores of SimpleLib, the Enderite of Simple Ores and
 * the mob heads of Simple Mobheads. Call after registration; an unregistered id gives air.
 */
public final class PadRefs {
    public static final String ORES = "simpleores";
    public static final String MOBHEADS = "simplemobheads";
    /** The Trial Chamber heads of Simple Mobheads (its tag {@code simplemobheads:trial_chamber_heads}) next to Vanilla's two. */
    public static final List<String> TRIAL_CHAMBER_HEADS = List.of("husk_head", "spider_head", "cave_spider_head", "stray_skull",
            "bogged_skull", "slime_head", "silverfish_head", "breeze_head");

    private PadRefs() {
    }

    /** {@code copper}, {@code iron}, {@code gold}, {@code diamond}, {@code netherite}, {@code enderite}: SimpleLib's core. */
    public static Item core(String metal) {
        return com.simplelib.api.SimpleLibApi.materialItem(metal + "_core");
    }

    public static Item ores(String path) {
        return BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(ORES, path));
    }

    public static Item enderiteTemplate() {
        return ores("enderite_upgrade_template");
    }

    public static Item mobHead(String path) {
        return BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(MOBHEADS, path));
    }

    /** Zombie head, skeleton skull and the Trial Chamber heads of Simple Mobheads. */
    public static List<Item> trialChamberHeads() {
        List<Item> heads = new ArrayList<>(List.of(Items.ZOMBIE_HEAD, Items.SKELETON_SKULL));
        for (String path : TRIAL_CHAMBER_HEADS) {
            Item head = mobHead(path);
            if (head != Items.AIR) heads.add(head);
        }
        return heads;
    }
}

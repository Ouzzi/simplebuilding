package com.simpledinos;

import com.simpledinos.config.DinosConfig;
import java.nio.file.Path;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Module constants (concept docs/ai/KONZEPT-A-WP18-2026-10-29.md section 3, owner choice ABN29b-4). Loader entry points call
 * {@link #init} once with their config directory, before any registry event.
 */
public final class SimpleDinos {
    public static final String MOD_ID = "simpledinos";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Dinosaurs");

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /**
     * Super-mod switch "Enable Simple Dinosaurs" (framework SubMods, mirrored by SimpleBuilding) and the own config. Off hides
     * the module's creative entries and its guide; fossil blocks, eggs and dinosaurs that exist keep working.
     */
    public static boolean on() {
        return com.simplebuilding.framework.api.SubMods.enabled(MOD_ID) && DinosConfig.enabled;
    }

    public static void init(Path configDir) {
        DinosConfig.load(configDir);
        DinosViewer.register();
        // Test centre station (framework contract, no import of the test centre): the module's items by id.
        com.simplebuilding.framework.api.TestCentreStations.register(MOD_ID, "dinos", "simpledinos.testcentre.station",
                java.util.List.of("simpledinos:fossil_block", "simpledinos:trex_egg", "simpledinos:triceratops_egg",
                        "simpledinos:stegosaurus_egg", "simpledinos:utahraptor_egg", "simpledinos:parasaurolophus_egg",
                        "simpledinos:large_saddle", "simpledinos:trex_armor_leather", "simpledinos:trex_armor_iron",
                        "simpledinos:trex_armor_diamond", "simpledinos:trex_armor_copper", "simpledinos:trex_armor_gold", "simpledinos:trex_armor_rose_gold", "simpledinos:trex_armor_rose_bronze", "simpledinos:trex_armor_netherite", "simpledinos:trex_armor_enderite", "simpledinos:triceratops_armor_leather", "simpledinos:triceratops_armor_iron",
                        "simpledinos:triceratops_armor_diamond", "simpledinos:triceratops_armor_gold", "simpledinos:triceratops_armor_rose_gold", "simpledinos:triceratops_armor_rose_bronze", "simpledinos:stegosaurus_armor_leather", "simpledinos:stegosaurus_armor_iron",
                        "simpledinos:stegosaurus_armor_diamond", "simpledinos:stegosaurus_armor_gold", "simpledinos:stegosaurus_armor_rose_gold", "simpledinos:stegosaurus_armor_rose_bronze", "simpledinos:dino_plow", "simpledinos:parasaurolophus_armor_leather",
                        "simpledinos:parasaurolophus_armor_iron", "simpledinos:parasaurolophus_armor_diamond", "simpledinos:parasaurolophus_armor_gold", "simpledinos:parasaurolophus_armor_rose_gold", "simpledinos:parasaurolophus_armor_rose_bronze", "simpledinos:utahraptor_armor_leather",
                        "simpledinos:utahraptor_armor_iron", "simpledinos:utahraptor_armor_diamond", "simpledinos:utahraptor_armor_gold", "simpledinos:utahraptor_armor_rose_gold", "simpledinos:utahraptor_armor_rose_bronze", "simpledinos:raptor_feather", "simpledinos:triceratops_eggshell", "simpledinos:stegosaurus_eggshell",
                        "simpledinos:utahraptor_eggshell", "simpledinos:parasaurolophus_eggshell", "minecraft:apple", "minecraft:fern", "minecraft:sweet_berries", "minecraft:chicken", "minecraft:wheat_seeds", "minecraft:brush",
                        "simpledinos:guide_book"));
    }

    private SimpleDinos() {}
}

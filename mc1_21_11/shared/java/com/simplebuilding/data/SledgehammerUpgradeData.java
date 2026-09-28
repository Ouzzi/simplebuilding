package com.simplebuilding.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simplebuilding.util.SledgehammerUpgrades;
import com.simplebuilding.util.SledgehammerUpgrades.Upgrade;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The sledgehammer machine upgrades as datapack data:
 * {@code data/<namespace>/sledgehammer_upgrades/<name>.json}.
 *
 * <pre>{@code
 * {
 *   "upgrades": [
 *     {
 *       "from": "simplebuilding:reinforced_hopper",
 *       "to": "simplebuilding:netherite_hopper",
 *       "material": "simplebuilding:netherite_nugget",   // held in the off hand, used up on success
 *       "min_hammer": "diamond",                         // any | diamond | netherite | enderite
 *       "damage_per_hit": 4,                             // durability per blow, five blows
 *       "tier": "netherite"                              // reinforced | netherite | enderite (advancements)
 *     }
 *   ],
 *   "remove": ["simplebuilding:reinforced_piston"]      // optional: no upgrade from these blocks
 * }
 * }</pre>
 *
 * The block keeps its properties and its block entity (the target block class has to allow that,
 * as every mod machine and chest does). The mod ships {@code reinforced.json} (the eight copper
 * chests), {@code netherite.json} and {@code enderite.json}, generated from the built-in table in
 * {@link SledgehammerUpgrades}; files in the {@code simplebuilding} namespace are applied first,
 * later entries for the same {@code from} block win.
 */
public final class SledgehammerUpgradeData {

    private static final Logger LOGGER = LoggerFactory.getLogger("simplebuilding/sledgehammer_upgrades");

    public static final String DIRECTORY = "sledgehammer_upgrades";
    public static final List<String> DEFAULT_FILES = List.of("reinforced", "netherite", "enderite");
    private static final List<String> HAMMERS = List.of("any", "diamond", "netherite", "enderite");

    private static volatile @Nullable Map<Block, Upgrade> loaded;

    private SledgehammerUpgradeData() {
    }

    /** The effective table: the loaded files, or the built-in table before the first load. */
    public static Map<Block, Upgrade> table() {
        Map<Block, Upgrade> table = loaded;
        return table != null ? table : SledgehammerUpgrades.builtInTable();
    }

    public static boolean isLoaded() {
        return loaded != null;
    }

    public static void resetToBuiltIn() {
        loaded = null;
    }

    /** Replaces the effective table with what {@code files} say. */
    public static void apply(Map<Identifier, JsonElement> files) {
        Map<Block, Upgrade> table = new LinkedHashMap<>();
        for (Map.Entry<Identifier, JsonElement> file : ModDataTables.inApplyOrder(files)) {
            try {
                JsonObject json = file.getValue().getAsJsonObject();
                if (json.has("upgrades")) {
                    for (JsonElement element : json.getAsJsonArray("upgrades")) {
                        Upgrade upgrade = upgrade(file.getKey(), element.getAsJsonObject());
                        if (upgrade != null) {
                            table.put(upgrade.from(), upgrade);
                        }
                    }
                }
                if (json.has("remove")) {
                    for (JsonElement element : json.getAsJsonArray("remove")) {
                        Block block = block(file.getKey(), element.getAsString());
                        if (block != null) {
                            table.remove(block);
                        }
                    }
                }
            } catch (RuntimeException e) {
                LOGGER.warn("Skipping sledgehammer upgrade file {}: {}", file.getKey(), e.toString());
            }
        }
        loaded = Map.copyOf(table);
    }

    private static @Nullable Upgrade upgrade(Identifier file, JsonObject json) {
        Block from = block(file, json.get("from").getAsString());
        Block to = block(file, json.get("to").getAsString());
        Identifier materialId = Identifier.tryParse(json.get("material").getAsString());
        if (from == null || to == null || materialId == null || !BuiltInRegistries.ITEM.containsKey(materialId)) {
            return null;
        }
        Item material = BuiltInRegistries.ITEM.getValue(materialId);
        int rank = json.has("min_hammer") ? HAMMERS.indexOf(json.get("min_hammer").getAsString()) : 0;
        if (rank < 0) {
            throw new IllegalArgumentException("unknown min_hammer " + json.get("min_hammer"));
        }
        int damage = json.has("damage_per_hit") ? Math.max(0, json.get("damage_per_hit").getAsInt()) : 1;
        String tier = json.has("tier") ? json.get("tier").getAsString() : "netherite";
        return new Upgrade(from, to, material, rank, damage, "enderite".equals(tier), "reinforced".equals(tier));
    }

    private static @Nullable Block block(Identifier file, String id) {
        Identifier key = Identifier.tryParse(id);
        if (key == null || !BuiltInRegistries.BLOCK.containsKey(key)) {
            LOGGER.warn("{}: unknown block {}, entry skipped", file, id);
            return null;
        }
        return BuiltInRegistries.BLOCK.getValue(key);
    }

    /** Which default file an upgrade belongs in. */
    public static String fileOf(Upgrade upgrade) {
        return upgrade.toReinforced() ? "reinforced" : upgrade.toEnderite() ? "enderite" : "netherite";
    }

    /** The default file {@code name} (one of {@link #DEFAULT_FILES}) from the built-in table, sorted by source block. */
    public static JsonObject defaultFile(String name) {
        List<Upgrade> upgrades = new ArrayList<>();
        for (Upgrade upgrade : SledgehammerUpgrades.builtInTable().values()) {
            if (fileOf(upgrade).equals(name)) {
                upgrades.add(upgrade);
            }
        }
        upgrades.sort(Comparator.comparing(u -> BuiltInRegistries.BLOCK.getKey(u.from()).toString()));
        JsonArray array = new JsonArray();
        for (Upgrade upgrade : upgrades) {
            JsonObject entry = new JsonObject();
            entry.addProperty("from", BuiltInRegistries.BLOCK.getKey(upgrade.from()).toString());
            entry.addProperty("to", BuiltInRegistries.BLOCK.getKey(upgrade.to()).toString());
            entry.addProperty("material", BuiltInRegistries.ITEM.getKey(upgrade.nugget()).toString());
            entry.addProperty("min_hammer", HAMMERS.get(upgrade.minHammerRank()));
            entry.addProperty("damage_per_hit", upgrade.damagePerHit());
            entry.addProperty("tier", fileOf(upgrade));
            array.add(entry);
        }
        JsonObject json = new JsonObject();
        json.add("upgrades", array);
        return json;
    }
}

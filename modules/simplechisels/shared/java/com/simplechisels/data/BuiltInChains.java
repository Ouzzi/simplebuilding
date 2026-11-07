package com.simplechisels.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simplechisels.SimpleChisels;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

/**
 * The built-in chains: this mod's own default files ({@code data/simplechisels/chisel_transformations/<tier>_<table>.json},
 * the Vanilla chains SimpleBuilding's chisel had), read from the mod's jar. They apply until the first datapack load and on a
 * client the server has not sent its tables yet; one source for both, so the defaults never drift from the shipped files.
 */
public final class BuiltInChains {
    private static volatile List<ChiselTables.Chain> chains;

    private BuiltInChains() {
    }

    /** The default file names, in apply order. */
    public static List<String> fileNames() {
        List<String> out = new ArrayList<>();
        for (String tier : ChiselTables.TIERS) {
            for (String table : ChiselTables.TABLES) out.add(tier + "_" + table);
        }
        return out;
    }

    public static List<ChiselTables.Chain> all() {
        List<ChiselTables.Chain> result = chains;
        if (result == null) {
            result = List.copyOf(load());
            chains = result;
        }
        return result;
    }

    private static List<ChiselTables.Chain> load() {
        List<ChiselTables.Chain> out = new ArrayList<>();
        for (String name : fileNames()) {
            String path = "/data/" + SimpleChisels.MOD_ID + "/" + ChiselTables.DIRECTORY + "/" + name + ".json";
            try (InputStream in = BuiltInChains.class.getResourceAsStream(path)) {
                if (in == null) continue;
                JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                String tier = json.get("tier").getAsString();
                String table = json.has("table") ? json.get("table").getAsString() : "chisel";
                for (JsonElement element : json.getAsJsonArray("chains")) {
                    JsonObject chain = element.getAsJsonObject();
                    List<Block> blocks = new ArrayList<>();
                    for (JsonElement block : chain.getAsJsonArray("blocks")) {
                        Identifier id = Identifier.tryParse(block.getAsString());
                        blocks.add(id != null && BuiltInRegistries.BLOCK.containsKey(id) ? BuiltInRegistries.BLOCK.getValue(id) : null);
                    }
                    out.add(new ChiselTables.Chain(tier, table, chain.has("cyclic") && chain.get("cyclic").getAsBoolean(), blocks));
                }
            } catch (Exception e) {
                SimpleChisels.LOG.warn("Could not read the built-in chisel chains {}: {}", path, e.toString());
            }
        }
        return out;
    }
}

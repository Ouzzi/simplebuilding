package com.simplebuilding.modules.simplechallenges.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.simplebuilding.modules.simplechallenges.SimpleChallenges;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** The last played challenges (three buttons on the world creation screen), most recent first, kept in {@code config/simplechallenges/recent.json}. */
public final class RecentChallenges {
    public static final int SLOTS = 3;
    /** Shown when fewer than three were played yet. */
    public static final List<String> DEFAULTS = List.of("classic_skyblock", "sky_one_block", "vanilla_unlock");

    private RecentChallenges() {}

    /** The list after playing {@code id}: it moves to the front, no duplicates, at most {@link #SLOTS} entries. */
    public static List<String> push(List<String> recent, String id) {
        List<String> out = new ArrayList<>();
        out.add(id);
        for (String s : recent) if (!out.contains(s) && out.size() < SLOTS) out.add(s);
        return out;
    }

    /** What the three buttons show: the played ones, then the defaults for the free places. */
    public static List<String> shown(List<String> recent) {
        List<String> out = new ArrayList<>(recent.size() > SLOTS ? recent.subList(0, SLOTS) : recent);
        for (String d : DEFAULTS) if (out.size() < SLOTS && !out.contains(d)) out.add(d);
        return out;
    }

    private static Path file(Path dir) {
        return dir.resolve(SimpleChallenges.MOD_ID).resolve("recent.json");
    }

    public static List<String> load(Path dir) {
        List<String> out = new ArrayList<>();
        try {
            Path f = file(dir);
            if (!Files.isRegularFile(f)) return out;
            JsonElement e = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8));
            if (e.isJsonArray()) for (JsonElement s : e.getAsJsonArray()) if (s.isJsonPrimitive() && out.size() < SLOTS) out.add(s.getAsString());
        } catch (IOException | RuntimeException e) {
            SimpleChallenges.LOG.warn("Could not read recent.json: {}", e.toString());
        }
        return out;
    }

    public static void save(Path dir, List<String> recent) {
        try {
            Path f = file(dir);
            Files.createDirectories(f.getParent());
            JsonArray a = new JsonArray();
            for (String s : recent) a.add(s);
            Files.writeString(f, new Gson().toJson(a), StandardCharsets.UTF_8);
        } catch (IOException e) {
            SimpleChallenges.LOG.warn("Could not write recent.json: {}", e.toString());
        }
    }
}

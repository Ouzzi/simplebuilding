package com.simplebuilding.modules.simplechallenges.core;

import com.simplebuilding.modules.simplechallenges.SimpleChallenges;
import com.simplebuilding.modules.simplechallenges.preset.Preset;
import com.simplebuilding.modules.simplechallenges.preset.PresetParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Every preset the game knows: the shipped ones and those of data packs ({@code data/<ns>/simplechallenges/preset/<id>.json}, so
 * a world's datapacks folder works) and the user's own in {@code config/simplechallenges/presets/}. Broken files are kept with
 * their readable errors ({@link #problems}) instead of crashing; loaded at server start and on /reload.
 */
public final class PresetRegistry {
    private static final String DIR = "simplechallenges/preset";
    private static final Map<String, Preset> PRESETS = new LinkedHashMap<>();
    private static final List<String> PROBLEMS = new ArrayList<>();

    private PresetRegistry() {}

    public static synchronized Preset get(String id) {
        return PRESETS.get(id);
    }

    public static synchronized List<String> ids() {
        return new ArrayList<>(PRESETS.keySet());
    }

    public static synchronized List<Preset> all() {
        return new ArrayList<>(PRESETS.values());
    }

    public static synchronized List<String> problems() {
        return Collections.unmodifiableList(new ArrayList<>(PROBLEMS));
    }

    /** The id of a data pack file: the plain name in our namespace, else {@code namespace/name}. */
    static String idOf(Identifier file) {
        String name = file.getPath().substring(DIR.length() + 1, file.getPath().length() - ".json".length());
        return file.getNamespace().equals(SimpleChallenges.MOD_ID) ? name : file.getNamespace() + "/" + name;
    }

    public static synchronized void load(ResourceManager resources, Path configDir) {
        PRESETS.clear();
        PROBLEMS.clear();
        try {
            Map<Identifier, Resource> found = resources.listResources(DIR, id -> id.getPath().endsWith(".json"));
            List<Identifier> ids = new ArrayList<>(found.keySet());
            ids.sort(null);
            for (Identifier file : ids) {
                try (Reader in = found.get(file).openAsReader()) {
                    add(idOf(file), readAll(in), file.toString());
                } catch (Exception e) {
                    PROBLEMS.add(file + ": " + e);
                }
            }
        } catch (RuntimeException e) {
            PROBLEMS.add("could not list presets: " + e);
        }
        if (configDir != null) {
            Path folder = configDir.resolve(SimpleChallenges.MOD_ID).resolve("presets");
            if (Files.isDirectory(folder)) {
                try (Stream<Path> files = Files.list(folder)) {
                    for (Path p : (Iterable<Path>) files.filter(f -> f.getFileName().toString().endsWith(".json")).sorted()::iterator) {
                        String name = p.getFileName().toString();
                        try {
                            add("user/" + name.substring(0, name.length() - 5), Files.readString(p, StandardCharsets.UTF_8), p.toString());
                        } catch (IOException e) {
                            PROBLEMS.add(p + ": " + e);
                        }
                    }
                } catch (IOException e) {
                    PROBLEMS.add("could not read " + folder + ": " + e);
                }
            }
        }
        SimpleChallenges.LOG.info("Simple Challenges: {} presets, {} problems", PRESETS.size(), PROBLEMS.size());
    }

    private static String readAll(Reader in) throws IOException {
        StringBuilder text = new StringBuilder();
        char[] buf = new char[4096];
        for (int n; (n = in.read(buf)) > 0; ) text.append(buf, 0, n);
        return text.toString();
    }

    /** Parses and stores one preset text; problems are recorded, a later file with the same id replaces an earlier one. */
    public static synchronized void add(String id, String text, String source) {
        PresetParser.Result r = PresetParser.parse(id, text);
        if (r.ok()) {
            PRESETS.put(id, r.preset());
            for (String w : r.warnings()) SimpleChallenges.LOG.info("Preset {}: {}", id, w);
        } else {
            PROBLEMS.add(source + ": " + String.join("; ", r.errors()));
        }
    }

    public static synchronized void remove(String id) {
        PRESETS.remove(id);
    }
}

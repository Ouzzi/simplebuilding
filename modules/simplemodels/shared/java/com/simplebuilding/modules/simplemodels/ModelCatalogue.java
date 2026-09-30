package com.simplebuilding.modules.simplemodels;

import com.google.gson.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.*;

/** Bounded nonrecursive folder scan. No client upload, archive extraction, or network access. */
public final class ModelCatalogue {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final int MAX_SNAPSHOT_CHARS = 28000;
    public record Snapshot(ModelPolicy policy, List<ModelDefinition> models) {}
    private static void checkDepth(String json) {
        int depth = 0; boolean quoted = false, escaped = false;
        for (char c : json.toCharArray()) {
            if (quoted) { if (escaped) escaped = false; else if (c == '\\') escaped = true; else if (c == '"') quoted = false; }
            else if (c == '"') quoted = true;
            else if (c == '{' || c == '[') { if (++depth > 16) throw new IllegalArgumentException("JSON nesting limit"); }
            else if (c == '}' || c == ']') { if (--depth < 0) throw new IllegalArgumentException("Invalid JSON"); }
        }
    }
    public static String read(Path file, int limit) throws IOException {
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(file))
            throw new IOException("Not a regular file");
        try (var input = Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)) {
            byte[] bytes = input.readNBytes(limit + 1);
            if (bytes.length > limit) throw new IOException("Oversized file");
            String json = new String(bytes, StandardCharsets.UTF_8);
            try { checkDepth(json); } catch (IllegalArgumentException e) { throw new IOException(e.getMessage(), e); }
            return json;
        }
    }
    public static Snapshot load(Path root) throws IOException {
        Files.createDirectories(root);
        if (Files.isSymbolicLink(root)) throw new IOException("Symlinked model folder");
        var policy = new ModelPolicy();
        Path config = root.resolve("config.json");
        if (Files.exists(config, LinkOption.NOFOLLOW_LINKS)) {
            try { policy = GSON.fromJson(read(config, 4096), ModelPolicy.class); if (policy == null) throw new IOException("Empty policy"); }
            catch (RuntimeException | IOException e) { policy = new ModelPolicy(); policy.enabled = false; }
        } else Files.writeString(config, GSON.toJson(policy), StandardOpenOption.CREATE_NEW);
        policy.clamp();
        Path folder = root.resolve("catalogue");
        Files.createDirectories(folder);
        if (Files.isSymbolicLink(folder)) throw new IOException("Symlinked catalogue");
        var models = new ArrayList<ModelDefinition>();
        var ids = new HashSet<String>();
        // Enumerate at most 256 paths, not an unbounded sort of an attacker-controlled directory.
        var files = new ArrayList<Path>();
        try (var entries = Files.newDirectoryStream(folder, "*.json")) {
            for (Path file : entries) {
                if (files.size() == 256) throw new IOException("Catalogue file limit exceeded");
                files.add(file);
            }
        }
        files.sort(Comparator.comparing(p -> p.getFileName().toString()));
        int length = 0;
        for (Path file : files) {
            if (models.size() == policy.maxModels) break;
            try {
                var def = ModelDefinition.parse(JsonParser.parseString(read(file, policy.maxFileBytes)).getAsJsonObject());
                int size = GSON.toJson(def).length();
                if (!ids.add(def.id()) || length + size > 24000) continue;
                models.add(def); length += size;
            } catch (RuntimeException | IOException e) {
                org.slf4j.LoggerFactory.getLogger("simplemodels").warn("Rejected model definition {}: {}", file.getFileName(), e.getMessage());
            }
        }
        return new Snapshot(policy, List.copyOf(models));
    }
    public static Snapshot decode(String json) {
        if (json.length() > MAX_SNAPSHOT_CHARS) throw new IllegalArgumentException("Oversized catalogue");
        checkDepth(json);
        var obj = JsonParser.parseString(json).getAsJsonObject();
        var p = GSON.fromJson(obj.get("policy"), ModelPolicy.class); p.clamp();
        var models = new ArrayList<ModelDefinition>();
        for (var element : obj.getAsJsonArray("models")) {
            if (models.size() == p.maxModels) break;
            models.add(ModelDefinition.parse(element.getAsJsonObject()));
        }
        return new Snapshot(p, List.copyOf(models));
    }
    private ModelCatalogue() {}
}

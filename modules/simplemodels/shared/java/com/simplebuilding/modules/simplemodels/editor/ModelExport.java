package com.simplebuilding.modules.simplemodels.editor;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Editor save / export (b-models14, concept §5 / stage 6). Pure file logic, no client classes, tested in
 * {@code editor_export}:
 * <ul>
 *   <li>workspace: {@code config/simplemodels/workspace/<name>.json} (vanilla model JSON, cube names kept);</li>
 *   <li>"use as model": a resource pack folder {@code resourcepacks/simplemodels-editor} with {@code pack.mcmeta},
 *       {@code assets/mymodels/models/item/<name>.json} and the item model definition {@code assets/mymodels/items/<name>.json},
 *       plus a catalogue entry {@code config/simplemodels/catalog/<name>.json} (anvil {@code @model:mymodels:<name>});</li>
 *   <li>zip: the same pack files as {@code simplemodels/exports/<name>.zip} to pass on or put on a server.</li>
 * </ul>
 * Path safety: names are {@code [a-z0-9_]{1,20}} (no separators, no dots), every target is resolved against a fixed root
 * folder chosen by the caller, normalised and must stay inside it; the root and every folder on the way must not be a
 * symbolic link; files are written to a temporary file and moved into place. Reading has a size limit.
 */
public final class ModelExport {
    public static final String NAMESPACE = "mymodels", PACK_FOLDER = "simplemodels-editor", PACK_ID = "file/" + PACK_FOLDER;
    public static final int MAX_NAME = 20, MAX_LISTED = 256;
    private static final Pattern NAME = Pattern.compile("[a-z0-9_]{1," + MAX_NAME + "}");
    private static final com.google.gson.Gson PRETTY = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /** {@code raw} as a file/model name: trimmed, lower case, spaces and dashes to {@code _}; null when not allowed. */
    public static String safeName(String raw) {
        if (raw == null) return null;
        String name = raw.trim().toLowerCase(Locale.ROOT).replaceAll("[\\s-]+", "_");
        return NAME.matcher(name).matches() ? name : null;
    }

    /** Item model id of an export ({@code mymodels:<name>}); the catalogue entry and {@code item_model} use it. */
    public static String modelId(String name) {
        return NAMESPACE + ":" + name;
    }

    /** The pack files of one export, relative path to text, in write order. */
    public static Map<String, String> packFiles(EditableModel model, String name, int packFormat) {
        if (safeName(name) == null || !safeName(name).equals(name)) throw new IllegalArgumentException("Unsafe model name");
        var files = new LinkedHashMap<String, String>();
        var pack = new JsonObject();
        pack.addProperty("description", "Simple Models editor exports");
        pack.addProperty("min_format", packFormat);
        pack.addProperty("max_format", packFormat);
        var meta = new JsonObject();
        meta.add("pack", pack);
        files.put("pack.mcmeta", PRETTY.toJson(meta));
        files.put("assets/" + NAMESPACE + "/models/item/" + name + ".json", ModelJson.write(model));
        var inner = new JsonObject();
        inner.addProperty("type", "minecraft:model");
        inner.addProperty("model", NAMESPACE + ":item/" + name);
        var item = new JsonObject();
        item.add("model", inner);
        files.put("assets/" + NAMESPACE + "/items/" + name + ".json", PRETTY.toJson(item));
        return files;
    }

    /** Catalogue entry (same fields as the built-in examples) so the anvil offers the export on {@code baseItem}. */
    public static String catalogueEntry(String name, String baseItem, String author) {
        var o = new JsonObject();
        o.addProperty("id", modelId(name));
        o.addProperty("base_item", baseItem);
        String display = name.replace('_', ' ');
        o.addProperty("match_name", Character.toUpperCase(display.charAt(0)) + display.substring(1));
        o.addProperty("model", modelId(name));
        var tags = new JsonArray();
        tags.add("editor");
        o.add("tags", tags);
        o.addProperty("author", author == null || author.isBlank() ? "Editor" : author.length() > 32 ? author.substring(0, 32) : author);
        return PRETTY.toJson(o);
    }

    /**
     * {@code relative} inside {@code root}: no absolute path, no {@code ..}, no backslash; the normalised result must start
     * with the root; the root and every existing folder from it to the target must not be a symbolic link.
     */
    public static Path resolveInside(Path root, String relative) throws IOException {
        if (relative == null || relative.isEmpty() || relative.contains("..") || relative.contains("\\") || relative.startsWith("/") || relative.contains(":"))
            throw new IOException("Unsafe path " + relative);
        Path base = root.toAbsolutePath().normalize();
        Path target = base.resolve(relative).normalize();
        if (!target.startsWith(base) || target.equals(base)) throw new IOException("Path leaves the export folder: " + relative);
        for (Path p = target; p != null && p.startsWith(base); p = p.getParent()) {
            if (Files.isSymbolicLink(p)) throw new IOException("Symbolic link on the way: " + base.relativize(p));
            if (p.equals(base)) break;
        }
        return target;
    }

    private static void folder(Path dir) throws IOException {
        Path abs = dir.toAbsolutePath().normalize();
        if (Files.isSymbolicLink(abs)) throw new IOException("Symbolic link as export folder");
        Files.createDirectories(abs);
        if (!Files.isDirectory(abs, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Not a folder: " + abs.getFileName());
    }

    /** Writes {@code files} below {@code root} (created if missing); each through a temporary file. Returns the targets. */
    public static List<Path> writeFiles(Path root, Map<String, String> files) throws IOException {
        folder(root);
        var out = new ArrayList<Path>();
        for (var entry : files.entrySet()) {
            Path target = resolveInside(root, entry.getKey());
            Files.createDirectories(target.getParent());
            resolveInside(root, entry.getKey()); // folders created just now: check the way again
            Path temp = Files.createTempFile(target.getParent(), ".simplemodels-", ".tmp");
            try {
                Files.writeString(temp, entry.getValue(), StandardCharsets.UTF_8);
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } finally {
                Files.deleteIfExists(temp);
            }
            out.add(target);
        }
        return out;
    }

    /** The pack files as {@code <dir>/<name>.zip}; entry names are the checked relative paths. */
    public static Path writeZip(Path dir, String name, Map<String, String> files) throws IOException {
        folder(dir);
        if (safeName(name) == null) throw new IOException("Unsafe model name");
        Path zip = resolveInside(dir, name + ".zip");
        Path temp = Files.createTempFile(zip.getParent(), ".simplemodels-", ".tmp");
        try {
            try (var out = new ZipOutputStream(Files.newOutputStream(temp))) {
                for (var entry : files.entrySet()) {
                    resolveInside(dir, entry.getKey()); // same rules for entry names (no zip slip for whoever unpacks it)
                    out.putNextEntry(new ZipEntry(entry.getKey()));
                    out.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                    out.closeEntry();
                }
            }
            Files.move(temp, zip, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(temp);
        }
        return zip;
    }

    // ---------------------------------------------------------------- workspace

    public static Path saveWorkspace(Path dir, String name, EditableModel model) throws IOException {
        if (safeName(name) == null || !safeName(name).equals(name)) throw new IOException("Unsafe model name");
        return writeFiles(dir, Map.of(name + ".json", ModelJson.write(model))).getFirst();
    }

    /** Saved workspace names (files {@code <name>.json} with an allowed name), sorted, at most {@link #MAX_LISTED}. */
    public static List<String> workspaces(Path dir) throws IOException {
        if (!Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(dir)) return List.of();
        var names = new ArrayList<String>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, "*.json")) {
            for (Path file : files) {
                if (names.size() == MAX_LISTED) break;
                String n = file.getFileName().toString();
                n = n.substring(0, n.length() - 5);
                if (NAME.matcher(n).matches() && Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) names.add(n);
            }
        }
        names.sort(null);
        return names;
    }

    /** Reads a workspace (size limit {@link ModelJson#MAX_CHARS}, no links); all or nothing like {@link ModelJson#read}. */
    public static ModelJson.Result loadWorkspace(Path dir, String name) throws IOException {
        if (safeName(name) == null || !safeName(name).equals(name)) throw new IOException("Unsafe model name");
        Path file = resolveInside(dir, name + ".json");
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) throw new IOException("No such workspace");
        try (var in = Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)) {
            byte[] bytes = in.readNBytes(ModelJson.MAX_CHARS * 4 + 1);
            if (bytes.length > ModelJson.MAX_CHARS * 4) throw new IOException("Oversized workspace");
            return ModelJson.read(new String(bytes, StandardCharsets.UTF_8));
        }
    }

    private ModelExport() {}
}

package com.simplebuilding.dev.testcentre;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * N50 TZEDIT50 Phase 2 (Besitzer: "die Testzentrale per Config", Plan {@code docs/ai/PLAN-B-TZEDIT50-2026-11-14.md}): die
 * Layout-Configs der Linien ({@code layout_lines_<area>.json}, Schema {@code tools/tzedit/tzedit_save.py}), je Bereich
 * {@link #AREAS} eine Datei. Quelle: eine Override-Datei im Spielordner ({@code config/simplebuilding/testcentre/}) hat Vorrang
 * vor der Jar-Ressource ({@code data/simplebuilding/testcentre/}); {@code /testcenter layout reload} liest neu, ohne neues Jar.
 * Ohne Datei fuer einen Bereich baut {@link OrderAreas#families} ihn rein algorithmisch.
 */
public final class LayoutConfig {

    /** Die Reihen-Bereiche der Linien in Welt-Reihenfolge (Seite A, dann Stein) und ihre Reihen-Art. */
    public static final List<String> AREAS = List.of("metals", "wood", "aging", "rest", "stone");
    static final Map<String, OrderAreas.Kind> KINDS = Map.of("metals", OrderAreas.Kind.METAL, "wood", OrderAreas.Kind.WOOD,
            "aging", OrderAreas.Kind.AGING, "rest", OrderAreas.Kind.REST, "stone", OrderAreas.Kind.STONE);
    /** Ressourcen-Ordner im Jar und Override-Ordner im Spielordner. */
    public static final String RESOURCE_DIR = "data/simplebuilding/testcentre/";
    public static final Path OVERRIDE_DIR = Path.of("config", "simplebuilding", "testcentre");
    static final Set<String> PLACEHOLDERS = Set.of("#grey", "#light", "#red", "#frame");
    private static final Pattern NAME = Pattern.compile("^[a-z0-9][a-z0-9_-]{0,47}$");
    private static final Pattern ID = Pattern.compile("^(?:[a-z0-9_.-]{1,64}:)?[a-z0-9_./-]{1,128}$");

    /** Eine Reihe der Config: Schluessel, Schild-Text, Zellen (Id, Platzhalter oder null) je Spalte, Achtel je Spalte. */
    public record RowSpec(String key, String label, List<String> cells, Map<String, String> tops) {
    }

    /** Ein Bereich: Spalten, Reihen in Welt-Reihenfolge, bewusst nicht platzierte Ids. */
    public record Area(String area, String source, List<String> columns, List<RowSpec> rows, Set<String> excluded) {
    }

    /** Keine Config: alles algorithmisch (Verhalten vor Phase 2). */
    public static final LayoutConfig NONE = new LayoutConfig(Map.of(), List.of());

    private static volatile LayoutConfig current;

    private final Map<String, Area> areas;
    private final List<String> errors;

    public LayoutConfig(Map<String, Area> areas, List<String> errors) {
        this.areas = Map.copyOf(areas);
        this.errors = List.copyOf(errors);
    }

    public boolean isEmpty() {
        return areas.isEmpty();
    }

    public Area area(String id) {
        return areas.get(id);
    }

    public Map<String, Area> areas() {
        return areas;
    }

    /** Schema-Fehler beim Lesen (je Datei/Zelle); fehlerhafte Bereiche fehlen in {@link #areas}. */
    public List<String> errors() {
        return errors;
    }

    /** Die geladene Config (beim ersten Aufruf gelesen). */
    public static LayoutConfig current() {
        LayoutConfig config = current;
        if (config == null) {
            config = load();
            current = config;
        }
        return config;
    }

    /** Fuer Tests und den Reload-Befehl: die Config setzen ({@code null} = beim naechsten Zugriff neu lesen). */
    public static void set(LayoutConfig config) {
        current = config;
    }

    /** Liest alle Bereiche neu (Override vor Jar) und macht sie zur aktuellen Config. */
    public static LayoutConfig reload() {
        LayoutConfig config = load();
        current = config;
        return config;
    }

    static LayoutConfig load() {
        Map<String, String> texts = new LinkedHashMap<>();
        Map<String, String> sources = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        for (String area : AREAS) {
            String file = "layout_lines_" + area + ".json";
            Path override = OVERRIDE_DIR.resolve(file);
            try {
                if (Files.isRegularFile(override)) {
                    texts.put(area, Files.readString(override, StandardCharsets.UTF_8));
                    sources.put(area, "override " + override);
                    continue;
                }
                try (InputStream in = LayoutConfig.class.getClassLoader().getResourceAsStream(RESOURCE_DIR + file)) {
                    if (in != null) {
                        texts.put(area, new String(in.readAllBytes(), StandardCharsets.UTF_8));
                        sources.put(area, "jar " + RESOURCE_DIR + file);
                    }
                }
            } catch (IOException e) {
                errors.add(file + ": " + e.getMessage());
            }
        }
        LayoutConfig parsed = parse(texts, sources);
        errors.addAll(parsed.errors());
        return new LayoutConfig(parsed.areas(), errors);
    }

    /** Liest die Texte je Bereich; ein Bereich mit Schema-Fehler wird ausgelassen (Fehler je Zelle in {@link #errors}). */
    public static LayoutConfig parse(Map<String, String> texts, Map<String, String> sources) {
        Map<String, Area> areas = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        texts.forEach((id, text) -> {
            List<String> bad = new ArrayList<>();
            Area area = null;
            try {
                area = area(id, sources.getOrDefault(id, "?"), JsonParser.parseString(text).getAsJsonObject(), bad);
            } catch (RuntimeException e) {
                bad.add("not a JSON object (" + e.getMessage() + ")");
            }
            if (bad.isEmpty() && area != null) {
                areas.put(id, area);
            } else {
                bad.forEach(problem -> errors.add("layout_lines_" + id + ": " + problem));
            }
        });
        return new LayoutConfig(areas, errors);
    }

    private static Area area(String id, String source, JsonObject json, List<String> bad) {
        if (!"lines".equals(string(json, "variant")) || !id.equals(string(json, "area"))) {
            bad.add("variant/area is not lines/" + id);
        }
        List<String> columns = new ArrayList<>();
        JsonArray cols = json.has("columns") && json.get("columns").isJsonArray() ? json.getAsJsonArray("columns") : new JsonArray();
        for (JsonElement c : cols) {
            String name = c.isJsonPrimitive() ? c.getAsString() : "";
            if (!NAME.matcher(name).matches() || columns.contains(name)) {
                bad.add("invalid or duplicate column " + name);
            }
            columns.add(name);
        }
        if (columns.isEmpty()) {
            bad.add("no columns");
        }
        List<RowSpec> rows = new ArrayList<>();
        Set<String> keys = new LinkedHashSet<>();
        JsonArray list = json.has("rows") && json.get("rows").isJsonArray() ? json.getAsJsonArray("rows") : new JsonArray();
        for (JsonElement element : list) {
            if (!element.isJsonObject()) {
                bad.add("row is no object");
                continue;
            }
            JsonObject row = element.getAsJsonObject();
            String key = string(row, "key");
            if (key == null || !NAME.matcher(key).matches() || !keys.add(key)) {
                bad.add("invalid or duplicate row key " + key);
                continue;
            }
            JsonArray cellArray = row.has("cells") && row.get("cells").isJsonArray() ? row.getAsJsonArray("cells") : new JsonArray();
            if (cellArray.size() != columns.size()) {
                bad.add("row " + key + ": " + cellArray.size() + " cells for " + columns.size() + " columns");
                continue;
            }
            List<String> cells = new ArrayList<>();
            for (int i = 0; i < cellArray.size(); i++) {
                JsonElement cell = cellArray.get(i);
                String value = cell == null || cell.isJsonNull() ? null : cell.isJsonPrimitive() ? cell.getAsString() : "";
                if (value != null && !PLACEHOLDERS.contains(value) && !ID.matcher(value).matches()) {
                    bad.add("row " + key + " column " + columns.get(i) + ": invalid cell " + value);
                }
                cells.add(value);
            }
            Map<String, String> tops = new LinkedHashMap<>();
            if (row.has("tops") && row.get("tops").isJsonObject()) {
                for (Map.Entry<String, JsonElement> top : row.getAsJsonObject("tops").entrySet()) {
                    String value = top.getValue().isJsonPrimitive() ? top.getValue().getAsString() : "";
                    if (!columns.contains(top.getKey()) || !ID.matcher(value).matches()) {
                        bad.add("row " + key + ": invalid top " + top.getKey() + " = " + value);
                    }
                    tops.put(top.getKey(), value);
                }
            }
            String label = string(row, "label");
            rows.add(new RowSpec(key, label == null ? key : label, java.util.Collections.unmodifiableList(cells), Map.copyOf(tops)));
        }
        Set<String> excluded = new LinkedHashSet<>();
        if (json.has("excluded") && json.get("excluded").isJsonArray()) {
            for (JsonElement e : json.getAsJsonArray("excluded")) {
                String value = e.isJsonPrimitive() ? e.getAsString() : "";
                if (!ID.matcher(value).matches()) {
                    bad.add("invalid excluded id " + value);
                }
                excluded.add(full(value));
            }
        }
        return new Area(id, source, List.copyOf(columns), List.copyOf(rows), Set.copyOf(excluded));
    }

    /** Id mit Namensraum ({@code stone -> minecraft:stone}). */
    static String full(String id) {
        return id.contains(":") ? id : "minecraft:" + id;
    }

    private static String string(JsonObject json, String key) {
        JsonElement e = json.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
    }

    /** Eine Kurzbeschreibung fuer den Reload-Befehl. */
    public String summary() {
        List<String> parts = new ArrayList<>();
        for (String id : AREAS) {
            Area area = areas.get(id);
            parts.add(id + "=" + (area == null ? "algorithm" : area.rows().size() + " rows (" + area.source() + ")"));
        }
        return String.join(", ", parts);
    }
}

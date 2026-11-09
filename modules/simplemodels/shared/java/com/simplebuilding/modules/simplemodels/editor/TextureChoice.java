package com.simplebuilding.modules.simplemodels.editor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Editor texture choice (b-models14, concept §3 "Texturwahl"): the model's texture variables ({@code #side}, {@code #top}
 * ...) and a search over the block atlas ids the client has loaded (vanilla and mods). Only references to existing
 * atlas textures, no image files (uploads stay out, as the concept says). Pure functions, tested in
 * {@code editor_textures}; the client {@code TexturePickerScreen} draws them.
 */
public final class TextureChoice {
    public static final int MAX_RESULTS = 512;

    /** One variable: its name, its raw value ({@code #other} or an id) and the id it finally resolves to (or null). */
    public record Variable(String name, String value, String resolved) {}

    public static List<Variable> variables(EditableModel model) {
        var out = new ArrayList<Variable>();
        model.textures().forEach((name, value) -> out.add(new Variable(name, value, resolve(model, value))));
        return out;
    }

    /** {@code #var} references followed (max. 8 steps, so loops end); the id, or null when unresolved. */
    public static String resolve(EditableModel model, String texture) {
        String value = texture;
        for (int i = 0; i < 8 && value != null && value.startsWith("#"); i++) value = model.textures().get(value.substring(1));
        return value == null || value.startsWith("#") ? null : value;
    }

    /**
     * Atlas ids containing every word of {@code query} (case-insensitive), shortest first then alphabetical, at most
     * {@code limit}. An empty query lists everything (up to the limit).
     */
    public static List<String> search(Collection<String> ids, String query, int limit) {
        String[] words = query == null ? new String[0] : query.toLowerCase(Locale.ROOT).trim().split("\\s+");
        return ids.stream()
                .filter(id -> {
                    String lower = id.toLowerCase(Locale.ROOT);
                    for (String w : words) if (!w.isEmpty() && !lower.contains(w)) return false;
                    return true;
                })
                .sorted(Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder()))
                .limit(Math.max(0, Math.min(limit, MAX_RESULTS)))
                .toList();
    }

    /** Sets variable {@code name} to texture id {@code id}; invalid ids or names leave the model unchanged. */
    public static EditableModel assign(EditableModel model, String name, String id) {
        if (name == null || id == null || id.startsWith("#") || !ModelJson.texture(id) || !ModelJson.texture("#" + name)) return model;
        if (!model.textures().containsKey(name) && model.textures().size() >= ModelJson.MAX_TEXTURES) return model;
        return model.withTexture(name, id);
    }

    /** {@code base}, else {@code base_2}, {@code base_3} ... whichever variable is still free. */
    public static String freeVariable(EditableModel model, String base) {
        if (!model.textures().containsKey(base)) return base;
        for (int n = 2; ; n++) if (!model.textures().containsKey(base + "_" + n)) return base + "_" + n;
    }

    private TextureChoice() {}
}

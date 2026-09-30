package com.simplebuilding.modules.simplemodels;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;
import java.util.List;
import java.util.ArrayList;

/** The original renamed CIT fields, with strict length, registry and identifier validation. */
public record ModelDefinition(String id, String base_item, String match_name, String model,
                              List<String> tags, String author) {
    public static Identifier safeId(String value) {
        if (value == null || value.length() > 128 || value.contains("..") || value.contains("\\"))
            throw new IllegalArgumentException("Unsafe model identifier");
        var id = Identifier.tryParse(value);
        if (id == null || id.getPath().startsWith("/") || id.getPath().endsWith("/"))
            throw new IllegalArgumentException("Invalid model identifier");
        return id;
    }
    private static String text(JsonObject json, String key, int max) {
        String value = json.get(key).getAsString();
        if (value.isBlank() || value.length() > max || value.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Invalid " + key);
        return value;
    }
    public static ModelDefinition parse(JsonObject json) {
        String rawId = text(json, "id", 40);
        String id = safeId(rawId.contains(":") ? rawId : "renamed:" + rawId).toString();
        if (id.length() > 40) throw new IllegalArgumentException("Assignment id exceeds anvil limit");
        String base = safeId(text(json, "base_item", 128)).toString();
        if (!BuiltInRegistries.ITEM.containsKey(safeId(base)) || BuiltInRegistries.ITEM.getValue(safeId(base)) == Items.AIR)
            throw new IllegalArgumentException("Unknown base item");
        String name = text(json, "match_name", 48);
        String model = safeId(text(json, "model", 128)).toString();
        List<String> tags = new ArrayList<>();
        if (json.has("tags")) {
            if (json.getAsJsonArray("tags").size() > 8) throw new IllegalArgumentException("Too many tags");
            for (var tag : json.getAsJsonArray("tags")) {
                String s = tag.getAsString();
                if (s.length() > 24 || s.chars().anyMatch(Character::isISOControl)) throw new IllegalArgumentException("Invalid tag");
                tags.add(s);
            }
        }
        String author = json.has("author") ? text(json, "author", 32) : "Unknown";
        return new ModelDefinition(id, base, name, model, List.copyOf(tags), author);
    }
    public boolean matches(String query) {
        String q = query.toLowerCase(java.util.Locale.ROOT);
        return (id + " " + base_item + " " + model + " " + match_name + " " + author + " " + String.join(" ", tags))
                .toLowerCase(java.util.Locale.ROOT).contains(q);
    }
}

package com.simplebuilding.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * How a serialised loot entry spells its item functions - MC 26.2 side (twin in
 * mc26_3/overlay/java). 26.2: an array under "functions", each element naming its type under
 * "function". 26.3: a single object or an inline sequence under "modifier", typed under "type".
 * The loot tests read entries through this, so their assertions stay the same on both lines.
 */
final class LootJsonShape {

    /** The key an item function object names its type under. */
    static final String TYPE_KEY = "function";

    private LootJsonShape() {
    }

    /** The item functions of one entry, in order; empty when it has none. */
    static List<JsonObject> functions(JsonObject entry) {
        List<JsonObject> out = new ArrayList<>();
        JsonElement functions = entry.get("functions");
        if (functions != null && functions.isJsonArray()) {
            for (JsonElement element : functions.getAsJsonArray()) {
                if (element.isJsonObject()) {
                    out.add(element.getAsJsonObject());
                }
            }
        }
        return out;
    }

    /** The type id of one function object, or {@code null}. */
    static String type(JsonObject function) {
        JsonElement id = function.get("function");
        return id != null && id.isJsonPrimitive() ? id.getAsString() : null;
    }
}

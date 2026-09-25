package com.simplebuilding.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/** MC 26.3 side of {@code LootJsonShape}: functions sit under "modifier", typed under "type". */
final class LootJsonShape {

    static final String TYPE_KEY = "type";

    private LootJsonShape() {
    }

    static List<JsonObject> functions(JsonObject entry) {
        List<JsonObject> out = new ArrayList<>();
        collect(entry.get("modifier"), out);
        return out;
    }

    /** A modifier is one typed object, or an inline sequence (a plain array) of them. */
    private static void collect(JsonElement modifier, List<JsonObject> out) {
        if (modifier == null) {
            return;
        }
        if (modifier.isJsonArray()) {
            for (JsonElement element : modifier.getAsJsonArray()) {
                collect(element, out);
            }
        } else if (modifier.isJsonObject()) {
            JsonObject function = modifier.getAsJsonObject();
            // A typed sequence (e.g. when it carries a condition) nests its members.
            if ("minecraft:sequence".equals(type(function))) {
                collect(function.get("functions"), out);
            } else {
                out.add(function);
            }
        }
    }

    static String type(JsonObject function) {
        JsonElement id = function.get("type");
        return id != null && id.isJsonPrimitive() ? id.getAsString() : null;
    }
}

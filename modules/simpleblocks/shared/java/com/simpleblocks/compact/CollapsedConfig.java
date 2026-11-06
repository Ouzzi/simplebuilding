package com.simpleblocks.compact;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;

/**
 * Group {@code collapsed} of {@code config/simpleblocks-server.json}, read and written by {@link com.simpleblocks.snow.SnowConfig}
 * (one file): {@code enabled} (default true) - Collapsed blocks slow their carrier down ({@link CollapsedEffect}). Read on every
 * check, so a change works at once.
 */
public final class CollapsedConfig {
    public static final List<String> KEYS = List.of("collapsed.enabled");
    public static boolean enabled = true;

    private CollapsedConfig() {}

    public static void apply(JsonObject group) {
        JsonElement on = group.get("enabled");
        enabled = on == null || !on.isJsonPrimitive() || !on.getAsJsonPrimitive().isBoolean() || on.getAsBoolean();
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", enabled);
        return o;
    }
}

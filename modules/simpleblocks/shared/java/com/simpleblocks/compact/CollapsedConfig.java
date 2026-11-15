package com.simpleblocks.compact;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;

/**
 * Group {@code collapsed} of {@code config/simpleblocks-server.json}, read and written by {@link com.simpleblocks.snow.SnowConfig}
 * (one file): {@code enabled} (default true) - Collapsed blocks slow their carrier down ({@link CollapsedEffect}); {@code soulSandRush}
 * (default true) - Soul Speed is faster on Dense/Collapsed soul sand ({@link SoulSandRush}, N57-2). Read on every check, so a change works at once.
 */
public final class CollapsedConfig {
    public static final List<String> KEYS = List.of("collapsed.enabled", "collapsed.soulSandRush");
    public static boolean enabled = true;
    public static boolean soulSandRush = true;

    private CollapsedConfig() {}

    public static void apply(JsonObject group) {
        enabled = flag(group.get("enabled"));
        soulSandRush = flag(group.get("soulSandRush"));
    }

    private static boolean flag(JsonElement on) {
        return on == null || !on.isJsonPrimitive() || !on.getAsJsonPrimitive().isBoolean() || on.getAsBoolean();
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", enabled);
        o.addProperty("soulSandRush", soulSandRush);
        return o;
    }
}

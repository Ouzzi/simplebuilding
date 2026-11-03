package com.simplearrows.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simplearrows.SimpleArrows;
import com.simplebuilding.framework.api.ConfigFiles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Server config {@code config/simplearrows-server.json}: {@code silverArrowUndeadBonus} (until 03.11. Simple Ores' option, taken over
 * once by {@link SimpleArrows#init}) and {@code lightArrow}. Missing or broken keys use the defaults, values are clamped (A9) and the
 * file is rewritten complete.
 */
public final class ArrowsConfig {
    public static final String FILE = "simplearrows-server.json";
    /** Every key of the file (wiki and check_data read this list). */
    public static final List<String> KEYS = List.of("silverArrowUndeadBonus", "lightArrow");

    /** Extra damage of a silver arrow against #minecraft:undead, 0 = Vanilla arrow damage (0 to 6). */
    public static int silverArrowUndeadBonus = 3;
    /** A Light Arrow stuck in a block lights the air in front of it (light level 15) until it despawns; off = a plain arrow. */
    public static boolean lightArrow = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void load(Path dir) {
        JsonObject json = new JsonObject();
        Path file = dir == null ? null : dir.resolve(FILE);
        JsonObject read = ConfigFiles.readOrQuarantine(file, t -> GSON.fromJson(t, JsonObject.class), SimpleArrows.LOG::warn);
        if (read != null) json = read;
        apply(json);
        if (file != null) {
            try {
                Files.createDirectories(dir);
                ConfigFiles.writeAtomic(file, GSON.toJson(toJson()));
            } catch (Exception e) {
                SimpleArrows.LOG.warn("Could not write {}: {}", FILE, e.toString());
            }
        }
    }

    public static void apply(JsonObject json) {
        JsonElement v = json.get("silverArrowUndeadBonus");
        double bonus = v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber() ? v.getAsDouble() : 3;
        silverArrowUndeadBonus = Double.isNaN(bonus) ? 3 : (int) Math.round(Math.max(0, Math.min(6, bonus)));
        JsonElement light = json.get("lightArrow");
        lightArrow = light == null || !light.isJsonPrimitive() || !light.getAsJsonPrimitive().isBoolean() || light.getAsBoolean();
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("silverArrowUndeadBonus", silverArrowUndeadBonus);
        o.addProperty("lightArrow", lightArrow);
        return o;
    }

    public static void reset() {
        apply(new JsonObject());
    }

    static {
        reset();
    }

    private ArrowsConfig() {}
}

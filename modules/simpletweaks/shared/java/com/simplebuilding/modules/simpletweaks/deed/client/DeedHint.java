package com.simplebuilding.modules.simpletweaks.deed.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;

/**
 * The deed's "Simple Maps recommended" hint (C12): shown until the player hides it with its x; remembered on this
 * client in {@code config/simpletweaks-client.json} ({@code "deedMapsHint": false}). Unreadable file: the hint shows.
 */
final class DeedHint {
    private static final String FILE = "simpletweaks-client.json", KEY = "deedMapsHint";
    private static Boolean shown;

    private DeedHint() {}

    private static Path file() { return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve(FILE); }

    static boolean shown() {
        if (shown == null) {
            shown = true;
            try {
                Path f = file();
                if (Files.isRegularFile(f) && Files.size(f) < 65536) {
                    var root = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
                    if (root.has(KEY) && root.get(KEY).isJsonPrimitive()) shown = root.get(KEY).getAsBoolean();
                }
            } catch (Exception e) { shown = true; }
        }
        return shown;
    }

    static void hide() {
        shown = false;
        try {
            Path f = file();
            JsonObject root = new JsonObject();
            if (Files.isRegularFile(f) && Files.size(f) < 65536) {
                try { root = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject(); } catch (Exception broken) { root = new JsonObject(); }
            }
            root.addProperty(KEY, false);
            Files.createDirectories(f.getParent());
            Files.writeString(f, root.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) { /* only a convenience: the hint comes back next start */ }
    }
}

package com.simplebuilding.modules.simpletweaks.admin;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * World state of the admin features that survives relogs and restarts: vanished players (and their pickup choice),
 * frozen players, maintenance on/off. One small JSON file, written atomically on every change.
 */
public final class AdminState {
    public final Set<UUID> vanished = new LinkedHashSet<>();
    public final Set<UUID> vanishPickup = new LinkedHashSet<>();
    public final Set<UUID> frozen = new LinkedHashSet<>();
    public boolean maintenance;
    private transient Path file;

    static Path fileOf(MinecraftServer server) { return server.getWorldPath(LevelResource.ROOT).resolve("simpletweaks").resolve("admin-state.json"); }

    static AdminState load(MinecraftServer server) {
        var state = new AdminState();
        state.file = fileOf(server);
        try {
            if (Files.isRegularFile(state.file)) {
                var json = JsonParser.parseString(Files.readString(state.file)).getAsJsonObject();
                read(json, "vanished", state.vanished); read(json, "vanishPickup", state.vanishPickup); read(json, "frozen", state.frozen);
                state.maintenance = json.has("maintenance") && json.get("maintenance").getAsBoolean();
            }
        } catch (Exception e) {
            // REV2-C: keep the unreadable file (the next save would overwrite it with the empty state).
            org.slf4j.LoggerFactory.getLogger("simpletweaks-admin").warn("Could not read {}; starting with an empty admin state, old file kept as {}",
                    state.file, com.simplebuilding.framework.api.ConfigFiles.quarantine(state.file), e);
        }
        return state;
    }
    private static void read(JsonObject json, String key, Set<UUID> into) {
        if (!json.has(key)) return;
        for (var e : json.getAsJsonArray(key)) { try { into.add(UUID.fromString(e.getAsString())); } catch (IllegalArgumentException ignored) {} }
    }

    public void save() {
        if (file == null) return;
        var json = new JsonObject();
        json.add("vanished", array(vanished)); json.add("vanishPickup", array(vanishPickup)); json.add("frozen", array(frozen));
        json.addProperty("maintenance", maintenance);
        try { AdminFiles.atomicWrite(file, new GsonBuilder().setPrettyPrinting().create().toJson(json)); }
        catch (java.io.IOException e) { org.slf4j.LoggerFactory.getLogger("simpletweaks-admin").warn("Could not write {}", file, e); }
    }
    private static JsonArray array(Set<UUID> ids) { var a = new JsonArray(); ids.forEach(id -> a.add(id.toString())); return a; }
}

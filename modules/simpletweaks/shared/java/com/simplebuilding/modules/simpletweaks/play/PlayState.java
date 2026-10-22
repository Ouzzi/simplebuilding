package com.simplebuilding.modules.simpletweaks.play;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * World state of the player features that survives relogs and restarts (one JSON file, written atomically):
 * personal PvP, ended newcomer protections, phantom opt-outs, last seen times and names, login streaks, the pregeneration job.
 * Keys are UUID strings.
 */
public final class PlayState {
    public Set<String> pvpOn = new LinkedHashSet<>();
    public Set<String> pvpOff = new LinkedHashSet<>();
    /** T12: players whose newcomer protection is over for good. */
    public Set<String> shieldEnded = new LinkedHashSet<>();
    public Set<String> phantomsOff = new LinkedHashSet<>();
    public Map<String, Long> lastSeen = new LinkedHashMap<>();
    public Map<String, String> names = new LinkedHashMap<>();
    public Map<String, Streak> streaks = new LinkedHashMap<>();
    public PregenJob pregen;

    public static final class Streak { public String lastDay = ""; public int days; }
    public static final class PregenJob {
        public String dimension = "minecraft:overworld";
        public int centerX, centerZ, radius;
        public long next, done, total;
        public boolean paused;
    }

    private transient Path file;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    static Path fileOf(MinecraftServer server) { return server.getWorldPath(LevelResource.ROOT).resolve("simpletweaks").resolve("play-state.json"); }

    static PlayState load(MinecraftServer server) {
        var file = fileOf(server);
        PlayState state = null;
        try {
            if (Files.isRegularFile(file) && Files.size(file) < 16_000_000) state = GSON.fromJson(Files.readString(file), PlayState.class);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("simpletweaks-play").warn("Could not read {}; starting with an empty state", file, e);
        }
        if (state == null) state = new PlayState();
        state.fill();
        state.file = file;
        return state;
    }
    private void fill() {
        if (pvpOn == null) pvpOn = new LinkedHashSet<>(); if (pvpOff == null) pvpOff = new LinkedHashSet<>();
        if (phantomsOff == null) phantomsOff = new LinkedHashSet<>(); if (lastSeen == null) lastSeen = new LinkedHashMap<>();
        if (shieldEnded == null) shieldEnded = new LinkedHashSet<>();
        if (names == null) names = new LinkedHashMap<>(); if (streaks == null) streaks = new LinkedHashMap<>();
    }

    public void save() {
        if (file == null) return;
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(this));
            try { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING); }
        } catch (java.io.IOException e) {
            org.slf4j.LoggerFactory.getLogger("simpletweaks-play").warn("Could not write {}", file, e);
        }
    }
}

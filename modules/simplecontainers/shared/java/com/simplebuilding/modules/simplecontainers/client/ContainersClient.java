package com.simplebuilding.modules.simplecontainers.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.simplebuilding.modules.simplecontainers.ContainersConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;

/** Loads and saves the local config ({@code config/simplecontainers.json}); a broken file falls back to defaults. */
public final class ContainersClient {
    public static ContainersConfig CONFIG = new ContainersConfig();
    private static boolean loaded;

    private ContainersClient() {}

    private static Path path() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config/simplecontainers.json");
    }

    public static ContainersConfig config() {
        if (!loaded) load();
        return CONFIG;
    }

    public static void load() {
        loaded = true;
        try {
            if (Files.isRegularFile(path()) && Files.size(path()) <= 65536) {
                ContainersConfig c = new Gson().fromJson(Files.readString(path()), ContainersConfig.class);
                if (c != null) CONFIG = c;
            }
        } catch (IOException | RuntimeException ignored) {
            CONFIG = new ContainersConfig();
        }
        CONFIG.normalize();
    }

    public static void save() {
        CONFIG.normalize();
        try {
            Files.createDirectories(path().getParent());
            Files.writeString(path(), new GsonBuilder().setPrettyPrinting().create().toJson(CONFIG));
        } catch (IOException e) {
            org.slf4j.LoggerFactory.getLogger("simplecontainers").warn("Cannot save local container settings", e);
        }
    }
}

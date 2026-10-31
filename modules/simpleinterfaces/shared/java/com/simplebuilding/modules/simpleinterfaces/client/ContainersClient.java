package com.simplebuilding.modules.simpleinterfaces.client;

import com.google.gson.Gson;
import com.simplebuilding.framework.api.ConfigFiles;
import com.google.gson.GsonBuilder;
import com.simplebuilding.modules.simpleinterfaces.ContainersConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;

/** Loads and saves the local config ({@code config/simpleinterfaces.json}); a broken file falls back to defaults. */
public final class ContainersClient {
    public static ContainersConfig CONFIG = new ContainersConfig();
    private static boolean loaded;

    private ContainersClient() {}

    private static Path path() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config/simpleinterfaces.json");
    }

    public static ContainersConfig config() {
        if (!loaded) load();
        return CONFIG;
    }

    public static void load() {
        loaded = true;
        ContainersConfig c = null;
        try {
            // a broken file is moved aside (not overwritten by the next save); too big = ignored
            if (Files.isRegularFile(path()) && Files.size(path()) <= 65536) {
                c = ConfigFiles.readOrQuarantine(path(), text -> new Gson().fromJson(text, ContainersConfig.class),
                        message -> org.slf4j.LoggerFactory.getLogger("simpleinterfaces").warn(message));
            }
        } catch (IOException ignored) {
            // unreadable: defaults
        }
        CONFIG = c != null ? c : new ContainersConfig();
        CONFIG.normalize();
    }

    public static void save() {
        CONFIG.normalize();
        try {
            Files.createDirectories(path().getParent());
            ConfigFiles.writeAtomic(path(), new GsonBuilder().setPrettyPrinting().create().toJson(CONFIG));
        } catch (IOException e) {
            org.slf4j.LoggerFactory.getLogger("simpleinterfaces").warn("Cannot save local container settings", e);
        }
    }
}

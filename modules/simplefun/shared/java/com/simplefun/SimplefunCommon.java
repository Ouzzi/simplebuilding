package com.simplefun;

import com.google.gson.GsonBuilder;
import com.simplefun.config.SimplefunConfig;
import java.nio.file.*;

public final class SimplefunCommon {
  private static SimplefunConfig config;

  public static void init() {
    Constants.LOG.info("Initializing Simple Fun 26.3");
  }

  public static synchronized void registerConfig() {
    if (config != null) return;
    var p = Path.of("config/simplefun.json");
    var gson = new GsonBuilder().setPrettyPrinting().create();
    config =
        com.simplebuilding.framework.api.ConfigFiles.readOrQuarantine(
            p, SimplefunConfig.class, gson, Constants.LOG::warn);
    if (config == null) config = new SimplefunConfig();
    config.normalize();
    if (!Files.exists(p)) saveConfig();
  }

  public static SimplefunConfig getConfig() {
    registerConfig();
    config.normalize();
    return config;
  }

  public static void saveConfig() {
    try {
      config.normalize();
      Files.createDirectories(Path.of("config"));
      Files.writeString(
          Path.of("config/simplefun.json"),
          new GsonBuilder().setPrettyPrinting().create().toJson(config));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}

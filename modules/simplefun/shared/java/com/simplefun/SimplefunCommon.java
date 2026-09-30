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
    try {
      config =
          Files.exists(p)
              ? gson.fromJson(Files.readString(p), SimplefunConfig.class)
              : new SimplefunConfig();
      if (config == null) config = new SimplefunConfig();
      config.normalize();
      if (!Files.exists(p)) saveConfig();
    } catch (Exception e) {
      throw new IllegalStateException("Invalid Simple Fun server config", e);
    }
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

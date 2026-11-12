package com.simplefun;

import com.google.gson.GsonBuilder;
import com.simplefun.config.SimplefunConfig;
import java.nio.file.*;

public final class SimplefunCommon {
  private static SimplefunConfig config;

  /** Config folder of the loader (REV2-A-9: never the working directory); every loader sets it before {@link #registerConfig}. */
  public static volatile Path configDir = Path.of("config");

  public static Path configFile() {
    return configDir.resolve("simplefun.json");
  }

  /** Super-mod switch "Enable Simple Fun" (framework SubMods): off = items stay registered but inert, no tabs, no loot. */
  public static boolean on() {
    return com.simplebuilding.framework.api.SubMods.enabled("simplefun");
  }

  /**
   * AUDSB31E1 F11: set by the loader at start-up - whether the Knockback data override (built-in data pack
   * {@code simplefun:higher_knockback}: max level 4, feathers/sticks/melee weapons) was added to this session.
   */
  public static volatile boolean higherKnockbackData;

  /**
   * AUDSB31E1 F11: the Vanilla {@code minecraft:enchantment/knockback} override exists only while Simple Fun is on and
   * {@code enableHigherKnockback} is on. Data registries load once per world start, so this is read at start-up.
   * (A load condition on the override file would delete the Vanilla enchantment when false: the override is the top file.)
   */
  public static boolean wantsHigherKnockbackData() {
    return on() && getConfig().fun.enableHigherKnockback;
  }

  public static void init() {
    Constants.LOG.info("Initializing Simple Fun 26.3");
  }

  public static synchronized void registerConfig() {
    if (config != null) return;
    var p = configFile();
    var gson = new GsonBuilder().setPrettyPrinting().create();
    config =
        com.simplebuilding.framework.api.ConfigFiles.readOrQuarantine(
            p, t -> gson.fromJson(t, SimplefunConfig.class), Constants.LOG::warn);
    if (config == null) config = new SimplefunConfig();
    config.normalize();
    if (!Files.exists(p)) saveConfig();
  }

  public static SimplefunConfig getConfig() {
    registerConfig();
    config.normalize();
    return config;
  }

  /** CMD24: the config command swaps in a new, normalised object. */
  public static synchronized void replaceConfig(SimplefunConfig c) {
    c.normalize();
    config = c;
  }

  public static void saveConfig() {
    try {
      config.normalize();
      Files.createDirectories(configDir);
      // Atomic like every other module config (REV24-N-4): a crash mid-write must not leave half a file.
      com.simplebuilding.framework.api.ConfigFiles.writeAtomic(
          configFile(),
          new GsonBuilder().setPrettyPrinting().create().toJson(config));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}

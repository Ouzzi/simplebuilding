package com.simplebuilding.modules.simplelayers;

import com.simplebuilding.modules.simplelayers.config.LayersConfig;
import com.simplebuilding.modules.simplelayers.world.gen.BackroomsLevel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Layers (IG31-56, owner 08.11.: "Bitte Konzept für die Wolken und Backrooms implementieren"; concept
 * docs/ai/KONZEPT-A-BIOMES09-2026-11-08.md, plan docs/ai/PLAN-A-LAYERS10-2026-11-08.md): two world layers as own dimensions -
 * the Cloud Sea above y 700 of the Overworld and the Backrooms below the bedrock. The loader entry calls {@link #init} once.
 */
public final class SimpleLayers {
    public static final String MOD_ID = "simplelayers";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Layers");
    /** The sky dimension (concept §1.2 variant B). */
    public static final ResourceKey<Level> CLOUD_SEA = ResourceKey.create(Registries.DIMENSION, id("cloud_sea"));
    /** The Backrooms dimension (concept §2.2 variant B). */
    public static final ResourceKey<Level> BACKROOMS = ResourceKey.create(Registries.DIMENSION, id("backrooms"));
    /** BACK18: every Backrooms level is its own dimension (Level 0 = {@link #BACKROOMS}), in {@link BackroomsLevel} order. */
    private static final java.util.Map<BackroomsLevel, ResourceKey<Level>> LEVEL_KEYS = new java.util.EnumMap<>(BackroomsLevel.class);

    static {
        for (BackroomsLevel l : BackroomsLevel.values()) LEVEL_KEYS.put(l, l == BackroomsLevel.LOBBY ? BACKROOMS : ResourceKey.create(Registries.DIMENSION, id(l.dimension)));
    }

    private SimpleLayers() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Super-mod switch "Enable Simple Layers" (framework SubMods, set by SimpleBuilding): off = no tab entries, no travel. */
    public static boolean on() {
        return com.simplebuilding.framework.api.SubMods.enabled(MOD_ID);
    }

    public static boolean cloudsOn() {
        return on() && LayersConfig.cloudsEnabled;
    }

    public static boolean backroomsOn() {
        return on() && LayersConfig.backroomsEnabled;
    }

    public static ResourceKey<Level> levelKey(BackroomsLevel level) {
        return LEVEL_KEYS.get(level);
    }

    /** The Backrooms level of a dimension, or null outside the Backrooms. */
    public static BackroomsLevel levelOf(ResourceKey<Level> dimension) {
        return SimpleLayers.MOD_ID.equals(dimension.identifier().getNamespace()) ? BackroomsLevel.byDimension(dimension.identifier().getPath()) : null;
    }

    public static BackroomsLevel levelOf(Level level) {
        return levelOf(level.dimension());
    }

    /** Whether the level is one of the Backrooms levels (BACK18: protection, effect, sounds hold in all of them). */
    public static boolean inBackrooms(Level level) {
        return levelOf(level) != null;
    }

    /** Config, config command and test centre station; before the registrations. */
    public static void init(Path configDir) {
        LayersConfig.load(configDir);
        com.simplelib.api.config.ConfigCommands.server(MOD_ID).json(LayersConfig::toJson, LayersConfig::apply)
                .file(configDir, LayersConfig.FILE).register();
        List<String> items = new ArrayList<>();
        for (String path : LayersRegistry.ITEM_PATHS) items.add(MOD_ID + ":" + path);
        com.simplebuilding.framework.api.TestCentreStations.register(MOD_ID, "layers", "simplelayers.testcentre.station", items);
    }
}

package com.simplebuilding.modules.simplelayers;

import com.simplebuilding.modules.simplelayers.config.LayersConfig;
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
    /**
     * BACK22: the stacked Backrooms - ONE dimension with ten levels of 96 blocks, a full bedrock section between them (concept
     * docs/ai/KONZEPT-B-BACK22-KERN-2026-11-10.md). The five BACK18 dimensions are gone (no release yet, owner 6).
     */
    public static final ResourceKey<Level> STACK = ResourceKey.create(Registries.DIMENSION, id("backrooms_stack"));

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

    /** The level (1..10) a player is on, or 0 outside the stack (or inside its bedrock). */
    public static int bandOf(Level level, double y) {
        return inBackrooms(level) ? com.simplebuilding.modules.simplelayers.backrooms.stack.BandStack.bandAt((int) Math.floor(y)) : 0;
    }

    /** Whether the level is the stacked Backrooms (protection, effect, sounds). */
    public static boolean inBackrooms(Level level) {
        return level.dimension() == STACK;
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

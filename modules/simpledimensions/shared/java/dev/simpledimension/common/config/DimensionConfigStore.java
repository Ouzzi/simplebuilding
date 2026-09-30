package dev.simpledimension.common.config;

import com.google.gson.Gson;
import com.simplebuilding.modules.simpledimensions.ConfigLimits;
import com.google.gson.GsonBuilder;
import dev.simpledimension.common.portal.DimensionPortalConfig;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared, loader-agnostic config loading + datapack generation.
 *
 * <p>Reads every {@code <id>.json} under {@code <configRoot>/dimensions}, writing
 * the default examples on first run, and regenerates a datapack under
 * {@code <configRoot>/generated_datapack} that turns the configured terrain into
 * real dimension + dimension_type files. Loaders are responsible for making the
 * game read that datapack (NeoForge does so automatically).
 */
public final class DimensionConfigStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private DimensionConfigStore() {
    }

    public static List<DimensionPortalConfig> loadAndGenerate(Path configRoot, int dataPackFormat) {
        Path dimensionsDir = configRoot.resolve("dimensions");
        try {
            Files.createDirectories(dimensionsDir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create config folder " + dimensionsDir, e);
        }

        ensureExample(dimensionsDir.resolve("skyblock.json"), DimensionPortalConfig.defaultSkyblock());
        ensureExample(dimensionsDir.resolve("mining.json"), DimensionPortalConfig.miningDimensionPreset());
        ensureExample(dimensionsDir.resolve("travel.json"), DimensionPortalConfig.travelDimensionPreset());

        List<DimensionPortalConfig> configs = new ArrayList<>();
        try (var stream = Files.list(dimensionsDir)) {
            stream.filter(path -> path.toString().endsWith(".json")).sorted().limit(ConfigLimits.MAX_DEFINITIONS).forEach(path -> {
                DimensionPortalConfig config = read(path);
                if (config != null) {
                    configs.add(config);
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException("Could not read dimension configs from " + dimensionsDir, e);
        }

        generateDatapack(generatedDatapackRoot(configRoot), configs, dataPackFormat);
        return configs;
    }

    /**
     * Folder that the loader should register as a pack <em>source</em>. It
     * contains exactly one pack ({@code simpledimension_generated}) so a folder
     * repository source picks up only our generated datapack.
     */
    public static Path datapacksFolder(Path configRoot) {
        return configRoot.resolve("datapacks");
    }

    /** Absolute path of the generated datapack root (where pack.mcmeta lives). */
    public static Path generatedDatapackRoot(Path configRoot) {
        return datapacksFolder(configRoot).resolve("simpledimension_generated");
    }

    private static DimensionPortalConfig read(Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            if (Files.isSymbolicLink(path) || Files.size(path)>ConfigLimits.MAX_BYTES) return null;
            return ConfigLimits.validate(GSON.fromJson(reader, DimensionPortalConfig.class));
        } catch (Exception error) {
            System.err.println("Simple Dimensions rejected " + path.getFileName() + ": " + error.getMessage());
            return null;
        }
    }

    private static void write(Path path, DimensionPortalConfig config) {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            throw new IllegalStateException("Could not write config " + path, e);
        }
    }

    private static void ensureExample(Path path, DimensionPortalConfig config) {
        if (!Files.exists(path)) {
            write(path, config);
        }
    }

    // ---- datapack generation ----------------------------------------------------

    private static void generateDatapack(Path packRoot, List<DimensionPortalConfig> configs, int dataPackFormat) {
        Path dataRoot = packRoot.resolve("data").resolve("simpledimension");
        Path dimensionDir = dataRoot.resolve("dimension");
        Path dimensionTypeDir = dataRoot.resolve("dimension_type");
        try {
            Files.createDirectories(dimensionDir);
            Files.createDirectories(dimensionTypeDir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create generated datapack folders", e);
        }

        writeJson(packRoot.resolve("pack.mcmeta"), packMeta(dataPackFormat));
        for (DimensionPortalConfig cfg : configs) {
            String id = safeId(cfg.id);
            writeJson(dimensionTypeDir.resolve(id + "_type.json"), toDimensionTypeJson(cfg));
            writeJson(dimensionDir.resolve(id + ".json"), toDimensionJson(cfg));
        }
    }

    private static Map<String, Object> packMeta(int dataPackFormat) {
        int format = dataPackFormat > 0 ? dataPackFormat : 88;
        Map<String, Object> pack = new LinkedHashMap<>();
        pack.put("description", "SimpleDimension generated dimensions");
        pack.put("pack_format", format);
        Map<String, Object> supported = new LinkedHashMap<>();
        supported.put("min_inclusive", 0);
        supported.put("max_inclusive", Math.max(format, 999));
        pack.put("min_format", format);
        pack.put("max_format", format);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("pack", pack);
        return out;
    }

    private static Map<String, Object> toDimensionTypeJson(DimensionPortalConfig cfg) {
        DimensionPortalConfig.WorldGenerationConfig gen = generationOf(cfg);
        // Minecraft requires min_y and height to be multiples of 16 in legal ranges with
        // min_y + height <= 2032, or the dimension_type is rejected and world load aborts.
        int minY = clampMinY(gen.minY);
        int height = clampHeight(gen.height, minY);
        int logicalHeight = Math.max(0, Math.min(gen.logicalHeight, height));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ultrawarm", gen.ultraWarm);
        out.put("natural", gen.natural);
        out.put("coordinate_scale", gen.coordinateScale);
        out.put("has_skylight", gen.hasSkylight);
        out.put("has_ceiling", gen.hasCeiling);
        out.put("ambient_light", 0.0);
        out.put("piglin_safe", false);
        out.put("bed_works", true);
        out.put("respawn_anchor_works", false);
        out.put("has_ender_dragon_fight", false);
        out.put("has_raids", true);
        out.put("logical_height", logicalHeight);
        out.put("min_y", minY);
        out.put("height", height);
        out.put("infiniburn", "#minecraft:infiniburn_overworld");
        out.put("skybox", "overworld");
        out.put("cardinal_light", "default");
        out.put("attributes", Map.of("minecraft:gameplay/water_evaporates",gen.ultraWarm,
                "minecraft:gameplay/fast_lava",gen.ultraWarm,
                "minecraft:gameplay/bed_rule",Map.of("can_set_spawn","never","can_sleep","never"),
                "minecraft:gameplay/respawn_anchor_works",false));
        if(gen.natural){out.put("default_clock","minecraft:overworld");out.put("timelines","#minecraft:in_overworld");}
        out.put("monster_spawn_light_level", 0);
        out.put("monster_spawn_block_light_limit", 0);
        return out;
    }

    private static int snapTo16(int value) {
        return Math.floorDiv(value + 8, 16) * 16;
    }

    private static int clampMinY(int rawMinY) {
        int minY = snapTo16(rawMinY);
        if (minY < -2032) {
            minY = -2032;
        }
        if (minY > 2016) {
            minY = 2016; // leave room for at least one 16-tall section
        }
        return minY;
    }

    private static int clampHeight(int rawHeight, int minY) {
        int height = snapTo16(rawHeight);
        if (height < 16) {
            height = 16;
        }
        int maxHeight = 2032 - minY;     // keep min_y + height <= 2032
        if (maxHeight > 4064) {
            maxHeight = 4064;
        }
        if (height > maxHeight) {
            height = Math.floorDiv(maxHeight, 16) * 16;
        }
        return Math.max(16, height);
    }

    private static Map<String, Object> toDimensionJson(DimensionPortalConfig cfg) {
        DimensionPortalConfig.WorldGenerationConfig gen = generationOf(cfg);
        String id = safeId(cfg.id);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("type", "simpledimension:" + id + "_type");

        Map<String, Object> generator = new LinkedHashMap<>();
        if ("noise".equalsIgnoreCase(gen.generatorType)) {
            generator.put("type", "minecraft:noise");
            Map<String, Object> biomeSource = new LinkedHashMap<>();
            if (gen.biomeSelection != null && gen.biomeSelection.size() > 1) {
                // Restrict generation to exactly the configured biomes. A checkerboard
                // source honours the list (the multi_noise overworld preset ignored it).
                biomeSource.put("type", "minecraft:checkerboard");
                biomeSource.put("scale", 2);
                biomeSource.put("biomes", new ArrayList<>(gen.biomeSelection));
            } else {
                biomeSource.put("type", "minecraft:fixed");
                biomeSource.put("biome", gen.baseBiome);
            }
            generator.put("biome_source", biomeSource);
            generator.put("settings", gen.generateOres ? "minecraft:overworld" : "minecraft:caves");
        } else {
            generator.put("type", "minecraft:flat");
            Map<String, Object> settings = new LinkedHashMap<>();
            settings.put("biome", gen.baseBiome);
            settings.put("features", gen.generateOres);
            settings.put("lakes", false);
            List<Map<String, Object>> layers = new ArrayList<>();
            if (gen.flatLayers != null) {
                for (DimensionPortalConfig.FlatLayer layer : gen.flatLayers) {
                    Map<String, Object> layerJson = new LinkedHashMap<>();
                    layerJson.put("block", layer.block);
                    layerJson.put("height", layer.height);
                    layers.add(layerJson);
                }
            }
            settings.put("layers", layers);
            settings.put("structure_overrides", List.of());
            generator.put("settings", settings);
        }

        out.put("generator", generator);
        return out;
    }

    private static DimensionPortalConfig.WorldGenerationConfig generationOf(DimensionPortalConfig cfg) {
        return cfg.worldGeneration == null
                ? DimensionPortalConfig.WorldGenerationConfig.skyblockPreset()
                : cfg.worldGeneration;
    }

    private static String safeId(String id) {
        if (id == null || !id.matches("[a-z0-9_]{1,48}")) throw new IllegalArgumentException("Invalid output path");
        return id;
    }

    private static void writeJson(Path path, Map<String, Object> content) {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(content, writer);
        } catch (IOException e) {
            throw new IllegalStateException("Could not write generated datapack file " + path, e);
        }
    }
}

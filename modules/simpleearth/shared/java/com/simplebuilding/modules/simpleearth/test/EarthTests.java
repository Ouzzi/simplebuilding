package com.simplebuilding.modules.simpleearth.test;

import com.simplebuilding.modules.simpleearth.SimpleEarth;
import com.simplebuilding.modules.simpleearth.config.EarthClientConfig;
import com.simplebuilding.modules.simpleearth.config.EarthServerConfig;
import com.simplebuilding.modules.simpleearth.config.EarthSettings;
import com.simplebuilding.modules.simpleearth.net.OrbitMovePayload;
import com.simplebuilding.modules.simpleearth.net.TileRequestPayload;
import com.simplebuilding.modules.simpleearth.orbit.OrbitTracker;
import com.simplebuilding.modules.simpleearth.terrain.EarthColors;
import com.simplebuilding.modules.simpleearth.terrain.TerrainSampler;
import com.simplebuilding.modules.simpleearth.terrain.TileCodec;
import com.simplebuilding.modules.simpleearth.terrain.TileMath;
import com.simplebuilding.modules.simpleearth.terrain.TileService;
import com.simplebuilding.modules.simpleearth.terrain.TileStore;
import com.simplebuilding.modules.simpleearth.view.EarthView;
import com.simplebuilding.modules.simpleearth.view.Scene;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

/** Server-side checks of Simple Earth (bodies for the Fabric adapter {@code ModuleGameTest}, all on simpleearth:empty). */
public final class EarthTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();

    private EarthTests() {}

    static {
        ALL.put("config_command", h -> {
            var spec = com.simplelib.api.config.ConfigCommands.spec(SimpleEarth.MOD_ID, false);
            h.assertTrue(spec != null && spec.options().size() >= EarthServerConfig.KEYS.size() - 1, "/simpleearth config not announced");
            com.simplelib.test.ConfigCommandTests.roundTrip(h, SimpleEarth.MOD_ID, "tilesPerSecond", "40", "40", "x");
            h.assertTrue(spec.set("tilesPerSecond", "9999").status() == com.simplelib.api.config.ConfigCommands.Status.CLAMPED
                    && EarthServerConfig.tilesPerSecond == 512, "clamped to 512");
            spec.reset("tilesPerSecond");
            h.assertTrue(EarthServerConfig.tilesPerSecond == 24, "reset");
            h.succeed();
        });

        ALL.put("config_defaults", h -> {
            EarthServerConfig.reset();
            h.assertTrue(EarthServerConfig.enabled && !EarthServerConfig.exploredOnly && EarthServerConfig.showOthers, "E8/E9 defaults");
            h.assertTrue(EarthServerConfig.dimensions.equals(List.of("minecraft:overworld")), "E7: Overworld only");
            h.assertTrue(EarthServerConfig.activationY(100, 320) == 1024 && EarthServerConfig.activationY(2000, 320) == 2000,
                    "activation floor 1024 (E11), wishes above it kept");
            h.assertTrue(EarthServerConfig.activationY(500, 1200) == 1264, "never below build height + 64");
            h.assertTrue(EarthServerConfig.switchY(1000, 1024) == 1152, "switch at least 128 above activation");
            h.assertTrue(EarthServerConfig.edge(1) == 8192 && EarthServerConfig.edge(9_999_999) == 262144, "edge 8192..262144 (E1)");
            h.assertTrue(EarthServerConfig.speed(9_999_999) == 32768 && EarthServerConfig.maxOrbitSpeed == 32768, "orbit speed cap");
            h.assertTrue(EarthClientConfig.version == EarthSettings.DEFAULT_VERSION && EarthSettings.DEFAULT_VERSION == 6, "standard V6");
            h.assertTrue(EarthSettings.VERSIONS == 12, "V1-V12 (WD25-17)");
            h.assertTrue(EarthServerConfig.minGrid == 5 && EarthServerConfig.biomeVotes == 2 && EarthServerConfig.surfaceDetail
                    && !EarthServerConfig.globalGrid && !EarthServerConfig.requireFlight && EarthServerConfig.reentryAtOrbit
                    && EarthServerConfig.slowFallSeconds == 30 && EarthServerConfig.fastHeights, "WD25-16 server defaults (E4 per player, E5 any flight, E6 orbit position)");
            for (int v = 1; v <= EarthSettings.VERSIONS; v++) {
                h.assertTrue(EarthSettings.find(v, "activationY").def() == 1024 && EarthSettings.find(v, "switchY").def() == 1536
                        && EarthSettings.find(v, "ceilingY").def() == 6144 && EarthSettings.find(v, "edge").def() == 65536
                        && EarthSettings.find(v, "orbitSpeed").def() == 256, "E1/E2/E11 defaults in V" + v);
            }
            h.assertTrue(EarthSettings.find(1, "edge").clamp(1e9) == 262144 && EarthSettings.find(6, "foldStart").clamp(-3) == 0, "client clamps");
            h.succeed();
        });

        ALL.put("render_config", h -> {
            com.google.gson.JsonObject saved = EarthClientConfig.toJson();
            try {
                EarthClientConfig.apply(new com.google.gson.JsonObject());
                h.assertTrue(EarthClientConfig.render("detail") == 3 && EarthClientConfig.render("detailRange") == 0
                        && EarthClientConfig.render("renderDistance") == 2 && EarthClientConfig.render("paintBudget") == 192
                        && EarthClientConfig.render("textureCache") == 64 && EarthClientConfig.render("tileCache") == 2048
                        && Math.abs(EarthClientConfig.render("faceEdge") - 0.28) < 1e-9, "display defaults");
                h.assertTrue(EarthClientConfig.renderOn("moonPhases") && EarthClientConfig.renderOn("showOthers"), "E9/E10 on by default");
                var json = com.google.gson.JsonParser.parseString("{\"version\": 12, \"render\": {\"detail\": 99, \"moonPhases\": false,"
                        + " \"faceEdge\": \"x\", \"renderDistance\": 1}, \"versions\": {\"V11\": {\"cells\": 40}}}").getAsJsonObject();
                EarthClientConfig.apply(json);
                h.assertTrue(EarthClientConfig.version == 12 && EarthClientConfig.render("detail") == 5 && !EarthClientConfig.renderOn("moonPhases")
                        && Math.abs(EarthClientConfig.render("faceEdge") - 0.28) < 1e-9 && EarthClientConfig.render("renderDistance") == 1,
                        "clamped, toggles as booleans, broken value = default");
                h.assertTrue(EarthClientConfig.get(11, "cells") == 8, "V11 cells clamped to 8");
                var out = EarthClientConfig.toJson();
                h.assertTrue(out.getAsJsonObject("render").get("moonPhases").getAsJsonPrimitive().isBoolean(), "toggle written as true/false");
                EarthClientConfig.apply(out);
                h.assertTrue(EarthClientConfig.toJson().equals(out), "JSON round trip");
                EarthClientConfig.apply(com.google.gson.JsonParser.parseString("{\"version\": 13}").getAsJsonObject());
                h.assertTrue(EarthClientConfig.version == 6, "unknown version -> V6");
                h.assertTrue(EarthSettings.find(10, "tipTime").def() == 0.6 && EarthSettings.find(10, "rounding").def() == 0.02
                        && EarthSettings.find(1, "rounding").def() == 0.07 && EarthSettings.find(11, "cells").def() == 3
                        && EarthSettings.find(12, "cornerTilt").def() == 1 && EarthSettings.find(12, "tiltStart") != null, "V10-V12 own values");
                // detail level: 2 = the old 256 px on the own tile, standard 3 = 512 near, halved per ring outside detailRange
                int[] old = TileMath.ringResolutions(65536, 0.3, 256, 2, 0, 5), std = TileMath.ringResolutions(65536, 0.3, 256, 3, 0, 5);
                h.assertTrue(old[0] == 256 && std[0] == 512 && std[1] == 256 && std[2] == 128, "rings " + Arrays.toString(std));
                int[] ranged = TileMath.ringResolutions(65536, 0.3, 256, 3, 1, 5), top = TileMath.ringResolutions(65536, 0.3, 256, 5, 2, 5);
                h.assertTrue(ranged[1] == 512 && top[0] == TileMath.MAX_RES && top[2] == TileMath.MAX_RES, "range and cap");
                int[] small = TileMath.ringResolutions(8192, 0.3, 256, 5, 0, 5);
                h.assertTrue(small[0] == 8192 >> 5, "never finer than the server grid: " + small[0]);
                h.assertTrue(TileMath.ringResolutions(65536, 3, 256, 3, 0, 5)[0] == 128, "high above: coarser");
            } finally {
                EarthClientConfig.apply(saved);
            }
            h.succeed();
        });

        ALL.put("orbit_options", h -> {
            ServerPlayer player = h.makeMockServerPlayerInLevel();
            try {
                EarthServerConfig.reset();
                EarthServerConfig.globalGrid = true;
                OrbitTracker.forget(player.getUUID());
                OrbitTracker.State s = OrbitTracker.state(player);
                s.configure(1024, 1536, 65536, 256, h.getLevel().getMaxY());
                player.setPos(40000, 1100, -20000);
                OrbitTracker.tick(player, true);
                h.assertTrue(s.active && s.ax == 65536 && s.az == 0 && s.ox == 40000, "E4 global grid: centre " + s.ax + "/" + s.az);
                h.assertTrue(OrbitTracker.centre(40000, 65536) == 65536, "grid point");
                EarthServerConfig.globalGrid = false;
                h.assertTrue(OrbitTracker.centre(40000, 65536) == 40000, "per player");
                // E5: only while flying
                EarthServerConfig.requireFlight = true;
                OrbitTracker.forget(player.getUUID());
                s = OrbitTracker.state(player);
                player.getAbilities().flying = false;
                player.setPos(10, 1100, 10);
                OrbitTracker.tick(player, true);
                h.assertTrue(!s.active, "E5: not flying = no orbit");
                player.getAbilities().flying = true;
                OrbitTracker.tick(player, true);
                h.assertTrue(s.active, "E5: flying starts the orbit");
                EarthServerConfig.requireFlight = false;
                // E6 off: back to the park point
                EarthServerConfig.reentryAtOrbit = false;
                player.setPos(12, 1600, 14);
                OrbitTracker.tick(player, true);
                h.assertTrue(s.parked, "parked");
                OrbitTracker.handleMove(player, new OrbitMovePayload(10, 0));
                player.setPos(12, 1500, 14);
                OrbitTracker.tick(player, true);
                h.assertTrue(!s.parked && Math.abs(player.getX() - 12) < 1e-6 && Math.abs(player.getZ() - 14) < 1e-6,
                        "E6 off: re-entry at the park point " + player.getX());
            } finally {
                EarthServerConfig.reset();
                OrbitTracker.forget(player.getUUID());
            }
            h.succeed();
        });

        ALL.put("biome_table", h -> {
            h.assertTrue(EarthColors.NAMES.length == 62, "62 map colours");
            for (int i = 0; i < EarthColors.NAMES.length; i++) {
                h.assertTrue(EarthColors.byName(new com.google.gson.JsonPrimitive(EarthColors.NAMES[i])) == MapColor.byId(i), "name " + EarthColors.NAMES[i]);
            }
            h.assertTrue(EarthColors.byName(new com.google.gson.JsonPrimitive("GLOW_LICHEN")) == MapColor.GLOW_LICHEN
                    && EarthColors.byName(new com.google.gson.JsonPrimitive("deepslate")) == MapColor.DEEPSLATE
                    && EarthColors.byName(new com.google.gson.JsonPrimitive("terracotta_orange")) == MapColor.TERRACOTTA_ORANGE
                    && EarthColors.byName(new com.google.gson.JsonPrimitive(12)) == MapColor.WATER
                    && EarthColors.byName(new com.google.gson.JsonPrimitive("rainbow")) == null, "names and ids");
            // the shipped table (data/simpleearth/simpleearth/biome_colors.json), loaded at server start
            EarthColors.Table shipped = EarthColors.table();
            h.assertTrue(shipped.biomes().get("simpleend:ash_valley") == MapColor.COLOR_LIGHT_GRAY && shipped.biomes().size() >= 4
                    && shipped.tags().size() >= 20, "shipped table: " + shipped.biomes().size() + " biomes, " + shipped.tags().size() + " tags");
            var biomes = h.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
            ResourceKey<Biome> ash = ResourceKey.create(Registries.BIOME, net.minecraft.resources.Identifier.fromNamespaceAndPath("simpleend", "ash_valley"));
            var ashHolder = biomes.get(ash);
            if (ashHolder.isPresent()) h.assertTrue(EarthColors.color(ashHolder.get(), false) == MapColor.COLOR_LIGHT_GRAY, "Simple End ash valley");
            try {
                var a = com.google.gson.JsonParser.parseString("{\"biomes\": {\"minecraft:plains\": \"color_red\", \"minecraft:desert\": \"rainbow\"},"
                        + " \"tags\": [{\"tag\": \"minecraft:is_forest\", \"color\": \"color_blue\"}]}").getAsJsonObject();
                var b = com.google.gson.JsonParser.parseString("{\"biomes\": {\"minecraft:plains\": \"color_yellow\"},"
                        + " \"tags\": [{\"tag\": \"#minecraft:is_forest\", \"color\": 25}]}").getAsJsonObject();
                EarthColors.Table t = EarthColors.parse(List.of(a, b));
                h.assertTrue(t.biomes().get("minecraft:plains") == MapColor.COLOR_YELLOW && !t.biomes().containsKey("minecraft:desert"),
                        "later file wins, unknown colour skipped");
                h.assertTrue(t.tag(biomes.getOrThrow(Biomes.FOREST)) == MapColor.COLOR_BLUE && t.tag(biomes.getOrThrow(Biomes.PLAINS)) == null,
                        "tag rules, later file first");
                h.assertTrue(!t.signature().equals(shipped.signature()), "signature follows the content");
                EarthColors.use(t);
                h.assertTrue(EarthColors.color(biomes.getOrThrow(Biomes.PLAINS), false) == MapColor.COLOR_YELLOW, "exact entry beats the built-in rule");
                h.assertTrue(EarthColors.color(biomes.getOrThrow(Biomes.PLAINS), true) == MapColor.WATER, "below sea level still water");
                h.assertTrue(EarthColors.color(biomes.getOrThrow(Biomes.FOREST), false) == MapColor.PLANT, "Vanilla biomes keep their rules over tags");
                h.assertTrue(EarthColors.nearest(0x79C05A) == MapColor.GRASS && EarthColors.nearest(0xDBD3A0) == MapColor.SAND, "grass colour fallback");
            } finally {
                EarthColors.use(shipped);
            }
            h.assertTrue(EarthColors.color(biomes.getOrThrow(Biomes.PLAINS), false) == MapColor.GRASS, "table restored");
            h.succeed();
        });

        ALL.put("surface_rules", h -> {
            var biomes = h.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
            var plains = biomes.getOrThrow(Biomes.PLAINS);
            int sea = 63;
            h.assertTrue(EarthColors.surface(MapColor.GRASS, plains, 0, 70, 0, sea, 0) == MapColor.GRASS, "plains low: grass");
            h.assertTrue(EarthColors.surface(MapColor.GRASS, plains, 0, 900, 0, sea, 0) == MapColor.SNOW, "plains at 900: snow (height temperature)");
            h.assertTrue(EarthColors.surface(MapColor.GRASS, plains, 0, 90, 0, sea, 2) == MapColor.STONE, "very steep: stone");
            h.assertTrue(EarthColors.surface(MapColor.SAND, biomes.getOrThrow(Biomes.DESERT), 0, 900, 0, sea, 0) == MapColor.SAND, "desert never snows");
            var badlands = biomes.getOrThrow(Biomes.BADLANDS);
            h.assertTrue(EarthColors.surface(MapColor.TERRACOTTA_ORANGE, badlands, 0, 70, 0, sea, 0) == MapColor.COLOR_ORANGE, "badlands: red sand");
            h.assertTrue(EarthColors.surface(MapColor.TERRACOTTA_ORANGE, badlands, 0, 90, 0, sea, 1) == EarthColors.band(90), "badlands slope: band");
            h.assertTrue(EarthColors.surface(MapColor.WATER, biomes.getOrThrow(Biomes.SNOWY_PLAINS), 0, 60, 0, sea, 0) == MapColor.ICE, "cold water freezes");
            h.assertTrue(EarthColors.surface(MapColor.WATER, plains, 0, 60, 0, sea, 0) == MapColor.WATER, "warm water stays");
            h.assertTrue(EarthColors.surface(MapColor.SNOW, biomes.getOrThrow(Biomes.JAGGED_PEAKS), 0, 200, 0, sea, 0.7) == MapColor.STONE, "steep peak: stone");
            h.assertTrue(EarthColors.surface(MapColor.SNOW, biomes.getOrThrow(Biomes.FROZEN_PEAKS), 0, 200, 0, sea, 0.7) == MapColor.ICE, "steep frozen peak: packed ice");
            h.assertTrue(EarthColors.surface(MapColor.NETHER, biomes.getOrThrow(Biomes.NETHER_WASTES), 0, 900, 0, sea, 3) == MapColor.NETHER, "Nether untouched");
            short[] heights = new short[TileMath.AREA];
            heights[TileMath.TILE + 1] = 512;
            h.assertTrue(TerrainSampler.slope(heights, 1, 1, 256) == 2 && TerrainSampler.slope(heights, 5, 5, 256) == 0, "slope from neighbours");
            h.succeed();
        });

        ALL.put("sampling_speed", h -> {
            // WD25-22: the overworld generator of a fixed seed (the test level itself may be flat), exact vs fast heights
            var reg = h.getLevel().registryAccess();
            var settings = reg.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(net.minecraft.world.level.levelgen.NoiseGeneratorSettings.OVERWORLD);
            var preset = reg.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                    .getOrThrow(net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists.OVERWORLD);
            var generator = new net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator(
                    net.minecraft.world.level.biome.MultiNoiseBiomeSource.createFromPreset(preset), settings);
            var random = net.minecraft.world.level.levelgen.RandomState.create(reg.lookupOrThrow(Registries.NOISE), 4242L, settings.value());
            var fast = TerrainSampler.surfaceSamplerForTests(generator, random);
            h.assertTrue(fast != null, "noise generator has a surface level");
            int n = 256;
            int[] exact = new int[n], quick = new int[n];
            long t0 = System.nanoTime();
            for (int k = 0; k < n; k++) exact[k] = TerrainSampler.height(generator, random, h.getLevel(), null, 100_000 + (k % 16) * 512, -60_000 + (k / 16) * 512);
            long t1 = System.nanoTime();
            for (int k = 0; k < n; k++) quick[k] = TerrainSampler.height(generator, random, h.getLevel(), fast, 100_000 + (k % 16) * 512, -60_000 + (k / 16) * 512);
            long t2 = System.nanoTime();
            int[] diff = new int[n];
            for (int k = 0; k < n; k++) diff[k] = Math.abs(exact[k] - quick[k]);
            Arrays.sort(diff);
            double exactUs = (t1 - t0) / 1000.0 / n, fastUs = (t2 - t1) / 1000.0 / n;
            long s0 = System.nanoTime();
            TerrainSampler.sample(generator, random, h.getLevel(), null, 8, 40, -20, 2, true, true);
            long s1 = System.nanoTime();
            TerrainSampler.sample(generator, random, h.getLevel(), null, 8, 41, -20, 2, true, false);
            long s2 = System.nanoTime();
            String m = String.format(java.util.Locale.ROOT, "[earth-measure] column exact %.1f us, fast %.1f us (x%.0f); height diff median %d, p90 %d, max %d;"
                    + " tile 64x64 fast %.0f ms, exact %.0f ms", exactUs, fastUs, exactUs / Math.max(0.001, fastUs), diff[n / 2], diff[n * 9 / 10], diff[n - 1],
                    (s1 - s0) / 1e6, (s2 - s1) / 1e6);
            SimpleEarth.LOG.info(m);
            h.assertTrue(fastUs * 5 < exactUs, "fast heights at least 5x faster: " + m);
            h.assertTrue(diff[n / 2] <= 16 && diff[n * 9 / 10] <= 48, "fast heights close to the exact ground: " + m);
            h.succeed();
        });

        ALL.put("color_table", h -> {
            ServerLevel level = h.getLevel();
            var biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
            Object[][] cases = {{Biomes.PLAINS, MapColor.GRASS}, {Biomes.DESERT, MapColor.SAND}, {Biomes.OCEAN, MapColor.WATER},
                    {Biomes.FROZEN_OCEAN, MapColor.ICE}, {Biomes.SNOWY_PLAINS, MapColor.SNOW}, {Biomes.BADLANDS, MapColor.TERRACOTTA_ORANGE},
                    {Biomes.FOREST, MapColor.PLANT}, {Biomes.MUSHROOM_FIELDS, MapColor.COLOR_PURPLE}, {Biomes.CHERRY_GROVE, MapColor.COLOR_PINK},
                    {Biomes.RIVER, MapColor.WATER}, {Biomes.BEACH, MapColor.SAND}, {Biomes.DARK_FOREST, MapColor.COLOR_GREEN}};
            for (Object[] c : cases) {
                @SuppressWarnings("unchecked") ResourceKey<Biome> key = (ResourceKey<Biome>) c[0];
                MapColor got = EarthColors.color(biomes.getOrThrow(key), false);
                h.assertTrue(got == c[1], key.identifier() + " -> map colour " + got.id + ", expected " + ((MapColor) c[1]).id);
            }
            h.assertTrue(EarthColors.color(biomes.getOrThrow(Biomes.PLAINS), true) == MapColor.WATER, "ground well below sea level = water");
            h.succeed();
        });

        ALL.put("tile_colors_from_biomes", h -> {
            // far outside anything generated: about 3.3 million blocks east, 2.5 million north
            ServerLevel level = h.getLevel();
            int e = 8, tx = 200, tz = -150;
            int loaded = level.getChunkSource().getLoadedChunksCount();
            int sampledBefore = TileService.SAMPLED.get();
            TerrainSampler.Tile tile = TerrainSampler.sample(level, e, tx, tz, false);
            var generator = level.getChunkSource().getGenerator();
            var random = level.getChunkSource().randomState();
            BiomeResolver independent = generator.getBiomeSource().createUncachedResolver(random);
            int sea = generator.getSeaLevel();
            Set<Byte> distinct = new HashSet<>();
            for (int k = 0; k < TileMath.AREA; k += 7) {
                int i = k % TileMath.TILE, j = k / TileMath.TILE;
                int x = (int) TileMath.sampleBlock((long) tx * TileMath.TILE + i, e), z = (int) TileMath.sampleBlock((long) tz * TileMath.TILE + j, e);
                int height = TerrainSampler.height(generator, random, level,
                        EarthServerConfig.fastHeights ? TerrainSampler.surfaceSamplerForTests(generator, random) : null, x, z);
                double slope = EarthServerConfig.surfaceDetail ? TerrainSampler.slope(tile.heights(), i, j, 1 << e) : -1;
                MapColor expected = TerrainSampler.colorAt(independent, x, height, z, sea, 1 << e, EarthServerConfig.biomeVotes, slope);
                h.assertTrue(tile.colors()[k] == (byte) expected.id, "sample " + i + "/" + j + " at " + x + "/" + z + ": " + tile.colors()[k] + " vs " + expected.id);
                h.assertTrue(tile.heights()[k] == height, "height at " + x + "/" + z);
                h.assertTrue(tile.colors()[k] != 0, "every sample known");
                distinct.add(tile.colors()[k]);
                h.assertTrue(level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null, "no chunk at " + x + "/" + z);
            }
            // a noise world (not the flat/void test level) changes biome within 16384 blocks
            if (generator instanceof net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator) {
                h.assertTrue(distinct.size() >= 2, "a 16384-block tile shows more than one map colour: " + distinct);
            }
            h.assertTrue(level.getChunkSource().getLoadedChunksCount() == loaded, "sampling loaded no chunk");
            int x0 = (int) TileMath.sampleBlock((long) tx * TileMath.TILE, e), z0 = (int) TileMath.sampleBlock((long) tz * TileMath.TILE, e);
            Path region = TerrainSampler.regionFolder(level).resolve("r." + (x0 >> 9) + "." + (z0 >> 9) + ".mca");
            h.assertTrue(!Files.exists(region), "no region file written: " + region);
            h.assertTrue(TileService.SAMPLED.get() == sampledBefore, "direct sampling bypasses the service counter");
            h.succeed();
        });

        ALL.put("tile_codec_and_store", h -> {
            byte[] colors = new byte[TileMath.AREA];
            short[] heights = new short[TileMath.AREA];
            for (int k = 0; k < TileMath.AREA; k++) {
                colors[k] = (byte) (k % 62);
                heights[k] = (short) (k % 700 - 64);
            }
            byte[] data = TileCodec.encode(colors, heights);
            byte[] c2 = new byte[TileMath.AREA];
            short[] h2 = new short[TileMath.AREA];
            h.assertTrue(TileCodec.decode(data, c2, h2) && Arrays.equals(colors, c2) && Arrays.equals(heights, h2), "round trip");
            h.assertTrue(!TileCodec.decode(new byte[]{1, 2, 3}, c2, h2), "broken data refused");
            h.assertTrue(data.length < TileCodec.RAW, "deflated");
            try {
                Path dir = Files.createTempDirectory("simpleearth-store");
                TileStore store = new TileStore(dir, 8, 1 << 20);
                for (int t = 0; t < 12; t++) store.put(6, t, -t, data);
                h.assertTrue(store.memorySize() == 8, "memory LRU bounded");
                h.assertTrue(store.memory(6, 0, 0) == null && store.get(6, 0, 0) != null, "evicted tile comes back from disk");
                h.assertTrue(Arrays.equals(store.get(6, 11, -11), data), "same bytes");
                TileStore small = new TileStore(dir.resolve("small"), 64, data.length * 3L);
                for (int t = 0; t < 10; t++) small.put(5, t, t, data);
                long files;
                try (var walk = Files.walk(dir.resolve("small"))) {
                    files = walk.filter(Files::isRegularFile).count();
                }
                h.assertTrue(files <= 3, "disk limit prunes the oldest files: " + files);
            } catch (Exception ex) {
                throw new AssertionError(ex);
            }
            h.succeed();
        });

        ALL.put("explored_only_hides_unexplored", h -> {
            TerrainSampler.Tile tile = TerrainSampler.sample(h.getLevel(), 9, -300, 280, true);
            int sea = h.getLevel().getChunkSource().getGenerator().getSeaLevel();
            for (int k = 0; k < TileMath.AREA; k++) {
                h.assertTrue(tile.colors()[k] == 0 && tile.heights()[k] == sea, "unexplored stays unknown (E8)");
            }
            h.succeed();
        });

        ALL.put("tile_requests_validated", h -> {
            h.assertTrue(TileService.validKey(new TileRequestPayload.Key(8, 10, -10)), "normal key");
            h.assertTrue(!TileService.validKey(new TileRequestPayload.Key(2, 0, 0)) && !TileService.validKey(new TileRequestPayload.Key(15, 0, 0)),
                    "grid exponent range");
            h.assertTrue(!TileService.validKey(new TileRequestPayload.Key(4, 1_000_000, 0)), "beyond the world border");
            ServerLevel level = h.getLevel();
            TileStore store = new TileStore(null, 16, 0);
            int before = TileService.SAMPLED.get();
            byte[] a = TileService.fetch(level, store, new TileRequestPayload.Key(10, 3, 4), false);
            byte[] b = TileService.fetch(level, store, new TileRequestPayload.Key(10, 3, 4), false);
            h.assertTrue(a == b && TileService.SAMPLED.get() == before + 1, "second request served from the cache");
            h.assertTrue(TileMath.exponentFor(65536, 256) == 8 && TileMath.exponentFor(65536, 64) == 10, "grid for face resolution");
            h.succeed();
        });

        ALL.put("orbit_lifecycle", h -> {
            ServerPlayer player = h.makeMockServerPlayerInLevel();
            OrbitTracker.forget(player.getUUID());
            OrbitTracker.State s = OrbitTracker.state(player);
            s.configure(1024, 1536, 65536, 256, h.getLevel().getMaxY());
            double x0 = player.getX(), z0 = player.getZ();
            player.setPos(x0, 1100, z0);
            OrbitTracker.tick(player, true);
            h.assertTrue(s.active && !s.parked && s.ax == x0 && s.az == z0, "activation height: centre A = here");
            player.setPos(x0 + 2, 1600, z0);
            OrbitTracker.tick(player, true);
            h.assertTrue(s.parked && s.ox == x0 + 2, "render switch: parked");
            OrbitTracker.handleMove(player, new OrbitMovePayload(1_000_000, 0));
            h.assertTrue(Math.abs(s.ox - (x0 + 2 + EarthServerConfig.maxOrbitSpeed / 20.0 * 1.25)) < 1e-6, "glide step capped by the server speed limit: " + s.ox);
            OrbitTracker.handleMove(player, new OrbitMovePayload(Float.NaN, 0));
            h.assertTrue(Double.isFinite(s.ox), "NaN ignored");
            double target = s.ox;
            player.setPos(x0 + 2, 1500, z0);
            OrbitTracker.tick(player, true);
            h.assertTrue(!s.parked && s.active, "below the switch: re-entry");
            h.assertTrue(Math.abs(player.getX() - target) < 1e-6, "re-entry at the orbit position (E6): " + player.getX());
            h.assertTrue(player.getAbilities().flying || player.hasEffect(MobEffects.SLOW_FALLING), "slow falling after re-entry");
            player.setPos(player.getX(), 900, z0);
            OrbitTracker.tick(player, true);
            h.assertTrue(!s.active, "below the activation height everything is Vanilla");
            player.setPos(player.getX(), 1200, z0);
            OrbitTracker.tick(player, false);
            h.assertTrue(!s.active, "players without the mod never enter the orbit");
            OrbitTracker.forget(player.getUUID());
            h.succeed();
        });

        ALL.put("sub_mod_switch", h -> {
            boolean before = com.simplebuilding.framework.api.SubMods.enabled(SimpleEarth.MOD_ID);
            ServerPlayer player = h.makeMockServerPlayerInLevel();
            try {
                com.simplebuilding.framework.api.SubMods.set(SimpleEarth.MOD_ID, false);
                h.assertTrue(!SimpleEarth.on() && !OrbitTracker.allowed(player), "switched off: no orbit");
                OrbitTracker.State s = OrbitTracker.state(player);
                player.setPos(player.getX(), 5000, player.getZ());
                OrbitTracker.tick(player, true);
                h.assertTrue(!s.active, "inert while off");
            } finally {
                com.simplebuilding.framework.api.SubMods.set(SimpleEarth.MOD_ID, before);
                OrbitTracker.forget(player.getUUID());
            }
            h.assertTrue(SimpleEarth.on(), "back on");
            h.succeed();
        });

        ALL.put("views_in_game", h -> {
            for (int v = 1; v <= EarthSettings.VERSIONS; v++) {
                EarthView view = EarthView.create(v);
                var params = new com.simplebuilding.modules.simpleearth.view.ViewParams();
                for (int k = 0; k < 400; k++) {
                    Scene s = view.step(k * 300.0, -k * 210.0, 1 / 20.0, params);
                    h.assertTrue(s.finite() && !s.patches.isEmpty(), "V" + v + " frame " + k);
                }
            }
            h.succeed();
        });
    }
}

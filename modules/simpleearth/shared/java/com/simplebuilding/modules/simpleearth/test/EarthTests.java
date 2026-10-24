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
        ALL.put("config_defaults", h -> {
            EarthServerConfig.reset();
            h.assertTrue(EarthServerConfig.enabled && !EarthServerConfig.exploredOnly && EarthServerConfig.showOthers, "E8/E9 defaults");
            h.assertTrue(EarthServerConfig.dimensions.equals(List.of("minecraft:overworld")), "E7: Overworld only");
            h.assertTrue(EarthServerConfig.activationY(100, 320) == 1024 && EarthServerConfig.activationY(2000, 320) == 2000,
                    "activation floor 1024 (E11), wishes above it kept");
            h.assertTrue(EarthServerConfig.activationY(500, 1200) == 1264, "never below build height + 64");
            h.assertTrue(EarthServerConfig.switchY(1000, 1024) == 1152, "switch at least 128 above activation");
            h.assertTrue(EarthServerConfig.edge(1) == 8192 && EarthServerConfig.edge(9_999_999) == 262144, "edge 8192..262144 (E1)");
            h.assertTrue(EarthServerConfig.speed(99999) == 2048, "orbit speed cap");
            h.assertTrue(EarthClientConfig.version == EarthSettings.DEFAULT_VERSION && EarthSettings.DEFAULT_VERSION == 6, "standard V6");
            for (int v = 1; v <= EarthSettings.VERSIONS; v++) {
                h.assertTrue(EarthSettings.find(v, "activationY").def() == 1024 && EarthSettings.find(v, "switchY").def() == 1536
                        && EarthSettings.find(v, "ceilingY").def() == 6144 && EarthSettings.find(v, "edge").def() == 65536
                        && EarthSettings.find(v, "orbitSpeed").def() == 256, "E1/E2/E11 defaults in V" + v);
            }
            h.assertTrue(EarthSettings.find(1, "edge").clamp(1e9) == 262144 && EarthSettings.find(6, "foldStart").clamp(-3) == 0, "client clamps");
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
            h.assertTrue(EarthColors.color(biomes.getOrThrow(Biomes.PLAINS), true) == MapColor.WATER, "ground below sea level = water");
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
                int height = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, random);
                MapColor expected = TerrainSampler.color(independent, x, Math.max(height, sea), z, height < sea);
                h.assertTrue(tile.colors()[k] == (byte) expected.id, "sample " + i + "/" + j + " at " + x + "/" + z + ": " + tile.colors()[k] + " vs " + expected.id);
                h.assertTrue(tile.heights()[k] == height, "height at " + x + "/" + z);
                h.assertTrue(tile.colors()[k] != 0, "every sample known");
                distinct.add(tile.colors()[k]);
                h.assertTrue(level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null, "no chunk at " + x + "/" + z);
            }
            h.assertTrue(distinct.size() >= 2, "a 16384-block tile shows more than one map colour: " + distinct);
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
            OrbitTracker.handleMove(player, new OrbitMovePayload(1000, 0));
            h.assertTrue(Math.abs(s.ox - (x0 + 2 + 256 / 20.0 * 1.25)) < 1e-6, "glide step capped by the orbit speed: " + s.ox);
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

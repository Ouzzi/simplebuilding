package com.simplebuilding.modules.simpleweather;

import com.simplebuilding.framework.api.SubMods;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

/** Shared bodies; Fabric wraps them in ModuleGameTest, NeoForge registers {@link #ALL} in ModuleNeoTests. */
public final class WeatherTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();
    static {
        ALL.put("weather", WeatherTests::weather);
        ALL.put("config", WeatherTests::config);
        ALL.put("qol_migration", WeatherTests::qolMigration);
        ALL.put("super_switch", WeatherTests::superSwitch);
    }

    private WeatherTests() {}

    public static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(SimpleWeather.MOD_ID, name);
    }

    private static void advance(GameTestHelper h) {
        try {
            var method = h.getLevel().getClass().getDeclaredMethod("advanceWeatherCycle");
            method.setAccessible(true);
            method.invoke(h.getLevel());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void rain(GameTestHelper h) {
        var world = h.getLevel();
        var data = world.getWeatherData();
        data.setRaining(true); data.setThundering(true);
        world.setRainLevel(1); world.setThunderLevel(1);
    }

    private static void configured(Runnable body) {
        var old = SimpleWeather.CONFIG;
        try {
            SimpleWeather.CONFIG = new WeatherConfig();
            body.run();
        } finally {
            SimpleWeather.CONFIG = old;
            SubMods.set(SimpleWeather.MOD_ID, true);
        }
    }

    /** The real server hook clears rain and thunder when switched on. */
    public static void weather(GameTestHelper h) {
        configured(() -> {
            rain(h);
            SimpleWeather.CONFIG.disableWeather = true;
            advance(h);
            var data = h.getLevel().getWeatherData();
            h.assertTrue(!data.isRaining() && !data.isThundering(), "server weather hook clears both");
        });
        h.succeed();
    }

    public static void config(GameTestHelper h) {
        var c = new WeatherConfig();
        h.assertTrue(!c.disableWeather && c.clientRainParticleDensity == 20, "defaults like Simple QoL");
        c.clientRainParticleDensity = 999; c.normalize();
        h.assertTrue(c.clientRainParticleDensity == 100, "density capped");
        c.clientRainParticleDensity = -5; c.normalize();
        h.assertTrue(c.clientRainParticleDensity == 0, "density floor");
        h.succeed();
    }

    /** First start: the old qOL values are taken over once; an existing own file is never overwritten. */
    public static void qolMigration(GameTestHelper h) {
        try {
            var dir = Files.createTempDirectory("simpleweather-migration");
            Files.writeString(dir.resolve("simplequalityoflife.json"),
                    "{\"frostWalkerWalkOnPowderSnow\":true,\"qOL\":{\"disableWeather\":true,\"clientRainParticleDensity\":55}}");
            var taken = SimpleWeather.read(dir);
            h.assertTrue(taken.disableWeather && taken.clientRainParticleDensity == 55, "old QoL values taken over");
            h.assertTrue(Files.isRegularFile(dir.resolve("simpleweather.json")), "own file written as marker");
            Files.writeString(dir.resolve("simpleweather.json"), "{\"disableWeather\":false,\"clientRainParticleDensity\":7}");
            var own = SimpleWeather.read(dir);
            h.assertTrue(!own.disableWeather && own.clientRainParticleDensity == 7, "own file wins over QoL");
            var empty = SimpleWeather.read(Files.createTempDirectory("simpleweather-empty"));
            h.assertTrue(!empty.disableWeather && empty.clientRainParticleDensity == 20, "no file: defaults");
        } catch (java.io.IOException e) {
            h.fail("migration io: " + e);
        }
        h.succeed();
    }

    /** "Enable Simple Weather" off in the super-mod: the hook leaves Vanilla weather alone. */
    public static void superSwitch(GameTestHelper h) {
        configured(() -> {
            rain(h);
            SimpleWeather.CONFIG.disableWeather = true;
            SubMods.set(SimpleWeather.MOD_ID, false);
            h.assertTrue(!SimpleWeather.active(), "switch read through framework");
            advance(h);
            h.assertTrue(h.getLevel().getWeatherData().isRaining(), "switched-off sub-mod keeps Vanilla weather");
            var data = h.getLevel().getWeatherData();
            data.setRaining(false); data.setThundering(false);
            h.getLevel().setRainLevel(0); h.getLevel().setThunderLevel(0);
        });
        h.succeed();
    }
}

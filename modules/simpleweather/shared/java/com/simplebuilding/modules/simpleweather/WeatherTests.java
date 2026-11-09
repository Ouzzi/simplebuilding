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
        ALL.put("frequency_factor", WeatherTests::frequencyFactor);
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

    /** The frequency factors scale the dry-spell countdowns; factor 1 is exactly Vanilla; durations (raining) stay untouched. */
    public static void frequencyFactor(GameTestHelper h) {
        long fast = 0, slow = 0;
        for (long t = 1000; t < 2000; t++) { fast += SimpleWeather.shift(t, 4f); slow += SimpleWeather.shift(t, 0.25f); }
        h.assertTrue(fast == 3000, "factor 4 skips 3 extra ticks per tick: " + fast);
        h.assertTrue(slow == -750, "factor 0.25 adds 0.75 ticks per tick: " + slow);
        h.assertTrue(SimpleWeather.shift(777, 1f) == 0 && SimpleWeather.scaled(0, 5) == 0 && SimpleWeather.scaled(3, 99) == 1, "Vanilla at 1, unset stays, never below 1");
        var c = new WeatherConfig();
        c.rainFrequencyFactor = 99f; c.thunderFrequencyFactor = 0f; c.normalize();
        h.assertTrue(c.rainFrequencyFactor == 4f && c.thunderFrequencyFactor == 0.25f, "factors bounded 0.25-4");
        h.assertTrue(new WeatherConfig().rainFrequencyFactor == 1f, "default is Vanilla");
        configured(() -> {
            var data = h.getLevel().getWeatherData();
            data.setRaining(false); data.setThundering(false); data.setClearWeatherTime(0);
            long time = h.getLevel().getGameTime();
            data.setRainTime(100_000); data.setThunderTime(100_000);
            advance(h);
            h.assertTrue(data.getRainTime() == 99_999 && data.getThunderTime() == 99_999, "factor 1: one tick per tick, got " + data.getRainTime());
            SimpleWeather.CONFIG.rainFrequencyFactor = 4f;
            SimpleWeather.CONFIG.thunderFrequencyFactor = 0.25f;
            data.setRainTime(100_000); data.setThunderTime(100_000);
            advance(h);
            int rain = 100_000 - 1 - SimpleWeather.shift(time, 4f), thunder = 100_000 - 1 - SimpleWeather.shift(time, 0.25f);
            h.assertTrue(data.getRainTime() == rain && data.getThunderTime() == thunder, "countdowns scaled in the real hook: " + data.getRainTime() + "/" + data.getThunderTime());
            data.setRainTime(0); data.setThunderTime(0);
        });
        h.succeed();
    }
}

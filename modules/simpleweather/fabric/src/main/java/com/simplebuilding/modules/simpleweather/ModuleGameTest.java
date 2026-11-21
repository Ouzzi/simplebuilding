package com.simplebuilding.modules.simpleweather;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric catalogue (Fabric runs only {@code @GameTest} methods); NeoForge mirrors WeatherTests.ALL in ModuleNeoTests. */
public final class ModuleGameTest {
 /** N132: every config option, category and the title have an EN and a DE name (configLabels, JSON config). */
 @GameTest(maxTicks = 100) public void configLabels(GameTestHelper h) { com.simplelib.test.ConfigCommandTests.labels(h, "simpleweather"); }
 @GameTest public void configCommand(GameTestHelper h) { com.simplelib.test.ConfigCommandTests.module(h, "simpleweather", false); }
    @GameTest(maxTicks = 100) public void weather(GameTestHelper h) { WeatherTests.weather(h); }
    @GameTest public void config(GameTestHelper h) { WeatherTests.config(h); }
    @GameTest public void qolMigration(GameTestHelper h) { WeatherTests.qolMigration(h); }
    @GameTest(maxTicks = 100) public void superSwitch(GameTestHelper h) { WeatherTests.superSwitch(h); }
    @GameTest(maxTicks = 100) public void frequencyFactor(GameTestHelper h) { WeatherTests.frequencyFactor(h); }
}

package com.simplebuilding.modules.simpleweather;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric catalogue (Fabric runs only {@code @GameTest} methods); NeoForge mirrors WeatherTests.ALL in ModuleNeoTests. */
public final class ModuleGameTest {
    @GameTest(maxTicks = 100) public void weather(GameTestHelper h) { WeatherTests.weather(h); }
    @GameTest public void config(GameTestHelper h) { WeatherTests.config(h); }
    @GameTest public void qolMigration(GameTestHelper h) { WeatherTests.qolMigration(h); }
    @GameTest(maxTicks = 100) public void superSwitch(GameTestHelper h) { WeatherTests.superSwitch(h); }
}

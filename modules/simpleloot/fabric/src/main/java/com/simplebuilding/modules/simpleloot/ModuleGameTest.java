package com.simplebuilding.modules.simpleloot;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric catalogue (Fabric runs only {@code @GameTest} methods); NeoForge/Forge register LootTests.ALL. */
public final class ModuleGameTest {
 @GameTest public void configCommand(GameTestHelper h) { com.simplelib.test.ConfigCommandTests.module(h, "simpleloot", false); }
    @GameTest public void launch(GameTestHelper h) { LootTests.launch(h); }
    @GameTest public void vault(GameTestHelper h) { LootTests.vault(h); }
    @GameTest public void vaultPersistence(GameTestHelper h) { LootTests.vaultPersistence(h); }
    @GameTest public void config(GameTestHelper h) { LootTests.config(h); }
    @GameTest public void qolMigration(GameTestHelper h) { LootTests.qolMigration(h); }
    @GameTest public void superSwitch(GameTestHelper h) { LootTests.superSwitch(h); }
}

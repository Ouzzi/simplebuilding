package com.simplebuilding.modules.simplemobs;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
public final class ModuleGameTest {
    @GameTest public void registered(GameTestHelper helper) { MobsTests.ALL.get("registered").accept(helper); }
    @GameTest public void phases(GameTestHelper helper) { MobsTests.ALL.get("phases").accept(helper); }
    @GameTest public void waveSizes(GameTestHelper helper) { MobsTests.ALL.get("wave_sizes").accept(helper); }
    @GameTest public void waveKinds(GameTestHelper helper) { MobsTests.ALL.get("wave_kinds").accept(helper); }
    @GameTest public void provocation(GameTestHelper helper) { MobsTests.ALL.get("provocation").accept(helper); }
    @GameTest public void pulseAndHeal(GameTestHelper helper) { MobsTests.ALL.get("pulse_and_heal").accept(helper); }
    @GameTest public void spawnRules(GameTestHelper helper) { MobsTests.ALL.get("spawn_rules").accept(helper); }
    @GameTest public void fight(GameTestHelper helper) { MobsTests.ALL.get("fight").accept(helper); }
}

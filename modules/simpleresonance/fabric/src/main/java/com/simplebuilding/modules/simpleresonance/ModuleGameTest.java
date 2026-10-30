package com.simplebuilding.modules.simpleresonance;

import com.simpleresonance.test.ResonanceTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: bodies in {@link ResonanceTests}, one @GameTest per entry (the runner reports a test missing here as absent). */
public final class ModuleGameTest {
    @GameTest(structure = "simpleresonance:empty") public void configCommand(GameTestHelper h) { com.simplelib.test.ConfigCommandTests.module(h, "simpleresonance", false); }
    @GameTest(structure = "simpleresonance:empty") public void registered(GameTestHelper h) { ResonanceTests.ALL.get("registered").accept(h); }
    @GameTest(structure = "simpleresonance:empty") public void configCaps(GameTestHelper h) { ResonanceTests.ALL.get("config_caps").accept(h); }
    @GameTest(structure = "simpleresonance:empty") public void subModSwitch(GameTestHelper h) { ResonanceTests.ALL.get("sub_mod_switch").accept(h); }
    @GameTest(structure = "simpleresonance:empty") public void zoneCurve(GameTestHelper h) { ResonanceTests.ALL.get("zone_curve").accept(h); }
    @GameTest(structure = "simpleresonance:empty") public void zoneDimensions(GameTestHelper h) { ResonanceTests.ALL.get("zone_dimensions").accept(h); }
    @GameTest(structure = "simpleresonance:empty") public void playerValue(GameTestHelper h) { ResonanceTests.ALL.get("player_value").accept(h); }
    @GameTest(structure = "simpleresonance:empty") public void deathAndRespawn(GameTestHelper h) { ResonanceTests.ALL.get("death_and_respawn").accept(h); }
    @GameTest(structure = "simpleresonance:empty") public void killPointsFarmGuard(GameTestHelper h) { ResonanceTests.ALL.get("kill_points_farm_guard").accept(h); }
    @GameTest(structure = "simpleresonance:empty") public void bossAward(GameTestHelper h) { ResonanceTests.ALL.get("boss_award").accept(h); }
    @GameTest(structure = "simpleresonance:empty") public void ownerProbabilities(GameTestHelper h) { ResonanceTests.ALL.get("owner_probabilities").accept(h); }
    @GameTest(structure = "simpleresonance:empty") public void ownerDistributionSeeded(GameTestHelper h) { ResonanceTests.ALL.get("owner_distribution_seeded").accept(h); }
    @GameTest(structure = "simpleresonance:empty") public void newcomerCaps(GameTestHelper h) { ResonanceTests.ALL.get("newcomer_caps").accept(h); }
    @GameTest(structure = "simpleresonance:empty") public void spawnServiceLevels(GameTestHelper h) { ResonanceTests.ALL.get("spawn_service_levels").accept(h); }
    @GameTest(structure = "simpleresonance:empty") public void migrationLegacyBlock(GameTestHelper h) { ResonanceTests.ALL.get("migration_legacy_block").accept(h); }
    @GameTest(structure = "simpleresonance:empty") public void migrationTickConsumes(GameTestHelper h) { ResonanceTests.ALL.get("migration_tick_consumes").accept(h); }
    @GameTest(structure = "simpleresonance:empty") public void contractValues(GameTestHelper h) { ResonanceTests.ALL.get("contract_values").accept(h); }
    @GameTest(structure = "simpleresonance:empty") public void command(GameTestHelper h) { ResonanceTests.ALL.get("command").accept(h); }
    @GameTest(structure = "simpleresonance:empty") public void performance(GameTestHelper h) { ResonanceTests.ALL.get("performance").accept(h); }
    @GameTest(structure = "simpleresonance:empty") public void guideBook(GameTestHelper h) { ResonanceTests.ALL.get("guide_book").accept(h); }
}

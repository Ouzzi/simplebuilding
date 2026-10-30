package com.simplebuilding.modules.simplechallenges;

import com.simplebuilding.modules.simplechallenges.test.ChallengeTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link ChallengeTests}, all on simplechallenges:empty. */
public final class ModuleGameTest {
    @GameTest(structure = "simplechallenges:empty", maxTicks = 100)
    public void configDefaults(GameTestHelper h) {
        ChallengeTests.ALL.get("config_defaults").accept(h);
    }

    @GameTest(structure = "simplechallenges:empty", maxTicks = 100)
    public void configCommand(GameTestHelper h) {
        ChallengeTests.ALL.get("config_command").accept(h);
    }

    @GameTest(structure = "simplechallenges:empty", maxTicks = 100)
    public void safetyLock(GameTestHelper h) {
        ChallengeTests.ALL.get("safety_lock").accept(h);
    }

    @GameTest(structure = "simplechallenges:empty", maxTicks = 100)
    public void gridLogic(GameTestHelper h) {
        ChallengeTests.ALL.get("grid_logic").accept(h);
    }

    @GameTest(structure = "simplechallenges:empty", maxTicks = 100)
    public void presetParser(GameTestHelper h) {
        ChallengeTests.ALL.get("preset_parser").accept(h);
    }

    @GameTest(structure = "simplechallenges:empty", maxTicks = 100)
    public void presetsShipped(GameTestHelper h) {
        ChallengeTests.ALL.get("presets_shipped").accept(h);
    }

    @GameTest(structure = "simplechallenges:empty", maxTicks = 100)
    public void priceEngine(GameTestHelper h) {
        ChallengeTests.ALL.get("price_engine").accept(h);
    }

    @GameTest(structure = "simplechallenges:empty", maxTicks = 100)
    public void solvability(GameTestHelper h) {
        ChallengeTests.ALL.get("solvability").accept(h);
    }

    @GameTest(structure = "simplechallenges:empty", maxTicks = 100)
    public void stateCodec(GameTestHelper h) {
        ChallengeTests.ALL.get("state_codec").accept(h);
    }

    @GameTest(structure = "simplechallenges:empty", maxTicks = 100)
    public void syncPayload(GameTestHelper h) {
        ChallengeTests.ALL.get("sync_payload").accept(h);
    }

    @GameTest(structure = "simplechallenges:empty", maxTicks = 200)
    public void probeNoChunks(GameTestHelper h) {
        ChallengeTests.ALL.get("probe_no_chunks").accept(h);
    }

    @GameTest(structure = "simplechallenges:empty", maxTicks = 200)
    public void enforcementInWorld(GameTestHelper h) {
        ChallengeTests.ALL.get("enforcement_in_world").accept(h);
    }

    @GameTest(structure = "simplechallenges:empty", maxTicks = 200)
    public void unlockPayAndCredits(GameTestHelper h) {
        ChallengeTests.ALL.get("unlock_pay_and_credits").accept(h);
    }

    @GameTest(structure = "simplechallenges:empty", maxTicks = 200)
    public void skyPlans(GameTestHelper h) {
        ChallengeTests.ALL.get("sky_plans").accept(h);
    }

    @GameTest(structure = "simplechallenges:empty", maxTicks = 200)
    public void presetWorldModes(GameTestHelper h) {
        ChallengeTests.ALL.get("preset_world_modes").accept(h);
    }

    @GameTest(structure = "simplechallenges:empty", maxTicks = 200)
    public void skyWorldModes(GameTestHelper h) {
        ChallengeTests.ALL.get("sky_world_modes").accept(h);
    }

    @GameTest(structure = "simplechallenges:empty", maxTicks = 200)
    public void leanSpawnLook(GameTestHelper h) {
        ChallengeTests.ALL.get("lean_spawn_look").accept(h);
    }

    @GameTest(structure = "simplechallenges:empty", maxTicks = 200)
    public void skylineLayout(GameTestHelper h) {
        ChallengeTests.ALL.get("skyline_layout").accept(h);
    }

    @GameTest(structure = "simplechallenges:empty", maxTicks = 400)
    public void skylineSequence(GameTestHelper h) {
        ChallengeTests.ALL.get("skyline_sequence").accept(h);
    }

    @GameTest(structure = "simplechallenges:empty", maxTicks = 200)
    public void luckyTable(GameTestHelper h) {
        ChallengeTests.ALL.get("lucky_table").accept(h);
    }

    @GameTest(structure = "simplechallenges:empty", maxTicks = 200)
    public void luckyBreak(GameTestHelper h) {
        ChallengeTests.ALL.get("lucky_break").accept(h);
    }

    @GameTest(structure = "simplechallenges:empty", maxTicks = 200)
    public void presetModesNew(GameTestHelper h) {
        ChallengeTests.ALL.get("preset_modes_new").accept(h);
    }

    @GameTest(structure = "simplechallenges:empty", maxTicks = 300)
    public void skylineWorld(GameTestHelper h) {
        ChallengeTests.ALL.get("skyline_world").accept(h);
    }

    @GameTest(structure = "simplechallenges:empty", maxTicks = 300)
    public void easterEggs(GameTestHelper h) {
        ChallengeTests.ALL.get("easter_eggs").accept(h);
    }

    @GameTest(structure = "simplechallenges:empty", maxTicks = 100)
    public void guidePages(GameTestHelper h) {
        com.simplebuilding.modules.simplechallenges.guide.ChallengesGuide.gameTest(h);
    }
}

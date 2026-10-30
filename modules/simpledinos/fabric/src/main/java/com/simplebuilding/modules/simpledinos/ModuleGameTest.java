package com.simplebuilding.modules.simpledinos;

import com.simpledinos.test.DinosTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link DinosTests}, all on the 16 x 8 x 16 stone arena. */
public final class ModuleGameTest {
    @GameTest public void configCommand(GameTestHelper h) { com.simplelib.test.ConfigCommandTests.module(h, "simpledinos", false); }

    @GameTest(structure = "simpledinos:arena", maxTicks = 100)
    public void registered(GameTestHelper h) {
        DinosTests.ALL.get("registered").accept(h);
    }

    @GameTest(structure = "simpledinos:arena", maxTicks = 100)
    public void configCaps(GameTestHelper h) {
        DinosTests.ALL.get("config_caps").accept(h);
    }

    @GameTest(structure = "simpledinos:arena", maxTicks = 100)
    public void subModSwitch(GameTestHelper h) {
        DinosTests.ALL.get("sub_mod_switch").accept(h);
    }

    @GameTest(structure = "simpledinos:arena", maxTicks = 100)
    public void progressRewindsDoubleSpeed(GameTestHelper h) {
        DinosTests.ALL.get("progress_rewinds_double_speed").accept(h);
    }

    @GameTest(structure = "simpledinos:arena", maxTicks = 100)
    public void fossilBrushingStagesAndRewind(GameTestHelper h) {
        DinosTests.ALL.get("fossil_brushing_stages_and_rewind").accept(h);
    }

    @GameTest(structure = "simpledinos:arena", maxTicks = 100)
    public void fossilFinishDropsTrexEgg(GameTestHelper h) {
        DinosTests.ALL.get("fossil_finish_drops_trex_egg").accept(h);
    }

    @GameTest(structure = "simpledinos:arena", maxTicks = 100)
    public void fossilKindIsStablePerPosition(GameTestHelper h) {
        DinosTests.ALL.get("fossil_kind_is_stable_per_position").accept(h);
    }

    @GameTest(structure = "simpledinos:arena", maxTicks = 100)
    public void eggBroodingPhasesAndLock(GameTestHelper h) {
        DinosTests.ALL.get("egg_brooding_phases_and_lock").accept(h);
    }

    @GameTest(structure = "simpledinos:arena", maxTicks = 100)
    public void eggRewindsTwiceAsFastWhenNobodySits(GameTestHelper h) {
        DinosTests.ALL.get("egg_rewinds_twice_as_fast_when_nobody_sits").accept(h);
    }

    @GameTest(structure = "simpledinos:arena", maxTicks = 100)
    public void eggSeatAndCleanup(GameTestHelper h) {
        DinosTests.ALL.get("egg_seat_and_cleanup").accept(h);
    }

    @GameTest(structure = "simpledinos:arena", maxTicks = 100)
    public void eggHatchesTameHatchling(GameTestHelper h) {
        DinosTests.ALL.get("egg_hatches_tame_hatchling").accept(h);
    }

    @GameTest(structure = "simpledinos:arena", maxTicks = 100)
    public void eggWaitsWithoutPlayers(GameTestHelper h) {
        DinosTests.ALL.get("egg_waits_without_players").accept(h);
    }

    @GameTest(structure = "simpledinos:arena", maxTicks = 100)
    public void eggOtherKindsWaitForTheirDinosaur(GameTestHelper h) {
        DinosTests.ALL.get("egg_other_kinds_wait_for_their_dinosaur").accept(h);
    }

    @GameTest(structure = "simpledinos:arena", maxTicks = 100)
    public void dinoGrowthScale(GameTestHelper h) {
        DinosTests.ALL.get("dino_growth_scale").accept(h);
    }

    @GameTest(structure = "simpledinos:arena", maxTicks = 100)
    public void trexCoreHitboxAndStats(GameTestHelper h) {
        DinosTests.ALL.get("trex_core_hitbox_and_stats").accept(h);
    }

    @GameTest(structure = "simpledinos:arena", maxTicks = 100)
    public void trexSaddleArmorRiding(GameTestHelper h) {
        DinosTests.ALL.get("trex_saddle_armor_riding").accept(h);
    }

    @GameTest(structure = "simpledinos:arena", maxTicks = 100)
    public void trexEatsMeatAndHeals(GameTestHelper h) {
        DinosTests.ALL.get("trex_eats_meat_and_heals").accept(h);
    }

    @GameTest(structure = "simpledinos:arena", maxTicks = 100)
    public void trexWithoutOwnerCannotBeRidden(GameTestHelper h) {
        DinosTests.ALL.get("trex_without_owner_cannot_be_ridden").accept(h);
    }

    @GameTest(structure = "simpledinos:arena", maxTicks = 100)
    public void guideBook(GameTestHelper h) {
        DinosTests.ALL.get("guide_book").accept(h);
    }
}

package com.simplebuilding.modules.simpleanimals;

import com.simpleanimals.test.AnimalsTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link AnimalsTests}, all on the 16 x 8 x 16 stone arena. */
public final class ModuleGameTest {
    @GameTest public void configCommand(GameTestHelper h) { com.simplelib.test.ConfigCommandTests.module(h, "simpleanimals", false); }

    @GameTest(structure = "simpleanimals:arena", maxTicks = 100)
    public void registered(GameTestHelper h) {
        AnimalsTests.ALL.get("registered").accept(h);
    }

    @GameTest(structure = "simpleanimals:arena", maxTicks = 100)
    public void configCaps(GameTestHelper h) {
        AnimalsTests.ALL.get("config_caps").accept(h);
    }

    @GameTest(structure = "simpleanimals:arena", maxTicks = 100)
    public void subModSwitch(GameTestHelper h) {
        AnimalsTests.ALL.get("sub_mod_switch").accept(h);
    }

    @GameTest(structure = "simpleanimals:arena", maxTicks = 100)
    public void spawnGround(GameTestHelper h) {
        AnimalsTests.ALL.get("spawn_ground").accept(h);
    }

    @GameTest(structure = "simpleanimals:arena", maxTicks = 100)
    public void boarPigletAggro(GameTestHelper h) {
        AnimalsTests.ALL.get("boar_piglet_aggro").accept(h);
    }

    @GameTest(structure = "simpleanimals:arena", maxTicks = 100)
    public void boarNoPigletProvocation(GameTestHelper h) {
        AnimalsTests.ALL.get("boar_no_piglet_provocation").accept(h);
    }

    @GameTest(structure = "simpleanimals:arena", maxTicks = 100)
    public void boarCloseTimeCountsOnlyNearPlayers(GameTestHelper h) {
        AnimalsTests.ALL.get("boar_close_time_counts_only_near_players").accept(h);
    }

    @GameTest(structure = "simpleanimals:arena", maxTicks = 100)
    public void boarRamDamage(GameTestHelper h) {
        AnimalsTests.ALL.get("boar_ram_damage").accept(h);
    }

    @GameTest(structure = "simpleanimals:arena", maxTicks = 120)
    public void hedgehogCurlsFasterThanArmadillo(GameTestHelper h) {
        AnimalsTests.ALL.get("hedgehog_curls_faster_than_armadillo").accept(h);
    }

    @GameTest(structure = "simpleanimals:arena", maxTicks = 100)
    public void hedgehogCurledTakesLessAndPricks(GameTestHelper h) {
        AnimalsTests.ALL.get("hedgehog_curled_takes_less_and_pricks").accept(h);
    }

    @GameTest(structure = "simpleanimals:arena", maxTicks = 160)
    public void hedgehogHuntsSilverfish(GameTestHelper h) {
        AnimalsTests.ALL.get("hedgehog_hunts_silverfish").accept(h);
    }

    @GameTest(structure = "simpleanimals:arena", maxTicks = 100)
    public void zebraCrossesWithHorseToZorse(GameTestHelper h) {
        AnimalsTests.ALL.get("zebra_crosses_with_horse_to_zorse").accept(h);
    }

    @GameTest(structure = "simpleanimals:arena", maxTicks = 100)
    public void zebraUntamedDoesNotBreed(GameTestHelper h) {
        AnimalsTests.ALL.get("zebra_untamed_does_not_breed").accept(h);
    }

    @GameTest(structure = "simpleanimals:arena", maxTicks = 100)
    public void zorseIsInfertile(GameTestHelper h) {
        AnimalsTests.ALL.get("zorse_is_infertile").accept(h);
    }

    @GameTest(structure = "simpleanimals:arena", maxTicks = 100)
    public void zebraIsAHorse(GameTestHelper h) {
        AnimalsTests.ALL.get("zebra_is_a_horse").accept(h);
    }

    @GameTest(structure = "simpleanimals:arena", maxTicks = 100)
    public void deerFoodAndOffspring(GameTestHelper h) {
        AnimalsTests.ALL.get("deer_food_and_offspring").accept(h);
    }

    @GameTest(structure = "simpleanimals:arena", maxTicks = 100)
    public void crabFoodCooksLikeCod(GameTestHelper h) {
        AnimalsTests.ALL.get("crab_food_cooks_like_cod").accept(h);
    }

    @GameTest(structure = "simpleanimals:arena", maxTicks = 100)
    public void naturalSpawnNeverTouchesPlayersOrBlocks(GameTestHelper h) {
        AnimalsTests.ALL.get("natural_spawn_never_touches_players_or_blocks").accept(h);
    }

    @GameTest(structure = "simpleanimals:arena", maxTicks = 100)
    public void guideBook(GameTestHelper h) {
        AnimalsTests.ALL.get("guide_book").accept(h);
    }
}

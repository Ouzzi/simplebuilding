package com.simplebuilding.modules.simplewanderers;

import com.simplewanderers.test.WandererTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link WandererTests}, all on the 15 x 12 x 15 arena. */
public final class ModuleGameTest {
    @GameTest(structure = "simplewanderers:arena", maxTicks = 100)
    public void registered(GameTestHelper h) {
        WandererTests.ALL.get("registered").accept(h);
    }

    @GameTest(structure = "simplewanderers:arena", maxTicks = 100)
    public void configCaps(GameTestHelper h) {
        WandererTests.ALL.get("config_caps").accept(h);
    }

    @GameTest(structure = "simplewanderers:arena", maxTicks = 100)
    public void subModSwitch(GameTestHelper h) {
        WandererTests.ALL.get("sub_mod_switch").accept(h);
    }

    @GameTest(structure = "simplewanderers:arena", maxTicks = 100)
    public void spawnOnePerVillage(GameTestHelper h) {
        WandererTests.ALL.get("spawn_one_per_village").accept(h);
    }

    @GameTest(structure = "simplewanderers:arena", maxTicks = 100)
    public void duplicateRemovedOnJoin(GameTestHelper h) {
        WandererTests.ALL.get("duplicate_removed_on_join").accept(h);
    }

    @GameTest(structure = "simplewanderers:arena", maxTicks = 100)
    public void spawnWithoutVillageIsIgnored(GameTestHelper h) {
        WandererTests.ALL.get("spawn_without_village_is_ignored").accept(h);
    }

    @GameTest(structure = "simplewanderers:arena", maxTicks = 100)
    public void respawnAfterDaysOnlyWhenVisited(GameTestHelper h) {
        WandererTests.ALL.get("respawn_after_days_only_when_visited").accept(h);
    }

    @GameTest(structure = "simplewanderers:arena", maxTicks = 100)
    public void lostRecordRecovers(GameTestHelper h) {
        WandererTests.ALL.get("lost_record_recovers").accept(h);
    }

    @GameTest(structure = "simplewanderers:arena", maxTicks = 100)
    public void villageRecordsRoundTrip(GameTestHelper h) {
        WandererTests.ALL.get("village_records_round_trip").accept(h);
    }

    @GameTest(structure = "simplewanderers:arena", maxTicks = 100)
    public void horseLeashedAndCapturable(GameTestHelper h) {
        WandererTests.ALL.get("horse_leashed_and_capturable").accept(h);
    }

    @GameTest(structure = "simplewanderers:arena", maxTicks = 100)
    public void rotationRerollEveryPeriodRestockEveryHalf(GameTestHelper h) {
        WandererTests.ALL.get("rotation_reroll_every_period_restock_every_half").accept(h);
    }

    @GameTest(structure = "simplewanderers:arena", maxTicks = 100)
    public void rotationWaitsForTradingAndNeverBursts(GameTestHelper h) {
        WandererTests.ALL.get("rotation_waits_for_trading_and_never_bursts").accept(h);
    }

    @GameTest(structure = "simplewanderers:arena", maxTicks = 100)
    public void rotationPersistsAcrossReload(GameTestHelper h) {
        WandererTests.ALL.get("rotation_persists_across_reload").accept(h);
    }

    @GameTest(structure = "simplewanderers:arena", maxTicks = 200)
    public void tradesOnlyFromWandererPools(GameTestHelper h) {
        WandererTests.ALL.get("trades_only_from_wanderer_pools").accept(h);
    }

    @GameTest(structure = "simplewanderers:arena", maxTicks = 100)
    public void vanillaCommonPartitionComplete(GameTestHelper h) {
        WandererTests.ALL.get("vanilla_common_partition_complete").accept(h);
    }

    @GameTest(structure = "simplewanderers:arena", maxTicks = 100)
    public void noVeryRareModItemAndCoresStayAtTheTrader(GameTestHelper h) {
        WandererTests.ALL.get("no_very_rare_mod_item_and_cores_stay_at_the_trader").accept(h);
    }

    @GameTest(structure = "simplewanderers:arena", maxTicks = 100)
    public void biomeAssortment(GameTestHelper h) {
        WandererTests.ALL.get("biome_assortment").accept(h);
    }

    @GameTest(structure = "simplewanderers:arena", maxTicks = 100)
    public void worksStandalone(GameTestHelper h) {
        WandererTests.ALL.get("works_standalone").accept(h);
    }

    @GameTest(structure = "simplewanderers:arena", maxTicks = 100)
    public void guideBook(GameTestHelper h) {
        WandererTests.ALL.get("guide_book").accept(h);
    }

    @GameTest(structure = "simplewanderers:arena", maxTicks = 100)
    public void discoverRecipeUnlocks(GameTestHelper h) {
        WandererTests.ALL.get("discover_recipe_unlocks").accept(h);
    }

    @GameTest(structure = "simplewanderers:arena", maxTicks = 100)
    public void discoverBehaviourHints(GameTestHelper h) {
        WandererTests.ALL.get("discover_behaviour_hints").accept(h);
    }
}

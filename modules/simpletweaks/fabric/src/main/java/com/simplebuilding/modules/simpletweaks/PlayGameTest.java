package com.simplebuilding.modules.simpletweaks;
import com.simplebuilding.modules.simpletweaks.play.PlayTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
/** Fabric runner of {@link PlayTests} (time limits as in {@link PlayTests#maxTicks}). */
public final class PlayGameTest {
 @GameTest(maxTicks=100) public void configDefaultsAndBounds(GameTestHelper h) { PlayTests.TESTS.get("config_defaults_and_bounds").accept(h); }
 @GameTest(maxTicks=100) public void commandsFollowSwitches(GameTestHelper h) { PlayTests.TESTS.get("commands_follow_switches").accept(h); }
 @GameTest(maxTicks=100) public void coordsLineAndPing(GameTestHelper h) { PlayTests.TESTS.get("coords_line_and_ping").accept(h); }
 @GameTest(maxTicks=100) public void deathHintAfterDeath(GameTestHelper h) { PlayTests.TESTS.get("death_hint_after_death").accept(h); }
 @GameTest(maxTicks=100) public void mentionsHighlightAndSoundTargets(GameTestHelper h) { PlayTests.TESTS.get("mentions_highlight_and_sound_targets").accept(h); }
 @GameTest(maxTicks=100) public void playtimeAndSeen(GameTestHelper h) { PlayTests.TESTS.get("playtime_and_seen").accept(h); }
 @GameTest(maxTicks=100) public void mobGriefingPerMob(GameTestHelper h) { PlayTests.TESTS.get("mob_griefing_per_mob").accept(h); }
 @GameTest(maxTicks=100) public void pvpBothMustAgree(GameTestHelper h) { PlayTests.TESTS.get("pvp_both_must_agree").accept(h); }
 @GameTest(maxTicks=100) public void welcomeLinesAndTips(GameTestHelper h) { PlayTests.TESTS.get("welcome_lines_and_tips").accept(h); }
 @GameTest(maxTicks=100) public void loginRewardStreak(GameTestHelper h) { PlayTests.TESTS.get("login_reward_streak").accept(h); }
 @GameTest(maxTicks=100) public void eventsXpLootSchedule(GameTestHelper h) { PlayTests.TESTS.get("events_xp_loot_schedule").accept(h); }
 @GameTest(maxTicks=100) public void leaderboardSortsAndSidebar(GameTestHelper h) { PlayTests.TESTS.get("leaderboard_sorts_and_sidebar").accept(h); }
 @GameTest(maxTicks=100) public void voteMajorityAndOptions(GameTestHelper h) { PlayTests.TESTS.get("vote_majority_and_options").accept(h); }
 @GameTest(maxTicks=100) public void afkTabNameAndSleep(GameTestHelper h) { PlayTests.TESTS.get("afk_tab_name_and_sleep").accept(h); }
 @GameTest(maxTicks=100) public void borderPerDimension(GameTestHelper h) { PlayTests.TESTS.get("border_per_dimension").accept(h); }
 @GameTest(maxTicks=100) public void netherRoofMovesDown(GameTestHelper h) { PlayTests.TESTS.get("nether_roof_moves_down").accept(h); }
 @GameTest(maxTicks=100) public void keepInventoryModes(GameTestHelper h) { PlayTests.TESTS.get("keep_inventory_modes").accept(h); }
 @GameTest(maxTicks=100) public void dragonEggEveryFight(GameTestHelper h) { PlayTests.TESTS.get("dragon_egg_every_fight").accept(h); }
 @GameTest(maxTicks=100) public void phantomsOptOut(GameTestHelper h) { PlayTests.TESTS.get("phantoms_opt_out").accept(h); }
 @GameTest(maxTicks=100) public void entityLimitReportsAndBlocks(GameTestHelper h) { PlayTests.TESTS.get("entity_limit_reports_and_blocks").accept(h); }
 @GameTest(maxTicks=100) public void cleanupSparesNamedAndRare(GameTestHelper h) { PlayTests.TESTS.get("cleanup_spares_named_and_rare").accept(h); }
 @GameTest(maxTicks=12000) public void pregenSpiralAndGenerates(GameTestHelper h) { PlayTests.TESTS.get("pregen_spiral_and_generates").accept(h); }
}

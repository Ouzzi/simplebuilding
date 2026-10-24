package com.simplebuilding.modules.simpletweaks;
import com.simplebuilding.modules.simpletweaks.admin.AdminTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
/** Fabric runner of {@link AdminTests} (time limits as in {@link AdminTests#maxTicks}). */
public final class AdminGameTest {
 @GameTest(maxTicks=100) public void configDefaultsAndBounds(GameTestHelper h) { AdminTests.TESTS.get("config_defaults_and_bounds").accept(h); }
 @GameTest(maxTicks=100) public void commandsFollowSwitches(GameTestHelper h) { AdminTests.TESTS.get("commands_follow_switches").accept(h); }
 @GameTest(maxTicks=100) public void blocklogRecordsBreaksPlacesContainers(GameTestHelper h) { AdminTests.TESTS.get("blocklog_records_breaks_places_containers").accept(h); }
 @GameTest(maxTicks=100) public void blocklogInspectDoesNotBreak(GameTestHelper h) { AdminTests.TESTS.get("blocklog_inspect_does_not_break").accept(h); }
 @GameTest(maxTicks=100) public void blocklogRollback(GameTestHelper h) { AdminTests.TESTS.get("blocklog_rollback").accept(h); }
 @GameTest(maxTicks=100) public void blocklogRetentionAndFormat(GameTestHelper h) { AdminTests.TESTS.get("blocklog_retention_and_format").accept(h); }
 @GameTest(maxTicks=100) public void blocklogSignsExplosionsFireHoppers(GameTestHelper h) { AdminTests.TESTS.get("blocklog_signs_explosions_fire_hoppers").accept(h); }
 @GameTest(maxTicks=100) public void blocklogLookupFilters(GameTestHelper h) { AdminTests.TESTS.get("blocklog_lookup_filters").accept(h); }
 @GameTest(maxTicks=100) public void blocklogPreviewAndRestore(GameTestHelper h) { AdminTests.TESTS.get("blocklog_preview_and_restore").accept(h); }
 @GameTest(maxTicks=100) public void blocklogPurgePlayer(GameTestHelper h) { AdminTests.TESTS.get("blocklog_purge_player").accept(h); }
 @GameTest(maxTicks=100) public void blocklogCommandPermissions(GameTestHelper h) { AdminTests.TESTS.get("blocklog_command_permissions").accept(h); }
 @GameTest(maxTicks=100) public void blocklogLegacyLinesReadable(GameTestHelper h) { AdminTests.TESTS.get("blocklog_legacy_lines_readable").accept(h); }
 @GameTest(maxTicks=100) public void deathsBackupOnDeathAndRestore(GameTestHelper h) { AdminTests.TESTS.get("deaths_backup_on_death_and_restore").accept(h); }
 @GameTest(maxTicks=100) public void deathsOfflineRestore(GameTestHelper h) { AdminTests.TESTS.get("deaths_offline_restore").accept(h); }
 @GameTest(maxTicks=100) public void invseeLiveTakeAndRules(GameTestHelper h) { AdminTests.TESTS.get("invsee_live_take_and_rules").accept(h); }
 @GameTest(maxTicks=100) public void invseeOfflineWriteBack(GameTestHelper h) { AdminTests.TESTS.get("invsee_offline_write_back").accept(h); }
 @GameTest(maxTicks=100) public void simpletweaksRootMirrorsAllCommands(GameTestHelper h) { AdminTests.TESTS.get("simpletweaks_root_mirrors_all_commands").accept(h); }
 @GameTest(maxTicks=100) public void vanishHidesAndIgnores(GameTestHelper h) { AdminTests.TESTS.get("vanish_hides_and_ignores").accept(h); }
 @GameTest(maxTicks=200) public void vanishNoVibrations(GameTestHelper h) { AdminTests.TESTS.get("vanish_no_vibrations").accept(h); }
 @GameTest(maxTicks=100) public void freezeHoldsPlayer(GameTestHelper h) { AdminTests.TESTS.get("freeze_holds_player").accept(h); }
 @GameTest(maxTicks=200) public void restartCountdown(GameTestHelper h) { AdminTests.TESTS.get("restart_countdown").accept(h); }
 @GameTest(maxTicks=1200) public void backupZipAndRotation(GameTestHelper h) { AdminTests.TESTS.get("backup_zip_and_rotation").accept(h); }
 @GameTest(maxTicks=1300) public void backupStopResumesSaving(GameTestHelper h) { AdminTests.TESTS.get("backup_stop_resumes_saving").accept(h); }
 @GameTest(maxTicks=100) public void maintenanceLoginAndMotd(GameTestHelper h) { AdminTests.TESTS.get("maintenance_login_and_motd").accept(h); }
 @GameTest(maxTicks=100) public void lagFinderSamples(GameTestHelper h) { AdminTests.TESTS.get("lag_finder_samples").accept(h); }
 @GameTest(maxTicks=100) public void unreadableStateFileKept(GameTestHelper h) { AdminTests.TESTS.get("unreadable_state_file_kept").accept(h); }
}

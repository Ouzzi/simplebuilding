package com.simplebuilding.modules.simpletweaks;
import com.simplebuilding.modules.simpletweaks.protect.ProtectTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
/** Fabric runner of {@link ProtectTests} (time limits as in {@link ProtectTests#maxTicks}). */
public final class ProtectGameTest {
 @GameTest(maxTicks=100) public void configDefaultsAndBounds(GameTestHelper h) { ProtectTests.TESTS.get("config_defaults_and_bounds").accept(h); }
 @GameTest(maxTicks=100) public void commandsFollowSwitches(GameTestHelper h) { ProtectTests.TESTS.get("commands_follow_switches").accept(h); }
 @GameTest(maxTicks=100) public void obfuscatedChunkHidesEnclosedOre(GameTestHelper h) { ProtectTests.TESTS.get("obfuscated_chunk_hides_enclosed_ore").accept(h); }
 @GameTest(maxTicks=100) public void exemptAdminSeesRealBlocks(GameTestHelper h) { ProtectTests.TESTS.get("exempt_admin_sees_real_blocks").accept(h); }
 @GameTest(maxTicks=100) public void miningRevealsNeighbours(GameTestHelper h) { ProtectTests.TESTS.get("mining_reveals_neighbours").accept(h); }
 @GameTest(maxTicks=100) public void fakeOresModeTwo(GameTestHelper h) { ProtectTests.TESTS.get("fake_ores_mode_two").accept(h); }
 @GameTest(maxTicks=100) public void benchCostLogged(GameTestHelper h) { ProtectTests.TESTS.get("bench_cost_logged").accept(h); }
 @GameTest(maxTicks=100) public void freecamRulePayloadAndKick(GameTestHelper h) { ProtectTests.TESTS.get("freecam_rule_payload_and_kick").accept(h); }
 @GameTest(maxTicks=100) public void seedHashRandomised(GameTestHelper h) { ProtectTests.TESTS.get("seed_hash_randomised").accept(h); }
 @GameTest(maxTicks=100) public void hideHealthOfOthers(GameTestHelper h) { ProtectTests.TESTS.get("hide_health_of_others").accept(h); }
 @GameTest(maxTicks=100) public void checksFlagFarBreakNotServerBreaks(GameTestHelper h) { ProtectTests.TESTS.get("checks_flag_far_break_not_server_breaks").accept(h); }
 @GameTest(maxTicks=100) public void fastBreakUsesRealSpeed(GameTestHelper h) { ProtectTests.TESTS.get("fast_break_uses_real_speed").accept(h); }
 @GameTest(maxTicks=100) public void hammerAreaBreakNotFlagged(GameTestHelper h) { ProtectTests.TESTS.get("hammer_area_break_not_flagged").accept(h); }
 @GameTest(maxTicks=100) public void autoOffWithPartnerMods(GameTestHelper h) { ProtectTests.TESTS.get("auto_off_with_partner_mods").accept(h); }
}

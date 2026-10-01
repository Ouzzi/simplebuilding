package com.simplebuilding.modules.simpletweaks;
import com.simplebuilding.modules.simpletweaks.claims.ClaimTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
public final class ClaimsGameTest {
 @GameTest(environment="simpletweaks:claims_portal",maxTicks=400) public void claimsPortalFlow(GameTestHelper h) { ClaimTests.TESTS.get("claims_portal_flow").accept(h); }
 @GameTest public void claimsAccessCommands(GameTestHelper h) { ClaimTests.TESTS.get("claims_access_commands").accept(h); }
 @GameTest public void claimsAccessCapsFailures(GameTestHelper h) { ClaimTests.TESTS.get("claims_access_caps_failures").accept(h); }
 @GameTest public void claimsAccessAdmin(GameTestHelper h) { ClaimTests.TESTS.get("claims_access_admin").accept(h); }
 @GameTest public void claimsToolsBedHammer(GameTestHelper h) { ClaimTests.TESTS.get("claims_tools_bed_hammer").accept(h); }
 @GameTest public void claimsToolsWandHammer(GameTestHelper h) { ClaimTests.TESTS.get("claims_tools_wand_hammer").accept(h); }
 @GameTest public void claimsToolsBeam(GameTestHelper h) { ClaimTests.TESTS.get("claims_tools_beam").accept(h); }
 @GameTest public void claimsToolsEcho(GameTestHelper h) { ClaimTests.TESTS.get("claims_tools_echo").accept(h); }
 @GameTest public void claimsToolsPad(GameTestHelper h) { ClaimTests.TESTS.get("claims_tools_pad").accept(h); }

 @GameTest public void claimsDisabledHooks(GameTestHelper h) { ClaimTests.TESTS.get("claims_disabled_hooks").accept(h); }
 @GameTest public void claimsBedFootprint(GameTestHelper h) { ClaimTests.TESTS.get("claims_bed_footprint").accept(h); }
 @GameTest public void claimsVanillaBorder(GameTestHelper h) { ClaimTests.TESTS.get("claims_vanilla_border").accept(h); }
 @GameTest public void claimsBucketEntityHooks(GameTestHelper h) { ClaimTests.TESTS.get("claims_bucket_entity_hooks").accept(h); }
 @GameTest public void claimsCommandCollision(GameTestHelper h) { ClaimTests.TESTS.get("claims_command_collision").accept(h); }
 @GameTest public void claimsConfigBounds(GameTestHelper h) { ClaimTests.TESTS.get("claims_config_bounds").accept(h); }
 @GameTest public void claimsLegacyAtomicRoundtrip(GameTestHelper h) { ClaimTests.TESTS.get("claims_legacy_atomic_roundtrip").accept(h); }
 @GameTest public void claimsMalformedPreserved(GameTestHelper h) { ClaimTests.TESTS.get("claims_malformed_preserved").accept(h); }
 @GameTest public void claimsDisabledNoIo(GameTestHelper h) { ClaimTests.TESTS.get("claims_disabled_no_io").accept(h); }
 @GameTest public void claimsCapsAndCooldown(GameTestHelper h) { ClaimTests.TESTS.get("claims_caps_and_cooldown").accept(h); }
 @GameTest public void claimsDeedTwoPlayers(GameTestHelper h) { ClaimTests.TESTS.get("claims_deed_two_players").accept(h); }
 @GameTest public void claimsPolicyBoundaries(GameTestHelper h) { ClaimTests.TESTS.get("claims_policy_boundaries").accept(h); }
}

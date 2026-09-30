package com.simplebuilding.modules.simpletweaks;
import com.simplebuilding.modules.simpletweaks.claims.ClaimTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
public final class ClaimsGameTest {
 @GameTest public void claimsCommandCollision(GameTestHelper h) { ClaimTests.TESTS.get("claims_command_collision").accept(h); }
 @GameTest public void claimsConfigBounds(GameTestHelper h) { ClaimTests.TESTS.get("claims_config_bounds").accept(h); }
 @GameTest public void claimsLegacyAtomicRoundtrip(GameTestHelper h) { ClaimTests.TESTS.get("claims_legacy_atomic_roundtrip").accept(h); }
 @GameTest public void claimsMalformedPreserved(GameTestHelper h) { ClaimTests.TESTS.get("claims_malformed_preserved").accept(h); }
 @GameTest public void claimsDisabledNoIo(GameTestHelper h) { ClaimTests.TESTS.get("claims_disabled_no_io").accept(h); }
 @GameTest public void claimsCapsAndCooldown(GameTestHelper h) { ClaimTests.TESTS.get("claims_caps_and_cooldown").accept(h); }
 @GameTest public void claimsDeedTwoPlayers(GameTestHelper h) { ClaimTests.TESTS.get("claims_deed_two_players").accept(h); }
 @GameTest public void claimsPolicyBoundaries(GameTestHelper h) { ClaimTests.TESTS.get("claims_policy_boundaries").accept(h); }
}

package com.simplebuilding.modules.simpletweaks;
import com.simplebuilding.modules.simpletweaks.claims.ClaimTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
public final class ClaimsGameTest {
 @GameTest public void claimsToolsBedHeadFootprint(GameTestHelper h) { ClaimTests.TESTS.get("claims_tools_bed_head_footprint").accept(h); }
 @GameTest public void claimsFollowupCrafter(GameTestHelper h) { ClaimTests.TESTS.get("claims_followup_crafter").accept(h); }
 @GameTest public void claimsFollowupCopperGolem(GameTestHelper h) { ClaimTests.TESTS.get("claims_followup_copper_golem").accept(h); }
 @GameTest public void claimsFollowupLightning(GameTestHelper h) { ClaimTests.TESTS.get("claims_followup_lightning").accept(h); }
 @GameTest public void claimsEnvironmentNaturalDamage(GameTestHelper h) { ClaimTests.TESTS.get("claims_environment_natural_damage").accept(h); }
 @GameTest public void claimsEnvironmentExplosionMultipart(GameTestHelper h) { ClaimTests.TESTS.get("claims_environment_explosion_multipart").accept(h); }
 @GameTest public void claimsEnvironmentIndirectCloud(GameTestHelper h) { ClaimTests.TESTS.get("claims_environment_indirect_cloud").accept(h); }
 @GameTest public void claimsEnvironmentConnectedPistons(GameTestHelper h) { ClaimTests.TESTS.get("claims_environment_connected_pistons").accept(h); }
 @GameTest public void claimsEnvironmentCustomPistons(GameTestHelper h) { ClaimTests.TESTS.get("claims_environment_custom_pistons").accept(h); }
 @GameTest public void claimsEnvironmentCustomHoppers(GameTestHelper h) { ClaimTests.TESTS.get("claims_environment_custom_hoppers").accept(h); }
 @GameTest public void claimsEnvironmentAttractor(GameTestHelper h) { ClaimTests.TESTS.get("claims_environment_attractor").accept(h); }
 @GameTest public void claimsEnvironmentDisabled(GameTestHelper h) { ClaimTests.TESTS.get("claims_environment_disabled").accept(h); }
 @GameTest public void claimsEnvironmentPickup(GameTestHelper h) { ClaimTests.TESTS.get("claims_environment_pickup").accept(h); }
 @GameTest public void claimsEnvironmentExplosionFire(GameTestHelper h) { ClaimTests.TESTS.get("claims_environment_explosion_fire").accept(h); }
 @GameTest public void claimsEnvironmentFluidPiston(GameTestHelper h) { ClaimTests.TESTS.get("claims_environment_fluid_piston").accept(h); }
 @GameTest public void claimsEnvironmentHopper(GameTestHelper h) { ClaimTests.TESTS.get("claims_environment_hopper").accept(h); }
 @GameTest public void claimsEnvironmentProjectiles(GameTestHelper h) { ClaimTests.TESTS.get("claims_environment_projectiles").accept(h); }
 @GameTest public void claimsEnvironmentDispenser(GameTestHelper h) { ClaimTests.TESTS.get("claims_environment_dispenser").accept(h); }

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

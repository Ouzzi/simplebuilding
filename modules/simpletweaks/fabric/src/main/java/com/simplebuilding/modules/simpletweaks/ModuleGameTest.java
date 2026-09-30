package com.simplebuilding.modules.simpletweaks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
public final class ModuleGameTest {
 @GameTest public void bootAndNoDuplicateRegistrations(GameTestHelper h) { CompatibilityTests.TESTS.get("boot_and_no_duplicate_registrations").accept(h); }
 @GameTest public void everyLegacyRegistryLookup(GameTestHelper h) { CompatibilityTests.TESTS.get("every_legacy_registry_lookup").accept(h); }
 @GameTest public void everyOldItemDecodesAndSavesCanonicalId(GameTestHelper h) { CompatibilityTests.TESTS.get("every_old_item_decodes_and_saves_canonical_id").accept(h); }
 @GameTest public void oldStackedPadsKeepTheirCount(GameTestHelper h) { CompatibilityTests.TESTS.get("old_stacked_pads_keep_their_count").accept(h); }
 @GameTest public void everyOldBlockStateDecodes(GameTestHelper h) { CompatibilityTests.TESTS.get("every_old_block_state_decodes").accept(h); }
 @GameTest public void everyOldBlockEntityDecodesAndPreservesOwner(GameTestHelper h) { CompatibilityTests.TESTS.get("every_old_block_entity_decodes_and_preserves_owner").accept(h); }
 @GameTest public void oldLaunchpadPreservesStoredCharges(GameTestHelper h) { CompatibilityTests.TESTS.get("old_launchpad_preserves_stored_charges").accept(h); }
 @GameTest public void oldElytraComponentsDecodeAndSaveCanonicalIds(GameTestHelper h) { CompatibilityTests.TESTS.get("old_elytra_components_decode_and_save_canonical_ids").accept(h); }
 @GameTest public void deedCustomDataSurvivesWithoutClaimAuthority(GameTestHelper h) { CompatibilityTests.TESTS.get("deed_custom_data_survives_without_claim_authority").accept(h); }
 @GameTest public void legacyItemsWorkInSimplebuildingStorage(GameTestHelper h) { CompatibilityTests.TESTS.get("legacy_items_work_in_simplebuilding_storage").accept(h); }
 @GameTest public void unknownNamesAndWrongRegistriesAreRefused(GameTestHelper h) { CompatibilityTests.TESTS.get("unknown_names_and_wrong_registries_are_refused").accept(h); }
 @GameTest public void claimsAndOldCommandsAreNotRegistered(GameTestHelper h) { CompatibilityTests.TESTS.get("claims_and_old_commands_are_not_registered").accept(h); }
}

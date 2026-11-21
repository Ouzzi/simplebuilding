package com.simplebuilding.modules.simpletweaks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
public final class ModuleGameTest {
 /** N124: the Mod Menu settings screen names the title, every category and every option (no raw or humanized paths), in EN (loaded language) and DE (the jar's de_de.json), server and client config. */
 @GameTest(maxTicks=100) public void configLabels(GameTestHelper h){var missing=com.simplelib.api.config.ConfigScreenModel.missingLabels("simpletweaks"); h.assertTrue(missing.isEmpty(), "config screen without an English name: "+missing); java.util.List<String> de=new java.util.ArrayList<>(); try(var in=ModuleGameTest.class.getResourceAsStream("/assets/simpletweaks/lang/de_de.json")){h.assertTrue(in!=null, "no de_de.json"); var lang=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject(); for(boolean client:new boolean[]{false,true}){var spec=com.simplelib.api.config.ConfigCommands.spec("simpletweaks", client); if(spec!=null) de.addAll(com.simplelib.api.config.ConfigScreenModel.missingLabels(spec, lang::has));}}catch(java.io.IOException e){throw new RuntimeException(e);} h.assertTrue(de.isEmpty(), "config screen without a German name: "+de); h.succeed();}
 @GameTest public void configCommand(GameTestHelper h) { com.simplelib.test.ConfigCommandTests.module(h, "simpletweaks", false); }
 @GameTest public void guideBook(GameTestHelper h) { CompatibilityTests.TESTS.get("guide_book").accept(h); }
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
 @GameTest public void discoverRecipeUnlocks(GameTestHelper h) { com.simplebuilding.modules.simpletweaks.discover.TweaksDiscover.recipeUnlocks(h); }
 @GameTest public void discoverBehaviourHints(GameTestHelper h) { com.simplebuilding.modules.simpletweaks.discover.TweaksDiscover.behaviourHints(h); }
 @GameTest public void jeiDeedCopy(GameTestHelper h) { com.simplebuilding.modules.simpletweaks.deed.DeedJeiTests.deedCopy(h); }
}

package com.simplebuilding.modules.simpleshops;

import com.simpleshops.test.ShopsTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link ShopsTests}, all on the 15 x 12 x 15 arena. */
public final class ModuleGameTest {
    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void configDefaults(GameTestHelper h) {
        ShopsTests.ALL.get("config_defaults").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void registered(GameTestHelper h) {
        ShopsTests.ALL.get("registered").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void recipes(GameTestHelper h) {
        ShopsTests.ALL.get("recipes").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void placeNeedsSalesBlock(GameTestHelper h) {
        ShopsTests.ALL.get("place_needs_sales_block").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void ownerSetFromPlacer(GameTestHelper h) {
        ShopsTests.ALL.get("owner_set_from_placer").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void strangerCannotBreak(GameTestHelper h) {
        ShopsTests.ALL.get("stranger_cannot_break").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void ownerBreakDropsOnceNoTemplates(GameTestHelper h) {
        ShopsTests.ALL.get("owner_break_drops_once_no_templates").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void breakOtherWaysDropsOnce(GameTestHelper h) {
        ShopsTests.ALL.get("break_other_ways_drops_once").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void tntCreeperWitherDragonResistance(GameTestHelper h) {
        ShopsTests.ALL.get("tnt_creeper_wither_dragon_resistance").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void pistonBlocked(GameTestHelper h) {
        ShopsTests.ALL.get("piston_blocked").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 120)
    public void fluidDoesNotReplace(GameTestHelper h) {
        ShopsTests.ALL.get("fluid_does_not_replace").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void hopperDispenserGolemCannotAccess(GameTestHelper h) {
        ShopsTests.ALL.get("hopper_dispenser_golem_cannot_access").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void transferLookupNull(GameTestHelper h) {
        ShopsTests.ALL.get("transfer_lookup_null").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void limitsPerPlayer(GameTestHelper h) {
        ShopsTests.ALL.get("limits_per_player").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void subModSwitch(GameTestHelper h) {
        ShopsTests.ALL.get("sub_mod_switch").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void claimsFreeUseReachesTheShop(GameTestHelper h) {
        ShopsTests.ALL.get("claims_free_use_reaches_the_shop").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void priceSlotClicks(GameTestHelper h) {
        ShopsTests.ALL.get("price_slot_clicks").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void priceRowLayout(GameTestHelper h) {
        ShopsTests.ALL.get("price_row_layout").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void templateNotConsumed(GameTestHelper h) {
        ShopsTests.ALL.get("template_not_consumed").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void tradeOk(GameTestHelper h) {
        ShopsTests.ALL.get("trade_ok").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void tradeAtomicFailures(GameTestHelper h) {
        ShopsTests.ALL.get("trade_atomic_failures").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void tradeExactVsComponents(GameTestHelper h) {
        ShopsTests.ALL.get("trade_exact_vs_components").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void lockSecondPlayer(GameTestHelper h) {
        ShopsTests.ALL.get("lock_second_player").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void lockDisconnectRelease(GameTestHelper h) {
        ShopsTests.ALL.get("lock_disconnect_release").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void lockStaleTakeover(GameTestHelper h) {
        ShopsTests.ALL.get("lock_stale_takeover").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void idleTimeoutCloses(GameTestHelper h) {
        ShopsTests.ALL.get("idle_timeout_closes").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void chunkUnloadCloses(GameTestHelper h) {
        ShopsTests.ALL.get("chunk_unload_closes").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void buyerSlotsReturnedOnClose(GameTestHelper h) {
        ShopsTests.ALL.get("buyer_slots_returned_on_close").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void revisionMismatchRejected(GameTestHelper h) {
        ShopsTests.ALL.get("revision_mismatch_rejected").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void stockMenu(GameTestHelper h) {
        ShopsTests.ALL.get("stock_menu").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 400)
    public void dupeInvariant(GameTestHelper h) {
        ShopsTests.ALL.get("dupe_invariant").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void guideBook(GameTestHelper h) {
        ShopsTests.ALL.get("guide_book").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void discoverRecipeUnlocks(GameTestHelper h) {
        ShopsTests.ALL.get("discover_recipe_unlocks").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void discoverBehaviourHints(GameTestHelper h) {
        ShopsTests.ALL.get("discover_behaviour_hints").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 200)
    public void servershopCommandsRights(GameTestHelper h) {
        ShopsTests.ALL.get("servershop_commands_rights").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 200)
    public void servershopBuyUnlimited(GameTestHelper h) {
        ShopsTests.ALL.get("servershop_buy_unlimited").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 200)
    public void servershopProtection(GameTestHelper h) {
        ShopsTests.ALL.get("servershop_protection").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 200)
    public void servershopPersistence(GameTestHelper h) {
        ShopsTests.ALL.get("servershop_persistence").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void storageRegisteredProtected(GameTestHelper h) {
        ShopsTests.ALL.get("storage_registered_protected").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void storagePlacementRules(GameTestHelper h) {
        ShopsTests.ALL.get("storage_placement_rules").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 120)
    public void storageChainFlow(GameTestHelper h) {
        ShopsTests.ALL.get("storage_chain_flow").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void storageFullChainLosesNothing(GameTestHelper h) {
        ShopsTests.ALL.get("storage_full_chain_loses_nothing").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void storageTradeDepositsIntoChain(GameTestHelper h) {
        ShopsTests.ALL.get("storage_trade_deposits_into_chain").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void storageOwnerChangeBreaksChain(GameTestHelper h) {
        ShopsTests.ALL.get("storage_owner_change_breaks_chain").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void storageUnloadedOrRemoved(GameTestHelper h) {
        ShopsTests.ALL.get("storage_unloaded_or_removed").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void storageRoundtripNoDupe(GameTestHelper h) {
        ShopsTests.ALL.get("storage_roundtrip_no_dupe").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void storageBreakAndMenuRules(GameTestHelper h) {
        ShopsTests.ALL.get("storage_break_and_menu_rules").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void storageExplosionPistonHopper(GameTestHelper h) {
        ShopsTests.ALL.get("storage_explosion_piston_hopper").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void devBuyerView(GameTestHelper h) {
        ShopsTests.ALL.get("dev_buyer_view").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void ownerScreensLayouts(GameTestHelper h) {
        ShopsTests.ALL.get("owner_screens_layouts").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void storageEmptyGoodsAndDisconnect(GameTestHelper h) {
        ShopsTests.ALL.get("storage_empty_goods_and_disconnect").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 400)
    public void storageDupeFuzz(GameTestHelper h) {
        ShopsTests.ALL.get("storage_dupe_fuzz").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void recipesEnderPearl(GameTestHelper h) {
        ShopsTests.ALL.get("recipes_ender_pearl").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void noInsertEveryClickType(GameTestHelper h) {
        ShopsTests.ALL.get("no_insert_every_click_type").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 120)
    public void noInsertHoppers(GameTestHelper h) {
        ShopsTests.ALL.get("no_insert_hoppers").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void enderMigrationLossless(GameTestHelper h) {
        ShopsTests.ALL.get("ender_migration_lossless").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void enderCopiedDataIsEmpty(GameTestHelper h) {
        ShopsTests.ALL.get("ender_copied_data_is_empty").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void enderBreakDeletesEntry(GameTestHelper h) {
        ShopsTests.ALL.get("ender_break_deletes_entry").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void enderOrphanKeptAndCleared(GameTestHelper h) {
        ShopsTests.ALL.get("ender_orphan_kept_and_cleared").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void enderWorldDataRoundtrip(GameTestHelper h) {
        ShopsTests.ALL.get("ender_world_data_roundtrip").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void terminalRegistered(GameTestHelper h) {
        ShopsTests.ALL.get("terminal_registered").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void terminalRemoteBuyEqualsOnSite(GameTestHelper h) {
        ShopsTests.ALL.get("terminal_remote_buy_equals_on_site").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void terminalNeverLoadsChunks(GameTestHelper h) {
        ShopsTests.ALL.get("terminal_never_loads_chunks").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void mapShowsUnloadedPublicShops(GameTestHelper h) {
        ShopsTests.ALL.get("map_shows_unloaded_public_shops").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void terminalLockSharedWithSite(GameTestHelper h) {
        ShopsTests.ALL.get("terminal_lock_shared_with_site").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 120)
    public void terminalTakeDelivery(GameTestHelper h) {
        ShopsTests.ALL.get("terminal_take_delivery").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void terminalPrivateAndDimension(GameTestHelper h) {
        ShopsTests.ALL.get("terminal_private_and_dimension").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void terminalRateLimitAndBadRequests(GameTestHelper h) {
        ShopsTests.ALL.get("terminal_rate_limit_and_bad_requests").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 100)
    public void terminalCollectAndPrices(GameTestHelper h) {
        ShopsTests.ALL.get("terminal_collect_and_prices").accept(h);
    }

    @GameTest(structure = "simpleshops:arena", maxTicks = 400)
    public void terminalDupeFuzz(GameTestHelper h) {
        ShopsTests.ALL.get("terminal_dupe_fuzz").accept(h);
    }
}

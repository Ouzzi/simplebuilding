package com.simplebuilding.modules.simplemusic;

import com.simplemusic.test.MusicTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link MusicTests}, all on simplemusic:empty (8 x 8 x 8). */
public final class ModuleGameTest {
    @GameTest(structure = "simplemusic:empty", maxTicks = 100)
    public void configDefaults(GameTestHelper h) {
        MusicTests.ALL.get("config_defaults").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 100)
    public void registered(GameTestHelper h) {
        MusicTests.ALL.get("registered").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 100)
    public void soundsPolicy(GameTestHelper h) {
        MusicTests.ALL.get("sounds_policy").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 100)
    public void sheetSaveRules(GameTestHelper h) {
        MusicTests.ALL.get("sheet_save_rules").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 100)
    public void orchestrionBecomesJukebox(GameTestHelper h) {
        MusicTests.ALL.get("orchestrion_becomes_jukebox").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 100)
    public void examplesParse(GameTestHelper h) {
        MusicTests.ALL.get("examples_parse").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 100)
    public void toneVolume(GameTestHelper h) {
        MusicTests.ALL.get("tone_volume").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 100)
    public void toneLimit(GameTestHelper h) {
        MusicTests.ALL.get("tone_limit").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 100)
    public void uploadModerationDownload(GameTestHelper h) {
        MusicTests.ALL.get("upload_moderation_download").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 100)
    public void uploadWorkerDelivers(GameTestHelper h) {
        MusicTests.ALL.get("upload_worker_delivers").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 60)
    public void uploadAbandonedExpires(GameTestHelper h) {
        MusicTests.ALL.get("upload_abandoned_expires").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 100)
    public void uploadRejectsBadChunks(GameTestHelper h) {
        MusicTests.ALL.get("upload_rejects_bad_chunks").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 60)
    public void jukeboxCustomDisc(GameTestHelper h) {
        MusicTests.ALL.get("jukebox_custom_disc").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 100)
    public void guideBook(GameTestHelper h) {
        MusicTests.ALL.get("guide_book").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 100)
    public void discoverRecipeUnlocks(GameTestHelper h) {
        MusicTests.ALL.get("discover_recipe_unlocks").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 100)
    public void discoverBehaviourHints(GameTestHelper h) {
        MusicTests.ALL.get("discover_behaviour_hints").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 100)
    public void discoverFirstPickupTips(GameTestHelper h) {
        MusicTests.ALL.get("discover_first_pickup_tips").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 100)
    public void pocketRecipe(GameTestHelper h) {
        MusicTests.ALL.get("pocket_recipe").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 100)
    public void pocketInsertRemove(GameTestHelper h) {
        MusicTests.ALL.get("pocket_insert_remove").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 60)
    public void pocketPlayPause(GameTestHelper h) {
        MusicTests.ALL.get("pocket_play_pause").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 240)
    public void pocketStops(GameTestHelper h) {
        MusicTests.ALL.get("pocket_stops").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 100)
    public void pocketPlaceBreak(GameTestHelper h) {
        MusicTests.ALL.get("pocket_place_break").accept(h);
    }

    @GameTest(structure = "simplemusic:empty")
    public void pocketKeepsReadableDiscs(GameTestHelper h) {
        MusicTests.ALL.get("pocket_keeps_readable_discs").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 40)
    public void pocketPlaceWhilePlaying(GameTestHelper h) {
        MusicTests.ALL.get("pocket_place_while_playing").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 60)
    public void pocketBlockPlay(GameTestHelper h) {
        MusicTests.ALL.get("pocket_block_play").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 200)
    public void pressSheetStays(GameTestHelper h) {
        MusicTests.ALL.get("press_sheet_stays").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 100)
    public void pressRules(GameTestHelper h) {
        MusicTests.ALL.get("press_rules").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 100)
    public void cartographyCopiesSheet(GameTestHelper h) {
        MusicTests.ALL.get("cartography_copies_sheet").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 40)
    public void subModSwitch(GameTestHelper h) {
        MusicTests.ALL.get("sub_mod_switch").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 60)
    public void pocketScrollRemove(GameTestHelper h) {
        MusicTests.ALL.get("pocket_scroll_remove").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 80)
    public void pocketInventoryContinuity(GameTestHelper h) {
        MusicTests.ALL.get("pocket_inventory_continuity").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 60)
    public void pocketDropContinues(GameTestHelper h) {
        MusicTests.ALL.get("pocket_drop_continues").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 240)
    public void pocketBreakContinues(GameTestHelper h) {
        MusicTests.ALL.get("pocket_break_continues").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 100)
    public void pocketRedstone(GameTestHelper h) {
        MusicTests.ALL.get("pocket_redstone").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 100)
    public void pocketSounds(GameTestHelper h) {
        MusicTests.ALL.get("pocket_sounds").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 100)
    public void pressLayout(GameTestHelper h) {
        MusicTests.ALL.get("press_layout").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 100)
    public void musicDiscs(GameTestHelper h) {
        MusicTests.ALL.get("music_discs").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 100)
    public void musicDiscLoot(GameTestHelper h) {
        MusicTests.ALL.get("music_disc_loot").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 100)
    public void noteGridRoundTrip(GameTestHelper h) {
        MusicTests.ALL.get("note_grid_round_trip").accept(h);
    }
}

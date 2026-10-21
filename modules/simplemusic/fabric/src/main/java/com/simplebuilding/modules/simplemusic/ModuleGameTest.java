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

    @GameTest(structure = "simplemusic:empty", maxTicks = 60)
    public void orchestrionPlaysSteps(GameTestHelper h) {
        MusicTests.ALL.get("orchestrion_plays_steps").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 80)
    public void orchestrionRedstone(GameTestHelper h) {
        MusicTests.ALL.get("orchestrion_redstone").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 60)
    public void orchestrionPlaylist(GameTestHelper h) {
        MusicTests.ALL.get("orchestrion_playlist").accept(h);
    }

    @GameTest(structure = "simplemusic:empty", maxTicks = 100)
    public void orchestrionPopOut(GameTestHelper h) {
        MusicTests.ALL.get("orchestrion_pop_out").accept(h);
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

    @GameTest(structure = "simplemusic:empty", maxTicks = 40)
    public void pocketStops(GameTestHelper h) {
        MusicTests.ALL.get("pocket_stops").accept(h);
    }
}

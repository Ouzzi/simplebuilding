package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public final class JukeboxPlaylistGameTest {
    @GameTest(maxTicks = 40)
    public void emptyJukeboxTakesFirstDiscOfTheChest(GameTestHelper helper) {
        JukeboxPlaylistTests.emptyJukeboxTakesFirstDiscOfTheChest(helper);
    }
    @GameTest(maxTicks = 40)
    public void finishedDiscSwapsWithTheNextOne(GameTestHelper helper) {
        JukeboxPlaylistTests.finishedDiscSwapsWithTheNextOne(helper);
    }
    @GameTest
    public void discsPlayInSlotOrderAndWrapAround(GameTestHelper helper) {
        JukeboxPlaylistTests.discsPlayInSlotOrderAndWrapAround(helper);
    }
    @GameTest
    public void lonelyDiscLoops(GameTestHelper helper) {
        JukeboxPlaylistTests.lonelyDiscLoops(helper);
    }
    @GameTest
    public void nothingHappensWithoutPlaylist(GameTestHelper helper) {
        JukeboxPlaylistTests.nothingHappensWithoutPlaylist(helper);
    }
    @GameTest
    public void fullChestStillSwaps(GameTestHelper helper) {
        JukeboxPlaylistTests.fullChestStillSwaps(helper);
    }
    @GameTest
    public void ejectingSkipsToTheNextDisc(GameTestHelper helper) {
        JukeboxPlaylistTests.ejectingSkipsToTheNextDisc(helper);
    }
    @GameTest
    public void playlistPositionIsSavedWithTheJukebox(GameTestHelper helper) {
        JukeboxPlaylistTests.playlistPositionIsSavedWithTheJukebox(helper);
    }
    @GameTest
    public void modPartIsActiveOnlyWithoutVanillasOwn(GameTestHelper helper) {
        JukeboxPlaylistTests.modPartIsActiveOnlyWithoutVanillasOwn(helper);
    }
    @GameTest
    public void amplifierCarriesTheSwappedDisc(GameTestHelper helper) {
        JukeboxPlaylistTests.amplifierCarriesTheSwappedDisc(helper);
    }
}

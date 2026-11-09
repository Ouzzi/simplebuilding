package com.simplemusic.test;

import com.simplelib.api.RecipeViewer;
import com.simplelib.compat.jei.InWorldUpgrade;
import com.simplemusic.SimpleMusic;
import com.simplemusic.registry.MusicRegistry;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;

/** JEI24: the sheet copy in "Special Ways", the disc press in its own tab (b-rel13). */
public final class MusicJeiTests {
    private MusicJeiTests() {
    }

    public static void pressAndCopy(GameTestHelper h) {
        List<InWorldUpgrade> entries = RecipeViewer.special(SimpleMusic.MOD_ID);
        List<InWorldUpgrade> press = RecipeViewer.station(SimpleMusic.MOD_ID); // b-rel13: the press has its own tab
        h.assertTrue(press.stream().anyMatch(e -> e.id().equals("simplemusic/disc_press") && e.output().is(MusicRegistry.CUSTOM_DISC)
                && e.tools().contains(MusicRegistry.DISC_PRESS_ITEM)), "disc press: " + press);
        h.assertTrue(press.stream().anyMatch(e -> e.id().equals("simplemusic/disc_press_dyed") && e.inputs().size() == 3
                && e.inputs().get(2).items().size() == 16), "disc press with dye: " + press);
        h.assertTrue(RecipeViewer.stations().stream().anyMatch(st -> st.item().get() == MusicRegistry.DISC_PRESS_ITEM), "disc press tab");
        h.assertTrue(entries.stream().anyMatch(e -> e.id().equals("simplemusic/sheet_copy") && e.output().is(MusicRegistry.SHEET_MUSIC)), "sheet copy");
        h.assertTrue(RecipeViewer.info(SimpleMusic.MOD_ID).size() == com.simplemusic.disc.MusicDiscs.DISCS.size(), "an info page per structure disc");
        h.succeed();
    }
}

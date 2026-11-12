package com.simplemusic.test;

import com.simplelib.api.RecipeViewer;
import com.simplelib.compat.jei.InWorldUpgrade;
import com.simplemusic.SimpleMusic;
import com.simplemusic.registry.MusicRegistry;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;

/** JEI24: the sheet copy in "Special Ways", the disc press in its own tab (b-rel13), turning sided discs over (a-shopmusic37b). */
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
        long sided = com.simplemusic.disc.MusicDiscs.DISCS.stream().filter(com.simplemusic.disc.MusicDiscs.Disc::sided).count();
        h.assertTrue(RecipeViewer.info(SimpleMusic.MOD_ID).size() == com.simplemusic.disc.MusicDiscs.DISCS.size() + sided,
                "an info page per disc and per B-side");
        // a-shopmusic37b: turning a sided disc over is shown as a special way, A-side in, B-side out, no tool
        for (int i = 0; i < com.simplemusic.disc.MusicDiscs.DISCS.size(); i++) {
            var disc = com.simplemusic.disc.MusicDiscs.DISCS.get(i);
            if (!disc.sided()) continue;
            var a = com.simplemusic.disc.MusicDiscs.ITEMS.get(i);
            var b = com.simplemusic.disc.MusicDiscs.bSide(disc);
            h.assertTrue(entries.stream().anyMatch(e -> e.id().equals("simplemusic/turn_" + disc.id()) && e.tools().isEmpty()
                    && e.inputs().get(0).items().contains(a) && e.output().is(b)), "turn over " + disc.id());
        }
        h.succeed();
    }
}

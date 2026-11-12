package com.simplebuilding.modules.simpleshops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.simpleshops.logic.ShopMusicPick;
import com.simpleshops.logic.ShopMusicPick.Shop;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Which shop music a listener hears (a-shopmusic37, plain logic, no game classes). <b>What breaks this test:</b> a farther shop
 * winning over a nearer one, a shop out of reach playing, the playing group flapping on the border, shops with the same disc
 * not forming one group (one sound, at the member nearest to the listener), a boost above 2.5 times, or a wrong loop offset.
 */
class ShopMusicPickTest {
    private static Shop shop(double x, String song, long start) {
        return new Shop(x, 64.5, 0.5, song, start, 70.0F, 24);
    }

    @Test
    void nearestShopInReachWins() {
        Shop bossa = shop(0.5, "simplemusic:music_disc.browsing_bossa", 100);
        Shop lounge = shop(30.5, "simplemusic:music_disc.coin_lounge", 200);
        ShopMusicPick.Pick near = ShopMusicPick.pick(List.of(bossa, lounge), 10.5, 64.5, 0.5, null);
        assertNotNull(near);
        assertSame(bossa, near.at(), "10 blocks to the bossa shop, 20 to the lounge shop");
        ShopMusicPick.Pick other = ShopMusicPick.pick(List.of(bossa, lounge), 25.5, 64.5, 0.5, null);
        assertSame(lounge, other.at(), "walking on: the lounge shop is nearer");
        assertNull(ShopMusicPick.pick(List.of(bossa), 60.5, 64.5, 0.5, null), "out of reach (24 blocks): silence");
        assertNull(ShopMusicPick.pick(List.of(), 0.5, 64.5, 0.5, null), "no shops: silence");
    }

    @Test
    void playingGroupKeepsTheMusicOnTheBorder() {
        Shop a = shop(0.5, "a", 0), b = shop(20.5, "b", 0);
        // Listener at 10.9: b is 0.8 blocks nearer - not enough to take over from the playing a.
        assertSame(a, ShopMusicPick.pick(List.of(a, b), 10.9, 64.5, 0.5, a).at(), "hysteresis keeps a");
        assertSame(b, ShopMusicPick.pick(List.of(a, b), 10.9, 64.5, 0.5, null).at(), "without a playing group the nearer wins");
        assertSame(b, ShopMusicPick.pick(List.of(a, b), 12.5, 64.5, 0.5, a).at(), "4 blocks nearer: b takes over");
    }

    @Test
    void sameDiscSameStartIsOneAmplifiedGroup() {
        String song = "simplemusic:music_disc.window_shopping";
        Shop s1 = shop(0.5, song, 50), s2 = shop(8.5, song, 50), s3 = shop(16.5, song, 50), s4 = shop(24.5, song, 50), s5 = shop(32.5, song, 50);
        Shop late = shop(4.5, song, 900);
        ShopMusicPick.Pick pick = ShopMusicPick.pick(List.of(s1, s2, s3, s4, s5, late), 15.5, 64.5, 0.5, null);
        assertEquals(5, pick.group().size(), "one group: same song, same start (the late one is its own group)");
        assertSame(s3, pick.at(), "the sound sits at the member nearest to the listener");
        assertEquals(24 * 2.5, pick.reach(), 1e-9, "four further shops count three times 50 %: at most 2.5 times");
        assertEquals(1.0, ShopMusicPick.factor(1), 1e-9);
        assertEquals(1.5, ShopMusicPick.factor(2), 1e-9);
        assertEquals(ShopMusicPick.MAX_FACTOR, ShopMusicPick.factor(50), 1e-9);
        // A lone shop is out of reach at 40 blocks, the amplified group is not.
        assertNull(ShopMusicPick.pick(List.of(s1), 40.5, 64.5, 0.5, null));
        assertNotNull(ShopMusicPick.pick(List.of(s1, s2, s3), 52.5, 64.5, 0.5, null), "reach 24 x 2 = 48 from the member at 16.5");
    }

    @Test
    void gainAndLoopOffset() {
        assertEquals(1.0F, ShopMusicPick.gain(0.0, 24.0), 1e-6);
        assertEquals(0.5F, ShopMusicPick.gain(12.0, 24.0), 1e-6);
        assertEquals(0.0F, ShopMusicPick.gain(24.0, 24.0), 1e-6);
        assertEquals(0, ShopMusicPick.offsetSeconds(1000, 1000, 70.0F));
        assertEquals(10, ShopMusicPick.offsetSeconds(1000 + 200, 1000, 70.0F), "10 s into the loop");
        assertEquals(5, ShopMusicPick.offsetSeconds(1000 + 1400 + 100, 1000, 70.0F), "second round of a 70 s loop");
        assertTrue(ShopMusicPick.offsetSeconds(500, 1000, 70.0F) >= 0, "a start in the future still gives a valid offset");
        assertEquals(0, ShopMusicPick.offsetSeconds(1234, 0, 0.0F), "unknown length: from the start");
    }
}

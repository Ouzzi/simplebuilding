package com.simplemusic.disc;

/**
 * A music disc with an A- and a B-side (a-shopmusic37b). The turning itself (right click, cooldown, sound) is SimpleLib's
 * {@link com.simplelib.item.SidedDiscItem} (N47 M6); this only names Simple Music's side table ({@link MusicDiscs#turnOver}).
 */
public final class SidedDiscItem extends com.simplelib.item.SidedDiscItem {
    public SidedDiscItem(Properties properties) {
        super(properties, MusicDiscs::turnOver);
    }
}

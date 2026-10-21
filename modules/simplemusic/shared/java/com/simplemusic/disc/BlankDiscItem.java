package com.simplemusic.disc;

import net.minecraft.world.item.Item;

/**
 * Blank disc: the disc press (owner 21.10.) presses the music of a sheet onto it and turns it into a
 * {@link CustomDiscItem}. It no longer opens a window itself - uploads go through the sheet music editor.
 */
public class BlankDiscItem extends Item {
    public BlankDiscItem(Properties properties) {
        super(properties);
    }
}

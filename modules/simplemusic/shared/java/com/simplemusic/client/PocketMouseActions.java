package com.simplemusic.client;

import com.simplemusic.pocket.PocketJukeboxItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.BundleMouseActions;
import net.minecraft.world.inventory.Slot;

/**
 * The mouse wheel over a Pocket Jukebox in a slot selects a disc like Vanilla's bundle (PJ23-1): Vanilla's own handler,
 * which only matches {@code #minecraft:bundles} (a tag the jukebox stays out of, its dye recipes would turn it into a
 * bundle). Vanilla's select packet then works on the server as for a bundle; right click takes the selected disc out.
 */
public class PocketMouseActions extends BundleMouseActions {
    public PocketMouseActions(Minecraft minecraft) {
        super(minecraft);
    }

    @Override
    public boolean matches(Slot slot) {
        return slot.hasItem() && slot.getItem().getItem() instanceof PocketJukeboxItem;
    }
}

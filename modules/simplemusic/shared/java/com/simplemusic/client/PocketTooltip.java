package com.simplemusic.client;

import com.simplemusic.pocket.PocketContents;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;

/** Vanilla's bundle tooltip for a Pocket Jukebox; {@code ClientBundleTooltipMixin} scales its fill bar to 20 discs. */
public final class PocketTooltip extends ClientBundleTooltip {
    public PocketTooltip(PocketContents data) {
        super(data.contents());
    }
}

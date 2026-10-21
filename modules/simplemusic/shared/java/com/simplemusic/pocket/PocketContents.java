package com.simplemusic.pocket;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.component.BundleContents;

/** Tooltip data of a Pocket Jukebox: Vanilla's bundle grid, the fill bar counts discs out of {@link PocketJukeboxItem#CAPACITY}. */
public record PocketContents(BundleContents contents) implements TooltipComponent {}

package com.simplebuilding.neoforge;

import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/** Haengt die Baublock-Zeilen in den Tab "Simple Blocks" (Vanilla-Tabs bleiben unveraendert, IG31-3). */
public final class NeoForgeSearchTabPlacement {
    private NeoForgeSearchTabPlacement() {
    }

    public static void onBuildContents(BuildCreativeModeTabContentsEvent event) {
        com.simplebuilding.items.ModItemGroupsContent.contributeToSimpleBlocks(event.getTabKey(), event::accept);
    }
}

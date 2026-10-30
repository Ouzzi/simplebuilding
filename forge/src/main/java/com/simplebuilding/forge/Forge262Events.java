package com.simplebuilding.forge;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.level.BlockEvent;

/** Loader hooks for existing 26.2 creative placement and Breeze head abilities. */
public final class Forge262Events {
    private Forge262Events() {}

    public static void register() {
        BuildCreativeModeTabContentsEvent.BUS.addListener(Forge262Events::onBuildContents);
        // EventBus 7 cancellable listeners return true to prevent trampling.
        BlockEvent.FarmlandTrampleEvent.BUS.addListener(event ->
                !com.simplebuilding.tweaks.heads.HeadAbilities.tramplesFarmland(event.getEntity()));
    }

    private static void onBuildContents(BuildCreativeModeTabContentsEvent event) {
        com.simplebuilding.items.ModItemGroupsContent.contributeToSimpleBlocks(event.getTabKey(),
                (stack, visibility) -> event.getEntries().put(stack, visibility));
    }
}

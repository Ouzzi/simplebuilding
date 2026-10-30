package com.simplebuilding.forge;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.level.BlockEvent;

/** Main-line loader hooks absent from the deferred 26.2 Forge adapter. */
public final class Forge263Events {
    private Forge263Events() {}

    public static void register() {
        // Last, so module items (simplemobs: Shellker shell, Soulfire rod ...) that anchor on the same Vanilla items do not
        // slip between an anchor and the placement behind it.
        BuildCreativeModeTabContentsEvent.BUS.addListener(net.minecraftforge.eventbus.api.listener.Priority.LOWEST, Forge263Events::onBuildContents);
        // EventBus 7 cancellable listeners return true to prevent trampling.
        BlockEvent.FarmlandTrampleEvent.BUS.addListener(event ->
                !com.simplebuilding.tweaks.heads.HeadAbilities.tramplesFarmland(event.getEntity()));
    }

    private static void onBuildContents(BuildCreativeModeTabContentsEvent event) {
        com.simplebuilding.items.ModItemGroupsContent.contributeToSimpleBlocks(event.getTabKey(),
                (stack, visibility) -> event.getEntries().put(stack, visibility));
    }
}

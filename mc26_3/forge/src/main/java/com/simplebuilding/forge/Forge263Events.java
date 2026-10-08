package com.simplebuilding.forge;

import com.simplebuilding.items.SearchTabPlacement;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.level.BlockEvent;

/** Main-line loader hooks absent from the deferred 26.2 Forge adapter. */
public final class Forge263Events {
    private Forge263Events() {}

    public static void register() {
        BuildCreativeModeTabContentsEvent.BUS.addListener(Forge263Events::onBuildContents);
        // EventBus 7 cancellable listeners return true to prevent trampling.
        BlockEvent.FarmlandTrampleEvent.BUS.addListener(event ->
                !com.simplebuilding.tweaks.heads.HeadAbilities.tramplesFarmland(event.getEntity()));
    }

    private static void onBuildContents(BuildCreativeModeTabContentsEvent event) {
        if (!SearchTabPlacement.enabled() || !SearchTabPlacement.TABS.contains(event.getTabKey())) return;
        var entries = event.getEntries();
        var visibility = CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
        for (SearchTabPlacement.Placement placement : SearchTabPlacement.placementsIfEnabled(event.getTabKey())) {
            ItemStack anchor = new ItemStack(placement.anchor());
            if (!entries.contains(anchor)) continue;
            ItemStack previous = anchor;
            for (ItemStack stack : placement.stacks()) {
                if (entries.contains(stack)) continue;
                ItemStack entry = stack.copy();
                if (placement.before()) entries.putBefore(anchor, entry, visibility);
                else {
                    entries.putAfter(previous, entry, visibility);
                    previous = entry;
                }
            }
        }
    }
}

package com.simplebuilding.forge;

import com.simplebuilding.items.SearchTabPlacement;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
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
        if (!SearchTabPlacement.TABS.contains(event.getTabKey())) return;
        var entries = event.getEntries();
        var visibility = CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
        for (SearchTabPlacement.Placement placement : SearchTabPlacement.placements(event.getTabKey())) {
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

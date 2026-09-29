package com.simplebuilding.neoforge;

import com.simplebuilding.items.SearchTabPlacement;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/**
 * Setzt die Mod-Items zusaetzlich neben ihre Vanilla-Vorbilder in die Vanilla-Tabs, damit sie im Suchtab
 * dort stehen statt am Ende ({@link SearchTabPlacement}). NeoForge wirft bei fehlendem Anker oder schon
 * vorhandenem Stapel, darum wird beides vorher geprueft: eine Platzierung ohne Anker faellt weg, ein schon
 * vorhandener Stapel wird uebersprungen.
 */
public final class NeoForgeSearchTabPlacement {
    private NeoForgeSearchTabPlacement() {
    }

    public static void onBuildContents(BuildCreativeModeTabContentsEvent event) {
        if (!SearchTabPlacement.TABS.contains(event.getTabKey())) {
            return;
        }
        CreativeModeTab.TabVisibility visibility = CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
        for (SearchTabPlacement.Placement placement : SearchTabPlacement.placements(event.getTabKey())) {
            ItemStack anchor = new ItemStack(placement.anchor());
            if (!event.getParentEntries().contains(anchor) || !event.getSearchEntries().contains(anchor)) {
                continue;
            }
            ItemStack previous = anchor;
            for (ItemStack stack : placement.stacks()) {
                if (event.getParentEntries().contains(stack) || event.getSearchEntries().contains(stack)) {
                    continue;
                }
                ItemStack entry = stack.copy();
                if (placement.before()) {
                    event.insertBefore(anchor, entry, visibility);
                } else {
                    event.insertAfter(previous, entry, visibility);
                    previous = entry;
                }
            }
        }
    }
}

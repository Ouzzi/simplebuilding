package com.simplebuilding.util;

import com.simplebuilding.blocks.custom.PlacedEggBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * Eier ablegen (Besitzer 2026-10-02): Schleichen + Rechtsklick mit einem Vanilla-, blauen oder braunen Ei stellt es
 * aufrecht auf einen Boden - als Teil eines Kleinteil-Haeufchens ({@link PlacedSmallParts}, bis zu vier Teile auf
 * einem Fleck). Es gelten dieselben Server-Optionen wie fuer die Kleinteile ({@code server.features.placeVanillaItems},
 * {@code placeDisabledItems}).
 */
public final class PlacedEggs {
    private PlacedEggs() {
    }

    public static boolean isPlaceableEgg(ItemStack stack) {
        if (!com.simplebuilding.version.McVersion.SMALL_PLACEABLES || PlacedEggBlock.Egg.of(stack) == null) {
            return false;
        }
        var features = com.simplebuilding.config.ServerTuning.get().features;
        return features.placeVanillaItems && !PlacedTemplates.itemListed(features.placeDisabledItems, BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }
}

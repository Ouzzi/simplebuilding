package com.simplebuilding.util;

import com.simplebuilding.items.custom.ReinforcedBundleItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

public class BundleUtil {

    /**
     * Sucht nach einem Pfeil im Bundle.
     * @param bundle Das Bundle Item
     * @return Den gefundenen Pfeil-Stack oder ItemStack.EMPTY
     */
    public static ItemStack findArrow(ItemStack bundle) {
        if (!(bundle.getItem() instanceof ReinforcedBundleItem)) return ItemStack.EMPTY;

        BundleContentsComponent contents = bundle.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (contents == null) return ItemStack.EMPTY;

        for (ItemStack s : contents.iterate()) {
            // Wir prüfen auf Vanilla Pfeil-Typen. (Mod-Pfeile ggf. über Tags prüfen)
            if (s.getItem() == Items.ARROW || s.getItem() == Items.SPECTRAL_ARROW || s.getItem() == Items.TIPPED_ARROW) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Entfernt genau einen Pfeil aus dem Bundle.
     * @param bundle Das Bundle Item
     * @return true, wenn ein Pfeil entfernt wurde
     */
    public static boolean removeOneArrow(ItemStack bundle) {
        if (!(bundle.getItem() instanceof ReinforcedBundleItem)) return false;

        BundleContentsComponent contents = bundle.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) return false;

        List<ItemStack> newItems = new ArrayList<>();
        boolean foundAndRemoved = false;

        for (ItemStack s : contents.iterate()) {
            // Wir suchen den ersten Pfeil und verringern ihn
            if (!foundAndRemoved && (s.getItem() == Items.ARROW || s.getItem() == Items.SPECTRAL_ARROW || s.getItem() == Items.TIPPED_ARROW)) {
                ItemStack copy = s.copy();
                copy.decrement(1);

                if (!copy.isEmpty()) {
                    newItems.add(copy);
                }
                foundAndRemoved = true;
            } else {
                newItems.add(s.copy());
            }
        }

        if (foundAndRemoved) {
            bundle.set(DataComponentTypes.BUNDLE_CONTENTS, new BundleContentsComponent(newItems));
            return true;
        }
        return false;
    }
}
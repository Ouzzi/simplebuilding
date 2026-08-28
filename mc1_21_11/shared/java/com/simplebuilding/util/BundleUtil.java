package com.simplebuilding.util;

import com.simplebuilding.items.custom.ReinforcedBundleItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;

public class BundleUtil {

    public static ItemStack findArrow(ItemStack bundle) {
        if (!(bundle.getItem() instanceof ReinforcedBundleItem)) return ItemStack.EMPTY;

        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null) return ItemStack.EMPTY;

        for (ItemStack s : contents.itemsCopy()) {
            if (s.getItem() == Items.ARROW || s.getItem() == Items.SPECTRAL_ARROW || s.getItem() == Items.TIPPED_ARROW) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    public static boolean removeOneArrow(ItemStack bundle) {
        if (!(bundle.getItem() instanceof ReinforcedBundleItem)) return false;

        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) return false;

        List<ItemStack> newItems = new ArrayList<>();
        boolean foundAndRemoved = false;

        for (ItemStack s : contents.itemsCopy()) {
            if (!foundAndRemoved && (s.getItem() == Items.ARROW || s.getItem() == Items.SPECTRAL_ARROW || s.getItem() == Items.TIPPED_ARROW)) {
                ItemStack copy = s.copy();
                copy.shrink(1);

                if (!copy.isEmpty()) {
                    newItems.add(copy);
                }
                foundAndRemoved = true;
            } else {
                newItems.add(s);
            }
        }

        if (foundAndRemoved) {
            bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.copyOf(newItems)));
            return true;
        }
        return false;
    }
}

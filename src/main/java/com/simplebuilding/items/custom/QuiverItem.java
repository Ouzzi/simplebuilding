package com.simplebuilding.items.custom;

import com.simplebuilding.items.ModItems;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.ClickType;
import org.apache.commons.lang3.math.Fraction;

import java.util.ArrayList;
import java.util.List;

public class QuiverItem extends ReinforcedBundleItem {

    public QuiverItem(Settings settings) {
        super(settings);
    }

    // --- Interaction Logic Override (Only Arrows) ---

    @Override
    public boolean onStackClicked(ItemStack bundle, Slot slot, ClickType clickType, PlayerEntity player) {
        if (clickType == ClickType.LEFT && !slot.getStack().isEmpty()) {
            if (!slot.getStack().isIn(ItemTags.ARROWS)) return false;
        }
        return super.onStackClicked(bundle, slot, clickType, player);
    }

    @Override
    public boolean onClicked(ItemStack bundle, ItemStack cursorStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference) {
        if (clickType == ClickType.LEFT && !cursorStack.isEmpty()) {
            if (!cursorStack.isIn(ItemTags.ARROWS)) return false;
        }
        return super.onClicked(bundle, cursorStack, slot, clickType, player, cursorStackReference);
    }

    @Override
    public boolean tryInsertStackFromWorld(ItemStack bundle, ItemStack stackToInsert, PlayerEntity player) {
        if (!stackToInsert.isIn(ItemTags.ARROWS)) return false;
        return super.tryInsertStackFromWorld(bundle, stackToInsert, player);
    }

    // --- Capacity Logic (Fixed 1 Stack = 64 items) ---

    @Override
    protected Fraction getMaxCapacity(ItemStack stack, PlayerEntity player) {
        // Der Köcher hält genau 1 Stack (64 Items)
        return Fraction.ONE;
    }

    @Override
    protected Fraction getMaxCapacityForVisuals(ItemStack stack) {
        return Fraction.ONE;
    }

    // --- Helper Methods for Bow Mixin ---

    public static ItemStack findProjectileForBow(PlayerEntity player) {
        // 1. Check Offhand
        ItemStack offhand = player.getOffHandStack();
        if (offhand.getItem() instanceof QuiverItem) {
            ItemStack arrow = findFirstArrow(offhand);
            if (!arrow.isEmpty()) return arrow;
        }

        // 2. Check Chest Slot (EquipmentSlot.CHEST)
        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof QuiverItem) {
            ItemStack arrow = findFirstArrow(chest);
            if (!arrow.isEmpty()) return arrow;
        }

        // 3. Removed Inventory Scan (Quiver Enchantment removed)

        return ItemStack.EMPTY;
    }

    public static void consumeProjectileForBow(PlayerEntity player) {
        // 1. Offhand
        ItemStack offhand = player.getOffHandStack();
        if (offhand.getItem() instanceof QuiverItem) {
            if (tryConsumeArrow(offhand)) return;
        }

        // 2. Chest
        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof QuiverItem) {
            if (tryConsumeArrow(chest)) return;
        }
    }

    private static ItemStack findFirstArrow(ItemStack bundle) {
        BundleContentsComponent contents = bundle.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) return ItemStack.EMPTY;

        // Suche nach Pfeilen
        for (ItemStack s : contents.iterate()) {
            if (s.isIn(ItemTags.ARROWS)) return s;
        }
        return ItemStack.EMPTY;
    }

    private static boolean tryConsumeArrow(ItemStack bundle) {
        BundleContentsComponent contents = bundle.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) return false;

        List<ItemStack> newItems = new ArrayList<>();
        boolean found = false;

        for (ItemStack s : contents.iterate()) {
            if (!found && s.isIn(ItemTags.ARROWS)) {
                ItemStack copy = s.copy();
                copy.decrement(1);
                if (!copy.isEmpty()) {
                    newItems.add(copy);
                }
                found = true;
            } else {
                newItems.add(s.copy());
            }
        }

        if (found) {
            bundle.set(DataComponentTypes.BUNDLE_CONTENTS, new BundleContentsComponent(newItems));
            return true;
        }
        return false;
    }
}
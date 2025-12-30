package com.simplebuilding.items.custom;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ClickType;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.apache.commons.lang3.math.Fraction;

import java.util.ArrayList;
import java.util.List;

public class QuiverItem extends ReinforcedBundleItem {

    public QuiverItem(Settings settings) {
        super(settings);
    }

    // --- 1. Rechtsklick in die Luft (Angepasst an deine Version) ---
    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        // In deiner Version gibt 'use' nur ActionResult zurück.
        // PASS verhindert das Ausschütten.
        return ActionResult.PASS;
    }

    // --- 2. Rechtsklick auf einen Block ---
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        // PASS verhindert Interaktion mit Blöcken
        return ActionResult.PASS;
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

    // --- Capacity Logic (UPDATED FOR DRAWER) ---

    @Override
    protected Fraction getMaxCapacity(ItemStack stack, PlayerEntity player) {
        Fraction capacity = stack.isOf(ModItems.NETHERITE_QUIVER) ? Fraction.getFraction(2, 1) : Fraction.getFraction(1, 1);

        if (player == null || player.getEntityWorld() == null) return capacity;

        var registry = player.getEntityWorld().getRegistryManager();
        var enchantments = registry.getOrThrow(RegistryKeys.ENCHANTMENT);

        // 1. DRAWER Logic (Hinzugefügt!)
        var drawer = enchantments.getOptional(ModEnchantments.DRAWER);
        if (drawer.isPresent()) {
            int level = EnchantmentHelper.getLevel(drawer.get(), stack);
            if (level > 0) {
                // Formel: (8 + level) / 8 -> Gleiche Steigerung wie beim Bundle
                Fraction drawerBonus = Fraction.getFraction(16 + level, 8);
                capacity = capacity.multiplyBy(drawerBonus);
            }
        }

        // 2. Deep Pockets Logic
        var deepPockets = enchantments.getOptional(ModEnchantments.DEEP_POCKETS);
        if (deepPockets.isPresent()) {
            int level = EnchantmentHelper.getLevel(deepPockets.get(), stack);
            if (level == 1) capacity = capacity.multiplyBy(Fraction.getFraction(2, 1));
            if (level >= 2) capacity = capacity.multiplyBy(Fraction.getFraction(4, 1));
        }
        return capacity;
    }

    @Override
    protected Fraction getMaxCapacityForVisuals(ItemStack stack) {
        Fraction capacity = stack.isOf(ModItems.NETHERITE_QUIVER) ? Fraction.getFraction(2, 1) : Fraction.getFraction(1, 1);

        var enchantments = stack.getEnchantments();
        for (var entry : enchantments.getEnchantmentEntries()) {
            if (entry.getKey().getKey().isPresent()) {
                String id = entry.getKey().getKey().get().getValue().toString();

                // Deep Pockets
                if (id.contains("deep_pockets")) {
                    int level = entry.getIntValue();
                    if (level == 1) capacity = capacity.multiplyBy(Fraction.getFraction(2, 1));
                    if (level >= 2) capacity = capacity.multiplyBy(Fraction.getFraction(4, 1));
                }

                // Drawer (Hinzugefügt!)
                if (id.contains("drawer")) {
                    int level = entry.getIntValue();
                    if (level > 0) {
                        Fraction drawerBonus = Fraction.getFraction(16 + level, 8);
                        capacity = capacity.multiplyBy(drawerBonus);
                    }
                }
            }
        }
        return capacity;
    }

    // --- Helper Methods for Bow Mixin ---

    public static ItemStack findProjectileForBow(PlayerEntity player) {
        // 1. Offhand
        ItemStack offhand = player.getOffHandStack();
        if (offhand.getItem() instanceof QuiverItem) {
            ItemStack arrow = findFirstArrow(offhand);
            if (!arrow.isEmpty()) return arrow;
        }

        // 2. Chest Slot
        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof QuiverItem) {
            ItemStack arrow = findFirstArrow(chest);
            if (!arrow.isEmpty()) return arrow;
        }

        // 3. Inventar (NUR mit Constructors Touch)
        // Durchsuchen des gesamten Inventars, damit es überall funktioniert
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() instanceof QuiverItem) {
                if (hasConstructorsTouchEnchantment(stack, player)) {
                    ItemStack arrow = findFirstArrow(stack);
                    if (!arrow.isEmpty()) return arrow;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public static void consumeProjectileForBow(PlayerEntity player) {
        // Gleiche Reihenfolge wie beim Finden

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

        // 3. Inventar (mit Constructors Touch)
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() instanceof QuiverItem) {
                if (hasConstructorsTouchEnchantment(stack, player)) {
                    if (tryConsumeArrow(stack)) return;
                }
            }
        }
    }

    private static ItemStack findFirstArrow(ItemStack bundle) {
        BundleContentsComponent contents = bundle.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) return ItemStack.EMPTY;
        for (ItemStack s : contents.iterate()) {
            if (s.isIn(ItemTags.ARROWS)) return s.copy();
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
                if (!copy.isEmpty()) newItems.add(copy);
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

    private static boolean hasConstructorsTouchEnchantment(ItemStack stack, PlayerEntity player) {
        var registry = player.getEntityWorld().getRegistryManager();
        var enchantments = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
        var ct = enchantments.getOptional(ModEnchantments.CONSTRUCTORS_TOUCH);
        return ct.isPresent() && EnchantmentHelper.getLevel(ct.get(), stack) > 0;
    }
}
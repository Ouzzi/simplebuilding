package com.simplebuilding.items.custom;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import org.apache.commons.lang3.math.Fraction;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

import static com.simplebuilding.util.EnchantmentHelper.hasEnchantment;

public class QuiverItem extends ReinforcedBundleItem {

    public QuiverItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack bundle, Slot slot, ClickAction ClickAction, Player player) {
        if (ClickAction == ClickAction.PRIMARY && !slot.getItem().isEmpty()) {
            if (!slot.getItem().is(ItemTags.ARROWS)) return false;
        }
        return super.overrideStackedOnOther(bundle, slot, ClickAction, player);
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack bundle, ItemStack cursorStack, Slot slot, ClickAction ClickAction, Player player, SlotAccess cursorStackReference) {
        if (ClickAction == ClickAction.PRIMARY && !cursorStack.isEmpty()) {
            if (!cursorStack.is(ItemTags.ARROWS)) return false;
        }
        return super.overrideOtherStackedOnMe(bundle, cursorStack, slot, ClickAction, player, cursorStackReference);
    }

    @Override
    public boolean tryInsertStackFromWorld(ItemStack bundle, ItemStack stackToInsert, Player player) {
        if (!stackToInsert.is(ItemTags.ARROWS)) return false;
        return super.tryInsertStackFromWorld(bundle, stackToInsert, player);
    }

    @Override
    protected Fraction getTierCapacityMultiplier(Item item) {
        if (item == ModItems.ENDERITE_QUIVER) {
            return Fraction.getFraction(3, 1);
        }
        if (item == ModItems.NETHERITE_QUIVER) {
            return Fraction.getFraction(2, 1);
        }
        return Fraction.getFraction(1, 1);
    }

    /** Koecher bekommen den 1,5-Faktor des Buendels NICHT: 64 / 128 / 192 Pfeile. */
    @Override
    protected Fraction getBaseCapacity(Item item) {
        return getTierCapacityMultiplier(item);
    }

    @Override
    protected Fraction getMaxCapacity(ItemStack stack, Player player) {
        Fraction capacity = getBaseCapacity(stack.getItem());

        if (player == null || player.level() == null) return capacity;

        var registry = player.level().registryAccess();
        var enchantments = registry.lookupOrThrow(Registries.ENCHANTMENT);

        // 1. DRAWER Logic (Hinzugefügt!)
        var drawer = enchantments.get(ModEnchantments.DRAWER);
        if (drawer.isPresent()) {
            int level = EnchantmentHelper.getItemEnchantmentLevel(drawer.get(), stack);
            if (level > 0) {
                // Formel: (8 + level) / 8 -> Gleiche Steigerung wie beim Bundle
                Fraction drawerBonus = Fraction.getFraction(16 + level, 8);
                capacity = capacity.multiplyBy(drawerBonus);
            }
        }

        // 2. Deep Pockets Logic
        var deepPockets = enchantments.get(ModEnchantments.DEEP_POCKETS);
        if (deepPockets.isPresent()) {
            int level = EnchantmentHelper.getItemEnchantmentLevel(deepPockets.get(), stack);
            if (level == 1) capacity = capacity.multiplyBy(Fraction.getFraction(2, 1));
            if (level >= 2) capacity = capacity.multiplyBy(Fraction.getFraction(4, 1));
        }
        return capacity;
    }

    @Override
    protected Fraction getMaxCapacityForVisuals(ItemStack stack) {
        Fraction capacity = getBaseCapacity(stack.getItem());

        var enchantments = stack.getEnchantments();
        for (var entry : enchantments.entrySet()) {
            if (entry.getKey().unwrapKey().isPresent()) {
                String id = entry.getKey().unwrapKey().get().identifier().toString();

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

    public static ItemStack findProjectileForBow(Player player) {
        // 1. Offhand
        ItemStack arrow = findArrowInQuiver(player.getOffhandItem());
        if (!arrow.isEmpty()) return arrow;

        // 2. Chest Slot
        arrow = findArrowInQuiver(player.getItemBySlot(EquipmentSlot.CHEST));
        if (!arrow.isEmpty()) return arrow;

        // 3. Hotbar (ohne Constructors Touch)
        for (int i = 0; i < 9; i++) {
            arrow = findArrowInQuiver(player.getInventory().getItem(i));
            if (!arrow.isEmpty()) return arrow;
        }

        // 4. Restliches Inventar (nur mit Constructors Touch)
        for (int i = 9; i < player.getInventory().getContainerSize(); i++) {
            ItemStack quiver = player.getInventory().getItem(i);
            if (isRemoteQuiver(quiver, player)) {
                arrow = findFirstArrow(quiver);
                if (!arrow.isEmpty()) return arrow;
            }
        }
        return ItemStack.EMPTY;
    }

    public static void consumeProjectileForBow(Player player) {
        // Gleiche Reihenfolge wie beim Finden

        // 1. Offhand
        if (tryConsumeArrow(player.getOffhandItem())) return;

        // 2. Chest
        if (tryConsumeArrow(player.getItemBySlot(EquipmentSlot.CHEST))) return;

        // 3. Hotbar (ohne Constructors Touch)
        for (int i = 0; i < 9; i++) {
            if (tryConsumeArrow(player.getInventory().getItem(i))) return;
        }

        // 4. Restliches Inventar (nur mit Constructors Touch)
        for (int i = 9; i < player.getInventory().getContainerSize(); i++) {
            ItemStack quiver = player.getInventory().getItem(i);
            if (isRemoteQuiver(quiver, player) && tryConsumeArrow(quiver)) {
                return;
            }
        }
    }

    private static ItemStack findArrowInQuiver(ItemStack stack) {
        if (!(stack.getItem() instanceof QuiverItem)) return ItemStack.EMPTY;
        return findFirstArrow(stack);
    }

    private static boolean isRemoteQuiver(ItemStack stack, Player player) {
        return stack.getItem() instanceof QuiverItem
                && hasEnchantment(stack, player.level(), ModEnchantments.CONSTRUCTORS_TOUCH);
    }

    private static ItemStack findFirstArrow(ItemStack bundle) {
        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) return ItemStack.EMPTY;
        for (ItemStack s : contents.itemsCopy()) {
            if (s.is(ItemTags.ARROWS)) return s.copy();
        }
        return ItemStack.EMPTY;
    }

    private static boolean tryConsumeArrow(ItemStack bundle) {
        if (!(bundle.getItem() instanceof QuiverItem)) return false;

        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) return false;

        List<ItemStack> newItems = new ArrayList<>();
        boolean found = false;

        for (ItemStack s : contents.itemsCopy()) {
            if (!found && s.is(ItemTags.ARROWS)) {
                ItemStack copy = s.copy();
                copy.shrink(1);
                if (!copy.isEmpty()) newItems.add(copy);
                found = true;
            } else {
                newItems.add(s);
            }
        }

        if (found) {
            bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.copyOf(newItems)));
            return true;
        }
        return false;
    }

}
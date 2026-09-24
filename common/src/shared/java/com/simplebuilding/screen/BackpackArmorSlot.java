package com.simplebuilding.screen;

import java.util.function.BooleanSupplier;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;

/**
 * Ruestungs-Slot des Rucksack-Menues: Vanillas {@code ArmorSlot} nachgebaut (auf 1.21.11 ist die
 * Klasse paketprivat) plus eine Sperre.
 *
 * <p>Die Sperre gilt dem Brust-Slot, solange das Menue eines <em>getragenen</em> Rucksacks offen
 * ist: der Rucksack, dessen Inhalt man gerade sieht, darf weder herausgenommen noch ersetzt werden.
 * Schnellverschieben, Zifferntasten-Tausch, Werfen und Doppelklick-Sammeln fragen alle
 * {@code mayPickup}, Einlegen fragt {@code mayPlace}.
 */
public class BackpackArmorSlot extends Slot {
    private final LivingEntity owner;
    private final EquipmentSlot slot;
    private final @Nullable Identifier emptyIcon;
    private final BooleanSupplier locked;

    public BackpackArmorSlot(Container inventory, LivingEntity owner, EquipmentSlot slot, int slotIndex, int x, int y,
                             @Nullable Identifier emptyIcon, BooleanSupplier locked) {
        super(inventory, slotIndex, x, y);
        this.owner = owner;
        this.slot = slot;
        this.emptyIcon = emptyIcon;
        this.locked = locked;
    }

    public boolean isLocked() {
        return this.locked.getAsBoolean();
    }

    @Override
    public void setByPlayer(ItemStack itemStack, ItemStack previous) {
        this.owner.onEquipItem(this.slot, previous, itemStack);
        super.setByPlayer(itemStack, previous);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean mayPlace(ItemStack itemStack) {
        return !isLocked() && this.owner.isEquippableInSlot(itemStack, this.slot);
    }

    @Override
    public boolean isActive() {
        return this.owner.canUseSlot(this.slot);
    }

    @Override
    public boolean mayPickup(Player player) {
        if (isLocked()) {
            return false;
        }
        ItemStack itemStack = this.getItem();
        return !itemStack.isEmpty() && !player.isCreative() && EnchantmentHelper.has(itemStack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)
                ? false
                : super.mayPickup(player);
    }

    @Override
    public @Nullable Identifier getNoItemIcon() {
        return this.emptyIcon;
    }
}

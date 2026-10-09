package com.simplebuilding.dummy;

import com.simplebuilding.entity.ModEntities;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.mixin.ArmorStandAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import org.jspecify.annotations.Nullable;

/**
 * Mittlerer und kleiner Ruestungsstaender (Besitzer 2026-10-08, docs/ai/PLAN-STAENDER-2026-10-09.md): Vanillas
 * Ruestungsstaender mit weniger Teilen. Der mittlere traegt Hose und Stiefel (Beine, Hueftstange, Bodenplatte), der
 * kleine nur Stiefel (Bodenplatte). Alle anderen Slots sind ueber Vanillas {@code DisabledSlots} gesperrt - so lehnen
 * Rechtsklick, Spender und Ruestungstausch sie ab, ohne Vanillas Logik nachzubauen. Keine Arme.
 */
public class PartialArmorStand extends ArmorStand {
    public PartialArmorStand(EntityType<? extends ArmorStand> type, Level level) {
        super(type, level);
        this.setShowArms(false);
        lockSlots();
    }

    public boolean isMedium() {
        return ModEntities.MEDIUM_ARMOR_STAND != null && this.getType() == ModEntities.MEDIUM_ARMOR_STAND;
    }

    /** Die Slots, die dieser Staender zeigt. */
    public boolean allows(EquipmentSlot slot) {
        return slot == EquipmentSlot.FEET || slot == EquipmentSlot.LEGS && this.isMedium();
    }

    /** Alles ausser den eigenen Slots: nicht nehmen, nicht anlegen (Vanilla-Bit je Slot, Filter 0). */
    private void lockSlots() {
        int bits = 0;
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR && slot.getType() != EquipmentSlot.Type.HAND || allows(slot)) {
                continue;
            }
            bits |= 1 << slot.getFilterBit(0);
        }
        ((ArmorStandAccessor) this).simplebuilding$setDisabledSlots(bits);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setShowArms(false);
        lockSlots();
    }

    /** Das eigene Item (Abbauen ueber {@code ArmorStandStandsMixin}, Pick-Block). */
    public @Nullable Item item() {
        return this.isMedium() ? ModItems.MEDIUM_ARMOR_STAND : ModItems.SMALL_ARMOR_STAND;
    }

    @Override
    public ItemStack getPickResult() {
        Item item = item();
        return item == null ? super.getPickResult() : new ItemStack(item);
    }
}

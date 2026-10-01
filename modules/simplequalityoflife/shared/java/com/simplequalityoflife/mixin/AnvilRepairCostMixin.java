package com.simplequalityoflife.mixin;

import com.simplequalityoflife.Simplequalityoflife;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reparieren macht den Amboss nicht teurer (Besitzer 2026-10-02): Vanilla erhoeht die Arbeitskosten
 * ({@code repair_cost}) bei jeder Amboss-Benutzung. Aendert der Vorgang die Verzauberungen nicht - Reparatur mit
 * Material, zwei Werkzeuge ohne neue Verzauberung zusammenlegen, Umbenennen -, behaelt das Ergebnis die hoehere
 * Arbeitskosten seiner Zutaten statt sie zu verdoppeln. Nur Verzaubern macht den Gegenstand teurer; so lohnt das
 * Reparieren am Amboss neben Reparatur. Server-Option {@code qOL.anvilRepairKeepsCost}.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilRepairCostMixin extends ItemCombinerMenu {
    protected AnvilRepairCostMixin(MenuType<?> type, int containerId, Inventory inventory, ContainerLevelAccess access,
                                   ItemCombinerMenuSlotDefinition slots) {
        super(type, containerId, inventory, access, slots);
    }

    @Inject(method = "createResult", at = @At("RETURN"))
    private void qol$repairKeepsCost(CallbackInfo ci) {
        if (!Simplequalityoflife.getConfig().qOL.anvilRepairKeepsCost) {
            return;
        }
        ItemStack result = this.resultSlots.getItem(0);
        ItemStack left = this.inputSlots.getItem(0);
        if (result.isEmpty() || left.isEmpty()) {
            return;
        }
        if (!EnchantmentHelper.getEnchantmentsForCrafting(result).equals(EnchantmentHelper.getEnchantmentsForCrafting(left))) {
            return;
        }
        ItemStack right = this.inputSlots.getItem(1);
        int kept = Math.max(left.getOrDefault(DataComponents.REPAIR_COST, 0), right.getOrDefault(DataComponents.REPAIR_COST, 0));
        result.set(DataComponents.REPAIR_COST, kept);
    }
}

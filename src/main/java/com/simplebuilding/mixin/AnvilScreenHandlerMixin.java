package com.simplebuilding.mixin;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.SledgehammerItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.ForgingSlotsManager;
import net.minecraft.text.Text;
import net.minecraft.util.StringHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin extends ForgingScreenHandler {

    @Shadow private int repairItemUsage;
    @Shadow @Final private Property levelCost;
    @Shadow @Nullable private String newItemName;

    public AnvilScreenHandlerMixin(@Nullable ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, ScreenHandlerContext context, ForgingSlotsManager forgingSlotsManager) {
        super(type, syncId, playerInventory, context, forgingSlotsManager);
    }

    // --- 1. SLEDGEHAMMER CUSTOM LOGIC (HEAD) ---
    @Inject(method = "updateResult", at = @At("HEAD"), cancellable = true)
    private void updateResultHead(CallbackInfo ci) {
        ItemStack leftStack = this.input.getStack(0);
        ItemStack rightStack = this.input.getStack(1);

        if (leftStack.isEmpty()) return;

        if (leftStack.getItem() instanceof SledgehammerItem) {

            // FALL A: HAMMER + HAMMER
            if (rightStack.getItem() instanceof SledgehammerItem) {
                int leftDurability = leftStack.getMaxDamage() - leftStack.getDamage();
                int rightDurability = rightStack.getMaxDamage() - rightStack.getDamage();

                int bonus = (int) (leftStack.getMaxDamage() * 0.12f);
                int combinedDurability = leftDurability + rightDurability + bonus;

                int newDamage = leftStack.getMaxDamage() - combinedDurability;
                if (newDamage < 0) newDamage = 0;

                ItemStack result = leftStack.copy();
                result.setDamage(newDamage);
                result.set(DataComponentTypes.REPAIR_COST, 0);

                handleRenaming(leftStack, result);

                this.output.setStack(0, result);
                this.levelCost.set(1);
                this.repairItemUsage = 1;

                ci.cancel();
            }
            // FALL B: HAMMER + MATERIAL (KORRIGIERT)
            // Nutze leftStack.canRepairWith(rightStack) - Methode von ItemStack!
            else if (leftStack.isDamaged() && leftStack.canRepairWith(rightStack)) {
                ItemStack result = leftStack.copy();

                // Teilen durch 11 statt 4
                int repairPerItem = result.getMaxDamage() / 11;
                if (repairPerItem <= 0) repairPerItem = 1;

                int damage = result.getDamage();
                int materialsUsed = 0;
                int materialsAvailable = rightStack.getCount();

                while (damage > 0 && materialsUsed < materialsAvailable) {
                    damage -= repairPerItem;
                    materialsUsed++;
                }

                if (damage < 0) damage = 0;
                result.setDamage(damage);

                this.repairItemUsage = materialsUsed;

                int cost = materialsUsed;
                if (handleRenaming(leftStack, result)) {
                    cost += 1;
                }

                result.set(DataComponentTypes.REPAIR_COST, 0);

                if (cost <= 0) cost = 1;
                this.levelCost.set(Math.min(cost, 39));

                this.output.setStack(0, result);
                ci.cancel();
            }
        }
    }

    // --- 2. GENERAL FIXES & RESTRICTIONS (RETURN) ---
    @Inject(method = "updateResult", at = @At("RETURN"))
    private void applyAnvilTweaks(CallbackInfo ci) {
        ItemStack outputStack = this.output.getStack(0);

        if (outputStack.isEmpty()) return;

        // A) SLEDGEHAMMER REPAIR COST FIX
        if (outputStack.getItem() instanceof SledgehammerItem) {
            outputStack.set(DataComponentTypes.REPAIR_COST, 0);
            if (this.levelCost.get() >= 40) {
                this.levelCost.set(39);
            }
        }

        // B) COLOR PALETTE RESTRICTION
        RegistryWrapper.WrapperLookup registryManager = this.player.getEntityWorld().getRegistryManager();
        var colorPaletteEntry = getEnchantment(registryManager, ModEnchantments.COLOR_PALETTE);
        var masterBuilderEntry = getEnchantment(registryManager, ModEnchantments.MASTER_BUILDER);

        if (colorPaletteEntry != null && masterBuilderEntry != null) {
            int colorPaletteLevel = EnchantmentHelper.getLevel(colorPaletteEntry, outputStack);
            int masterBuilderLevel = EnchantmentHelper.getLevel(masterBuilderEntry, outputStack);

            if (colorPaletteLevel > 0 && masterBuilderLevel <= 0) {
                this.output.setStack(0, ItemStack.EMPTY);
                this.levelCost.set(0);
            }
        }
    }

    @Unique
    private boolean handleRenaming(ItemStack original, ItemStack result) {
        boolean renamed = false;
        if (this.newItemName != null && !StringHelper.isBlank(this.newItemName)) {
            if (!this.newItemName.equals(original.getName().getString())) {
                result.set(DataComponentTypes.CUSTOM_NAME, Text.literal(this.newItemName));
                renamed = true;
            }
        } else if (original.contains(DataComponentTypes.CUSTOM_NAME)) {
            result.remove(DataComponentTypes.CUSTOM_NAME);
            renamed = true;
        }
        return renamed;
    }

    @Unique
    private RegistryEntry<Enchantment> getEnchantment(RegistryWrapper.WrapperLookup registry, net.minecraft.registry.RegistryKey<Enchantment> key) {
        Optional<RegistryEntry.Reference<Enchantment>> optional = registry.getOrThrow(RegistryKeys.ENCHANTMENT).getOptional(key);
        return optional.orElse(null);
    }
}
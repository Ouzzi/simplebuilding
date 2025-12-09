package com.simplebuilding.mixin;

import com.simplebuilding.enchantment.ModEnchantments;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.*;
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

    public AnvilScreenHandlerMixin(@Nullable ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(type, syncId, playerInventory, context, null);
    }

    @Shadow @Final private Property levelCost;

    @Inject(method = "updateResult", at = @At("RETURN"))
    private void restrictColorPaletteUsage(CallbackInfo ci) {
        ItemStack outputStack = this.output.getStack(0);

        if (outputStack.isEmpty()) return;

        RegistryWrapper.WrapperLookup registryManager = this.player.getEntityWorld().getRegistryManager();

        var colorPaletteEntry = getEnchantment(registryManager, ModEnchantments.COLOR_PALETTE);
        var masterBuilderEntry = getEnchantment(registryManager, ModEnchantments.MASTER_BUILDER);

        if (colorPaletteEntry == null || masterBuilderEntry == null) {return;}

        int colorPaletteLevel = EnchantmentHelper.getLevel(colorPaletteEntry, outputStack);
        int masterBuilderLevel = EnchantmentHelper.getLevel(masterBuilderEntry, outputStack);

        if (colorPaletteLevel > 0) {
            if (masterBuilderLevel <= 0) {
                this.output.setStack(0, ItemStack.EMPTY);
                this.levelCost.set(0);
            }
        }
    }

    @Unique
    private RegistryEntry<Enchantment> getEnchantment(RegistryWrapper.WrapperLookup registry, net.minecraft.registry.RegistryKey<Enchantment> key) {
        Optional<RegistryEntry.Reference<Enchantment>> optional = registry.getOrThrow(RegistryKeys.ENCHANTMENT).getOptional(key);
        return optional.orElse(null);
    }

}
package com.simplelib.mixin;

import com.simplelib.api.LegendaryMoments;
import net.minecraft.advancements.triggers.EnchantedItemTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Legendary moments (SP22-2): every enchanting table fires {@code minecraft:enchanted_item} with the freshly enchanted item
 * (Vanilla's table and mod tables such as the Astral Enchanter), so this one hook covers them all.
 */
@Mixin(EnchantedItemTrigger.class)
public abstract class LegendaryEnchantTableMixin {
    @Inject(method = "trigger", at = @At("HEAD"))
    private void simplelib$legendary(ServerPlayer player, ItemStack stack, int levels, CallbackInfo ci) {
        LegendaryMoments.enchanted(player, ItemStack.EMPTY, stack);
    }
}

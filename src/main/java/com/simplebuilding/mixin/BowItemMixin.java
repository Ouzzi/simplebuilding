package com.simplebuilding.mixin;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.ReinforcedBundleItem;
import com.simplebuilding.util.BundleUtil; // Import!
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BowItem.class)
public class BowItemMixin {

    @Inject(method = "onStoppedUsing", at = @At("RETURN"))
    private void consumeArrowFromBundle(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;

        if (!(user instanceof PlayerEntity player) || player.getAbilities().creativeMode) return;

        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack slotStack = player.getInventory().getStack(i);

            if (slotStack.getItem() instanceof ReinforcedBundleItem) {
                if (hasQuiverEnchantment(slotStack, player)) {
                    ItemStack arrow = BundleUtil.findArrow(slotStack);
                    if (!arrow.isEmpty()) {
                        if (BundleUtil.removeOneArrow(slotStack)) {
                            return;
                        }
                    }
                }
            }
        }
    }

    @Unique
    private boolean hasQuiverEnchantment(ItemStack stack, PlayerEntity player) {
        var registry = player.getEntityWorld().getRegistryManager();
        var enchantments = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
        var quiver = enchantments.getOptional(ModEnchantments.QUIVER);
        return quiver.isPresent() && EnchantmentHelper.getLevel(quiver.get(), stack) > 0;
    }
}
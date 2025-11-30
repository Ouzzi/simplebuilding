package com.simplebuilding.mixin;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.ReinforcedBundleItem;
import com.simplebuilding.util.BundleUtil; // Import!
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    @Shadow public abstract PlayerInventory getInventory();

    @Inject(method = "getProjectileType", at = @At("HEAD"), cancellable = true)
    private void getProjectileTypeFromBundle(ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        if (!(stack.getItem() instanceof net.minecraft.item.BowItem)) return;

        PlayerEntity player = (PlayerEntity) (Object) this;

        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack slotStack = player.getInventory().getStack(i);

            if (slotStack.getItem() instanceof ReinforcedBundleItem) {
                if (hasQuiverEnchantment(slotStack, player)) {
                    // Nutzung der Utility Klasse
                    ItemStack arrowStack = BundleUtil.findArrow(slotStack);
                    if (!arrowStack.isEmpty()) {
                        cir.setReturnValue(arrowStack.copy());
                        return;
                    }
                }
            }
        }
    }

    private boolean hasQuiverEnchantment(ItemStack stack, PlayerEntity player) {
        var registry = player.getEntityWorld().getRegistryManager();
        var enchantments = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
        var quiver = enchantments.getOptional(ModEnchantments.QUIVER);
        return quiver.isPresent() && EnchantmentHelper.getLevel(quiver.get(), stack) > 0;
    }
}
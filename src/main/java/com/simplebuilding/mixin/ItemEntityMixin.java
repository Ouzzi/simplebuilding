package com.simplebuilding.mixin;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.ReinforcedBundleItem;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity {

    public ItemEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Shadow public abstract ItemStack getStack();

    @Inject(method = "onPlayerCollision", at = @At("HEAD"))
    private void onPlayerCollision(PlayerEntity player, CallbackInfo ci) {
        System.out.println("onPlayerCollision triggered");

        if (this.getEntityWorld().isClient()) return;

        if (!player.isSneaking()) return;

        ItemStack itemOnGround = this.getStack();
        if (itemOnGround.isEmpty()) return;
        
        System.out.println("item detected");

        for (Hand hand : Hand.values()) {
            ItemStack heldItem = player.getStackInHand(hand);

            if (tryPickupWithBundle(heldItem, itemOnGround, player)) {
                if (itemOnGround.isEmpty()) {
                    this.discard();
                }
                return;
            }
        }
    }

    private boolean tryPickupWithBundle(ItemStack bundleStack, ItemStack itemToPickup, PlayerEntity player) {
        if (!(bundleStack.getItem() instanceof ReinforcedBundleItem)) return false;

        var registry = player.getEntityWorld().getRegistryManager();
        var enchantments = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
        var funnel = enchantments.getOptional(ModEnchantments.FUNNEL);

        if (funnel.isEmpty() || EnchantmentHelper.getLevel(funnel.get(), bundleStack) <= 0) {
            System.out.println("no funnel enchantment");
            return false;
        }

        boolean success = ((ReinforcedBundleItem) bundleStack.getItem()).tryInsertStackFromWorld(bundleStack, itemToPickup, player);

        if (success) {
            player.sendPickup(this, itemToPickup.getCount());
        }
        System.out.println("bundle tryes to pick up. response -> " + success);
        return success;
    }
}
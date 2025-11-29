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
        if (this.getEntityWorld().isClient()) return;

        // 1. Prüfen: Sneakt der Spieler?
        if (!player.isSneaking()) return;

        ItemStack itemOnGround = this.getStack();
        if (itemOnGround.isEmpty()) return;

        // 2. Prüfen: Hat er ein Funnel-Bundle in der Hand? (Main oder Offhand)
        for (Hand hand : Hand.values()) {
            ItemStack heldItem = player.getStackInHand(hand);

            // Prüfen ob es das richtige Item ist und ob Funnel drauf ist
            if (tryPickupWithBundle(heldItem, itemOnGround, player)) {
                // Wenn Item aufgenommen wurde, löschen wir das ItemEntity
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
            return false;
        }

        // Wir rufen die API im Item auf
        boolean success = ((ReinforcedBundleItem) bundleStack.getItem()).tryInsertStackFromWorld(bundleStack, itemToPickup, player);

        if (success) {
            // WICHTIG: Die Animation hier triggern, da wir hier Zugriff auf 'this' (ItemEntity) haben!
            player.sendPickup(this, itemToPickup.getCount());
        }
        return success;
    }
}
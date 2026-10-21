package com.simplestorage.mixin;

import com.simplestorage.bundle.ReinforcedBundleItem;
import com.simplestorage.registry.StorageItems;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bundles and quivers on the ground (split stage 8c-1, the bundle parts of SimpleBuilding's {@code ItemEntityMixin}):
 * the Netherite and Enderite tiers ignore explosions, and a bundle with Funnel in the hands or the inventory sweeps up
 * what the player touches (hands first, then the inventory). Priority below the default, so the bundles came before
 * SimpleBuilding's worn backpack with Funnel; since split stage 8c-2 the worn backpack is the module's and comes right
 * after the bundles here (its Netherite and Enderite tiers are blast-proof on the ground too).
 */
@Mixin(value = ItemEntity.class, priority = 900)
public abstract class ItemEntityBundleMixin extends Entity {
    @Shadow public abstract ItemStack getItem();
    @Shadow private int pickupDelay;
    @Shadow private java.util.UUID target;

    public ItemEntityBundleMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    /** The top tiers survive explosions on the ground (an upgrade never takes that away). */
    @Inject(method = "ignoreExplosion", at = @At("HEAD"), cancellable = true)
    private void simplestorage$topTierIgnoresExplosion(Explosion explosion, CallbackInfoReturnable<Boolean> cir) {
        ItemStack stack = this.getItem();
        if (stack.is(StorageItems.NETHERITE_BUNDLE) || stack.is(StorageItems.ENDERITE_BUNDLE)
                || stack.is(StorageItems.NETHERITE_QUIVER) || stack.is(StorageItems.ENDERITE_QUIVER)
                || stack.is(StorageItems.BACKPACKS.get(com.simplestorage.backpack.BackpackTier.NETHERITE))
                || stack.is(StorageItems.BACKPACKS.get(com.simplestorage.backpack.BackpackTier.ENDERITE))) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void simplestorage$funnelPickup(Player player, CallbackInfo ci) {
        if (this.level().isClientSide() || player.isShiftKeyDown()) return;
        ItemStack itemOnGround = this.getItem();
        if (itemOnGround.isEmpty()) return;
        // Vanilla's own pickup conditions for every way: expired pickup delay, no other target (audit 2026-09-26 #14).
        if (this.pickupDelay != 0) return;
        if (this.target != null && !this.target.equals(player.getUUID())) return;
        // Taken before: the ways below shrink the stack on the ground (audit N7).
        int countBefore = itemOnGround.getCount();
        Item pickedItem = itemOnGround.getItem();
        for (InteractionHand hand : InteractionHand.values()) {
            if (simplestorage$tryBundle(player.getItemInHand(hand), itemOnGround, player)) {
                simplestorage$picked(player, itemOnGround, pickedItem, countBefore, ci);
                return;
            }
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (simplestorage$tryBundle(player.getInventory().getItem(i), itemOnGround, player)) {
                simplestorage$picked(player, itemOnGround, pickedItem, countBefore, ci);
                return;
            }
        }
        // 8c-2: the worn backpack with Funnel - after every bundle.
        if (com.simplestorage.backpack.BackpackItem.tryFunnelPickup(player, itemOnGround)) {
            simplestorage$picked(player, itemOnGround, pickedItem, countBefore, ci);
        }
    }

    @Unique
    private static boolean simplestorage$tryBundle(ItemStack bundle, ItemStack itemOnGround, Player player) {
        return bundle.getItem() instanceof ReinforcedBundleItem item && item.canAutoPickup(bundle, itemOnGround, player.level())
                && item.tryInsertStackFromWorld(bundle, itemOnGround, player);
    }

    /** Pickup animation and statistic for what was swept up; a stack taken whole leaves the world (audit N7). */
    @Unique
    private void simplestorage$picked(Player player, ItemStack itemOnGround, Item pickedItem, int countBefore, CallbackInfo ci) {
        int picked = countBefore - itemOnGround.getCount();
        if (picked > 0) {
            player.take(this, picked);
            player.awardStat(Stats.ITEM_PICKED_UP.get(pickedItem), picked);
        }
        if (itemOnGround.isEmpty()) {
            this.discard();
            ci.cancel();
        }
    }
}

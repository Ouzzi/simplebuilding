package com.simplesandwiches.mixin;

import com.simplesandwiches.item.BundleEating;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Eat the top food item straight out of a bundle (see {@link BundleEating}). */
@Mixin(BundleItem.class)
public abstract class BundleEatMixin extends Item {
    private BundleEatMixin(Properties properties) {
        super(properties);
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void simplesandwiches$use(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack bundle = player.getItemInHand(hand);
        Consumable consumable = BundleEating.begin(bundle, player); // only while sneaking (F14); a normal click stays Vanilla
        if (consumable != null) cir.setReturnValue(BundleEating.use(level, player, hand, bundle, consumable));
    }

    @Inject(method = "onUseTick", at = @At("HEAD"), cancellable = true)
    private void simplesandwiches$tick(Level level, LivingEntity entity, ItemStack bundle, int ticksRemaining, CallbackInfo ci) {
        Consumable consumable = BundleEating.eatingNow(bundle, entity);
        if (consumable != null) {
            BundleEating.tick(entity, bundle, consumable, ticksRemaining);
            ci.cancel();
        }
    }

    @Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
    private void simplesandwiches$duration(ItemStack bundle, LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        Consumable consumable = BundleEating.eatingNow(bundle, entity);
        if (consumable != null) cir.setReturnValue(consumable.consumeTicks());
    }

    @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
    private void simplesandwiches$animation(ItemStack bundle, CallbackInfoReturnable<ItemUseAnimation> cir) {
        Consumable consumable = BundleEating.anyMeal() ? BundleEating.eating(bundle) : null;
        if (consumable != null) cir.setReturnValue(consumable.animation());
    }

    @Override
    public boolean releaseUsing(ItemStack bundle, Level level, LivingEntity entity, int remainingTime) {
        BundleEating.abort(entity);
        return super.releaseUsing(bundle, level, entity, remainingTime);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack bundle, Level level, LivingEntity entity) {
        if (BundleEating.eatingNow(bundle, entity) != null) return BundleEating.finish(level, entity, bundle);
        return super.finishUsingItem(bundle, level, entity);
    }
}

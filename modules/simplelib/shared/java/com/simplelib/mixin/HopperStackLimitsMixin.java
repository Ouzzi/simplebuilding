package com.simplelib.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simplelib.api.StackLimits;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hoppers fill raised slots up to their container's limit (owner N15: Enderite crucible and barrel 128, not 64).
 * Vanilla counts with {@code stack.getMaxStackSize()} when it merges, calls a slot above that unmergeable and a
 * container of full normal stacks full. Here the container's own {@link Container#getMaxStackSize(ItemStack)} decides
 * ({@link StackLimits#limit}); it only ever raises, so Vanilla containers keep Vanilla's numbers. Covers Vanilla
 * hoppers, hopper minecarts and every mod hopper that goes through {@code HopperBlockEntity#addItem}.
 */
@Mixin(HopperBlockEntity.class)
public abstract class HopperStackLimitsMixin {

    @WrapOperation(method = "tryMoveInItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;canMergeItems(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"))
    private static boolean simplelib$mergeIntoRaisedSlots(ItemStack current, ItemStack incoming, Operation<Boolean> original,
                                                          @Local(argsOnly = true, ordinal = 1) Container container) {
        int limit = StackLimits.limit(container, current, current.getMaxStackSize());
        if (limit <= current.getMaxStackSize()) return original.call(current, incoming);
        return current.getCount() < limit && ItemStack.isSameItemSameComponents(current, incoming);
    }

    @WrapOperation(method = "tryMoveInItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I"))
    private static int simplelib$spaceInRaisedSlots(ItemStack stack, Operation<Integer> original,
                                                    @Local(argsOnly = true, ordinal = 1) Container container) {
        return StackLimits.limit(container, stack, original.call(stack));
    }

    @WrapOperation(method = "isFullContainer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I"))
    private static int simplelib$raisedSlotsAreNotFull(ItemStack stack, Operation<Integer> original,
                                                       @Local(argsOnly = true) Container container) {
        return StackLimits.limit(container, stack, original.call(stack));
    }
}

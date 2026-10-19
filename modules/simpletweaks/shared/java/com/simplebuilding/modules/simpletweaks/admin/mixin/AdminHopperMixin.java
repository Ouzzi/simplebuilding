package com.simplebuilding.modules.simpletweaks.admin.mixin;

import com.simplebuilding.modules.simpletweaks.admin.blocklog.BlockRecorder;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Block log (I/O, switch hoppers): items a hopper or hopper minecart moved into or out of a container. Server thread. */
@Mixin(HopperBlockEntity.class)
public abstract class AdminHopperMixin {
    @Unique private static ItemStack simpletweaks$moving = ItemStack.EMPTY;

    @Inject(method = "addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"))
    private static void simpletweaks$beforeMove(Container from, Container to, ItemStack stack, Direction direction, CallbackInfoReturnable<ItemStack> cir) {
        simpletweaks$moving = from != null && BlockRecorder.hoppers() ? stack.copy() : ItemStack.EMPTY;
    }

    @Inject(method = "addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private static void simpletweaks$afterMove(Container from, Container to, ItemStack stack, Direction direction, CallbackInfoReturnable<ItemStack> cir) {
        var moving = simpletweaks$moving;
        if (moving.isEmpty()) return;
        simpletweaks$moving = ItemStack.EMPTY;
        BlockRecorder.hopperMoved(from, to, moving, moving.getCount() - cir.getReturnValue().getCount());
    }
}

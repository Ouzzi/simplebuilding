package com.simplecrucibles.mixin;

import com.simplecrucibles.fluid.CrucibleBucketItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Nachtrag 60: honeycomb waxes and an axe scrapes a copper bucket shown in an item frame (as on a copper block). */
@Mixin(ItemFrame.class)
public abstract class CopperBucketFrameCareMixin {
    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void simplecrucibles$careForFramedBucket(Player player, InteractionHand hand, Vec3 hitPos, CallbackInfoReturnable<InteractionResult> cir) {
        ItemFrame frame = (ItemFrame) (Object) this;
        ItemStack handStack = player.getItemInHand(hand);
        if (!CrucibleBucketItem.isCopper(frame.getItem()) || handStack.isEmpty()
                || !(handStack.is(Items.HONEYCOMB) || handStack.is(net.minecraft.tags.ItemTags.AXES))) return;
        ItemStack shown = frame.getItem().copy();
        EquipmentSlot toolSlot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        InteractionResult care = CrucibleBucketItem.care(player.level(), player, shown, handStack, toolSlot, frame.blockPosition());
        if (care == null) return;
        if (care == InteractionResult.SUCCESS && !player.level().isClientSide()) frame.setItem(shown);
        cir.setReturnValue(care);
    }
}

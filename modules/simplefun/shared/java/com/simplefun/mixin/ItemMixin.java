package com.simplefun.mixin;

import com.simplefun.SimplefunCommon;
import com.simplefun.entity.BrickProjectileEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class ItemMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void simplefun$useThrowableBrick(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!SimplefunCommon.getConfig().fun.enableThrowableBricks) {
            return;
        }
        Item self = (Item) (Object) this;
        if (self == Items.BRICK || self == Items.NETHER_BRICK || self == Items.RESIN_BRICK) {
            BrickProjectileEntity.throwFrom(level, player, player.getItemInHand(hand));
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}

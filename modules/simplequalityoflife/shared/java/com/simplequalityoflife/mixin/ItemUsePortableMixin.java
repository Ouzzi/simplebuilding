package com.simplequalityoflife.mixin;

import com.simplequalityoflife.container.PortableContainers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Easy Shulkers / Ender Chests: right-click in the air opens the held box (block items do not override use). */
@Mixin(Item.class)
public abstract class ItemUsePortableMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void qol$use(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (PortableContainers.onUse(level, player, hand)) cir.setReturnValue(InteractionResult.SUCCESS);
    }
}

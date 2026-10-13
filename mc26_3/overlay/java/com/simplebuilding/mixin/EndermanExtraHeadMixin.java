package com.simplebuilding.mixin;

import com.simplebuilding.util.ExtraHead;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A carved pumpkin in the extra head slot (owner N49, {@link ExtraHead}) keeps endermen calm like one in the helmet
 * slot. 26.2 twin targets {@code EnderMan}, 26.3 {@code Enderman}.
 */
@Mixin(Enderman.class)
public abstract class EndermanExtraHeadMixin {
    @Inject(method = "isBeingStaredBy", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$extraHeadDisguise(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (ExtraHead.get(player).is(Items.CARVED_PUMPKIN)) {
            cir.setReturnValue(false);
        }
    }
}

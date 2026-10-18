package com.simplebuilding.modules.simpletweaks.play.mixin;

import com.simplebuilding.modules.simpletweaks.play.keep.KeepInventory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Partial keep inventory (T27): kept experience is not dropped as orbs (stacks: {@link PlayInventoryMixin}). */
@Mixin(Player.class)
public abstract class PlayPlayerMixin {
    @Inject(method = "dropEquipment", at = @At("HEAD"))
    private void simpletweaks$playDeathStart(ServerLevel level, CallbackInfo ci) { KeepInventory.deathStart((Player) (Object) this); }

    @Inject(method = "dropEquipment", at = @At("RETURN"))
    private void simpletweaks$playDeathEnd(ServerLevel level, CallbackInfo ci) { KeepInventory.deathEnd((Player) (Object) this); }

    @Inject(method = "getBaseExperienceReward", at = @At("RETURN"), cancellable = true)
    private void simpletweaks$playKeepXp(ServerLevel level, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(KeepInventory.droppedXp((Player) (Object) this, cir.getReturnValueI()));
    }
}

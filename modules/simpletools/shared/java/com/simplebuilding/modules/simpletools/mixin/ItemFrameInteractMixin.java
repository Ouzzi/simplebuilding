package com.simplebuilding.modules.simpletools.mixin;

import com.simplebuilding.modules.simpletools.ItemFrames;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Better item frames: sneaking actions on a filled frame ({@link ItemFrames}); covers glow item frames (same method).
 * Priority 1100: runs after SimpleBuilding's own frame hook (glass-pane lock, shears hide, sneak show), which keeps
 * precedence when SimpleBuilding is installed; Simple Tools then adds what it leaves alone.
 */
@Mixin(value = ItemFrame.class, priority = 1100)
public abstract class ItemFrameInteractMixin {
    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void simpletools$frameActions(Player player, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult result = ItemFrames.interact((ItemFrame) (Object) this, player, hand);
        if (result != null) cir.setReturnValue(result);
    }

    @Inject(method = "dropItem(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Z)V", at = @At("TAIL"))
    private void simpletools$showWhenEmptied(ServerLevel level, Entity causedBy, boolean withFrame, CallbackInfo ci) {
        ItemFrames.afterDrop((ItemFrame) (Object) this);
    }
}

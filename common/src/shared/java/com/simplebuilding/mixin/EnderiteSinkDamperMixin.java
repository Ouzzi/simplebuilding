package com.simplebuilding.mixin;

import com.simplebuilding.util.EnderiteSinkDamper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** {@link EnderiteSinkDamper}: weniger Schwerkraft beim schleichenden Fall, und der Fallweg zaehlt im selben Mass weniger. */
@Mixin(LivingEntity.class)
public abstract class EnderiteSinkDamperMixin {
    @Inject(method = "getEffectiveGravity", at = @At("RETURN"), cancellable = true)
    private void simplebuilding$dampGravity(CallbackInfoReturnable<Double> cir) {
        if ((Object) this instanceof Player player) {
            double damping = EnderiteSinkDamper.damping(player);
            if (damping > 0.0) {
                cir.setReturnValue(cir.getReturnValueD() * (1.0 - damping));
            }
        }
    }

    @ModifyVariable(method = "checkFallDamage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double simplebuilding$dampFallDistance(double ya, double ignored, boolean onGround, BlockState state, BlockPos pos) {
        if (!onGround && ya < 0.0 && (Object) this instanceof Player player) {
            double damping = EnderiteSinkDamper.damping(player);
            if (damping > 0.0) {
                return ya * (1.0 - damping);
            }
        }
        return ya;
    }
}

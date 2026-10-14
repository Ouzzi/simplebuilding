package com.simplebuilding.mixin;

import com.simplebuilding.util.ShriekerSilence;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ShriekParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SculkShriekerBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Silent Dandelion on a Sculk Shrieker (owner N57): the silenced flag lives in the block entity. A silenced
 * shrieker still shrieks for the game (vibration, warning level, Darkness, Warden) - only the sounds go: the
 * client plays the shriek sound itself on level event 3007, so the server sends the shriek particles directly
 * instead, and the warden reply sound is skipped.
 */
@Mixin(SculkShriekerBlockEntity.class)
public abstract class SculkShriekerSilenceMixin implements ShriekerSilence {
    @Unique private boolean simplebuilding$silenced;

    @Override
    public boolean simplebuilding$isSilenced() {
        return simplebuilding$silenced;
    }

    @Override
    public void simplebuilding$setSilenced(boolean silenced) {
        if (simplebuilding$silenced == silenced) return;
        simplebuilding$silenced = silenced;
        ((SculkShriekerBlockEntity) (Object) this).setChanged();
    }

    @Redirect(method = "shriek", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;levelEvent(ILnet/minecraft/core/BlockPos;I)V"))
    private void simplebuilding$quietShriek(ServerLevel level, int type, BlockPos pos, int data) {
        if (!simplebuilding$silenced) {
            level.levelEvent(type, pos, data);
            return;
        }
        // Same particles as level event 3007, without its sound.
        for (int i = 0; i < 10; i++) {
            level.sendParticles(new ShriekParticleOption(i * 5), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    @Inject(method = "playWardenReplySound", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$noReply(Level level, CallbackInfo ci) {
        if (simplebuilding$silenced) ci.cancel();
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void simplebuilding$loadSilenced(ValueInput input, CallbackInfo ci) {
        simplebuilding$silenced = input.getBooleanOr("simplebuilding_silenced", false);
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void simplebuilding$saveSilenced(ValueOutput output, CallbackInfo ci) {
        if (simplebuilding$silenced) output.putBoolean("simplebuilding_silenced", true);
    }
}

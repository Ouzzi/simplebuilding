package com.simplebuilding.mixin;

import com.simplebuilding.util.ShriekerSilence;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SculkShriekerBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.jspecify.annotations.Nullable;

/**
 * Silent Dandelion on a Sculk Shrieker (owner N57): the silenced flag lives in the block entity. A silenced
 * shrieker is mute for the game too: {@code tryShriek} does nothing (no warning level, no shriek, no SHRIEK game
 * event for sculk sensors); a shriek already running when it was silenced skips the warden reply sound.
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

    /**
     * A silenced shrieker does not react to vibrations at all: no warning level, no SHRIEKING state, no shriek sound or
     * particles and no SHRIEK game event, so nearby sculk sensors are not set off by it (owner 14.10.).
     */
    @Inject(method = "tryShriek", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$silentNoShriek(ServerLevel level, @Nullable ServerPlayer player, CallbackInfo ci) {
        if (simplebuilding$silenced) ci.cancel();
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

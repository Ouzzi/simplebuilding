package com.simplevisuals.mixin;

import com.simplevisuals.ChiseledFrame;
import com.simplevisuals.FrameChisel;
import com.simplevisuals.FrameTilt;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * W23-5: the frame's chisel step ({@link FrameChisel}) as a synced entity datum, saved as {@code SimpleVisualsTilt}
 * (only when not flat), back to flat when the item comes out. Common mixin without client classes; glow frames too.
 */
@Mixin(ItemFrame.class)
public abstract class ItemFrameChiselMixin implements ChiseledFrame {
    @Unique private static final EntityDataAccessor<Byte> SIMPLEVISUALS$TILT = SynchedEntityData.defineId(ItemFrame.class, EntityDataSerializers.BYTE);
    @Unique private static final String SIMPLEVISUALS$KEY = "SimpleVisualsTilt";

    @Shadow private boolean fixed;

    @Override
    public int simplevisuals$tiltStep() {
        return ((Entity) (Object) this).getEntityData().get(SIMPLEVISUALS$TILT);
    }

    @Override
    public void simplevisuals$setTiltStep(int step) {
        ((Entity) (Object) this).getEntityData().set(SIMPLEVISUALS$TILT, (byte) Math.max(0, Math.min(FrameTilt.STEPS, step)));
    }

    @Override
    public boolean simplevisuals$fixed() {
        return fixed;
    }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void simplevisuals$defineTilt(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(SIMPLEVISUALS$TILT, (byte) 0);
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void simplevisuals$chisel(Player player, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult result = FrameChisel.interact((ItemFrame) (Object) this, player, hand);
        if (result != null) cir.setReturnValue(result);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void simplevisuals$saveTilt(ValueOutput output, CallbackInfo ci) {
        int step = simplevisuals$tiltStep();
        if (step != 0) output.putInt(SIMPLEVISUALS$KEY, step);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void simplevisuals$loadTilt(ValueInput input, CallbackInfo ci) {
        simplevisuals$setTiltStep(input.getIntOr(SIMPLEVISUALS$KEY, 0));
    }

    @Inject(method = "dropItem(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Z)V", at = @At("TAIL"))
    private void simplevisuals$flatWhenEmptied(ServerLevel level, Entity causedBy, boolean withFrame, CallbackInfo ci) {
        if (((ItemFrame) (Object) this).getItem().isEmpty()) simplevisuals$setTiltStep(0);
    }
}

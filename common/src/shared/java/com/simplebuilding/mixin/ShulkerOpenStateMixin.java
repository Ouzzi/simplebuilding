package com.simplebuilding.mixin;

import com.simplebuilding.util.ShulkerLids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Shulker state (owner N17, {@link ShulkerLids}): a Vanilla shulker box remembers whether it stands open. While it
 * does, the lid opens and stays up whatever the opener count says; once the flag is cleared and nobody has the box
 * open, the lid closes like after a menu. The flag is saved and sent to clients (only the flag, not the contents).
 */
@Mixin(ShulkerBoxBlockEntity.class)
public abstract class ShulkerOpenStateMixin implements ShulkerLids.Kept {
    @Unique private boolean simplebuilding$keptOpen;

    @Shadow private int openCount;
    @Shadow private ShulkerBoxBlockEntity.AnimationStatus animationStatus;

    @Override
    public boolean simplebuilding$keptOpen() {
        return simplebuilding$keptOpen;
    }

    @Override
    public void simplebuilding$setKeptOpen(boolean open) {
        simplebuilding$keptOpen = open;
    }

    @Inject(method = "updateAnimation", at = @At("HEAD"))
    private void simplebuilding$holdTheLid(Level level, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (simplebuilding$keptOpen) {
            if (animationStatus == ShulkerBoxBlockEntity.AnimationStatus.CLOSED || animationStatus == ShulkerBoxBlockEntity.AnimationStatus.CLOSING) {
                animationStatus = ShulkerBoxBlockEntity.AnimationStatus.OPENING;
            }
        } else if (openCount <= 0 && (animationStatus == ShulkerBoxBlockEntity.AnimationStatus.OPENED
                || animationStatus == ShulkerBoxBlockEntity.AnimationStatus.OPENING)) {
            // Released: nobody has it open, so close like after the last menu (Vanilla never stays here otherwise).
            animationStatus = ShulkerBoxBlockEntity.AnimationStatus.CLOSING;
        }
    }

    /** A menu opened or closed on a box that stands open: count the viewers, keep the lid where it is. */
    @Inject(method = "triggerEvent", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$keepLidUp(int b0, int b1, CallbackInfoReturnable<Boolean> cir) {
        if (simplebuilding$keptOpen && b0 == 1) {
            openCount = b1;
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void simplebuilding$loadOpen(ValueInput input, CallbackInfo ci) {
        simplebuilding$keptOpen = input.getBooleanOr("simplebuilding_open", false);
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void simplebuilding$saveOpen(ValueOutput output, CallbackInfo ci) {
        if (simplebuilding$keptOpen) output.putBoolean("simplebuilding_open", true);
    }

    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create((ShulkerBoxBlockEntity) (Object) this);
    }

    /** Only the flag reaches the client (Vanilla sends no shulker data at all). */
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (simplebuilding$keptOpen) tag.putBoolean("simplebuilding_open", true);
        return tag;
    }
}

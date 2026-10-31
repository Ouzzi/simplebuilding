package com.simpleresonance.mixin;

import com.simpleresonance.LegacyImport;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reads SimpleBuilding's old tracker block {@code SimpleBuildingData} out of the player's data and parks it for the one-time import. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerLegacyMixin {
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void simpleresonance$parkLegacy(ValueInput view, CallbackInfo ci) {
        view.read(LegacyImport.KEY, CompoundTag.CODEC).ifPresent(block -> LegacyImport.park(((ServerPlayer) (Object) this).getUUID(), block));
    }
}

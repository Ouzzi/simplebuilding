package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.trials.EvilOmen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Where a player's Raid Omen starts or extends a raid: an Evil / Void Raid (TR31-R) gets its extra wave. */
@Mixin(Raids.class)
public abstract class RaidsEvilMixin {
    @Inject(method = "createOrExtendRaid", at = @At("RETURN"))
    private void simplemobs$evilRaid(ServerPlayer player, BlockPos pos, CallbackInfoReturnable<Raid> cir) {
        EvilOmen.onRaidCreated(player, cir.getReturnValue());
    }
}

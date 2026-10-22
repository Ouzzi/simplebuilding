package com.simplemobheads.mixin;

import com.simplemobheads.ability.HeadAbilities;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Abilities that run every tick of the wearer: eggs, truffle nose, sniffing, floating items, greeting, Herobrine; the haunting; the giant heads' size. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerTickMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void simplemobheads$headTick(CallbackInfo ci) {
        HeadAbilities.tick((ServerPlayer) (Object) this);
        com.simplemobheads.ritual.HerobrineHaunt.tick((ServerPlayer) (Object) this);
        com.simplemobheads.easter.Giants.wearerTick((ServerPlayer) (Object) this);
    }
}

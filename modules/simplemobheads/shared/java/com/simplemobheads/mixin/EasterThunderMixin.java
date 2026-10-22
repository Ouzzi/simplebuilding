package com.simplemobheads.mixin;

import com.simplemobheads.easter.Giants;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Giant zombies (EH21 ZO-1): zombies do not override {@code thunderHit}; a full-moon bolt makes one a giant, then Vanilla burns it. */
@Mixin(Entity.class)
public abstract class EasterThunderMixin {
    @Inject(method = "thunderHit", at = @At("HEAD"))
    private void simplemobheads$giantZombie(ServerLevel level, LightningBolt bolt, CallbackInfo ci) {
        if ((Object) this instanceof Zombie zombie) {
            Giants.onLightning(level, zombie, bolt, null);
        }
    }
}

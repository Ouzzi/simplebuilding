package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.trim.TrimAbilities;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The armour trim abilities that only players have (MOB27-3, moved from the SimpleBuilding core's PlayerEntityMixin, same hooks):
 * Stasis/Astralit on the tick, Nihilith gravity and Astralit gliding (client and server, for prediction), sprint hunger (Wayfinder)
 * and experience (Raiser, Lapis, Quartz). Only while {@link TrimAbilities#active}.
 */
@Mixin(Player.class)
public abstract class TrimAbilityPlayerMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void simplemobs$trimTick(CallbackInfo ci) {
        if (!TrimAbilities.active()) return;
        Player player = (Player) (Object) this;
        TrimAbilities.tick(player);
        TrimAbilities.handleNihilithGravity(player);
        TrimAbilities.handleAstralitGlide(player);
    }

    @ModifyVariable(method = "causeFoodExhaustion", at = @At("HEAD"), argsOnly = true)
    private float simplemobs$trimExhaustion(float exhaustion) {
        Player player = (Player) (Object) this;
        if (!TrimAbilities.active() || !player.isSprinting()) return exhaustion;
        float reductionPct = TrimAbilities.getExhaustionReduction(player);
        return reductionPct > 0 ? exhaustion * (1.0f - reductionPct) : exhaustion;
    }

    @ModifyVariable(method = "giveExperiencePoints", at = @At("HEAD"), argsOnly = true)
    private int simplemobs$trimExperience(int experience) {
        if (experience <= 0 || !TrimAbilities.active()) return experience;
        float multiplier = TrimAbilities.getXPMultiplier((Player) (Object) this);
        return multiplier > 1.0f ? Math.round(experience * multiplier) : experience;
    }
}

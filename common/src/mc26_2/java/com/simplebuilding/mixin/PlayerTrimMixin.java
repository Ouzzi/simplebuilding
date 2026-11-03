package com.simplebuilding.mixin;

import com.simplebuilding.util.TrimEffectUtil;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.2 line: the armour trim abilities only players have (Stasis/Astralit, Nihilith gravity, Astralit glide, sprint hunger,
 * experience), moved out of {@link PlayerEntityMixin} unchanged. On 26.3 Simple Mobs applies them (ZO9 A / MOB27-3,
 * docs/ai/PLAN-B-TRIMMOVE01-2026-11-03.md) and the 26.3 twin of this mixin is empty.
 */
@Mixin(Player.class)
public abstract class PlayerTrimMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void simplebuilding$tickTrimLogic(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        TrimEffectUtil.tick(player);
        // Nihilith-Schwerkraft und Astralit-Gleiten: Client und Server (Vorhersage).
        TrimEffectUtil.handleNihilithGravity(player);
        TrimEffectUtil.handleAstralitGlide(player);
    }

    @ModifyVariable(method = "causeFoodExhaustion", at = @At("HEAD"), argsOnly = true)
    private float simplebuilding$reduceExhaustion(float exhaustion) {
        Player player = (Player) (Object) this;
        if (player.isSprinting()) {
            float reductionPct = TrimEffectUtil.getExhaustionReduction(player);
            if (reductionPct > 0) {
                return exhaustion * (1.0f - reductionPct);
            }
        }
        return exhaustion;
    }

    @ModifyVariable(method = "giveExperiencePoints", at = @At("HEAD"), argsOnly = true)
    private int simplebuilding$modifyXpGain(int experience) {
        if (experience <= 0) return experience;
        float multiplier = TrimEffectUtil.getXPMultiplier((Player) (Object) this);
        return multiplier > 1.0f ? Math.round(experience * multiplier) : experience;
    }
}

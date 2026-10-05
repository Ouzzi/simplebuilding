package com.simplelib.mixin;

import com.simplelib.warm.Warm;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Warm food in the hand (owner answer 41, "in der Hand"): a little steam rises from the held stack, more the warmer
 * it is (four steps). Client side only, nothing is sent.
 */
@Mixin(Player.class)
public abstract class WarmSteamMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void simplelib$warmSteam(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (!player.level().isClientSide() || player.isInvisible()) return;
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.isEmpty()) continue;
            float warmth = Warm.warmth(stack, player.level().getGameTime());
            if (warmth <= 0) continue;
            int step = Math.min(4, 1 + (int) (warmth * 4));
            if (player.getRandom().nextInt(40) >= step * 2) continue;
            boolean right = (hand == InteractionHand.MAIN_HAND) == (player.getMainArm() == HumanoidArm.RIGHT);
            Vec3 look = player.getViewVector(1.0F);
            Vec3 side = new Vec3(-look.z, 0, look.x).normalize().scale(right ? 0.35 : -0.35);
            Vec3 at = player.getEyePosition().add(look.scale(0.45)).add(side).add(0, -0.45, 0);
            player.level().addParticle(ParticleTypes.WHITE_SMOKE, at.x, at.y, at.z, 0.0, 0.03, 0.0);
        }
    }
}

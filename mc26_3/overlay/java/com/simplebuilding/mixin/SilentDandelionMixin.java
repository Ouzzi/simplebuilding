package com.simplebuilding.mixin;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.version.McVersion;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class SilentDandelionMixin {
    @Unique private int simplebuilding$silenceParticleTimer;

    // Like name tags, handle this before trading, mounting, or other mob-specific interactions.
    // Mob.interact already rejects dead mobs. Players and armor stands are not Mobs.
    @Inject(method = "checkAndHandleImportantInteractions", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$toggleSilence(Player player, InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir) {
        Mob mob = (Mob) (Object) this;
        var stack = player.getItemInHand(hand);
        if (!McVersion.SILENT_DANDELION || !stack.is(ModItems.SILENT_DANDELION)
                || simplebuilding$silenceParticleTimer != 0
                // Keep boss combat feedback audible; silence applies to ordinary mobs of any age.
                || mob instanceof EnderDragon || mob instanceof WitherBoss) return;
        if (!mob.level().isClientSide()) {
            // Vanilla handles creative consumption, persistence and both golden-dandelion sounds.
            // Only the supplied setter changes: no age or age-lock data is touched.
            AgeableMob.setAgeLocked(mob, mob::isSilent, player, stack, target -> target.setSilent(!target.isSilent()));
            simplebuilding$silenceParticleTimer = 40;
        }
        cir.setReturnValue(InteractionResult.SUCCESS);
    }

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void simplebuilding$silenceParticles(CallbackInfo ci) {
        if (simplebuilding$silenceParticleTimer > 0) {
            Mob mob = (Mob) (Object) this;
            simplebuilding$silenceParticleTimer = AgeableMob.makeAgeLockedParticle(
                    mob.level(), mob, simplebuilding$silenceParticleTimer, mob.isSilent());
        }
    }
}

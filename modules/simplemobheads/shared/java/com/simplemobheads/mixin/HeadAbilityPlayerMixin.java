package com.simplemobheads.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simplemobheads.ability.HeadAbilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Spider head (cobwebs do not slow, like the spider) and cave spider head (bare hands cut cobwebs as fast as a sword),
 * on client and server alike. {@code getDestroySpeed*}: NeoForge computes in the overload with a position; Forge names it
 * differently ({@code require = 0}).
 */
@Mixin(Player.class)
public abstract class HeadAbilityPlayerMixin {
    @Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true)
    private void simplemobheads$spiderHeadIgnoresCobwebs(BlockState state, Vec3 speedMultiplier, CallbackInfo ci) {
        if (HeadAbilities.ignoresCobweb((Player) (Object) this, state)) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "getDestroySpeed*", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;getDestroySpeed(Lnet/minecraft/world/level/block/state/BlockState;)F"))
    private float simplemobheads$caveSpiderHeadCutsCobwebs(float speed, @Local(argsOnly = true) BlockState state) {
        return HeadAbilities.cobwebBreakSpeed((Player) (Object) this, state, speed);
    }
}

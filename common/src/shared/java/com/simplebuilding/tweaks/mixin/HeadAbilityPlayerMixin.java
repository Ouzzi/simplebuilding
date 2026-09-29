package com.simplebuilding.tweaks.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simplebuilding.tweaks.heads.HeadAbilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Spinnenkopf (Netze bremsen nicht, wie bei der Spinne selbst) und Hoehlenspinnenkopf (Netze bricht die Hand so
 * schnell wie ein Schwert), siehe {@link HeadAbilities}. Beides laeuft auf Client und Server gleich: der
 * getragene Kopf ist beiden bekannt, die Bewegung sagt der Client voraus.
 *
 * <p>Das Abbautempo wird am Grundwert des gehaltenen Items gehoben ({@code ItemStack#getDestroySpeed}), damit Eile,
 * Abbaulaehmung, Wasser und Luftsprung wie bei einem Schwert wirken. {@code getDestroySpeed*}: NeoForge rechnet in
 * der Ueberladung mit Blockposition und laesst die alte nur weiterleiten; {@code require = 0}, weil Forge die
 * Methode anders nennt (dort fehlt die Faehigkeit bis zum Port-Run).
 */
@Mixin(Player.class)
public abstract class HeadAbilityPlayerMixin {

    @Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$spiderHeadIgnoresCobwebs(BlockState state, Vec3 speedMultiplier, CallbackInfo ci) {
        if (HeadAbilities.ignoresCobweb((Player) (Object) this, state)) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "getDestroySpeed*", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;getDestroySpeed(Lnet/minecraft/world/level/block/state/BlockState;)F"))
    private float simplebuilding$caveSpiderHeadCutsCobwebs(float speed, @Local(argsOnly = true) BlockState state) {
        return HeadAbilities.cobwebBreakSpeed((Player) (Object) this, state, speed);
    }
}

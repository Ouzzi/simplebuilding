package com.simplebuilding.modules.simpletools.mixin;

import com.simplebuilding.modules.simpletools.vault.DeathLedger;
import com.simplebuilding.modules.simpletools.vault.RemovalContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Death Vault context: marks the entity's own tick (despawn), the damage it takes (fire, lava, cactus, explosion) and a player's touch (pickup), and keeps
 * recorded death drops from merging (a partial merge would split a recorded stack over two entities).
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityLedgerMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void simpletools$tickStart(CallbackInfo ci) {
        RemovalContext.ticking = (ItemEntity) (Object) this;
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void simpletools$tickEnd(CallbackInfo ci) {
        if (RemovalContext.ticking == (Object) this) RemovalContext.ticking = null;
    }

    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void simpletools$touchStart(Player player, CallbackInfo ci) {
        if (!player.level().isClientSide()) RemovalContext.touchStart((ItemEntity) (Object) this, player);
    }

    @Inject(method = "playerTouch", at = @At("RETURN"))
    private void simpletools$touchEnd(Player player, CallbackInfo ci) {
        RemovalContext.touchEnd((ItemEntity) (Object) this);
    }

    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void simpletools$hurtStart(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        RemovalContext.hurtTarget = (ItemEntity) (Object) this;
        RemovalContext.hurtSource = source;
    }

    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void simpletools$hurtEnd(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        if (RemovalContext.hurtTarget == (Object) this) {
            RemovalContext.hurtTarget = null;
            RemovalContext.hurtSource = null;
        }
    }

    @Inject(method = "isMergable", at = @At("RETURN"), cancellable = true)
    private void simpletools$noMergeWhileTracked(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        DeathLedger ledger = DeathLedger.of((ItemEntity) (Object) this);
        if (ledger != null && ledger.tracked(((ItemEntity) (Object) this).getUUID())) cir.setReturnValue(false);
    }
}

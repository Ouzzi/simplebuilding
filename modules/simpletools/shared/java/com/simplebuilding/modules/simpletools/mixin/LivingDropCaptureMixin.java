package com.simplebuilding.modules.simpletools.mixin;

import com.simplebuilding.modules.simpletools.soulbound.Soulbound;
import com.simplebuilding.modules.simpletools.vault.DeathVault;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Every item entity a dying player creates during its death drops is handed to the Death Vault bookkeeping. */
@Mixin(LivingEntity.class)
public abstract class LivingDropCaptureMixin {
    @Inject(method = "createItemStackToDrop", at = @At("RETURN"))
    private void simpletools$collect(ItemStack stack, boolean randomly, boolean thrownFromHand, CallbackInfoReturnable<ItemEntity> cir) {
        if ((Object) this instanceof Player player && Soulbound.dying(player)) DeathVault.collect(cir.getReturnValue());
    }
}

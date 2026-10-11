package com.simplebuilding.modules.simpletools.mixin;

import com.simplebuilding.modules.simpletools.SimpleTools;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class FullDurabilityEfficiencyMixin {

    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void modifyMiningSpeed(BlockState block, CallbackInfoReturnable<Float> info) {
        Player player = (Player) (Object) this;
        if (!SimpleTools.active(player.level()) || !SimpleTools.configFor(player.level()).enableFullDurabilityBonus) return;
        ItemStack stack = player.getMainHandItem();

        // Nur wenn das Item Schaden nehmen kann
        if (stack.isEmpty() || !stack.isDamageableItem()) return;

        float maxDamage = stack.getMaxDamage();
        float currentDamage = stack.getDamageValue(); // getDamageValue() gibt den SCHADEN zurück, nicht die Haltbarkeit
        float durabilityPercent = (maxDamage - currentDamage) / maxDamage;

        if (durabilityPercent >= SimpleTools.configFor(player.level()).fullDurabilityThreshold) {
            float originalSpeed = info.getReturnValue();
            // Multipliziere die Geschwindigkeit
            info.setReturnValue((float) (originalSpeed * SimpleTools.configFor(player.level()).fullDurabilityBonusMultiplier));
        }
    }
}

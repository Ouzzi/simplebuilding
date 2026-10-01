package com.simplequalityoflife.mixin;

import com.simplequalityoflife.registry.Thrift;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sparsamkeit (Thrift, 2026-10-02): mehr Haltbarkeit statt Reparatur. Jede Stufe hebt die maximale Haltbarkeit um
 * ein Drittel, Stufe III verdoppelt sie. Weil Vanillas Material-Reparatur ein Viertel der maximalen Haltbarkeit je
 * Material zurueckgibt, reicht am Amboss dieselbe Materialmenge wie ohne Verzauberung - doppelt materialeffizient.
 * Nicht mit Reparatur (Mending) kombinierbar ({@code exclusive_set}).
 */
@Mixin(ItemStack.class)
public abstract class ThriftDurabilityMixin {
    @Inject(method = "getMaxDamage", at = @At("RETURN"), cancellable = true)
    private void qol$thrift(CallbackInfoReturnable<Integer> cir) {
        int max = cir.getReturnValueI();
        if (max > 0) {
            int level = Thrift.level((ItemStack) (Object) this);
            if (level > 0) {
                cir.setReturnValue(Thrift.scaled(max, level));
            }
        }
    }
}

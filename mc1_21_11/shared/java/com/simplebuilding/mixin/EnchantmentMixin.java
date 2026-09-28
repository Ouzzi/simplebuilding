package com.simplebuilding.mixin;

import com.simplebuilding.util.ModTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keine Erfahrungs-Reparatur (Mending und jede andere Verzauberung mit {@code repair_with_xp}) auf
 * Items im Tag {@code simplebuilding:xp_repair_incompatible} (Besitzer 2026-09-28: der Rotator
 * nimmt Unbreaking, aber kein Mending - seine Haltbarkeit ist eine Ladung, die mit Enderperlen
 * aufgeladen wird, nicht mit Erfahrung).
 *
 * <p>Mending und Unbreaking teilen sich {@code #minecraft:enchantable/durability}; ein Tag allein
 * kann das eine nicht ohne das andere geben. Deshalb die beiden Fragen der Verzauberung selbst:
 * {@code canEnchant} (Amboss, {@code enchant_randomly}, Befehl {@code /enchant}) und
 * {@code isSupportedItem} (Verzauberungstisch ueber {@code isPrimaryItem}; auf NeoForge ruft auch
 * {@code IItemExtension#supportsEnchantment} genau diese Methode).
 */
@Mixin(Enchantment.class)
public abstract class EnchantmentMixin {

    @Inject(method = "canEnchant", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$noXpRepairOnCanEnchant(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (simplebuilding$refusesXpRepair(stack)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isSupportedItem", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$noXpRepairOnSupportedItem(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (simplebuilding$refusesXpRepair(stack)) {
            cir.setReturnValue(false);
        }
    }

    private boolean simplebuilding$refusesXpRepair(ItemStack stack) {
        return stack.is(ModTags.Items.XP_REPAIR_INCOMPATIBLE)
                && ((Enchantment) (Object) this).effects().has(EnchantmentEffectComponents.REPAIR_WITH_XP);
    }
}

package com.simplebuilding.mixin;

import com.simplebuilding.items.custom.QuiverItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Ein steckengebliebener, aufsammelbarer Pfeil geht beim Aufsammeln zuerst in einen Koecher mit
 * Trichter (Besitzer 2026-09-28, {@link QuiverItem#tryFunnelArrows}); ohne Trichter landet er wie
 * bei Vanilla im Inventar. Dreizack und andere Geschosse ohne Pfeil-Tag bleiben unberuehrt.
 */
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowPickupMixin {

    @Redirect(method = "tryPickup", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean simplebuilding$funnelIntoQuiver(Inventory inventory, ItemStack stack) {
        QuiverItem.tryFunnelArrows(inventory.player, stack);
        return stack.isEmpty() || inventory.add(stack);
    }
}

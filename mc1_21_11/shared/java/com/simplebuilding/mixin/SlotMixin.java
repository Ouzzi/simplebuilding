package com.simplebuilding.mixin;

import com.simplebuilding.items.ModItems;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ein Platz, der den Layout-Platzhalter {@code simplebuilding:creative_spacer} haelt, ist inaktiv.
 *
 * <p>{@code AbstractContainerScreen} fragt {@link Slot#isActive()} beim Zeichnen des Platzes, bei der
 * Hover-Suche ({@code getHoveredSlot}) und damit bei jedem Klick, jeder Zifferntaste, dem Mittelklick
 * und dem Tooltip. Ein inaktiver Platz wird nicht gezeichnet, nicht hervorgehoben, zeigt keinen Tooltip
 * und nimmt keinen Klick an - im Kreativinventar ist der Platzhalter damit leere Flaeche. Der Platzhalter
 * steht nie in einem echten Inventar (er loescht sich dort selbst), also trifft das sonst keinen Platz.
 */
@Mixin(Slot.class)
public abstract class SlotMixin {
    @Shadow
    public abstract ItemStack getItem();

    @Inject(method = "isActive", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$spacerIsInactive(CallbackInfoReturnable<Boolean> cir) {
        if (this.getItem().is(ModItems.CREATIVE_SPACER)) {
            cir.setReturnValue(false);
        }
    }
}

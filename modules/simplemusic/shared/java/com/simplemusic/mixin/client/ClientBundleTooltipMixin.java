package com.simplemusic.mixin.client;

import com.simplemusic.client.PocketTooltip;
import com.simplemusic.pocket.PocketJukeboxItem;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;
import org.apache.commons.lang3.math.Fraction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Pocket Jukebox tooltip: a disc weighs a whole bundle in Vanilla's count, so the fill bar of a {@link PocketTooltip}
 * shows discs out of {@link PocketJukeboxItem#CAPACITY} instead. Every other bundle tooltip stays untouched (Simple
 * Storage scales its own ones at the same call; the two modifications chain).
 */
@Mixin(ClientBundleTooltip.class)
public abstract class ClientBundleTooltipMixin {
    @ModifyArg(method = "extractImage", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientBundleTooltip;extractBundleWithItemsTooltip(Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/client/gui/GuiGraphicsExtractor;Lorg/apache/commons/lang3/math/Fraction;)V"),
            index = 6)
    private Fraction simplemusic$pocketFill(Fraction weight) {
        return (Object) this instanceof PocketTooltip ? weight.divideBy(Fraction.getFraction(PocketJukeboxItem.CAPACITY, 1)) : weight;
    }
}

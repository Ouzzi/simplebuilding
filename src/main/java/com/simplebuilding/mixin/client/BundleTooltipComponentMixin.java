package com.simplebuilding.mixin.client;

import com.simplebuilding.util.BundleTooltipAccessor;
import net.minecraft.client.gui.tooltip.BundleTooltipComponent;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.math.Fraction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BundleTooltipComponent.class)
public abstract class BundleTooltipComponentMixin implements BundleTooltipAccessor {

    @Final
    @Shadow
    private BundleContentsComponent bundleContents;

    @Unique
    private float capacityScale = 1.0f;

    @Override
    public void simplebuilding$setCapacityScale(float scale) {
        this.capacityScale = scale;
    }

    @Redirect(
            method = "drawProgressBar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/tooltip/BundleTooltipComponent;getProgressBarFillTexture()Lnet/minecraft/util/Identifier;"
            )
    )
    private Identifier redirectTexture(BundleTooltipComponent instance) {
        Fraction threshold = Fraction.getFraction((int) capacityScale, 1);
        if (this.bundleContents.getOccupancy().compareTo(threshold) >= 0) {
            return Identifier.ofVanilla("container/bundle/bundle_progressbar_full");
        }
        return Identifier.ofVanilla("container/bundle/bundle_progressbar_fill");
    }

    @Redirect(
            method = "drawProgressBar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/tooltip/BundleTooltipComponent;getProgressBarFill()I"
            )
    )
    private int redirectWidth(BundleTooltipComponent instance) {
        Fraction occupancy = this.bundleContents.getOccupancy();
        if (capacityScale > 1.0f) {
            occupancy = occupancy.divideBy(Fraction.getFraction((int) capacityScale, 1));
        }
        return MathHelper.clamp(MathHelper.multiplyFraction(occupancy, 94), 0, 94);
    }

    // --- HIER IST DIE ÄNDERUNG FÜR DEN NAMEN ---
    @Redirect(
            method = "drawProgressBar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/tooltip/BundleTooltipComponent;getProgressBarLabel()Lnet/minecraft/text/Text;"
            )
    )
    private Text redirectLabel(BundleTooltipComponent instance) {
        if (this.bundleContents.isEmpty()) {
            return Text.translatable("item.minecraft.bundle.empty");
        }

        // 1. Prüfen, ob ein Item ausgewählt ist (durch Scrollen)
        int selectedIndex = this.bundleContents.getSelectedStackIndex();
        if (selectedIndex != -1) {
            ItemStack selectedStack = this.bundleContents.get(selectedIndex);
            // Gibt den Namen des Items zurück (z.B. "Diamant (32)")
            return Text.literal(selectedStack.getName().getString() + " x" + selectedStack.getCount());
        }

        // 2. Ansonsten Standard (Voll oder nichts)
        Fraction threshold = Fraction.getFraction((int) capacityScale, 1);
        if (this.bundleContents.getOccupancy().compareTo(threshold) >= 0) {
            return Text.translatable("item.minecraft.bundle.full");
        }

        return null;
    }
}
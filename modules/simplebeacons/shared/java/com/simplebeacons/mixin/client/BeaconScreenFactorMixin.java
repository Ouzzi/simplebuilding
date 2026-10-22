package com.simplebeacons.mixin.client;

import com.simplebeacons.beacon.RangeFactor;
import com.simplebeacons.beacon.RangeFactorView;
import com.simplebeacons.client.StrongBeaconScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.BeaconMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * SP22-10: the Vanilla beacon screen shows the material factor and the range it gives in the right field below the
 * secondary buttons, once the pyramid is active and the server has {@code vanillaBeaconMaterialRange} on. Drawn on an
 * opaque field in Vanilla's own colour (#212121, text #E0E0E0 = 12.2:1), so a restyled screen stays readable. The
 * Strong Beacon screen draws its own line ({@link StrongBeaconScreen}).
 */
@Mixin(BeaconScreen.class)
public abstract class BeaconScreenFactorMixin extends AbstractContainerScreen<BeaconMenu> {
    private BeaconScreenFactorMixin(BeaconMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void simplebeacons$factor(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if ((Object) this instanceof StrongBeaconScreen) return;
        int percent = ((RangeFactorView) this.menu).simplebeacons$rangePercent();
        int levels = this.menu.getLevels();
        if (percent <= 0 || levels <= 0) return;
        Component factor = StrongBeaconScreen.factorLine(percent);
        Component range = Component.translatable("simplebeacons.screen.range", Math.round(RangeFactor.apply(levels * 10 + 10, percent)));
        int centre = this.leftPos + 169, top = this.topPos + 76;
        int width = Math.max(this.font.width(factor), this.font.width(range)) + 4;
        graphics.fill(centre - width / 2, top - 2, centre + (width + 1) / 2, top + 21, StrongBeaconScreen.FIELD);
        graphics.centeredText(this.font, factor, centre, top, StrongBeaconScreen.LABEL);
        graphics.centeredText(this.font, range, centre, top + 12, StrongBeaconScreen.LABEL);
    }
}

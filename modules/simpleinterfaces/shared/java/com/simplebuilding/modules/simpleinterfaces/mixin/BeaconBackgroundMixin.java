package com.simplebuilding.modules.simpleinterfaces.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.simplebuilding.modules.simpleinterfaces.client.WorkScreens;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.BeaconMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Group "work" (W1 G2): the beacon in the Simple style ({@link WorkScreens#beacon}): the Vanilla PNG is skipped, the
 * "Primary/Secondary Power" labels give way to engraved pyramid and star symbols; the payment items row, the buttons
 * (styled by {@link BeaconButtonMixin}) and their tooltips stay.
 */
@Mixin(BeaconScreen.class)
public abstract class BeaconBackgroundMixin extends AbstractContainerScreen<BeaconMenu> {
    @Unique
    private boolean simpleinterfaces$styled;

    private BeaconBackgroundMixin(BeaconMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "extractBackground", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private void simpleinterfaces$drawStyle(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        this.simpleinterfaces$styled = WorkScreens.beacon(this, graphics, (this.width - this.imageWidth) / 2,
                (this.height - this.imageHeight) / 2, this.imageWidth, this.imageHeight, this.titleLabelY);
    }

    @WrapWithCondition(method = "extractBackground", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"))
    private boolean simpleinterfaces$vanillaTexture(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier texture,
            int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        return !this.simpleinterfaces$styled;
    }

    @Inject(method = "extractLabels", at = @At("HEAD"), cancellable = true)
    private void simpleinterfaces$labels(GuiGraphicsExtractor graphics, int xm, int ym, CallbackInfo ci) {
        if (WorkScreens.palette(this, this.imageWidth, this.imageHeight, this.titleLabelY) != null) ci.cancel();
    }
}

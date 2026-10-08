package com.simplebuilding.modules.simplecontainers.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.simplebuilding.modules.simplecontainers.client.StyledScreens;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.DispenserScreen;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Group "storage" (W0): the generic chest screen, shulker box, hopper and dispenser/dropper. Vanilla's
 * {@code AbstractContainerScreen} has no background of its own - each of these screens calls
 * {@code super.extractBackground} (the dimmed world) and then blits its PNG. Right after the super call the Simple
 * style is drawn when it applies ({@link StyledScreens}); the PNG blits are then skipped. Everything else in the
 * method - and other mods' injections at its tail - still runs. A new screen group adds its own mixin like this one.
 */
@Mixin({ContainerScreen.class, ShulkerBoxScreen.class, HopperScreen.class, DispenserScreen.class})
public abstract class StorageBackgroundMixin extends AbstractContainerScreen<AbstractContainerMenu> {
    @Unique
    private boolean simplecontainers$styled;

    private StorageBackgroundMixin(AbstractContainerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "extractBackground", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private void simplecontainers$drawStyle(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        this.simplecontainers$styled = StyledScreens.drawBackground(this, graphics, this.leftPos, this.topPos,
                this.imageWidth, this.imageHeight, this.titleLabelY);
    }

    @WrapWithCondition(method = "extractBackground", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"))
    private boolean simplecontainers$vanillaTexture(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier texture,
            int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        return !this.simplecontainers$styled;
    }
}

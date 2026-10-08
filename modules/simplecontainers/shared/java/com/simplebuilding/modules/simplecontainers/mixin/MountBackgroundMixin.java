package com.simplebuilding.modules.simplecontainers.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.simplebuilding.modules.simplecontainers.client.StorageDecors;
import com.simplebuilding.modules.simplecontainers.client.StyledScreens;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractMountInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractMountInventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Group "storage" (W1 G1): mount inventories (horse, donkey, mule, llama, camel ... and the nautilus; preview
 * g1-pferd.png). The background, chest-slot strip and saddle/armor slot frames of Vanilla are skipped while styled;
 * the live animal preview still renders on top of the style's sunk field. Which screen classes are styled decides
 * {@link StyledScreens#style} (exact classes HorseInventoryScreen and NautilusInventoryScreen).
 */
@Mixin(AbstractMountInventoryScreen.class)
public abstract class MountBackgroundMixin extends AbstractContainerScreen<AbstractMountInventoryMenu> {
    @Unique
    private boolean simplecontainers$styled;

    private MountBackgroundMixin(AbstractMountInventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "extractBackground", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private void simplecontainers$drawStyle(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        this.simplecontainers$styled = StyledScreens.drawBackground(this, graphics, this.leftPos, this.topPos,
                this.imageWidth, this.imageHeight, this.titleLabelY, StorageDecors.mount());
    }

    @WrapWithCondition(method = "extractBackground", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"))
    private boolean simplecontainers$vanillaTexture(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier texture,
            int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        return !this.simplecontainers$styled;
    }

    @WrapWithCondition(method = "extractBackground", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIIIII)V"))
    private boolean simplecontainers$vanillaChestSlots(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite,
            int spriteWidth, int spriteHeight, int textureX, int textureY, int x, int y, int width, int height) {
        return !this.simplecontainers$styled;
    }

    @WrapWithCondition(method = "extractBackground", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractMountInventoryScreen;extractSlot(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"))
    private boolean simplecontainers$vanillaSlotFrames(AbstractMountInventoryScreen<?> screen, GuiGraphicsExtractor graphics, int x, int y) {
        return !this.simplecontainers$styled;
    }
}

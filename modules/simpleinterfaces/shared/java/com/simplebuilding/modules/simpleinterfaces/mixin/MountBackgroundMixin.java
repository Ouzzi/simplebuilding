package com.simplebuilding.modules.simpleinterfaces.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.simplebuilding.modules.simpleinterfaces.client.StorageDecors;
import com.simplebuilding.modules.simpleinterfaces.client.StyledScreens;
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
 * the live animal preview still renders on top of the style's sunk field. The style is drawn just before the Vanilla
 * blit, after anything other mods inject behind the window (simpleriding's hoof panel). Which screen classes are styled decides
 * {@link StyledScreens#style} (exact classes HorseInventoryScreen and NautilusInventoryScreen).
 */
@Mixin(AbstractMountInventoryScreen.class)
public abstract class MountBackgroundMixin extends AbstractContainerScreen<AbstractMountInventoryMenu> {
    @Unique
    private boolean simpleinterfaces$styled;

    private MountBackgroundMixin(AbstractMountInventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    // Right before Vanilla's background blit, not right after the super call: other mods drawing behind the mount window
    // at that point (simpleriding's hoof panel tab, whose right end hides under this box's left frame) come first.
    @Inject(method = "extractBackground", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"))
    private void simpleinterfaces$drawStyle(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        this.simpleinterfaces$styled = StyledScreens.drawBackground(this, graphics, this.leftPos, this.topPos,
                this.imageWidth, this.imageHeight, this.titleLabelY, StorageDecors.mount());
    }

    @WrapWithCondition(method = "extractBackground", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"))
    private boolean simpleinterfaces$vanillaTexture(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier texture,
            int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        return !this.simpleinterfaces$styled;
    }

    @WrapWithCondition(method = "extractBackground", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIIIII)V"))
    private boolean simpleinterfaces$vanillaChestSlots(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite,
            int spriteWidth, int spriteHeight, int textureX, int textureY, int x, int y, int width, int height) {
        return !this.simpleinterfaces$styled;
    }

    @WrapWithCondition(method = "extractBackground", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractMountInventoryScreen;extractSlot(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"))
    private boolean simpleinterfaces$vanillaSlotFrames(AbstractMountInventoryScreen<?> screen, GuiGraphicsExtractor graphics, int x, int y) {
        return !this.simpleinterfaces$styled;
    }
}

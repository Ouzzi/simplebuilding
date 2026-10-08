package com.simplebuilding.modules.simplecontainers.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.simplebuilding.modules.simplecontainers.client.WorkScreens;
import com.simplelib.api.client.ui.UiBoxes.ProgressColors;
import com.simplelib.api.client.ui.UiSymbols;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.BlastFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.SmokerScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Group "work" (W1 G2): furnace, blast furnace and smoker (all drawn by {@code AbstractFurnaceScreen}) in the Simple
 * style ({@link WorkScreens#furnace}): the Vanilla PNG, flame and arrow sprites are skipped - the fuel slot shows the
 * burn time, the arrow the cooking progress. Only the exact Vanilla screen classes are styled (registry lookup).
 * The title moves to (8, 6); the recipe book button stays where Vanilla puts it.
 */
@Mixin(AbstractFurnaceScreen.class)
public abstract class FurnaceBackgroundMixin extends AbstractRecipeBookScreen<AbstractFurnaceMenu> {
    @Unique
    private boolean simplecontainers$styled;

    private FurnaceBackgroundMixin(AbstractFurnaceMenu menu, RecipeBookComponent<?> book, Inventory inventory, Component title) {
        super(menu, book, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void simplecontainers$title(CallbackInfo ci) {
        if (WorkScreens.palette(this, this.imageWidth, this.imageHeight, this.titleLabelY) != null) this.titleLabelX = 8;
    }

    @Inject(method = "extractBackground", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractRecipeBookScreen;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private void simplecontainers$drawStyle(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        Object self = this;
        ProgressColors colors = self instanceof BlastFurnaceScreen ? ProgressColors.BLAST : ProgressColors.FIRE;
        UiSymbols.Bitmap waves = self instanceof SmokerScreen ? UiSymbols.SMOKE : UiSymbols.HEAT;
        this.simplecontainers$styled = WorkScreens.furnace(this, this.menu, graphics, this.leftPos, this.topPos, this.imageWidth,
                this.imageHeight, this.titleLabelY, colors, waves);
    }

    @WrapWithCondition(method = "extractBackground", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"))
    private boolean simplecontainers$vanillaTexture(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier texture,
            int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        return !this.simplecontainers$styled;
    }

    @WrapWithCondition(method = "extractBackground", require = 2, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIIIII)V"))
    private boolean simplecontainers$vanillaSprites(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite,
            int spriteWidth, int spriteHeight, int u, int v, int x, int y, int width, int height) {
        return !this.simplecontainers$styled;
    }
}

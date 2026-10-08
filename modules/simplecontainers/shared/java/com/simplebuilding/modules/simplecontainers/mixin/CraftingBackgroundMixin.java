package com.simplebuilding.modules.simplecontainers.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.simplebuilding.modules.simplecontainers.client.WorkScreens;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Group "work" (W1 G2): the crafting table in the Simple style ({@link WorkScreens#crafting}); the Vanilla PNG is
 * skipped. The title moves to (8, 6) like every styled screen; the recipe book button stays where Vanilla puts it.
 */
@Mixin(CraftingScreen.class)
public abstract class CraftingBackgroundMixin extends AbstractRecipeBookScreen<CraftingMenu> {
    @Unique
    private boolean simplecontainers$styled;

    private CraftingBackgroundMixin(CraftingMenu menu, RecipeBookComponent<?> book, Inventory inventory, Component title) {
        super(menu, book, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void simplecontainers$title(CallbackInfo ci) {
        if (WorkScreens.palette(this, this.imageWidth, this.imageHeight, this.titleLabelY) != null) this.titleLabelX = 8;
    }

    @Inject(method = "extractBackground", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractRecipeBookScreen;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private void simplecontainers$drawStyle(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        this.simplecontainers$styled = WorkScreens.crafting(this, graphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight,
                this.titleLabelY);
    }

    @WrapWithCondition(method = "extractBackground", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"))
    private boolean simplecontainers$vanillaTexture(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier texture,
            int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        return !this.simplecontainers$styled;
    }
}

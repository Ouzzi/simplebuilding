package com.simplebuilding.modules.simplecontainers.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.simplebuilding.modules.simplecontainers.client.WorkScreens;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CartographyTableScreen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Group "work II" (W1 G3): grindstone, stonecutter, loom, cartography table, villager trading, player inventory and -
 * through {@code ItemCombinerScreen}, which blits the PNG for both - anvil and smithing table. Each of them calls
 * {@code super.extractBackground} (the dimmed world) and then blits its PNG; in place of that blit the Simple style is
 * drawn when it applies ({@link WorkScreens#draw}). Everything after the blit (sprites, map, banner, entity preview,
 * other mods' injections) still runs; the sprites the style replaces are handled by the per-screen mixins.
 */
@Mixin({GrindstoneScreen.class, StonecutterScreen.class, LoomScreen.class, CartographyTableScreen.class, MerchantScreen.class,
        InventoryScreen.class, ItemCombinerScreen.class})
public abstract class WorkBackgroundMixin extends AbstractContainerScreen<AbstractContainerMenu> {
    private WorkBackgroundMixin(AbstractContainerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @WrapWithCondition(method = "extractBackground", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"))
    private boolean simplecontainers$workBackground(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier texture,
            int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        return !WorkScreens.draw(this, graphics, x, y, this.imageWidth, this.imageHeight);
    }
}

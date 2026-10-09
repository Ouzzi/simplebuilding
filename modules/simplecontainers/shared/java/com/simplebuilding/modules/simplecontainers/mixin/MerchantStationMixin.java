package com.simplebuilding.modules.simplecontainers.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.simplebuilding.modules.simplecontainers.client.OfferChoice;
import com.simplelib.api.client.ui.UiSymbols;
import com.simplebuilding.modules.simplecontainers.client.StationScreens;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Villager trading in the Simple style: out-of-stock arrow → red cross, scroller thumb as a raised tile, the trader's
 * XP bar as a sunk bar with a flat green fill, trade arrows engraved, the title (+ level) at the top of the right part
 * in the label colour; the offers and inventory labels are left out. The offer buttons: {@code TradeOfferButtonMixin}.
 */
@Mixin(MerchantScreen.class)
public abstract class MerchantStationMixin extends AbstractContainerScreen<MerchantMenu> implements OfferChoice {
    @Shadow private int shopItem;
    @Shadow private int scrollOff;

    private MerchantStationMixin(MerchantMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public boolean simplecontainers$chosen(int buttonY) {
        return (buttonY - this.topPos - 18) / 20 + this.scrollOff == this.shopItem;
    }

    @WrapWithCondition(method = "extractBackground", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private boolean simplecontainers$outOfStock(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int w, int h) {
        if (!StationScreens.active(this)) return true;
        StationScreens.error(graphics, this.leftPos + 186, this.topPos + 38, UiSymbols.ARROW.width(), UiSymbols.ARROW.height());
        return false;
    }

    @WrapWithCondition(method = "extractScroller", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private boolean simplecontainers$scroller(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int w, int h) {
        if (!StationScreens.active(this)) return true;
        StationScreens.thumb(this, graphics, sprite.getPath(), x, y, w, h);
        return false;
    }

    @WrapWithCondition(method = "extractProgressBar", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private boolean simplecontainers$xpBackground(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int w, int h) {
        return !StationScreens.active(this);
    }

    @WrapWithCondition(method = "extractProgressBar", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIIIII)V"))
    private boolean simplecontainers$xpFill(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int spriteWidth,
            int spriteHeight, int u, int v, int x, int y, int w, int h) {
        if (!StationScreens.active(this)) return true;
        StationScreens.xpFill(graphics, this.leftPos, x, y, w, sprite.getPath().endsWith("_result"));
        return false;
    }

    @WrapWithCondition(method = "extractButtonArrows", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private boolean simplecontainers$tradeArrow(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int w, int h) {
        if (!StationScreens.active(this)) return true;
        StationScreens.tradeArrow(this, graphics, x, y, sprite.getPath().endsWith("_out_of_stock"));
        return false;
    }

    @WrapWithCondition(method = "extractLabels", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"))
    private boolean simplecontainers$labels(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int color, boolean shadow) {
        return !StationScreens.active(this);
    }

    @Inject(method = "extractLabels", at = @At("TAIL"))
    private void simplecontainers$title(GuiGraphicsExtractor graphics, int xm, int ym, CallbackInfo ci) {
        if (StationScreens.active(this)) StationScreens.merchantTitle(this, graphics, this.font, this.title);
    }
}

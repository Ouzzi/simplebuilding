package com.simplebuilding.mixin.client;

import com.simplebuilding.util.TrimMultiplierLogic;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends HandledScreen<PlayerScreenHandler> {

    public InventoryScreenMixin(PlayerScreenHandler screenHandler, PlayerInventory playerInventory, Text text) {
        super(screenHandler, playerInventory, text);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void renderTrimStats(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.client == null || this.client.player == null) return;

        // Berechnung
        double xpMult = TrimMultiplierLogic.calculateXPMultiplier(this.client.player);
        double survMult = TrimMultiplierLogic.calculateSurvivalMultiplier(this.client.player);
        double totalMult = TrimMultiplierLogic.getMultiplier(this.client.player);

        // Formatierung (z.B. "1.50x")
        String xpText = String.format("%.2fx", xpMult);
        String survText = String.format("%.2fx", survMult);
        String totalText = String.format("%.2fx", totalMult);

        // Positionierung: Links neben dem GUI-Fenster
        int startX = this.x - 105;
        int startY = this.y + 10;
        int width = 100;
        int height = 55;

        // Hintergrundbox
        context.fill(startX, startY, startX + width, startY + height, 0xD0000000);

        // FIX: drawBorder existiert nicht -> drawStrokedRectangle nutzen
        context.drawStrokedRectangle(startX, startY, width, height, 0xFFFFFFFF);

        // Text Rendern
        int textX = startX + 5;
        int textY = startY + 5;
        int lineHeight = 12;

        context.drawTextWithShadow(this.textRenderer, Text.literal("Trim Power").formatted(Formatting.GOLD, Formatting.BOLD), textX, textY, 0xFFFFFFFF);
        textY += 14;

        context.drawTextWithShadow(this.textRenderer, Text.literal("Level: ").formatted(Formatting.GRAY).append(Text.literal(xpText).formatted(Formatting.AQUA)), textX, textY, 0xFFFFFFFF);
        textY += lineHeight;

        context.drawTextWithShadow(this.textRenderer, Text.literal("Survival: ").formatted(Formatting.GRAY).append(Text.literal(survText).formatted(Formatting.RED)), textX, textY, 0xFFFFFFFF);
        textY += lineHeight + 2;

        context.drawTextWithShadow(this.textRenderer, Text.literal("Total: ").formatted(Formatting.WHITE).append(Text.literal(totalText).formatted(Formatting.GREEN, Formatting.BOLD)), textX, textY, 0xFFFFFFFF);
    }
}
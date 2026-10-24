package com.simplebuilding.mixin.client;

import com.simplebuilding.client.gui.DoubleJumpHudOverlay;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The air jump cooldown bar in vanilla's contextual bar slot (owner 2026-09-29): where the
 * {@code Hud} draws the bar it picked (experience, locator or vehicle jump bar), the air jump bar
 * is drawn instead while {@link DoubleJumpHudOverlay#takesTheSlot} says so - after a recent
 * experience change the experience bar wins, exactly like vanilla's own experience-over-locator
 * rule. The experience level number in between stays vanilla's.
 *
 * <p>The same two calls sit in a different method on every loader: vanilla (Fabric) draws the
 * whole group in {@code extractHotbarAndDecorations}; NeoForge splits it into the layers
 * {@code extractContextualInfoBarBackground} / {@code extractContextualInfoBar}; Forge into
 * {@code updateContextualInfo} / {@code extractContextualInfoState}. Each redirect names all of
 * them - on a given loader only its own exist, and one match is required.
 */
@Mixin(Hud.class)
public abstract class HudContextualBarMixin {

    @Redirect(method = {"extractHotbarAndDecorations", "extractContextualInfoBarBackground", "updateContextualInfo"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/contextualbar/ContextualBar;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
    private void simplebuilding$airJumpBarBackground(ContextualBar bar, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (DoubleJumpHudOverlay.takesTheSlot(bar)) {
            DoubleJumpHudOverlay.drawBar(graphics);
        } else {
            bar.extractBackground(graphics, deltaTracker);
        }
    }

    @Redirect(method = {"extractHotbarAndDecorations", "extractContextualInfoBar", "extractContextualInfoState"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/contextualbar/ContextualBar;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
    private void simplebuilding$airJumpBarForeground(ContextualBar bar, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        // The locator bar draws its waypoint dots here; with the air jump bar in the slot they go too.
        if (!DoubleJumpHudOverlay.takesTheSlot(bar)) {
            bar.extractRenderState(graphics, deltaTracker);
        }
    }
}

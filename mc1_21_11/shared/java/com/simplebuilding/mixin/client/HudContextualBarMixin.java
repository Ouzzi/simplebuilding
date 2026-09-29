package com.simplebuilding.mixin.client;

import com.simplebuilding.client.gui.DoubleJumpHudOverlay;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.contextualbar.ContextualBarRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The air jump cooldown bar in vanilla's contextual bar slot (owner 2026-09-29): where the
 * {@code Gui} draws the bar it picked (experience, locator or vehicle jump bar - 1.21.11 has the
 * locator bar too), the air jump bar is drawn instead while
 * {@link DoubleJumpHudOverlay#takesTheSlot} says so - after a recent experience change the
 * experience bar wins, exactly like vanilla's own experience-over-locator rule. The experience
 * level number in between stays vanilla's.
 *
 * <p>Vanilla (Fabric) draws the whole group in {@code renderHotbarAndDecorations}; NeoForge splits
 * it into the layers {@code renderContextualInfoBarBackground} / {@code renderContextualInfoBar}.
 * Each redirect names both - on a given loader only its own exists, and one match is required.
 */
@Mixin(Gui.class)
public abstract class HudContextualBarMixin {

    @Redirect(method = {"renderHotbarAndDecorations", "renderContextualInfoBarBackground"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;renderBackground(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"))
    private void simplebuilding$airJumpBarBackground(ContextualBarRenderer bar, GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (DoubleJumpHudOverlay.takesTheSlot(bar)) {
            DoubleJumpHudOverlay.drawBar(graphics);
        } else {
            bar.renderBackground(graphics, deltaTracker);
        }
    }

    @Redirect(method = {"renderHotbarAndDecorations", "renderContextualInfoBar"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"))
    private void simplebuilding$airJumpBarForeground(ContextualBarRenderer bar, GuiGraphics graphics, DeltaTracker deltaTracker) {
        // The locator bar draws its waypoint dots here; with the air jump bar in the slot they go too.
        if (!DoubleJumpHudOverlay.takesTheSlot(bar)) {
            bar.render(graphics, deltaTracker);
        }
    }
}

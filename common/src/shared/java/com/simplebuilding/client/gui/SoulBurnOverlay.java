package com.simplebuilding.client.gui;

import com.simplebuilding.effect.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Seelenbrand: solange der lokale Spieler den Effekt traegt, liegt ein leichter blau-dunkler
 * Vollbildfilter ueber allem (owner round 11 P1) - ein Farbton statt Text, wie die harte Regel
 * fuer Gadgets es verlangt. Der Server schickt die Dauer, das Icon oben rechts bleibt Vanilla.
 */
public final class SoulBurnOverlay {
    /** Dunkelblau des Seelenfeuers, rund ein Fuenftel deckend. */
    public static final int FILTER = 0x3310233D;

    private SoulBurnOverlay() {
    }

    public static void render(GuiGraphicsExtractor context) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || ModEffects.SOUL_BURN == null) return;
        if (client.gui.hud.isHidden()) return;
        if (!client.player.hasEffect(ModEffects.SOUL_BURN)) return;
        context.fill(0, 0, context.guiWidth(), context.guiHeight(), FILTER);
    }
}

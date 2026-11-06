package com.simplebuildingtools.client.gui;

import com.simplelib.api.client.GadgetHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Where and whether the HUD boxes are drawn: SimpleLib {@link GadgetHud} (SimpleBuilding hands in its client config). */
public final class ModHud {
    private ModHud() {
    }

    public static boolean visible() {
        return GadgetHud.settings().visible();
    }

    public static void begin(GuiGraphicsExtractor context, int width, int height, int offsetY) {
        GadgetHud.begin(context, width, height, offsetY);
    }

    public static void end(GuiGraphicsExtractor context) {
        GadgetHud.end(context);
    }

    static boolean shown(Minecraft client) {
        return GadgetHud.visible(client);
    }
}

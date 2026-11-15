package com.simplebuilding.modules.simpleinterfaces.client;

import com.simplebuilding.modules.simpleinterfaces.linked.LinkedContainers;
import com.simplebuilding.modules.simpleinterfaces.network.LinkedCancelPayload;
import com.simplebuilding.modules.simpleinterfaces.network.LinkedOpenPayload;
import com.simplelib.api.client.GadgetHud;
import com.simplelib.api.client.HudReserve;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * N65 HUD variant A: one tile per marked container in the top left corner, mirrored to Vanilla's effect tiles (top right):
 * 24x24 at x = 1 + 25 i, y = 1, the block's item at +4/+4, a small dot in the hand particle's purple. The gadget HUD's dark
 * box (SimpleLib {@link GadgetHud#background}). HUD only, no screen; client switch {@code linkedHudIcons}. The corner is
 * reserved in SimpleLib ({@link HudReserve}), so gadget boxes moved to the top stay below the tiles. The server says when
 * tiles come and go; a new player object (respawn, rejoin) clears them, because the marks do not survive either. Escape
 * ends every mark ({@link #cancel}).
 */
public final class LinkedHud {
    /** The hand particle's purple ({@code DustParticleOptions(0xB38EF3)}). */
    public static final int DOT = 0xFFB38EF3;
    public static final int TILE = 24, PITCH = 25, X = 1, Y = 1, ICON = 4;
    private static final String RESERVE = "simpleinterfaces:linked";
    private static List<LinkedOpenPayload.Entry> tiles = List.of();
    private static Object owner;

    private LinkedHud() {
    }

    static void set(List<LinkedOpenPayload.Entry> entries) {
        tiles = List.copyOf(entries);
        owner = Minecraft.getInstance().player;
    }

    public static boolean active() {
        Minecraft minecraft = Minecraft.getInstance();
        if (owner != minecraft.player) tiles = List.of();
        return !tiles.isEmpty();
    }

    /**
     * Escape in the world while a container is marked: ends every mark (the server decides, the tiles go at once)
     * instead of opening the pause menu. False when nothing is marked (Escape then behaves as always).
     */
    public static boolean cancel() {
        if (!active()) return false;
        tiles = List.of();
        HudReserve.release(RESERVE);
        LinkedContainers.cancelToServer.accept(new LinkedCancelPayload());
        return true;
    }

    /** Left edge of tile {@code i}. */
    public static int tileX(int i) {
        return X + PITCH * i;
    }

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean shown = active() && minecraft.gui.screen() == null && !minecraft.gui.hud.isHidden() && ContainersClient.config().linkedHudIcons;
        if (!shown) {
            HudReserve.release(RESERVE);
            return;
        }
        HudReserve.topLeft(RESERVE, tileX(tiles.size() - 1) + TILE, Y + TILE);
        int inner = TILE - 2 * GadgetHud.PADDING;
        for (int i = 0; i < tiles.size(); i++) {
            int x = tileX(i);
            GadgetHud.background(graphics, x + GadgetHud.PADDING, Y + GadgetHud.PADDING, inner, inner);
            graphics.item(tiles.get(i).icon(), x + ICON, Y + ICON);
            graphics.fill(x + TILE - 5, Y + TILE - 5, x + TILE - 3, Y + TILE - 3, DOT);
        }
    }
}

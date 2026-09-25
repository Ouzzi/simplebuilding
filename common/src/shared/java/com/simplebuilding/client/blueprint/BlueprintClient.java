package com.simplebuilding.client.blueprint;

import com.simplebuilding.items.custom.BlueprintItem;
import net.minecraft.client.Minecraft;

/**
 * Client-Start der Blaupause, fuer alle Loader gleich: haengt den Editor an das Item. Die
 * Tooltip-Fabrik ({@link BlueprintTooltip#create}) registriert jeder Loader selbst.
 */
public final class BlueprintClient {
    private BlueprintClient() {
    }

    public static void init() {
        BlueprintItem.setClientOpener((player, hand) -> Minecraft.getInstance().gui.setScreen(new BlueprintScreen(player, hand)));
    }
}

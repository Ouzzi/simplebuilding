package com.simplebuilding.modules.simpleinterfaces.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplebuilding.modules.simpleinterfaces.ContainersConfig;
import com.simplebuilding.modules.simpleinterfaces.network.SortPayload;
import com.simplebuilding.modules.simpleinterfaces.sort.ContainerSort;
import java.util.function.Consumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;

/** Sort key (WE25-3), default middle mouse button: asks the server to sort the area under the cursor; nothing is changed locally. */
public final class SortClient {
    public static final KeyMapping KEY = new KeyMapping("key.simpleinterfaces.sort", InputConstants.Type.MOUSE, 2, StyleToggleClient.CATEGORY);
    /** Loader hook: sends the payload when the server knows it. */
    public static Consumer<SortPayload> toServer = p -> {};

    private SortClient() {}

    /** True = the click was a sort request and is consumed (Vanilla's middle click does nothing outside creative). */
    public static boolean click(AbstractContainerScreen<?> screen, MouseButtonEvent event, net.minecraft.world.inventory.Slot hovered) {
        Minecraft mc = Minecraft.getInstance();
        if (!ContainersConfig.on() || !ContainersClient.config().sortKey || mc.player == null || hovered == null
                || mc.player.hasInfiniteMaterials() || !ContainerSort.eligible(screen.getMenu())
                || !KEY.matchesMouse(event)) return false;
        toServer.accept(new SortPayload(screen.getMenu().containerId, hovered.index));
        return true;
    }
}

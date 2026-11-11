package com.simplebuilding.modules.simpleinterfaces.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplebuilding.modules.simpleinterfaces.ContainersConfig;
import com.simplebuilding.modules.simpleinterfaces.network.SortPayload;
import com.simplebuilding.modules.simpleinterfaces.sort.ContainerSort;
import java.util.function.Consumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;

/** Sort key (WE25-3), default middle mouse button: asks the server to sort the area under the cursor; nothing is changed locally. */
public final class SortClient {
    public static final KeyMapping KEY = new KeyMapping("key.simpleinterfaces.sort", InputConstants.Type.MOUSE, 2, StyleToggleClient.CATEGORY);
    /** Loader hook: sends the payload when the server knows it. */
    public static Consumer<SortPayload> toServer = p -> {};

    private SortClient() {}

    /**
     * True = the click was a sort request and is consumed (Vanilla's middle click does nothing outside creative). In creative
     * only the main rows of the creative screen's Survival Inventory tab sort ({@link ContainerSort#creativeInventoryIndex});
     * everywhere else Vanilla's creative middle click (clone a full stack) stays.
     */
    public static boolean click(AbstractContainerScreen<?> screen, MouseButtonEvent event, net.minecraft.world.inventory.Slot hovered) {
        Minecraft mc = Minecraft.getInstance();
        if (!ContainersConfig.on() || !ContainersClient.config().sortKey || mc.player == null || hovered == null
                || !KEY.matchesMouse(event)) return false;
        if (mc.player.hasInfiniteMaterials()) {
            // The creative screen's menu lives only on the client; the server still has the inventory menu open.
            int index = ContainerSort.creativeInventoryIndex(screen instanceof CreativeModeInventoryScreen creative && creative.isInventoryOpen(),
                    hovered.container == mc.player.getInventory(), hovered.getContainerSlot());
            if (index < 0) return false;
            toServer.accept(new SortPayload(mc.player.inventoryMenu.containerId, index));
            return true;
        }
        if (!ContainerSort.eligible(screen.getMenu())) return false;
        toServer.accept(new SortPayload(screen.getMenu().containerId, hovered.index));
        return true;
    }
}

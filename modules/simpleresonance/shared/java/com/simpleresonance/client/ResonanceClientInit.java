package com.simpleresonance.client;

import com.simplelib.api.client.InventoryExtrasClient;

/** Loader-neutral client start (MOB27-3 R2): the resonance button in rebuilt player inventories (Simple Storage's backpack). */
public final class ResonanceClientInit {
    private static boolean done;

    private ResonanceClientInit() {}

    public static void init() {
        if (done) return;
        done = true;
        InventoryExtrasClient.register(new InventoryExtrasClient.Panel() {
            @Override
            public void foreground(net.minecraft.client.gui.GuiGraphicsExtractor graphics, net.minecraft.client.gui.Font font,
                                   net.minecraft.client.Minecraft minecraft, int x, int y, int mouseX, int mouseY,
                                   net.minecraft.world.inventory.Slot hovered, net.minecraft.world.item.ItemStack carried) {
                ResonancePanel.render(graphics, font, minecraft, x + ResonanceLayout.INVENTORY_X, y + ResonanceLayout.INVENTORY_Y, mouseX, mouseY);
            }
        });
    }
}

package com.simplebuildingtools.client;

import com.simplebuildingtools.client.gui.BuildingWandScreen;
import com.simplebuildingtools.client.gui.HudPanel;
import com.simplebuildingtools.client.gui.OctantScreen;
import com.simplebuildingtools.enchantment.BtEnchantments;
import com.simplebuildingtools.items.custom.BuildingWandItem;
import com.simplebuildingtools.items.custom.OctantItem;
import com.simplelib.api.client.GadgetHud;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/** Client start of the tools, the same for every loader: blueprint editor, settings key, octant box (SimpleLib HUD). */
public final class BtClient {
    private BtClient() {
    }

    public static void init() {
        com.simplebuildingtools.BtClientHooks.setClientThread(() -> Minecraft.getInstance().isSameThread());
        com.simplebuildingtools.client.blueprint.BlueprintClient.init();
        GadgetHud.register(HudPanel.OCTANT_ID, 0, client -> client.player != null && !(client.gui.screen() instanceof OctantScreen)
                && (client.player.getMainHandItem().getItem() instanceof OctantItem || client.player.getOffhandItem().getItem() instanceof OctantItem));
    }

    /** Every client tick: the settings key opens the octant manager or (with Constructor's Touch) the wand settings. */
    public static void tick(Minecraft client) {
        if (ClientState.octantFigureToggleKey != null) {
            while (ClientState.octantFigureToggleKey.consumeClick()) {
                ClientState.showOctantFigure = !ClientState.showOctantFigure;
            }
        }
        if (ClientState.settingsKey == null) return;
        while (ClientState.settingsKey.consumeClick()) {
            if (client.player == null) continue;
            ItemStack stack = client.player.getMainHandItem();
            if (stack.getItem() instanceof OctantItem) {
                client.gui.setScreen(new OctantScreen(stack));
            } else if (stack.getItem() instanceof BuildingWandItem && client.level != null
                    && com.simplebuildingtools.util.guiDrawHelper.hasEnchantment(stack, client, BtEnchantments.CONSTRUCTORS_TOUCH)) {
                client.gui.setScreen(new BuildingWandScreen(stack));
            }
        }
    }
}

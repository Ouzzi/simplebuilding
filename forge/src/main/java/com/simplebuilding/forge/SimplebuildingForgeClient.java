package com.simplebuilding.forge;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.client.ClientState;
import com.simplebuilding.client.gui.NetheriteHopperScreen;
import com.simplebuilding.forge.networking.ForgeNetworkRegistration;
import com.simplebuilding.items.tooltip.ReinforcedBundleTooltipData;
import com.simplebuilding.platform.ClientNetworking;
import com.simplebuilding.util.BundleTooltipAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Forge client wiring on the mod bus (client dist): networking sender, key
 * mappings, menu screen, and tooltip components. Game-bus client events (tick,
 * login, highlight) live in {@link ForgeClientGameEvents}.
 *
 * Not yet ported (no clean Forge 26.1.2 equivalent / cosmetic): in-world build
 * highlight rendering (Forge has no RenderLevelStageEvent), HUD overlays
 * (AddGuiOverlayLayersEvent has a different API), and the enchant_type select
 * item-model property (no Forge registration event).
 */
@Mod.EventBusSubscriber(modid = Simplebuilding.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SimplebuildingForgeClient {
    @SuppressWarnings("deprecation")
    public static final KeyMapping.Category KEY_CATEGORY_SIMPLEMODS = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "simplemods"));

    private SimplebuildingForgeClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ClientNetworking.setSender(ForgeNetworkRegistration::sendToServer);
        event.enqueueWork(() ->
                MenuScreens.register(ForgeModRegistries.NETHERITE_HOPPER_MENU.get(), NetheriteHopperScreen::new));
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        ClientState.highlightToggleKey = new KeyMapping(
                "key.simplebuilding.toggle_highlight",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                KEY_CATEGORY_SIMPLEMODS
        );
        ClientState.octantFigureToggleKey = new KeyMapping(
                "key.simplebuilding.toggle_octant_figure",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                KEY_CATEGORY_SIMPLEMODS
        );
        ClientState.settingsKey = new KeyMapping(
                "key.simplebuilding.simple_settings",
                GLFW.GLFW_KEY_G,
                KEY_CATEGORY_SIMPLEMODS
        );
        event.register(ClientState.highlightToggleKey);
        event.register(ClientState.octantFigureToggleKey);
        event.register(ClientState.settingsKey);
    }

    @SubscribeEvent
    public static void onRegisterTooltips(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(ReinforcedBundleTooltipData.class, data -> {
            ClientBundleTooltip component = new ClientBundleTooltip(data.contents());
            float scale = (float) data.maxCapacity() / 64.0f;
            ((BundleTooltipAccessor) component).simplebuilding$setCapacityScale(scale);
            return component;
        });
    }
}

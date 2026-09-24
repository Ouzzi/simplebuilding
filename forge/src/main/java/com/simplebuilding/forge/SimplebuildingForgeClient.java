package com.simplebuilding.forge;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.client.ClientState;
import com.simplebuilding.client.gui.NetheriteHopperScreen;
import com.simplebuilding.forge.networking.ForgeNetworkRegistration;
import com.simplebuilding.items.tooltip.ReinforcedBundleTooltipData;
import com.simplebuilding.client.gui.tooltip.ReinforcedBundleTooltips;
import com.simplebuilding.platform.ClientNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
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
 * In-world highlights (sledgehammer field, vein/strip miner, building wand preview)
 * and the extra breaking cracks have no Forge 65 event (no extract/submit hook like
 * NeoForge's ExtractLevelRenderStateEvent / SubmitCustomGeometryEvent); they are wired
 * by the client mixins LevelExtractorMixin and LevelRendererMixin in
 * com.simplebuilding.mixin.forge (simplebuilding.forge.mixins.json).
 *
 * Not yet ported (cosmetic): HUD overlays (AddGuiOverlayLayersEvent has a different
 * API). The enchant_type select item-model property has no Forge registration event and is
 * registered by SelectItemModelPropertiesMixin.
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
        event.enqueueWork(() -> {
            MenuScreens.register(ForgeModRegistries.NETHERITE_HOPPER_MENU.get(), NetheriteHopperScreen::new);
            MenuScreens.register(ForgeModRegistries.BACKPACK_MENU.get(), com.simplebuilding.client.gui.BackpackScreen::new);
        });
    }

    /**
     * Forge 65 fuehrt Tasten- und Tooltip-Registrierung auf dem Standard-Bus, nicht auf dem Mod-Bus -
     * ein Mod-Bus-Abonnent dafuer bricht das Laden ab. Darum eine eigene Klasse ohne bus-Parameter.
     */
    @Mod.EventBusSubscriber(modid = Simplebuilding.MOD_ID, value = Dist.CLIENT)
    public static final class DefaultBusEvents {
        private DefaultBusEvents() {
        }

    /** Wie Fabric/NeoForge: schwebender Sand/Kies nutzt den Vanilla-FallingBlockRenderer. */
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(com.simplebuilding.entity.ModEntities.LEVITATING_BLOCK,
                net.minecraft.client.renderer.entity.FallingBlockRenderer::new);
    }

    /** Der getragene Rucksack auf dem Ruecken: beide Spielermodelle und die Mannequins. */
    @SubscribeEvent
    public static void onAddLayers(net.minecraftforge.client.event.EntityRenderersEvent.AddLayers event) {
        for (net.minecraft.world.entity.player.PlayerModelType type : event.getModelTypes()) {
            if (event.getPlayerRenderer(type) instanceof net.minecraft.client.renderer.entity.player.AvatarRenderer<?> player) {
                player.addLayer(new com.simplebuilding.client.render.BackpackLayer(player));
            }
            if (event.getMannequinRenderer(type) instanceof net.minecraft.client.renderer.entity.player.AvatarRenderer<?> mannequin) {
                mannequin.addLayer(new com.simplebuilding.client.render.BackpackLayer(mannequin));
            }
        }
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
        ClientState.backpackKey = new KeyMapping(
                "key.simplebuilding.open_backpack",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                KEY_CATEGORY_SIMPLEMODS
        );
        event.register(ClientState.highlightToggleKey);
        event.register(ClientState.octantFigureToggleKey);
        event.register(ClientState.settingsKey);
        event.register(ClientState.backpackKey);
    }

    /** Abgestellter gefaerbter Rucksack: Leder-Ebene in der Farbe der Block-Entity. */
    @SubscribeEvent
    public static void onRegisterBlockColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Block event) {
        event.register(java.util.List.of(com.simplebuilding.client.render.BackpackBlockTint.INSTANCE),
                com.simplebuilding.blocks.ModBlocks.BACKPACK, com.simplebuilding.blocks.ModBlocks.REINFORCED_BACKPACK,
                com.simplebuilding.blocks.ModBlocks.NETHERITE_BACKPACK, com.simplebuilding.blocks.ModBlocks.ENDERITE_BACKPACK);
    }

    @SubscribeEvent
    public static void onRegisterTooltips(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(ReinforcedBundleTooltipData.class, ReinforcedBundleTooltips::create);
    }
    }
}

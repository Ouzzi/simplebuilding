package com.simplebuilding.neoforge;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.client.ClientState;
import com.simplebuilding.client.DoubleJumpController;
import com.simplebuilding.client.gui.BuildingWandScreen;
import com.simplebuilding.client.gui.NetheriteHopperScreen;
import com.simplebuilding.client.gui.OctantScreen;
import com.simplebuilding.client.property.EnchantmentModelProperty;
import com.simplebuilding.client.render.BlockHighlightRenderer;
import com.simplebuilding.config.SimplebuildingConfig;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.BuildingWandItem;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.items.tooltip.ReinforcedBundleTooltipData;
import com.simplebuilding.networking.*;
import com.simplebuilding.platform.ClientNetworking;
import com.simplebuilding.neoforge.NeoForgeModRegistries;
import com.simplebuilding.util.BundleTooltipAccessor;
import com.simplebuilding.util.EnchantmentHelper;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.serialization.Codec;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigManager;
import me.shedaniel.autoconfig.gui.ConfigScreenProvider;
import me.shedaniel.autoconfig.gui.DefaultGuiProviders;
import me.shedaniel.autoconfig.gui.DefaultGuiTransformers;
import me.shedaniel.autoconfig.gui.registry.GuiRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterSelectItemModelPropertyEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

@Mod(value = Simplebuilding.MOD_ID, dist = Dist.CLIENT)
public final class SimplebuildingNeoForgeClient {
    @SuppressWarnings("deprecation") // KeyMapping.Category.register(Identifier) — same call Fabric uses
    public static final KeyMapping.Category KEY_CATEGORY_SIMPLEMODS = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "simplemods"));
    public static SelectItemModelProperty.Type<EnchantmentModelProperty, String> ENCHANTMENT_PROPERTY_TYPE;

    private boolean wasJumpPressed = false;

    public SimplebuildingNeoForgeClient(IEventBus modEventBus, ModContainer modContainer) {
        // Config button in the NeoForge mods list -> opens the cloth AutoConfig GUI.
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (container, parent) -> buildConfigScreen(parent));
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(SimplebuildingNeoForgeClient::registerMenus);
        modEventBus.addListener(SimplebuildingNeoForgeClient::registerKeys);
        modEventBus.addListener(SimplebuildingNeoForgeClient::registerHudLayers);
        modEventBus.addListener(SimplebuildingNeoForgeClient::registerSelectItemProperties);
        modEventBus.addListener(SimplebuildingNeoForgeClient::registerTooltipComponents);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onRenderLevelAfterTranslucentBlocks);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(NeoForgeClientHooks::onBlockOutlineExtract);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        ClientNetworking.setSender(ClientPacketDistributor::sendToServer);
    }

    private static Screen buildConfigScreen(Screen parent) {
        // cloth-config-neoforge exposes no AutoConfig.getConfigScreen (unlike the Fabric
        // variant / ModMenu path), so build the AutoConfig GUI directly from the config
        // manager + the default GUI registry.
        @SuppressWarnings("unchecked")
        ConfigManager<SimplebuildingConfig> manager =
                (ConfigManager<SimplebuildingConfig>) AutoConfig.getConfigHolder(SimplebuildingConfig.class);
        GuiRegistry registry = DefaultGuiTransformers.apply(DefaultGuiProviders.apply(new GuiRegistry()));
        return new ConfigScreenProvider<>(manager, registry, parent).get();
    }

    public static void registerMenus(RegisterMenuScreensEvent event) {
        event.register(NeoForgeModRegistries.NETHERITE_HOPPER_MENU.get(), NetheriteHopperScreen::new);
    }

    public static void registerKeys(RegisterKeyMappingsEvent event) {
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

    public static void registerHudLayers(RegisterGuiLayersEvent event) {
        NeoForgeClientHooks.registerHudLayers(event);
    }

    public static void registerSelectItemProperties(RegisterSelectItemModelPropertyEvent event) {
        ENCHANTMENT_PROPERTY_TYPE = SelectItemModelProperty.Type.create(EnchantmentModelProperty.CODEC, Codec.STRING);
        EnchantmentModelProperty.PROPERTY_TYPE = ENCHANTMENT_PROPERTY_TYPE;
        event.register(Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "enchant_type"), ENCHANTMENT_PROPERTY_TYPE);
    }

    public static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(ReinforcedBundleTooltipData.class, data -> {
            ClientBundleTooltip component = new ClientBundleTooltip(data.contents());
            float scale = (float) data.maxCapacity() / 64.0f;
            ((BundleTooltipAccessor) component).simplebuilding$setCapacityScale(scale);
            return component;
        });
    }

    private void onPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        SimplebuildingConfig config = AutoConfig.getConfigHolder(SimplebuildingConfig.class).getConfig();
        ClientNetworking.send(new TrimBenefitPayload(config.enableArmorTrimBenefits));
    }

    private void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        while (ClientState.highlightToggleKey != null && ClientState.highlightToggleKey.consumeClick()) {
            ClientState.showHighlights = !ClientState.showHighlights;
            client.player.sendSystemMessage(Component.literal("Highlights: " + (ClientState.showHighlights ? "ON" : "OFF")));
        }
        while (ClientState.octantFigureToggleKey != null && ClientState.octantFigureToggleKey.consumeClick()) {
            ClientState.showHighlights = !ClientState.showHighlights;
            client.player.sendSystemMessage(Component.literal("Octant Figure: " + (ClientState.showHighlights ? "ON" : "OFF")));
        }
        while (ClientState.settingsKey != null && ClientState.settingsKey.consumeClick()) {
            ItemStack stack = client.player.getMainHandItem();
            if (stack.getItem() instanceof OctantItem) {
                client.setScreen(new OctantScreen(stack));
            } else if (stack.getItem() instanceof BuildingWandItem && client.level != null
                    && EnchantmentHelper.hasEnchantment(stack, client.level, ModEnchantments.CONSTRUCTORS_TOUCH)) {
                client.setScreen(new BuildingWandScreen(stack));
            }
        }

        boolean isJumpPressed = client.options.keyJump.isDown();
        if (isJumpPressed != wasJumpPressed) {
            ClientNetworking.send(new SpaceKeyPayload(isJumpPressed));
            wasJumpPressed = isJumpPressed;
        }

        // Shared air-jump + level-dependent cooldown logic (see DoubleJumpController).
        DoubleJumpController.tick(client);
    }

    private void onRenderLevelAfterTranslucentBlocks(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        BlockHighlightRenderer.renderInWorldWithCamera(
                event.getPoseStack(),
                event.getLevelRenderState().cameraRenderState.pos
        );
    }

}

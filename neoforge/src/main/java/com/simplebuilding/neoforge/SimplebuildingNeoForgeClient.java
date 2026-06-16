package com.simplebuilding.neoforge;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.client.ClientState;
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
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
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
    public static final KeyMapping.Category KEY_CATEGORY_SIMPLEMODS = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "simplemods"));
    public static SelectItemModelProperty.Type<EnchantmentModelProperty, String> ENCHANTMENT_PROPERTY_TYPE;

    private boolean jumpKeyPressed = false;
    private int jumpsUsed = 0;
    private boolean wasOnGround = true;
    private boolean wasJumpPressed = false;

    public SimplebuildingNeoForgeClient(IEventBus modEventBus) {
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

        if (Simplebuilding.getConfig().enableDoubleJump) {
            boolean isOnGround = client.player.onGround();
            boolean isClimbing = client.player.onClimbable();
            boolean isInWater = client.player.isInWater();
            boolean jumping = client.options.keyJump.isDown();
            if (isOnGround || isClimbing || isInWater) {
                jumpsUsed = 0;
            } else if (jumping && !jumpKeyPressed && !wasOnGround) {
                int level = getDoubleJumpLevel(client.player);
                if (level > 0 && jumpsUsed < level && !client.player.getAbilities().flying) {
                    Vec3 velocity = client.player.getDeltaMovement();
                    client.player.setDeltaMovement(velocity.x, 0.5, velocity.z);
                    client.player.fallDistance = 0;
                    jumpsUsed++;
                    ClientNetworking.send(new DoubleJumpPayload());
                }
            }
            jumpKeyPressed = jumping;
            wasOnGround = isOnGround;
        }
    }

    private void onRenderLevelAfterTranslucentBlocks(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        BlockHighlightRenderer.renderInWorldWithCamera(
                event.getPoseStack(),
                event.getLevelRenderState().cameraRenderState.pos
        );
    }

    private static int getDoubleJumpLevel(net.minecraft.world.entity.player.Player player) {
        int maxLevel = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            int level = EnchantmentHelper.getEnchantmentLevel(player.getItemBySlot(slot), player.level(), ModEnchantments.DOUBLE_JUMP);
            if (level > maxLevel) {
                maxLevel = level;
            }
        }
        return maxLevel;
    }
}

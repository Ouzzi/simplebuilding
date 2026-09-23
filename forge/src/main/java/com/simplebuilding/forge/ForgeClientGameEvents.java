package com.simplebuilding.forge;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.client.ClientState;
import com.simplebuilding.client.DoubleJumpController;
import com.simplebuilding.client.gui.BuildingWandScreen;
import com.simplebuilding.client.gui.OctantScreen;
import com.simplebuilding.client.render.BlockOutlineSupport;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.BuildingWandItem;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.networking.SpaceKeyPayload;
import com.simplebuilding.networking.TrimBenefitPayload;
import com.simplebuilding.platform.ClientNetworking;
import com.simplebuilding.util.EnchantmentHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge client game-bus events: key polling, double jump, trim sync, outline suppression. The double
 * jump runs through the shared {@link DoubleJumpController} like on Fabric and NeoForge (level, cooldown,
 * payload); only its HUD bar is not registered on Forge.
 */
@Mod.EventBusSubscriber(modid = Simplebuilding.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ForgeClientGameEvents {
    private static boolean wasJumpPressed = false;

    private ForgeClientGameEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
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
                client.setScreenAndShow(new OctantScreen(stack));
            } else if (stack.getItem() instanceof BuildingWandItem && client.level != null
                    && EnchantmentHelper.hasEnchantment(stack, client.level, ModEnchantments.CONSTRUCTORS_TOUCH)) {
                client.setScreenAndShow(new BuildingWandScreen(stack));
            }
        }

        boolean isJumpPressed = client.options.keyJump.isDown();
        if (isJumpPressed != wasJumpPressed) {
            ClientNetworking.send(new SpaceKeyPayload(isJumpPressed));
            wasJumpPressed = isJumpPressed;
        }

        // Prueft enableDoubleJump selbst.
        DoubleJumpController.tick(client);
    }

    @SubscribeEvent
    public static void onClientLogin(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingIn event) {
        ClientNetworking.send(new TrimBenefitPayload(Simplebuilding.getConfig().enableArmorTrimBenefits));
    }

    /** Suppress the vanilla block outline when the mod's highlight system requests it. */
    @SubscribeEvent
    public static boolean onRenderHighlightBlock(RenderHighlightEvent.Block event) {
        return BlockOutlineSupport.suppressVanillaBlockOutline();
    }
}

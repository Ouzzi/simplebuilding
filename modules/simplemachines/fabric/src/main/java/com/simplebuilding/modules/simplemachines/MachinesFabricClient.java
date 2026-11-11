package com.simplebuilding.modules.simplemachines;

import com.simplemachines.upgrade.PistonConfigPayload;
import com.simplemachines.upgrade.PistonUpgrades;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Fabric client entry: the server's piston options (audit N16), the late hooks (nugget hand hint) once the client runs, hopper screens and cart renderers. */
public final class MachinesFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(PistonConfigPayload.ID, (payload, context) -> context.client().execute(payload::apply));
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> PistonUpgrades.registerLateHooks());
        // Stage 3: the hopper menu (one type per tier) and the hopper carts (Vanilla's cart model with the tier hopper).
        com.simplemachines.hopper.Hoppers.MENUS.values().forEach(type -> net.minecraft.client.gui.screens.MenuScreens.register(type, com.simplemachines.client.TierHopperScreen::new));
        // Stage 4: the Auto Smither's menu.
        net.minecraft.client.gui.screens.MenuScreens.register(com.simplemachines.smither.Smithers.MENU, com.simplemachines.client.AutoSmitherScreen::new);
        // Stage 4b: the Autonomous Crafter's menu.
        net.minecraft.client.gui.screens.MenuScreens.register(com.simplemachines.crafter.Crafters.MENU, com.simplemachines.client.AutonomousCrafterScreen::new);
        com.simplemachines.hopper.Hoppers.CARTS.values().forEach(type -> net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(type,
                c -> new net.minecraft.client.renderer.entity.MinecartRenderer(c, net.minecraft.client.model.geom.ModelLayers.HOPPER_MINECART)));
    }
}

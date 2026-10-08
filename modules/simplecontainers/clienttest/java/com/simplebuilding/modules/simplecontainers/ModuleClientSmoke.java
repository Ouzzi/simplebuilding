package com.simplebuilding.modules.simplecontainers;

import java.util.function.Function;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.DispenserScreen;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;

/** Client smoke: the W0 storage screens open styled in a world; screenshots for the owner's review. */
public final class ModuleClientSmoke implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simplecontainers")) throw new AssertionError("Module did not boot");
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            show(context, "simplecontainers-chest", c -> new ContainerScreen(ChestMenu.threeRows(0, c.player.getInventory()), c.player.getInventory(), Component.translatable("container.chest")));
            show(context, "simplecontainers-double-chest", c -> new ContainerScreen(ChestMenu.sixRows(0, c.player.getInventory()), c.player.getInventory(), Component.translatable("container.chestDouble")));
            show(context, "simplecontainers-barrel", c -> new ContainerScreen(ChestMenu.threeRows(0, c.player.getInventory()), c.player.getInventory(), Component.translatable("container.barrel")));
            show(context, "simplecontainers-ender-chest", c -> new ContainerScreen(ChestMenu.threeRows(0, c.player.getInventory()), c.player.getInventory(), Component.translatable("container.enderchest")));
            show(context, "simplecontainers-shulker", c -> new ShulkerBoxScreen(new ShulkerBoxMenu(0, c.player.getInventory()), c.player.getInventory(), Component.translatable("container.shulkerBox")));
            show(context, "simplecontainers-hopper", c -> new HopperScreen(new HopperMenu(0, c.player.getInventory()), c.player.getInventory(), Component.translatable("container.hopper")));
            show(context, "simplecontainers-dispenser", c -> new DispenserScreen(new DispenserMenu(0, c.player.getInventory()), c.player.getInventory(), Component.translatable("container.dispenser")));
            context.runOnClient(c -> c.setScreenAndShow(null));
        }
    }

    private static void show(ClientGameTestContext context, String name, Function<Minecraft, Screen> screen) {
        context.runOnClient(c -> c.setScreenAndShow(screen.apply(c)));
        context.waitTicks(3);
        context.runOnClient(c -> {
            try {
                Object style = Class.forName("com.simplebuilding.modules.simplecontainers.client.StyledScreens")
                        .getMethod("style", net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class).invoke(null, c.gui.screen());
                if (style == null) throw new AssertionError(name + ": screen is not styled");
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
        });
        context.takeScreenshot(name);
    }
}

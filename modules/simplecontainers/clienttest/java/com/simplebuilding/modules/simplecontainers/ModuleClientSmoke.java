package com.simplebuilding.modules.simplecontainers;

import java.util.function.Function;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CrafterScreen;
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.client.gui.screens.inventory.DispenserScreen;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;

/** Client smoke: the storage screens (W0 + W1 G1 crafter and mounts) open styled in a world; screenshots for the owner's review. */
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
            show(context, "simplecontainers-crafter", c -> {
                CrafterMenu menu = new CrafterMenu(0, c.player.getInventory());
                for (int i : new int[] {0, 1, 2, 6, 7, 8}) menu.getSlot(i).set(new ItemStack(Items.IRON_INGOT, 2));
                menu.getSlot(9).set(new ItemStack(Items.BUCKET));
                menu.setData(4, 1); // slot 4 disabled
                menu.setData(9, 1); // powered
                return new CrafterScreen(menu, c.player.getInventory(), Component.translatable("container.crafter"));
            });
            show(context, "simplecontainers-donkey", c -> mount(c, EntityType.DONKEY));
            show(context, "simplecontainers-horse", c -> mount(c, EntityType.HORSE));
            context.runOnClient(c -> c.setScreenAndShow(null));
        }
    }

    /** A tamed mount next to the player (a donkey with a chest: 5 columns); its inventory screen as the server would open it. */
    private static Screen mount(Minecraft c, EntityType<? extends AbstractHorse> type) {
        AbstractHorse horse = type.create(c.level, EntitySpawnReason.LOAD);
        horse.setPos(c.player.position());
        horse.setTamed(true);
        int columns = 0;
        if (horse instanceof AbstractChestedHorse chested) {
            chested.setChest(true);
            columns = chested.getInventoryColumns();
        }
        SimpleContainer container = new SimpleContainer(Math.max(1, columns * 3));
        if (columns > 0) {
            container.setItem(0, new ItemStack(Items.HAY_BLOCK, 12));
            container.setItem(1, new ItemStack(Items.GOLDEN_CARROT, 6));
        }
        HorseInventoryMenu menu = new HorseInventoryMenu(0, c.player.getInventory(), container, horse, columns);
        return new HorseInventoryScreen(menu, c.player.getInventory(), horse, columns);
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

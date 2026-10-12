package com.simplebuilding.modules.simplerespawn;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplebuilding.modules.simplerespawn.client.DownedScreen;
import com.simplebuilding.modules.simplerespawn.client.RespawnClient;
import com.simplebuilding.modules.simplerespawn.client.ViewOnlyInventoryScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * Downed screen, spectate camera, view-only inventory and give up, clicked like a player would (screenshots
 * simplerespawn-*.png). Alone in the world the module would let the player die normally, so the op command path
 * ({@link RespawnServer#knockDown}) puts the player down.
 */
public final class ModuleClientSmoke implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simplerespawn")) throw new AssertionError("Module did not boot");
        context.takeScreenshot("simplerespawn-title");
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                p.setGameMode(GameType.SURVIVAL);
                var level = p.level();
                var base = p.blockPosition();
                for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++)
                    level.setBlockAndUpdate(base.offset(dx, -1, dz), (dx + dz) % 2 == 0 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
                level.setBlockAndUpdate(base.offset(2, 0, 2), Blocks.TORCH.defaultBlockState());
                p.getInventory().add(new ItemStack(Items.IRON_SWORD));
                p.getInventory().add(new ItemStack(Items.BREAD, 12));
                p.getInventory().add(new ItemStack(Items.TORCH, 40));
                p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                RespawnServer.knockDown(p, p.damageSources().generic());
            });
            context.waitTicks(30);
            context.runOnClient(c -> require(c.gui.screen() instanceof DownedScreen, "downed screen open, got " + c.gui.screen()));
            context.takeScreenshot("simplerespawn-downed-screen");

            click(context, "screen.simplerespawn.spectate");
            context.runOnClient(c -> {
                require(c.gui.screen() == null && RespawnClient.orbiting(), "spectate closes the screen and frees the camera");
                RespawnClient.turn(1200, 0); // swing the camera round to the front of the body
            });
            context.waitTicks(10);
            context.takeScreenshot("simplerespawn-spectate");

            context.getInput().pressKey(InputConstants.KEY_E);
            context.waitTicks(25);
            context.runOnClient(c -> require(c.gui.screen() instanceof DownedScreen, "the inventory key opens the downed screen again"));

            click(context, "screen.simplerespawn.inventory");
            context.runOnClient(c -> require(c.gui.screen() instanceof ViewOnlyInventoryScreen, "view-only inventory open"));
            context.takeScreenshot("simplerespawn-inventory");
            int before = context.computeOnClient(c -> c.player.getInventory().countItem(Items.BREAD));
            context.getInput().pressKey(InputConstants.KEY_Q);
            context.waitTicks(5);
            context.runOnClient(c -> require(c.player.getInventory().countItem(Items.BREAD) == before, "nothing can be dropped from the view"));
            context.getInput().pressKey(InputConstants.KEY_E);
            context.waitTicks(25);
            context.runOnClient(c -> require(c.gui.screen() instanceof DownedScreen, "E goes back to the downed screen"));

            click(context, "screen.simplerespawn.give_up");
            context.waitTicks(20);
            context.runOnClient(c -> require(c.gui.screen() instanceof DeathScreen && !RespawnClient.downed(), "give up shows the Vanilla death screen"));
            context.takeScreenshot("simplerespawn-gave-up");
            context.runOnClient(c -> c.player.respawn());
            context.waitTicks(20);
        }
    }

    private static void click(ClientGameTestContext context, String key) {
        double[] point = context.computeOnClient(client -> {
            String label = Component.translatable(key).getString();
            var screen = client.gui.screen();
            require(screen != null, "no screen for " + key);
            AbstractWidget widget = screen.children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast)
                    .filter(w -> w.getMessage().getString().equals(label)).findFirst().orElseThrow(() -> new AssertionError("no button " + label));
            require(widget.active && widget.visible, "button " + label + " active");
            var window = client.getWindow();
            double x = widget.getX() + widget.getWidth() / 2.0;
            double y = widget.getY() + widget.getHeight() / 2.0;
            return new double[]{x * window.getScreenWidth() / window.getGuiScaledWidth(), y * window.getScreenHeight() / window.getGuiScaledHeight()};
        });
        context.getInput().setCursorPos(point[0], point[1]);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTicks(5);
    }

    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}

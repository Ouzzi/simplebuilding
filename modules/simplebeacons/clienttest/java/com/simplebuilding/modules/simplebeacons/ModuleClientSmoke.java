package com.simplebuilding.modules.simplebeacons;

import com.simplebeacons.beacon.StrongBeaconMenu;
import com.simplebeacons.client.StrongBeaconScreen;
import com.simplebeacons.mixin.client.BeaconScreenAccessor;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Client smoke: the Strong Beacon screen (Vanilla style, one extra row; pyramid 5/5 and 3/5, inside the window, no button
 * overlaps), the Strong Beacon on a six level pyramid with its beam, and a Strong Conduit in a full 7x7x7 frame.
 */
public final class ModuleClientSmoke implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simplebeacons")) throw new AssertionError("Module did not boot");
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            var server = world.getServer();
            server.runCommand("gamemode creative @a");
            server.runCommand("time set noon");
            server.runCommand("gamerule advance_time false");
            server.runCommand("weather clear");
            // Six level iron pyramid 12 blocks ahead, the Strong Beacon on top; a Strong Conduit in water with a 7x7x7 frame beside it.
            for (int step = 6; step >= 1; step--) {
                int y = 6 - step;
                server.runCommand("execute at @a run fill ~" + (-step) + " ~" + y + " ~" + (16 - step) + " ~" + step + " ~" + y + " ~" + (16 + step) + " minecraft:iron_block");
            }
            server.runCommand("execute at @a run setblock ~ ~6 ~16 simplebeacons:strong_beacon");
            server.runCommand("execute at @a run fill ~-12 ~2 ~6 ~-6 ~8 ~12 minecraft:glass");
            server.runCommand("execute at @a run fill ~-11 ~3 ~7 ~-7 ~7 ~11 minecraft:water");
            server.runCommand("execute at @a run fill ~-12 ~5 ~6 ~-6 ~5 ~12 minecraft:prismarine_bricks hollow");
            server.runCommand("execute at @a run fill ~-12 ~2 ~9 ~-6 ~8 ~9 minecraft:prismarine_bricks hollow");
            server.runCommand("execute at @a run fill ~-9 ~2 ~6 ~-9 ~8 ~12 minecraft:prismarine_bricks hollow");
            server.runCommand("execute at @a run fill ~-10 ~4 ~8 ~-8 ~6 ~10 minecraft:water");
            server.runCommand("execute at @a run setblock ~-9 ~5 ~9 simplebeacons:strong_conduit[waterlogged=true]");
            server.runCommand("execute as @a at @s run tp @s ~-2 ~3 ~ 20 10");
            context.waitTicks(200);
            context.takeScreenshot("simplebeacons-world");
            show(context, "simplebeacons-screen-5", 5, true);
            show(context, "simplebeacons-screen-3", 3, false);
        }
    }

    private static void show(ClientGameTestContext context, String name, int levels, boolean choose) {
        context.runOnClient(c -> {
            var menu = new StrongBeaconMenu(0, c.player.getInventory());
            menu.getSlot(0).set(new ItemStack(Items.DIAMOND));
            menu.setData(0, levels);
            c.setScreenAndShow(new StrongBeaconScreen(menu, c.player.getInventory(), Component.translatable("container.simplebeacons.strong_beacon")));
        });
        context.waitTicks(3);
        context.runOnClient(c -> {
            if (!(c.gui.screen() instanceof StrongBeaconScreen screen)) throw new AssertionError(name + ": not the Strong Beacon screen");
            if (choose) ((BeaconScreenAccessor) screen).simplebeacons$setPrimary(MobEffects.NIGHT_VISION);
            var buttons = screen.strongButtons();
            // A29: the Strong Beacon's own buttons - three powers on levels 1-3, Luck on level 4 (the secondary power is Vanilla's)
            int[] tiers = {1, 1, 1, 2, 2, 2, 3, 3, 3, 4};
            if (buttons.size() != tiers.length) throw new AssertionError(name + ": 10 buttons expected, got " + buttons.size());
            for (AbstractWidget a : buttons) {
                if (a.getRight() > c.getWindow().getGuiScaledWidth()) throw new AssertionError(name + ": button outside the window");
                for (var child : screen.children()) {
                    if (child != a && child instanceof AbstractWidget b && b.visible && a.getRectangle().intersects(b.getRectangle())) {
                        throw new AssertionError(name + ": button overlaps " + b.getClass().getName());
                    }
                }
                boolean expected = levels >= tiers[buttons.indexOf(a)];
                if (a.active != expected) throw new AssertionError(name + ": button " + buttons.indexOf(a) + " active=" + a.active);
            }
        });
        context.waitTicks(3);
        context.takeScreenshot(name);
        context.runOnClient(c -> c.setScreenAndShow(null));
    }
}

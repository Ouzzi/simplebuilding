package com.simplebuilding.integration.clienttest;

import com.mojang.blaze3d.platform.InputConstants;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * BS25-19 (owner 25.10.: dying with the soulfire blaze head on hangs the game): a real client in survival dies wearing
 * the head - helmet slot and SimpleBuilding's extra head slot, by soul fire, on_fire and generic damage -, the death
 * screen renders for a few seconds, the respawn button is clicked, a command runs as the player, then the world is
 * saved and left. A watchdog thread writes every thread's stack to {@code bs25-threads-N.txt} (run dir) when one step
 * takes longer than 45 s and halts the JVM after 120 s, so a hang shows where it sits instead of blocking the run.
 * Log lines: {@code [bs25] STEP|OK|FAIL|DUMP}, summary {@code [bs25] done: N deaths, faults: M}.
 */
public final class Bs25DeathClientTest implements FabricClientGameTest {
    private static final String HEAD = "simplemobheads:soulfire_blaze_head";
    private static volatile String step = "start";
    private static volatile long stepSince = System.nanoTime();
    private int faults;
    private int deaths;

    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            // From here on (world generation may be slow) every step must finish within 45 s.
            Thread watchdog = new Thread(Bs25DeathClientTest::watch, "bs25-watchdog");
            watchdog.setDaemon(true);
            watchdog.start();
            step("world ready");
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            for (String command : new String[] {"time set noon", "weather clear", "gamerule spawn_mobs false"}) {
                world.getServer().runCommand(command);
            }
            die(context, world, "helmet", "soul_fire");
            die(context, world, "helmet", "on_fire");
            die(context, world, "helmet", "generic");
            die(context, world, "extra", "soul_fire");
            die(context, world, "extra", "generic");
            step("save and quit");
        }
        step("left world");
        System.out.println("[bs25] done: " + deaths + " deaths, faults: " + faults);
        step("finished");
    }

    private void die(ClientGameTestContext context, TestSingleplayerContext world, String slot, String cause) {
        String name = slot + "-" + cause;
        try {
            step(name + ": equip");
            world.getServer().runOnServer(server -> {
                ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                p.setGameMode(GameType.SURVIVAL);
                p.setHealth(p.getMaxHealth());
                ItemStack head = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(HEAD)));
                if (slot.equals("helmet")) {
                    p.setItemSlot(EquipmentSlot.HEAD, head);
                } else {
                    p.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                    try {
                        Class.forName("com.simplebuilding.util.ExtraHead").getMethod("set", Player.class, ItemStack.class).invoke(null, p, head);
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException("no extra head slot", e);
                    }
                }
                BlockPos at = p.blockPosition();
                var level = p.level();
                for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
                    level.setBlockAndUpdate(at.offset(dx, -1, dz), Blocks.SOUL_SAND.defaultBlockState());
                    level.setBlockAndUpdate(at.offset(dx, 0, dz), Blocks.AIR.defaultBlockState());
                }
            });
            world.getConnection().waitForClientboundPackets();
            context.waitTicks(20);
            context.takeScreenshot("bs25-" + name + "-worn");
            step(name + ": dying");
            switch (cause) {
                case "soul_fire" -> world.getServer().runOnServer(server -> {
                    ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                    p.setHealth(2.0F);
                    p.level().setBlockAndUpdate(p.blockPosition(), Blocks.SOUL_FIRE.defaultBlockState());
                });
                case "on_fire" -> world.getServer().runCommand("damage @p 1000 minecraft:on_fire");
                default -> world.getServer().runCommand("damage @p 1000 minecraft:generic");
            }
            context.waitFor(c -> c.gui.screen() instanceof DeathScreen, 600);
            deaths++;
            step(name + ": death screen");
            context.waitTicks(60);
            context.takeScreenshot("bs25-" + name + "-death-screen");
            context.runOnClient(c -> System.out.println("[bs25] " + name + " client head slot: " + c.player.getItemBySlot(EquipmentSlot.HEAD)));
            world.getServer().runOnServer(server -> {
                ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                p.level().setBlockAndUpdate(p.blockPosition(), Blocks.AIR.defaultBlockState());
            });
            step(name + ": respawn click");
            click(context, "deathScreen.respawn");
            context.waitFor(c -> c.gui.screen() == null && c.player != null && c.player.isAlive(), 400);
            step(name + ": command as player");
            context.runOnClient(c -> c.player.connection.sendCommand("gamemode creative"));
            context.waitFor(c -> c.player != null && c.player.isCreative(), 200);
            context.runOnClient(c -> c.player.connection.sendCommand("gamemode survival"));
            context.waitFor(c -> c.player != null && !c.player.isCreative(), 200);
            String held = world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().getFirst()
                    .getItemBySlot(EquipmentSlot.HEAD).toString());
            System.out.println("[bs25] OK " + name + " (head after respawn: " + held + ")");
            context.takeScreenshot("bs25-" + name + "-respawned");
        } catch (RuntimeException | AssertionError e) {
            faults++;
            System.out.println("[bs25] FAIL " + name + " at " + step + ": " + e);
            context.setScreen(() -> null);
        }
    }

    private static void click(ClientGameTestContext context, String key) {
        double[] point = context.computeOnClient(client -> {
            String label = Component.translatable(key).getString();
            var screen = client.gui.screen();
            if (screen == null) throw new AssertionError("no screen for " + key);
            AbstractWidget widget = screen.children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast)
                    .filter(w -> w.getMessage().getString().equals(label)).findFirst().orElseThrow(() -> new AssertionError("no button " + label));
            if (!widget.active || !widget.visible) throw new AssertionError("button " + label + " inactive");
            var window = client.getWindow();
            double x = widget.getX() + widget.getWidth() / 2.0;
            double y = widget.getY() + widget.getHeight() / 2.0;
            return new double[] {x * window.getScreenWidth() / window.getGuiScaledWidth(), y * window.getScreenHeight() / window.getGuiScaledHeight()};
        });
        context.getInput().setCursorPos(point[0], point[1]);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTicks(5);
    }

    private static void step(String name) {
        step = name;
        stepSince = System.nanoTime();
        System.out.println("[bs25] STEP " + name);
    }

    /** One dump after 45 s on the same step, another after 90 s, then halt: a hang becomes a stack instead of a stuck run. */
    private static void watch() {
        int dumps = 0;
        String last = null;
        while (!"finished".equals(step)) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                return;
            }
            if (!step.equals(last)) {
                last = step;
                dumps = 0;
            }
            long seconds = (System.nanoTime() - stepSince) / 1_000_000_000L;
            if (seconds >= 45L * (dumps + 1) && dumps < 2) {
                dumps++;
                dump(dumps, seconds);
            } else if (seconds >= 120) {
                System.out.println("[bs25] HALT: step '" + step + "' hangs for " + seconds + " s");
                Runtime.getRuntime().halt(3);
            }
        }
    }

    private static void dump(int n, long seconds) {
        StringBuilder out = new StringBuilder("step '" + step + "' for " + seconds + " s\n");
        for (Map.Entry<Thread, StackTraceElement[]> e : Thread.getAllStackTraces().entrySet()) {
            out.append("\n\"").append(e.getKey().getName()).append("\" ").append(e.getKey().getState()).append('\n');
            for (StackTraceElement frame : e.getValue()) out.append("    at ").append(frame).append('\n');
        }
        Path file = Path.of("bs25-threads-" + n + ".txt");
        try {
            Files.writeString(file, out);
        } catch (IOException ignored) {
            // stdout below still carries it
        }
        System.out.println("[bs25] DUMP " + n + " -> " + file.toAbsolutePath() + "\n" + out);
    }
}

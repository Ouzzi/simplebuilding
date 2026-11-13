package com.simplebuilding.integration.clienttest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;

/**
 * TZ38D (owner 13.11., N42: "Client-Screenshot von oben + Gang-Ansicht"): the order areas of both test centre variants built by
 * the real command ({@code /testcenter order build lines|blocks x y z}) with every module loaded and photographed - from above
 * and walking along the grey centre line. Only vanilla, Fabric and commands (like Rel10). Log {@code [tz38d] OK|FAIL}; the
 * pictures are judged by eye (integration/run-fabric-263/screenshots, tools/testrunner target tz38d-client-263).
 */
public final class Tz38dClientTest implements FabricClientGameTest {
    private final List<String> faults = new ArrayList<>();
    private int shots;

    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            for (String command : new String[] {"gamemode spectator @a", "time set noon", "weather clear", "gamerule advance_time false",
                    "gamerule advance_weather false", "gamerule spawn_mobs false"}) {
                world.getServer().runCommand(command);
            }
            context.runOnClient(c -> {
                c.options.hideGui = true;
                c.options.renderDistance().set(16);
            });
            BlockPos base = world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().getFirst().blockPosition());
            BlockPos lines = base.offset(8, 0, 8);
            guard("lines", () -> {
                world.getServer().runCommand(String.format(Locale.ROOT, "testcenter order build lines %d %d %d",
                        lines.getX(), lines.getY(), lines.getZ()));
                // Die Mittellinie liegt am Ende der Seite A (Metalle bis ~100 tief): erst von oben, dann den Gang entlang.
                int lineZ = lineZ(world, lines);
                view(context, world, lines.offset(30, 70, lineZ - 10), 90, 0, "lines-top-metals");
                view(context, world, lines.offset(80, 70, lineZ - 10), 90, 0, "lines-top-wood-rest");
                view(context, world, lines.offset(40, 70, lineZ + 40), 90, 0, "lines-top-stone");
                view(context, world, lines.offset(1, 2, lineZ), 15, -90, "lines-aisle-start");
                view(context, world, lines.offset(40, 2, lineZ), 15, -90, "lines-aisle-wood");
                view(context, world, lines.offset(6, 4, lineZ), 35, 180, "lines-metals-close");
                view(context, world, lines.offset(6, 4, lineZ), 35, 0, "lines-stone-close");
            });
            BlockPos blocks = base.offset(8, 0, -260);
            guard("blocks", () -> {
                world.getServer().runCommand(String.format(Locale.ROOT, "testcenter order build blocks %d %d %d",
                        blocks.getX(), blocks.getY(), blocks.getZ()));
                view(context, world, blocks.offset(60, 70, 20), 90, 0, "blocks-top");
            });
        }
        faults.forEach(line -> System.out.println("[tz38d] FAULT " + line));
        System.out.println("[tz38d] screens shot: " + shots + ", faults: " + faults.size());
    }

    /** z of the grey line relative to the area origin: the first grey concrete slab in the column x+1. */
    private static int lineZ(TestSingleplayerContext world, BlockPos origin) {
        return world.getServer().computeOnServer(server -> {
            var level = server.overworld();
            for (int z = 0; z < 400; z++) {
                var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(origin.offset(1, 0, z)).getBlock());
                if (id.getPath().equals("gray_concrete_slab") || id.getPath().equals("gray_concrete_octet")) {
                    return z;
                }
            }
            return 100;
        });
    }

    private void view(ClientGameTestContext context, TestSingleplayerContext world, BlockPos at, float pitch, float yaw, String name) {
        world.getServer().runCommand(String.format(Locale.ROOT, "tp @a %d.5 %d %d.5 %.1f %.1f", at.getX(), at.getY(), at.getZ(), yaw, pitch));
        world.getConnection().waitForClientboundPackets();
        context.waitTicks(10);
        world.getConnection().waitForChunksRender();
        context.waitTicks(10);
        String file = String.format(Locale.ROOT, "tz38d-%02d-%s", shots, name);
        context.takeScreenshot(file);
        shots++;
        System.out.println("[tz38d] OK " + file);
    }

    private void guard(String what, Runnable work) {
        try {
            work.run();
        } catch (Throwable e) {
            faults.add(what + ": " + e);
            System.out.println("[tz38d] FAIL " + what + ": " + e);
        }
    }
}

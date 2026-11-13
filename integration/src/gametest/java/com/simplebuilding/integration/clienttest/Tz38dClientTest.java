package com.simplebuilding.integration.clienttest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;

/**
 * TZ38D/TZ38E (owner 13.11., N42: "Client-Screenshot von oben + Gang-Ansicht"; round 2 adds the wooden fences close up and a
 * night view of the light over the placeholders): the order areas of both test centre variants built by
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
            // Zuschauer-Modus: kein Hotbar-HUD vor dem Bild; weite Sicht fuer die Draufsicht.
            context.runOnClient(c -> c.options.renderDistance().set(10));
            BlockPos base = world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().getFirst().blockPosition());
            BlockPos lines = base.offset(8, 0, 8);
            guard("lines", () -> {
                world.getServer().runCommand(String.format(Locale.ROOT, "testcenter order build lines %d %d %d",
                        lines.getX(), lines.getY(), lines.getZ()));
                // TZ38E: Seite A (Metalle ~100 tief, dann Holz, Verwitterndes, dunkler Stein) liegt vor der Linie, Seite B dahinter.
                int lineZ = lineZ(world, lines);
                view(context, world, lines.offset(22, 62, lineZ - 40), 90, 0, "lines-top-metals");
                view(context, world, lines.offset(22, 62, lineZ - 12), 90, 0, "lines-top-metals-near");
                view(context, world, lines.offset(52, 62, lineZ - 18), 90, 0, "lines-top-wood-aging-stone");
                view(context, world, lines.offset(38, 48, lineZ + 10), 90, 0, "lines-top-stone");
                view(context, world, lines.offset(1, 2, lineZ), 15, -90, "lines-aisle-start");
                view(context, world, lines.offset(40, 2, lineZ), 15, -90, "lines-aisle-wood");
                view(context, world, lines.offset(6, 4, lineZ), 35, 180, "lines-metals-close");
                view(context, world, lines.offset(6, 4, lineZ), 35, 0, "lines-stone-close");
                view(context, world, lines.offset(38, 5, lineZ - 2), 55, 180, "lines-wood-fences-close");
                world.getServer().runCommand("time set midnight");
                view(context, world, lines.offset(30, 50, lineZ - 20), 90, 0, "lines-night-light");
                world.getServer().runCommand("time set noon");
            });
            BlockPos blocks = base.offset(8, 0, -260);
            guard("blocks", () -> {
                world.getServer().runCommand(String.format(Locale.ROOT, "testcenter order build blocks %d %d %d",
                        blocks.getX(), blocks.getY(), blocks.getZ()));
                view(context, world, blocks.offset(60, 48, 20), 90, 0, "blocks-top");
            });
        }
        faults.forEach(line -> System.out.println("[tz38d] FAULT " + line));
        System.out.println("[tz38d] screens shot: " + shots + ", faults: " + faults.size());
    }

    /** z of the grey line relative to the area origin: the title signs stand at x 0 right before and after it (TZ38E). */
    private static int lineZ(TestSingleplayerContext world, BlockPos origin) {
        return world.getServer().computeOnServer(server -> {
            var level = server.overworld();
            for (int z = 0; z < 400; z++) {
                var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(origin.offset(0, 0, z)).getBlock());
                if (id.getPath().equals("oak_sign")) {
                    return z + 1;
                }
            }
            return 100;
        });
    }

    private void view(ClientGameTestContext context, TestSingleplayerContext world, BlockPos at, float pitch, float yaw, String name) {
        world.getServer().runCommand(String.format(Locale.ROOT, "tp @a %d.5 %d %d.5 %.1f %.1f", at.getX(), at.getY(), at.getZ(), yaw, pitch));
        world.getConnection().waitForClientboundPackets();
        context.waitTicks(40);
        try {
            world.getConnection().waitForChunksRender();
        } catch (AssertionError e) {
            // Software-Rendering (xvfb) wird mit weiten Blicken nicht immer fertig: das Bild trotzdem nehmen.
            System.out.println("[tz38d] INFO chunks still rendering for " + name);
        }
        context.waitTicks(20);
        String file = String.format(Locale.ROOT, "tz38e-%02d-%s", shots, name);
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

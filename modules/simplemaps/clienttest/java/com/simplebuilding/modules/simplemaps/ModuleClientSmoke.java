package com.simplebuilding.modules.simplemaps;

import com.simplemaps.MapsComponents;
import com.simplemaps.SimpleMaps;
import com.simplemaps.Waypoint;
import com.simplemaps.client.WayfinderScreen;
import com.simplemaps.net.WaypointEditPayload;
import java.util.Optional;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;

/** Client smoke: map in hand, the map screen (modes, context menu, waypoints), the locator bar and a framed map. */
public final class ModuleClientSmoke implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simplemaps")) throw new AssertionError("Module did not boot");
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("gamemode survival @a");
            world.getServer().runCommand("time set day");
            world.getServer().runCommand("item replace entity @a weapon.mainhand with simplemaps:wayfinder_map");
            context.waitTicks(60);
            context.runOnClient(c -> {
                if (!c.player.getMainHandItem().has(MapsComponents.MAP_ID)) throw new AssertionError("map got no id while held");
            });
            context.takeScreenshot("simplemaps-hand");
            screen(context, "simplemaps-screen", true, true, false, 0);
            // Waypoints through the real packet path: one colour, one mob head.
            context.runOnClient(c -> {
                int x = c.player.getBlockX(), z = c.player.getBlockZ();
                SimpleMaps.toServer.accept(new WaypointEditPayload(0, false, new Waypoint(0, x + 30, z - 20, "Camp", 0xFFF9801D, Optional.empty())));
                SimpleMaps.toServer.accept(new WaypointEditPayload(0, false,
                        new Waypoint(2, x - 40, z + 25, "Mine", 0xFFFFFFFF, Optional.of(Identifier.withDefaultNamespace("creeper_head")))));
            });
            context.waitTicks(10);
            context.runOnClient(c -> {
                var list = c.player.getMainHandItem().get(MapsComponents.WAYPOINTS);
                if (list == null || list.list().size() != 2) throw new AssertionError("waypoints not stored on the map: " + list);
            });
            screen(context, "simplemaps-waypoints", true, true, false, 0);
            context.runOnClient(c -> {
                WayfinderScreen s = (WayfinderScreen) c.gui.screen();
                s.mouseClicked(new MouseButtonEvent(s.mapCenterX() + 20, s.mapCenterY() + 10, new MouseButtonInfo(1, 0)), false);
            });
            context.waitTicks(2);
            context.takeScreenshot("simplemaps-context-menu");
            context.runOnClient(c -> {
                WayfinderScreen s = (WayfinderScreen) c.gui.screen();
                s.mouseClicked(new MouseButtonEvent(s.mapCenterX() + 26, s.mapCenterY() + 16, new MouseButtonInfo(0, 0)), false);
            });
            context.waitTicks(2);
            context.takeScreenshot("simplemaps-pick-slot");
            screen(context, "simplemaps-contour", true, false, true, 0);
            screen(context, "simplemaps-zoom", false, true, false, 2);
            context.runOnClient(c -> c.gui.setScreen(null));
            context.waitTicks(5);
            context.takeScreenshot("simplemaps-locator");
            // Feature 4: a framed copy shows its stored view.
            world.getServer().runCommand("execute as @p at @s run setblock ~ ~1 ~3 minecraft:stone");
            world.getServer().runCommand("execute as @p at @s run tp @s ~ ~ ~ 0 0");
            world.getServer().runCommand("execute as @p at @s run summon minecraft:item_frame ~ ~1 ~2 {Facing:2b,Item:{id:\"simplemaps:wayfinder_map\",count:1,components:{\"simplemaps:map_id\":0}}}");
            world.getServer().runCommand("item replace entity @a weapon.mainhand with minecraft:air");
            context.waitTicks(40);
            context.takeScreenshot("simplemaps-frame");
        }
    }

    private static void screen(ClientGameTestContext context, String name, boolean snap, boolean grid, boolean contour, int zoom) {
        context.runOnClient(c -> {
            WayfinderScreen.setModes(snap, grid, contour, zoom);
            c.setScreenAndShow(WayfinderScreen.forHand(InteractionHand.MAIN_HAND));
        });
        context.waitTicks(30);
        context.takeScreenshot(name);
    }
}

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
            // Owner N38: the map fills the window instead of the old fixed 12 x 9 cells.
            context.runOnClient(c -> {
                WayfinderScreen s = (WayfinderScreen) c.gui.screen();
                if (s.mapCols() <= 12 || s.mapRows() <= 9) throw new AssertionError("map not enlarged: " + s.mapCols() + "x" + s.mapRows());
            });
            // Waypoints through the real packet path: one colour, one mob head.
            context.runOnClient(c -> {
                int x = c.player.getBlockX(), z = c.player.getBlockZ();
                SimpleMaps.toServer.accept(new WaypointEditPayload(0, false, new Waypoint(0, x + 30, z - 20, "Camp", 0xFFF9801D, Identifier.withDefaultNamespace("furnace"), com.simplemaps.CubeNet.NORTH)));
                SimpleMaps.toServer.accept(new WaypointEditPayload(0, false,
                        new Waypoint(2, x - 40, z + 25, "Mine", 0xFFFFFFFF, Identifier.withDefaultNamespace("creeper_head"), com.simplemaps.CubeNet.MODEL)));
            });
            context.waitTicks(10);
            context.runOnClient(c -> {
                var list = c.player.getMainHandItem().get(MapsComponents.WAYPOINTS);
                if (list == null || list.list().size() != 2) throw new AssertionError("waypoints not stored on the map: " + list);
            });
            screen(context, "simplemaps-waypoints", true, true, false, 0);
            // Owner N65: the configure dialog (icon grid, 3D button, unfolded cube) and the saved height-line mode in the hand.
            context.runOnClient(c -> ((WayfinderScreen) c.gui.screen()).openConfigure(0));
            context.waitTicks(5);
            context.takeScreenshot("simplemaps-configure");
            context.runOnClient(c -> c.gui.setScreen(null));
            context.runOnClient(c -> SimpleMaps.toServer.accept(new com.simplemaps.net.DisplayPayload(0, new MapsComponents.Display(true, 4))));
            context.waitTicks(20);
            context.takeScreenshot("simplemaps-hand-contour-zoom4");
            context.runOnClient(c -> SimpleMaps.toServer.accept(new com.simplemaps.net.DisplayPayload(0, MapsComponents.Display.DEFAULT)));
            context.waitTicks(5);
            context.runOnClient(c -> c.setScreenAndShow(WayfinderScreen.forHand(InteractionHand.MAIN_HAND)));
            context.waitTicks(10);
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
            // Feature 3: discovered structures (fake marks through the client path), then switched off by the bookmark.
            context.runOnClient(c -> {
                int x = c.player.getBlockX(), z = c.player.getBlockZ(), id = c.player.getMainHandItem().get(MapsComponents.MAP_ID).id();
                com.simplemaps.client.MapsClient.receive(new com.simplemaps.net.MapStatePayload(id, "minecraft:overworld", 1, true, java.util.List.of(
                        new com.simplemaps.WayfinderData.Mark("minecraft:village_plains", x + 45, z + 30),
                        new com.simplemaps.WayfinderData.Mark("minecraft:ancient_city", x - 60, z - 35),
                        new com.simplemaps.WayfinderData.Mark("minecraft:mineshaft", x + 10, z - 50))));
            });
            screen(context, "simplemaps-structures", true, true, false, 0);
            context.runOnClient(c -> {
                WayfinderScreen s = (WayfinderScreen) c.gui.screen();
                s.mouseClicked(new MouseButtonEvent(s.leftTabCenterX(), s.leftTabCenterY(4), new MouseButtonInfo(0, 0)), false);
            });
            context.waitTicks(3);
            context.takeScreenshot("simplemaps-structures-off");
            context.runOnClient(c -> {
                WayfinderScreen s = (WayfinderScreen) c.gui.screen();
                s.mouseClicked(new MouseButtonEvent(s.leftTabCenterX(), s.leftTabCenterY(4), new MouseButtonInfo(0, 0)), false);
            });
            screen(context, "simplemaps-contour", true, false, true, 0);
            screen(context, "simplemaps-zoom", false, true, false, 3);
            // N48: every zoom level, then keyboard panning and wheel zoom on the cursor.
            screen(context, "simplemaps-zoom-1", false, true, false, 0);
            screen(context, "simplemaps-zoom-2", false, true, false, 1);
            screen(context, "simplemaps-zoom-4", false, true, false, 2);
            screen(context, "simplemaps-zoom-8", false, true, false, 3);
            screen(context, "simplemaps-zoom-16", false, true, false, 4);
            screen(context, "simplemaps-zoom-32", false, true, false, 5);
            screen(context, "simplemaps-zoom-64", false, true, false, 6);
            // Zoom closer than 1:1 (negative levels = 2, 4, 8 screen pixels per block), then the layers and coordinates.
            screen(context, "simplemaps-zoom-in-2", false, true, false, -1);
            screen(context, "simplemaps-zoom-in-8", false, true, false, -3);
            context.runOnClient(c -> {
                if (!com.simplemaps.client.MapLayers.on(com.simplemaps.client.MapLayers.CHUNKS)) com.simplemaps.client.MapLayers.toggle(com.simplemaps.client.MapLayers.CHUNKS);
                if (!com.simplemaps.client.MapLayers.coordsOn()) com.simplemaps.client.MapLayers.toggleCoords();
            });
            screen(context, "simplemaps-layers-coords", false, true, false, 0);
            context.runOnClient(c -> {
                com.simplemaps.client.MapLayers.toggle(com.simplemaps.client.MapLayers.CHUNKS);
                com.simplemaps.client.MapLayers.toggleCoords();
            });
            screen(context, "simplemaps-nav", false, true, false, 1);
            context.runOnClient(c -> {
                WayfinderScreen s = (WayfinderScreen) c.gui.screen();
                int bx = s.blockAtMapCenterX();
                s.keyPressed(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_D, 0, 0));
                if (s.blockAtMapCenterX() <= bx) throw new AssertionError("D did not pan right");
                s.mouseScrolled(s.mapCenterX() + 30, s.mapCenterY() + 20, 0, 1);
                if (WayfinderScreen.zoom() != 1) throw new AssertionError("wheel up did not zoom in to 1:1, got 1:" + WayfinderScreen.zoom());
                s.keyPressed(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_HOME, 0, 0));
            });
            context.waitTicks(2);
            context.takeScreenshot("simplemaps-nav-home");
            // GUI scale 1: the same window has twice the GUI pixels, the map grows with it.
            context.runOnClient(c -> {
                c.options.guiScale().set(1);
                c.resizeGui();
            });
            screen(context, "simplemaps-screen-gui1", true, true, false, 0);
            context.runOnClient(c -> {
                c.options.guiScale().set(0);
                c.resizeGui();
            });
            context.runOnClient(c -> c.gui.setScreen(null));
            context.waitTicks(5);
            context.takeScreenshot("simplemaps-locator");
            // Owner N38: Nether and End maps in the Overworld show an animated jamming signal, not the Overworld.
            world.getServer().runCommand("item replace entity @a weapon.mainhand with simplemaps:nether_wayfinder_map");
            world.getServer().runCommand("item replace entity @a weapon.offhand with simplemaps:end_wayfinder_map");
            context.waitTicks(20);
            context.takeScreenshot("simplemaps-jammed");
            world.getServer().runCommand("item replace entity @a weapon.offhand with minecraft:air");
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
        // The height-line mode is saved on the map (owner N65): set it through the real packet path.
        context.runOnClient(c -> SimpleMaps.toServer.accept(new com.simplemaps.net.DisplayPayload(0, new MapsComponents.Display(contour, 1))));
        context.waitTicks(3);
        context.runOnClient(c -> {
            WayfinderScreen.setModes(snap, grid, contour, zoom);
            c.setScreenAndShow(WayfinderScreen.forHand(InteractionHand.MAIN_HAND));
        });
        context.waitTicks(30);
        context.takeScreenshot(name);
    }
}

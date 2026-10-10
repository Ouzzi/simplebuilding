package com.simplebuilding.clientgametest;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplebuilding.client.gui.AstralEnchantingScreen;
import com.simplebuilding.screen.AstralEnchantingMenu;
import com.simplebuilding.version.McVersion;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerInput;

/**
 * The Astral Enchanter's sliders follow a real mouse drag (owner queue N31: "Regler lassen sich nicht ziehen").
 *
 * <p>The table stands in the scene with a ring of bookshelves (tier 30), a diamond pickaxe goes into the item slot by
 * shift click, then the harness drives the very same window path a player's mouse takes ({@code MouseHandler.onButton}
 * / {@code onMove} -> {@code Screen.mouseClicked} / {@code mouseDragged} / {@code mouseReleased}): press on the first
 * slider's left end, drag to its right end, drag back, release. After each move the slider - on the client and on the
 * integrated server - must stand where the cursor is.
 *
 * <p><b>What breaks this case:</b> the press not starting a drag, the movement never reaching
 * {@code mouseDragged}, the drag row not surviving from press to move, or the server not taking the slider clicks.
 */
public final class AstralEnchanterClientTest {
    private static final int TIMEOUT = 120;
    /** Hotbar slot 0 inside the menu: player slots start after the 3 table slots, the hotbar after 27 inventory slots. */
    private static final int HOTBAR_0 = AstralEnchantingMenu.PLAYER_START + 27;

    private AstralEnchanterClientTest() {
    }

    public static void inWorld(Script script) {
        if (!McVersion.ASTRAL_ENCHANTING) return;
        TestScene.build(script, "minecraft:stone", "creative");
        // Table in the wall spot the player looks at, a ring of shelves around it (front column left open for the view).
        script.command("fill 8 1 18 12 2 22 minecraft:bookshelf");
        script.command("fill 9 1 19 11 2 21 minecraft:air");
        script.command("fill 9 1 18 11 2 18 minecraft:air");
        script.command("setblock 10 1 20 simplebuilding:astral_enchanting_table");
        script.command("item replace entity @a hotbar.0 with minecraft:diamond_pickaxe");
        script.command("tp @a 10.5 0.0 16.5 0.0 10.0");
        script.awaitPackets();
        script.idle("let the table and the shelves reach the client", 10);

        script.harness("right click the astral enchanter", harness -> harness.pressMouse(InputConstants.MOUSE_BUTTON_RIGHT));
        script.await("wait for the astral enchanter screen", TIMEOUT,
                c -> c.gui.screen() instanceof AstralEnchantingScreen,
                c -> "the screen never opened; it is " + c.gui.screen() + ". " + TestScene.describeAim(c));
        script.act("shift click the pickaxe into the item slot", c -> c.gameMode.handleContainerInput(
                menu(c).containerId, HOTBAR_0, 0, ContainerInput.QUICK_MOVE, c.player));
        script.await("the three enchantments arrived", TIMEOUT, c -> menu(c).maxLevel(0) > 0 && menu(c).tier() >= 30,
                c -> "no enchantments or a low tier: max " + menu(c).maxLevel(0) + ", tier " + menu(c).tier());
        script.idle("let the screen settle", 5);
        script.shot("astral-enchanter-open");

        int[] expected = new int[1];
        double[][] points = new double[2][];
        script.act("measure the first slider", c -> {
            expected[0] = menu(c).affordable(0);
            if (expected[0] < 1) throw new AssertionError("the first slider cannot move at tier " + menu(c).tier());
            AstralEnchantingScreen screen = (AstralEnchantingScreen) c.gui.screen();
            points[0] = windowPos(c, screen.sliderPoint(0, 0));
            points[1] = windowPos(c, screen.sliderPoint(0, menu(c).maxLevel(0)));
        });
        script.harness("park the cursor on the slider's left end", h -> h.setCursorPos(points[0][0], points[0][1]));
        script.idle("let the cursor arrive", 2);
        script.harness("press the left button", h -> h.holdMouse(InputConstants.MOUSE_BUTTON_LEFT));
        script.idle("let the press reach the screen", 2);
        script.act("the press started a drag on the first slider", c -> {
            AstralEnchantingScreen screen = (AstralEnchantingScreen) c.gui.screen();
            if (screen.draggingRow() != 0) {
                throw new AssertionError("The press on the first slider started no drag (row " + screen.draggingRow() + "). "
                        + diagnosis(c, points[0]));
            }
        });
        script.harness("drag to the slider's right end", h -> h.setCursorPos(points[1][0], points[1][1]));
        script.idle("let the drag reach the screen and the server", 6);
        script.act("the slider followed the drag to the right", c -> expectLevel(c, expected[0], "after dragging right"));
        script.shot("astral-enchanter-dragged");
        script.harness("drag back to the left end", h -> h.setCursorPos(points[0][0], points[0][1]));
        script.idle("let the drag reach the screen and the server", 6);
        script.act("the slider followed the drag back", c -> expectLevel(c, 0, "after dragging back left"));
        script.harness("release the left button", h -> h.releaseMouse(InputConstants.MOUSE_BUTTON_LEFT));
        script.idle("let the release reach the screen", 2);
        script.harness("move after the release", h -> h.setCursorPos(points[1][0], points[1][1]));
        script.idle("let the move reach the screen", 4);
        script.act("a move after the release leaves the slider alone", c -> expectLevel(c, 0, "after the release"));

        script.act("close the screen", c -> c.player.closeContainer());
        script.await("the screen is gone", TIMEOUT, c -> c.gui.screen() == null, c -> "still open: " + c.gui.screen());
        script.command("fill 8 0 18 12 3 22 minecraft:air");
        script.command("clear @a", true);
    }

    private static AstralEnchantingMenu menu(Minecraft c) {
        if (!(c.gui.screen() instanceof AstralEnchantingScreen screen)) {
            throw new AssertionError("the astral enchanter screen is not open: " + c.gui.screen());
        }
        return screen.getMenu();
    }

    private static void expectLevel(Minecraft c, int level, String when) {
        int client = menu(c).chosen(0);
        int server = -1;
        var integrated = c.getSingleplayerServer();
        if (integrated != null && !integrated.getPlayerList().getPlayers().isEmpty()) {
            ServerPlayer player = integrated.getPlayerList().getPlayers().getFirst();
            if (player.containerMenu instanceof AstralEnchantingMenu m) server = m.chosen(0);
        }
        if (client != level || server != level) {
            throw new AssertionError("The first slider stands at " + client + " (client) / " + server + " (server) " + when
                    + ", expected " + level + " (max " + menu(c).maxLevel(0) + ", affordable " + menu(c).affordable(0) + ", drag row "
                    + ((AstralEnchantingScreen) c.gui.screen()).draggingRow() + ").");
        }
    }

    private static String diagnosis(Minecraft c, double[] aimed) {
        AstralEnchantingScreen screen = (AstralEnchantingScreen) c.gui.screen();
        double guiX = c.mouseHandler.xpos() * c.getWindow().getGuiScaledWidth() / c.getWindow().getScreenWidth();
        double guiY = c.mouseHandler.ypos() * c.getWindow().getGuiScaledHeight() / c.getWindow().getScreenHeight();
        int[] knob = screen.sliderPoint(0, 0);
        return "Cursor in the window " + c.mouseHandler.xpos() + "," + c.mouseHandler.ypos() + " (aimed " + aimed[0] + ","
                + aimed[1] + "), in the GUI " + guiX + "," + guiY + "; the knob is at " + knob[0] + "," + knob[1]
                + "; trace" + screen.trace + "; window active " + c.isWindowActive() + ", mouse grabbed " + c.mouseHandler.isMouseGrabbed() + ".";
    }

    /** Inverse of MouseHandler's scaling: guiX = xpos * guiScaledWidth / screenWidth. */
    private static double[] windowPos(Minecraft c, int[] gui) {
        return new double[] {
                (gui[0] + 0.5) * c.getWindow().getScreenWidth() / c.getWindow().getGuiScaledWidth(),
                (gui[1] + 0.5) * c.getWindow().getScreenHeight() / c.getWindow().getGuiScaledHeight()};
    }
}

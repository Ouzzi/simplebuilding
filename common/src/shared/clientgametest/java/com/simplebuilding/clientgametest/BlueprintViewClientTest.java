package com.simplebuilding.clientgametest;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplebuilding.blueprint.BlueprintCode;
import com.simplebuilding.blueprint.BlueprintViewControls;
import com.simplebuilding.client.blueprint.BlueprintScreen;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.joml.Quaternionf;

/**
 * Dragging the blueprint preview rotates it - the interaction the preview's own hint promises
 * ("Drag: rotate, wheel: zoom") - and the two restorations that surround it: holding Ctrl drags
 * the view across the paper, and the reset button brings rotation, zoom and position back.
 *
 * <p>Owner report 2026-10-08: dragging in the 3D preview does nothing. The static chain on 26.3 is
 * complete and sound (SDL button event -> {@code MouseHandler.onButton} -> {@code Screen.mouseClicked}
 * -> {@code draggingView} -> {@code handleAccumulatedMovement} -> {@code Screen.mouseDragged}), so the
 * report cannot be derived from the code alone - this test drives the real window path instead: the
 * harness calls the very same {@code MouseHandler.onButton}/{@code onMove} entry points the window
 * calls, and this test reads back what the screen made of them. The press is checked <em>before</em>
 * the movement, because {@code mouseClicked} is where the drag starts:
 * {@code Screen.mouseClicked} (really {@code ContainerEventHandler.mouseClicked}) returns
 * {@code true} for every click that merely <em>lands</em> on a child, whether that child consumed
 * the click or not. If some child sits inside the preview rectangle, {@code BlueprintScreen} never
 * sets {@code draggingView} and the drag is dead. The failure message therefore names the child.
 *
 * <p>The preview is tested at three heights: its centre, its top row (where the hidden help tabs
 * would live) and its bottom row (where the hidden example button would live). A hidden widget must
 * not claim the press for itself.
 *
 * <p><b>What breaks this case:</b> a child widget inside the preview rectangle that claims the press,
 * the drag flag not surviving from press to movement, the movement never reaching
 * {@code Screen.mouseDragged}, or Ctrl dragging instead of rotating (and vice versa).
 */
public final class BlueprintViewClientTest {

    private static final int SCREEN_TIMEOUT_TICKS = 100;

    /** Straight up: a right click then hits air, so the blueprint's own {@code use} opens the editor. */
    private static final String PLAYER_SPOT_LOOKING_UP = "10.5 0.0 16.5 0.0 -90.0";

    /**
     * A cube of 4 x 4 x 4: valid code (region syntax {@code x0..x1,y0..y1,z0..z1}), small enough to
     * stay untruncated, and definitely a non-empty model - the preview has to draw something for
     * the drag to prove anything. The editor's own test pastes the text {@code stone 2}, which is
     * not valid code and would leave the preview on its empty-view text.
     */
    private static final String CODE_WITH_MODEL = "stone 0..3,0..3,0..3\noak_planks 0..3,4,0..3\nglass 1..2,1..2,0\n$roof = oak_stairs[facing=north]\n$roof 0..3,5,0";

    /** Window pixels the drag travels - large enough that a quarter of it is still visible. */
    private static final double DRAG_PIXELS = 60;

    /** The preview's Ctrl mask, as {@code InputWithModifiers.hasControlDown} reads it. */
    private static final int CTRL_MODIFIERS = 192;

    private BlueprintViewClientTest() {
    }

    /** A left-button press with the Ctrl mask the harness never supplies. */
    private static MouseButtonInfo ctrlInfo() {
        return new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, CTRL_MODIFIERS);
    }

    public static void inWorld(Script script) {
        TestScene.build(script, "minecraft:stone", "creative");
        script.command("item replace entity @a weapon.mainhand with simplebuilding:blueprint");
        script.command("tp @a " + PLAYER_SPOT_LOOKING_UP);
        script.awaitPackets();
        script.idle("let the blueprint and the view angles reach the client", 10);

        script.harness("right click the blueprint into the air",
                harness -> harness.pressMouse(InputConstants.MOUSE_BUTTON_RIGHT));
        script.await("wait for the blueprint editor", SCREEN_TIMEOUT_TICKS,
                client -> client.gui.screen() instanceof BlueprintScreen,
                client -> "the editor never opened; the current screen is " + describeScreen(client));
        script.idle("let the editor render a few frames", 5);

        fillCode(script, CODE_WITH_MODEL);
        script.await("the code parsed into a model", SCREEN_TIMEOUT_TICKS,
                client -> !modelIsEmpty(client),
                client -> "code '" + code(client) + "' never produced a model, so the preview would "
                        + "draw its empty-view text and the test could not prove anything. Problems: "
                        + problems(client));
        script.idle("let the preview settle for the screenshot", 5);
        script.shot("blueprint-editor-layout");

        Later<double[]> centre = new Later<>("the window position of the preview centre");
        Later<double[]> top = new Later<>("the window position of the preview's top row");
        Later<double[]> bottom = new Later<>("the window position of the preview's bottom row");
        Later<double[]> reset = new Later<>("the window position of the reset view button");
        script.act("compute the window positions of the preview and the reset button", client -> {
            Screen screen = screen(client);
            int viewX = intField(screen, "viewX");
            int viewW = intField(screen, "viewW");
            int bodyY = intField(screen, "bodyY");
            int bodyH = intField(screen, "bodyH");
            // MouseHandler scales window coordinates to GUI coordinates with
            // guiX = xpos * guiScaledWidth / screenWidth, so this is that formula inverted.
            centre.set(windowPos(client, viewX + viewW / 2.0, bodyY + bodyH / 2.0));
            top.set(windowPos(client, viewX + viewW / 2.0, bodyY + 8.0));
            bottom.set(windowPos(client, viewX + viewW / 2.0, bodyY + bodyH - 15.0));
            // Queue N23: the reset icon sits inside the preview, top right (16x16, 2 px from the edge).
            reset.set(windowPos(client, viewX + viewW - 10.0, bodyY + 10.0));
        });

        // ================================================================================
        // 1. Dragging rotates the preview - at the centre, the top row and the bottom row.
        // ================================================================================
        dragAndExpectRotation(script, centre, "centre");
        dragAndExpectRotation(script, top, "top row");
        dragAndExpectRotation(script, bottom, "bottom row");

        // ================================================================================
        // 2. With Ctrl held, dragging pans the view instead of rotating it.
        //    The harness hands MouseHandler no keyboard state - every press event carries
        //    modifiers 0 - and mouseClicked decides the pan from the press event's own modifiers,
        //    so a ctrl press cannot travel through the real window path. Screen.mouseClicked,
        //    mouseDragged and mouseReleased are therefore called directly with the ctrl flag a
        //    player's hand (SDL_GetModState) would have produced. The real-mouse plumbing for
        //    press, drag and release is proven by the three rotate drags above.
        // ================================================================================
        Later<double[]> ctrlPress = new Later<>("the GUI position of the ctrl press");
        script.act("a ctrl press reaches the screen", client -> {
            BlueprintScreen sc = screen(client);
            double gx = intField(sc, "viewX") + intField(sc, "viewW") / 2.0;
            double gy = intField(sc, "bodyY") + intField(sc, "bodyH") / 2.0;
            ctrlPress.set(new double[]{gx, gy});
            if (!sc.mouseClicked(new MouseButtonEvent(gx, gy, ctrlInfo()), false)) {
                throw new AssertionError("The screen rejected the ctrl press at GUI " + (int) gx
                        + "," + (int) gy + ". " + viewDiagnosis(client, centre.get()));
            }
        });
        script.act("the ctrl press started a pan drag and a view drag", client -> {
            if (!boolField(screen(client), "draggingPan")) {
                throw new AssertionError("The ctrl press did not set draggingPan, so dragging would "
                        + "rotate instead of panning. " + viewDiagnosis(client, centre.get()));
            }
            if (!boolField(screen(client), "draggingView")) {
                throw new AssertionError("The ctrl press did not set draggingView. "
                        + viewDiagnosis(client, centre.get()));
            }
        });
        script.act("a ctrl drag moves the view and does not rotate it", client -> {
            double[] p = ctrlPress.get();
            BlueprintViewControls controls = controls(client);
            Quaternionf rotationBefore = new Quaternionf(controls.rotation());
            float panBefore = controls.panX();
            screen(client).mouseDragged(new MouseButtonEvent(p[0] + 40, p[1], ctrlInfo()), 40, 0);
            if (panBefore == controls.panX()) {
                throw new AssertionError("The ctrl drag left panX at " + controls.panX() + "; the pan "
                        + "never reached the view. " + viewDiagnosis(client, centre.get()));
            }
            if (!rotationBefore.equals(controls.rotation(), 1.0e-5f)) {
                throw new AssertionError("The ctrl drag rotated the view instead of panning it: "
                        + controls.rotation() + " after, " + rotationBefore + " before. "
                        + viewDiagnosis(client, centre.get()));
            }
        });
        script.act("the ctrl release ends the pan drag", client -> {
            double[] p = ctrlPress.get();
            screen(client).mouseReleased(new MouseButtonEvent(p[0] + 40, p[1], ctrlInfo()));
            if (boolField(screen(client), "draggingPan") || boolField(screen(client), "draggingView")) {
                throw new AssertionError("A drag flag is still set after the ctrl release, so the "
                        + "next press would continue the old drag.");
            }
        });

        // ================================================================================
        // 3. The reset button brings rotation, zoom and position back to their defaults.
        // ================================================================================
        script.act("the view is not at its default after all that dragging", client -> {
            if (controls(client).isDefault()) {
                throw new AssertionError("The view is already at its default before the reset button "
                        + "was pressed, so the click could prove nothing.");
            }
        });
        script.harness("park the cursor on the reset view button",
                harness -> harness.setCursorPos(reset.get()[0], reset.get()[1]));
        script.idle("let the moved cursor be picked up by the screen", 1);
        script.harness("click the reset view button",
                harness -> harness.pressMouse(InputConstants.MOUSE_BUTTON_LEFT));
        script.idle("let the reset reach the view", 2);
        script.act("the reset view button restored the defaults", client -> {
            BlueprintViewControls controls = controls(client);
            if (!controls.isDefault()) {
                throw new AssertionError("After clicking the reset view button the view is still "
                        + "rotation " + controls.rotation() + ", zoom " + controls.zoom() + ", pan "
                        + controls.panX() + "/" + controls.panY() + ". The button either was not "
                        + "pressed or did not reach the controls.");
            }
        });

        script.shot("screen-blueprint-preview-dragged");

        // ================================================================================
        // 4. Queue N23: the help book opens on the Blocks tab with the insert field focused;
        //    the Guide tab ends in a copy button. Screenshots of both for the owner.
        // ================================================================================
        script.act("open the help book", client -> screen(client).toggleHelp());
        script.idle("let the help book render", 3);
        script.act("the help book opens on Blocks with the insert field focused", client -> {
            BlueprintScreen sc = screen(client);
            if (!boolField(sc, "helpOpen") || !boolField(sc, "helpBlocks")) {
                throw new AssertionError("The book opened with helpOpen=" + boolField(sc, "helpOpen")
                        + ", helpBlocks=" + boolField(sc, "helpBlocks") + " - Blocks must be the default tab.");
            }
            if (!(sc.getFocused() instanceof net.minecraft.client.gui.components.EditBox box) || !box.isVisible()) {
                throw new AssertionError("Opening the book focused " + sc.getFocused() + ", not the visible insert field.");
            }
        });
        script.shot("blueprint-help-blocks");
        script.act("the help tabs are icons with their names as tooltips (Queue N29)", client -> {
            BlueprintScreen sc = screen(client);
            iconWithTooltip(widgetField(sc, "blocksTab"), "simplebuilding.blueprint.help.blocks_tab");
            iconWithTooltip(widgetField(sc, "guideTab"), "simplebuilding.blueprint.help.guide_tab");
        });
        script.act("switch to the Guide tab", client -> screen(client).showTab(false));
        script.idle("let the guide render", 3);
        script.shot("blueprint-help-guide");
        script.act("the copy button puts the whole guide on the clipboard", client -> {
            BlueprintScreen sc = screen(client);
            String saved = client.keyboardHandler.getClipboard();
            net.minecraft.client.gui.components.AbstractWidget copy = widgetField(sc, "copyButton");
            if (!copy.visible) {
                throw new AssertionError("The Guide tab shows no visible copy button.");
            }
            iconWithTooltip(copy, "simplebuilding.blueprint.help.copy");
            client.keyboardHandler.setClipboard("");
            copy.mouseClicked(new MouseButtonEvent(copy.getX() + 2, copy.getY() + 2,
                    new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)), false);
            String copied = client.keyboardHandler.getClipboard();
            client.keyboardHandler.setClipboard(saved == null ? "" : saved);
            if (!copied.equals(BlueprintScreen.guideText()) || copied.split("\n## ").length != 7) {
                throw new AssertionError("The copy button put " + copied.length() + " characters with "
                        + (copied.split("\n## ").length - 1) + " headings on the clipboard, not the whole guide.");
            }
        });
        script.act("close the help book", client -> screen(client).toggleHelp());
    }

    /**
     * One full press-drag-release at {@code point}: the press must start a view drag (rotate, no
     * pan) and the movement must change the rotation.
     */
    private static void dragAndExpectRotation(Script script, Later<double[]> point, String where) {
        script.harness("park the cursor in the " + where + " of the preview", harness ->
                harness.setCursorPos(point.get()[0], point.get()[1]));
        script.idle("let the moved cursor be picked up by the screen", 2);

        Later<Quaternionf> rotationBefore = new Later<>("the preview rotation before the " + where + " press");
        script.act("remember the preview rotation", client ->
                rotationBefore.set(new Quaternionf(controls(client).rotation())));

        script.harness("press the left button in the " + where + " of the preview",
                harness -> harness.holdMouse(InputConstants.MOUSE_BUTTON_LEFT));
        script.idle("let the press reach the screen", 1);

        script.act("the press started a rotate drag in the " + where, client -> {
            if (!boolField(screen(client), "draggingView")) {
                throw new AssertionError("Dragging the preview does nothing because the press never "
                        + "started a drag: BlueprintScreen.mouseClicked saw no free view under the cursor. "
                        + viewDiagnosis(client, point.get()));
            }
            if (boolField(screen(client), "draggingPan")) {
                throw new AssertionError("A plain press in the " + where + " started a pan drag, but "
                        + "only Ctrl should pan. " + viewDiagnosis(client, point.get()));
            }
        });

        script.harness("drag the cursor " + (int) DRAG_PIXELS + " px to the right in the " + where, harness ->
                harness.setCursorPos(point.get()[0] + DRAG_PIXELS, point.get()[1]));
        script.idle("let the accumulated movement reach the screen", 3);

        script.act("the drag in the " + where + " rotated the preview", client -> {
            Quaternionf after = controls(client).rotation();
            if (after.equals(rotationBefore.get())) {
                throw new AssertionError("The press started a drag in the " + where + ", but the "
                        + "movement left the preview rotation at " + after + ". Either "
                        + "Screen.mouseDragged was never called or draggingView was cleared in between. "
                        + viewDiagnosis(client, point.get()));
            }
        });

        script.harness("release the left button", harness ->
                harness.releaseMouse(InputConstants.MOUSE_BUTTON_LEFT));
        script.idle("let the release reach the screen", 1);

        script.act("the release ends the drag", client -> {
            if (boolField(screen(client), "draggingView")) {
                throw new AssertionError("draggingView is still set after releaseMouse, so the next "
                        + "press would be treated as a continuation of this drag. "
                        + viewDiagnosis(client, point.get()));
            }
        });
    }

    // =====================================================================================
    // setup
    // =====================================================================================

    private static double[] windowPos(Minecraft client, double guiX, double guiY) {
        return new double[]{
                guiX * client.getWindow().getScreenWidth() / client.getWindow().getGuiScaledWidth(),
                guiY * client.getWindow().getScreenHeight() / client.getWindow().getGuiScaledHeight()
        };
    }

    /** Hand the code to the editor the way a player's clipboard does: pasted in one keystroke. */
    private static void fillCode(Script script, String code) {
        script.act("put '" + code + "' on the clipboard", client -> {
            String saved = client.keyboardHandler.getClipboard();
            client.keyboardHandler.setClipboard(code);
            LAST_CLIPBOARD[0] = saved == null ? "" : saved;
        });
        script.harness("paste the code into the editor",
                harness -> harness.pressKeyInScreen(InputConstants.KEY_V, 2));
        script.act("the code area took the paste", client -> {
            if (!code.equals(code(client))) {
                throw new AssertionError("The code area reads '" + code(client) + "' after pasting '"
                        + code + "'. Focused: " + screen(client).getFocused() + ", screen: "
                        + describeScreen(client) + ".");
            }
        });
        script.act("give the clipboard back", client ->
                client.keyboardHandler.setClipboard(LAST_CLIPBOARD[0]));
        // onCodeChanged only marks the screen dirty; tick() reparses after 150 ms.
        script.idle("let the editor reparse the pasted code", 10);
    }

    private static final String[] LAST_CLIPBOARD = new String[1];

    // =====================================================================================
    // diagnosis
    // =====================================================================================

    /** Everything the click needs to know, in one message: which child, if any, swallowed the press. */
    private static String viewDiagnosis(Minecraft client, double[] windowCentre) {
        Screen screen = screen(client);
        double guiX = windowCentre[0] * client.getWindow().getGuiScaledWidth()
                / client.getWindow().getScreenWidth();
        double guiY = windowCentre[1] * client.getWindow().getGuiScaledHeight()
                / client.getWindow().getScreenHeight();

        StringBuilder out = new StringBuilder();
        out.append("Cursor at GUI ").append((int) guiX).append(",").append((int) guiY)
                .append(" (window ").append((int) windowCentre[0]).append(",").append((int) windowCentre[1])
                .append("), overView=").append(overView(screen, guiX, guiY))
                .append(", helpOpen=").append(boolField(screen, "helpOpen"))
                .append(", helpBlocks=").append(boolField(screen, "helpBlocks"))
                .append(", readOnly=").append(boolField(screen, "readOnly"))
                .append(", code='").append(code(client)).append("'")
                .append(", modelEmpty=").append(modelIsEmpty(client))
                .append(". Children whose widget contains that point: ");

        List<String> claiming = new ArrayList<>();
        for (GuiEventListener child : screen.children()) {
            if (child.isMouseOver(guiX, guiY)) {
                claiming.add(describe(child));
            }
        }
        out.append(claiming.isEmpty() ? "none" : String.join(", ", claiming));
        out.append(". All children: ");
        List<String> all = new ArrayList<>();
        for (GuiEventListener child : screen.children()) {
            boolean visible = !(child instanceof net.minecraft.client.gui.components.AbstractWidget widget)
                    || widget.visible;
            all.add(describe(child) + (visible ? "" : " [hidden]"));
        }
        out.append(String.join(", ", all));
        return out.toString();
    }

    private static String describe(GuiEventListener child) {
        if (!(child instanceof net.minecraft.client.gui.components.AbstractWidget widget)) {
            return child.getClass().getSimpleName();
        }
        return widget.getClass().getSimpleName() + "(" + widget.getX() + "," + widget.getY()
                + " " + widget.getWidth() + "x" + widget.getHeight() + ")";
    }

    /** The screen's own view rectangle test, read through reflection so the numbers cannot drift. */
    private static boolean overView(Screen screen, double mx, double my) {
        return mx >= intField(screen, "viewX") && mx < intField(screen, "viewX") + intField(screen, "viewW")
                && my >= intField(screen, "bodyY") && my < intField(screen, "bodyY") + intField(screen, "bodyH");
    }

    // =====================================================================================
    // reflection helpers (private fields with no accessor - the screen has none and should
    // not grow them just for a test; same approach as trimButtonProblem/expectTextInput)
    // =====================================================================================

    private static BlueprintScreen screen(Minecraft client) {
        if (!(client.gui.screen() instanceof BlueprintScreen screen)) {
            throw new AssertionError("The blueprint editor is not open; the screen is "
                    + describeScreen(client) + ".");
        }
        return screen;
    }

    private static BlueprintViewControls controls(Minecraft client) {
        return field(screen(client), "controls", BlueprintViewControls.class);
    }

    private static <T> T field(Object owner, String name, Class<T> type) {
        try {
            return type.cast(read(owner, name).get(owner));
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static net.minecraft.client.gui.components.AbstractWidget widgetField(Object owner, String name) {
        try {
            return (net.minecraft.client.gui.components.AbstractWidget) read(owner, name).get(owner);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Queue N29: a help tab or the copy button is a small icon (no text label drawn, at most 20 px wide)
     * whose name ({@code key}) is its narration message and appears in its tooltip.
     */
    private static void iconWithTooltip(net.minecraft.client.gui.components.AbstractWidget widget, String key) {
        String name = net.minecraft.network.chat.Component.translatable(key).getString();
        if (widget.getWidth() > 20 || widget instanceof net.minecraft.client.gui.components.Button) {
            throw new AssertionError("'" + name + "' is still a " + widget.getWidth() + " px text button, not an icon: " + widget);
        }
        if (!widget.getMessage().getString().equals(name)) {
            throw new AssertionError("The icon for '" + name + "' is named '" + widget.getMessage().getString() + "'.");
        }
        Object tip;
        try {
            tip = read(widget, "tipText").get(widget);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        if (!(tip instanceof net.minecraft.network.chat.Component text)) {
            throw new AssertionError("The icon for '" + name + "' has no tooltip.");
        }
        if (!text.getString().contains(name)) {
            throw new AssertionError("The tooltip of the '" + name + "' icon reads '" + text + "' and does not name it.");
        }
    }

    private static boolean boolField(Object owner, String name) {
        try {
            return read(owner, name).getBoolean(owner);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static int intField(Object owner, String name) {
        try {
            return read(owner, name).getInt(owner);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Field read(Object owner, String name) {
        try {
            Field field = owner.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("No field '" + name + "' on " + owner.getClass().getName(), e);
        }
    }

    private static String code(Minecraft client) {
        return client.gui.screen() instanceof BlueprintScreen ? codeArea(client).getValue() : "<no screen>";
    }

    private static boolean modelIsEmpty(Minecraft client) {
        if (!(client.gui.screen() instanceof BlueprintScreen screen)) {
            return true;
        }
        return field(screen, "parsed", BlueprintCode.ParseResult.class).model().isEmpty();
    }

    private static String problems(Minecraft client) {
        if (!(client.gui.screen() instanceof BlueprintScreen screen)) {
            return "<no screen>";
        }
        return field(screen, "parsed", BlueprintCode.ParseResult.class).problems().toString();
    }

    private static com.simplebuilding.client.blueprint.BlueprintCodeArea codeArea(Minecraft client) {
        Screen screen = client.gui.screen();
        if (screen == null) {
            throw new AssertionError("There is no screen to hold a code area.");
        }
        for (GuiEventListener child : screen.children()) {
            if (child instanceof com.simplebuilding.client.blueprint.BlueprintCodeArea area) {
                return area;
            }
        }
        throw new AssertionError("The editor has no code area.");
    }

    private static String describeScreen(Minecraft client) {
        return client.gui.screen() == null ? "none" : client.gui.screen().getClass().getName();
    }
}
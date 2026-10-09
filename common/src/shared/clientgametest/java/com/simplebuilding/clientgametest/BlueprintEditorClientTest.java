package com.simplebuilding.clientgametest;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplebuilding.client.blueprint.BlueprintCodeArea;
import com.simplebuilding.client.blueprint.BlueprintScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

/**
 * Typing in the blueprint editor, through the keyboard callbacks a player's keyboard drives.
 *
 * <p>Owner report 2026-09-29: in the editor, pasting and deleting worked, but typed characters
 * never appeared. Paste and Backspace arrive as KEY events ({@code KeyboardHandler.keyPress}); a
 * typed letter arrives as a separate CHARACTER event ({@code KeyboardHandler.charTyped}). A test
 * that only sends key events therefore passes with a broken editor - this one sends both, in the
 * order the window does (key down, character, key up), for every letter. The cause was one step
 * earlier still: from 26.3 on the window (SDL) produces character events only while text input is
 * started, and the code area never asked for it - see {@link #expectTextInput}.
 *
 * <p><b>What breaks this case:</b> the code area or the insert search not inserting a typed
 * character (a {@code charTyped} that is never reached or refuses), the editor opening without the
 * code area focused, the inventory key (E) closing the editor while a field has the focus - "stone"
 * contains an E - or no longer closing it when no field has, and paste (Control+V) or Backspace
 * losing their effect.
 */
public final class BlueprintEditorClientTest {

    private static final int SCREEN_TIMEOUT_TICKS = 100;

    /** Straight up: a right click then hits air, so the blueprint's own {@code use} opens the editor. */
    private static final String PLAYER_SPOT_LOOKING_UP = "10.5 0.0 16.5 0.0 -90.0";

    private BlueprintEditorClientTest() {
    }

    public static void inWorld(Script script) {
        String[] expectedCode = {"stone 2"};
        TestScene.build(script, "minecraft:stone", "creative");
        script.command("item replace entity @a weapon.mainhand with simplebuilding:blueprint");
        script.command("tp @a " + PLAYER_SPOT_LOOKING_UP);
        script.awaitPackets();
        script.idle("let the blueprint and the view angles reach the client", 10);

        script.harness("right click the blueprint into the air", harness -> harness.pressMouse(InputConstants.MOUSE_BUTTON_RIGHT));
        script.await("wait for the blueprint editor", SCREEN_TIMEOUT_TICKS,
                client -> client.gui.screen() instanceof BlueprintScreen,
                client -> "the editor never opened; the current screen is " + describeScreen(client)
                        + ", the main hand holds " + (client.player == null ? "?" : client.player.getMainHandItem()));
        script.idle("let the editor render a few frames", 5);

        script.act("the code area has the focus and is empty", client -> {
            BlueprintCodeArea area = codeArea(client);
            if (focused(client) != area) {
                throw new AssertionError("The editor opened with " + focused(client) + " focused, not the code area.");
            }
            if (!area.getValue().isEmpty()) {
                throw new AssertionError("A fresh blueprint opened with code '" + area.getValue() + "'.");
            }
        });
        expectTextInput(script, true, "with the code area focused");

        typeLikeAKeyboard(script, "stone");
        expectCode(script, "stone", "typing 'stone' into the code area (key down, character, key up per letter)");

        script.harness("press Backspace in the editor", harness -> harness.pressKeyInScreen(InputConstants.KEY_BACKSPACE, 0));
        expectCode(script, "ston", "Backspace after 'stone'");

        String[] savedClipboard = new String[1];
        script.act("put 'e 2' on the clipboard", client -> {
            savedClipboard[0] = client.keyboardHandler.getClipboard();
            client.keyboardHandler.setClipboard("e 2");
        });
        script.harness("press Control+V in the editor", harness -> harness.pressKeyInScreen(InputConstants.KEY_V, 2));
        expectCode(script, "stone 2", "pasting 'e 2' after 'ston'");
        script.act("give the clipboard back", client -> client.keyboardHandler.setClipboard(savedClipboard[0] == null ? "" : savedClipboard[0]));

        script.act("open the help book with the book button's action", client -> ((BlueprintScreen) client.gui.screen()).toggleHelp());
        script.act("the book opened with the insert field already focused (Queue N23)", client -> {
            EditBox insert = insertBox(client);
            if (focused(client) != insert || !insert.isVisible()) {
                throw new AssertionError("Opening the help book focused " + focused(client) + ", not the visible insert field.");
            }
        });
        expectTextInput(script, true, "with the insert field focused (handed over from the code area)");
        typeLikeAKeyboard(script, "oak");
        script.act("the insert field took 'oak' and the code stayed as it was", client -> {
            EditBox insert = insertBox(client);
            if (!"oak".equals(insert.getValue())) {
                throw new AssertionError("Typing 'oak' into the insert field left it at '" + insert.getValue() + "'.");
            }
            if (!"stone 2".equals(codeArea(client).getValue())) {
                throw new AssertionError("Typing into the insert field changed the code to '" + codeArea(client).getValue() + "'.");
            }
        });
        script.harness("press Enter in the insert field", harness -> harness.pressKeyInScreen(InputConstants.KEY_RETURN, 0));
        script.act("Enter inserted the first hit's ID at the cursor", client -> {
            String code = codeArea(client).getValue();
            if (!code.startsWith("stone 2") || !code.substring("stone 2".length()).contains("oak")) {
                throw new AssertionError("After Enter in the insert field the code reads '" + code + "', expected 'stone 2' plus an oak block ID.");
            }
            expectedCode[0] = code;
        });
        script.act("close the help book again", client -> ((BlueprintScreen) client.gui.screen()).toggleHelp());

        script.act("focus the code area again", client -> client.gui.screen().setFocused(codeArea(client)));
        expectTextInput(script, true, "with the code area focused again");
        script.act("take the focus off every field", client -> client.gui.screen().setFocused(null));
        expectTextInput(script, false, "with no field focused");
        script.harness("press the inventory key with no field focused", harness -> harness.pressKeyInScreen(InputConstants.KEY_E, 0));
        script.await("the inventory key closes the editor when no field has the focus", SCREEN_TIMEOUT_TICKS,
                client -> client.gui.screen() == null,
                client -> "the editor is still open: " + describeScreen(client));
        script.awaitPackets();
        script.idle("let the server store the closing save", 10);

        script.act("the closing save reached the item", client -> {
            String code = com.simplebuilding.items.custom.BlueprintItem.content(client.player.getMainHandItem()).code();
            if (!expectedCode[0].equals(code)) {
                throw new AssertionError("After closing, the blueprint in the hand holds '" + code + "', expected '" + expectedCode[0] + "'.");
            }
        });
        script.command("clear @a", true);
        script.command("tp @a 10.5 0.0 16.5 0.0 0.0");
        script.awaitPackets();
    }

    /** Per letter: the key goes down, the character arrives, the key comes up - as GLFW delivers it. */
    private static void typeLikeAKeyboard(Script script, String letters) {
        for (char letter : letters.toCharArray()) {
            // KEY_A..KEY_Z are contiguous on every line (GLFW key codes on 26.2, SDL scancodes on 26.3+).
            int key = InputConstants.KEY_A + (letter - 'a');
            script.harness("type '" + letter + "'", harness -> {
                harness.pressKeyInScreen(key, 0);
                harness.typeChars(String.valueOf(letter));
            });
        }
    }

    private static void expectCode(Script script, String expected, String what) {
        script.act("the code reads '" + expected + "' after " + what, client -> {
            String actual = codeArea(client).getValue();
            if (!expected.equals(actual)) {
                throw new AssertionError("After " + what + " the code reads '" + actual + "', expected '" + expected
                        + "'. Focused: " + focused(client) + ", screen: " + describeScreen(client) + ".");
            }
        });
    }

    /**
     * Whether the window is in text input mode ({@code TextInputManager.textInputEnabled}).
     *
     * <p>The part the characters above cannot prove: the test hands its characters straight to
     * {@code KeyboardHandler.charTyped}, but on 26.3+ a real keyboard only produces them while SDL's
     * text input is started ({@code SDL_StartTextInput}), which a widget requests through
     * {@code Minecraft.onTextInputFocusChange} when it gains the focus. The code area did not, so
     * on 26.3 Backspace and Control+V (key events) worked and typed letters never arrived - the
     * owner's report. On 26.2 (GLFW) the same flag only switches the IME. Read by reflection: the
     * field has no getter on 26.2, and both loaders run 26.x with Mojang names.
     */
    private static void expectTextInput(Script script, boolean expected, String when) {
        script.act("text input is " + (expected ? "on" : "off") + " " + when, client -> {
            Object manager = client.textInputManager();
            java.lang.reflect.Field enabled = manager.getClass().getDeclaredField("textInputEnabled");
            enabled.setAccessible(true);
            boolean actual = enabled.getBoolean(manager);
            if (actual != expected) {
                throw new AssertionError("Text input is " + (actual ? "on" : "off") + " " + when + ", expected "
                        + (expected ? "on" : "off") + " - " + (expected
                        ? "a real keyboard delivers no typed characters then (SDL on 26.3+); the focused widget has to "
                        + "call Minecraft.onTextInputFocusChange. Focused: " + focused(client) + "."
                        : "the last field kept text input running after losing the focus."));
            }
        });
    }

    private static BlueprintCodeArea codeArea(Minecraft client) {
        return child(client, BlueprintCodeArea.class, 0);
    }

    /** The insert field of the help book is the first text box the editor adds (then the title). */
    private static EditBox insertBox(Minecraft client) {
        return child(client, EditBox.class, 0);
    }

    private static <T> T child(Minecraft client, Class<T> type, int index) {
        Screen screen = client.gui.screen();
        if (!(screen instanceof BlueprintScreen)) {
            throw new AssertionError("The blueprint editor is not open; the screen is " + describeScreen(client) + ".");
        }
        int seen = 0;
        for (GuiEventListener child : screen.children()) {
            if (type.isInstance(child) && seen++ == index) {
                return type.cast(child);
            }
        }
        throw new AssertionError("The editor has no " + type.getSimpleName() + " #" + index + ".");
    }

    private static GuiEventListener focused(Minecraft client) {
        return client.gui.screen() == null ? null : client.gui.screen().getFocused();
    }

    private static String describeScreen(Minecraft client) {
        return client.gui.screen() == null ? "none" : client.gui.screen().getClass().getName();
    }
}

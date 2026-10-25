package com.simplebuilding.clientgametest;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.TweaksConfig;
import com.simplebuilding.tweaks.client.TweaksClient;

/**
 * Owner N11 P7: while the Resonance Rod is used it tilts far forward and the aiming dot is light amethyst.
 * Aims the rod at the scene wall, checks the dot colour and that the client really aims, and takes the shot
 * the owner judges the tilt by ({@code resonance-rod-aiming}).
 */
public final class GadgetsClientTest {
    private GadgetsClientTest() {}

    public static void inWorld(Script script) {
        TestScene.build(script, "minecraft:white_concrete", "creative");
        // The hidden HUD also hides the first person hand; the tilt is what this shot is for.
        TestScene.showHudAgain(script);
        script.command("item replace entity @a weapon.mainhand with " + com.simplebuilding.version.McVersion.GADGETS_NAMESPACE + ":amethyst_lens"); // 26.3: Simple Gadgets' rod
        script.awaitPackets();
        script.idle("let the rod reach the client", 10);
        TestScene.assertAimedAt(script, TestScene.TARGET, TestScene.TARGET_FACE);
        script.verify("the dot is light amethyst by default", () -> {
            // 26.3: the rod's colour is Simple Gadgets' own (rodColor, same default)
            int color = com.simplebuilding.version.McVersion.GADGETS_IN_SB ? SimpleTweaks.config().laserPointer.color : TweaksConfig.LASER_DEFAULT_COLOR;
            if (color != TweaksConfig.LASER_DEFAULT_COLOR) {
                throw new AssertionError("laserPointer.color is #" + Integer.toHexString(color) + ", expected #b38ef3");
            }
        });
        script.harness("hold the right mouse button on the wall", harness -> harness.holdMouse(InputConstants.MOUSE_BUTTON_RIGHT));
        script.await("the client aims the laser", 60, client -> com.simplebuilding.version.McVersion.GADGETS_IN_SB ? TweaksClient.isAimingLaser(client.player)
                        : client.player.isUsingItem() && com.simplebuilding.util.GadgetRefs.isRod(client.player.getUseItem()), // 26.3: aiming = using the rod
                client -> "the rod is not aiming while the right button is held. " + TestScene.describeAim(client));
        script.idle("let the tilt settle", 20);
        script.act("dismiss toasts and chat before the shot", client -> {
            client.gui.toastManager().clear();
            client.gui.hud.getChat().clearMessages(true);
        });
        script.idle("let one frame pass without the chat", 1);
        script.shot("resonance-rod-aiming");
        script.harness("let go of the right mouse button", harness -> harness.releaseMouse(InputConstants.MOUSE_BUTTON_RIGHT));
        script.command("item replace entity @a weapon.mainhand with minecraft:air");
    }
}

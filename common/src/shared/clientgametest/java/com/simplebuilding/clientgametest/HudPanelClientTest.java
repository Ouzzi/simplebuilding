package com.simplebuilding.clientgametest;

import com.simplebuilding.client.gui.HudPanel;

/**
 * Owner N67: the HUD panels' triple stair frame. An octant with both positions in the main hand and the velocity gauge
 * in the off hand put two panels on the screen, in front of white concrete (the brightest world, where the panel text
 * has the least contrast; N68: every text colour, the old style incl. all sixteen octant dye themes, must keep 4,5:1 there and on black). Takes the pictures the
 * owner judges the frame by: {@code hud-panels-octant-gauge}.
 */
public final class HudPanelClientTest {
    private HudPanelClientTest() {}

    public static void inWorld(Script script) {
        TestScene.build(script, "minecraft:white_concrete", "creative");
        TestScene.showHudAgain(script);
        script.act("hud text contrast >= 4.5 over white and black worlds (ui_text_contrast for the HUD)", client -> {
            java.util.List<Integer> inks = new java.util.ArrayList<>(java.util.List.of(HudPanel.COLOR_VALUE, HudPanel.COLOR_SECONDARY,
                    HudPanel.COLOR_DANGER, HudPanel.COLOR_IMPACT, 0xFFFFFFFF));
            for (net.minecraft.world.item.DyeColor dye : net.minecraft.world.item.DyeColor.values()) {
                var theme = com.simplebuilding.util.guiDrawHelper.getColorTheme(dye);
                inks.add(HudPanel.ink(theme.pos1()));
                inks.add(HudPanel.ink(theme.pos2()));
            }
            for (int ink : inks) {
                for (int world : new int[] {0xFFFFFF, 0x000000}) {
                    double ratio = HudPanel.contrastOver(ink, world);
                    if (ratio < HudPanel.MIN_CONTRAST) {
                        throw new AssertionError("HUD ink " + Integer.toHexString(ink) + " has contrast " + ratio + " over world " + Integer.toHexString(world));
                    }
                }
            }
        });
        script.command("item replace entity @a weapon.mainhand with simplebuilding:octant"
                + "[minecraft:custom_data={Pos1:[I;0,64,0],Pos2:[I;11,70,7]}]");
        script.command("item replace entity @a weapon.offhand with simplebuilding:velocity_gauge");
        script.awaitPackets();
        script.idle("let both gadgets arrive and the item name popup finish", 60);
        script.act("dismiss toasts and chat before the shot", client -> {
            client.gui.toastManager().clear();
            client.gui.hud.getChat().clearMessages(true);
        });
        script.idle("let one frame pass without the chat", 1);
        script.shot("hud-panels-octant-gauge");
        script.command("item replace entity @a weapon.mainhand with minecraft:air");
        script.command("item replace entity @a weapon.offhand with minecraft:air");
    }
}

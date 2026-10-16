package com.simplebuilding.clientgametest;

/**
 * Owner N67: the HUD panels' triple stair frame. An octant with both positions in the main hand and the velocity gauge
 * in the off hand put two panels on the screen, in front of white concrete (the brightest world, where the panel text
 * has the least contrast: {@code HudPanel} documents 7.1 / 5.2 / 4.7 : 1 for its tones there). Takes the pictures the
 * owner judges the frame by: {@code hud-panels-octant-gauge}.
 */
public final class HudPanelClientTest {
    private HudPanelClientTest() {}

    public static void inWorld(Script script) {
        TestScene.build(script, "minecraft:white_concrete", "creative");
        TestScene.showHudAgain(script);
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

package com.simplebuilding.clientgametest;

/**
 * Nachtrag 65, small things (26.3): floating glass panes seen from below (Vanilla glass, a stained pane, an iron-bars
 * look-alike and the mod's wooden sheet) and the Simple Mobheads shulker head, standing and worn. Pictures only; clears
 * what it places.
 */
public final class N65SmallClientTest {
    private N65SmallClientTest() {
    }

    public static void inWorld(Script script) {
        TestScene.build(script, "minecraft:smooth_stone", "creative");
        // Floating panes, two blocks apart, four blocks above the floor: single posts and a row of three.
        script.command("setblock 6 4 18 minecraft:glass_pane");
        script.command("setblock 8 4 18 minecraft:red_stained_glass_pane");
        script.command("setblock 10 4 18 simplebuilding:oak_sheet");
        script.command("setblock 12 4 18 minecraft:glass_pane");
        script.command("setblock 13 4 18 minecraft:glass_pane");
        script.command("setblock 14 4 18 minecraft:glass_pane");
        // The mod's light panes (flat glass-like lamps) floating without a block behind them, seen from below.
        script.command("setblock 6 6 18 simplelights:transparent_light_pane[facing=up]");
        script.command("setblock 8 6 18 simplelights:light_pane[facing=up]");
        script.command("setblock 10 6 18 simplelights:red_light_pane[facing=up]");
        script.command("setblock 12 6 18 simplelights:blue_light_pane[facing=north]");
        script.awaitPackets();
        script.command("tp @a 10.5 0.0 12.5 0.0 -30.0");
        script.awaitPackets();
        script.act("look up at the panes", client -> client.player.setXRot(-30.0F));
        script.idle("let the panes render", 20);
        script.shot("n65-panes-from-below");
        script.command("tp @a 9.5 0.0 15.5 0.0 -60.0");
        script.awaitPackets();
        script.act("look steeply up", client -> client.player.setXRot(-60.0F));
        script.idle("let the close-up settle", 20);
        script.shot("n65-panes-from-below-steep");
        script.command("fill 5 4 18 15 4 18 minecraft:air", true);
        script.command("fill 5 6 18 15 6 18 minecraft:air", true);

        // The shulker head: placed on the floor, and worn (third person, from the front).
        script.command("setblock 9 0 18 simplemobheads:shulker_head[rotation=8]", true);
        script.command("setblock 10 0 18 simplemobheads:pig_head[rotation=0]", true);
        script.command("setblock 11 0 18 simplemobheads:shulker_head[rotation=12]", true);
        script.command("tp @a 10.0 0.0 16.5 0.0 25.0");
        script.awaitPackets();
        script.idle("let the placed heads render", 20);
        script.shot("n65-heads-placed");
        script.command("item replace entity @a armor.head with simplemobheads:shulker_head", true);
        script.command("tp @a 10.5 0.0 15.0 0.0 10.0");
        script.awaitPackets();
        script.act("third person from the front", client -> client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));
        script.idle("let the heads render", 20);
        script.shot("n65-shulker-head");
        script.act("back to first person", client -> client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
        script.command("item replace entity @a armor.head with minecraft:air", true);
        script.command("fill 8 0 18 12 0 18 minecraft:air", true);
    }
}

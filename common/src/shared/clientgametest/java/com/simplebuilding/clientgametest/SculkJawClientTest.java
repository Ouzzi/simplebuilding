package com.simplebuilding.clientgametest;

import com.simplebuilding.version.McVersion;

/**
 * Owner N37 (26.3): the Sculk Jaw hides as sculk. One picture of the shut jaw between real sculk blocks on a sculk floor
 * ({@code jaw-closed}), then the bite as a sequence of its block states: open, the two motion frames, shut
 * ({@code jaw-bite-0} .. {@code jaw-bite-3}). The states are set directly, so nothing ticks between the pictures.
 */
public final class SculkJawClientTest {
    private static final String[] BITE = {"triggered=false,motion=0", "triggered=true,motion=2", "triggered=true,motion=1",
            "triggered=true,motion=0"};

    private SculkJawClientTest() {
    }

    private static void frame(Script script, int index, String shot) {
        script.command("setblock 10 0 18 simplebuilding:sculk_jaw[" + BITE[index] + "]");
        script.awaitPackets();
        script.idle("let bite frame " + index + " render", 10);
        script.shot(shot);
    }

    public static void inWorld(Script script) {
        if (!McVersion.NATURE_VARIANTS) {
            return;
        }
        TestScene.build(script, "minecraft:stone", "creative");
        script.command("gamemode spectator @a");
        script.command("fill 6 -1 16 14 -1 19 minecraft:sculk");
        script.command("setblock 8 0 18 minecraft:sculk");
        script.command("setblock 12 0 18 minecraft:sculk");
        script.command("setblock 10 0 18 simplebuilding:sculk_jaw[triggered=true,motion=0]");
        script.command("tp @a 10.5 1.6 15.6 0.0 38.0");
        script.awaitPackets();
        script.awaitChunks();
        script.idle("let the jaw and the sculk render", 40);
        script.shot("jaw-closed");
        // Close up: the edges of the stepped lips (no light seams, N37) and the bite frames.
        script.command("tp @a 10.5 1.0 16.9 0.0 40.0");
        script.awaitPackets();
        script.idle("let the close view settle", 20);
        script.shot("jaw-closed-near");
        frame(script, 0, "jaw-bite-0");
        frame(script, 1, "jaw-bite-1");
        frame(script, 2, "jaw-bite-2");
        frame(script, 3, "jaw-bite-3");
        script.command("fill 6 -1 16 14 0 19 minecraft:air", true);
        script.command("fill 6 -1 16 14 -1 19 minecraft:stone", true);
        script.command("gamemode creative @a");
        script.awaitPackets();
    }
}

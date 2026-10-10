package com.simplebuilding.clientgametest;

import com.simplebuilding.version.McVersion;

/** End structures (Queue N23): one picture per structure variant, placed with /place template. */
public final class EndStructureClientTest {
    private static final String[] SHOTS = {"well_intact", "well_broken_a", "well_broken_b", "gateway_a", "gateway_c",
            "wreck_bow_none", "wreck_stern_elytra", "wreck_broken_empty", "wreck_tilt_none", "path_cross", "path_wave"};

    private EndStructureClientTest() {
    }

    public static void inWorld(Script script) {
        if (!McVersion.END_STRUCTURES) {
            return;
        }
        TestScene.build(script, "minecraft:stone", "creative");
        for (String name : SHOTS) {
            script.command("place template simplebuilding:end/" + name + " 2 0 13");
            script.command("tp @a 7.5 6.0 10.0 0.0 38.0");
            script.awaitPackets();
            script.awaitChunks();
            script.idle("let " + name + " render", 40);
            script.shot("end-" + name);
            script.command("fill -2 0 10 18 14 19 air");
            script.command("kill @e[type=minecraft:item_frame]", true);
            script.awaitPackets();
        }
    }
}

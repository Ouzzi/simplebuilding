package com.simplebuilding.util;

import com.simplebuilding.Simplebuilding;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;

/**
 * Own sounds of the redstone lines Astral, Nihil and Bridge (owner 22.10., Queue SP22-5,
 * docs/ai/PLAN-B-REDSOUND22-2026-10-22.md): until then dust, lamps and pistons all sounded like amethyst.
 * Dust is powdery, lamps are struck crystal glass, pistons a stone knock with a mechanical clack; Astral is brighter,
 * Nihil duller. Files and events: {@code tools/sounds/make_redstone_line_sounds.py}, 26.3 overlay {@code sounds.json}.
 *
 * <p>The events here are plain objects; {@link ModSounds} registers them (only with {@code McVersion.END_SYSTEMS}),
 * so {@code ModBlocks} can take the sound types without touching the sound registry.
 */
public final class LineSounds {

    /** Every event below, in registration order. */
    public static final List<SoundEvent> EVENTS = new ArrayList<>();

    public static final SoundType ASTRAL_DUST = type("astral_redstone");
    public static final SoundType NIHIL_DUST = type("nihil_redstone");
    public static final SoundType BRIDGE_DUST = type("bridge_redstone");
    public static final SoundType ASTRAL_LAMP = type("astralit_lamp");
    public static final SoundType NIHIL_LAMP = type("nihilith_lamp");
    public static final SoundType ASTRAL_PISTON = type("astral_piston");
    public static final SoundType NIHIL_PISTON = type("nihil_piston");

    public static final SoundEvent ASTRAL_LAMP_ON = event("block.astralit_lamp.turn_on");
    public static final SoundEvent ASTRAL_LAMP_OFF = event("block.astralit_lamp.turn_off");
    public static final SoundEvent NIHIL_LAMP_ON = event("block.nihilith_lamp.turn_on");
    public static final SoundEvent NIHIL_LAMP_OFF = event("block.nihilith_lamp.turn_off");
    /** The Astral piston pushes, the Nihil piston pulls (Vanilla piston event with an own pitch, sounds.json). */
    public static final SoundEvent ASTRAL_PISTON_MOVE = event("block.astral_piston.move");
    public static final SoundEvent NIHIL_PISTON_MOVE = event("block.nihil_piston.move");

    private LineSounds() {
    }

    /** The lamp's toggle sound: on or off, Astral or Nihil. */
    public static SoundEvent lampToggle(boolean astral, boolean on) {
        return astral ? (on ? ASTRAL_LAMP_ON : ASTRAL_LAMP_OFF) : (on ? NIHIL_LAMP_ON : NIHIL_LAMP_OFF);
    }

    private static SoundType type(String block) {
        String base = "block." + block + ".";
        return new SoundType(1.0F, 1.0F, event(base + "break"), event(base + "step"), event(base + "place"),
                event(base + "hit"), event(base + "fall"));
    }

    private static SoundEvent event(String path) {
        SoundEvent event = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, path));
        EVENTS.add(event);
        return event;
    }
}

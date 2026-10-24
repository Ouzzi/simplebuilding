package com.simplelib.registry;

import com.simplelib.SimpleLib;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

/** SimpleLib's own sounds (legendary: ElevenLabs choir takes, W24-3; docs/ai/PLAN-A-SOUND24-2026-10-23.md). */
public final class LibSounds {
    /** The fanfare of a legendary moment ({@link com.simplelib.api.LegendaryMoments}). */
    public static Holder<SoundEvent> LEGENDARY;

    public static void register() {
        if (LEGENDARY == null) {
            var id = SimpleLib.id("legendary");
            LEGENDARY = Registry.registerForHolder(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
        }
    }

    private LibSounds() {}
}

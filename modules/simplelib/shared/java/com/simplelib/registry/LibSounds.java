package com.simplelib.registry;

import com.simplelib.SimpleLib;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

/** SimpleLib's own sounds (synthesised, tools/sounds/make_legendary_sound.py). */
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

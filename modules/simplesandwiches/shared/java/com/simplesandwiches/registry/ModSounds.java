package com.simplesandwiches.registry;

import com.simplesandwiches.Sandwiches;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

/**
 * Own knife sounds for cutting butter and cheese (assets/simplesandwiches/sounds.json, OGGs made by
 * tools/sounds/make_sandwich_sounds.py). Each loader calls {@link #register()} during its sound
 * event registration.
 */
public final class ModSounds {
    public static SoundEvent BUTTER_CUT, CHEESE_CUT;

    public static void register() {
        BUTTER_CUT = register("block.butter_block.cut");
        CHEESE_CUT = register("block.cheese_block.cut");
    }

    private static SoundEvent register(String name) {
        var id = Sandwiches.id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    private ModSounds() {}
}

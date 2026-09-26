package com.simplebuilding.util;

import com.simplebuilding.Simplebuilding;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * Die eigenen Klaenge der Mod. Die Dateien und Untertitel stehen in
 * {@code assets/simplebuilding/sounds.json}, die Untertitel-Texte in den Sprachdateien.
 *
 * <p>Die Klasse registriert beim Laden. Fabric laedt sie ueber {@link #registerSounds()} beim
 * Start, NeoForge und Forge im {@code RegisterEvent} der Klang-Registry; vorher darf sie niemand
 * anfassen.
 */
public final class ModSounds {

    /** Der Kolben der Mod bohrt sich durch einen Block ({@link PistonBoreEffects}). */
    public static final Identifier PISTON_BORE_ID = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "block.piston.bore");
    public static final SoundEvent PISTON_BORE = register(PISTON_BORE_ID);

    private ModSounds() {
    }

    private static SoundEvent register(Identifier id) {
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    /** Laedt die Klasse und damit ihre Registrierungen. */
    public static void registerSounds() {
        Simplebuilding.LOGGER.info("Registering Mod Sounds for " + Simplebuilding.MOD_ID);
    }
}

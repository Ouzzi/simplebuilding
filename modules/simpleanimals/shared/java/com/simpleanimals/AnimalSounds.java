package com.simpleanimals;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;

/**
 * The deer's and the hedgehog's own voices (PLAN40 simpleanimals-2; assets/simpleanimals/sounds.json, two takes each, ElevenLabs
 * sound generation, see CREDITS.md). Same pattern as Simple Dinos' sounds: plain constants for the entities, {@link #registerAll} for
 * Fabric and {@link #forEach} for the RegisterEvent loaders. Footsteps of both animals stay Vanilla sounds.
 */
public final class AnimalSounds {
    private static final Map<String, SoundEvent> ALL = new LinkedHashMap<>();

    public static final SoundEvent DEER_AMBIENT = make("entity.deer.ambient");
    public static final SoundEvent DEER_HURT = make("entity.deer.hurt");
    public static final SoundEvent DEER_DEATH = make("entity.deer.death");
    public static final SoundEvent HEDGEHOG_AMBIENT = make("entity.hedgehog.ambient");
    public static final SoundEvent HEDGEHOG_HURT = make("entity.hedgehog.hurt");
    public static final SoundEvent HEDGEHOG_DEATH = make("entity.hedgehog.death");
    public static final SoundEvent HEDGEHOG_CURL = make("entity.hedgehog.curl");
    public static final SoundEvent HEDGEHOG_UNCURL = make("entity.hedgehog.uncurl");

    private static SoundEvent make(String name) {
        SoundEvent ev = SoundEvent.createVariableRangeEvent(SimpleAnimals.id(name));
        ALL.put(name, ev);
        return ev;
    }

    /** Key and event of every sound, for RegisterEvent based loaders. */
    public static void forEach(BiConsumer<ResourceKey<SoundEvent>, SoundEvent> out) {
        ALL.forEach((name, ev) -> out.accept(ResourceKey.create(Registries.SOUND_EVENT, SimpleAnimals.id(name)), ev));
    }

    /** Fabric: direct registration. */
    public static void registerAll() {
        ALL.forEach((name, ev) -> Registry.register(BuiltInRegistries.SOUND_EVENT, SimpleAnimals.id(name), ev));
    }

    /** The event names, for the data checks. */
    public static java.util.Set<String> names() {
        return java.util.Collections.unmodifiableSet(ALL.keySet());
    }

    private AnimalSounds() {}
}

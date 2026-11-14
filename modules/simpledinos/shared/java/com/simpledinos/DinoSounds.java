package com.simpledinos;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;

/**
 * The dinosaurs' own voices (PLAN40 simpledinos-3; assets/simpledinos/sounds.json, two takes each, ElevenLabs sound generation, see
 * sounds/CREDITS.md). Same pattern as Simple Mobs' sounds: plain constants for the entities, {@link #registerAll} for Fabric and
 * {@link #forEach} for the RegisterEvent loaders. The Utahraptor gets its own set with its own order.
 */
public final class DinoSounds {
    private static final Map<String, SoundEvent> ALL = new LinkedHashMap<>();

    public static final SoundEvent TREX_AMBIENT = make("entity.trex.ambient");
    public static final SoundEvent TREX_HURT = make("entity.trex.hurt");
    public static final SoundEvent TREX_DEATH = make("entity.trex.death");
    public static final SoundEvent TREX_ROAR = make("entity.trex.roar");
    public static final SoundEvent TRICERATOPS_AMBIENT = make("entity.triceratops.ambient");
    public static final SoundEvent TRICERATOPS_HURT = make("entity.triceratops.hurt");
    public static final SoundEvent TRICERATOPS_DEATH = make("entity.triceratops.death");
    public static final SoundEvent TRICERATOPS_ANGRY = make("entity.triceratops.angry");
    public static final SoundEvent STEGOSAURUS_AMBIENT = make("entity.stegosaurus.ambient");
    public static final SoundEvent STEGOSAURUS_HURT = make("entity.stegosaurus.hurt");
    public static final SoundEvent STEGOSAURUS_DEATH = make("entity.stegosaurus.death");
    public static final SoundEvent PARASAUROLOPHUS_AMBIENT = make("entity.parasaurolophus.ambient");
    public static final SoundEvent PARASAUROLOPHUS_HURT = make("entity.parasaurolophus.hurt");
    public static final SoundEvent PARASAUROLOPHUS_DEATH = make("entity.parasaurolophus.death");

    private static SoundEvent make(String name) {
        SoundEvent ev = SoundEvent.createVariableRangeEvent(SimpleDinos.id(name));
        ALL.put(name, ev);
        return ev;
    }

    /** Key and event of every sound, for RegisterEvent based loaders. */
    public static void forEach(BiConsumer<ResourceKey<SoundEvent>, SoundEvent> out) {
        ALL.forEach((name, ev) -> out.accept(ResourceKey.create(Registries.SOUND_EVENT, SimpleDinos.id(name)), ev));
    }

    /** Fabric: direct registration. */
    public static void registerAll() {
        ALL.forEach((name, ev) -> Registry.register(BuiltInRegistries.SOUND_EVENT, SimpleDinos.id(name), ev));
    }

    /** The event names, for the data checks. */
    public static java.util.Set<String> names() {
        return java.util.Collections.unmodifiableSet(ALL.keySet());
    }

    private DinoSounds() {}
}

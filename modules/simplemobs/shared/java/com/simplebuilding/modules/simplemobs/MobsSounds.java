package com.simplebuilding.modules.simplemobs;

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
 * The Hollow Howler's own voice (assets/simplemobs/sounds.json; OGGs synthesised by tools/sounds/make_howler_sounds.py).
 * The events are plain constants so the entity can use them directly; each loader registers them in its sound event
 * registration ({@link #forEach} for the RegisterEvent loaders, {@link #registerAll} for Fabric).
 */
public final class MobsSounds {
    private static final Map<String, SoundEvent> ALL = new LinkedHashMap<>();

    public static final SoundEvent HOWLER_HEARTBEAT = make("entity.hollow_howler.heartbeat");
    public static final SoundEvent HOWLER_AMBIENT = make("entity.hollow_howler.ambient");
    public static final SoundEvent HOWLER_AMBIENT_HUNGER = make("entity.hollow_howler.ambient_hunger");
    public static final SoundEvent HOWLER_AMBIENT_RAGE = make("entity.hollow_howler.ambient_rage");
    public static final SoundEvent HOWLER_HURT = make("entity.hollow_howler.hurt");
    public static final SoundEvent HOWLER_DEATH = make("entity.hollow_howler.death");
    public static final SoundEvent HOWLER_STEP = make("entity.hollow_howler.step");
    public static final SoundEvent HOWLER_NOTICE = make("entity.hollow_howler.notice");
    public static final SoundEvent HOWLER_SNIFF = make("entity.hollow_howler.sniff");
    public static final SoundEvent HOWLER_ROAR = make("entity.hollow_howler.roar");
    public static final SoundEvent HOWLER_ROAR_RAGE = make("entity.hollow_howler.roar_rage");

    private static SoundEvent make(String name) {
        SoundEvent ev = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(MobsRegistry.ID, name));
        ALL.put(name, ev);
        return ev;
    }

    /** Key and event of every sound, for RegisterEvent based loaders. */
    public static void forEach(BiConsumer<ResourceKey<SoundEvent>, SoundEvent> out) {
        ALL.forEach((name, ev) -> out.accept(ResourceKey.create(Registries.SOUND_EVENT, Identifier.fromNamespaceAndPath(MobsRegistry.ID, name)), ev));
    }

    /** Fabric: direct registration. */
    public static void registerAll() {
        ALL.forEach((name, ev) -> Registry.register(BuiltInRegistries.SOUND_EVENT, Identifier.fromNamespaceAndPath(MobsRegistry.ID, name), ev));
    }

    /** Event names (without namespace), for the sounds.json consistency test. */
    public static Iterable<String> names() { return ALL.keySet(); }

    private MobsSounds() {}
}

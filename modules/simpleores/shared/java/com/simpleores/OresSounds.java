package com.simpleores;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;

/**
 * N25 (b-metals25, owner 10.11.: "je Metall eigene Sounds"): one sound type per building metal with its own events
 * {@code simpleores:block.<metal>.break|step|place|hit|fall}. The events live in sounds.json (tools/gen_data.py): Vanilla sound files
 * at the metal's pitch (no Mojang audio is shipped), tin adds its "tin cry" takes. Quiet on purpose; lead keeps the louder, low type of
 * concept ORES31 (whoever walks on lead is heard). Loaders register {@link #events} in their sound event registry event.
 */
public final class OresSounds {
    /** Metal -> {volume, pitch} of its sound type (the takes carry their own pitch in sounds.json). */
    public static final Map<String, float[]> TYPE = new LinkedHashMap<>();
    static {
        TYPE.put("silver", new float[] {0.8F, 1.0F});
        TYPE.put("lead", new float[] {1.2F, 0.9F});
        TYPE.put("zinc", new float[] {0.8F, 1.0F});
        TYPE.put("nickel", new float[] {0.8F, 1.0F});
        TYPE.put("tin", new float[] {0.8F, 1.0F});
        TYPE.put("brass", new float[] {0.8F, 1.0F});
        TYPE.put("bronze", new float[] {0.8F, 1.0F});
        TYPE.put("stainless_steel", new float[] {0.8F, 1.0F});
    }
    public static final String[] KINDS = {"break", "step", "place", "hit", "fall"};
    /** Event path -> event, in registration order. */
    public static final Map<String, SoundEvent> EVENTS = new LinkedHashMap<>();
    /** Metal -> its sound type. */
    public static final Map<String, SoundType> SOUNDS = new LinkedHashMap<>();
    static {
        TYPE.forEach((metal, vp) -> {
            SoundEvent[] e = new SoundEvent[KINDS.length];
            for (int i = 0; i < KINDS.length; i++) {
                String path = "block." + metal + "." + KINDS[i];
                e[i] = SoundEvent.createVariableRangeEvent(SimpleOres.id(path));
                EVENTS.put(path, e[i]);
            }
            SOUNDS.put(metal, new SoundType(vp[0], vp[1], e[0], e[1], e[2], e[3], e[4]));
        });
    }

    private OresSounds() {}

    public static SoundType of(String metal) {
        return SOUNDS.get(metal);
    }

    public static void events(BiConsumer<ResourceKey<SoundEvent>, SoundEvent> sink) {
        EVENTS.forEach((path, event) -> sink.accept(ResourceKey.create(Registries.SOUND_EVENT, SimpleOres.id(path)), event));
    }
}

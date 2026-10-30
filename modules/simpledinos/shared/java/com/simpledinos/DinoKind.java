package com.simpledinos;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;

/**
 * The five dinosaurs of the concept (3.4) with the egg weights of R10. Stage 1 builds the T. rex; the others have eggs and
 * textures, but no entity yet ({@link #entity} is {@code null}), so a find only rolls among the {@link #hatchable} kinds.
 */
public enum DinoKind {
    TREX("trex", 10),
    TRICERATOPS("triceratops", 25),
    STEGOSAURUS("stegosaurus", 25),
    UTAHRAPTOR("utahraptor", 20),
    PARASAUROLOPHUS("parasaurolophus", 20);

    public final String id;
    public final int weight;

    DinoKind(String id, int weight) {
        this.id = id;
        this.weight = weight;
    }

    /** The entity type that hatches from this kind's egg, or {@code null} while the dinosaur is not built yet. */
    public EntityType<?> entity() {
        return this == TREX ? DinosRegistry.TREX : null;
    }

    public boolean hatchable() {
        return entity() != null;
    }

    public static List<DinoKind> hatchableKinds() {
        List<DinoKind> list = new ArrayList<>();
        for (DinoKind k : values()) if (k.hatchable()) list.add(k);
        return list;
    }

    /** A kind by index (clamped), for stored values. */
    public static DinoKind byIndex(int index) {
        DinoKind[] all = values();
        return all[Math.max(0, Math.min(all.length - 1, index))];
    }

    /** Weighted roll among the kinds that can hatch (R10 weights, restricted to the built dinosaurs). */
    public static DinoKind roll(RandomSource random) {
        List<DinoKind> kinds = hatchableKinds();
        int total = 0;
        for (DinoKind k : kinds) total += k.weight;
        int r = random.nextInt(Math.max(1, total));
        for (DinoKind k : kinds) {
            r -= k.weight;
            if (r < 0) return k;
        }
        return TREX;
    }
}

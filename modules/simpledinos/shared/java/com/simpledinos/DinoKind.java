package com.simpledinos;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import org.jspecify.annotations.Nullable;

/**
 * The five dinosaurs of the concept (3.4) with the egg weights of R10. A kind hatches once its dinosaur is built ({@link #entity};
 * a-dinorest1: T. rex and Triceratops, a-dinorest2: Stegosaurus, a-dinorest3: Parasaurolophus), so a find only rolls among the {@link #hatchable} kinds. The find site shifts the odds a
 * little toward the kinds of the matching rock layers (concept DINO36 7, owner: recommendation accepted): badlands Utahraptor x2 and
 * Stegosaurus x1.5 (Morrison and Cedar Mountain formations), desert Triceratops and T. rex x1.5 (Hell Creek), swamp and mangrove swamp
 * Parasaurolophus x2 (river delta deposits).
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
    public @Nullable EntityType<?> entity() {
        return switch (this) {
            case TREX -> DinosRegistry.TREX;
            case TRICERATOPS -> DinosRegistry.TRICERATOPS;
            case STEGOSAURUS -> DinosRegistry.STEGOSAURUS;
            case PARASAUROLOPHUS -> DinosRegistry.PARASAUROLOPHUS;
            default -> null;
        };
    }

    public boolean hatchable() {
        return entity() != null;
    }

    /** The eggshell that falls when this kind hatches (3 pieces; the middle of its armor recipe), or {@code null} (T. rex: none). */
    public @Nullable Item eggshell() {
        return DinosRegistry.EGGSHELLS.get(this);
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

    /** Find-site factor in halves (2 = x1, 3 = x1.5, 4 = x2) for a biome; {@code null} biome = x1. */
    public int siteHalves(@Nullable Holder<Biome> biome) {
        if (biome == null) return 2;
        boolean badlands = biome.is(BiomeTags.IS_BADLANDS);
        boolean desert = biome.is(Biomes.DESERT);
        boolean swamp = biome.is(Biomes.SWAMP) || biome.is(Biomes.MANGROVE_SWAMP);
        return switch (this) {
            case UTAHRAPTOR -> badlands ? 4 : 2;
            case STEGOSAURUS -> badlands ? 3 : 2;
            case TRICERATOPS, TREX -> desert ? 3 : 2;
            case PARASAUROLOPHUS -> swamp ? 4 : 2;
        };
    }

    /** Weight of this kind at a find site (R10 weight x site factor, in halves so it stays an integer). */
    public int siteWeight(@Nullable Holder<Biome> biome) {
        return weight * siteHalves(biome);
    }

    /** Weighted roll among the kinds that can hatch (R10 weights, restricted to the built dinosaurs), without a find site. */
    public static DinoKind roll(RandomSource random) {
        return roll(random, null);
    }

    /** Weighted roll among the kinds that can hatch, with the find-site factors of {@code biome}. */
    public static DinoKind roll(RandomSource random, @Nullable Holder<Biome> biome) {
        List<DinoKind> kinds = hatchableKinds();
        int total = 0;
        for (DinoKind k : kinds) total += k.siteWeight(biome);
        int r = random.nextInt(Math.max(1, total));
        for (DinoKind k : kinds) {
            r -= k.siteWeight(biome);
            if (r < 0) return k;
        }
        return TREX;
    }
}

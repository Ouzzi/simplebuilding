package com.simpledinos;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

/**
 * Coat variants of the dinosaurs by rearing biome (owner choice ABN30-1): the biome where the egg lay when it hatched
 * (a dinosaur summoned by command: the biome where it stands). The same idea as the Vanilla cow and pig variants, kept as a
 * small catalog (no registry, the module stays standalone): a coat at home in the biome weighs {@link #HOME}, every other
 * {@link #AWAY}, so most hatchlings wear the colours of their rearing biome and a few differ. The coats are earthy tones with
 * a dark back and a light belly (counter-shading), as in scientific reconstructions: olive-brown for temperate land, sandy
 * yellow in dry country, red-brown in badlands, dark green-brown in wet forests, grey-brown in the cold. Every dinosaur
 * kind uses these five coat names and brings its own textures {@code textures/entity/<kind>/<coat>/...}; the index is
 * stored in the world and never changes its meaning.
 */
public final class DinoVariants {
    public static final int HOME = 8;
    public static final int AWAY = 1;

    /** A coat: texture name, whether it is the fallback of every biome that is no other coat's home, and its home. */
    public record Coat(String name, boolean rest, Predicate<Holder<Biome>> home) {}

    private static Predicate<Holder<Biome>> tag(TagKey<Biome> t) {
        return b -> b.is(t);
    }

    private static Predicate<Holder<Biome>> key(ResourceKey<Biome> k) {
        return b -> b.is(k);
    }

    private static final Predicate<Holder<Biome>> NONE = b -> false;

    /** Index 0 olive (standard), 1 sand (desert, savanna), 2 rust (badlands), 3 moss (jungle, swamp), 4 ash (cold, mountains). */
    public static final List<Coat> COATS = List.of(
            new Coat("olive", true, NONE),
            new Coat("sand", false, key(Biomes.DESERT).or(tag(BiomeTags.IS_SAVANNA))),
            new Coat("rust", false, tag(BiomeTags.IS_BADLANDS)),
            new Coat("moss", false, key(Biomes.SWAMP).or(key(Biomes.MANGROVE_SWAMP)).or(tag(BiomeTags.IS_JUNGLE))),
            new Coat("ash", false, tag(BiomeTags.SPAWNS_COLD_VARIANT_FARM_ANIMALS).or(tag(BiomeTags.IS_TAIGA))
                    .or(tag(BiomeTags.IS_MOUNTAIN)).or(tag(BiomeTags.IS_HILL))));

    /** True when {@code biome} is the home of the coat; the rest coat is home where no other coat is. */
    public static boolean atHome(int coat, Holder<Biome> biome) {
        Coat c = COATS.get(clamp(coat));
        if (!c.rest()) return c.home().test(biome);
        for (Coat other : COATS) if (other != c && other.home().test(biome)) return false;
        return true;
    }

    /** Weighted pick: {@link #HOME} for the coats at home, {@link #AWAY} for the others. */
    public static int pick(Holder<Biome> biome, RandomSource random) {
        int total = 0;
        for (int i = 0; i < COATS.size(); i++) total += atHome(i, biome) ? HOME : AWAY;
        int r = random.nextInt(total);
        for (int i = 0; i < COATS.size(); i++) {
            r -= atHome(i, biome) ? HOME : AWAY;
            if (r < 0) return i;
        }
        return 0;
    }

    /** A stored value limited to the catalog. */
    public static int clamp(int coat) {
        return Math.max(0, Math.min(COATS.size() - 1, coat));
    }

    public static String name(int coat) {
        return COATS.get(clamp(coat)).name();
    }

    private DinoVariants() {}
}

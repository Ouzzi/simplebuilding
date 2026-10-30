package com.simpleanimals;

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
 * Coat variants by biome (owner choice ABN30-2). The idea is the Vanilla one for cow and pig variants (a variant is picked
 * from the biome where the animal appears), kept as a small catalog so the module needs no extra registry: every variant
 * has a home predicate over the biome; a variant at home weighs {@link #HOME}, any other {@link #AWAY}, so most animals
 * wear the coat of their biome and a few differ. The standard variant (index 0 or the one flagged {@code rest}) is at home
 * wherever no other variant is. Indices are stored in the world (NBT {@code Variant}) and never change their meaning.
 * There are no sexes: Vanilla has none.
 */
public final class AnimalVariants {
    public static final int HOME = 8;
    public static final int AWAY = 1;

    /** A coat: texture name, whether it is the fallback coat of every biome that is no other coat's home, and its home. */
    public record Variant(String name, boolean rest, Predicate<Holder<Biome>> home) {}

    private static Predicate<Holder<Biome>> tag(TagKey<Biome> t) {
        return b -> b.is(t);
    }

    private static Predicate<Holder<Biome>> key(ResourceKey<Biome> k) {
        return b -> b.is(k);
    }

    private static final Predicate<Holder<Biome>> NONE = b -> false;
    /** Snow, taiga, mountains, hills: the cold coats (the Vanilla cold farm-animal biomes included). */
    public static final Predicate<Holder<Biome>> COLD = tag(BiomeTags.SPAWNS_COLD_VARIANT_FARM_ANIMALS).or(tag(BiomeTags.IS_TAIGA))
            .or(tag(BiomeTags.IS_MOUNTAIN)).or(tag(BiomeTags.IS_HILL));
    /** Desert, badlands, savanna: the dry warm coats. */
    public static final Predicate<Holder<Biome>> DRY = key(Biomes.DESERT).or(tag(BiomeTags.IS_BADLANDS)).or(tag(BiomeTags.IS_SAVANNA));
    /** Swamp, jungle, mangrove: the wet coats. */
    public static final Predicate<Holder<Biome>> WET = key(Biomes.SWAMP).or(key(Biomes.MANGROVE_SWAMP)).or(tag(BiomeTags.IS_JUNGLE));
    /** Beach, ocean and river shores. */
    public static final Predicate<Holder<Biome>> COAST = tag(BiomeTags.IS_BEACH).or(tag(BiomeTags.IS_OCEAN)).or(tag(BiomeTags.IS_RIVER));

    /** Deer: 0 red deer (cold, mountains, taiga), 1 fallow deer (all the rest). */
    public static final List<Variant> DEER = List.of(new Variant("red", false, COLD), new Variant("fallow", true, NONE));
    /** Boar: 0 dark (standard), 1 sandy (dry), 2 frost grey (cold), 3 rust (wet). */
    public static final List<Variant> BOAR = List.of(new Variant("dark", true, NONE), new Variant("sandy", false, DRY),
            new Variant("frost", false, COLD), new Variant("rust", false, WET));
    /** Hedgehog: 0 brown (European, standard), 1 desert, 2 white-breasted (cold), 3 four-toed (wet). */
    public static final List<Variant> HEDGEHOG = List.of(new Variant("brown", true, NONE), new Variant("desert", false, DRY),
            new Variant("whitebreast", false, COLD), new Variant("fourtoed", false, WET));
    /** Crab: 0 red (standard), 1 coral red (coast). */
    public static final List<Variant> CRAB = List.of(new Variant("red", true, NONE), new Variant("coral", false, COAST));

    /** True when {@code biome} is the home of {@code variant}; the rest variant is home where no other variant is. */
    public static boolean atHome(List<Variant> catalog, int variant, Holder<Biome> biome) {
        Variant v = catalog.get(clamp(catalog, variant));
        if (!v.rest()) return v.home().test(biome);
        for (Variant other : catalog) if (other != v && other.home().test(biome)) return false;
        return true;
    }

    /** Weighted pick: {@link #HOME} for the variants at home, {@link #AWAY} for the others. */
    public static int pick(List<Variant> catalog, Holder<Biome> biome, RandomSource random) {
        int total = 0;
        for (int i = 0; i < catalog.size(); i++) total += atHome(catalog, i, biome) ? HOME : AWAY;
        int r = random.nextInt(total);
        for (int i = 0; i < catalog.size(); i++) {
            r -= atHome(catalog, i, biome) ? HOME : AWAY;
            if (r < 0) return i;
        }
        return 0;
    }

    /** A stored value limited to the catalog. */
    public static int clamp(List<Variant> catalog, int variant) {
        return Math.max(0, Math.min(catalog.size() - 1, variant));
    }

    private AnimalVariants() {}
}

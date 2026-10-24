package com.simplebuilding.modules.simpleearth.terrain;

import java.util.Locale;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.MapColor;

/**
 * Biome -> Vanilla map colour (concept §4.4): the colour Vanilla's map gives the biome's typical surface block (grass GRASS,
 * leaves PLANT, sand SAND, snow SNOW, water WATER, ...). Unknown (mod) biomes take the map colour nearest to their grass
 * colour, so Terralith & co. get plausible colours without a table.
 */
public final class EarthColors {
    private static final MapColor[] FALLBACK = {MapColor.GRASS, MapColor.PLANT, MapColor.SAND, MapColor.SNOW, MapColor.STONE,
            MapColor.DIRT, MapColor.TERRACOTTA_ORANGE, MapColor.COLOR_GREEN, MapColor.COLOR_PURPLE, MapColor.COLOR_PINK, MapColor.PODZOL};

    private EarthColors() {}

    /** The map colour of a biome at a sample; {@code underwater} = the ground there is below sea level. */
    public static MapColor color(Holder<Biome> biome, boolean underwater) {
        String path = biome.unwrapKey().map(k -> k.identifier().getPath()).orElse("").toLowerCase(Locale.ROOT);
        if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER)) return path.contains("frozen") ? MapColor.ICE : MapColor.WATER;
        if (underwater && !biome.is(BiomeTags.IS_NETHER) && !biome.is(BiomeTags.IS_END)) {
            // the ground lies below sea level: noise generation fills it with water (aquifers up to sea level)
            return path.contains("frozen") || path.contains("snowy") ? MapColor.ICE : MapColor.WATER;
        }
        if (biome.is(BiomeTags.IS_NETHER)) return MapColor.NETHER;
        if (biome.is(BiomeTags.IS_END)) return path.contains("void") ? MapColor.NONE : MapColor.SAND;
        if (path.contains("mushroom")) return MapColor.COLOR_PURPLE;
        if (biome.is(BiomeTags.IS_BADLANDS)) return MapColor.TERRACOTTA_ORANGE;
        if (path.contains("stony_shore")) return MapColor.STONE;
        if (biome.is(BiomeTags.IS_BEACH)) return path.contains("snowy") ? MapColor.SNOW : MapColor.SAND;
        if (path.contains("desert")) return MapColor.SAND;
        if (path.contains("snowy") || path.contains("frozen") || path.contains("ice") || path.contains("grove") || path.contains("jagged")) {
            return MapColor.SNOW;
        }
        if (path.contains("stony_peaks") || path.contains("gravelly") || path.contains("dripstone")) return MapColor.STONE;
        if (path.contains("deep_dark")) return MapColor.DEEPSLATE;
        if (path.contains("cherry")) return MapColor.COLOR_PINK;
        if (path.contains("pale_garden")) return MapColor.COLOR_LIGHT_GRAY;
        if (path.contains("dark_forest") || path.contains("swamp") || path.contains("mangrove")) return MapColor.COLOR_GREEN;
        if (biome.is(BiomeTags.IS_FOREST) || biome.is(BiomeTags.IS_TAIGA) || biome.is(BiomeTags.IS_JUNGLE)
                || path.contains("forest") || path.contains("taiga") || path.contains("jungle") || path.contains("lush")) {
            return MapColor.PLANT;
        }
        if (biome.is(BiomeTags.IS_OVERWORLD) && isVanilla(biome)) return MapColor.GRASS;
        return nearest(biome.value().getGrassColor(0, 0));
    }

    private static boolean isVanilla(Holder<Biome> biome) {
        return biome.unwrapKey().map(k -> k.identifier().getNamespace().equals("minecraft")).orElse(false);
    }

    /** The fallback map colour closest (RGB) to {@code rgb}. */
    static MapColor nearest(int rgb) {
        MapColor best = MapColor.GRASS;
        long bestD = Long.MAX_VALUE;
        for (MapColor c : FALLBACK) {
            int r = ((c.col >> 16) & 0xFF) - ((rgb >> 16) & 0xFF), g = ((c.col >> 8) & 0xFF) - ((rgb >> 8) & 0xFF), b = (c.col & 0xFF) - (rgb & 0xFF);
            long d = (long) r * r + (long) g * g + (long) b * b;
            if (d < bestD) {
                bestD = d;
                best = c;
            }
        }
        return best;
    }
}

package com.simplebuilding.modules.simpleearth.terrain;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simplebuilding.modules.simpleearth.SimpleEarth;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.MapColor;

/**
 * Biome -> Vanilla map colour (concept §4.4): the colour Vanilla's map gives the biome's typical surface block (grass GRASS,
 * leaves PLANT, sand SAND, snow SNOW, water WATER, ...).
 *
 * <p><b>Biome colour table</b> (N7/N8, owner 25.10.): datapack files {@code data/<namespace>/simpleearth/biome_colors.json}
 * of every namespace and pack, read at server start and on {@code /reload} (Simple Earth's own file first, the others in
 * namespace order, later entries win):
 * <pre>{"biomes": {"simpleend:ash_valley": "color_light_gray"},
 *  "tags": [{"tag": "c:is_desert", "color": "sand"}]}</pre>
 * Colours are the {@link MapColor} names in lower case ({@link #NAMES}) or their numeric ids. Order of the lookup: an exact
 * {@code biomes} entry; ocean/river tags = water; Nether/End; for mod biomes the {@code tags} rules (first match; later files'
 * rules are checked first); the built-in Vanilla rules; finally the map colour nearest the biome's grass colour, so
 * Terralith &amp; co. get plausible colours even without an entry. Ground below sea level in a land biome is water.
 * Results are cached per biome (the samplers ask millions of times); a new table clears the cache.
 *
 * <p><b>Surface detail</b> ({@link #surface}, server option {@code surfaceDetail}, WD25-16 "Block-Nutzung"): what the world
 * generator would put on top, without a chunk - snow where Vanilla's height-adjusted biome temperature lets it snow, frozen
 * water in cold biomes, stone (or packed ice) on steep slopes, red sand and terracotta bands in the badlands.
 */
public final class EarthColors {
    /** {@link MapColor} names by id (0..61), lower case. */
    public static final String[] NAMES = {"none", "grass", "sand", "wool", "fire", "ice", "metal", "plant", "snow", "clay", "dirt",
            "stone", "water", "wood", "quartz", "color_orange", "color_magenta", "color_light_blue", "color_yellow", "color_light_green",
            "color_pink", "color_gray", "color_light_gray", "color_cyan", "color_purple", "color_blue", "color_brown", "color_green",
            "color_red", "color_black", "gold", "diamond", "lapis", "emerald", "podzol", "nether", "terracotta_white", "terracotta_orange",
            "terracotta_magenta", "terracotta_light_blue", "terracotta_yellow", "terracotta_light_green", "terracotta_pink",
            "terracotta_gray", "terracotta_light_gray", "terracotta_cyan", "terracotta_purple", "terracotta_blue", "terracotta_brown",
            "terracotta_green", "terracotta_red", "terracotta_black", "crimson_nylium", "crimson_stem", "crimson_hyphae",
            "warped_nylium", "warped_stem", "warped_hyphae", "warped_wart_block", "deepslate", "raw_iron", "glow_lichen"};
    /** Datapack path of the colour table (under {@code data/<namespace>/}). */
    public static final String TABLE_PATH = "simpleearth/biome_colors.json";
    /** Slope (height difference per block of sample spacing) from which ground shows as stone; mountains and badlands earlier. */
    public static final double STEEP = 1.2, STEEP_MOUNTAIN = 0.6, STEEP_BADLANDS = 0.35;
    /** Badlands ground up to this far above sea level is red sand. */
    public static final int BADLANDS_SAND = 12;
    private static final MapColor[] BANDS = {MapColor.TERRACOTTA_ORANGE, MapColor.COLOR_ORANGE, MapColor.TERRACOTTA_YELLOW,
            MapColor.COLOR_ORANGE, MapColor.TERRACOTTA_BROWN, MapColor.TERRACOTTA_WHITE, MapColor.COLOR_ORANGE, MapColor.TERRACOTTA_RED,
            MapColor.TERRACOTTA_ORANGE, MapColor.TERRACOTTA_LIGHT_GRAY, MapColor.COLOR_ORANGE, MapColor.TERRACOTTA_RED};
    private static final MapColor[] FALLBACK = {MapColor.GRASS, MapColor.PLANT, MapColor.SAND, MapColor.SNOW, MapColor.STONE,
            MapColor.DIRT, MapColor.TERRACOTTA_ORANGE, MapColor.COLOR_GREEN, MapColor.COLOR_PURPLE, MapColor.COLOR_PINK, MapColor.PODZOL};

    /** One {@code tags} rule. */
    public record Rule(TagKey<Biome> tag, MapColor color) {}

    /** The merged colour table; {@code signature} changes with its content (disk cache key). */
    public record Table(Map<String, MapColor> biomes, List<Rule> tags, String signature) {
        public static final Table EMPTY = new Table(Map.of(), List.of(), "0");

        public MapColor exact(Holder<Biome> biome) {
            return biomes.isEmpty() ? null : biome.unwrapKey().map(k -> biomes.get(k.identifier().toString())).orElse(null);
        }

        /** The first tag rule the biome matches, or null. */
        public MapColor tag(Holder<Biome> biome) {
            for (Rule r : tags) if (biome.is(r.tag())) return r.color();
            return null;
        }
    }

    /** What a biome looks like: dry and below-sea-level colour plus the flags the surface rules need. */
    record Info(MapColor dry, MapColor wet, boolean skip, boolean mountain, boolean badlands, boolean wooded, boolean frozen) {}

    private static volatile Table table = Table.EMPTY;
    private static final Map<String, Info> CACHE = new ConcurrentHashMap<>();

    private EarthColors() {}

    public static Table table() {
        return table;
    }

    /** Uses a table (load, tests); clears the per-biome cache. */
    public static void use(Table t) {
        table = t == null ? Table.EMPTY : t;
        CACHE.clear();
    }

    /** Reads every {@code data/<ns>/simpleearth/biome_colors.json} (server start, /reload). */
    public static void load(ResourceManager resources) {
        List<JsonObject> files = new ArrayList<>();
        try {
            Map<Identifier, List<Resource>> stacks = resources.listResourceStacks("simpleearth", id -> id.getPath().equals(TABLE_PATH));
            List<Identifier> ids = new ArrayList<>(stacks.keySet());
            ids.sort(Comparator.comparing((Identifier id) -> !id.getNamespace().equals(SimpleEarth.MOD_ID)).thenComparing(Identifier::getNamespace));
            for (Identifier id : ids) {
                for (Resource r : stacks.get(id)) {
                    try (Reader in = r.openAsReader()) {
                        JsonElement e = JsonParser.parseReader(in);
                        if (e.isJsonObject()) files.add(e.getAsJsonObject());
                    } catch (Exception ex) {
                        SimpleEarth.LOG.warn("Broken biome colour table {} in {}: {}", id, r.sourcePackId(), ex.toString());
                    }
                }
            }
        } catch (RuntimeException ex) {
            SimpleEarth.LOG.warn("Could not read the biome colour tables: {}", ex.toString());
        }
        use(parse(files));
        SimpleEarth.LOG.info("Simple Earth biome colour table: {} biomes, {} tag rules", table.biomes().size(), table.tags().size());
    }

    /** Merges table files (in load order: later biome entries win, later files' tag rules come first). Unknown colours are skipped. */
    public static Table parse(List<JsonObject> files) {
        Map<String, MapColor> biomes = new LinkedHashMap<>();
        List<Rule> tags = new ArrayList<>();
        StringBuilder sig = new StringBuilder();
        for (JsonObject f : files) {
            sig.append(f);
            if (f.has("biomes") && f.get("biomes").isJsonObject()) {
                for (Map.Entry<String, JsonElement> e : f.getAsJsonObject("biomes").entrySet()) {
                    MapColor c = byName(e.getValue());
                    Identifier id = Identifier.tryParse(e.getKey());
                    if (c != null && id != null) biomes.put(id.toString(), c);
                }
            }
            if (f.has("tags") && f.get("tags").isJsonArray()) {
                List<Rule> own = new ArrayList<>();
                for (JsonElement e : f.getAsJsonArray("tags")) {
                    if (!e.isJsonObject() || !e.getAsJsonObject().has("tag")) continue;
                    String name = e.getAsJsonObject().get("tag").getAsString();
                    Identifier id = Identifier.tryParse(name.startsWith("#") ? name.substring(1) : name);
                    MapColor c = byName(e.getAsJsonObject().get("color"));
                    if (id != null && c != null) own.add(new Rule(TagKey.create(Registries.BIOME, id), c));
                }
                tags.addAll(0, own);
            }
        }
        return new Table(Map.copyOf(biomes), List.copyOf(tags), Integer.toHexString(sig.toString().hashCode()));
    }

    /** A map colour by name ({@link #NAMES}, case-insensitive) or id; null when unknown. */
    public static MapColor byName(JsonElement e) {
        if (e == null || !e.isJsonPrimitive()) return null;
        try {
            if (e.getAsJsonPrimitive().isNumber()) {
                int id = e.getAsInt();
                return id >= 0 && id < NAMES.length ? MapColor.byId(id) : null;
            }
            String name = e.getAsString().toLowerCase(Locale.ROOT).trim();
            for (int i = 0; i < NAMES.length; i++) if (NAMES[i].equals(name)) return MapColor.byId(i);
        } catch (RuntimeException ignored) {
        }
        return null;
    }

    /** The map colour of a biome at a sample; {@code underwater} = the ground there is below sea level. */
    public static MapColor color(Holder<Biome> biome, boolean underwater) {
        Info info = info(biome);
        return underwater ? info.wet() : info.dry();
    }

    static Info info(Holder<Biome> biome) {
        String key = biome.unwrapKey().map(k -> k.identifier().toString()).orElse(null);
        if (key == null) return compute(biome);
        Info hit = CACHE.get(key);
        if (hit != null) return hit;
        Info made = compute(biome);
        CACHE.put(key, made);
        return made;
    }

    private static Info compute(Holder<Biome> biome) {
        String path = biome.unwrapKey().map(k -> k.identifier().getPath()).orElse("").toLowerCase(Locale.ROOT);
        boolean skip = biome.is(BiomeTags.IS_NETHER) || biome.is(BiomeTags.IS_END);
        MapColor dry = dry(biome, path);
        MapColor wet = dry;
        if (dry != MapColor.WATER && dry != MapColor.ICE && !skip) {
            // the ground lies below sea level: noise generation fills it with water (aquifers up to sea level)
            wet = path.contains("frozen") || path.contains("snowy") ? MapColor.ICE : MapColor.WATER;
        }
        return new Info(dry, wet, skip, biome.is(BiomeTags.IS_MOUNTAIN), biome.is(BiomeTags.IS_BADLANDS) || dry == MapColor.TERRACOTTA_ORANGE,
                path.contains("wooded"), path.contains("frozen") || path.contains("ice"));
    }

    private static MapColor dry(Holder<Biome> biome, String path) {
        MapColor exact = table.exact(biome);
        if (exact != null) return exact;
        if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER)) return path.contains("frozen") ? MapColor.ICE : MapColor.WATER;
        if (biome.is(BiomeTags.IS_NETHER)) return isVanilla(biome) ? MapColor.NETHER : orElse(table.tag(biome), MapColor.NETHER);
        if (biome.is(BiomeTags.IS_END)) {
            if (!isVanilla(biome)) return orElse(table.tag(biome), MapColor.SAND);
            return path.contains("void") ? MapColor.NONE : MapColor.SAND;
        }
        if (!isVanilla(biome)) {
            MapColor tagged = table.tag(biome);
            if (tagged != null) return tagged;
        }
        if (path.contains("mushroom")) return MapColor.COLOR_PURPLE;
        if (biome.is(BiomeTags.IS_BADLANDS)) return MapColor.TERRACOTTA_ORANGE;
        if (path.contains("stony_shore")) return MapColor.STONE;
        if (biome.is(BiomeTags.IS_BEACH)) return path.contains("snowy") ? MapColor.SNOW : MapColor.SAND;
        if (path.contains("desert")) return MapColor.SAND;
        if (path.contains("cherry")) return MapColor.COLOR_PINK;
        if (path.contains("snowy") || path.contains("frozen") || path.contains("ice") || path.contains("grove") || path.contains("jagged")) {
            return MapColor.SNOW;
        }
        if (path.contains("stony_peaks") || path.contains("gravelly") || path.contains("dripstone")) return MapColor.STONE;
        if (path.contains("deep_dark")) return MapColor.DEEPSLATE;
        if (path.contains("pale_garden")) return MapColor.COLOR_LIGHT_GRAY;
        if (path.contains("dark_forest") || path.contains("swamp") || path.contains("mangrove")) return MapColor.COLOR_GREEN;
        if (biome.is(BiomeTags.IS_FOREST) || biome.is(BiomeTags.IS_TAIGA) || biome.is(BiomeTags.IS_JUNGLE)
                || path.contains("forest") || path.contains("taiga") || path.contains("jungle") || path.contains("lush")) {
            return MapColor.PLANT;
        }
        if (biome.is(BiomeTags.IS_OVERWORLD) && isVanilla(biome)) return MapColor.GRASS;
        return nearest(biome.value().getGrassColor(0, 0));
    }

    private static MapColor orElse(MapColor c, MapColor fallback) {
        return c != null ? c : fallback;
    }

    /**
     * Surface detail of one sample: {@code base} from {@link #color}, ground height {@code y}, {@code slope} = largest height
     * difference to the neighbouring samples per block of spacing. Nether/End and unknown samples stay as they are.
     */
    public static MapColor surface(MapColor base, Holder<Biome> biome, int x, int y, int z, int sea, double slope) {
        if (base == MapColor.NONE || base == MapColor.ICE) return base;
        Info info = info(biome);
        if (info.skip()) return base;
        Biome b = biome.value();
        if (base == MapColor.WATER) return b.coldEnoughToSnow(new BlockPos(x, sea, z), sea) ? MapColor.ICE : base;
        if (info.badlands()) {
            if (slope >= STEEP_BADLANDS || y >= 256) return band(y);
            if (info.wooded() && y >= 97) return MapColor.PLANT;
            // flat badlands ground: red sand (its map colour is the plain terracotta orange)
            return base == MapColor.TERRACOTTA_ORANGE ? MapColor.COLOR_ORANGE : base;
        }
        boolean cold = b.coldEnoughToSnow(new BlockPos(x, Math.max(y, sea) + 1, z), sea);
        if (slope >= (info.mountain() ? STEEP_MOUNTAIN : STEEP)) return cold && info.frozen() ? MapColor.ICE : MapColor.STONE;
        if (cold && base != MapColor.SAND && b.hasPrecipitation()) return MapColor.SNOW;
        return base;
    }

    /** Badlands terracotta band at a height (a fixed band order; Vanilla's are seed-random but alike). */
    public static MapColor band(int y) {
        return BANDS[Math.floorMod(Math.floorDiv(y, 3), BANDS.length)];
    }

    private static boolean isVanilla(Holder<Biome> biome) {
        return biome.unwrapKey().map(k -> k.identifier().getNamespace().equals("minecraft")).orElse(false);
    }

    /** The fallback map colour closest (RGB) to {@code rgb}. */
    public static MapColor nearest(int rgb) {
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

package dev.simpledimension.common.portal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One configurable dimension + its portal, fully described by a single JSON file.
 *
 * <p>Everything a pack author asked for lives here: the travel link
 * ({@link #sourceDimensionId}/{@link #targetDimensionId}), the portal "build
 * recipe" ({@link #frameBlock} plus the accepted interior size range), the
 * portal tint ({@link #portalColorHex}, applied to a nether-style animated
 * texture) and the destination terrain ({@link #worldGeneration}).
 *
 * <p>Field names map 1:1 to the JSON keys, so this class doubles as the schema.
 */
public final class DimensionPortalConfig {

    // ---- identity & travel link -------------------------------------------------
    public boolean enabled = true;
    public boolean requireSeparateLight = false;
    public String id = "skyblock";
    public String sourceDimensionId = "minecraft:overworld";
    public String targetDimensionId = "simpledimension:skyblock";
    public String targetDisplayName = "Skyblock";

    // ---- portal appearance ------------------------------------------------------
    /** Hue applied to the nether-style portal texture, as #RRGGBB. */
    public String portalColorHex = "#66D9FF";

    // ---- portal build recipe (the "constellation") ------------------------------
    /**
     * Explicit shape recipes. When non-empty these take precedence over the flexible
     * rectangle below, letting you draw the exact portal form and pick the block for each
     * cell (e.g. obsidian frame with glowstone corners). Any recipe that matches activates.
     */
    public List<PortalRecipeConfig> portalRecipes = new ArrayList<>();

    /** Flexible rectangular fallback (used when {@link #portalRecipes} is empty). */
    /** The block the frame must be built from. Igniting this frame opens the portal. */
    public String frameBlock = "minecraft:crying_obsidian";
    /** Inclusive interior size limits (interior = empty cells inside the frame). */
    public int minPortalWidth = 2;
    public int maxPortalWidth = 21;
    public int minPortalHeight = 3;
    public int maxPortalHeight = 21;

    // ---- travel behavior -------------------------------------------------------
    /** Ticks the player must stand inside the portal before being sent across (vanilla-like delay). */
    public int portalDelayTicks = 0;
    /** Cooldown after a teleport so the player does not bounce straight back. */
    public int teleportCooldownTicks = 60;
    /**
     * Distance ratio between the two dimensions: how many <b>source</b>-dimension blocks
     * correspond to one <b>target</b>-dimension block. Like vanilla's nether ratio
     * (8.0 = 1 block in the target equals 8 blocks in the source). 1.0 = identical
     * coordinates. Only the horizontal axes are scaled; Y is preserved.
     */
    public double travelCoordinateScale = 1.0;

    // ---- which sides may open / receive a portal --------------------------------
    /** Allow lighting the frame in the source dimension (travels to the target). */
    public boolean allowIgniteFromSource = true;
    /** Allow lighting the frame in the target dimension (manually builds a portal back to the source). */
    public boolean allowIgniteFromTarget = true;
    /**
     * Extra "origin" dimensions (besides {@link #sourceDimensionId}) from which the frame
     * may be lit to open a portal to the target — e.g. open the mining portal from the
     * overworld, the nether and the end. Lighting in any origin sends you to the target;
     * the return portal sends you back to whichever origin you came from. Empty by default.
     */
    public List<String> openFromDimensions = new ArrayList<>();
    /** When arriving with no portal nearby, automatically build a matching return portal. */
    public boolean generateReturnPortalOnArrival = true;
    /** Build a small frame-block platform under a freshly generated destination portal (avoids void death). */
    public boolean createDestinationPlatform = true;

    // ---- destination terrain ----------------------------------------------------
    public WorldGenerationConfig worldGeneration = WorldGenerationConfig.skyblockPreset();

    /** Skyblock: 1:1 travel, empty void (no terrain, no biome). Only openable from the overworld. */
    public static DimensionPortalConfig copperSkyblock() {
        DimensionPortalConfig config = new DimensionPortalConfig();
        config.id = "skyblock";
        config.targetDimensionId = "simpledimension:skyblock";
        config.targetDisplayName = "Skyblock";
        config.portalColorHex = "#66D9FF";
        config.frameBlock = "minecraft:crying_obsidian"; // arrival-platform block
        config.travelCoordinateScale = 1.0;
        onlyFromOverworld(config);
        config.portalRecipes = singleRecipe(
                List.of(
                        "BIIB",
                        "G..G",
                        "G..G",
                        "G..G",
                        "GIIG"),
                Map.of(
                        "B", "minecraft:waxed_oxidized_copper_bulb",
                        "I", "minecraft:blue_ice",
                        "G", "minecraft:waxed_oxidized_copper_grate"));
        config.worldGeneration = WorldGenerationConfig.skyblockPreset();
        return config;
    }

    public static DimensionPortalConfig defaultSkyblock() {
        var c = copperSkyblock(); c.requireSeparateLight = true;
        c.portalRecipes = new ArrayList<>();
        for (var rows : List.of(List.of("AGA","G.G","G.G"),List.of("AGGA","G..G","G..G","G..G"),
            List.of("AGGGA","G...G","G...G","G...G"),List.of("AAGGAA","AG..GA","G....G","G....G","G....G"),
            List.of("AAGGGAA","AG...GA","G.....G","G.....G","G.....G"),List.of("AAGGGGAA","AG....GA","G......G","G......G","G......G"))) {
            var r = new PortalRecipeConfig(); r.rows=new ArrayList<>(rows); r.legend=Map.of("G","minecraft:glowstone","A","minecraft:air");
            c.portalRecipes.add(r);
        }
        return c;
    }

    /** Mining: superflat at high altitude, deep stone/deepslate column with vanilla ores + trees;
     *  travel runs at 1 overworld : 2 mining blocks (slower). Only openable from the overworld. */
    public static DimensionPortalConfig miningDimensionPreset() {
        DimensionPortalConfig config = new DimensionPortalConfig();
        config.id = "mining";
        config.targetDimensionId = "simpledimension:mining";
        config.targetDisplayName = "Mining";
        config.portalColorHex = "#FFAA55";
        config.frameBlock = "minecraft:deepslate_bricks";
        config.travelCoordinateScale = 0.5; // 1 overworld block == 2 mining blocks (slower in mining)
        onlyFromOverworld(config);
        config.portalRecipes = singleRecipe(
                List.of(
                        "DXXD",
                        "D..D",
                        "D..D",
                        "D..D",
                        "DEED"),
                Map.of(
                        "D", "minecraft:deepslate_bricks",
                        "X", "minecraft:diamond_ore",
                        "E", "minecraft:emerald_ore"));
        config.worldGeneration = WorldGenerationConfig.miningPreset();
        return config;
    }

    /** Travel: flat bedrock plane, no terrain/biome; travel runs at 10 overworld : 1 travel block
     *  (fast overworld traversal). Only openable from the overworld. */
    public static DimensionPortalConfig travelDimensionPreset() {
        DimensionPortalConfig config = new DimensionPortalConfig();
        config.id = "travel";
        config.targetDimensionId = "simpledimension:travel";
        config.targetDisplayName = "Travel";
        config.portalColorHex = "#B080FF";
        config.frameBlock = "minecraft:purpur_block";
        config.travelCoordinateScale = 10.0; // 1 travel block == 10 overworld blocks (fast travel)
        onlyFromOverworld(config);
        config.portalRecipes = singleRecipe(
                List.of(
                        "KRRK",
                        "O..O",
                        "O..O",
                        "O..O",
                        "SSSS"),
                Map.of(
                        "K", "minecraft:wither_skeleton_skull",
                        "R", "minecraft:resin_bricks",
                        "O", "minecraft:dark_oak_log",
                        "S", "minecraft:soul_sand"));
        config.worldGeneration = WorldGenerationConfig.travelPreset();
        return config;
    }

    private static void onlyFromOverworld(DimensionPortalConfig config) {
        config.sourceDimensionId = "minecraft:overworld";
        config.allowIgniteFromSource = true;
        config.allowIgniteFromTarget = false; // portals can only be activated from the overworld
        config.openFromDimensions = new ArrayList<>();
        config.generateReturnPortalOnArrival = true; // a return portal still appears so you can walk back
    }

    private static List<PortalRecipeConfig> singleRecipe(List<String> rows, Map<String, String> legend) {
        PortalRecipeConfig recipe = new PortalRecipeConfig();
        recipe.rows = new ArrayList<>(rows);
        recipe.legend = new LinkedHashMap<>(legend);
        return new ArrayList<>(List.of(recipe));
    }

    /** Parse {@link #portalColorHex} into a packed 0xRRGGBB int, falling back to a soft blue. */
    public int portalColorRgb() {
        return parseHexColor(portalColorHex, 0x66D9FF);
    }

    public static int parseHexColor(String hex, int fallback) {
        if (hex == null) {
            return fallback;
        }
        String value = hex.trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        if (value.length() == 3) {
            // shorthand #RGB -> #RRGGBB
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 3; i++) {
                char c = value.charAt(i);
                sb.append(c).append(c);
            }
            value = sb.toString();
        }
        if (value.length() != 6) {
            return fallback;
        }
        try {
            return Integer.parseInt(value, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Clamp the configured interior size range into a sane, self-consistent window. */
    public PortalFrameScanner buildScanner() {
        int minW = Math.max(1, minPortalWidth);
        int maxW = Math.max(minW, maxPortalWidth);
        int minH = Math.max(1, minPortalHeight);
        int maxH = Math.max(minH, maxPortalHeight);
        return new PortalFrameScanner(minW, maxW, minH, maxH);
    }

    /** Parse the configured shape recipes, skipping any that are malformed or have no interior. */
    public List<PortalRecipe> buildRecipes() {
        List<PortalRecipe> out = new ArrayList<>();
        if (portalRecipes == null) {
            return out;
        }
        for (PortalRecipeConfig recipe : portalRecipes) {
            if (recipe == null || recipe.rows == null || recipe.rows.isEmpty()) {
                continue;
            }
            try {
                PortalRecipe parsed = PortalRecipe.parse(recipe.rows, recipe.legend, recipe.interiorChar(), recipe.ignoreChar());
                if (parsed.hasInterior()) {
                    out.add(parsed);
                }
            } catch (RuntimeException ignored) {
                // skip malformed recipe rather than break loading
            }
        }
        return out;
    }

    public static final class PortalRecipeConfig {
        /** Grid rows, top to bottom; one char per cell. */
        public List<String> rows = new ArrayList<>();
        /** Maps a frame char to a block id, e.g. {"O":"minecraft:obsidian","G":"minecraft:glowstone"}. */
        public Map<String, String> legend = new LinkedHashMap<>();
        /** Char for interior cells that become portal blocks (default "."). */
        public String interior = ".";
        /** Char for cells that are not checked at all (default " "). */
        public String ignore = " ";

        char interiorChar() {
            return interior == null || interior.isEmpty() ? '.' : interior.charAt(0);
        }

        char ignoreChar() {
            return ignore == null || ignore.isEmpty() ? ' ' : ignore.charAt(0);
        }
    }

    public static final class WorldGenerationConfig {
        public String preset = "skyblock";
        public String generatorType = "flat";
        public String baseBiome = "minecraft:plains";
        public List<String> biomeSelection = new ArrayList<>(List.of("minecraft:plains"));
        public int minY = -64;
        public int height = 384;
        public int logicalHeight = 384;
        public boolean hasSkylight = true;
        public boolean hasCeiling = false;
        public boolean ultraWarm = false;
        public boolean natural = true;
        public double coordinateScale = 1.0;
        public boolean generateOres = false;
        public String orePreset = "minecraft:overworld";
        public List<FlatLayer> flatLayers = new ArrayList<>(List.of(
                new FlatLayer("minecraft:bedrock", 1),
                new FlatLayer("minecraft:stone", 2),
                new FlatLayer("minecraft:grass_block", 1)
        ));

        /** Empty void: no terrain, no biome (1:1 skyblock). */
        public static WorldGenerationConfig skyblockPreset() {
            WorldGenerationConfig cfg = new WorldGenerationConfig();
            cfg.preset = "skyblock_void";
            cfg.generatorType = "flat";
            cfg.baseBiome = "minecraft:the_void";
            cfg.minY = -64;
            cfg.height = 384;
            cfg.logicalHeight = 384;
            cfg.generateOres = false;
            cfg.flatLayers = new ArrayList<>(); // truly empty: the arrival platform is your start island
            return cfg;
        }

        /**
         * Superflat at high altitude with a vanilla-aligned column down to bedrock at Y -64:
         * bedrock, a thick deepslate band, a thick stone band, then a dirt + grass cap. Ores and
         * trees come from biome features ({@code generateOres = true}) on a forest surface.
         */
        public static WorldGenerationConfig miningPreset() {
            WorldGenerationConfig cfg = new WorldGenerationConfig();
            cfg.preset = "mining_superflat";
            cfg.generatorType = "flat";
            cfg.baseBiome = "minecraft:forest";
            cfg.minY = -64;
            cfg.height = 384;
            cfg.logicalHeight = 384;
            cfg.generateOres = true; // enables ore + tree features on the flat terrain
            cfg.orePreset = "minecraft:overworld";
            cfg.flatLayers = new ArrayList<>(List.of(
                    new FlatLayer("minecraft:bedrock", 1),     // Y -64
                    new FlatLayer("minecraft:deepslate", 64),  // Y -63..0
                    new FlatLayer("minecraft:stone", 188),     // Y 1..188
                    new FlatLayer("minecraft:dirt", 2),        // Y 189..190
                    new FlatLayer("minecraft:grass_block", 1)  // Y 191 (high surface)
            ));
            return cfg;
        }

        /** Single flat bedrock layer: no terrain, no biome (used for fast 10:1 travel). */
        public static WorldGenerationConfig travelPreset() {
            WorldGenerationConfig cfg = new WorldGenerationConfig();
            cfg.preset = "travel_flat";
            cfg.generatorType = "flat";
            cfg.baseBiome = "minecraft:the_void";
            cfg.minY = 0;
            cfg.height = 256;
            cfg.logicalHeight = 256;
            cfg.generateOres = false;
            cfg.flatLayers = new ArrayList<>(List.of(new FlatLayer("minecraft:bedrock", 1)));
            return cfg;
        }
    }

    public static final class FlatLayer {
        public String block = "minecraft:stone";
        public int height = 1;

        public FlatLayer() {
        }

        public FlatLayer(String block, int height) {
            this.block = block;
            this.height = height;
        }
    }
}

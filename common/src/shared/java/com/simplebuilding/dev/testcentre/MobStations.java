package com.simplebuilding.dev.testcentre;

import com.simplebuilding.blocks.ModBlocks;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mob station (Queue N43): one cell per mob of every loaded mod (Simple Mobs, Simple End Dragon Fight, ...), found in
 * the entity registry by id - SimpleBuilding imports none of them, a mod that is not loaded simply has no cell.
 *
 * <p>Each cell: the mob's habitat on the floor, a sign with its name and its spawn egg on the back wall, and buttons in
 * front - spawn it standing still (NoAI), spawn it live, remove it. Extras by mob: the Deceiver also spawns armored;
 * the Shellker sits on a Vanilla End gateway (bedrock frame) and its button puts the four guards on it, the way the
 * gateways place them in the End.
 */
public final class MobStations {

    /** Cell width; the command blocks stand at x 1, 3, 5, 7. */
    static final int CELL = 10;
    static final int WALL_Z = 8;
    /** Command blocks of a cell (front row). */
    static final int[] BUTTON_X = {1, 3, 5, 7};
    static final int BUTTON_Z = 1;
    /** Zellen je Reihe (TZ22-5: keine ellenlangen Reihen); weitere Reihen je {@link #ROW_PITCH} dahinter. */
    static final int CELLS_PER_ROW = 10;
    static final int ROW_PITCH = WALL_Z + 4;

    /** The End gateway of the Shellker cell (cell coordinates); the bedrock frame reaches 2 below and above. */
    public static final int GATEWAY_X = 4, GATEWAY_Y = 3, GATEWAY_Z = 5;

    private MobStations() {
    }

    /** Mobs of every mod except Minecraft: summonable entity types that are not MISC (projectiles, vehicles, markers). */
    public static List<EntityType<?>> modMobs() {
        List<EntityType<?>> out = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (!id.getNamespace().equals("minecraft") && type.getCategory() != MobCategory.MISC && type.canSummon()) {
                out.add(type);
            }
        }
        out.sort(Comparator.comparing(type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).toString()));
        return out;
    }

    /** Anchor (viewpoint in the aisle, facing the cell) of a mob's cell. */
    public static String anchorName(Identifier id) {
        return "mob/" + id.getNamespace() + "/" + id.getPath();
    }

    public static TcCanvas build(TcContext ctx) {
        return build(modMobs());
    }

    public static TcCanvas build(List<EntityType<?>> mobs) {
        TcCanvas c = new TcCanvas();
        if (mobs.isEmpty()) {
            return c;
        }
        // TZ22-5: hoechstens CELLS_PER_ROW Zellen je Reihe, weitere Reihen dahinter (Gang von drei Feldern dazwischen).
        int rows = (mobs.size() + CELLS_PER_ROW - 1) / CELLS_PER_ROW;
        for (int row = 0; row < rows; row++) {
            TcCanvas line = new TcCanvas();
            int from = row * CELLS_PER_ROW;
            int count = Math.min(CELLS_PER_ROW, mobs.size() - from);
            for (int i = 0; i < count; i++) {
                cell(line, i * CELL, mobs.get(from + i));
            }
            if (row == 0) {
                line.title(0, 4, WALL_Z, TcText.t("section.mobs", "Mobs"), TcText.t("section.mobs.sub", "every mod's mobs"));
            }
            line.backWall(0, count * CELL - 1, WALL_Z, 6);
            c.append(line, 0, row * ROW_PITCH);
        }
        return c;
    }

    /** Which habitat a mob gets (by id; unknown mobs stand on grass). */
    public enum Habitat { END_GATEWAY, DEEP_DARK, DARK_FOREST, SOUL_VALLEY, END_ISLAND, MEADOW, DREAD_FIGURE, WITHER_FIGURE }

    public static Habitat habitat(Identifier id) {
        return switch (id.getPath()) {
            case "shellker" -> Habitat.END_GATEWAY;
            case "hollow_howler" -> Habitat.DEEP_DARK;
            case "deceiver" -> Habitat.DARK_FOREST;
            case "soulfire_blaze" -> Habitat.SOUL_VALLEY;
            case "blighted_wither", "abyssal_wither" -> Habitat.WITHER_FIGURE;
            case "dragonmite", "hollow_watcher" -> Habitat.END_ISLAND;
            case "dread" -> Habitat.DREAD_FIGURE;
            default -> Habitat.MEADOW;
        };
    }

    private static void cell(TcCanvas c, int x0, EntityType<?> type) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        Habitat habitat = habitat(id);
        // Where the still mob stands (cell coordinates, standing level).
        int sx = habitat == Habitat.END_GATEWAY ? 7 : 4, sy = 0, sz = habitat == Habitat.END_GATEWAY ? 3 : 4;
        habitat(c, x0, habitat);
        if (habitat == Habitat.WITHER_FIGURE) witherHelmet(c, x0, id);

        String still = summon(id, BUTTON_X[0], sx, sy, sz, "{NoAI:1b,PersistenceRequired:1b,Rotation:[180f,0f]}");
        c.command(x0 + BUTTON_X[0], 1, BUTTON_Z, Direction.NORTH, still,
                TcText.t("mobs.still", "Spawn (still)"), type.getDescription());
        String live = summon(id, BUTTON_X[1], sx, sy, sz, "{PersistenceRequired:1b}");
        c.command(x0 + BUTTON_X[1], 1, BUTTON_Z, Direction.NORTH, live,
                TcText.t("mobs.live", "Spawn (live)"), TcText.t("mobs.live.sub", "with AI"));
        c.command(x0 + BUTTON_X[2], 1, BUTTON_Z, Direction.NORTH, clear(id),
                TcText.t("mobs.clear", "Remove"), type.getDescription());
        if (habitat == Habitat.DARK_FOREST) {
            String armored = summon(id, BUTTON_X[3], sx, sy, sz, "{NoAI:1b,PersistenceRequired:1b,Rotation:[180f,0f],Armored:1b,"
                    + "attributes:[{id:\"minecraft:max_health\",base:240.0d}],Health:240f}");
            c.command(x0 + BUTTON_X[3], 1, BUTTON_Z, Direction.NORTH, armored,
                    TcText.t("mobs.armored", "Spawn armored"), type.getDescription());
        }
        if (habitat == Habitat.END_GATEWAY) {
            c.command(x0 + BUTTON_X[3], 1, BUTTON_Z, Direction.NORTH, guards(id, BUTTON_X[3]),
                    TcText.t("mobs.guards", "Gateway guards"), TcText.t("mobs.guards.sub", "4 on the bedrock"));
        }

        c.wallSign(x0 + 3, 4, WALL_Z, TcText.bold(type.getDescription()), TcText.lit(id.getNamespace()),
                TcText.t("mobs.habitat." + habitat.name().toLowerCase(Locale.ROOT), pretty(habitat)));
        Identifier egg = Identifier.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "_spawn_egg");
        if (BuiltInRegistries.ITEM.containsKey(egg)) {
            c.wallFrame(x0 + 5, 4, WALL_Z, new ItemStack(BuiltInRegistries.ITEM.getValue(egg)));
        }
        c.anchor(anchorName(id), x0 + 4, 0, -2);
    }

    /** {@code summon} at a cell spot, relative to the command block at {@code (bx, 1, BUTTON_Z)}; block-aligned. */
    static String summon(Identifier id, int bx, int sx, int sy, int sz, String nbt) {
        return "execute align xyz positioned ~" + half(sx - bx) + " ~" + (sy - 1) + " ~" + half(sz - BUTTON_Z) + " run summon "
                + id + " ~ ~ ~ " + nbt;
    }

    /** Block offset to the centre of the target block, from the aligned corner ({@code -3} becomes {@code -2.5}). */
    private static String half(int blocks) {
        return Double.toString(blocks + 0.5);
    }

    static String clear(Identifier id) {
        return "kill @e[type=" + id + ",distance=..12]";
    }

    /**
     * The four gateway guards (north, south, east, west of the gateway, on the bedrock cross below), as the End gateways
     * place them (Simple Mobs' EndNetherServer.placeGuards): one command, chained {@code execute summon}s.
     */
    static String guards(Identifier id, int bx) {
        int dx = GATEWAY_X - bx, dy = GATEWAY_Y - 1, dz = GATEWAY_Z - 1 - BUTTON_Z;
        return "execute align xyz positioned ~" + half(dx) + " ~" + dy + " ~" + half(dz) + " summon " + id
                + " positioned ~ ~ ~2 summon " + id + " positioned ~1 ~ ~-1 summon " + id + " positioned ~-2 ~ ~ summon " + id
                + " positioned ~1 ~ ~ as @e[type=" + id + ",distance=..1.6] run data merge entity @s {PersistenceRequired:1b}";
    }

    private static String pretty(Habitat habitat) {
        return switch (habitat) {
            case END_GATEWAY -> "End gateway";
            case DEEP_DARK -> "Deep dark";
            case DARK_FOREST -> "Dark forest";
            case SOUL_VALLEY -> "Soul sand valley";
            case END_ISLAND -> "End island";
            case MEADOW -> "Meadow";
            case DREAD_FIGURE -> "Summoning figure";
            case WITHER_FIGURE -> "Wither figure + helmet";
        };
    }

    private static void habitat(TcCanvas c, int x0, Habitat habitat) {
        switch (habitat) {
            case END_GATEWAY -> {
                floor(c, x0, Blocks.END_STONE.defaultBlockState(), Blocks.END_STONE.defaultBlockState());
                gateway(c, x0 + GATEWAY_X, GATEWAY_Y, GATEWAY_Z);
                c.place(x0 + 1, 0, 6, Blocks.END_STONE);
                c.place(x0 + 8, 0, 6, Blocks.END_STONE);
                c.place(x0 + 8, 0, 7, Blocks.END_STONE);
                c.place(x0 + 8, 1, 7, Blocks.END_STONE);
            }
            case DEEP_DARK -> {
                floor(c, x0, Blocks.SCULK.defaultBlockState(), Blocks.DEEPSLATE_TILES.defaultBlockState());
                c.place(x0 + 1, 0, 6, Blocks.SCULK_SENSOR);
                c.place(x0 + 8, 0, 6, Blocks.SCULK_SHRIEKER);
                c.place(x0 + 8, 0, 3, Blocks.SCULK_CATALYST);
                if (ModBlocks.SCULK_JAW != null) {
                    c.place(x0 + 2, 0, 7, ModBlocks.SCULK_JAW);
                }
                c.place(x0 + 1, 0, 7, Blocks.DEEPSLATE_BRICKS);
                c.place(x0 + 1, 1, 7, Blocks.DEEPSLATE_BRICK_WALL);
            }
            case DARK_FOREST -> {
                floor(c, x0, Blocks.PODZOL.defaultBlockState(), Blocks.MOSS_BLOCK.defaultBlockState());
                for (int y = 0; y <= 2; y++) {
                    c.place(x0 + 8, y, 6, Blocks.DARK_OAK_LOG);
                }
                BlockState leaves = Blocks.DARK_OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        c.place(x0 + 8 + dx, 3, 6 + dz, leaves);
                    }
                }
                c.place(x0 + 8, 4, 6, leaves);
                c.place(x0 + 1, 0, 6, Blocks.RED_MUSHROOM);
                c.place(x0 + 2, 0, 7, Blocks.BROWN_MUSHROOM);
            }
            case SOUL_VALLEY -> {
                floor(c, x0, Blocks.SOUL_SOIL.defaultBlockState(), Blocks.SOUL_SAND.defaultBlockState());
                for (int y = 0; y <= 2; y++) {
                    c.place(x0 + 1, y, 7, Blocks.BASALT);
                }
                c.place(x0 + 8, 0, 6, Blocks.BASALT);
                c.place(x0 + 8, 1, 6, Blocks.BASALT);
                c.place(x0 + 8, 0, 3, Blocks.BONE_BLOCK);
                c.place(x0 + 7, 0, 7, Blocks.SOUL_FIRE);
            }
            case END_ISLAND -> {
                floor(c, x0, Blocks.END_STONE.defaultBlockState(), Blocks.END_STONE.defaultBlockState());
                for (int y = 0; y <= 3; y++) {
                    c.place(x0 + 8, y, 7, Blocks.OBSIDIAN);
                }
                c.place(x0 + 8, 4, 7, Blocks.END_ROD);
                c.place(x0 + 1, 0, 7, Blocks.OBSIDIAN);
                c.place(x0 + 1, 1, 7, Blocks.OBSIDIAN);
            }
            case MEADOW -> floor(c, x0, Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.MOSS_BLOCK.defaultBlockState());
            case DREAD_FIGURE -> {
                // Simple Mobs' Dread figure: iron T with an anvil in the middle and a skeleton skull on top; right-click
                // the skull with the Iron Helmet from the frame to wake it
                floor(c, x0, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), Blocks.DEEPSLATE_TILES.defaultBlockState());
                c.place(x0 + 7, 0, 6, Blocks.IRON_BLOCK);
                c.place(x0 + 7, 1, 6, Blocks.ANVIL);
                c.place(x0 + 6, 1, 6, Blocks.IRON_BLOCK);
                c.place(x0 + 8, 1, 6, Blocks.IRON_BLOCK);
                c.place(x0 + 7, 2, 6, Blocks.SKELETON_SKULL);
                c.wallFrame(x0 + 5, 2, WALL_Z, new ItemStack(net.minecraft.world.item.Items.IRON_HELMET));
            }
            case WITHER_FIGURE -> {
                // the Wither T with the middle skull and one side skull: helmet on the middle skull, then the third skull
                floor(c, x0, Blocks.SOUL_SOIL.defaultBlockState(), Blocks.SOUL_SAND.defaultBlockState());
                c.place(x0 + 7, 0, 6, Blocks.SOUL_SAND);
                for (int x = 6; x <= 8; x++) {
                    c.place(x0 + x, 1, 6, Blocks.SOUL_SAND);
                }
                c.place(x0 + 7, 2, 6, Blocks.WITHER_SKELETON_SKULL);
                c.place(x0 + 6, 2, 6, Blocks.WITHER_SKELETON_SKULL);
                c.place(x0 + 1, 0, 7, Blocks.SOUL_FIRE);
            }
        }
    }

    /**
     * Simple Mobs' stronger Withers (owner N50): the frames hold the helmet - Netherite for the Blighted Wither, Enderite
     * (if a mod adds it) for the Abyssal Wither - and the third skull. Put the helmet on the middle skull, then the skull.
     */
    private static void witherHelmet(TcCanvas c, int x0, Identifier id) {
        Identifier enderite = Identifier.fromNamespaceAndPath("simplebuilding", "enderite_helmet");
        ItemStack helmet = id.getPath().equals("abyssal_wither") && BuiltInRegistries.ITEM.containsKey(enderite)
                ? new ItemStack(BuiltInRegistries.ITEM.getValue(enderite)) : new ItemStack(net.minecraft.world.item.Items.NETHERITE_HELMET);
        c.wallFrame(x0 + 5, 2, WALL_Z, helmet);
        c.wallFrame(x0 + 6, 2, WALL_Z, new ItemStack(net.minecraft.world.item.Items.WITHER_SKELETON_SKULL));
    }

    /** The cell's floor (x 1..8, z 2..7) in a loose checker of two blocks. */
    private static void floor(TcCanvas c, int x0, BlockState a, BlockState b) {
        for (int x = 1; x <= 8; x++) {
            for (int z = 2; z <= 7; z++) {
                c.place(x0 + x, -1, z, (x * 3 + z * 5) % 4 == 0 ? b : a);
            }
        }
    }

    /**
     * Vanilla's End gateway (EndGatewayFeature): the gateway block, air around it at its height, a bedrock cross one
     * below and one above, a single bedrock two below and two above.
     */
    private static void gateway(TcCanvas c, int gx, int gy, int gz) {
        c.place(gx, gy, gz, Blocks.END_GATEWAY);
        for (int dy : new int[]{-1, 1}) {
            c.place(gx, gy + dy, gz, Blocks.BEDROCK);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                c.place(gx + d.getStepX(), gy + dy, gz + d.getStepZ(), Blocks.BEDROCK);
            }
        }
        c.place(gx, gy - 2, gz, Blocks.BEDROCK);
        c.place(gx, gy + 2, gz, Blocks.BEDROCK);
    }
}

package com.simplebuilding.modules.simplechallenges.skyline;

import com.simplebuilding.modules.simplechallenges.SimpleChallenges;
import com.simplebuilding.modules.simplechallenges.lucky.BlockCatalog;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

/**
 * The Easter eggs of a linear or grid Skyline (S7), in -X where no line block stands: End = a small ship with an Elytra in a frame,
 * Nether = a mini bastion whose chest always holds a smithing template, Overworld = a vault that gives a Heavy Core and a frame with
 * a trial key. Each egg exists twice at the same distance (main at -X, backup 30 degrees aside). The Elytra and the key come back
 * whenever someone takes them ({@link #keep}); the vault stays Vanilla (each player opens it once). The structures are plain
 * block lists built in code (no template files, so they are fully testable) and built when the chunk of their anchor is generated.
 */
public final class Eggs {
    public enum Kind { END_SHIP, NETHER_BASTION, OVERWORLD_VAULT }

    /** An egg place: {@code anchor} is the middle of its floor at line height. */
    public record Site(String name, Kind kind, BlockPos anchor) {}

    /** An item frame of an egg that is kept filled. */
    public record Frame(BlockPos pos, Direction facing, Item item) {}

    public static final String LOOT_VAULT = "simplechallenges:vault/overworld";
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    private Eggs() {}

    public static Kind kindOf(BlockCatalog.Dim dim) {
        return switch (dim) {
            case END -> Kind.END_SHIP;
            case NETHER -> Kind.NETHER_BASTION;
            case OVERWORLD -> Kind.OVERWORLD_VAULT;
        };
    }

    /** The two sites of a dimension (none for a ring or when eggs are off): main at -X, backup at the same distance 30 degrees aside (+z). */
    public static List<Site> sites(SkylinePlans.Dim d, boolean eggsOn) {
        if (!eggsOn) return List.of();
        int dist = d.eggDistance;
        Kind kind = kindOf(d.kind);
        return List.of(new Site("main", kind, new BlockPos(d.ox - dist, d.oy, d.oz)),
                new Site("backup", kind, new BlockPos(d.ox - (int) Math.round(dist * Math.cos(Math.toRadians(30))), d.oy, d.oz + (int) Math.round(dist * Math.sin(Math.toRadians(30))))));
    }

    /** The state key of a built site. */
    public static String key(SkylinePlans.Dim d, Site s) {
        return d.key.identifier() + "|" + s.name();
    }

    public static boolean inChunk(Site s, int chunkX, int chunkZ) {
        return (s.anchor().getX() >> 4) == chunkX && (s.anchor().getZ() >> 4) == chunkZ;
    }

    public static List<Frame> frames(Site s) {
        BlockPos a = s.anchor();
        return switch (s.kind()) {
            case END_SHIP -> List.of(new Frame(a.offset(-3, 1, 0), Direction.EAST, Items.ELYTRA));
            case OVERWORLD_VAULT -> List.of(new Frame(a.offset(0, 1, -1), Direction.SOUTH, Items.TRIAL_KEY));
            case NETHER_BASTION -> List.of();
        };
    }

    private static void put(ServerLevel level, BlockPos a, int dx, int dy, int dz, Block b) {
        level.setBlock(a.offset(dx, dy, dz), b.defaultBlockState(), FLAGS);
    }

    private static void put(ServerLevel level, BlockPos a, int dx, int dy, int dz, BlockState s) {
        level.setBlock(a.offset(dx, dy, dz), s, FLAGS);
    }

    /** Builds a whole site (loads the chunks it covers) and puts in what it keeps: frames, chest, vault. */
    public static void build(ServerLevel level, Site s) {
        BlockPos a = s.anchor();
        switch (s.kind()) {
            case END_SHIP -> {
                for (int x = -4; x <= 4; x++) {
                    for (int z = -2; z <= 2; z++) {
                        put(level, a, x, 0, z, Blocks.PURPUR_BLOCK);
                        boolean wall = Math.abs(x) == 4 || Math.abs(z) == 2;
                        boolean door = x == 4 && Math.abs(z) <= 1;
                        for (int y = 1; y <= 2; y++) put(level, a, x, y, z, wall && !door ? Blocks.PURPUR_PILLAR : Blocks.AIR);
                    }
                }
                put(level, a, -4, 1, 0, Blocks.PURPUR_BLOCK); // the wall the Elytra frame hangs on
                put(level, a, -4, 2, 0, Blocks.PURPUR_BLOCK);
                for (int sx : new int[]{-4, 4}) for (int sz : new int[]{-2, 2}) put(level, a, sx, 3, sz, Blocks.END_ROD);
            }
            case NETHER_BASTION -> {
                for (int x = -3; x <= 3; x++) {
                    for (int z = -3; z <= 3; z++) {
                        boolean wall = Math.abs(x) == 3 || Math.abs(z) == 3;
                        boolean door = x == 3 && z == 0;
                        boolean gilded = Math.abs(x) == 2 && Math.abs(z) == 2;
                        put(level, a, x, 0, z, gilded ? Blocks.GILDED_BLACKSTONE : Blocks.POLISHED_BLACKSTONE_BRICKS);
                        for (int y = 1; y <= 3; y++) put(level, a, x, y, z, wall && !(door && y <= 2) ? Blocks.BLACKSTONE_BRICKS : Blocks.AIR);
                        put(level, a, x, 4, z, Blocks.BLACKSTONE_BRICKS);
                    }
                }
                BlockPos chest = a.offset(-2, 1, 0);
                level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST), FLAGS);
                if (level.getBlockEntity(chest) instanceof ChestBlockEntity c) {
                    ItemStack[] loot = {new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), new ItemStack(Items.GOLD_INGOT, 12), new ItemStack(Items.GOLDEN_APPLE),
                            new ItemStack(Items.CRYING_OBSIDIAN, 4), new ItemStack(Items.GILDED_BLACKSTONE, 2)};
                    for (int i = 0; i < loot.length; i++) c.setItem(i, loot[i]);
                    c.setChanged();
                }
            }
            case OVERWORLD_VAULT -> {
                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        boolean wall = Math.abs(x) == 2 || Math.abs(z) == 2;
                        boolean door = x == 2 && z == 0;
                        put(level, a, x, 0, z, Blocks.DEEPSLATE_TILES);
                        for (int y = 1; y <= 3; y++) put(level, a, x, y, z, wall && !(door && y <= 2) ? Blocks.DEEPSLATE_BRICKS : Blocks.AIR);
                        put(level, a, x, 4, z, Blocks.DEEPSLATE_BRICKS);
                    }
                }
                put(level, a, 1, 1, 1, Blocks.LANTERN);
                BlockPos vault = a.offset(-1, 1, 0);
                BlockState state = Blocks.VAULT.defaultBlockState();
                if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST);
                level.setBlock(vault, state, FLAGS);
                configureVault(level, vault);
            }
        }
        keep(level, s);
    }

    /** Points the vault at the guaranteed Heavy Core loot table and the trial key (the block entity is configured like a command would). */
    static void configureVault(ServerLevel level, BlockPos vault) {
        MinecraftServer server = level.getServer();
        String cmd = "data merge block " + vault.getX() + " " + vault.getY() + " " + vault.getZ()
                + " {config:{key_item:{id:\"minecraft:trial_key\",count:1},loot_table:\"" + LOOT_VAULT + "\"}}";
        try {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withLevel(level).withSuppressedOutput(), cmd);
        } catch (RuntimeException e) {
            SimpleChallenges.LOG.warn("Could not configure the vault at {}: {}", vault, e.toString());
        }
    }

    /** Fills the frames of a built site again: a taken Elytra or key is put back, a missing frame is hung anew. Only loaded places are touched. */
    public static int keep(ServerLevel level, Site s) {
        int refilled = 0;
        for (Frame f : frames(s)) {
            if (!level.hasChunkAt(f.pos())) continue;
            List<ItemFrame> found = level.getEntitiesOfClass(ItemFrame.class, new AABB(f.pos()));
            if (found.isEmpty()) {
                ItemFrame frame = new ItemFrame(level, f.pos(), f.facing());
                frame.setItem(new ItemStack(f.item()), false);
                frame.setInvulnerable(true);
                level.addFreshEntity(frame);
                refilled++;
            } else {
                for (ItemFrame frame : found) {
                    if (frame.getItem().isEmpty()) {
                        frame.setItem(new ItemStack(f.item()), false);
                        frame.setInvulnerable(true);
                        refilled++;
                    }
                }
            }
        }
        return refilled;
    }

    /** The End portal the seal opens: a ring of 12 frames with eyes round 3 x 3 portal blocks, its middle 4 blocks beyond the seal. */
    public static void buildPortal(ServerLevel level, BlockPos seal) {
        BlockPos c = seal.offset(4, 0, 0);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) == 2 && Math.abs(dz) == 2) continue;
                BlockState state;
                if (Math.abs(dx) < 2 && Math.abs(dz) < 2) {
                    state = Blocks.END_PORTAL.defaultBlockState();
                } else {
                    Direction toMiddle = dz == -2 ? Direction.SOUTH : dz == 2 ? Direction.NORTH : dx == -2 ? Direction.EAST : Direction.WEST;
                    state = Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(EndPortalFrameBlock.FACING, toMiddle).setValue(EndPortalFrameBlock.HAS_EYE, true);
                }
                level.setBlock(c.offset(dx, 0, dz), state, Block.UPDATE_ALL);
            }
        }
    }
}

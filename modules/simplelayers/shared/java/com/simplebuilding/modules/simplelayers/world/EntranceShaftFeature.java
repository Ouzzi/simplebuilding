package com.simplebuilding.modules.simplelayers.world;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.modules.simplelayers.SimpleLayers;
import com.simplebuilding.modules.simplelayers.world.gen.HouseTemplate;
import com.simplebuilding.modules.simplelayers.world.gen.Piece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * The Backrooms entrance in the Overworld (BACK20 house, BACK22 core K6): the chunks {@code Entrances.houseChunk} picks (pure, rarity
 * {@code backrooms.shaftRarity}, 0 = never) get the abandoned house of {@link HouseTemplate} in their middle, only on nearly flat,
 * dry ground. Its cellar holds the cellar double door (portal doors, rule two-way) into the Lobby's entrance door in the same chunk
 * column; the house registers its door in SimpleLib's {@code PortalLinks} (its height is only known here). The wood follows the biome;
 * the barrels hold {@code simplelayers:chests/backrooms_house}. (Feature id and config key keep their BACK18 names.)
 */
public record EntranceShaftFeature() implements Feature {
    public static final MapCodec<EntranceShaftFeature> CODEC = MapCodec.unit(EntranceShaftFeature::new);
    /** Largest height difference of the ground under the house (blocks). */
    public static final int MAX_SLOPE = 2;
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, SimpleLayers.id("chests/backrooms_house"));

    @Override
    public MapCodec<EntranceShaftFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int chunkX = Math.floorDiv(origin.getX(), 16), chunkZ = Math.floorDiv(origin.getZ(), 16);
        if (!SimpleLayers.backroomsOn() || !com.simplebuilding.modules.simplelayers.backrooms.door.Entrances.houseChunk(level.getSeed(), chunkX, chunkZ)) return false;
        int cx = chunkX * 16 + 8, cz = chunkZ * 16 + 8;
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                int h = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, cx + dx, cz + dz);
                min = Math.min(min, h);
                max = Math.max(max, h);
            }
        }
        if (max - min > MAX_SLOPE) return false;
        BlockPos ground = new BlockPos(cx, level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, cx, cz) - 1, cz);
        BlockState top = level.getBlockState(ground);
        if (!top.getFluidState().isEmpty() || top.isAir() || ground.getY() - HouseTemplate.DEPTH <= level.getMinY() + 1) return false;
        BlockPos anchor = build(level, ground, wood(level.getBiome(ground).value()), random);
        var server = level.getServer();
        if (server != null) {
            long link = com.simplebuilding.modules.simplelayers.backrooms.door.Entrances.link(chunkX, chunkZ);
            var end = new com.simplelib.api.portal.PortalLinks.End(net.minecraft.world.level.Level.OVERWORLD, anchor, net.minecraft.core.Direction.WEST, 2);
            server.execute(() -> com.simplelib.api.portal.PortalLinks.put(server, link, end)); // saved data only on the server thread
        }
        return true;
    }

    /** The house's wood for a biome: spruce below temperature 0.3, acacia above 1.5 (savanna, desert edge), else oak. */
    public static BackroomsBlocks.Wood wood(Biome biome) {
        float t = biome.getBaseTemperature();
        return t < 0.3F ? BackroomsBlocks.Wood.SPRUCE : t > 1.5F ? BackroomsBlocks.Wood.ACACIA : BackroomsBlocks.Wood.OAK;
    }

    /** Builds the house with its floor (dy 0) at {@code ground}; returns the cellar door's anchor (left leaf, lower half). */
    public static BlockPos build(LevelAccessor level, BlockPos ground, BackroomsBlocks.Wood wood, RandomSource random) {
        int broken = HouseTemplate.brokenWindow(ground.getX(), ground.getZ());
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dy = -HouseTemplate.DEPTH; dy <= HouseTemplate.HEIGHT; dy++) {
            for (int dx = HouseTemplate.MIN_X; dx <= HouseTemplate.MAX_X; dx++) {
                for (int dz = -HouseTemplate.HALF_Z; dz <= HouseTemplate.HALF_Z; dz++) {
                    Piece piece = HouseTemplate.piece(dx, dy, dz, broken);
                    if (piece == null) continue;
                    p.set(ground.getX() + dx, ground.getY() + dy, ground.getZ() + dz);
                    level.setBlock(p, BackroomsBlocks.wood(piece, wood), 2);
                    if (piece == Piece.BARREL_LOOT) RandomizableContainer.setBlockEntityLootTable(level, random, p.immutable(), LOOT);
                }
            }
        }
        // the cellar double door (walking west into it; symbol 1 = the Lobby)
        BlockPos anchor = ground.offset(HouseTemplate.PLUG_X, HouseTemplate.DOOR_DY, HouseTemplate.DOOR_LEFT_Z);
        for (int i = 0; i < 2; i++) {
            BlockPos leaf = anchor.relative(net.minecraft.core.Direction.NORTH, i);
            BlockState door = com.simplebuilding.modules.simplelayers.backrooms.door.DoorStamp.door(net.minecraft.core.Direction.WEST,
                    i == 0 ? net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT : net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT,
                    1, com.simplelib.api.portal.PortalRule.TWO_WAY).setValue(com.simplebuilding.modules.simplelayers.block.BackroomsDoorBlock.SYMBOL, 1);
            level.setBlock(leaf, door, 2);
            level.setBlock(leaf.above(), door.setValue(net.minecraft.world.level.block.DoorBlock.HALF,
                    net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), 2);
        }
        return anchor;
    }
}

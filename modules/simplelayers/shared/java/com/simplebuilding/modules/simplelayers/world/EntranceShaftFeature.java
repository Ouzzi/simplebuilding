package com.simplebuilding.modules.simplelayers.world;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.modules.simplelayers.SimpleLayers;
import com.simplebuilding.modules.simplelayers.config.LayersConfig;
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
 * The Backrooms entrance in the Overworld (BACK18 shaft, BACK20 house, plan docs/ai/PLAN-B-BACK20-2026-11-10.md): one new chunk in
 * {@code backrooms.shaftRarity} (0 = never) gets the abandoned house of {@link HouseTemplate} in its middle, only on nearly flat,
 * dry ground. Its cellar is concreted shut; behind the plug the old maintenance shaft leads down to the rift floor, the portal into
 * Level 0. The wood follows the biome (spruce when cold, acacia when hot and dry, else oak); the barrels hold
 * {@code simplelayers:chests/backrooms_house}. The Glitched Bedrock stays as the hidden second way in. (Feature id and config key
 * keep their BACK18 names so worlds and config files stay valid.)
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
        if (!SimpleLayers.backroomsOn() || LayersConfig.shaftRarity <= 0 || random.nextInt(LayersConfig.shaftRarity) != 0) return false;
        int cx = Math.floorDiv(origin.getX(), 16) * 16 + 8, cz = Math.floorDiv(origin.getZ(), 16) * 16 + 8;
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
        build(level, ground, wood(level.getBiome(ground).value()), random);
        return true;
    }

    /** The house's wood for a biome: spruce below temperature 0.3, acacia above 1.5 (savanna, desert edge), else oak. */
    public static BackroomsBlocks.Wood wood(Biome biome) {
        float t = biome.getBaseTemperature();
        return t < 0.3F ? BackroomsBlocks.Wood.SPRUCE : t > 1.5F ? BackroomsBlocks.Wood.ACACIA : BackroomsBlocks.Wood.OAK;
    }

    /** Builds the house with its floor (dy 0) at {@code ground}; the gametest calls this directly. */
    public static void build(LevelAccessor level, BlockPos ground, BackroomsBlocks.Wood wood, RandomSource random) {
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
    }
}

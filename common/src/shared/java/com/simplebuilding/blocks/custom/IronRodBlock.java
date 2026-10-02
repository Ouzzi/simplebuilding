package com.simplebuilding.blocks.custom;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.blocks.ModBlocks;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Eisenstab (Besitzer 2026-10-02): eine Kopie des Blitzableiters aus Eisen - gleiche Form, gleiche Ausrichtung,
 * gleiches Redstone-Signal beim Einschlag (Vanillas {@code LightningBolt} versorgt jeden {@link LightningRodBlock}),
 * oxidiert nicht. Er zieht Blitze schwaecher an: nur im Umkreis von {@link #RANGE} Bloecken statt 128, und nur wenn
 * kein Kupfer-Blitzableiter in Reichweite ist (der Kupferstab gewinnt). Zutat fuer den Resonanzstab und den
 * Rotator.
 */
public class IronRodBlock extends LightningRodBlock {
    /** Anziehungs-Radius in Bloecken (Kupfer-Blitzableiter: {@link LightningRodBlock#RANGE} = 128). */
    public static final int RANGE = 32;
    public static final MapCodec<IronRodBlock> CODEC = com.simplebuilding.version.BlockCodecs.simple(IronRodBlock::new);

    public IronRodBlock(Properties properties) {
        super(properties);
    }

    // No @Override: MC 26.3 removed block codecs; this only overrides on 26.2.
    public MapCodec<IronRodBlock> codec() {
        return CODEC;
    }

    /**
     * Der naechste Eisenstab, der oben auf seiner Saeule steht (wie Vanillas Bedingung fuer Blitzableiter), im Umkreis
     * von {@link #RANGE} um {@code center}; nur geladene Chunks. Gibt wie Vanilla den Block ueber dem Stab zurueck.
     */
    public static Optional<BlockPos> find(ServerLevel level, BlockPos center) {
        if (ModBlocks.IRON_ROD == null) {
            return Optional.empty();
        }
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        long bestDistance = Long.MAX_VALUE;
        for (int dx = -RANGE; dx <= RANGE; dx++) {
            for (int dz = -RANGE; dz <= RANGE; dz++) {
                if (dx * dx + dz * dz > RANGE * RANGE) {
                    continue;
                }
                int x = center.getX() + dx;
                int z = center.getZ() + dz;
                if (!level.hasChunk(x >> 4, z >> 4)) {
                    continue;
                }
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                probe.set(x, y, z);
                if (level.getBlockState(probe).is(ModBlocks.IRON_ROD)) {
                    long dy = y - center.getY();
                    long distance = (long) dx * dx + (long) dz * dz + dy * dy;
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = probe.immutable();
                    }
                }
            }
        }
        return best == null ? Optional.empty() : Optional.of(best.above());
    }
}

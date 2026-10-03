package com.simplebuilding.blocks.custom;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Metallstab (Besitzer 2026-10-02, Netherit/Enderit 2026-10-03): eine Kopie des Blitzableiters aus Eisen, Gold,
 * Netherit oder Enderit - gleiche Form, gleiche
 * Ausrichtung, gleiches Redstone-Signal beim Einschlag (Vanillas {@code LightningBolt} versorgt jeden
 * {@link LightningRodBlock}), oxidiert nicht. Er zieht Blitze schwaecher an als Kupfer: nur im Umkreis seiner eigenen
 * {@link #range()} (Eisen {@link #IRON_RANGE}, Gold {@link #GOLD_RANGE}, Netherit {@link #NETHERITE_RANGE}, Enderit
 * {@link #ENDERITE_RANGE} = Kupfer 128), und nur wenn kein
 * Kupfer-Blitzableiter in Reichweite ist (der Kupferstab gewinnt). Unter den Metallstaeben gewinnt der naechste.
 */
public class MetalRodBlock extends LightningRodBlock {
    /** Anziehungs-Radius des Eisenstabs in Bloecken (Kupfer-Blitzableiter: 128). */
    public static final int IRON_RANGE = 32;
    /** Anziehungs-Radius des Goldstabs in Bloecken. */
    public static final int GOLD_RANGE = 64;
    /** Anziehungs-Radius des Netheritstabs in Bloecken (Familie in 32er-Schritten, 2026-10-03). */
    public static final int NETHERITE_RANGE = 96;
    /** Anziehungs-Radius des Enderitstabs in Bloecken: so weit wie der Kupfer-Blitzableiter, nicht weiter. */
    public static final int ENDERITE_RANGE = 128;
    /** Groesster Radius aller Metallstaebe und harte Obergrenze: so weit sucht {@link #find}. */
    public static final int MAX_RANGE = ENDERITE_RANGE;
    public static final MapCodec<MetalRodBlock> CODEC = com.simplebuilding.version.BlockCodecs.simple(p -> new MetalRodBlock(IRON_RANGE, p));

    private final int range;

    public MetalRodBlock(int range, Properties properties) {
        super(properties);
        this.range = Math.min(range, MAX_RANGE);
    }

    /** Anziehungs-Radius dieses Stabs in Bloecken. */
    public int range() {
        return range;
    }

    // No @Override: MC 26.3 removed block codecs; this only overrides on 26.2.
    public MapCodec<MetalRodBlock> codec() {
        return CODEC;
    }

    /**
     * Der naechste Metallstab, der oben auf seiner Saeule steht (wie Vanillas Bedingung fuer Blitzableiter) und dessen
     * eigene Reichweite (waagrechter Kreis) {@code center} erreicht; nur geladene Chunks. Gibt wie Vanilla den Block
     * ueber dem Stab zurueck.
     */
    public static Optional<BlockPos> find(ServerLevel level, BlockPos center) {
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        long bestDistance = Long.MAX_VALUE;
        for (int dx = -MAX_RANGE; dx <= MAX_RANGE; dx++) {
            for (int dz = -MAX_RANGE; dz <= MAX_RANGE; dz++) {
                int horizontal = dx * dx + dz * dz;
                if (horizontal > MAX_RANGE * MAX_RANGE) {
                    continue;
                }
                int x = center.getX() + dx;
                int z = center.getZ() + dz;
                if (!level.hasChunk(x >> 4, z >> 4)) {
                    continue;
                }
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                probe.set(x, y, z);
                if (level.getBlockState(probe).getBlock() instanceof MetalRodBlock rod && horizontal <= rod.range * rod.range) {
                    long dy = y - center.getY();
                    long distance = horizontal + dy * dy;
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

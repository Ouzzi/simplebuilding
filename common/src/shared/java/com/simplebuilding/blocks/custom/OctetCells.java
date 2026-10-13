package com.simplebuilding.blocks.custom;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.entity.custom.MixedOctetBlockEntity;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Achtel verschiedener Materialien in einer Zelle (Queue N48, docs/ai/PLAN-OCTETS-MIX-2026-10-13.md). Eine Zelle wird
 * hier als acht "Teile" gelesen und geschrieben: je Achtel der <em>Materialzustand</em> (der Zustand einer reinen Zelle
 * ohne Bits und ohne Wasser, z. B. {@code oak_octet} oder {@code checker_octet[color=lapis]}) oder null.
 *
 * <p>{@link #set} normalisiert: ein Material ergibt die reine, gebackene Zelle (kein Block-Entity), mehrere die
 * gemischte Zelle {@link MixedOctetBlock} mit {@link MixedOctetBlockEntity}.
 */
public final class OctetCells {
    private OctetCells() {
    }

    /** Der Materialzustand eines Zellzustands (Bits aus, kein Wasser); null fuer gemischte und fremde Zustaende. */
    public static @Nullable BlockState material(@Nullable BlockState state) {
        if (state == null || !(state.getBlock() instanceof OctetCellBlock) || state.getBlock() instanceof MixedOctetBlock) {
            return null;
        }
        BlockState out = state.setValue(OctetCellBlock.WATERLOGGED, false);
        for (int i = 0; i < 8; i++) {
            out = out.setValue(OctetCellBlock.OCTETS[i], false);
        }
        return out;
    }

    /** Die acht Teile der Zelle an {@code pos} (leer ausserhalb von Achtelzellen). */
    public static BlockState[] parts(BlockGetter level, BlockPos pos, BlockState state) {
        BlockState[] parts = new BlockState[8];
        if (!(state.getBlock() instanceof OctetCellBlock)) {
            return parts;
        }
        int mask = OctetCellBlock.mask(state);
        if (state.getBlock() instanceof MixedOctetBlock) {
            if (level.getBlockEntity(pos) instanceof MixedOctetBlockEntity be) {
                for (int i = 0; i < 8; i++) {
                    parts[i] = (mask & (1 << i)) != 0 ? be.part(i) : null;
                }
            }
            return parts;
        }
        BlockState material = material(state);
        for (int i = 0; i < 8; i++) {
            if ((mask & (1 << i)) != 0) {
                parts[i] = material;
            }
        }
        return parts;
    }

    /** Bitmaske der belegten Teile. */
    public static int mask(BlockState[] parts) {
        int mask = 0;
        for (int i = 0; i < 8; i++) {
            if (parts[i] != null) {
                mask |= 1 << i;
            }
        }
        return mask;
    }

    /** Das einzige Material der Teile, null bei keinem oder mehreren. */
    public static @Nullable BlockState single(BlockState[] parts) {
        BlockState only = null;
        for (BlockState part : parts) {
            if (part == null) {
                continue;
            }
            if (only != null && only != part) {
                return null;
            }
            only = part;
        }
        return only;
    }

    /** Das haeufigste Material (bei Gleichstand das des niedrigsten Achtels); null ohne Teile. */
    public static @Nullable BlockState dominant(BlockState[] parts) {
        BlockState best = null;
        int bestCount = 0;
        for (BlockState candidate : parts) {
            if (candidate == null) {
                continue;
            }
            int count = 0;
            for (BlockState other : parts) {
                if (other == candidate) {
                    count++;
                }
            }
            if (count > bestCount) {
                best = candidate;
                bestCount = count;
            }
        }
        return best;
    }

    /**
     * Der Zustand, den die Teile ohne Block-Entity hätten: reine Zelle (bei mehreren Materialien die des haeufigsten mit
     * allen Bits - fuer Blaupausen, die nur Zustaende speichern) bzw. Luft/Wasser ohne Teile.
     */
    public static BlockState plainState(BlockState[] parts, boolean wet) {
        BlockState material = dominant(parts);
        if (material == null) {
            return wet ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
        }
        return OctetCellBlock.withMask(material.setValue(OctetCellBlock.WATERLOGGED, wet), mask(parts));
    }

    /** Fuer Blaupausen und Kopien ohne Block-Entity: gemischte Zellen werden zur reinen Zelle des haeufigsten Materials. */
    public static BlockState portable(BlockGetter level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof MixedOctetBlock)) {
            return state;
        }
        return plainState(parts(level, pos, state), state.getValue(OctetCellBlock.WATERLOGGED));
    }

    /** Ob die Zelle an {@code pos} Wasser haelt. */
    public static boolean wet(BlockState state) {
        return state.hasProperty(OctetCellBlock.WATERLOGGED) && state.getValue(OctetCellBlock.WATERLOGGED);
    }

    /**
     * Schreibt die Teile als Zelle an {@code pos}: keine Teile = Luft bzw. Wasser, ein Material = reine Zelle (gebacken,
     * kein Block-Entity), mehrere = {@link MixedOctetBlock} + Block-Entity. Volle Zellen halten kein Wasser.
     */
    public static boolean set(LevelAccessor level, BlockPos pos, BlockState[] parts, boolean wet, int flags) {
        BlockState single = single(parts);
        int mask = mask(parts);
        if (mask == 0 || single != null || ModBlocks.MIXED_OCTET == null) {
            return level.setBlock(pos, plainState(parts, wet), flags);
        }
        int light = 0;
        for (BlockState part : parts) {
            if (part != null) {
                light = Math.max(light, part.getLightEmission());
            }
        }
        BlockState mixed = OctetCellBlock.withMask(ModBlocks.MIXED_OCTET.defaultBlockState()
                .setValue(OctetCellBlock.WATERLOGGED, wet).setValue(MixedOctetBlock.LIGHT, light), mask);
        if (!level.setBlock(pos, mixed, flags)) {
            return false;
        }
        if (level.getBlockEntity(pos) instanceof MixedOctetBlockEntity be) {
            be.setParts(parts);
            return true;
        }
        return false;
    }

    /** Das Achtel-Item eines Materialzustands. */
    public static ItemStack item(@Nullable BlockState part) {
        if (part == null || !(part.getBlock() instanceof OctetCellBlock cell)) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(cell.octetItem(part));
    }

    /** Der Zustand, mit dem ein einzelnes Achtel gezeichnet/gehoert wird (Material mit genau diesem Bit). */
    public static BlockState single(BlockState part, int index) {
        return OctetCellBlock.withMask(part, 1 << index);
    }

    /**
     * Das Achtel, auf das {@code player} schaut: Blickstrahl ueber die Reichweite gegen die Form der Zelle; trifft er
     * keins (schnelles Wegdrehen), das hoechste belegte.
     */
    public static int lookedAt(Player player, BlockPos pos, BlockState state) {
        int mask = OctetCellBlock.mask(state);
        Vec3 eye = player.getEyePosition();
        Vec3 reach = eye.add(player.getViewVector(1.0F).scale(player.blockInteractionRange() + 1.0));
        BlockHitResult hit = OctetCellBlock.shape(mask).clip(eye, reach, pos);
        if (hit != null) {
            int index = OctetCellBlock.hitIndex(pos, hit);
            if ((mask & (1 << index)) != 0) {
                return index;
            }
        }
        return mask == 0 ? 0 : 31 - Integer.numberOfLeadingZeros(mask);
    }

    /** Kopie ohne Achtel {@code index}. */
    public static BlockState[] without(BlockState[] parts, int index) {
        BlockState[] rest = Arrays.copyOf(parts, 8);
        rest[index] = null;
        return rest;
    }
}

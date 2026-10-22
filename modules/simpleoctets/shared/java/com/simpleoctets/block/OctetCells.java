package com.simpleoctets.block;

import com.simpleoctets.item.MaterialOctetItem;
import com.simpleoctets.registry.OctetRegistry;
import java.util.Arrays;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.Direction;
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
 * gemischte Zelle {@link MixedOctetBlock} mit {@link MixedOctetBlockEntity}; acht Achtel eines Materials mit Vollblock
 * ergeben den Vollblock ({@link #fullBlock}, Besitzer SP22-1).
 */
public class OctetCells {
    /** Nur fuer Unterklassen gleichen Namens in Partner-Mods (SimpleBuilding 26.3 erbt die Statics). */
    protected OctetCells() {
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
        BlockState full = mask == OctetCellBlock.FULL ? fullBlock(single) : null;
        if (full != null) {
            return level.setBlock(pos, full, flags);
        }
        if (mask == 0 || single != null || OctetRegistry.MIXED_OCTET == null) {
            return level.setBlock(pos, plainState(parts, wet), flags);
        }
        int light = 0;
        for (BlockState part : parts) {
            if (part != null) {
                light = Math.max(light, part.getLightEmission());
            }
        }
        BlockState mixed = OctetCellBlock.withMask(OctetRegistry.MIXED_OCTET.defaultBlockState()
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

    /**
     * Der Vollblock, zu dem acht Achtel des Materials {@code part} werden (Besitzer SP22-1): der Quellblock einer
     * Material-Achtelzelle, deren Achtel ein {@link MaterialOctetItem} ist (8 Achtel = 1 Block, das Rezept
     * {@code <block>_from_octets}); null fuer Melone (Scheiben sind keine Achtel), Schach-Achtel (kein einfarbiger
     * Vollblock), gemischte Zellen und bei ausgeschaltetem Sub-Mod.
     */
    public static @Nullable BlockState fullBlock(@Nullable BlockState part) {
        if (part == null || !(part.getBlock() instanceof MaterialOctetBlock cell) || !(cell.piece() instanceof MaterialOctetItem)
                || !com.simpleoctets.SimpleOctets.on()) {
            return null;
        }
        return cell.sourceState();
    }

    /** Die Achtelzelle, in die ein Vollblock beim Ecken-Abbau zerfaellt (Umkehrung von {@link #fullBlock}), oder null. */
    public static @Nullable Block cornerCell(Block full) {
        Block cell = com.simplelib.api.OctetCells.cellFor(full);
        BlockState back = cell == null ? null : fullBlock(material(cell.defaultBlockState()));
        return back != null && back.getBlock() == full ? cell : null;
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

    /** Gibt dem Spieler ein Achtel: in die leere Haupthand, sonst ins Inventar, sonst vor die Fuesse. */
    public static void give(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (player.getMainHandItem().isEmpty()) {
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
            return;
        }
        if (!player.getInventory().add(stack) && !stack.isEmpty()) {
            Block.popResource(player.level(), player.blockPosition(), stack);
        }
    }

    /** Setzt ein Achtel der Zelle {@code block} nach dem Trefferpunkt und verbraucht ein Item. */
    public static InteractionResult place(UseOnContext context, @Nullable Block block) {
        return block == null ? InteractionResult.PASS : place(context, block.defaultBlockState());
    }

    /**
     * Setzt ein Achtel des Materials {@code material} (Zustand einer reinen Achtelzelle, z. B. mit Schachfarbe) nach dem
     * Trefferpunkt und verbraucht ein Item. Eine Zelle anderen Materials wird zur gemischten Zelle (Queue N48,
     * {@link OctetCells#set}).
     */
    public static InteractionResult place(UseOnContext context, BlockState material) {
        Player player = context.getPlayer();
        BlockState part = OctetCells.material(material);
        if (part == null || (player != null && !player.mayBuild())) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        Direction face = context.getClickedFace();
        Vec3 target = context.getClickLocation().add(face.getStepX() * 0.25, face.getStepY() * 0.25, face.getStepZ() * 0.25);
        BlockPos cell = BlockPos.containing(target);
        BlockState current = level.getBlockState(cell);
        BlockState[] next = placed(level, part, cell, OctetCellBlock.indexAt(cell, target), player);
        if (next == null) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            boolean wet = current.getBlock() instanceof OctetCellBlock ? OctetCells.wet(current)
                    : level.getFluidState(cell).getType() == Fluids.WATER;
            if (!OctetCells.set(level, cell, next, wet, Block.UPDATE_ALL)) {
                return InteractionResult.FAIL;
            }
            SoundType sound = part.getSoundType();
            level.playSound(null, cell, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 1.2F);
            level.gameEvent(GameEvent.BLOCK_PLACE, cell, GameEvent.Context.of(player, level.getBlockState(cell)));
            ItemStack stack = context.getItemInHand();
            stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Die Teile der Zelle mit dem neuen Achtel ({@link OctetCells#parts}), oder null, wenn es dort nicht hin darf: das
     * Achtel ist belegt, ein Wesen steht darin, oder die Stelle ist weder Achtelzelle noch ersetzbar.
     */
    public static @Nullable BlockState[] placed(Level level, BlockState part, BlockPos cell, int index, @Nullable Player player) {
        if (player != null && !level.mayInteract(player, cell)) {
            return null;
        }
        BlockState current = level.getBlockState(cell);
        BlockState[] parts;
        if (current.getBlock() instanceof OctetCellBlock) {
            parts = OctetCells.parts(level, cell, current);
            if (parts[index] != null) {
                return null;
            }
        } else if (current.canBeReplaced()) {
            parts = new BlockState[8];
        } else {
            return null;
        }
        if (obstructed(level, cell, index)) {
            return null;
        }
        parts[index] = OctetCells.material(part);
        return parts;
    }

    /** Ein Wesen steht im neuen Achtel. */
    private static boolean obstructed(Level level, BlockPos cell, int index) {
        return !level.isUnobstructed(null, OctetCellBlock.box(index).move(cell.getX(), cell.getY(), cell.getZ()));
    }
}

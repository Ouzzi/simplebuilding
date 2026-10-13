package com.simplebuilding.items.custom;

import com.simplebuilding.blocks.custom.OctetCellBlock;
import com.simplebuilding.blocks.custom.OctetCells;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Ein Achtel eines Materials (Holz, Queue Nachtrag 24). Setzt wie die Schach-Achtel ({@link CheckerOctetItem}) ins
 * Sub-Raster: Ziel ist der Punkt ein Viertel Block vor dem Trefferpunkt. Jede Achtelzelle wird ergaenzt (anderes
 * Material: gemischte Zelle, Queue N48), eine leere oder ersetzbare Stelle wird zur neuen Zelle; belegte Achtel lehnen ab. {@link #place}
 * nutzen auch die Melonenscheiben (Schleichen + Rechtsklick, {@code MaterialOctets#tryPlaceSlice}).
 */
public class MaterialOctetItem extends Item {
    private final Supplier<Block> cell;

    public MaterialOctetItem(Supplier<Block> cell, Item.Properties properties) {
        super(properties);
        this.cell = cell;
    }

    /** Die Achtelzelle, in die dieses Item setzt. */
    public Block cell() {
        return this.cell.get();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return place(context, this.cell.get());
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

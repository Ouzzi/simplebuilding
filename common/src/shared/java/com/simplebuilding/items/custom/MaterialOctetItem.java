package com.simplebuilding.items.custom;

import com.simplebuilding.blocks.custom.OctetCellBlock;
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
 * Sub-Raster: Ziel ist der Punkt ein Viertel Block vor dem Trefferpunkt. Eine Zelle desselben Materials wird ergaenzt,
 * eine leere oder ersetzbare Stelle wird zur neuen Zelle; andere Zellen und belegte Achtel lehnen ab. {@link #place}
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
        Player player = context.getPlayer();
        if (block == null || (player != null && !player.mayBuild())) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        Direction face = context.getClickedFace();
        Vec3 target = context.getClickLocation().add(face.getStepX() * 0.25, face.getStepY() * 0.25, face.getStepZ() * 0.25);
        BlockPos cell = BlockPos.containing(target);
        BlockState next = placed(level, block, cell, OctetCellBlock.indexAt(cell, target), player);
        if (next == null) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            if (!level.setBlock(cell, next, Block.UPDATE_ALL)) {
                return InteractionResult.FAIL;
            }
            SoundType sound = next.getSoundType();
            level.playSound(null, cell, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 1.2F);
            level.gameEvent(GameEvent.BLOCK_PLACE, cell, GameEvent.Context.of(player, next));
            ItemStack stack = context.getItemInHand();
            stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }

    /** Der Zustand der Zelle mit dem neuen Achtel, oder null, wenn es dort nicht hin darf. */
    public static @Nullable BlockState placed(Level level, Block block, BlockPos cell, int index, @Nullable Player player) {
        if (player != null && !level.mayInteract(player, cell)) {
            return null;
        }
        BlockState current = level.getBlockState(cell);
        int bit = 1 << index;
        if (current.is(block)) {
            int mask = OctetCellBlock.mask(current);
            if ((mask & bit) != 0) {
                return null;
            }
            return obstructed(level, cell, index) ? null : OctetCellBlock.withMask(current, mask | bit);
        }
        if (!current.canBeReplaced() || obstructed(level, cell, index)) {
            return null;
        }
        BlockState fresh = block.defaultBlockState()
                .setValue(OctetCellBlock.WATERLOGGED, level.getFluidState(cell).getType() == Fluids.WATER);
        return OctetCellBlock.withMask(fresh, bit);
    }

    /** Ein Wesen steht im neuen Achtel. */
    private static boolean obstructed(Level level, BlockPos cell, int index) {
        return !level.isUnobstructed(null, OctetCellBlock.box(index).move(cell.getX(), cell.getY(), cell.getZ()));
    }
}

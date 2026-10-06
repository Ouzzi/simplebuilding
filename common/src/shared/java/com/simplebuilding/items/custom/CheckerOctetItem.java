package com.simplebuilding.items.custom;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.CheckerOctetBlock;
import com.simplebuilding.chess.ChessColor;
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
 * Ein Achtelblock einer Farbe (docs/ai/PLAN-SCHACH-2026-10-06.md). Rechtsklick setzt ihn ins Sub-Raster: Ziel ist der
 * Punkt ein Viertel Block vor dem Trefferpunkt (in Richtung der getroffenen Flaeche) - so landet er neben dem
 * angeklickten Achtel bzw. auf der angeklickten Haelfte einer Blockflaeche. Eine Achtelzelle gleicher Farbe wird
 * ergaenzt, eine leere oder ersetzbare Stelle (Luft, Wasser, Gras) wird zur neuen Zelle; fremde Farben und belegte
 * Achtel lehnen ab.
 */
public class CheckerOctetItem extends Item {
    private final ChessColor color;

    public CheckerOctetItem(ChessColor color, Item.Properties properties) {
        super(properties);
        this.color = color;
    }

    public ChessColor color() {
        return this.color;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Block block = ModBlocks.CHECKER_OCTET;
        Player player = context.getPlayer();
        if (block == null || (player != null && !player.mayBuild())) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        Direction face = context.getClickedFace();
        Vec3 target = context.getClickLocation().add(face.getStepX() * 0.25, face.getStepY() * 0.25, face.getStepZ() * 0.25);
        BlockPos cell = BlockPos.containing(target);
        int index = CheckerOctetBlock.indexAt(cell, target);
        BlockState next = placed(level, cell, index, player);
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
    public @Nullable BlockState placed(Level level, BlockPos cell, int index, @Nullable Player player) {
        Block block = ModBlocks.CHECKER_OCTET;
        if (block == null || (player != null && !level.mayInteract(player, cell))) {
            return null;
        }
        BlockState current = level.getBlockState(cell);
        int bit = 1 << index;
        if (current.is(block)) {
            int mask = CheckerOctetBlock.mask(current);
            if (current.getValue(CheckerOctetBlock.COLOR) != this.color || (mask & bit) != 0) {
                return null;
            }
            return obstructed(level, cell, index) ? null : CheckerOctetBlock.withMask(current, mask | bit);
        }
        if (!current.canBeReplaced() || obstructed(level, cell, index)) {
            return null;
        }
        BlockState fresh = block.defaultBlockState().setValue(CheckerOctetBlock.COLOR, this.color)
                .setValue(CheckerOctetBlock.WATERLOGGED, level.getFluidState(cell).getType() == Fluids.WATER);
        return CheckerOctetBlock.withMask(fresh, bit);
    }

    /** Ein Wesen steht im neuen Achtel. */
    private static boolean obstructed(Level level, BlockPos cell, int index) {
        return !level.isUnobstructed(null, CheckerOctetBlock.box(index).move(cell.getX(), cell.getY(), cell.getZ()));
    }
}

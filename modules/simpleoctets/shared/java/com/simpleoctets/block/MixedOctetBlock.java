package com.simpleoctets.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Achtelzelle aus mehreren Materialien (Queue N48, docs/ai/PLAN-OCTETS-MIX-2026-10-13.md). Bits, Wasser und Licht stehen
 * wie bei den reinen Zellen im Zustand (Form, Kollision, Licht-Verdeckung ohne Block-Entity); das Material je Achtel
 * haelt {@link MixedOctetBlockEntity}. Entsteht nur ueber {@link OctetCells#set}, sobald ein fremdes Achtel in eine Zelle
 * kommt, und wird dort wieder zur reinen Zelle, wenn nur ein Material bleibt.
 *
 * <p>Gezeichnet vom {@code MixedOctetRenderer} mit den gebackenen Modellen der reinen Zellen ({@link RenderShape#INVISIBLE}
 * hier). Abbaugeschwindigkeit, Werkzeug und Drops richten sich nach dem Material des angeschauten Achtels
 * ({@link OctetCellBlock}).
 */
public class MixedOctetBlock extends OctetCellBlock implements EntityBlock {
    /** Leuchtkraft: das Maximum der Materialien (z. B. Astralit-Schachachtel). */
    public static final IntegerProperty LIGHT = IntegerProperty.create("light", 0, 15);

    public MixedOctetBlock(BlockBehaviour.Properties properties) {
        super(properties.lightLevel(state -> state.getValue(LIGHT)));
        this.registerDefaultState(this.defaultBlockState().setValue(LIGHT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIGHT);
        super.createBlockStateDefinition(builder);
    }

    /** Keins: Items kommen je Achtel aus dem Material ({@link OctetCells#item}). */
    @Override
    protected Item octetItem(BlockState state) {
        return Items.AIR;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MixedOctetBlockEntity(pos, state);
    }

    /** Tempo wie das haerteste Material (ganze Zelle); schleichend wie das angeschaute Achtel (Material, Haerte, Werkzeug). */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        BlockState[] parts = OctetCells.parts(level, pos, state);
        if (!player.isShiftKeyDown()) {
            // Normal faellt die ganze Zelle: so langsam wie ihr haertestes Material.
            float slowest = Float.MAX_VALUE;
            for (BlockState part : parts) {
                if (part != null) {
                    slowest = Math.min(slowest, part.getDestroyProgress(player, level, pos));
                }
            }
            return slowest == Float.MAX_VALUE ? super.getDestroyProgress(state, player, level, pos) : slowest;
        }
        BlockState part = parts[OctetCells.lookedAt(player, pos, state)];
        return part == null ? super.getDestroyProgress(state, player, level, pos) : part.getDestroyProgress(player, level, pos);
    }
}

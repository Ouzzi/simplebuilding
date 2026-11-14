package com.simplechess.block;

import com.simplechess.ChessColor;
import com.simplechess.registry.ChessItems;
import com.simpleoctets.block.OctetCellBlock;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * A cell of up to eight octets (0.5 x 0.5 x 0.5) of one colour (docs/ai/PLAN-SCHACH-2026-10-06.md). The octet logic (bits,
 * shape, water, drops, taking out) is Simple Octets' {@link OctetCellBlock}; this adds the colour ({@link #COLOR}). Everything
 * is in the block state, no block entity: the models are baked (multipart per colour and bit). Needs Simple Octets
 * ({@code registry/ChessOctets}).
 *
 * <p>Placed through the octet items ({@link com.simplechess.item.CheckerOctetItem}) by hit point.
 */
public class CheckerOctetBlock extends OctetCellBlock {
    public static final EnumProperty<ChessColor> COLOR = EnumProperty.create("color", ChessColor.class);

    public CheckerOctetBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(COLOR, ChessColor.QUARTZ));
    }

    @Override
    protected Item octetItem(BlockState state) {
        Item item = ChessItems.octet(state.getValue(COLOR));
        return item != null ? item : net.minecraft.world.item.Items.AIR;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COLOR);
        super.createBlockStateDefinition(builder);
    }
}

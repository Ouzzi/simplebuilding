package com.simplebuilding.blocks.custom;

import com.simplebuilding.chess.ChessColor;
import com.simplebuilding.chess.ChessItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Eine Blockzelle aus bis zu acht Achtelbloecken (0,5 x 0,5 x 0,5) einer Farbe (docs/ai/PLAN-SCHACH-2026-10-06.md).
 * Die Achtel-Logik (Bits, Form, Wasser, Drops, Herausnehmen) steht in {@link OctetCellBlock}; hier kommt nur die Farbe
 * dazu ({@link #COLOR}). Alles im Blockzustand, kein Block-Entity: die Modelle sind gebacken (Multipart je Farbe und
 * Bit), auch grosse Bauten kosten nichts beim Zeichnen.
 *
 * <p>Gesetzt wird ueber die Achtel-Items ({@link com.simplebuilding.items.custom.CheckerOctetItem}) nach Trefferpunkt.
 */
public class CheckerOctetBlock extends OctetCellBlock {
    public static final EnumProperty<ChessColor> COLOR = EnumProperty.create("color", ChessColor.class);

    public CheckerOctetBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(COLOR, ChessColor.QUARTZ));
    }

    @Override
    protected Item octetItem(BlockState state) {
        return ChessItems.octet(state.getValue(COLOR));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COLOR);
        super.createBlockStateDefinition(builder);
    }
}

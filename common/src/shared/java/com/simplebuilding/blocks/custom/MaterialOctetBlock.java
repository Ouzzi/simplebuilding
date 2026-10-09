package com.simplebuilding.blocks.custom;

import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Achtelzelle eines Materials (Queue Nachtrag 24): ein Block je Material ({@code oak_octet} ... , {@code melon_octet}),
 * damit Klang, Brennbarkeit und Kartenfarbe vom Material kommen. {@link #source()} ist der volle Block, aus dem die
 * Achtel stammen (Bretter, Melone); der Vorschlaghammer zerlegt dessen Formen in diese Zelle
 * ({@link com.simplebuilding.util.HammerCorners}).
 */
public class MaterialOctetBlock extends OctetCellBlock {
    private final Block source;
    private final Supplier<Item> piece;

    public MaterialOctetBlock(Block source, Supplier<Item> piece, BlockBehaviour.Properties properties) {
        super(properties);
        this.source = source;
        this.piece = piece;
    }

    /** Der volle Block des Materials. */
    public Block source() {
        return this.source;
    }

    /** Das Item eines Achtels (Holz: {@code <holz>_octet}, Melone: Melonenscheibe). */
    public Item piece() {
        return this.piece.get();
    }

    @Override
    protected Item octetItem(BlockState state) {
        return this.piece.get();
    }
}

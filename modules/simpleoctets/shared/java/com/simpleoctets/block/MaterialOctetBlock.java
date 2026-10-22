package com.simpleoctets.block;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Achtelzelle eines Materials (Queue Nachtrag 24): ein Block je Material ({@code oak_octet} ... , {@code melon_octet}),
 * damit Klang, Brennbarkeit und Kartenfarbe vom Material kommen. {@link #source()} ist der volle Block, aus dem die
 * Achtel stammen (Bretter, Melone); der Vorschlaghammer zerlegt dessen Formen in diese Zelle
 * (SimpleBuilding {@code HammerCorners} ueber SimpleLib {@code OctetCells}).
 */
public class MaterialOctetBlock extends OctetCellBlock {
    private final Supplier<Block> source;
    private final Supplier<Item> piece;

    public MaterialOctetBlock(Block source, Supplier<Item> piece, BlockBehaviour.Properties properties) {
        this(() -> source, piece, properties);
    }

    /** Mit spaet gelesener Quelle: Bloecke anderer Mods (Simple-End-Holz), die beim Registrieren noch fehlen koennen. */
    public MaterialOctetBlock(Supplier<Block> source, Supplier<Item> piece, BlockBehaviour.Properties properties) {
        super(properties);
        this.source = source;
        this.piece = piece;
    }

    /** Der volle Block des Materials. */
    public Block source() {
        return this.source.get();
    }

    /** Der Grundzustand des vollen Blocks, null solange er fehlt (Block eines anderen Mods). */
    public @Nullable BlockState sourceState() {
        Block block = source();
        return block == null || block == Blocks.AIR ? null : block.defaultBlockState();
    }

    /**
     * Abbautempo wie der volle Block (Besitzer SP22-1: Vanilla-artig): Haerte, Werkzeug und Werkzeugstufe des Materials -
     * die Zellen tragen nur die mineable-Tags, nicht {@code needs_iron_tool} &amp; Co.
     */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        BlockState full = sourceState();
        return full == null ? super.getDestroyProgress(state, player, level, pos) : full.getDestroyProgress(player, level, pos);
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

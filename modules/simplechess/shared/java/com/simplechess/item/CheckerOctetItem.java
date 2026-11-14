package com.simplechess.item;

import com.simplechess.ChessColor;
import com.simplechess.block.CheckerOctetBlock;
import com.simplechess.registry.ChessOctets;
import com.simpleoctets.block.OctetCells;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * An octet of one colour (docs/ai/PLAN-SCHACH-2026-10-06.md). Right-click puts it into the sub-grid: the target is the point a
 * quarter block in front of the hit point (towards the clicked face) - so it lands next to the clicked octet or on the clicked
 * half of a block face. Every octet cell is completed (other colour or material: a mixed cell), an empty or replaceable spot
 * (air, water, grass) becomes a new cell; taken octets refuse. Placing through Simple Octets' {@link OctetCells#place}.
 */
public class CheckerOctetItem extends Item {
    /** Octets stack to the engine maximum (owner N18: "128"; item stacks are saved with at most 99). */
    public static final int MAX_STACK = 99;

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
        return ChessOctets.cell() == null ? InteractionResult.PASS : OctetCells.place(context, material());
    }

    /** The material state of this octet: the chess cell in its colour. */
    public BlockState material() {
        return ChessOctets.cell().defaultBlockState().setValue(CheckerOctetBlock.COLOR, this.color);
    }

    /** The parts of the cell with the new octet, or null where it may not go ({@link OctetCells#placed}). */
    public @Nullable BlockState[] placed(Level level, BlockPos cell, int index, @Nullable Player player) {
        return ChessOctets.cell() == null ? null : OctetCells.placed(level, material(), cell, index, player);
    }
}

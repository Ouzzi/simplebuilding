package com.simplebuilding.items.custom;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.CheckerOctetBlock;
import com.simplebuilding.chess.ChessColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Ein Achtelblock einer Farbe (docs/ai/PLAN-SCHACH-2026-10-06.md). Rechtsklick setzt ihn ins Sub-Raster: Ziel ist der
 * Punkt ein Viertel Block vor dem Trefferpunkt (in Richtung der getroffenen Flaeche) - so landet er neben dem
 * angeklickten Achtel bzw. auf der angeklickten Haelfte einer Blockflaeche. Jede Achtelzelle wird ergaenzt
 * (andere Farbe oder anderes Material: gemischte Zelle, Queue N48), eine leere oder ersetzbare Stelle (Luft, Wasser,
 * Gras) wird zur neuen Zelle; belegte Achtel lehnen ab. Setzen ueber {@link MaterialOctetItem#place}.
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
        return ModBlocks.CHECKER_OCTET == null ? InteractionResult.PASS : MaterialOctetItem.place(context, material());
    }

    /** Der Materialzustand dieses Achtels: die Schachzelle in seiner Farbe. */
    public BlockState material() {
        return ModBlocks.CHECKER_OCTET.defaultBlockState().setValue(CheckerOctetBlock.COLOR, this.color);
    }

    /** Die Teile der Zelle mit dem neuen Achtel, oder null, wenn es dort nicht hin darf ({@link MaterialOctetItem#placed}). */
    public @Nullable BlockState[] placed(Level level, BlockPos cell, int index, @Nullable Player player) {
        return ModBlocks.CHECKER_OCTET == null ? null : MaterialOctetItem.placed(level, material(), cell, index, player);
    }
}

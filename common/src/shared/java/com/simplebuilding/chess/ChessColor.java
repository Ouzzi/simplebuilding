package com.simplebuilding.chess;

import com.simplebuilding.blocks.ModBlocks;
import java.util.function.Supplier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * Die Farben der Achtelbloecke und Schachfiguren (docs/ai/PLAN-SCHACH-2026-10-06.md): die zwoelf Quarz-Schachbretter
 * (Farbe = ihre Material-Haelfte) und Quarz als helle Seite eines Spiels. Die Reihenfolge ist die der Checker im
 * Kreativtab, Quarz vorn; sie bestimmt auch die Werte der Blockzustands-Eigenschaft {@code color}.
 */
public enum ChessColor implements StringRepresentable {
    QUARTZ("quartz", () -> null, 0),
    PURPUR("purpur", () -> ModBlocks.PURPUR_QUARTZ_CHECKER, 0),
    LAPIS("lapis", () -> ModBlocks.LAPIS_QUARTZ_CHECKER, 0),
    BLACKSTONE("blackstone", () -> ModBlocks.BLACKSTONE_QUARTZ_CHECKER, 0),
    RESIN("resin", () -> ModBlocks.RESIN_QUARTZ_CHECKER, 0),
    NETHER_BRICK("nether_brick", () -> ModBlocks.NETHER_BRICK_QUARTZ_CHECKER, 0),
    RED_NETHER_BRICK("red_nether_brick", () -> ModBlocks.RED_NETHER_BRICK_QUARTZ_CHECKER, 0),
    NIHILITH("nihilith", () -> ModBlocks.NIHILITH_QUARTZ_CHECKER, 0),
    // Astralit leuchtet wie sein Schachbrett mit 5.
    ASTRALIT("astralit", () -> ModBlocks.ASTRALIT_QUARTZ_CHECKER, 5),
    ENDER_QUARTZ("ender_quartz", () -> ModBlocks.ENDER_QUARTZ_CHECKER, 0),
    POLISHED_ASTRALIT("polished_astralit", () -> ModBlocks.POLISHED_ASTRALIT_CHECKER, 5),
    POLISHED_NIHILITH("polished_nihilith", () -> ModBlocks.POLISHED_NIHILITH_CHECKER, 0),
    POLISHED_ENDER_QUARTZ("polished_ender_quartz", () -> ModBlocks.POLISHED_ENDER_QUARTZ_CHECKER, 0);

    private final String id;
    private final Supplier<Block> checker;
    private final int light;

    ChessColor(String id, Supplier<Block> checker, int light) {
        this.id = id;
        this.checker = checker;
        this.light = light;
    }

    public String id() {
        return this.id;
    }

    /** Das Quarz-Schachbrett dieser Farbe; null fuer Quarz (der hat keins). */
    public @Nullable Block checker() {
        return this.checker.get();
    }

    /** Woraus der Steinmetz die Achtel dieser Farbe schneidet: das Schachbrett, bei Quarz der Quarzblock. */
    public ItemLike octetSource() {
        Block checker = checker();
        return checker != null ? checker : Items.QUARTZ_BLOCK;
    }

    /** Lichtstufe der Achtel dieser Farbe (wie ihr Schachbrett). */
    public int light() {
        return this.light;
    }

    @Override
    public String getSerializedName() {
        return this.id;
    }
}

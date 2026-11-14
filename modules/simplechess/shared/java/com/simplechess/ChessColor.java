package com.simplechess;

import com.simplechess.registry.ChessRegistry;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * The colours of the chess octets and pieces (docs/ai/PLAN-SCHACH-2026-10-06.md): the twelve quartz checkers (colour = their
 * material half) and quartz as the light side of a game. The order is the creative-tab order, quartz first; it also fixes the
 * values of the block state property {@code color} (all thirteen always, so saved octet cells stay valid).
 *
 * <p>The six palette colours (Astralit, Nihilith, Ender Quartz and their polished blocks) are made of SimpleBuilding's End
 * palettes: their checker, its stairs and slab, their octets and pieces only exist while {@link #PALETTE_SOURCE} is loaded
 * (b-chess47, N47 M7; the palettes may move to Simple Ores later, M4 - then only this constant changes).
 */
public enum ChessColor implements StringRepresentable {
    QUARTZ("quartz", null, 0, false),
    PURPUR("purpur", "purpur_quartz_checker", 0, false),
    LAPIS("lapis", "lapis_quartz_checker", 0, false),
    BLACKSTONE("blackstone", "blackstone_quartz_checker", 0, false),
    RESIN("resin", "resin_quartz_checker", 0, false),
    NETHER_BRICK("nether_brick", "nether_brick_quartz_checker", 0, false),
    RED_NETHER_BRICK("red_nether_brick", "red_nether_brick_quartz_checker", 0, false),
    NIHILITH("nihilith", "nihilith_quartz_checker", 0, true),
    // Astralit glows like its checker with 5.
    ASTRALIT("astralit", "astralit_quartz_checker", 5, true),
    ENDER_QUARTZ("ender_quartz", "ender_quartz_checker", 0, true),
    POLISHED_ASTRALIT("polished_astralit", "polished_astralit_checker", 5, true),
    POLISHED_NIHILITH("polished_nihilith", "polished_nihilith_checker", 0, true),
    POLISHED_ENDER_QUARTZ("polished_ender_quartz", "polished_ender_quartz_checker", 0, true);

    /** The mod whose End palettes the palette colours are made of (their recipes name its blocks). */
    public static final String PALETTE_SOURCE = "simplebuilding";

    private final String id;
    private final @Nullable String checkerId;
    private final int light;
    private final boolean palette;

    ChessColor(String id, @Nullable String checkerId, int light, boolean palette) {
        this.id = id;
        this.checkerId = checkerId;
        this.light = light;
        this.palette = palette;
    }

    public String id() {
        return this.id;
    }

    /** Path of this colour's checker ({@code simplechess:<path>}); null for quartz (it has none). */
    public @Nullable String checkerId() {
        return this.checkerId;
    }

    /** Whether this colour is made of {@link #PALETTE_SOURCE}'s palettes. */
    public boolean palette() {
        return this.palette;
    }

    /** Whether this colour's checker, octets and pieces are registered (palette colours only with their source). */
    public boolean available() {
        return !this.palette || SimpleChess.isLoaded(PALETTE_SOURCE);
    }

    /** This colour's quartz checker; null for quartz and for a colour that is not {@link #available}. */
    public @Nullable Block checker() {
        return ChessRegistry.checker(this);
    }

    /** What the stonecutter cuts this colour's octets and pieces from: the checker, for quartz the quartz block. */
    public ItemLike octetSource() {
        Block checker = checker();
        return checker != null ? checker : Items.QUARTZ_BLOCK;
    }

    /** Light level of this colour's octets (like its checker). */
    public int light() {
        return this.light;
    }

    @Override
    public String getSerializedName() {
        return this.id;
    }
}

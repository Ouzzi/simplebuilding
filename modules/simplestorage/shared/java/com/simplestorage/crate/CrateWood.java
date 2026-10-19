package com.simplestorage.crate;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;
import org.jspecify.annotations.Nullable;

/**
 * The woods with a crate ({@code <id>_crate}, ids as in SimpleBuilding's woodwork family before split stage 8a): the
 * twelve Vanilla woods, and Ashwood and Duskwood of Simple End only while that mod is loaded. An End wood's crate copies
 * the oak planks' properties with its own map colour (its planks may register after this mod; Fabric does not order the
 * mods' entrypoints). Nether woods do not burn and are no fuel.
 */
public enum CrateWood {
    OAK("oak", Blocks.OAK_PLANKS),
    SPRUCE("spruce", Blocks.SPRUCE_PLANKS),
    BIRCH("birch", Blocks.BIRCH_PLANKS),
    JUNGLE("jungle", Blocks.JUNGLE_PLANKS),
    ACACIA("acacia", Blocks.ACACIA_PLANKS),
    DARK_OAK("dark_oak", Blocks.DARK_OAK_PLANKS),
    MANGROVE("mangrove", Blocks.MANGROVE_PLANKS),
    CHERRY("cherry", Blocks.CHERRY_PLANKS),
    PALE_OAK("pale_oak", Blocks.PALE_OAK_PLANKS),
    CRIMSON("crimson", Blocks.CRIMSON_PLANKS),
    WARPED("warped", Blocks.WARPED_PLANKS),
    BAMBOO("bamboo", Blocks.BAMBOO_PLANKS),
    ASHWOOD("ashwood", MapColor.COLOR_LIGHT_GRAY),
    DUSKWOOD("duskwood", MapColor.TERRACOTTA_LIGHT_GRAY);

    /** The mod that owns the End woods. */
    public static final String SIMPLE_END = "simpleend";

    private final String id;
    private final @Nullable Block planks;
    private final @Nullable MapColor endColor;

    CrateWood(String id, Block planks) {
        this.id = id;
        this.planks = planks;
        this.endColor = null;
    }

    CrateWood(String id, MapColor endColor) {
        this.id = id;
        this.planks = null;
        this.endColor = endColor;
    }

    /** {@code oak}, {@code dark_oak}, {@code ashwood}. */
    public String id() {
        return this.id;
    }

    public String crateId() {
        return this.id + "_crate";
    }

    /** Ashwood and Duskwood (Simple End). */
    public boolean external() {
        return this.planks == null;
    }

    /** Crimson and warped: no fire, no furnace fuel. */
    public boolean nether() {
        return this == CRIMSON || this == WARPED;
    }

    /** The block whose properties the crate copies: the wood's planks, for an End wood the oak planks. */
    public Block template() {
        return this.planks != null ? this.planks : Blocks.OAK_PLANKS;
    }

    /** Map colour of an End wood's crate; null for a Vanilla wood (its planks' own). */
    public @Nullable MapColor endColor() {
        return this.endColor;
    }
}

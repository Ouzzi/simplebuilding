package com.simplebuilding.modules.simplelayers.vault.gen;

/**
 * What the Bedrock Vault generator puts at a position (UNDER21, plan docs/ai/PLAN-B-UNDER21-2026-11-10.md): a block id with
 * optional state properties, MC-free so the round images and the path checks use the same generator. {@code VaultBlocks}
 * turns the id into the BlockState in one place. {@link #open} = a player can stand in it (air, water, thin decoration).
 */
public enum VaultPiece {
    AIR("minecraft:air", true),
    BEDROCK("minecraft:bedrock", false),
    WATER("minecraft:water", true),
    // cladding (the cosmetic layer in front of the bedrock, mineable Vanilla blocks)
    POLISHED_DEEPSLATE("minecraft:polished_deepslate", false),
    DEEPSLATE_TILES("minecraft:deepslate_tiles", false),
    DEEPSLATE_BRICKS("minecraft:deepslate_bricks", false),
    CHISELED_DEEPSLATE("minecraft:chiseled_deepslate", false),
    CALCITE("minecraft:calcite", false),
    SMOOTH_STONE("minecraft:smooth_stone", false),
    POLISHED_TUFF("minecraft:polished_tuff", false),
    TUFF_BRICKS("minecraft:tuff_bricks", false),
    CHISELED_TUFF("minecraft:chiseled_tuff", false),
    CHISELED_TUFF_BRICKS("minecraft:chiseled_tuff_bricks", false),
    DARK_PRISMARINE("minecraft:dark_prismarine", false),
    PRISMARINE_BRICKS("minecraft:prismarine_bricks", false),
    SPRUCE_PLANKS("minecraft:spruce_planks", false),
    STRIPPED_SPRUCE("minecraft:stripped_spruce_log[axis=y]", false),
    // lights
    SEA_LANTERN("minecraft:sea_lantern", false, 15),
    SOUL_LANTERN("minecraft:soul_lantern[hanging=true]", true, 10),
    FROG_LIGHT("minecraft:ochre_froglight[axis=y]", false, 15),
    FROG_SOUND("minecraft:pearlescent_froglight[axis=y]", false, 15),
    FROG_PATH("minecraft:verdant_froglight[axis=y]", false, 15),
    LICHEN("minecraft:glow_lichen[up=true]", true, 7),
    CHAIN("minecraft:iron_chain[axis=y]", true),
    // stairs, railings, furniture
    TILE_STAIRS_N("minecraft:deepslate_tile_stairs[facing=north]", false),
    TILE_STAIRS_S("minecraft:deepslate_tile_stairs[facing=south]", false),
    TUFF_STAIRS_N("minecraft:tuff_brick_stairs[facing=north]", false),
    TUFF_STAIRS_S("minecraft:tuff_brick_stairs[facing=south]", false),
    RAIL("minecraft:deepslate_tile_wall", false),
    TUFF_RAIL("minecraft:tuff_brick_wall", false),
    CHAIR_N("minecraft:spruce_stairs[facing=south]", false),
    CHAIR_S("minecraft:spruce_stairs[facing=north]", false),
    TABLE("minecraft:spruce_trapdoor[half=top]", false),
    SHELF_N("minecraft:chiseled_bookshelf[facing=south]", false),
    SHELF_S("minecraft:chiseled_bookshelf[facing=north]", false),
    SHELF_E("minecraft:chiseled_bookshelf[facing=west]", false),
    SHELF_W("minecraft:chiseled_bookshelf[facing=east]", false),
    LECTERN("minecraft:lectern[facing=south]", false),
    POT("minecraft:decorated_pot", false),
    CANDLES("minecraft:white_candle[candles=3]", true),
    COBWEB("minecraft:cobweb", true),
    PLINTH("minecraft:chiseled_tuff", false),
    // the riddles (Simple Layers blocks, unbreakable)
    GATE_OPEN("simplelayers:vault_gate[open=true]", true, 6),
    GATE_CLOSED("simplelayers:vault_gate[open=false]", false),
    LAMP_OFF("simplelayers:vault_lamp[lit=false]", false),
    LAMP_ON("simplelayers:vault_lamp[lit=true]", false, 15),
    LEVER_N("simplelayers:vault_lever[face=wall,facing=north]", true),
    LEVER_S("simplelayers:vault_lever[face=wall,facing=south]", true),
    LEVER_E("simplelayers:vault_lever[face=wall,facing=east]", true),
    LEVER_W("simplelayers:vault_lever[face=wall,facing=west]", true),
    CRYSTAL_0("simplelayers:chime_crystal[tone=0]", false, 6),
    CRYSTAL_1("simplelayers:chime_crystal[tone=1]", false, 6),
    CRYSTAL_2("simplelayers:chime_crystal[tone=2]", false, 6),
    CRYSTAL_3("simplelayers:chime_crystal[tone=3]", false, 6),
    FRIEZE_0("minecraft:amethyst_block", false),
    FRIEZE_1("minecraft:prismarine_bricks", false),
    FRIEZE_2("minecraft:waxed_copper_block", false),
    FRIEZE_3("minecraft:quartz_block", false),
    PATH_TILE("simplelayers:path_tile[lit=false]", false),
    PEDESTAL_LIGHT("simplelayers:seal_pedestal[part=light]", false),
    PEDESTAL_SOUND("simplelayers:seal_pedestal[part=sound]", false),
    PEDESTAL_PATH("simplelayers:seal_pedestal[part=path]", false),
    SOCKET_LIGHT("simplelayers:seal_socket[part=light]", false),
    SOCKET_SOUND("simplelayers:seal_socket[part=sound]", false),
    SOCKET_PATH("simplelayers:seal_socket[part=path]", false),
    ALTAR("simplelayers:ascent_altar", false, 5),
    SEAL("simplelayers:ascent_seal", false);

    public static final VaultPiece[] CRYSTALS = {CRYSTAL_0, CRYSTAL_1, CRYSTAL_2, CRYSTAL_3};
    public static final VaultPiece[] FRIEZE = {FRIEZE_0, FRIEZE_1, FRIEZE_2, FRIEZE_3};
    public static final VaultPiece[] PEDESTALS = {PEDESTAL_LIGHT, PEDESTAL_SOUND, PEDESTAL_PATH};
    public static final VaultPiece[] SOCKETS = {SOCKET_LIGHT, SOCKET_SOUND, SOCKET_PATH};
    public static final VaultPiece[] MARKERS = {FROG_LIGHT, FROG_SOUND, FROG_PATH};

    /** Block id with optional {@code [key=value,...]}. */
    public final String id;
    /** A player can stand in it (air, water, lantern, lever, thin furniture). */
    public final boolean open;
    /** Block light it gives (for the round images; the game reads the block). */
    public final int light;

    VaultPiece(String id, boolean open) {
        this(id, open, 0);
    }

    VaultPiece(String id, boolean open, int light) {
        this.id = id;
        this.open = open;
        this.light = light;
    }

    /** Whether this is the bedrock shell or the cosmetic cladding (a solid block of the vault itself, not a riddle part). */
    public boolean shell() {
        return !open && ordinal() <= STRIPPED_SPRUCE.ordinal();
    }
}

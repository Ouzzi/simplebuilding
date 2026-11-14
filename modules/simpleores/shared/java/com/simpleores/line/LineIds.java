package com.simpleores.line;

import com.simpleores.SimpleOres;
import net.minecraft.core.registries.Registries;

/** The line's ids as plain data (no block class loads): IdMoves at mod construction, tests. */
public final class LineIds {
    private LineIds() {}

    /**
     * N47-M2: SimpleBuilding's ids of the line move here ({@code simplebuilding:<path>} -> {@code simpleores:<path>}); SimpleBuilding
     * registers the same moves itself (IdMovesHook), IdMoves accepts the identical second registration. Here too, so a world
     * made with SimpleBuilding keeps the line when only Simple Ores is installed.
     */
    public static void registerIdMoves() {
        for (String path : LINE_BLOCKS) {
            com.simplelib.api.IdMoves.move(Registries.BLOCK, net.minecraft.resources.Identifier.fromNamespaceAndPath("simplebuilding", path), SimpleOres.id(path));
        }
        for (String path : LINE_ITEMS) {
            com.simplelib.api.IdMoves.move(Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("simplebuilding", path), SimpleOres.id(path));
        }
    }

    private static final java.util.List<String> LINE_BLOCKS = java.util.List.of("astralit_ore", "nihilith_ore", "dull_astralit_ore", "dull_nihilith_ore",
            "astral_redstone", "nihil_redstone", "bridge_redstone", "astral_redstone_block", "nihil_redstone_block", "bridge_redstone_block",
            "astral_redstone_torch", "astral_redstone_wall_torch", "nihil_redstone_torch", "nihil_redstone_wall_torch", "astralit_lamp", "nihilith_lamp",
            "astralit_end_rod", "nihilith_end_rod", "astral_piston", "nihil_piston", "astral_rail", "nihil_rail");
    /** Every item id of the line (tests: the migration check). */
    public static final java.util.List<String> LINE_ITEMS = java.util.List.of("astralit_ore", "nihilith_ore", "dull_astralit_ore", "dull_nihilith_ore",
            "astralit_dust", "dull_astralit_dust", "nihilith_shard", "dull_nihilith_shard", "astral_redstone", "nihil_redstone", "bridge_redstone",
            "astral_redstone_block", "nihil_redstone_block", "bridge_redstone_block", "astral_redstone_torch", "nihil_redstone_torch", "astralit_lamp",
            "nihilith_lamp", "astralit_end_rod", "nihilith_end_rod", "astral_piston", "nihil_piston", "astral_rail", "nihil_rail");

}

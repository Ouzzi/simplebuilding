package com.simpleores.gear;

import com.simpleores.SimpleOres;
import net.minecraft.core.registries.Registries;

/** The gear's ids as plain data (no class loads of the registry): IdMoves at mod construction, tests. */
public final class GearIds {
    private GearIds() {}

    /** The five poles (block and item). */
    public static final java.util.List<String> POLES = java.util.List.of("iron_pole", "gold_pole", "diamond_pole", "netherite_pole", "enderite_pole");
    /** N77 (owner 16.11.): poles new in Simple Ores (no SimpleBuilding id to move). */
    public static final java.util.List<String> NEW_POLES = java.util.List.of("emerald_pole");
    /** Creative tab order of all poles (behind the copper lightning rod). */
    public static final java.util.List<String> TAB_ORDER = java.util.List.of("iron_pole", "gold_pole", "emerald_pole", "diamond_pole", "netherite_pole",
            "enderite_pole");
    /** Every item id of the group: Enderite tools and armor, the Netherite/Enderite food, the poles. */
    public static final java.util.List<String> ITEMS = java.util.List.of("enderite_sword", "enderite_spear", "enderite_pickaxe", "enderite_axe",
            "enderite_shovel", "enderite_hoe", "enderite_helmet", "enderite_chestplate", "enderite_leggings", "enderite_boots", "enderite_horse_armor",
            "enderite_nautilus_armor", "netherite_carrot", "netherite_apple", "enderite_carrot", "enderite_apple", "enchanted_netherite_apple",
            "enchanted_enderite_apple", "iron_pole", "gold_pole", "diamond_pole", "netherite_pole", "enderite_pole");

    /**
     * N47-M3: SimpleBuilding's ids of the gear move here ({@code simplebuilding:<path>} -> {@code simpleores:<path>}); SimpleBuilding registers the
     * same moves itself (IdMovesHook), IdMoves accepts the identical second registration. Here too, so a world made with SimpleBuilding keeps its
     * gear when only Simple Ores is installed. (The pre-pole {@code *_rod} ids stay SimpleBuilding's chain: {@code iron_rod} -> {@code iron_pole}.)
     */
    public static void registerIdMoves() {
        for (String path : POLES) {
            com.simplelib.api.IdMoves.move(Registries.BLOCK, net.minecraft.resources.Identifier.fromNamespaceAndPath("simplebuilding", path), SimpleOres.id(path));
        }
        for (String path : ITEMS) {
            com.simplelib.api.IdMoves.move(Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("simplebuilding", path), SimpleOres.id(path));
        }
    }
}

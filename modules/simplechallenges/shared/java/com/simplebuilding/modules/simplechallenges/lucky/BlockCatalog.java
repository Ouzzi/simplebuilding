package com.simplebuilding.modules.simplechallenges.lucky;

import com.simplebuilding.modules.simplechallenges.price.BlockValues;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * The blocks a survival player can hold as an item: the pool of the Lucky Block and the material of the Skyline sequences. A block
 * counts when it has its own item of the same name (wall variants, plants-in-pots, fluids and technical blocks have none or another),
 * is not a technical or unbreakable block and is not a variant that is only made by a recipe from another block ({@code waxed_},
 * {@code infested_}). Built from the registry, so blocks of other mods that follow the rule join by themselves.
 */
public final class BlockCatalog {
    /** Technical, unbreakable, not obtainable or one-of-a-kind blocks. */
    private static final Set<String> EXCLUDED = Set.of(
            "bedrock", "barrier", "light", "command_block", "repeating_command_block", "chain_command_block", "structure_block", "structure_void",
            "jigsaw", "test_block", "test_instance_block", "end_portal_frame", "end_portal", "end_gateway", "nether_portal", "reinforced_deepslate",
            "spawner", "dragon_egg", "budding_amethyst", "farmland", "frosted_ice", "petrified_oak_slab", "vault", "trial_spawner");

    public enum Dim { OVERWORLD, NETHER, END }

    private BlockCatalog() {}

    public static String idOf(Block b) {
        return BuiltInRegistries.BLOCK.getKey(b).toString();
    }

    public static boolean includable(Block b) {
        if (b == Blocks.AIR || b.asItem() == Items.AIR) return false;
        Identifier id = BuiltInRegistries.BLOCK.getKey(b);
        if (!id.equals(BuiltInRegistries.ITEM.getKey(b.asItem()))) return false; // wall torches, signs, banners... are the same item as their standing block
        String path = id.getPath();
        return !EXCLUDED.contains(path) && !path.startsWith("waxed_") && !path.startsWith("infested_");
    }

    /** All survival blocks in registry order. */
    public static List<Block> all() {
        List<Block> out = new ArrayList<>();
        for (Block b : BuiltInRegistries.BLOCK) if (includable(b)) out.add(b);
        return out;
    }

    private static final List<String> NETHER = List.of("netherrack", "nether_", "crimson_", "warped_", "soul_sand", "soul_soil", "soul_torch", "soul_lantern", "soul_campfire",
            "basalt", "blackstone", "glowstone", "magma_block", "shroomlight", "ancient_debris", "quartz", "weeping_vines", "twisting_vines", "respawn_anchor", "crying_obsidian");
    private static final List<String> END = List.of("end_stone", "end_rod", "purpur", "chorus", "shulker_box", "dragon_head", "ender_chest");

    /** The dimension whose line carries this block: Nether and End blocks by name, everything else the Overworld. */
    public static Dim dimOf(Block b) {
        String path = BuiltInRegistries.BLOCK.getKey(b).getPath();
        for (String s : END) if (path.contains(s)) return Dim.END;
        if (path.equals("smooth_basalt")) return Dim.OVERWORLD; // grows in amethyst geodes
        for (String s : NETHER) if (path.contains(s)) return Dim.NETHER;
        return Dim.OVERWORLD;
    }

    private static final List<String> HEAD_OVERWORLD = List.of("grass_block", "dirt", "oak_log", "oak_leaves", "sand", "sandstone", "gravel", "cobblestone", "stone", "coal_ore",
            "iron_ore", "copper_ore", "clay", "oak_planks", "crafting_table");
    private static final List<String> HEAD_NETHER = List.of("netherrack", "soul_sand", "nether_quartz_ore", "nether_gold_ore", "glowstone", "magma_block", "basalt", "blackstone");
    private static final List<String> HEAD_END = List.of("end_stone", "end_stone_bricks", "purpur_block", "chorus_flower");

    /**
     * The fixed order of one dimension's line: a short hand-made start, then everything else by rarity value (cheap first, id as the
     * tie-break, so the order never changes between runs), the Overworld ends with Obsidian (the portal block).
     */
    public static List<Block> sequence(Dim dim) {
        List<String> head = switch (dim) {
            case OVERWORLD -> HEAD_OVERWORLD;
            case NETHER -> HEAD_NETHER;
            case END -> HEAD_END;
        };
        BlockValues values = BlockValues.current();
        List<Block> rest = new ArrayList<>();
        for (Block b : all()) if (dimOf(b) == dim) rest.add(b);
        Set<Block> used = new LinkedHashSet<>();
        for (String h : head) {
            Block b = BuiltInRegistries.BLOCK.getValue(Identifier.parse("minecraft:" + h));
            if (b != Blocks.AIR && rest.contains(b)) used.add(b);
        }
        rest.removeAll(used);
        Block obsidian = Blocks.OBSIDIAN;
        if (dim == Dim.OVERWORLD) rest.remove(obsidian);
        rest.sort(Comparator.comparingDouble((Block b) -> values.value(idOf(b))).thenComparing(BlockCatalog::idOf));
        List<Block> out = new ArrayList<>(used);
        out.addAll(rest);
        if (dim == Dim.OVERWORLD) out.add(obsidian);
        return out;
    }
}

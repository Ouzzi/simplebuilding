package com.simplelib.api;

import java.util.Set;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

/**
 * Which {@link SimpleRarity} a mod item gets by its path (owner 06.11., TIP05, docs/ai/PLAN-A-RARITY06-2026-11-06.md).
 * One table for every module: registries call {@link #apply} while they build the item properties, so the rank is the
 * item component {@code simplelib:rarity} like everywhere else. Items not named here keep their Vanilla rarity.
 * <ul>
 *   <li>VOID: the Void and Nihil things (void wings, void vault/keys/potions, Nihil redstone, piston, rail, vault, shard, lamp, end rod, book).</li>
 *   <li>EVIL: the Evil trial things and the Wither and Nether wings (Simple Beacons' Evil things set it themselves).</li>
 *   <li>LEGENDARY: End endgame: Enderite ingot, block, template, tools, armor and everything made of it, End Wings, Ender Heart.</li>
 *   <li>MYTHIC: Astral things (the counterpart of Nihil) and the Strong things of Simple Beacons, Ender Gem.</li>
 *   <li>CORRUPTED: broken things: the cracked diamond and its block.</li>
 *   <li>CURSED: set explicitly where it is meant (Biome Brush, Wither items).</li>
 * </ul>
 */
public final class RarityRules {
    private static final Set<String> VOID = Set.of("nihil_redstone", "nihil_redstone_block", "nihil_redstone_torch", "nihil_piston", "nihil_rail",
            "nihil_vault", "nihilith_shard", "nihilith_lamp", "nihilith_end_rod", "nihilith_book");
    private static final Set<String> ASTRAL = Set.of("astral_redstone", "astral_redstone_block", "astral_redstone_torch", "astral_piston",
            "astral_rail", "astral_vault", "astralit_dust", "astralit_lamp", "astralit_end_rod");
    private static final Set<String> ENDERITE_RAW = Set.of("enderite_nugget", "enderite_scrap");
    private static final Set<String> CORRUPTED = Set.of("cracked_diamond", "cracked_diamond_block");

    private RarityRules() {}

    /** The rank for an item path (no namespace), or null for Vanilla rarity. */
    public static @Nullable SimpleRarity forPath(String path) {
        if (path.equals("biome_brush")) return SimpleRarity.CURSED;
        if (path.equals("ender_gem")) return SimpleRarity.MYTHIC;
        if (path.startsWith("evil_") || path.equals("wither_wings") || path.equals("nether_wings")) return SimpleRarity.EVIL;
        if (path.startsWith("void_") || VOID.contains(path)) return SimpleRarity.VOID;
        if (ASTRAL.contains(path)) return SimpleRarity.MYTHIC;
        if (CORRUPTED.contains(path)) return SimpleRarity.CORRUPTED;
        if (path.equals("end_wings") || path.equals("ender_heart") || path.contains("enderite_") && !ENDERITE_RAW.contains(path)
                && !path.startsWith("raw_") && !path.startsWith("layered_") && !path.startsWith("dull_")) return SimpleRarity.LEGENDARY;
        return null;
    }

    /** Item properties for {@code path}: the ranked ones through {@link SimpleRarity#apply}, others untouched. */
    public static Item.Properties apply(String path, Item.Properties properties) {
        SimpleRarity rarity = forPath(path);
        return rarity == null ? properties : rarity.apply(properties);
    }
}

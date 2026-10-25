package com.simplebuilding.tweaks.item;

import com.simplebuilding.tweaks.block.TweaksBlocks;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.level.ItemLike;

/**
 * Infoseiten fuer JEI, ohne JEI-Abhaengigkeit: Familie -> Gegenstaende. Der JEI-Plugin zeigt fuer jede
 * Familie den Text {@code jei.simplebuilding.info.<familie>} an allen ihren Gegenstaenden.
 */
public final class TweaksJeiInfo {
    public static final String KEY_PREFIX = "jei.simplebuilding.info.";

    private TweaksJeiInfo() {
    }

    public static Map<String, List<ItemLike>> families() {
        Map<String, List<ItemLike>> map = new LinkedHashMap<>();
        map.put("elytra_pad", List.of(TweaksBlocks.ELYTRA_PAD, TweaksBlocks.NETHERITE_ELYTRA_PAD, TweaksBlocks.ENDERITE_ELYTRA_PAD));
        map.put("flypad", List.of(TweaksBlocks.FLYPAD, TweaksBlocks.REINFORCED_FLYPAD, TweaksBlocks.STELLAR_FLYPAD));
        map.put("spawn_teleporter", List.of(TweaksBlocks.SPAWN_TELEPORTER, TweaksBlocks.SPAWN_TELEPORTER_TIER_2,
                TweaksBlocks.ENDERITE_SPAWN_TELEPORTER));
        map.put("chunk_loader", List.of(TweaksBlocks.CHUNK_LOADER, TweaksBlocks.NETHERITE_CHUNK_LOADER, TweaksBlocks.ENDERITE_CHUNK_LOADER));
        map.put("launchpad", List.of(TweaksBlocks.LAUNCHPAD, TweaksBlocks.NETHERITE_LAUNCHPAD, TweaksBlocks.ENDERITE_LAUNCHPAD));
        map.put("potion_pad", List.of(TweaksBlocks.POTION_PAD, TweaksBlocks.REINFORCED_POTION_PAD, TweaksBlocks.INFUSED_POTION_PAD));
        // 26.3: the heads are Simple Mobheads' items, which bring their own JEI pages.
        if (!com.simplebuilding.version.McVersion.HEADS_IN_MOBHEADS) {
            map.put("blaze_head", List.of(TweaksItems.head(com.simplebuilding.tweaks.block.BlazeHeadType.BLAZE)));
            map.put("enderman_head", List.of(TweaksItems.head(com.simplebuilding.tweaks.block.BlazeHeadType.ENDERMAN)));
            // Mob-Koepfe seit 2026-09-29 (docs/MOBKOEPFE.md); die geheimen Faehigkeiten verraten die Seiten nicht.
            map.put("trial_chamber_head", List.of(TweaksItems.head(com.simplebuilding.tweaks.block.BlazeHeadType.HUSK), TweaksItems.head(com.simplebuilding.tweaks.block.BlazeHeadType.SPIDER), TweaksItems.head(com.simplebuilding.tweaks.block.BlazeHeadType.CAVE_SPIDER),
                    TweaksItems.head(com.simplebuilding.tweaks.block.BlazeHeadType.STRAY), TweaksItems.head(com.simplebuilding.tweaks.block.BlazeHeadType.BOGGED), TweaksItems.head(com.simplebuilding.tweaks.block.BlazeHeadType.SLIME), TweaksItems.head(com.simplebuilding.tweaks.block.BlazeHeadType.SILVERFISH),
                    TweaksItems.head(com.simplebuilding.tweaks.block.BlazeHeadType.BREEZE)));
            map.put("shulker_head", List.of(TweaksItems.head(com.simplebuilding.tweaks.block.BlazeHeadType.SHULKER)));
            map.put("drowned_head", List.of(TweaksItems.head(com.simplebuilding.tweaks.block.BlazeHeadType.DROWNED)));
        }
        map.put("diamond_pressure_plate", List.of(TweaksBlocks.DIAMOND_PRESSURE_PLATE));
        map.put("netherite_pressure_plate", List.of(TweaksBlocks.NETHERITE_PRESSURE_PLATE));
        map.put("enderite_pressure_plate", List.of(TweaksBlocks.ENDERITE_PRESSURE_PLATE));
        map.put("copper_pressure_plate", List.of(TweaksBlocks.COPPER_PRESSURE_PLATE, TweaksBlocks.EXPOSED_COPPER_PRESSURE_PLATE,
                TweaksBlocks.WEATHERED_COPPER_PRESSURE_PLATE, TweaksBlocks.OXIDIZED_COPPER_PRESSURE_PLATE,
                TweaksBlocks.WAXED_COPPER_PRESSURE_PLATE, TweaksBlocks.WAXED_EXPOSED_COPPER_PRESSURE_PLATE,
                TweaksBlocks.WAXED_WEATHERED_COPPER_PRESSURE_PLATE, TweaksBlocks.WAXED_OXIDIZED_COPPER_PRESSURE_PLATE));
        map.put("spawn_elytra", List.of(TweaksItems.SPAWN_ELYTRA));
        if (TweaksItems.LASER_POINTER != null) map.put("amethyst_lens", List.of(TweaksItems.LASER_POINTER)); // 26.3: Simple Gadgets' rod
        map.put("echo_sounder", List.of(TweaksItems.ECHO_COMPASS));
        return map;
    }
}

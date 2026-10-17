package com.simplecrucibles.compat;

import com.simplecrucibles.api.SimpleCruciblesApi;
import com.simplecrucibles.fluid.CrucibleFluids;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What the optional JEI and Jade plugins of Simple Crucibles show, without any JEI or Jade class (split stage 5b: these
 * pages were SimpleBuilding's), so the server GameTests can check it:
 * <ul>
 *   <li>Jade, crucible: heat, occupied slots and the shortest remaining time of a running slot - from the server
 *       ({@link #crucibleStatus}, sent as an int array under {@link #CRUCIBLE_DATA}), turned into lines on the client
 *       ({@link #crucibleLines});</li>
 *   <li>Jade, reinforced cauldron: its content and, for water and powder snow, the fill level, from the block state
 *       ({@link #cauldronLines});</li>
 *   <li>JEI: the info pages of the items without a JEI-visible recipe ({@link #infoPages}).</li>
 * </ul>
 */
public final class CrucibleInfo {
    /** Key of the crucible status in Jade's server data. */
    public static final String CRUCIBLE_DATA = "simplecrucibles:crucible";
    /** Fill levels of the reinforced cauldron (without a level property it always holds a full bucket). */
    public static final int CAULDRON_LEVELS = 3;
    public static final String INFO_PREFIX = "jei.simplecrucibles.info.";

    private CrucibleInfo() {
    }

    /** Whether {@code be} is a crucible (Jade's server provider only answers for those). */
    public static boolean isCrucible(BlockEntity be) {
        return SimpleCruciblesApi.crucibleSlots(be.getBlockState().getBlock()) > 0;
    }

    /** {heat 0-3, occupied slots, slots, shortest remaining ticks or -1}; empty for anything that is no crucible. */
    public static int[] crucibleStatus(BlockEntity be) {
        return isCrucible(be) ? SimpleCruciblesApi.crucibleStatus(be) : new int[0];
    }

    /** The Jade lines of a crucible status ({@link #crucibleStatus}); seconds rounded up. */
    public static List<Component> crucibleLines(int[] status) {
        List<Component> lines = new ArrayList<>();
        if (status.length != 4) return lines;
        lines.add(Component.translatable("jade.simplecrucibles.crucible.heat", Component.translatable("crucible.simplecrucibles.heat." + status[0])));
        lines.add(Component.translatable("jade.simplecrucibles.crucible.slots", status[1], status[2]));
        if (status[3] >= 0) lines.add(Component.translatable("jade.simplecrucibles.crucible.remaining", (status[3] + 19L) / 20));
        return lines;
    }

    /** Content and fill level of a reinforced cauldron state; empty for any other block. */
    public static List<Component> cauldronLines(BlockState state) {
        List<Component> lines = new ArrayList<>();
        String content = SimpleCruciblesApi.cauldronContent(state);
        if (content == null) return lines;
        lines.add(Component.translatable("jade.simplecrucibles.cauldron.content", Component.translatable("jade.simplecrucibles.cauldron.content." + content)));
        if (content.equals("water") || content.equals("powder_snow")) {
            var property = state.getBlock().getStateDefinition().getProperty("level");
            int level = property == null ? CAULDRON_LEVELS : Integer.parseInt(levelName(state, property));
            lines.add(Component.translatable("jade.simplecrucibles.cauldron.level", level, CAULDRON_LEVELS));
        }
        return lines;
    }

    private static <T extends Comparable<T>> String levelName(BlockState state, net.minecraft.world.level.block.state.properties.Property<T> property) {
        return property.getName(state.getValue(property));
    }

    /** JEI info pages: page id (lang key {@link #INFO_PREFIX}id) -> the items that show it. Call after registration. */
    public static Map<String, List<ItemLike>> infoPages() {
        Map<String, List<ItemLike>> map = new LinkedHashMap<>();
        // Filled buckets only come from scooping (no recipe); the empty ones get a usage page next to their recipe.
        map.put("soul_lava_bucket", List.of(CrucibleFluids.SOUL_LAVA_BUCKET, CrucibleFluids.ENDERITE_SOUL_LAVA_BUCKET,
                CrucibleFluids.FULL_ENDERITE_SOUL_LAVA_BUCKET));
        map.put("copper_bucket_filled", List.of(CrucibleFluids.COPPER_WATER_BUCKET, CrucibleFluids.COPPER_LAVA_BUCKET));
        map.put("enderite_bucket_filled", List.of(CrucibleFluids.ENDERITE_WATER_BUCKET, CrucibleFluids.ENDERITE_LAVA_BUCKET,
                CrucibleFluids.FULL_ENDERITE_WATER_BUCKET, CrucibleFluids.FULL_ENDERITE_LAVA_BUCKET,
                CrucibleFluids.ENDERITE_MILK_BUCKET, CrucibleFluids.FULL_ENDERITE_MILK_BUCKET));
        map.put("ceramic_bucket_filled", List.of(CrucibleFluids.CERAMIC_WATER_BUCKET, CrucibleFluids.CERAMIC_LAVA_BUCKET));
        // N12b: worn stages only come from use.
        map.put("ceramic_bucket_worn", List.<ItemLike>copyOf(CrucibleFluids.wornCeramicBuckets()));
        map.put("copper_bucket", List.of(CrucibleFluids.COPPER_BUCKET));
        map.put("enderite_bucket", List.of(CrucibleFluids.ENDERITE_BUCKET));
        map.put("ceramic_bucket", List.of(CrucibleFluids.RAW_CERAMIC_BUCKET, CrucibleFluids.CERAMIC_BUCKET));
        // The Enderite crucible and barrel have no crafting recipe: they are upgraded in place.
        map.put("enderite_crucible", List.of(com.simplecrucibles.registry.CrucibleItems.ENDERITE_CRUCIBLE,
                com.simplecrucibles.registry.CrucibleItems.ENDERITE_BARREL));
        return map;
    }
}

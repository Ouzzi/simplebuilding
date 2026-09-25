package com.simplebuilding.blueprint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * Was ein Bauwerk kostet: welches Item fuer einen Blockzustand verbraucht wird, und die
 * Materialliste (Item, Menge) nach Menge sortiert.
 *
 * <p>Regeln: ein Block kostet ein Stueck seines Items ({@link Block#asItem()}); eine doppelte Stufe
 * kostet zwei; die obere Haelfte zweiteiliger Bloecke (Tuer, hohe Pflanze) und das Kopfteil eines
 * Bettes kosten nichts, weil sie mit ihrem Gegenstueck kommen. Zustaende ohne Item (Wasser,
 * Feuer, Kolbenkopf ...) kann nur der Kreativmodus setzen - sie stehen als {@link Items#AIR} in
 * der Liste.
 */
public final class BlueprintMaterials {
    private BlueprintMaterials() {
    }

    /** Preis eines Zustands: {@code count} Stueck von {@code item}; {@code item == AIR} = nur Kreativ. */
    public record Cost(Item item, int count) {
        public boolean creativeOnly() {
            return item == Items.AIR;
        }
    }

    public record Entry(Item item, Block block, int count) {
    }

    public static Cost cost(BlockState state) {
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
            return new Cost(state.getBlock().asItem(), 0);
        }
        if (state.hasProperty(BlockStateProperties.BED_PART)
                && state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD) {
            return new Cost(state.getBlock().asItem(), 0);
        }
        Item item = state.getBlock().asItem();
        if (item == Items.AIR) {
            return new Cost(Items.AIR, 1);
        }
        if (state.hasProperty(BlockStateProperties.SLAB_TYPE)
                && state.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE) {
            return new Cost(item, 2);
        }
        return new Cost(item, 1);
    }

    /**
     * Materialliste des Bauwerks, groesste Menge zuerst (bei Gleichstand nach Item-Id). Reine
     * Kreativ-Zustaende erscheinen je Block als eigener Eintrag mit {@code item == AIR}.
     */
    public static List<Entry> list(BlueprintModel model) {
        Map<Item, Integer> counts = new HashMap<>();
        Map<Block, Integer> creativeOnly = new HashMap<>();
        for (BlockState state : model.blocks().values()) {
            Cost cost = cost(state);
            if (cost.creativeOnly()) {
                creativeOnly.merge(state.getBlock(), 1, Integer::sum);
            } else if (cost.count() > 0) {
                counts.merge(cost.item(), cost.count(), Integer::sum);
            }
        }
        List<Entry> out = new ArrayList<>();
        counts.forEach((item, n) -> out.add(new Entry(item, null, n)));
        creativeOnly.forEach((block, n) -> out.add(new Entry(Items.AIR, block, n)));
        out.sort(Comparator.<Entry>comparingInt(e -> -e.count()).thenComparing(BlueprintMaterials::sortName));
        return out;
    }

    private static String sortName(Entry e) {
        return e.block() != null
                ? "~" + BuiltInRegistries.BLOCK.getKey(e.block())
                : BuiltInRegistries.ITEM.getKey(e.item()).toString();
    }

    /** Summe aller Items, die das Bauwerk im Ueberlebensmodus kostet. */
    public static int totalItems(BlueprintModel model) {
        int total = 0;
        for (Entry e : list(model)) {
            if (e.block() == null) {
                total += e.count();
            }
        }
        return total;
    }
}

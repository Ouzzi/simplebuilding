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
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * Was ein Bauwerk kostet: welches Item fuer einen Blockzustand verbraucht wird, und die
 * Materialliste (Item, Menge) nach Menge sortiert.
 *
 * <p>Regeln: ein Block kostet ein Stueck seines Items ({@link Block#asItem()}); eine doppelte Stufe
 * kostet zwei, ein Block mit Mengen-Eigenschaft so viele, wie sie zaehlt (vier Kerzen = vier Kerzen,
 * drei Seegurken, fuenf Schneeschichten ...), ein Mehrflaechen-Block eines je Flaeche
 * ({@link #itemsPerBlock}); die obere Haelfte zweiteiliger Bloecke (Tuer, hohe Pflanze) und das Kopfteil eines
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
        return new Cost(item, itemsPerBlock(state));
    }

    /**
     * Mengen-Eigenschaften: so viele Stueck des Items stecken in <b>einem</b> Blockzustand, weil man
     * den Block durch weiteres Setzen desselben Items auffuellt (Kerze auf Kerze, Seegurke auf
     * Seegurke). Allgemeine Regel statt Blockliste: jede dieser Eigenschaften zaehlt, egal an
     * welchem Block sie haengt - ein neuer Block mit {@code candles} kostet damit von selbst richtig.
     */
    private static final List<IntegerProperty> COUNT_PROPERTIES = List.of(
            BlockStateProperties.CANDLES,          // Kerzen 1..4
            BlockStateProperties.PICKLES,          // Seegurken 1..4
            BlockStateProperties.EGGS,             // Schildkroeteneier 1..4
            BlockStateProperties.LAYERS,           // Schneeschichten 1..8
            BlockStateProperties.FLOWER_AMOUNT,    // Rosa Blueten, Wildblumen 1..4
            BlockStateProperties.SEGMENT_AMOUNT);  // Laubstreu 1..4

    /**
     * Wie viele Items ein Zustand kostet (ohne die kostenlosen Gegenstuecke, siehe {@link #cost}):
     * doppelte Stufe 2; eine Mengen-Eigenschaft ({@link #COUNT_PROPERTIES}) ihren Wert; ein
     * Mehrflaechen-Block (Leuchtflechte, Sculkader, Harzklumpen, Ranken) eines je belegter Flaeche,
     * weil jedes Stueck genau eine Flaeche belegt; sonst 1.
     */
    static int itemsPerBlock(BlockState state) {
        if (state.hasProperty(BlockStateProperties.SLAB_TYPE)
                && state.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE) {
            return 2;
        }
        for (IntegerProperty property : COUNT_PROPERTIES) {
            if (state.hasProperty(property)) {
                return Math.max(1, state.getValue(property));
            }
        }
        if (state.getBlock() instanceof MultifaceBlock) {
            return Math.max(1, MultifaceBlock.availableFaces(state).size());
        }
        if (state.getBlock() instanceof VineBlock) {
            int faces = 0;
            for (BooleanProperty face : VineBlock.PROPERTY_BY_DIRECTION.values()) {
                if (state.hasProperty(face) && state.getValue(face)) {
                    faces++;
                }
            }
            return Math.max(1, faces);
        }
        return 1;
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

    /**
     * Eigenschaften, die ein Ueberlebens-Bau aus dem Code uebernimmt: Ausrichtung und Form (was ein
     * Spieler beim Setzen selbst bestimmt oder was sich aus den Nachbarn ergibt) und die bezahlten
     * Mengen ({@link #COUNT_PROPERTIES}, Flaechen von Ranken und Flechten). Alles andere - Wachstum
     * ({@code age}), Fuellstaende ({@code level}, {@code honey_level}, {@code charges}), eingesetzte
     * Dinge ({@code eye}, {@code has_book}, {@code berries} ...), {@code waterlogged} - faellt auf den
     * Grundzustand des Blocks zurueck, sonst waere ein Nether-Warzen-Feld mit {@code age=3} oder ein
     * voller Komposter fuer ein Item zu haben (Audit 2026-09-26 #2). Das Instrument eines Notenblocks
     * bleibt wie im Code (sonst wurde jeder zur Harfe, Nach-Audit 2026-09-27 N15).
     */
    private static final java.util.Set<net.minecraft.world.level.block.state.properties.Property<?>> SURVIVAL_KEPT = java.util.Set.of(
            BlockStateProperties.FACING, BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.FACING_HOPPER,
            BlockStateProperties.VERTICAL_DIRECTION, BlockStateProperties.AXIS, BlockStateProperties.HORIZONTAL_AXIS,
            BlockStateProperties.ROTATION_16, BlockStateProperties.ORIENTATION, BlockStateProperties.ATTACH_FACE,
            BlockStateProperties.BELL_ATTACHMENT, BlockStateProperties.HALF, BlockStateProperties.DOUBLE_BLOCK_HALF,
            BlockStateProperties.BED_PART, BlockStateProperties.SLAB_TYPE, BlockStateProperties.STAIRS_SHAPE,
            BlockStateProperties.RAIL_SHAPE, BlockStateProperties.RAIL_SHAPE_STRAIGHT, BlockStateProperties.DOOR_HINGE,
            BlockStateProperties.CHEST_TYPE, BlockStateProperties.OPEN, BlockStateProperties.HANGING,
            BlockStateProperties.ATTACHED, BlockStateProperties.IN_WALL,
            BlockStateProperties.NORTH, BlockStateProperties.EAST, BlockStateProperties.SOUTH, BlockStateProperties.WEST,
            BlockStateProperties.UP, BlockStateProperties.DOWN,
            BlockStateProperties.NORTH_WALL, BlockStateProperties.EAST_WALL, BlockStateProperties.SOUTH_WALL, BlockStateProperties.WEST_WALL,
            BlockStateProperties.NORTH_REDSTONE, BlockStateProperties.EAST_REDSTONE, BlockStateProperties.SOUTH_REDSTONE,
            BlockStateProperties.WEST_REDSTONE, BlockStateProperties.MODE_COMPARATOR, BlockStateProperties.DELAY,
            BlockStateProperties.NOTE, BlockStateProperties.NOTEBLOCK_INSTRUMENT, BlockStateProperties.INVERTED,
            BlockStateProperties.DISTANCE,
            BlockStateProperties.STABILITY_DISTANCE, BlockStateProperties.BOTTOM, BlockStateProperties.SNOWY,
            BlockStateProperties.CANDLES, BlockStateProperties.PICKLES, BlockStateProperties.EGGS, BlockStateProperties.LAYERS,
            BlockStateProperties.FLOWER_AMOUNT, BlockStateProperties.SEGMENT_AMOUNT);

    /**
     * Der Zustand, den ein Ueberlebens-Bau fuer {@code state} setzt: der Block im Grundzustand mit den
     * Eigenschaften aus {@link #SURVIVAL_KEPT}; Laub bleibt dauerhaft (wie von Hand gesetzt). Ein
     * Zustand eines Blocks, den sein Item gar nicht setzt (gefuellter Kessel -&gt; Kessel), wird zu dem
     * Block, den das Item setzt - Wand-Varianten (Fackel, Schild, Kopf) bleiben. Der Kreativmodus
     * setzt den Code unveraendert.
     */
    public static BlockState survivalState(BlockState state) {
        Block block = state.getBlock();
        if (block.asItem() instanceof net.minecraft.world.item.BlockItem blockItem && blockItem.getBlock() != block
                && !(blockItem instanceof net.minecraft.world.item.StandingAndWallBlockItem)) {
            block = blockItem.getBlock();
        }
        BlockState out = block.defaultBlockState();
        for (net.minecraft.world.level.block.state.properties.Property<?> property : state.getProperties()) {
            if (SURVIVAL_KEPT.contains(property) && out.hasProperty(property)) {
                out = copy(state, out, property);
            }
        }
        if (out.hasProperty(BlockStateProperties.PERSISTENT)) {
            out = out.setValue(BlockStateProperties.PERSISTENT, true);
        }
        return out;
    }

    private static <T extends Comparable<T>> BlockState copy(BlockState from, BlockState to,
                                                             net.minecraft.world.level.block.state.properties.Property<T> property) {
        return to.setValue(property, from.getValue(property));
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

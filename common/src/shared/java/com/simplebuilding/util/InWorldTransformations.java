package com.simplebuilding.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.ChiselItem;
import com.simplebuilding.items.custom.SledgehammerItem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Beschreibt die Umwandlungen in der Welt - Maschinen-Aufwertung mit dem Vorschlaghammer,
 * Umformen, Diamantblock zerschlagen, Meissel und Spachtel, Schere auf Wolle - als JSON fuer das Wiki.
 *
 * <p>Der Datagen-Provider {@code WikiDataProvider} schreibt das Ergebnis nach
 * {@code src/main/generated/wiki/inworld.json}; {@code wiki/generate.py} macht daraus die
 * Kategorie "Umwandlung in der Welt". Alle Zahlen und Tabellen kommen aus denselben Konstanten
 * und Tabellen, die das Spiel benutzt ({@link SledgehammerUpgrades}, {@link SledgehammerItem},
 * {@link ChiselItem}); nichts ist hier abgeschrieben.
 *
 * <p>Kommt ohne {@code ItemStack} aus: im Datagen der 26.2-Linie liest dessen Konstruktor noch
 * nicht gebundene Komponenten.
 */
public final class InWorldTransformations {

    private InWorldTransformations() {
    }

    /** Die ganze Beschreibung. */
    public static JsonObject describe() {
        JsonObject root = new JsonObject();
        root.add("sledgehammerUpgrade", sledgehammerUpgrade());
        root.add("sledgehammerReshape", sledgehammerReshape());
        root.add("diamondCrush", diamondCrush());
        root.add("chisel", chisel());
        root.add("shearWool", shearWool());
        return root;
    }

    // =====================================================================================

    /** Verstaerkt -> Netherit -> Enderit: Stufen, Mindesthammer, Dauer, Schlaege, Haltbarkeit. */
    public static JsonObject sledgehammerUpgrade() {
        List<Item> hammers = modItems(SledgehammerItem.class);
        int hits = SledgehammerUpgrades.UPGRADE_TICKS / SledgehammerUpgrades.HIT_INTERVAL;

        JsonArray steps = new JsonArray();
        for (Block block : modBlocks()) {
            SledgehammerUpgrades.Upgrade upgrade = SledgehammerUpgrades.upgradeOf(block);
            if (upgrade == null) {
                continue;
            }
            JsonObject step = new JsonObject();
            step.addProperty("from", id(upgrade.from()));
            step.addProperty("to", id(upgrade.to()));
            step.addProperty("nugget", id(upgrade.nugget()));
            // SledgehammerUpgrades.finish verbraucht genau einen Nugget.
            step.addProperty("nuggetCount", 1);
            step.addProperty("minimumHammer", weakestHammer(hammers, upgrade.minHammerRank()));
            step.addProperty("damagePerHit", upgrade.damagePerHit());
            step.addProperty("totalDamage", upgrade.damagePerHit() * hits);
            steps.add(step);
        }

        JsonArray ranks = new JsonArray();
        for (Item hammer : hammers) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", id(hammer));
            entry.addProperty("rank", SledgehammerUpgrades.hammerRank(hammer));
            ranks.add(entry);
        }

        JsonObject o = new JsonObject();
        o.addProperty("durationTicks", SledgehammerUpgrades.UPGRADE_TICKS);
        o.addProperty("hitIntervalTicks", SledgehammerUpgrades.HIT_INTERVAL);
        o.addProperty("hits", hits);
        o.addProperty("finishCooldownTicks", SledgehammerUpgrades.FINISH_COOLDOWN_TICKS);
        o.add("hammers", ranks);
        o.add("steps", steps);
        return o;
    }

    /** Der schwaechste Hammer, dessen Rang reicht, oder null, wenn keiner reicht. */
    private static String weakestHammer(List<Item> hammers, int minimumRank) {
        Item best = null;
        int bestRank = Integer.MAX_VALUE;
        for (Item hammer : hammers) {
            int rank = SledgehammerUpgrades.hammerRank(hammer);
            if (rank >= minimumRank && rank < bestRank) {
                best = hammer;
                bestRank = rank;
            }
        }
        return best == null ? null : id(best);
    }

    /** Block -> Treppe -> Stufe: Ladezeit je Hammer ohne Effizienz und die Haltbarkeit. */
    public static JsonObject sledgehammerReshape() {
        JsonArray hammers = new JsonArray();
        for (Item item : modItems(SledgehammerItem.class)) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", id(item));
            entry.addProperty("ticks", SledgehammerItem.reshapeTicks(((SledgehammerItem) item).getMaterial().speed(), 0));
            hammers.add(entry);
        }
        JsonObject o = new JsonObject();
        o.addProperty("damage", SledgehammerItem.RESHAPE_DAMAGE);
        o.addProperty("reverseDamage", SledgehammerItem.RESHAPE_REVERSE_DAMAGE);
        o.addProperty("minTicks", SledgehammerItem.RESHAPE_MIN_TICKS);
        o.addProperty("maxTicks", SledgehammerItem.RESHAPE_MAX_TICKS);
        o.add("hammers", hammers);
        return o;
    }

    /**
     * Die Umform-Paare des Vorschlaghammers als {@code [von, nach]}, vorwaerts (Block -&gt; Treppe
     * -&gt; Stufe) oder rueckwaerts (Schleichen + Constructor's Touch), nach derselben Namensregel
     * wie im Spiel ({@link SledgehammerItem#reshapeTarget}). Ueber alle registrierten Bloecke, also
     * auch die anderer Mods. Nicht Teil von {@link #describe()} - das Wiki nennt fuers Umformen nur
     * die Zahlen; der JEI-Katalog ({@code InWorldRecipeCatalog}) liest die Paare hier.
     *
     * <p>Vorwaerts wird ein Block nur dann zur Treppe, wenn er ein voller Kollisionswuerfel ist; das
     * Spiel fragt den Zustand in der Welt, hier zaehlt der Grundzustand.
     */
    public static List<Block[]> reshapePairs(boolean reverse) {
        List<Block[]> out = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            boolean full = !reverse && isFullBlock(block);
            SledgehammerItem.reshapeTarget(block, reverse, full).ifPresent(target -> out.add(new Block[]{block, target}));
        }
        out.sort(Comparator.<Block[], String>comparing(p -> id(p[0])).thenComparing(p -> id(p[1])));
        return out;
    }

    private static boolean isFullBlock(Block block) {
        try {
            return block.defaultBlockState().isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        } catch (RuntimeException e) {
            // Ein Fremdblock, dessen Form eine echte Welt braucht, ist fuer den Katalog kein voller Block.
            return false;
        }
    }

    /** Diamantblock mit dem Vorschlaghammer zerschlagen. */
    public static JsonObject diamondCrush() {
        JsonObject o = new JsonObject();
        o.addProperty("block", id(Blocks.DIAMOND_BLOCK));
        o.addProperty("result", id(ModItems.DIAMOND_PEBBLE));
        o.addProperty("count", SledgehammerItem.DIAMOND_BLOCK_PEBBLES);
        o.addProperty("damage", SledgehammerItem.DIAMOND_CRUSH_DAMAGE);
        return o;
    }

    /**
     * Schere auf Wolle ({@link ShearsWoolInteraction}). Das Spiel fragt den Tag
     * {@code minecraft:wool}; im Datagen sind Tags noch nicht gebunden, deshalb stehen die sechzehn
     * Farben hier ueber {@link DyeColor} - dass jede davon im Tag steht, prueft der Spieltest.
     */
    public static JsonObject shearWool() {
        JsonArray blocks = new JsonArray();
        for (Block block : woolBlocks()) {
            blocks.add(id(block));
        }
        JsonObject o = new JsonObject();
        o.addProperty("tag", "minecraft:wool");
        o.add("blocks", blocks);
        o.addProperty("tool", id(Items.SHEARS));
        o.addProperty("result", id(Items.STRING));
        o.addProperty("count", ShearsWoolInteraction.STRING_PER_WOOL);
        o.addProperty("damage", ShearsWoolInteraction.SHEARS_DAMAGE);
        return o;
    }

    /** Die sechzehn Wollbloecke in {@link DyeColor}-Reihenfolge. */
    public static List<Block> woolBlocks() {
        List<Block> out = new ArrayList<>();
        for (DyeColor color : DyeColor.values()) {
            out.add(BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(color.getSerializedName() + "_wool")));
        }
        return out;
    }

    /**
     * Meissel und Spachtel: je Werkzeug Abklingzeit und ein Verweis auf seine Tabelle. Werkzeuge
     * derselben Stufe teilen sich die Tabellen-Objekte, deshalb stehen sie nur einmal da.
     */
    public static JsonObject chisel() {
        Map<Map<Block, Block>, Integer> tableIndex = new IdentityHashMap<>();
        JsonArray tables = new JsonArray();
        JsonArray tools = new JsonArray();
        for (Item item : modItems(ChiselItem.class)) {
            ChiselItem chisel = (ChiselItem) item;
            Map<Block, Block> key = chisel.getForwardMap();
            Integer index = tableIndex.get(key);
            if (index == null) {
                index = tables.size();
                tableIndex.put(key, index);
                JsonObject table = new JsonObject();
                table.add("forward", pairs(chisel.getForwardMap()));
                table.add("backward", pairs(chisel.getBackwardMap()));
                table.add("touchForward", pairs(chisel.getTouchForwardMap()));
                table.add("touchBackward", pairs(chisel.getTouchBackwardMap()));
                tables.add(table);
            }
            JsonObject tool = new JsonObject();
            tool.addProperty("id", id(item));
            tool.addProperty("spatula", chisel.isDedicatedSpatula());
            tool.addProperty("cooldownTicks", chisel.getCooldownTicks());
            tool.addProperty("table", index);
            tools.add(tool);
        }
        JsonObject o = new JsonObject();
        o.addProperty("damage", ChiselItem.TRANSFORM_DAMAGE);
        o.addProperty("reverseDamage", ChiselItem.REVERSE_TRANSFORM_DAMAGE);
        o.add("tools", tools);
        o.add("tables", tables);
        return o;
    }

    /** Sortierte Paare [von, nach]; Eintraege, die auf sich selbst zeigen, bewirken nichts und fehlen. */
    private static JsonArray pairs(Map<Block, Block> map) {
        List<String[]> list = new ArrayList<>();
        for (Map.Entry<Block, Block> entry : map.entrySet()) {
            if (entry.getKey() != entry.getValue()) {
                list.add(new String[]{id(entry.getKey()), id(entry.getValue())});
            }
        }
        list.sort(Comparator.<String[], String>comparing(p -> p[0]).thenComparing(p -> p[1]));
        JsonArray out = new JsonArray();
        for (String[] pair : list) {
            JsonArray p = new JsonArray();
            p.add(pair[0]);
            p.add(pair[1]);
            out.add(p);
        }
        return out;
    }

    // =====================================================================================

    private static List<Item> modItems(Class<?> type) {
        List<Item> out = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier key = BuiltInRegistries.ITEM.getKey(item);
            if (type.isInstance(item) && "simplebuilding".equals(key.getNamespace())) {
                out.add(item);
            }
        }
        out.sort(Comparator.comparing(InWorldTransformations::id));
        return out;
    }

    private static List<Block> modBlocks() {
        List<Block> out = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if ("simplebuilding".equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace())) {
                out.add(block);
            }
        }
        out.sort(Comparator.comparing(InWorldTransformations::id));
        return out;
    }

    public static String id(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    public static String id(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }
}

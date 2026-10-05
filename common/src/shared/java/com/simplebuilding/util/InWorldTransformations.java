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
 * Umformen, Diamantblock zerschlagen, Meissel und Spachtel, Schere auf Wolle, Besatzvorlage im
 * Rahmen, Oktant im Kessel waschen, Brecherkolben reparieren, Kupfer-Druckplatte wachsen und
 * abkratzen, Rotator, Constructor's Touch und die Erz-Chance der Baukerne - als JSON fuer das Wiki
 * und den JEI-Katalog.
 *
 * <p>Der Datagen-Provider {@code WikiDataProvider} schreibt das Ergebnis nach
 * {@code src/main/generated/wiki/inworld.json}; {@code wiki/generate.py} macht daraus die
 * Kategorie "Umwandlung in der Welt". Alle Zahlen und Tabellen kommen aus denselben Konstanten
 * und Tabellen, die das Spiel benutzt ({@link SledgehammerUpgrades}, {@link SledgehammerItem},
 * {@link ChiselItem}, {@link SledgehammerEntityInteraction}, {@link OctantCauldronWash}); nichts ist
 * hier abgeschrieben.
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
        root.add("trimTemplate", trimTemplate());
        root.add("cauldronWash", cauldronWash());
        root.add("pistonRepair", pistonRepair());
        root.add("copperPressurePlate", copperPressurePlate());
        root.add("rotator", rotator());
        root.add("constructorsTouch", constructorsTouch());
        root.add("coreOre", coreOre());
        if (com.simplebuilding.version.McVersion.RARE_STRUCTURE_FINDS) {
            root.add("shellUpgrade", shellUpgrade());
        }
        if (com.simplebuilding.version.McVersion.MUSIC_DISCS) {
            root.add("discFlip", discFlip());
        }
        return root;
    }

    // =====================================================================================

    /** Verstaerkt -> Netherit -> Enderit: Stufen, Mindesthammer, Dauer, Schlaege, Haltbarkeit. */
    public static JsonObject sledgehammerUpgrade() {
        List<Item> hammers = modItems(SledgehammerItem.class);
        int hits = SledgehammerUpgrades.UPGRADE_TICKS / SledgehammerUpgrades.HIT_INTERVAL;

        JsonArray steps = new JsonArray();
        // Vorn die Vanilla-Kupfertruhe als Vertreterin aller acht Kupfertruhen (jede Oxidationsstufe,
        // gewachst oder nicht, wird genauso zur Verstaerkten Truhe), dann die Mod-Bloecke.
        List<Block> sources = new ArrayList<>();
        sources.add(BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("copper_chest")));
        // Ebenso die ungefaerbte Vanilla-Shulkerkiste fuer alle 17 (jede Farbe wird zur Verstaerkten Shulkerkiste).
        sources.add(BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("shulker_box")));
        sources.addAll(modBlocks());
        for (Block block : sources) {
            SledgehammerUpgrades.Upgrade upgrade = SledgehammerUpgrades.upgradeOf(block);
            if (upgrade == null) {
                continue;
            }
            JsonObject step = new JsonObject();
            step.addProperty("from", id(upgrade.from()));
            step.addProperty("to", id(upgrade.to()));
            step.addProperty("nugget", id(upgrade.nugget()));
            // SledgehammerUpgrades.finish verbraucht materialCost je Block (eine Doppeltruhe: doppelt so viel),
            // Shulkerkisten brauchen doppelt so viele Schlaege (TieredShulkerBoxes).
            int stepHits = hits * upgrade.durationFactor();
            step.addProperty("nuggetCount", upgrade.materialCost());
            step.addProperty("minimumHammer", weakestHammer(hammers, upgrade.minHammerRank()));
            step.addProperty("damagePerHit", upgrade.damagePerHit());
            step.addProperty("totalDamage", upgrade.damagePerHit() * stepHits);
            step.addProperty("hits", stepHits);
            step.addProperty("durationTicks", stepHits * SledgehammerUpgrades.HIT_INTERVAL);
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
        if (com.simplebuilding.version.McVersion.TRANSFORM_HINTS_AND_CORNERS) {
            o.addProperty("cornerSpeedMultiplier", 1.5);
            o.addProperty("cornerQuartersRemovedPerHit", 1);
        }
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

    /** Diamantblock mit dem Vorschlaghammer zerschlagen (Haemmer ab der Eisenstufe). */
    public static JsonObject diamondCrush() {
        JsonObject o = new JsonObject();
        o.addProperty("block", id(Blocks.DIAMOND_BLOCK));
        o.addProperty("result", id(ModItems.DIAMOND_PEBBLE));
        o.addProperty("count", com.simplebuilding.config.ServerTuning.diamondBlockPebbles());
        o.addProperty("damage", SledgehammerItem.DIAMOND_CRUSH_DAMAGE);
        o.addProperty("strikes", SledgehammerItem.DIAMOND_BLOCK_STRIKES);
        o.addProperty("strikeResetTicks", SledgehammerItem.DIAMOND_STRIKE_RESET_TICKS);
        // Nur Haemmer ab der Eisenstufe (SledgehammerItem#canCrushDiamondBlock).
        JsonArray hammers = new JsonArray();
        for (Item item : modItems(SledgehammerItem.class)) {
            if (SledgehammerItem.canCrushDiamondBlock(item)) {
                hammers.add(id(item));
            }
        }
        o.add("hammers", hammers);
        if (com.simplebuilding.version.McVersion.ANVIL_DIAMOND_CRUSH) {
            JsonObject anvil = new JsonObject();
            anvil.addProperty("count", AnvilDiamondCrushing.PEBBLES);
            anvil.addProperty("minimumFallBlocks", AnvilDiamondCrushing.MINIMUM_FALL_BLOCKS);
            JsonArray tools = new JsonArray();
            for (Block block : List.of(Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL)) {
                tools.add(id(block));
            }
            anvil.add("tools", tools);
            o.add("anvil", anvil);
        }
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

    /**
     * Abgelegte Besatzvorlage mit dem Vorschlaghammer aufwerten ({@link SledgehammerEntityInteraction}):
     * jede Vorlage nach der Namensregel des Spiels, jeder Vorschlaghammer, je Nebenhand-Material das
     * Ergebnis, dazu die Kosten und die Schlaege an der abgelegten Vorlage ({@link PlacedTemplates}).
     */
    public static JsonObject trimTemplate() {
        JsonArray templates = new JsonArray();
        List<String> templateIds = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (SledgehammerEntityInteraction.isTrimTemplate(item)) {
                templateIds.add(id(item));
            }
        }
        templateIds.sort(Comparator.naturalOrder());
        templateIds.forEach(templates::add);
        JsonArray hammers = new JsonArray();
        for (Item hammer : modItems(SledgehammerItem.class)) {
            hammers.add(id(hammer));
        }
        JsonArray upgrades = new JsonArray();
        for (Map.Entry<Item, Item> upgrade : SledgehammerEntityInteraction.trimUpgrades().entrySet()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("catalyst", id(upgrade.getKey()));
            entry.addProperty("catalystCount", SledgehammerEntityInteraction.CATALYST_COST);
            JsonArray materials = new JsonArray();
            for (var material : SledgehammerEntityInteraction.extraMaterials(upgrade.getKey()).entrySet()) {
                JsonObject cost = new JsonObject();
                cost.addProperty("id", id(material.getKey()));
                cost.addProperty("count", material.getValue());
                materials.add(cost);
            }
            entry.add("extraMaterials", materials);
            entry.addProperty("result", id(upgrade.getValue()));
            upgrades.add(entry);
        }
        JsonObject o = new JsonObject();
        o.add("templates", templates);
        o.add("hammers", hammers);
        o.addProperty("damage", SledgehammerEntityInteraction.HAMMER_DAMAGE);
        // Nur abgelegt (PlacedTemplates; der Rahmen-Weg ist entfallen): so viele Schlaege.
        o.addProperty("placedHits", PlacedTemplates.PLACED_HITS);
        o.add("upgrades", upgrades);
        return o;
    }

    /** Gefaerbten Oktanten im Wasserkessel waschen ({@link OctantCauldronWash}). */
    public static JsonObject cauldronWash() {
        JsonArray octants = new JsonArray();
        for (Item octant : OctantCauldronWash.washableOctants()) {
            octants.add(id(octant));
        }
        JsonObject o = new JsonObject();
        o.add("octants", octants);
        o.addProperty("cauldron", id(Items.CAULDRON));
        o.addProperty("result", id(ModItems.OCTANT));
        o.addProperty("waterLevels", OctantCauldronWash.WATER_LEVELS);
        return o;
    }

    /**
     * Beschaedigten Brecherkolben mit dem Klumpen seiner Stufe reparieren
     * ({@link com.simplebuilding.blocks.custom.NetheriteBreakerPistonBlock#canRepairWith}): volle
     * Haltbarkeit, ein Klumpen.
     */
    public static JsonObject pistonRepair() {
        JsonArray steps = new JsonArray();
        for (Block block : modBlocks()) {
            if (block instanceof com.simplebuilding.blocks.custom.NetheriteBreakerPistonBlock breaker) {
                JsonObject step = new JsonObject();
                step.addProperty("piston", id(block));
                step.addProperty("nugget", id(breaker.repairMaterial()));
                step.addProperty("nuggetCount", 1);
                step.addProperty("durability", breaker.maxDurability());
                steps.add(step);
            }
        }
        JsonObject o = new JsonObject();
        o.add("steps", steps);
        return o;
    }

    /**
     * Kupfer-Druckplatte ({@link com.simplebuilding.tweaks.block.CopperPressurePlateBlock#transformWith}):
     * Honigwabe wachst jede Stufe, eine Axt kratzt Wachs oder sonst eine Oxidationsstufe ab.
     */
    public static JsonObject copperPressurePlate() {
        List<Block> stages = com.simplebuilding.tweaks.block.CopperPressurePlateBlock.stages();
        List<Block> waxed = com.simplebuilding.tweaks.block.CopperPressurePlateBlock.waxedStages();
        JsonArray wax = new JsonArray();
        JsonArray unwax = new JsonArray();
        JsonArray scrape = new JsonArray();
        for (int i = 0; i < stages.size(); i++) {
            wax.add(pair(stages.get(i), waxed.get(i)));
            unwax.add(pair(waxed.get(i), stages.get(i)));
            if (i > 0) {
                scrape.add(pair(stages.get(i), stages.get(i - 1)));
            }
        }
        JsonArray axes = new JsonArray();
        for (Item axe : vanillaAxes()) {
            axes.add(id(axe));
        }
        JsonObject o = new JsonObject();
        o.addProperty("honeycomb", id(Items.HONEYCOMB));
        o.add("axes", axes);
        o.addProperty("axeDamage", 1);
        o.add("wax", wax);
        o.add("unwax", unwax);
        o.add("scrape", scrape);
        return o;
    }

    /** Die Vanilla-Aexte, Holz bis Netherit: jede kratzt (im Spiel der Tag {@code minecraft:axes}). */
    public static List<Item> vanillaAxes() {
        return List.of(Items.WOODEN_AXE, Items.STONE_AXE, Items.COPPER_AXE, Items.IRON_AXE, Items.GOLDEN_AXE,
                Items.DIAMOND_AXE, Items.NETHERITE_AXE);
    }

    /**
     * Rotator ({@link com.simplebuilding.items.custom.RotatorItem}): dreht Achse, Blickrichtung oder die
     * 16 Stufen eines Blocks. Beispiele je Art: Stamm (Achse), Beobachter (Blickrichtung), Schild (16 Stufen).
     */
    public static JsonObject rotator() {
        JsonArray examples = new JsonArray();
        for (Block block : List.of(Blocks.OAK_LOG, Blocks.OBSERVER, Blocks.OAK_SIGN)) {
            examples.add(id(block));
        }
        JsonObject o = new JsonObject();
        o.addProperty("tool", id(ModItems.ROTATOR));
        o.addProperty("chargePerTurn", com.simplebuilding.items.custom.RotatorItem.USE_COST);
        o.addProperty("maxCharge", com.simplebuilding.items.custom.RotatorItem.MAX_CHARGE);
        o.addProperty("pearlsForFull", com.simplebuilding.items.custom.RotatorItem.PEARLS_FOR_FULL);
        o.add("examples", examples);
        return o;
    }

    /**
     * Constructor's Touch auf einem Stock ({@link ConstructorsTouchInteraction}): dreht die erste
     * Ausrichtungs-Eigenschaft eines Blocks weiter. Beispiele: Treppe, Schiene, Hebel.
     */
    public static JsonObject constructorsTouch() {
        JsonArray examples = new JsonArray();
        for (Block block : List.of(Blocks.OAK_STAIRS, Blocks.RAIL, Blocks.LEVER)) {
            examples.add(id(block));
        }
        JsonObject o = new JsonObject();
        o.addProperty("tool", id(Items.STICK));
        o.addProperty("enchantment", "simplebuilding:constructors_touch");
        o.add("examples", examples);
        return o;
    }

    /**
     * Erz-Chance der Baukerne ({@link com.simplebuilding.items.custom.CoreOreTransmutation}): je Kern
     * "1 zu N" je Klick, je Wirt die Erze mit ihrem Gewicht (zusammen 100). Die Wirte stehen hier als
     * Bloecke, weil Tags im Datagen nicht gebunden sind; dass {@code hostOf} genau sie nimmt, prueft
     * {@code BuildingCoreTests#coreOreHostsAreTheBlocksOresGenerateIn}.
     */
    public static JsonObject coreOre() {
        JsonArray cores = new JsonArray();
        for (Item item : modItems(com.simplebuilding.items.custom.BuildingCoreItem.class)) {
            JsonObject core = new JsonObject();
            core.addProperty("id", id(item));
            core.addProperty("oneIn", ((com.simplebuilding.items.custom.BuildingCoreItem) item).oreChanceOneIn());
            cores.add(core);
        }
        JsonArray hosts = new JsonArray();
        for (com.simplebuilding.items.custom.CoreOreTransmutation.Host host : com.simplebuilding.items.custom.CoreOreTransmutation.Host.values()) {
            JsonArray blocks = new JsonArray();
            for (Block block : coreOreHostBlocks(host)) {
                blocks.add(id(block));
            }
            JsonArray ores = new JsonArray();
            for (com.simplebuilding.items.custom.CoreOreTransmutation.WeightedOre ore : com.simplebuilding.items.custom.CoreOreTransmutation.ores(host)) {
                JsonObject entry = new JsonObject();
                entry.addProperty("id", id(ore.ore()));
                entry.addProperty("weight", ore.weight());
                ores.add(entry);
            }
            JsonObject h = new JsonObject();
            h.addProperty("host", host.name().toLowerCase(java.util.Locale.ROOT));
            h.add("blocks", blocks);
            h.add("ores", ores);
            hosts.add(h);
        }
        JsonObject o = new JsonObject();
        o.add("cores", cores);
        o.add("hosts", hosts);
        return o;
    }

    /**
     * Abgelegte Shulkerschale mit einem Klumpen aufwerten ({@link ShulkerShells}): je Stufe Schale, Klumpen (genau
     * einer), Ergebnis.
     */
    public static JsonObject shellUpgrade() {
        JsonArray steps = new JsonArray();
        for (ShulkerShells.Step step : ShulkerShells.steps()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("shell", id(step.shell()));
            entry.addProperty("nugget", id(step.nugget()));
            entry.addProperty("nuggetCount", 1);
            entry.addProperty("result", id(step.result()));
            steps.add(entry);
        }
        JsonObject o = new JsonObject();
        o.add("steps", steps);
        return o;
    }

    /**
     * Abgelegte Schallplatte mit dem Vorschlaghammer zum naechsten Track schalten ({@link DiscFlips}): je Platte die
     * vorhandenen Tracks als Kreis (1 -&gt; 2 -&gt; ... -&gt; 1), alle Haemmer, Haltbarkeit je Wechsel.
     */
    public static JsonObject discFlip() {
        JsonArray cycles = new JsonArray();
        for (MusicDiscs.Disc disc : MusicDiscs.discs()) {
            JsonArray tracks = new JsonArray();
            for (Item track : disc.tracks()) {
                tracks.add(id(track));
            }
            cycles.add(tracks);
        }
        JsonArray hammers = new JsonArray();
        for (Item hammer : modItems(SledgehammerItem.class)) {
            hammers.add(id(hammer));
        }
        JsonObject o = new JsonObject();
        o.add("cycles", cycles);
        o.add("hammers", hammers);
        o.addProperty("durabilityPerFlip", SledgehammerItem.RESHAPE_DAMAGE);
        return o;
    }

    /** Die Vanilla-Bloecke eines Wirts (im Spiel: die Tags, siehe {@code CoreOreTransmutation#hostOf}). */
    public static List<Block> coreOreHostBlocks(com.simplebuilding.items.custom.CoreOreTransmutation.Host host) {
        return switch (host) {
            case STONE -> List.of(Blocks.STONE, Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE);
            case DEEPSLATE -> List.of(Blocks.DEEPSLATE, Blocks.TUFF);
            case NETHERRACK -> List.of(Blocks.NETHERRACK);
            case END_STONE -> List.of(Blocks.END_STONE);
        };
    }

    private static JsonArray pair(Block from, Block to) {
        JsonArray p = new JsonArray();
        p.add(id(from));
        p.add(id(to));
        return p;
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

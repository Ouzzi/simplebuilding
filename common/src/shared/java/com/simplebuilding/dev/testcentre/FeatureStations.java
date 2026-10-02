package com.simplebuilding.dev.testcentre;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.tweaks.item.TweaksItems;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;

/**
 * Teststationen der Testzentrale, die je ein Feature zum Ausprobieren aufbauen (B12/P10, Besitzer
 * 2026-10-01): Zustaende kaputt neben heil ({@link #states}), abgelegte Kleinteile, Eier und der
 * Eisenstab unter freiem Himmel ({@link #placeables}), je Mod-Verzauberung ohne eigene Station ein
 * Pfosten mit Testflaeche ({@link #enchants}) und der Fallturm fuer den Sinkdaempfer der
 * Enderit-Ruestung ({@link #sinkDamper}).
 */
public final class FeatureStations {

    private FeatureStations() {
    }

    // =====================================================================================
    // Zustaende: kaputt, leer, rissig neben repariert, geladen, heil
    // =====================================================================================

    /** Anteile des Schadens fuer die drei Riss-Stufen des Echolots (Modell: Schwellen 0, 1/3, 2/3), schlimmste zuerst. */
    static final float[] ECHO_CRACK_STAGES = {5F / 6F, 0.5F, 1F / 6F};
    /** Zeilen pro Haltbarkeits-Tafel; mehr Familien bekommen eine weitere Tafel daneben. */
    static final int STATE_PANEL_LINES = 7;
    /** Tab-Zeilen mit Werkzeugen und Ruestung, deren Haltbarkeit die Station zeigt. */
    public static final List<String> DURABILITY_ROWS = List.of("chisels", "building_wands", "sledgehammers", "pickaxes", "shovels",
            "hoes", "axes", "swords", "spears", "gadgets", "helmets", "chestplates", "leggings", "boots");

    /** Stapel mit dem Anteil {@code fraction} der Haltbarkeit verbraucht (0 = heil, 1 = leer/kaputt). */
    public static ItemStack worn(ItemStack base, float fraction) {
        ItemStack stack = base.copy();
        if (stack.isDamageableItem()) {
            stack.setDamageValue(Math.round(stack.getMaxDamage() * fraction));
        }
        return stack;
    }

    /** Fast verbraucht: ein Punkt Haltbarkeit uebrig. */
    public static ItemStack nearlyBroken(ItemStack base) {
        ItemStack stack = base.copy();
        if (stack.isDamageableItem()) {
            stack.setDamageValue(stack.getMaxDamage() - 1);
        }
        return stack;
    }

    /** Geraete mit eigener Zustandszeile (Ladung, Risse); die Haltbarkeits-Tafeln lassen sie aus. */
    public static boolean hasOwnStateLine(Item item) {
        return item == TweaksItems.LASER_POINTER || item == ModItems.ROTATOR || item == TweaksItems.ECHO_COMPASS;
    }

    public static TcCanvas states(TcContext ctx) {
        TcCanvas c = new TcCanvas();
        int wallZ = 6;
        int floorZ = wallZ - 1;
        int top = STATE_PANEL_LINES;

        // --- Boden vor der Wand: Reparieren und Heilen ---------------------------------------------
        int x = 1;
        // Amboss + Truhe mit allem, was repariert oder laedt.
        c.place(x, 0, floorZ, TestCentreSections.facing(Blocks.ANVIL.defaultBlockState(), Direction.EAST));
        c.place(x + 1, 0, floorZ, TestCentreSections.facing(Blocks.CHEST.defaultBlockState(), Direction.NORTH));
        List<ItemStack> materials = new ArrayList<>(List.of(new ItemStack(Items.AMETHYST_SHARD, 64), new ItemStack(Items.ENDER_PEARL, 16),
                new ItemStack(Items.ECHO_SHARD, 16), new ItemStack(Items.EXPERIENCE_BOTTLE, 64), new ItemStack(Items.IRON_INGOT, 64),
                new ItemStack(Items.DIAMOND, 64), new ItemStack(Items.NETHERITE_INGOT, 16), new ItemStack(ModItems.ENDERITE_INGOT, 16),
                new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.OAK_PLANKS, 64)));
        if (ModItems.SAGE_ORB != null) {
            materials.add(new ItemStack(ModItems.SAGE_ORB, 16));
        }
        c.contents(x + 1, 0, floorZ, materials);
        c.wallSign(x, 1, wallZ, TcText.bold(TcText.t("states.anvil", "Anvil")), TcText.t("states.anvil.sub", "repair, recharge"));
        c.wallSign(x + 1, 1, wallZ, TcText.bold(TcText.t("states.materials", "Materials")), TcText.t("states.materials.sub", "shards, pearls, XP"));
        x += 3;

        // Rissiger Diamantblock neben dem heilen; der Hochofen macht aus rissigen Diamanten wieder ganze.
        c.place(x, 0, floorZ, ModBlocks.CRACKED_DIAMOND_BLOCK);
        c.place(x + 1, 0, floorZ, Blocks.DIAMOND_BLOCK);
        c.wallSign(x, 1, wallZ, TcText.bold(TcText.t("states.cracked_block", "Cracked")), TcText.t("states.cracked_block.sub", "cracked block"));
        c.wallSign(x + 1, 1, wallZ, TcText.bold(TcText.t("states.whole_block", "Whole")), TcText.t("states.whole_block.sub", "diamond block"));
        x += 2;
        c.place(x, 0, floorZ, TestCentreSections.facing(Blocks.BLAST_FURNACE.defaultBlockState(), Direction.NORTH));
        c.contents(x, 0, floorZ, List.of(new ItemStack(ModItems.CRACKED_DIAMOND, 64), new ItemStack(Items.COAL, 64)));
        c.wallSign(x, 1, wallZ, TcText.bold(TcText.t("states.blasting", "Blast furnace")), TcText.t("states.blasting.sub", "cracked diamond"),
                TcText.t("states.blasting.sub2", "-> diamond"));
        x += 2;

        // Ruestung: abgenutzter neben neuem Enderit-Satz.
        List<Item> armour = new ArrayList<>();
        for (String row : List.of("helmets", "chestplates", "leggings", "boots")) {
            List<Item> items = ctx.rowItems(row);
            if (!items.isEmpty()) {
                armour.add(items.getLast());
            }
        }
        if (armour.size() == 4) {
            List<ItemStack> wornSet = new ArrayList<>();
            List<ItemStack> newSet = new ArrayList<>();
            for (Item piece : armour) {
                wornSet.add(nearlyBroken(new ItemStack(piece)));
                newSet.add(new ItemStack(piece));
            }
            wornSet.add(ItemStack.EMPTY);
            wornSet.add(ItemStack.EMPTY);
            newSet.add(ItemStack.EMPTY);
            newSet.add(ItemStack.EMPTY);
            c.stand(x, 0, floorZ - 2, 180F, wornSet, TcText.t("states.armour.worn", "Worn armour"));
            c.stand(x + 2, 0, floorZ - 2, 180F, newSet, TcText.t("states.armour.new", "New armour"));
            x += 4;
        }

        // Rotator zum Ausprobieren: drehbare Bloecke.
        c.wallSign(x, 1, wallZ, TcText.bold(TcText.t("states.rotator", "Rotator")), TcText.t("states.rotator.sub", "turn these"));
        for (Block block : List.of(Blocks.OAK_STAIRS, Blocks.OAK_LOG, Blocks.OBSERVER, Blocks.PISTON)) {
            x++;
            c.place(x, 0, floorZ, block);
        }
        x += 2;

        // --- Wand: Zustandszeilen ------------------------------------------------------------------
        List<TcCanvas.Line> gadgets = new ArrayList<>();
        gadgets.add(new TcCanvas.Line(TcText.t("states.lens", "Resonance Rod"), List.of(
                worn(new ItemStack(TweaksItems.LASER_POINTER), 1F), worn(new ItemStack(TweaksItems.LASER_POINTER), 0.5F),
                new ItemStack(TweaksItems.LASER_POINTER), new ItemStack(Items.AMETHYST_SHARD))));
        gadgets.add(new TcCanvas.Line(TcText.t("states.rotator_charge", "Rotator"), List.of(
                worn(new ItemStack(ModItems.ROTATOR), 1F), worn(new ItemStack(ModItems.ROTATOR), 0.5F),
                new ItemStack(ModItems.ROTATOR), new ItemStack(Items.ENDER_PEARL))));
        List<ItemStack> echo = new ArrayList<>();
        for (float stage : ECHO_CRACK_STAGES) {
            echo.add(worn(new ItemStack(TweaksItems.ECHO_COMPASS), stage));
        }
        echo.add(new ItemStack(TweaksItems.ECHO_COMPASS));
        echo.add(new ItemStack(Items.ECHO_SHARD));
        gadgets.add(new TcCanvas.Line(TcText.t("states.echo", "Echo Sounder"), echo));
        List<ItemStack> mending = new ArrayList<>();
        ctx.enchantment(Enchantments.MENDING).ifPresent(e -> {
            mending.add(TcContext.enchanted(worn(new ItemStack(TweaksItems.ECHO_COMPASS), ECHO_CRACK_STAGES[0]), List.of(e)));
            List<Item> pickaxes = ctx.rowItems("pickaxes");
            if (!pickaxes.isEmpty()) {
                mending.add(TcContext.enchanted(nearlyBroken(new ItemStack(pickaxes.getLast())), List.of(e)));
            }
        });
        mending.add(new ItemStack(Items.EXPERIENCE_BOTTLE));
        if (ModItems.SAGE_ORB != null) {
            mending.add(new ItemStack(ModItems.SAGE_ORB));
        }
        gadgets.add(new TcCanvas.Line(TcText.t("states.mending", "Mending: XP heals"), mending));
        gadgets.add(new TcCanvas.Line(TcText.t("states.diamonds", "Cracked diamond"), List.of(new ItemStack(ModItems.CRACKED_DIAMOND),
                new ItemStack(ModItems.DIAMOND_PEBBLE), new ItemStack(Items.DIAMOND), new ItemStack(ModItems.CRACKED_DIAMOND_BLOCK),
                new ItemStack(Items.DIAMOND_BLOCK))));

        c.title(0, top + 1, wallZ, TcText.t("section.states", "Broken & Repaired"), TcText.t("section.states.sub", "side by side"));
        c.wallSign(x, top + 1, wallZ, TcText.bold(TcText.t("states.gadgets", "Charge & cracks")),
                TcText.t("states.gadgets.sub", "empty/cracked -> full"));
        int end = c.rowsPanel(x, top, wallZ, gadgets);

        // Haltbarkeit: je Familie fast verbraucht neben neu.
        List<TcCanvas.Line> durability = new ArrayList<>();
        for (String row : DURABILITY_ROWS) {
            List<ItemStack> pairs = new ArrayList<>();
            for (ItemStack stack : ctx.row(row)) {
                if (stack.isEmpty() || !stack.isDamageableItem() || hasOwnStateLine(stack.getItem())) {
                    continue;
                }
                pairs.add(nearlyBroken(stack));
                pairs.add(stack.copy());
            }
            if (!pairs.isEmpty()) {
                durability.add(new TcCanvas.Line(TcText.t("tools." + row, TestCentreSections.pretty(row)), pairs));
            }
        }
        for (int i = 0; i < durability.size(); i += STATE_PANEL_LINES) {
            int px = end + 1;
            c.wallSign(px, top + 1, wallZ, TcText.bold(TcText.t("states.durability", "Durability")),
                    TcText.t("states.durability.sub", "worn | new"));
            end = c.rowsPanel(px, top, wallZ, durability.subList(i, Math.min(durability.size(), i + STATE_PANEL_LINES)));
        }
        c.backWall(0, end, wallZ, top + 3);
        return c;
    }

    // =====================================================================================
    // Ablegen: Kleinteile, Eier, Eisenstab unter freiem Himmel
    // =====================================================================================

    /** Alle Items, die sich als Kleinteil ablegen lassen (Tag {@code placeable_small}), Mod zuerst. */
    public static List<Item> smallParts() {
        List<Item> out = new ArrayList<>();
        if (!com.simplebuilding.version.McVersion.SMALL_PLACEABLES) {
            return out;
        }
        for (Item item : TestCentreSections.sortedItems()) {
            if (new ItemStack(item).is(com.simplebuilding.util.ModTags.Items.PLACEABLE_SMALL)) {
                out.add(item);
            }
        }
        return out;
    }

    public static TcCanvas placeables(TcContext ctx) {
        TcCanvas c = new TcCanvas();
        int wallZ = 4;
        int floorZ = wallZ - 1;
        List<ItemStack> parts = new ArrayList<>();
        for (Item item : smallParts()) {
            parts.add(new ItemStack(item));
        }
        List<ItemStack> eggs = new ArrayList<>();
        if (com.simplebuilding.version.McVersion.SMALL_PLACEABLES) {
            for (var egg : com.simplebuilding.blocks.custom.PlacedEggBlock.Egg.values()) {
                eggs.add(new ItemStack(egg.item()));
            }
        }
        if (parts.isEmpty() && eggs.isEmpty() && ModBlocks.IRON_ROD == null) {
            return c;
        }
        c.title(0, 4, wallZ, TcText.t("section.placeables", "Placing"), TcText.t("section.placeables.sub", "sneak + use on floor"));
        int x = 1;
        // Je Kleinteil eine Spalte: Rahmen, Schild, abgelegtes Teil auf dem Boden.
        for (ItemStack part : parts) {
            c.wallFrame(x, 2, wallZ, part);
            c.wallSign(x, 1, wallZ, part.getHoverName());
            c.place(x, 0, floorZ, ModBlocks.PLACED_SMITHING_TEMPLATE.defaultBlockState()
                    .setValue(com.simplebuilding.blocks.custom.PlacedTemplateBlock.FACE, AttachFace.FLOOR)
                    .setValue(com.simplebuilding.blocks.custom.PlacedTemplateBlock.FACING, Direction.SOUTH));
            c.contents(x, 0, floorZ, List.of(part));
            x++;
        }
        if (!parts.isEmpty()) {
            x++;
        }
        // Eier: aufrecht gestellt (Behutsamkeit / Zerbrechen und Schluepfen).
        for (int i = 0; i < eggs.size(); i++) {
            c.wallFrame(x, 2, wallZ, eggs.get(i));
            c.wallSign(x, 1, wallZ, eggs.get(i).getHoverName());
            c.place(x, 0, floorZ, ModBlocks.PLACED_EGG.defaultBlockState().setValue(com.simplebuilding.blocks.custom.PlacedEggBlock.EGG,
                    com.simplebuilding.blocks.custom.PlacedEggBlock.Egg.values()[i]));
            x++;
        }
        if (!eggs.isEmpty()) {
            x++;
        }
        // Eisenstab: frei vor der Wand, nichts darueber (zieht Blitze nur als oberster Block seiner Saeule an).
        if (ModBlocks.IRON_ROD != null) {
            c.place(x + 1, 0, floorZ - 1, ModBlocks.IRON_ROD);
            c.wallFrame(x, 2, wallZ, new ItemStack(ModItems.IRON_ROD));
            c.wallSign(x, 1, wallZ, TcText.bold(TcText.t("placeables.rod", "Iron Rod")), TcText.t("placeables.rod.sub", "open sky, thunder:"),
                    TcText.t("placeables.rod.sub2", "lightning in %s", com.simplebuilding.blocks.custom.IronRodBlock.RANGE));
            x += 3;
        }
        c.backWall(0, x, wallZ, 6);
        return c;
    }

    // =====================================================================================
    // Verzauberungen: je eine Station fuer die, die keine andere Station vorfuehrt
    // =====================================================================================

    /** Mod-Verzauberungen mit eigener Vorfuehrung in einer anderen Station (mining, planning, chisel). */
    static final Set<ResourceKey<Enchantment>> SHOWN_ELSEWHERE = Set.of(ModEnchantments.VERSATILITY, ModEnchantments.VEIN_MINER,
            ModEnchantments.STRIP_MINER, ModEnchantments.LINEAR, ModEnchantments.BRIDGE, ModEnchantments.COVER,
            ModEnchantments.COLOR_PALETTE, ModEnchantments.CONSTRUCTORS_TOUCH);
    /** Kurzer Hinweis je Verzauberung fuer das Schild (englischer Rueckfall; Schluessel {@code enchants.<pfad>}). */
    static final java.util.Map<String, String> ENCHANT_HINTS = java.util.Map.ofEntries(
            java.util.Map.entry("break_through", "hammer: 2 layers"),
            java.util.Map.entry("deep_pockets", "bundle holds more"),
            java.util.Map.entry("double_jump", "jump in the air"),
            java.util.Map.entry("drawer", "more of one kind"),
            java.util.Map.entry("fast_chiseling", "chisel the wall"),
            java.util.Map.entry("funnel", "pick-ups go in"),
            java.util.Map.entry("kinetic_protection", "fly into a wall"),
            java.util.Map.entry("master_builder", "build from bundle"),
            java.util.Map.entry("override", "hammer dirt, wood"),
            java.util.Map.entry("radius", "bigger hammer area"),
            java.util.Map.entry("range", "reach further"));
    static final Set<String> STORAGE_ROWS = Set.copyOf(TestCentreSections.STORAGE_FAMILIES);
    static final Set<String> ARMOUR_ROWS = Set.of("helmets", "chestplates", "leggings", "boots");

    /** Ein Stapel, der die Verzauberung traegt, und die Tab-Zeile, aus der er stammt. */
    record Sample(ItemStack stack, String row) {
    }

    /** Die hoechste Stufe der ersten Tab-Zeile (SimpleTools, dann Lager), die die Verzauberung annimmt; sonst das Buch. */
    static Sample sample(TcContext ctx, Holder<Enchantment> enchantment) {
        List<String> rows = new ArrayList<>();
        for (var row : ctx.toolRows()) {
            if (!row.name().equals("enchanted_books")) {
                rows.add(row.name());
            }
        }
        rows.addAll(TestCentreSections.STORAGE_FAMILIES);
        for (String row : rows) {
            List<ItemStack> stacks = ctx.row(row);
            for (int i = stacks.size() - 1; i >= 0; i--) {
                ItemStack stack = stacks.get(i);
                if (!stack.isEmpty() && enchantment.value().isSupportedItem(stack)) {
                    return new Sample(TcContext.enchanted(stack, List.of(enchantment)), row);
                }
            }
        }
        return new Sample(TcContext.book(enchantment), "");
    }

    /** Die Mod-Verzauberungen, die diese Station zeigt, nach Id. */
    public static List<Holder<Enchantment>> stationEnchantments(TcContext ctx) {
        List<Holder<Enchantment>> out = new ArrayList<>();
        ctx.enchantmentLookup().listElements().forEach(ref -> {
            var key = ref.key();
            if (TcContext.isMod(key.identifier()) && !SHOWN_ELSEWHERE.contains(key)) {
                out.add(ref);
            }
        });
        out.sort(Comparator.comparing(h -> h.unwrapKey().map(k -> k.identifier().toString()).orElse("")));
        return out;
    }

    public static TcCanvas enchants(TcContext ctx) {
        TcCanvas c = new TcCanvas();
        List<Holder<Enchantment>> enchantments = stationEnchantments(ctx);
        if (enchantments.isEmpty()) {
            return c;
        }
        TestCentreSections.postSign(c, 0, 0, TcText.bold(TcText.t("section.enchants", "Enchantments")),
                TcText.t("section.enchants.sub", "one post each"), TcText.t("section.enchants.sub2", "try it on the bed"));
        int px = 2;
        int pz = 0;
        for (Holder<Enchantment> enchantment : enchantments) {
            String path = enchantment.unwrapKey().map(k -> k.identifier().getPath()).orElse("?");
            Sample sample = sample(ctx, enchantment);
            TestCentreSections.post(c, px, pz, sample.stack(), TcText.bold(enchantment.value().description().copy()),
                    TcText.t("enchants." + path, ENCHANT_HINTS.getOrDefault(path, "try it here")), sample.stack().getHoverName());
            if (STORAGE_ROWS.contains(sample.row())) {
                // Buendel/Koecher: Truhe mit Nachschub zum Einsammeln und Bauen.
                c.place(px + 1, 0, pz + 1, TestCentreSections.facing(Blocks.CHEST.defaultBlockState(), Direction.NORTH));
                c.contents(px + 1, 0, pz + 1, List.of(new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.COBBLESTONE, 64),
                        new ItemStack(Items.TORCH, 64), new ItemStack(Items.ARROW, 64), new ItemStack(Items.OAK_PLANKS, 64)));
            } else if (!ARMOUR_ROWS.contains(sample.row())) {
                // Werkzeuge: Pruefwand, drei breit, drei hoch, zwei tief (Durchbruch braucht die zweite Lage).
                Block[] layers = {Blocks.STONE, Blocks.DIRT, Blocks.OAK_PLANKS};
                for (int y = 0; y < 3; y++) {
                    for (int dx = 1; dx <= 3; dx++) {
                        c.place(px + dx, y, pz + 3, layers[y]);
                        c.place(px + dx, y, pz + 4, Blocks.STONE);
                    }
                }
            }
            px += 5;
        }
        return c;
    }

    // =====================================================================================
    // Sinkdaempfer: Fallturm fuer die Enderit-Ruestung
    // =====================================================================================

    /** Hoehe der Plattform des Fallturms (Standflaeche eine Lage hoeher). */
    public static final int TOWER_HEIGHT = 12;

    public static TcCanvas sinkDamper(TcContext ctx) {
        TcCanvas c = new TcCanvas();
        List<ItemStack> gear = new ArrayList<>();
        for (String row : List.of("helmets", "chestplates", "leggings", "boots")) {
            List<Item> items = ctx.rowItems(row);
            gear.add(items.isEmpty() ? ItemStack.EMPTY : new ItemStack(items.getLast()));
        }
        gear.add(ItemStack.EMPTY);
        gear.add(ItemStack.EMPTY);
        TestCentreSections.postSign(c, 0, 0, TcText.bold(TcText.t("section.sinkdamper", "Sink Damper")),
                TcText.t("section.sinkdamper.sub", "wear enderite, climb"), TcText.t("section.sinkdamper.sub2", "sneak while falling"),
                TcText.t("section.sinkdamper.sub3", "sprint = brake"));
        // Turm: Saeule mit Leiter zum Gang, oben eine 3 x 3-Plattform; abspringen zur Seite.
        int tx = 4;
        int tz = 3;
        for (int y = 0; y < TOWER_HEIGHT; y++) {
            c.place(tx, y, tz, TcCanvas.TRIM);
        }
        c.fill(tx - 1, TOWER_HEIGHT, tz, tx + 1, TOWER_HEIGHT, tz + 2, TcCanvas.TRIM);
        for (int y = 0; y <= TOWER_HEIGHT; y++) {
            c.place(tx, y, tz - 1, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH));
        }
        c.anchor("sink_tower_top", tx, TOWER_HEIGHT + 1, tz + 1);
        // Enderit-Satz auf dem Staender (das Kit legt ihn ins Inventar).
        c.stand(tx + 3, 0, 1, 180F, gear, TcText.t("sinkdamper.stand", "Enderite: sneak to sink"));
        c.place(tx + 5, 0, 2, TcCanvas.TRIM);
        c.sign(tx + 5, 0, 1, Direction.NORTH, TcText.t("sinkdamper.pieces", "more pieces"), TcText.t("sinkdamper.pieces.sub", "= softer fall"),
                TcText.t("sinkdamper.damage", "less fall damage"));
        return c;
    }
}

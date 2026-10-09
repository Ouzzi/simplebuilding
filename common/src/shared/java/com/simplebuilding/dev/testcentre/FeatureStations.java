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
        // Mischungen (2026-10-03): Kerzen und Seegurken mit Kleinteilen. Feuerzeug zuendet an, leere Hand loescht;
        // die Gurken liegen nass hinter einer Glasscheibe (leuchten nur unter Wasser).
        if (ModBlocks.PLACED_SMALL_PARTS != null) {
            var pile = ModBlocks.PLACED_SMALL_PARTS.defaultBlockState()
                    .setValue(com.simplebuilding.blocks.custom.PlacedSmallPartsBlock.FACING, Direction.SOUTH);
            var lit = com.simplebuilding.blocks.custom.PlacedSmallPartsBlock.LIT;
            var wet = com.simplebuilding.blocks.custom.PlacedSmallPartsBlock.WATERLOGGED;
            c.place(x, 0, floorZ, pile);
            c.contents(x, 0, floorZ, List.of(new ItemStack(net.minecraft.world.item.Items.CANDLE), new ItemStack(ModItems.STONE_PEBBLE),
                    new ItemStack(ModItems.STONE_PEBBLE), new ItemStack(net.minecraft.world.item.Items.EGG)));
            c.wallSign(x, 1, wallZ, TcText.bold(TcText.t("placeables.mix", "Mix")), TcText.t("placeables.mix.candle", "candle, pebbles"),
                    TcText.t("placeables.mix.light", "flint: light"));
            x++;
            c.place(x, 0, floorZ, pile.setValue(lit, true));
            c.contents(x, 0, floorZ, List.of(new ItemStack(net.minecraft.world.item.Items.DYED_CANDLE.pick(net.minecraft.world.item.DyeColor.RED)),
                    new ItemStack(net.minecraft.world.item.Items.DYED_CANDLE.pick(net.minecraft.world.item.DyeColor.WHITE)), new ItemStack(ModItems.FLINT_CHIP)));
            c.wallSign(x, 1, wallZ, TcText.bold(TcText.t("placeables.mix", "Mix")), TcText.t("placeables.mix.lit", "lit candles"),
                    TcText.t("placeables.mix.out", "hand: put out"));
            x++;
            // Nass zwischen zwei Haeufchen (die nimmt fliessendes Wasser nicht an), vorn eine Glasscheibe.
            c.place(x, 0, floorZ - 1, Blocks.GLASS);
            c.place(x, 0, floorZ, pile.setValue(wet, true));
            c.contents(x, 0, floorZ, List.of(new ItemStack(net.minecraft.world.item.Items.SEA_PICKLE),
                    new ItemStack(net.minecraft.world.item.Items.SEA_PICKLE), new ItemStack(ModItems.STONE_PEBBLE)));
            c.wallSign(x, 1, wallZ, TcText.bold(TcText.t("placeables.mix", "Mix")), TcText.t("placeables.mix.pickles", "pickles, pebble"),
                    TcText.t("placeables.mix.water", "glow in water"));
            x++;
            c.place(x, 0, floorZ, pile);
            c.contents(x, 0, floorZ, List.of(new ItemStack(net.minecraft.world.item.Items.GLOWSTONE_DUST),
                    new ItemStack(net.minecraft.world.item.Items.NETHER_STAR), new ItemStack(net.minecraft.world.item.Items.DYED_CANDLE.pick(net.minecraft.world.item.DyeColor.LIME)),
                    new ItemStack(net.minecraft.world.item.Items.SEA_PICKLE)));
            c.wallSign(x, 1, wallZ, TcText.bold(TcText.t("placeables.mix", "Mix")), TcText.t("placeables.mix.glow", "glowing parts"),
                    TcText.t("placeables.mix.glow.sub", "particles"));
            x += 2;
        }
        // Eisenstab: frei vor der Wand, nichts darueber (zieht Blitze nur als oberster Block seiner Saeule an).
        if (ModBlocks.IRON_ROD != null) {
            c.place(x + 1, 0, floorZ - 1, ModBlocks.IRON_ROD);
            c.wallFrame(x, 2, wallZ, new ItemStack(ModItems.IRON_ROD));
            c.wallSign(x, 1, wallZ, TcText.bold(TcText.t("placeables.rod", "Iron Rod")), TcText.t("placeables.rod.sub", "open sky, thunder:"),
                    TcText.t("placeables.rod.sub2", "lightning in %s", com.simplebuilding.blocks.custom.MetalRodBlock.IRON_RANGE));
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

    // =====================================================================================
    // Pfeil-Station: jeder Pfeil gegen Trainingspuppen (Besitzer 2026-10-02)
    // =====================================================================================

    /** Abstand der Schusslinie (Truhen, z = 1) zu den Puppen. */
    public static final int ARCHERY_RANGE = 10;

    /** Ziel einer Puppe der Pfeil-Station: Kopf (leer = ihr Kuerbiskopf), Schild-Schluessel, englischer Rueckfall. */
    record Target(ItemStack head, String key, String label, String sub) {
    }

    static List<Target> archeryTargets() {
        return List.of(
                new Target(ItemStack.EMPTY, "archery.plain", "Pumpkin Head", "no mob type"),
                new Target(new ItemStack(Items.ZOMBIE_HEAD), "archery.zombie", "Zombie Head", "undead, zombie"),
                new Target(new ItemStack(TweaksItems.DROWNED_HEAD), "archery.drowned", "Drowned Head", "copper tip"),
                new Target(new ItemStack(Items.SKELETON_SKULL), "archery.skeleton", "Skeleton Skull", "undead: smite"),
                new Target(new ItemStack(TweaksItems.SPIDER_HEAD), "archery.spider", "Spider Head", "arthropod: bane"),
                new Target(new ItemStack(TweaksItems.ENDERMAN_HEAD), "archery.enderman", "Enderman Head", "arrows: immune"),
                new Target(new ItemStack(TweaksItems.BLAZE_HEAD), "archery.blaze", "Blaze Head", "fire: immune"),
                new Target(ItemStack.EMPTY, "archery.armour", "Iron Armor", "less damage"));
    }

    /** Alle Trank-Pfeile mit Wirkung, in Registry-Reihenfolge. */
    static List<ItemStack> tippedArrows() {
        List<ItemStack> out = new ArrayList<>();
        for (Holder.Reference<net.minecraft.world.item.alchemy.Potion> potion : net.minecraft.core.registries.BuiltInRegistries.POTION.listElements().toList()) {
            if (!potion.value().getEffects().isEmpty()) {
                out.add(net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.TIPPED_ARROW, potion).copyWithCount(16));
            }
        }
        return out;
    }

    public static int tippedArrowCount() {
        return tippedArrows().size();
    }

    /** Boegen und Armbrueste: schlicht und mit den Verzauberungen, die den Schaden aendern. */
    static List<ItemStack> launchers(TcContext ctx) {
        List<ItemStack> out = new ArrayList<>(List.of(new ItemStack(Items.BOW), new ItemStack(Items.CROSSBOW)));
        for (var set : List.of(List.of(Enchantments.POWER), List.of(Enchantments.POWER, Enchantments.FLAME, Enchantments.PUNCH))) {
            List<Holder<Enchantment>> holders = new ArrayList<>();
            set.forEach(key -> ctx.enchantment(key).ifPresent(holders::add));
            out.add(TcContext.enchanted(new ItemStack(Items.BOW), holders));
        }
        for (var key : List.of(Enchantments.MULTISHOT, Enchantments.PIERCING)) {
            ctx.enchantment(key).ifPresent(holder -> out.add(TcContext.enchanted(new ItemStack(Items.CROSSBOW), List.of(holder))));
        }
        out.add(new ItemStack(Items.ARROW, 64));
        out.add(new ItemStack(Items.SPECTRAL_ARROW, 64));
        return out;
    }

    /**
     * Pfeil-Station: vorn eine Reihe Truhen (Boegen/Armbrueste, alle Befiederungs-Pfeile, alle Trank-Pfeile), zehn
     * Bloecke dahinter Trainingspuppen mit verschiedenen Koepfen und eine in Eisenruestung. Jede Puppe zeigt den
     * Schaden gegen die Mob-Art ihres Kopfes.
     */
    public static TcCanvas archery(TcContext ctx) {
        TcCanvas c = new TcCanvas();
        if (!com.simplebuilding.version.McVersion.TRAINING_DUMMY) {
            return c;
        }
        int chestZ = 1;
        int dummyZ = chestZ + ARCHERY_RANGE;
        int wallZ = dummyZ + 2;
        List<List<ItemStack>> chests = new ArrayList<>();
        chests.add(launchers(ctx));
        if (com.simplebuilding.version.McVersion.FLETCHING) {
            List<ItemStack> crafted = new ArrayList<>();
            for (var parts : com.simplebuilding.fletching.ArrowParts.allCombinations()) {
                crafted.add(com.simplebuilding.fletching.ArrowParts.stack(parts, 16));
            }
            for (int i = 0; i < crafted.size(); i += 27) {
                chests.add(crafted.subList(i, Math.min(crafted.size(), i + 27)));
            }
        }
        // Mittlerer/kleiner Ruestungsstaender (2026-10-09) als Items in der Truhe der Station.
        chests.add(List.of(new ItemStack(ModItems.MEDIUM_ARMOR_STAND), new ItemStack(ModItems.SMALL_ARMOR_STAND)));
        List<ItemStack> tipped = tippedArrows();
        for (int i = 0; i < tipped.size(); i += 27) {
            chests.add(tipped.subList(i, Math.min(tipped.size(), i + 27)));
        }
        int x = 1;
        for (List<ItemStack> contents : chests) {
            c.place(x, 0, chestZ, TestCentreSections.facing(Blocks.CHEST.defaultBlockState(), Direction.NORTH));
            c.contents(x, 0, chestZ, contents);
            x++;
        }
        int dx = 1;
        for (Target target : archeryTargets()) {
            List<ItemStack> gear = new ArrayList<>(List.of(target.head(), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
            if (target.key().equals("archery.armour")) {
                gear = List.of(target.head(), new ItemStack(Items.IRON_CHESTPLATE), new ItemStack(Items.IRON_LEGGINGS),
                        new ItemStack(Items.IRON_BOOTS));
            }
            c.dummy(dx, 0, dummyZ, 180F, gear, TcText.t(target.key(), target.label()));
            c.wallSign(dx, 2, wallZ, TcText.bold(TcText.t(target.key(), target.label())), TcText.t(target.key() + ".sub", target.sub()));
            dx += 2;
        }
        int end = Math.max(x, dx);
        c.title(0, 3, wallZ, TcText.t("section.archery", "Archery"), TcText.t("section.archery.sub", "every arrow, dummies"));
        c.wallSign(end, 2, wallZ, TcText.bold(TcText.t("archery.how", "Training Dummy")), TcText.t("archery.how.sub", "sneak + hit: pick up"),
                TcText.t("archery.how.sub2", "red = critical"));
        c.backWall(0, end, wallZ, 5);
        return c;
    }
    /**
     * Musik-Station (2026-10-03, nur mit {@code McVersion.MUSIC_DISCS}): links ein Plattenspieler mit einem
     * Musik-Verstärker daneben und einer ohne, rechts ein Notenblock mit Noten-Verstärker und einer ohne -
     * zum Vergleichen der Hoerweite. Davor eine Truhe mit allen Platten und einem Vorschlaghammer und eine abgelegte
     * Platte zum Wenden (Rechtsklick mit dem Hammer: A-Seite &lt;-&gt; B-Seite).
     */
    public static TcCanvas music(TcContext ctx) {
        TcCanvas c = new TcCanvas();
        if (!com.simplebuilding.version.McVersion.MUSIC_DISCS) {
            return c;
        }
        int z = 2;
        int wallZ = 5;
        c.place(1, 0, z, Blocks.JUKEBOX);
        c.place(2, 0, z, ModBlocks.JUKEBOX_AMPLIFIER);
        c.place(4, 0, z, Blocks.JUKEBOX);
        // Notenbloecke auf Eichenbrettern mit passendem Instrument (Bass): sonst stellt der erste Nachbar-Update den
        // Zustand nach dem Boden um (Tiefenschiefer/Seelaterne), und die Zentrale weicht von ihrer Planung ab.
        var note = Blocks.NOTE_BLOCK.defaultBlockState().setValue(net.minecraft.world.level.block.NoteBlock.INSTRUMENT,
                net.minecraft.world.level.block.state.properties.NoteBlockInstrument.BASS);
        for (int x = 6; x <= 9; x++) {
            c.place(x, 0, z, Blocks.OAK_PLANKS);
        }
        c.place(6, 1, z, note);
        c.place(7, 1, z, ModBlocks.NOTE_AMPLIFIER);
        c.place(9, 1, z, note);
        c.place(1, 0, 1, TestCentreSections.facing(Blocks.CHEST.defaultBlockState(), Direction.NORTH));
        List<ItemStack> chest = new ArrayList<>();
        for (Item disc : com.simplebuilding.util.MusicDiscs.items()) {
            chest.add(new ItemStack(disc));
        }
        chest.add(new ItemStack(ModItems.IRON_SLEDGEHAMMER));
        chest.add(new ItemStack(ModItems.JUKEBOX_AMPLIFIER, 3));
        chest.add(new ItemStack(ModItems.NOTE_AMPLIFIER, 3));
        c.contents(1, 0, 1, chest);
        if (ModBlocks.PLACED_SMALL_PARTS != null) {
            c.place(3, 0, 1, ModBlocks.PLACED_SMALL_PARTS.defaultBlockState()
                    .setValue(com.simplebuilding.blocks.custom.PlacedSmallPartsBlock.FACING, Direction.SOUTH));
            c.contents(3, 0, 1, List.of(new ItemStack(ModItems.MUSIC_DISC_VOIDLINE)));
        }
        c.title(0, 3, wallZ, TcText.t("section.music", "Music"), TcText.t("section.music.sub", "discs, B-sides, speakers"));
        c.wallSign(2, 2, wallZ, TcText.bold(TcText.t("music.jukebox", "Jukebox Amplifier")),
                TcText.t("music.jukebox.sub", "left: louder jukebox"), TcText.t("music.jukebox.sub2", "right: vanilla"));
        c.wallSign(7, 2, wallZ, TcText.bold(TcText.t("music.note", "Note Amplifier")),
                TcText.t("music.note.sub", "left: louder note block"), TcText.t("music.note.sub2", "right: vanilla"));
        c.wallSign(4, 1, wallZ, TcText.bold(TcText.t("music.flip", "B-Side")),
                TcText.t("music.flip.sub", "placed disc + hammer"), TcText.t("music.flip.sub2", "right-click: flip"));
        c.backWall(0, 10, wallZ, 5);
        return c;
    }

    /**
     * Crucible station (P6, McVersion.CRUCIBLE): an Enderite crucible on a reinforced cauldron with soul lava (extreme
     * heat), an unattached Enderite barrel beside it (hammer it 6 times), an iron block to build a crucible on, a quartz
     * block to crush and a cauldron to reinforce; the chest holds the parts and every bucket.
     */
    public static TcCanvas crucible(TcContext ctx) {
        TcCanvas c = new TcCanvas();
        if (!com.simplebuilding.version.McVersion.CRUCIBLE) {
            return c;
        }
        int z = 2;
        int wallZ = 5;
        c.place(2, 0, z, com.simplebuilding.crucible.CrucibleCompat.reinforcedCauldron("extreme"));
        c.place(2, 1, z, com.simplebuilding.crucible.CrucibleCompat.enderiteCrucible());
        c.place(3, 1, z, com.simplebuilding.crucible.CrucibleCompat.enderiteBarrel());
        c.place(5, 0, z, Blocks.IRON_BLOCK);
        c.place(7, 0, z, Blocks.QUARTZ_BLOCK);
        c.place(9, 0, z, Blocks.CAULDRON);
        c.place(1, 0, 1, TestCentreSections.facing(Blocks.CHEST.defaultBlockState(), Direction.NORTH));
        List<ItemStack> chest = new ArrayList<>();
        chest.add(new ItemStack(ModItems.NETHERITE_SLEDGEHAMMER));
        chest.add(new ItemStack(Items.HEAVY_WEIGHTED_PRESSURE_PLATE, 4));
        if (ModItems.IRON_ROD != null) chest.add(new ItemStack(ModItems.IRON_ROD, 2));
        chest.add(new ItemStack(ModItems.CRACKED_DIAMOND, 16));
        chest.add(new ItemStack(ModItems.NETHERITE_NUGGET, 4));
        chest.add(new ItemStack(ModItems.ENDERITE_NUGGET, 4));
        for (Item bucket : com.simplebuilding.fluid.ModFluids.buckets()) { // 6 + 21 Eimer = 27, eine Truhe voll
            if (com.simplebuilding.fluid.ModFluids.fullEnderiteBuckets().contains(bucket)) continue; // volle: zweimal schoepfen
            chest.add(new ItemStack(bucket));
        }
        c.contents(1, 0, 1, chest);
        // Volle Enderit-Eimer (N21, 2 Eimer) passen nicht mehr in die Truhe: eigenes Fass daneben.
        c.place(3, 0, 1, Blocks.BARREL);
        List<ItemStack> full = new ArrayList<>();
        for (Item bucket : com.simplebuilding.fluid.ModFluids.fullEnderiteBuckets()) full.add(new ItemStack(bucket));
        c.contents(3, 0, 1, full);
        c.title(0, 3, wallZ, TcText.t("section.crucible", "Crucibles"), TcText.t("section.crucible.sub", "soul lava, buckets, hammer"));
        c.wallSign(2, 2, wallZ, TcText.bold(TcText.t("crucible.extreme", "Extreme heat")),
                TcText.t("crucible.extreme.sub", "soul lava in a"), TcText.t("crucible.extreme.sub2", "reinforced cauldron"));
        c.wallSign(5, 2, wallZ, TcText.bold(TcText.t("crucible.build", "Build")),
                TcText.t("crucible.build.sub", "hammer + 4 plates,"), TcText.t("crucible.build.sub2", "then 2 iron rods"));
        c.wallSign(8, 2, wallZ, TcText.bold(TcText.t("crucible.hammer", "Hammer")),
                TcText.t("crucible.hammer.sub", "quartz: 4 quartz"), TcText.t("crucible.hammer.sub2", "cauldron: 8 cracked"));
        c.backWall(0, 10, wallZ, 5);
        return c;
    }

    /** Achtelzellen der Station: je Farbe ein anderes Muster (Bitmaske, Index x | y << 1 | z << 2). */
    private static final int[] OCTET_MASKS = {0x01, 0x03, 0x05, 0x0F, 0x33, 0x55, 0x3F, 0x77, 0x7F, 0xFF, 0x8B, 0x17, 0xE8};

    /**
     * Schach-Station (docs/ai/PLAN-SCHACH-2026-10-06.md, McVersion.CHESS): links ein Brett aus 4 x 4 Lapis-Schachbrettern
     * mit der Grundstellung in 3D (Quarz gegen Schwarzstein, je Feld ein Viertel), daneben dasselbe mit flachen Figuren
     * (Quarz gegen Netherziegel) auf Harz-Schachbrettern; rechts je Farbe drei Zellen mit allen zwoelf Figuren auf ihrem
     * Schachbrett und davor eine Achtelzelle; dazu ein wassergefuelltes Achtel im Glas und eine Truhe mit allen Achteln.
     */
    public static TcCanvas chess(TcContext ctx) {
        TcCanvas c = new TcCanvas();
        if (!com.simplebuilding.version.McVersion.CHESS) {
            return c;
        }
        int wallZ = 8;
        chessBoard(c, 1, ModBlocks.LAPIS_QUARTZ_CHECKER, com.simplebuilding.chess.ChessColor.QUARTZ,
                com.simplebuilding.chess.ChessColor.BLACKSTONE, false);
        chessBoard(c, 6, ModBlocks.RESIN_QUARTZ_CHECKER, com.simplebuilding.chess.ChessColor.QUARTZ,
                com.simplebuilding.chess.ChessColor.NETHER_BRICK, true);
        // Galerie: je Farbe drei Zellen (3D Bauer-Laeufer, Dame/Koenig + flach Bauer/Turm, flach Springer-Koenig).
        com.simplebuilding.chess.ChessColor[] colors = com.simplebuilding.chess.ChessColor.values();
        com.simplebuilding.chess.ChessPiece[] pieces = com.simplebuilding.chess.ChessPiece.values();
        for (int i = 0; i < colors.length; i++) {
            int x = 11 + i;
            com.simplebuilding.chess.ChessColor color = colors[i];
            Block floor = color.checker() != null ? color.checker() : Blocks.QUARTZ_BLOCK;
            List<ItemStack> all = new ArrayList<>();
            for (boolean flat : new boolean[]{false, true}) {
                for (com.simplebuilding.chess.ChessPiece piece : pieces) {
                    all.add(new ItemStack(com.simplebuilding.chess.ChessItems.piece(color, piece, flat)));
                }
            }
            for (int cell = 0; cell < 3; cell++) {
                c.place(x, -1, 1 + cell, floor);
                c.place(x, 0, 1 + cell, ModBlocks.CHESS_PIECES.defaultBlockState()
                        .setValue(com.simplebuilding.blocks.custom.ChessPiecesBlock.FACING, Direction.NORTH));
                c.contents(x, 0, 1 + cell, all.subList(cell * 4, cell * 4 + 4));
            }
            c.place(x, 0, 5, com.simplebuilding.blocks.custom.CheckerOctetBlock.withMask(ModBlocks.CHECKER_OCTET.defaultBlockState()
                    .setValue(com.simplebuilding.blocks.custom.CheckerOctetBlock.COLOR, color), OCTET_MASKS[i % OCTET_MASKS.length]));
        }
        // Wassergefuelltes Achtel (3/8) in einem Glasbecken.
        c.place(2, -1, 6, Blocks.GLASS);
        c.place(1, 0, 6, Blocks.GLASS);
        c.place(3, 0, 6, Blocks.GLASS);
        c.place(2, 0, 5, Blocks.GLASS);
        c.place(2, 0, 7, Blocks.GLASS);
        c.place(2, 0, 6, com.simplebuilding.blocks.custom.CheckerOctetBlock.withMask(ModBlocks.CHECKER_OCTET.defaultBlockState()
                .setValue(com.simplebuilding.blocks.custom.CheckerOctetBlock.COLOR, com.simplebuilding.chess.ChessColor.PURPUR)
                .setValue(com.simplebuilding.blocks.custom.CheckerOctetBlock.WATERLOGGED, true), 0x07));
        c.place(5, 0, 6, TestCentreSections.facing(Blocks.CHEST.defaultBlockState(), Direction.NORTH));
        List<ItemStack> chest = new ArrayList<>();
        for (Item octet : com.simplebuilding.chess.ChessItems.octets()) {
            chest.add(new ItemStack(octet, 16));
        }
        chest.add(new ItemStack(ModBlocks.LAPIS_QUARTZ_CHECKER, 8));
        chest.add(new ItemStack(Items.QUARTZ_BLOCK, 8));
        chest.add(new ItemStack(Items.STONECUTTER));
        c.contents(5, 0, 6, chest);
        c.title(0, 3, wallZ, TcText.t("section.chess", "Chess"), TcText.t("section.chess.sub", "octets, pieces, checker stairs"));
        c.wallSign(3, 2, wallZ, TcText.bold(TcText.t("chess.place", "Pieces")),
                TcText.t("chess.place.sub", "one per checker field"), TcText.t("chess.place.sub2", "empty hand: turn"));
        c.wallSign(8, 2, wallZ, TcText.bold(TcText.t("chess.swap", "Swap")),
                TcText.t("chess.swap.sub", "sneak + piece: replace"), TcText.t("chess.swap.sub2", "sneak + hand: pick up"));
        c.wallSign(13, 2, wallZ, TcText.bold(TcText.t("chess.octet", "Octets")),
                TcText.t("chess.octet.sub", "placed where you click"), TcText.t("chess.octet.sub2", "hold water until full"));
        c.backWall(0, 11 + colors.length, wallZ, 5);
        return c;
    }

    /**
     * Holzwerk-Station (docs/ai/PLAN-HOLZWERK-2026-10-09.md, McVersion.WOODWORK): je Holzart eine Spalte - liegende
     * Roehre, liegende entrindete Roehre, Platte, entrindete Platte, Holzkessel (Wasser), Kiste mit Essen und Schnitzholz
     * (je Holzart ein anderes Motiv); davor eine Truhe mit Meissel, Scherben, Lavaeimer und Staemmen.
     */
    public static TcCanvas woodwork(TcContext ctx) {
        TcCanvas c = new TcCanvas();
        if (!com.simplebuilding.version.McVersion.WOODWORK) {
            return c;
        }
        int wallZ = 10;
        List<com.simplebuilding.woodwork.WoodBlocks.Family> families = com.simplebuilding.woodwork.WoodBlocks.families();
        com.simplebuilding.woodwork.SherdMotif[] motifs = com.simplebuilding.woodwork.SherdMotif.values();
        Item[] foods = {Items.BREAD, Items.APPLE, Items.CARROT, Items.POTATO, Items.BAKED_POTATO, Items.COOKED_BEEF,
                Items.GOLDEN_CARROT, Items.BEETROOT, Items.MELON_SLICE, Items.COOKIE, Items.SWEET_BERRIES, Items.PUMPKIN_PIE};
        for (int i = 0; i < families.size(); i++) {
            com.simplebuilding.woodwork.WoodBlocks.Family f = families.get(i);
            int x = 1 + i;
            c.place(x, 0, 1, f.hollow().defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.Z));
            c.place(x, 0, 2, f.hollowStripped().defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.Z));
            c.place(x, 0, 3, f.sheet());
            c.place(x, 0, 4, f.strippedSheet());
            c.place(x, 0, 5, f.cauldron().with(com.simplebuilding.woodwork.WoodenCauldronBlock.Content.WATER, 3));
            c.place(x, 0, 6, f.crate());
            c.contents(x, 0, 6, List.of(new ItemStack(foods[i % foods.length], 64), new ItemStack(foods[(i + 3) % foods.length], 32)));
            c.place(x, 0, 7, f.carved().defaultBlockState()
                    .setValue(com.simplebuilding.woodwork.CarvedLogBlock.FACING, Direction.NORTH)
                    .setValue(com.simplebuilding.woodwork.CarvedLogBlock.MOTIF, motifs[i % motifs.length]));
        }
        int chestX = families.size() + 2;
        c.place(chestX, 0, 7, TestCentreSections.facing(Blocks.CHEST.defaultBlockState(), Direction.NORTH));
        List<ItemStack> chest = new ArrayList<>();
        chest.add(new ItemStack(ModItems.STONE_CHISEL));
        for (com.simplebuilding.woodwork.SherdMotif motif : motifs) {
            chest.add(new ItemStack(motif.sherd()));
        }
        chest.add(new ItemStack(Items.LAVA_BUCKET));
        chest.add(new ItemStack(Items.WATER_BUCKET));
        chest.add(new ItemStack(Items.STRIPPED_OAK_LOG, 16));
        chest.add(new ItemStack(Items.OAK_LOG, 16));
        c.contents(chestX, 0, 7, chest);
        c.title(0, 3, wallZ, TcText.t("section.woodwork", "Woodwork"), TcText.t("section.woodwork.sub", "hollow logs, sheets, cauldrons, crates, carving"));
        c.wallSign(3, 2, wallZ, TcText.bold(TcText.t("woodwork.crawl", "Hollow logs")),
                TcText.t("woodwork.crawl.sub", "sneak into a lying one"), TcText.t("woodwork.crawl.sub2", "to crawl through"));
        c.wallSign(7, 2, wallZ, TcText.bold(TcText.t("woodwork.crate", "Crates")),
                TcText.t("woodwork.crate.sub", "food in, empty hand out"), TcText.t("woodwork.crate.sub2", "8 stacks, hoppers"));
        c.wallSign(11, 2, wallZ, TcText.bold(TcText.t("woodwork.carve", "Carving")),
                TcText.t("woodwork.carve.sub", "sherd in off hand,"), TcText.t("woodwork.carve.sub2", "chisel a stripped log"));
        c.backWall(0, chestX + 1, wallZ, 5);
        return c;
    }

    /** Brett aus 4 x 4 Schachbrettern ab {@code x0} (z 1-4) mit der Grundstellung: Weiss im Sueden, Schwarz im Norden. */
    private static void chessBoard(TcCanvas c, int x0, Block checker, com.simplebuilding.chess.ChessColor white,
                                   com.simplebuilding.chess.ChessColor black, boolean flat) {
        com.simplebuilding.chess.ChessPiece[] back = {com.simplebuilding.chess.ChessPiece.ROOK, com.simplebuilding.chess.ChessPiece.KNIGHT,
                com.simplebuilding.chess.ChessPiece.BISHOP, com.simplebuilding.chess.ChessPiece.QUEEN, com.simplebuilding.chess.ChessPiece.KING,
                com.simplebuilding.chess.ChessPiece.BISHOP, com.simplebuilding.chess.ChessPiece.KNIGHT, com.simplebuilding.chess.ChessPiece.ROOK};
        for (int bx = 0; bx < 4; bx++) {
            for (int bz = 1; bz <= 4; bz++) {
                c.place(x0 + bx, -1, bz, checker.defaultBlockState());
            }
            ItemStack pawnW = new ItemStack(com.simplebuilding.chess.ChessItems.piece(white, com.simplebuilding.chess.ChessPiece.PAWN, flat));
            ItemStack pawnB = new ItemStack(com.simplebuilding.chess.ChessItems.piece(black, com.simplebuilding.chess.ChessPiece.PAWN, flat));
            // Plaetze: 0 Nordwest, 1 Nordost, 2 Suedwest, 3 Suedost.
            c.place(x0 + bx, 0, 1, ModBlocks.CHESS_PIECES.defaultBlockState()
                    .setValue(com.simplebuilding.blocks.custom.ChessPiecesBlock.FACING, Direction.SOUTH));
            c.contents(x0 + bx, 0, 1, List.of(
                    new ItemStack(com.simplebuilding.chess.ChessItems.piece(black, back[bx * 2], flat)),
                    new ItemStack(com.simplebuilding.chess.ChessItems.piece(black, back[bx * 2 + 1], flat)), pawnB, pawnB));
            c.place(x0 + bx, 0, 4, ModBlocks.CHESS_PIECES.defaultBlockState()
                    .setValue(com.simplebuilding.blocks.custom.ChessPiecesBlock.FACING, Direction.NORTH));
            c.contents(x0 + bx, 0, 4, List.of(pawnW, pawnW,
                    new ItemStack(com.simplebuilding.chess.ChessItems.piece(white, back[bx * 2], flat)),
                    new ItemStack(com.simplebuilding.chess.ChessItems.piece(white, back[bx * 2 + 1], flat))));
        }
    }
}

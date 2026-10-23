package com.simplebuilding.dev.testcentre;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.DevEnchantedTab;
import com.simplebuilding.items.ModItemGroupsContent;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.ChiselItem;
import com.simplebuilding.items.custom.OreDetectorItem;
import com.simplebuilding.util.GlowingTrimUtils;
import com.simplebuilding.util.InWorldTransformations;
import com.simplebuilding.util.OctantCauldronWash;
import com.simplebuilding.util.SledgehammerEntityInteraction;
import com.simplebuilding.util.SledgehammerUpgrades;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Die Abschnitte der Testzentrale. Jeder Abschnitt zeichnet auf eine eigene {@link TcCanvas}; die
 * Inhalte kommen aus den Registern und den Kreativ-Tabs, nicht aus festen Listen - ein neues Item in
 * einer Tab-Zeile, eine neue Verzauberung, ein neues Besatzmuster oder eine neue Meissel-Kette
 * erscheint also ohne Aenderung hier.
 *
 * <p>Was keinem Abschnitt zufaellt, landet in {@link #unsorted} und laesst den Abdeckungstest rot
 * werden - dann gehoert das neue Item in einen passenden Abschnitt.
 */
public final class TestCentreSections {

    private TestCentreSections() {
    }

    // =====================================================================================
    // 1. Ruestung, Besatz, Vorlagen
    // =====================================================================================

    public static TcCanvas armour(TcContext ctx) {
        TcCanvas c = new TcCanvas();
        int wallZ = 9;
        List<Item> helmets = ctx.rowItems("helmets");
        List<Item> chests = ctx.rowItems("chestplates");
        List<Item> legs = ctx.rowItems("leggings");
        List<Item> boots = ctx.rowItems("boots");
        List<Item> swords = ctx.rowItems("swords");
        List<Holder<TrimPattern>> patterns = patterns(ctx);
        List<Holder<TrimMaterial>> materials = trimMaterials(ctx);

        // Reihe 1: je Stufe ein voller Satz.
        int x = 1;
        int tiers = Math.min(Math.min(helmets.size(), chests.size()), Math.min(legs.size(), boots.size()));
        for (int i = 0; i < tiers; i++) {
            ItemStack sword = i < swords.size() ? new ItemStack(swords.get(i)) : ItemStack.EMPTY;
            c.stand(x, 0, 2, 180F, List.of(new ItemStack(helmets.get(i)), new ItemStack(chests.get(i)),
                    new ItemStack(legs.get(i)), new ItemStack(boots.get(i)), sword, ItemStack.EMPTY),
                    new ItemStack(chests.get(i)).getHoverName());
            x += 2;
        }

        // Enderit (oberste Stufe) mit den Aufwertungen: leuchtend, strahlend, beides.
        if (tiers > 0 && !patterns.isEmpty() && !materials.isEmpty()) {
            int top = tiers - 1;
            Holder<TrimPattern> pattern = patterns.getFirst();
            Holder<TrimMaterial> material = preferredMaterial(materials);
            String[][] variants = {
                    {"glowing", "Glowing trim"},
                    {"emitting", "Emitting trim (Radiance)"},
                    {"both", "Glowing + Emitting"},
                    {"pulsating", "Pulsating trim"},
                    {"pulsating_glow", "Glowing + Pulsating"}};
            for (String[] variant : variants) {
                List<ItemStack> gear = new ArrayList<>();
                for (Item piece : List.of(helmets.get(top), chests.get(top), legs.get(top), boots.get(top))) {
                    ItemStack stack = trimmed(piece, material, pattern);
                    boolean pulsating = variant[0].startsWith("pulsating");
                    if (variant[0].equals("glowing") || variant[0].equals("both")) {
                        GlowingTrimUtils.setGlowLevel(stack, 1);
                    }
                    if (variant[0].equals("pulsating_glow")) {
                        GlowingTrimUtils.setGlowLevel(stack, 1);
                    }
                    if (variant[0].equals("emitting") || variant[0].equals("both")) {
                        setEmission(stack, 5);
                    }
                    GlowingTrimUtils.setPulsating(stack, pulsating);
                    gear.add(stack);
                }
                gear.add(top < swords.size() ? new ItemStack(swords.get(top)) : ItemStack.EMPTY);
                gear.add(ItemStack.EMPTY);
                c.stand(x, 0, 2, 180F, gear, TcText.t("armour." + variant[0], variant[1]));
                x += 2;
            }
        }

        // Reihe 2: je Besatzmuster ein Enderit-Satz, Material reihum.
        if (tiers > 0) {
            int top = tiers - 1;
            int px = 1;
            for (int i = 0; i < patterns.size() && !materials.isEmpty(); i++) {
                Holder<TrimPattern> pattern = patterns.get(i);
                Holder<TrimMaterial> material = materials.get(i % materials.size());
                c.stand(px, 0, 5, 180F, List.of(trimmed(helmets.get(top), material, pattern),
                        trimmed(chests.get(top), material, pattern), trimmed(legs.get(top), material, pattern),
                        trimmed(boots.get(top), material, pattern), ItemStack.EMPTY, ItemStack.EMPTY),
                        pattern.value().description());
                px += 2;
            }
        }

        // Rueckwand: Vorlagen und Besatzmaterialien.
        List<ItemStack> templates = new ArrayList<>();
        for (Item item : sortedItems()) {
            if (item instanceof SmithingTemplateItem) {
                templates.add(new ItemStack(item));
            }
        }
        List<ItemStack> materialItems = new ArrayList<>();
        for (Item item : sortedItems()) {
            ItemStack stack = new ItemStack(item);
            if (stack.has(DataComponents.PROVIDES_TRIM_MATERIAL)) {
                materialItems.add(stack);
            }
        }
        c.title(0, TcCanvas.SHELF_LINES, wallZ, TcText.t("section.armour", "Armour & Trims"),
                TcText.t("section.armour.sub", "sets, upgrades, patterns"));
        // TZ22-2: die Besatzmuster zeigen die Staender der zweiten Reihe (je Muster ein Enderit-Satz); die Rahmenwand mit
        // denselben vier Teilen je Muster fiel weg. An der Wand: Vorlagen und Besatzmaterialien.
        int end = c.rowsPanel(0, TcCanvas.SHELF_LINES - 1, wallZ, List.of(
                new TcCanvas.Line(TcText.t("armour.templates", "Templates"), templates),
                new TcCanvas.Line(TcText.t("armour.materials", "Trim materials"), materialItems)));
        c.backWall(0, Math.max(end, x), wallZ, TcCanvas.SHELF_HEIGHT);
        return c;
    }

    private static Holder<TrimMaterial> preferredMaterial(List<Holder<TrimMaterial>> materials) {
        for (Holder<TrimMaterial> material : materials) {
            if (material.unwrapKey().map(k -> k.identifier().getPath().equals("amethyst")).orElse(false)) {
                return material;
            }
        }
        return materials.getFirst();
    }

    private static List<Holder<TrimPattern>> patterns(TcContext ctx) {
        List<Holder<TrimPattern>> out = new ArrayList<>();
        ctx.lookup().lookupOrThrow(Registries.TRIM_PATTERN).listElements().forEach(out::add);
        out.sort(Comparator.comparing(h -> h.unwrapKey().map(k -> k.identifier().toString()).orElse("")));
        return out;
    }

    private static List<Holder<TrimMaterial>> trimMaterials(TcContext ctx) {
        List<Holder<TrimMaterial>> out = new ArrayList<>();
        ctx.lookup().lookupOrThrow(Registries.TRIM_MATERIAL).listElements().forEach(out::add);
        out.sort(Comparator.comparing(h -> h.unwrapKey().map(k -> k.identifier().toString()).orElse("")));
        return out;
    }

    private static ItemStack trimmed(Item item, Holder<TrimMaterial> material, Holder<TrimPattern> pattern) {
        ItemStack stack = new ItemStack(item);
        if (stack.is(ItemTags.TRIMMABLE_ARMOR)) {
            stack.set(DataComponents.TRIM, new ArmorTrim(material, pattern));
        }
        return stack;
    }

    private static void setEmission(ItemStack stack, int level) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(GlowingTrimUtils.EMISSION_LEVEL_KEY, level);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    // =====================================================================================
    // 2. Verzauberte Buecher
    // =====================================================================================

    public static TcCanvas books(TcContext ctx) {
        TcCanvas c = new TcCanvas();
        int wallZ = 2;
        List<Holder<Enchantment>> all = new ArrayList<>();
        ctx.enchantmentLookup().listElements().forEach(all::add);
        all.sort(Comparator.comparing(h -> h.unwrapKey().map(k -> k.identifier()).orElseThrow(), TcContext.MOD_FIRST));
        List<ItemStack> books = new ArrayList<>();
        List<List<Component>> labels = new ArrayList<>();
        for (Holder<Enchantment> enchantment : all) {
            books.add(TcContext.book(enchantment));
            String namespace = enchantment.unwrapKey().map(k -> k.identifier().getNamespace()).orElse("?");
            labels.add(List.of(Enchantment.getFullname(enchantment, enchantment.value().getMaxLevel()),
                    TcText.lit(namespace)));
        }
        c.title(0, 4, wallZ, TcText.t("section.books", "Enchanted Books"),
                TcText.t("section.books.sub", "all guide books"));
        int end = c.frameGrid(1, 0, wallZ, books, labels, 2);
        c.backWall(0, end, wallZ, TcCanvas.SHELF_HEIGHT);
        return c;
    }

    // =====================================================================================
    // 3. Werkzeuge und Waffen
    // =====================================================================================

    /** Familien in der Reihenfolge des Tabs SimpleTools. */
    static final List<String> TOOL_FAMILIES = List.of("chisels", "color_brush", "building_wands", "sledgehammers", "pickaxes",
            "shovels", "hoes", "axes", "swords", "spears", "gadgets");

    public static TcCanvas tools(TcContext ctx) {
        TcCanvas c = new TcCanvas();
        int wallZ = 2;
        List<TcCanvas.Line> plain = new ArrayList<>();
        List<TcCanvas.Line> enchanted = new ArrayList<>();
        for (String family : TOOL_FAMILIES) {
            List<ItemStack> stacks = ctx.row(family);
            Component label = TcText.t("tools." + family, pretty(family));
            plain.add(new TcCanvas.Line(label, stacks));
            List<ItemStack> max = new ArrayList<>();
            for (ItemStack stack : stacks) {
                max.add(ctx.maxEnchanted(stack));
            }
            enchanted.add(new TcCanvas.Line(label, max));
        }
        // Alte Spachtel: nur noch fuer bestehende Welten registriert.
        List<ItemStack> legacy = new ArrayList<>();
        for (Item item : sortedModItems()) {
            if (item instanceof ChiselItem chisel && chisel.isDedicatedSpatula()) {
                legacy.add(new ItemStack(item));
            }
        }
        plain.add(new TcCanvas.Line(TcText.t("tools.legacy", "Legacy spatulas"), legacy));

        // TZ22-5: je Wand hoechstens vier Familien, weitere auf Regalen dahinter (vorher eine 15 hohe Wand).
        int top = TcCanvas.SHELF_LINES;
        c.title(0, top, wallZ, TcText.t("section.tools", "Tools & Weapons"), TcText.t("tools.plain", "plain"));
        int end = c.shelves(0, wallZ, plain);
        int x = end + 1;
        c.wallSign(x, top, wallZ, TcText.bold(TcText.t("tools.enchanted", "max. enchanted")));
        end = c.shelves(x, wallZ, enchanted);
        x = end + 1;
        c.wallSign(x, top, wallZ, TcText.bold(TcText.t("tools.variants", "Dev tab variants")),
                TcText.t("tools.variants.sub", "every exclusive set"));
        end = c.frameGrid(x, 0, wallZ, DevEnchantedTab.variants(ctx.lookup()), null, top);
        c.shelfWalls(0, end, wallZ, Math.max(TcCanvas.shelfCount(plain.size()), TcCanvas.shelfCount(enchanted.size())));
        return c;
    }

    // =====================================================================================
    // 4. Meissel-Tuerme
    // =====================================================================================

    /** Eine Kette des Meissels: Start, Folgebloecke, noetige Stufe. */
    record ChiselChain(List<Block> blocks, Item chisel, boolean touch, boolean cycle) {
    }

    public static List<ChiselChain> chiselChains(TcContext ctx) {
        List<ChiselItem> chisels = new ArrayList<>();
        for (Item item : ctx.rowItems("chisels")) {
            if (item instanceof ChiselItem chisel) {
                chisels.add(chisel);
            }
        }
        if (chisels.isEmpty()) {
            return List.of();
        }
        ChiselItem topChisel = chisels.getLast();
        Map<Block, Block> map = new LinkedHashMap<>(topChisel.getForwardMap());
        map.putAll(topChisel.getTouchForwardMap());
        map.entrySet().removeIf(e -> e.getKey() == e.getValue());
        List<Block> keys = new ArrayList<>(map.keySet());
        keys.sort(Comparator.comparing(b -> TcContext.id(b).toString()));
        Set<Block> targets = new HashSet<>(map.values());
        Set<Block> used = new HashSet<>();
        List<ChiselChain> out = new ArrayList<>();
        for (int pass = 0; pass < 2; pass++) {
            for (Block start : keys) {
                if (used.contains(start) || (pass == 0 && targets.contains(start))) {
                    continue;
                }
                List<Block> chain = new ArrayList<>();
                Block current = start;
                boolean cycle = false;
                while (current != null && chain.size() < 24) {
                    if (chain.contains(current)) {
                        cycle = current == start;
                        break;
                    }
                    chain.add(current);
                    used.add(current);
                    current = map.get(current);
                }
                Item needed = null;
                boolean touch = false;
                for (ChiselItem chisel : chisels) {
                    Block next = chisel.getForwardMap().get(start);
                    if (next != null && next != start) {
                        needed = chisel;
                        break;
                    }
                }
                if (needed == null) {
                    for (ChiselItem chisel : chisels) {
                        Block next = chisel.getTouchForwardMap().get(start);
                        if (next != null && next != start) {
                            needed = chisel;
                            touch = true;
                            break;
                        }
                    }
                }
                out.add(new ChiselChain(chain, needed == null ? topChisel : needed, touch, cycle));
            }
        }
        return out;
    }

    public static TcCanvas chisel(TcContext ctx) {
        TcCanvas c = new TcCanvas();
        postSign(c, 0, 0, TcText.bold(TcText.t("section.chisel", "Chisel Towers")),
                TcText.t("section.chisel.sub", "chisel the front block"), TcText.t("section.chisel.sub2", "tower = whole chain"));
        List<ChiselChain> chains = chiselChains(ctx);
        List<ItemStack> shown = new ArrayList<>();
        int perRow = 24;
        for (int i = 0; i < chains.size(); i++) {
            ChiselChain chain = chains.get(i);
            int x = 3 + 2 * (i % perRow);
            int z0 = 1 + 5 * (i / perRow);
            tower(c, x, z0, chain.blocks());
            Component[] lines = {
                    new ItemStack(chain.chisel()).getHoverName(),
                    chain.touch() ? TcText.t("chisel.touch", "+ Constructor's Touch") : Component.empty(),
                    TcText.t("chisel.steps", "%s blocks", chain.blocks().size()),
                    chain.cycle() ? TcText.t("chisel.cycle", "cycle") : Component.empty()};
            c.sign(x, 0, z0 + 1, Direction.NORTH, lines);
            // TZ22-2: der Meissel steht auf dem Schild; ein Rahmen nur am ersten Turm je Meissel (vorher einer je Turm).
            ItemStack tool = touchIfNeeded(ctx, new ItemStack(chain.chisel()), chain.touch());
            if (shown.stream().noneMatch(stack -> ItemStack.isSameItemSameComponents(stack, tool))) {
                shown.add(tool);
                c.frame(x, 1, z0, Direction.UP, tool);
            }
        }
        return c;
    }

    /** Testblock bei {@code z0}, Turm mit der ganzen Kette bei {@code z0 + 2}. */
    private static void tower(TcCanvas c, int x, int z0, List<Block> blocks) {
        c.place(x, 0, z0, blocks.getFirst());
        for (int y = 0; y < blocks.size(); y++) {
            c.place(x, y, z0 + 2, blocks.get(y));
        }
    }

    private static ItemStack touchIfNeeded(TcContext ctx, ItemStack stack, boolean touch) {
        if (!touch) {
            return stack;
        }
        return ctx.enchantment(ModEnchantments.CONSTRUCTORS_TOUCH).map(e -> TcContext.enchanted(stack, List.of(e))).orElse(stack);
    }

    /** Schild an einem zwei Bloecke hohen Pfosten; der Pfosten steht bei {@code z + 1}. */
    static void postSign(TcCanvas c, int x, int z, Component... lines) {
        c.place(x, 0, z + 1, TcCanvas.TRIM);
        c.place(x, 1, z + 1, TcCanvas.TRIM);
        c.sign(x, 1, z, Direction.NORTH, lines);
    }

    /** Pfosten mit Rahmen obenauf und Schild davor. */
    static void post(TcCanvas c, int x, int z, ItemStack stack, Component... lines) {
        postSign(c, x, z, lines);
        c.frame(x, 2, z + 1, Direction.UP, stack);
    }

    // =====================================================================================
    // 5. In-World-Umwandlungen: je eine Station
    // =====================================================================================

    /**
     * Aufwertungsketten nach Stufen (Nachtrag 13, Besitzer 06.10.: "alle In-World-Umwandlungen nach Tiers"): aus der Live-
     * Engine ({@link SledgehammerUpgrades#upgradeOf}) je Kette die Stufen der Reihe nach. Start ist ein Ausgangsblock, der
     * selbst kein Ergebnis einer Aufwertung ist; Bloecke der Familie immer, von Vanilla-Starts nur der erste je Ziel
     * (Kupfertruhe statt acht Varianten, ungefaerbte Shulkerkiste statt 17). Zusammenlaufende Ketten (klebriger Kolben)
     * enden am ersten schon gezeigten Block.
     */
    public static List<List<SledgehammerUpgrades.Upgrade>> upgradeChains() {
        Map<Block, SledgehammerUpgrades.Upgrade> byFrom = new LinkedHashMap<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            SledgehammerUpgrades.Upgrade upgrade = SledgehammerUpgrades.upgradeOf(block);
            if (upgrade != null && upgrade.from() == block) {
                byFrom.put(block, upgrade);
            }
        }
        Set<Block> results = new HashSet<>();
        byFrom.values().forEach(upgrade -> results.add(upgrade.to()));
        Set<Block> vanillaTargets = new HashSet<>();
        Set<Block> used = new HashSet<>();
        List<List<SledgehammerUpgrades.Upgrade>> chains = new ArrayList<>();
        for (Map.Entry<Block, SledgehammerUpgrades.Upgrade> entry : byFrom.entrySet()) {
            Block start = entry.getKey();
            if (results.contains(start)) {
                continue;
            }
            if (!TcContext.isFamily(TcContext.id(start)) && (!TcContext.isFamily(TcContext.id(entry.getValue().to()))
                    || !vanillaTargets.add(entry.getValue().to()))) {
                continue;
            }
            List<SledgehammerUpgrades.Upgrade> chain = new ArrayList<>();
            for (Block b = start; byFrom.containsKey(b) && used.add(b); b = byFrom.get(b).to()) {
                chain.add(byFrom.get(b));
            }
            if (!chain.isEmpty()) {
                chains.add(chain);
            }
        }
        return chains;
    }

    /** Spalten je Stufe in {@link #upgrades}: Weg-Schild, Block, Material, Hammer. */
    static final int UPGRADE_STEP = 4;

    /**
     * Abschnitt "upgrades": je Kette ({@link #upgradeChains}) eine Zeile an Regalwaenden (vier Zeilen je Wand, TZ22-5) - je
     * Stufe das Weg-Schild (TcWays, aus der Engine), der Block selbst (gesetzt, zum Draufhauen), Material x Menge und der
     * schwaechste passende Hammer im Rahmen; am Ende der Endblock.
     */
    public static TcCanvas upgrades(TcContext ctx) {
        TcCanvas c = new TcCanvas();
        List<List<SledgehammerUpgrades.Upgrade>> chains = upgradeChains();
        if (chains.isEmpty()) {
            return c;
        }
        List<Item> hammers = ctx.rowItems("sledgehammers");
        int width = 2;
        for (List<SledgehammerUpgrades.Upgrade> chain : chains) {
            width = Math.max(width, 1 + chain.size() * UPGRADE_STEP + 1);
        }
        int walls = TcCanvas.shelfCount(chains.size());
        int firstWallZ = 2;
        c.shelfWalls(0, width, firstWallZ, walls);
        c.title(0, TcCanvas.SHELF_LINES, firstWallZ, TcText.t("section.upgrades", "Upgrades by tier"),
                TcText.t("section.upgrades.sub", "nugget in off hand"));
        for (int i = 0; i < chains.size(); i++) {
            int wallZ = firstWallZ + i / TcCanvas.SHELF_LINES * TcCanvas.SHELF_PITCH;
            int y = TcCanvas.SHELF_LINES - 1 - i % TcCanvas.SHELF_LINES;
            int x = 1;
            for (SledgehammerUpgrades.Upgrade upgrade : chains.get(i)) {
                TcWays.Way way = TcWays.upgrade(upgrade.from(), hammers);
                if (way != null) {
                    c.waySign(x, y, wallZ, way, (Component) null);
                } else {
                    c.wallSign(x, y, wallZ, upgrade.from().getName());
                }
                c.place(x + 1, y, wallZ, upgradeState(upgrade.from()));
                c.wallFrame(x + 2, y, wallZ, new ItemStack(upgrade.nugget(), upgrade.materialCost()));
                for (Item candidate : hammers) {
                    if (SledgehammerUpgrades.hammerRank(candidate) >= upgrade.minHammerRank()) {
                        c.wallFrame(x + 3, y, wallZ, new ItemStack(candidate));
                        break;
                    }
                }
                x += UPGRADE_STEP;
            }
            Block last = chains.get(i).getLast().to();
            c.place(x, y, wallZ, upgradeState(last));
        }
        return c;
    }

    /** Ein Block der Aufwertungskette, Front zum Gang (Shulkerkisten und Kolben in sechs Richtungen). */
    private static BlockState upgradeState(Block block) {
        List<BlockState> states = Exhibits.blockStates(block);
        return states.isEmpty() ? facing(block.defaultBlockState(), Direction.NORTH) : states.getFirst();
    }

    public static TcCanvas inWorld(TcContext ctx) {
        TcCanvas c = new TcCanvas();
        int wallZ = 5;
        List<Item> hammers = ctx.rowItems("sledgehammers");
        ItemStack hammer = hammers.isEmpty() ? ItemStack.EMPTY : new ItemStack(hammers.getFirst());
        c.title(0, 4, wallZ, TcText.t("section.inworld", "In-World Stations"),
                TcText.t("section.inworld.sub", "one per transformation"));
        int x = 2;

        // a) Umformen mit dem Vorschlaghammer: Block -> Treppe -> Stufe.
        Map<Block, Block> reshape = new LinkedHashMap<>();
        for (Block[] pair : InWorldTransformations.reshapePairs(false)) {
            reshape.putIfAbsent(pair[0], pair[1]);
        }
        Set<Block> targets = new HashSet<>(reshape.values());
        List<List<Block>> chains = new ArrayList<>();
        for (Map.Entry<Block, Block> entry : reshape.entrySet()) {
            Block start = entry.getKey();
            boolean wanted = TcContext.isMod(TcContext.id(start)) || start == Blocks.STONE || start == Blocks.OAK_PLANKS;
            if (!wanted || targets.contains(start)) {
                continue;
            }
            List<Block> chain = new ArrayList<>();
            for (Block b = start; b != null && !chain.contains(b) && chain.size() < 4; b = reshape.get(b)) {
                chain.add(b);
            }
            chains.add(chain);
        }
        c.wallFrame(x, 3, wallZ, hammer);
        c.wallSign(x, 2, wallZ, TcText.bold(TcText.t("inworld.reshape", "Reshape")),
                TcText.t("inworld.reshape.sub", "hold right-click"), com.simplebuilding.version.McVersion.TRANSFORM_HINTS_AND_CORNERS
                        ? TcText.t("inworld.reshape.corner", "sneak = corner") : TcText.t("inworld.reshape.sub2", "sneak = back"));
        for (int i = 0; i < chains.size(); i++) {
            int tx = x + 1 + 2 * i;
            c.place(tx, 0, 1, chains.get(i).getFirst());
            for (int y = 0; y < chains.get(i).size(); y++) {
                c.place(tx, y, 3, chains.get(i).get(y));
            }
            c.sign(tx, 0, 2, Direction.NORTH, chains.get(i).getFirst().getName());
        }
        x += 2 + 2 * chains.size() + 1;

        // b) Diamantblock zerschlagen.
        // Erst ab der Eisenstufe (Besitzer 2026-09-29): der Rahmen zeigt den schwaechsten Hammer, der es kann.
        c.place(x, 0, 2, Blocks.DIAMOND_BLOCK);
        ItemStack crusher = hammers.stream().filter(com.simplebuilding.items.custom.SledgehammerItem::canCrushDiamondBlock)
                .findFirst().map(ItemStack::new).orElse(hammer);
        c.wallFrame(x, 2, wallZ, crusher);
        // TZ22-8: Schildtext aus den Daten (InWorldTransformations), nicht von Hand.
        TcWays.Way crush = TcWays.crush(Items.DIAMOND_BLOCK, hammers);
        if (crush != null) {
            c.waySign(x, 1, wallZ, crush, TcText.t("inworld.crush", "Crush"));
        }
        x += 3;

        // c) Maschinen-Aufwertung: eigener Abschnitt "upgrades" (N13, je Kette nach Stufen).

        // d) Scheren an Wolle.
        c.place(x, 0, 2, vanilla("white_wool"));
        c.wallFrame(x, 2, wallZ, new ItemStack(Items.SHEARS));
        TcWays.Way shear = TcWays.shear(vanilla("white_wool"));
        if (shear != null) {
            c.waySign(x, 1, wallZ, shear, TcText.t("inworld.shears", "Shears"));
        }
        x += 3;

        // e) Besatzvorlage aufwerten (nur abgelegt; die Rahmen zeigen nur Vorlage, Material und Ergebnis).
        Item template = null;
        for (Item item : sortedItems()) {
            if (SledgehammerEntityInteraction.isTrimTemplate(item)) {
                template = item;
                break;
            }
        }
        c.wallSign(x, 3, wallZ, TcText.bold(TcText.t("inworld.trim", "Trim template")),
                TcText.t("inworld.trim.sub", "place it, hammer it"), TcText.t("inworld.trim.sub2", "catalyst in off hand"));
        x++;
        for (Map.Entry<Item, Item> upgrade : trimUpgradesShown().entrySet()) {
            if (template != null) {
                c.wallFrame(x, 2, wallZ, new ItemStack(template));
            }
            c.wallFrame(x, 1, wallZ, new ItemStack(upgrade.getKey()));
            c.wallFrame(x, 0, wallZ, new ItemStack(upgrade.getValue()));
            x++;
        }
        c.wallFrame(x, 2, wallZ, hammer);
        x += 2;

        // f) Oktant im Kessel waschen.
        c.place(x, 0, 2, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        List<Item> washable = OctantCauldronWash.washableOctants();
        if (!washable.isEmpty()) {
            c.wallFrame(x, 2, wallZ, new ItemStack(washable.getFirst()));
        }
        TcWays.Way wash = washable.isEmpty() ? null : TcWays.wash(washable.getFirst());
        if (wash != null) {
            c.waySign(x, 1, wallZ, wash, TcText.t("inworld.wash", "Cauldron wash"));
        }
        x += 2;

        c.backWall(0, x, wallZ, 6);
        return c;
    }

    // =====================================================================================
    // 5b. Abgelegte Schmiedevorlagen
    // =====================================================================================

    /**
     * Abgelegte Schmiedevorlagen ({@code PlacedTemplates}): vorn eine Reihe aufwertbarer
     * Besatzvorlagen auf dem Boden zum Ausprobieren (Kit: Hammer + Glowstonestaub, Leuchttinte im
     * Inventar; drei Schlaege je Vorlage, Funken in der Naehe), dahinter je eine Aufwertungs- und die
     * beiden Mod-Vorlagen auf dem Boden und an der Wand.
     */
    public static TcCanvas templates(TcContext ctx) {
        TcCanvas c = new TcCanvas();
        int wallZ = 5;
        c.title(0, 4, wallZ, TcText.t("section.templates", "Placed Templates"),
                TcText.t("section.templates.sub", "sneak + use with a template"));
        List<Item> trims = new ArrayList<>();
        for (Item item : sortedItems()) {
            if (SledgehammerEntityInteraction.isTrimTemplate(item)) {
                trims.add(item);
            }
        }
        int x = 2;
        // a) Uebungsreihe: aufwertbare Besatzvorlagen flach auf dem Boden (Oberkante vom Gang weg).
        c.wallSign(x, 1, wallZ, TcText.bold(TcText.t("templates.hit", "Hammer them")),
                TcText.t("templates.hit.sub", "glowstone / glow ink"), TcText.t("templates.hit.sub2", "in off hand: 5 hits"));
        for (Map.Entry<Item, Item> upgrade : trimUpgradesShown().entrySet()) {
            c.wallFrame(x, 2, wallZ, new ItemStack(upgrade.getKey()));
            x++;
        }
        for (int i = 0; i < Math.min(4, trims.size()); i++) {
            placedTemplate(c, x + i, 0, 1, AttachFace.FLOOR, Direction.SOUTH, new ItemStack(trims.get(i)));
        }
        x += Math.min(4, trims.size()) + 1;

        // b) Alle Arten: auf dem Boden und an der Wand.
        c.wallSign(x, 1, wallZ, TcText.bold(TcText.t("templates.kinds", "Floor and wall")),
                TcText.t("templates.kinds.sub", "upgrade and trim"), TcText.t("templates.kinds.sub2", "templates, mod ones too"));
        x++;
        List<ItemStack> kinds = new ArrayList<>();
        kinds.add(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
        kinds.add(new ItemStack(ModItems.BASIC_UPGRADE_TEMPLATE));
        kinds.add(new ItemStack(ModItems.ENDERITE_UPGRADE_TEMPLATE));
        for (Item upgraded : upgradedTrimTemplates()) {
            kinds.add(new ItemStack(upgraded));
        }
        kinds.add(new ItemStack(ModItems.MAGNET));
        for (ItemStack kind : kinds) {
            placedTemplate(c, x, 0, 3, AttachFace.FLOOR, Direction.SOUTH, kind);
            c.place(x, 2, wallZ, TcCanvas.WALL);
            placedTemplate(c, x, 2, wallZ - 1, AttachFace.WALL, Direction.NORTH, kind);
            x++;
        }
        x++;

        // c) Blaupausen legen sich genauso ab (eigener Block placed_blueprint): Boden, Wand, Decke.
        c.wallSign(x, 1, wallZ, TcText.bold(TcText.t("templates.blueprint", "Blueprints too")),
                TcText.t("templates.blueprint.sub", "sneak + use:"), TcText.t("templates.blueprint.sub2", "floor, wall, ceiling"));
        x++;
        ItemStack blueprint = new ItemStack(ModItems.BLUEPRINT);
        placedTemplate(c, x, 0, 3, AttachFace.FLOOR, Direction.SOUTH, blueprint);
        c.place(x, 2, wallZ, TcCanvas.WALL);
        placedTemplate(c, x, 2, wallZ - 1, AttachFace.WALL, Direction.NORTH, blueprint);
        c.place(x + 1, 3, 3, TcCanvas.WALL);
        placedTemplate(c, x + 1, 2, 3, AttachFace.CEILING, Direction.SOUTH, blueprint);
        x += 3;
        c.backWall(0, x, wallZ, 6);
        return c;
    }

    /**
     * Eine abgelegte Vorlage oder Blaupause: Block samt Lage, der Stapel selbst kommt beim Bau in die
     * Block-Entity.
     */
    private static void placedTemplate(TcCanvas c, int x, int y, int z, AttachFace face, Direction facing, ItemStack template) {
        net.minecraft.world.level.block.Block block = com.simplebuilding.util.PlacedTemplates.placedBlockFor(template);
        if (block == null && com.simplebuilding.util.PlacedTemplates.leftToSimpleTrims(template)) {
            // 26.3 (N18): trim templates lie in Simple Trims' own block (same face/facing properties, found by id).
            block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(SIMPLE_TRIMS_PLACED).orElse(null);
        }
        c.place(x, y, z, (block == null ? ModBlocks.PLACED_SMITHING_TEMPLATE : block).defaultBlockState()
                .setValue(net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock.FACE, face)
                .setValue(net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock.FACING, facing));
        c.contents(x, y, z, List.of(template));
    }

    /** Glowing, Emitting, Pulsating template: SB's own (26.2) or Simple Trims' by id (26.3, missing ids skipped). */
    static List<Item> upgradedTrimTemplates() {
        if (com.simplebuilding.version.McVersion.TRIMS_IN_SB) {
            return List.of(ModItems.GLOWING_TRIM_TEMPLATE, ModItems.EMITTING_TRIM_TEMPLATE, ModItems.PULSATING_TRIM_TEMPLATE);
        }
        List<Item> out = new ArrayList<>();
        for (String path : List.of("glowing_trim_template", "emitting_trim_template", "pulsating_trim_template")) {
            net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("simpletrims", path)).ifPresent(out::add);
        }
        return out;
    }

    /** Simple Trims' placed template block (26.3); {@code TestCentreBuilder} fills it through its saved key {@code Template}. */
    public static final net.minecraft.resources.Identifier SIMPLE_TRIMS_PLACED =
            net.minecraft.resources.Identifier.fromNamespaceAndPath("simpletrims", "placed_template");

    /**
     * Catalyst -> result of forging a placed trim template: SimpleBuilding's table on 26.2; on 26.3 Simple Trims'
     * templates by id (glow ink: Glowing, glowstone dust: Emitting), empty without the module.
     */
    static Map<Item, Item> trimUpgradesShown() {
        if (com.simplebuilding.version.McVersion.TRIMS_IN_SB) return SledgehammerEntityInteraction.trimUpgrades();
        Map<Item, Item> shown = new java.util.LinkedHashMap<>();
        for (var entry : Map.of(Items.GLOW_INK_SAC, "glowing_trim_template", Items.GLOWSTONE_DUST, "emitting_trim_template").entrySet()) {
            net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("simpletrims", entry.getValue())).ifPresent(item -> shown.put(entry.getKey(), item));
        }
        return shown;
    }

    // =====================================================================================
    // 6. Lager: Buendel, Koecher, Rucksaecke
    // =====================================================================================

    static final List<String> STORAGE_FAMILIES = List.of("bundles_and_quivers", "backpacks");
    static final List<DyeColor> DYE_EXAMPLES = List.of(DyeColor.RED, DyeColor.LIME, DyeColor.BLUE, DyeColor.YELLOW);

    /**
     * Vanilla-Faerbefarbe der Beispiel-Farbstoffe, fest statt ueber DyeColor: 26.4 hat die Farbmethoden
     * aus DyeColor entfernt, und die Werte sind seit Jahren dieselben (DyeColor#getTextureDiffuseColor).
     */
    private static int dyeRgb(DyeColor color) {
        return switch (color) {
            case RED -> 0xB02E26;
            case LIME -> 0x80C71F;
            case BLUE -> 0x3C44AA;
            case YELLOW -> 0xFED83D;
            default -> 0xFFFFFF;
        };
    }

    public static TcCanvas storage(TcContext ctx) {
        TcCanvas c = new TcCanvas();
        int wallZ = 4;
        List<TcCanvas.Line> lines = new ArrayList<>();
        int bx = 1;
        for (String family : STORAGE_FAMILIES) {
            List<ItemStack> stacks = ctx.row(family);
            Component label = TcText.t("storage." + family, pretty(family));
            lines.add(new TcCanvas.Line(label, stacks));
            List<ItemStack> enchanted = new ArrayList<>();
            List<ItemStack> dyed = new ArrayList<>();
            for (ItemStack stack : stacks) {
                enchanted.addAll(ctx.allEnchantedVariants(stack));
                if (TcContext.isMod(TcContext.id(stack.getItem()))) {
                    for (DyeColor color : DYE_EXAMPLES) {
                        ItemStack copy = stack.copy();
                        copy.set(DataComponents.DYED_COLOR, new DyedItemColor(dyeRgb(color)));
                        dyed.add(copy);
                    }
                }
                // Blockformen (Rucksaecke) stehen zusaetzlich auf dem Boden.
                if (stack.getItem() instanceof BlockItem blockItem && TcContext.isMod(TcContext.id(stack.getItem()))) {
                    c.place(bx, 0, 1, facing(blockItem.getBlock().defaultBlockState(), Direction.NORTH));
                    bx += 2;
                }
            }
            lines.add(new TcCanvas.Line(TcText.t("storage.enchanted", "%s, enchanted", label), enchanted));
            if (!dyed.isEmpty()) {
                lines.add(new TcCanvas.Line(TcText.t("storage.dyed", "%s, dyed", label), dyed));
            }
        }
        bx = placedBundles(c, bx);
        if (com.simplebuilding.version.McVersion.TRAPPED_TIERED_CHESTS) {
            lines.add(new TcCanvas.Line(TcText.t("storage.trapped_chests", "Trapped chests"), ctx.row("trapped_chests")));
            for (String tier : com.simplebuilding.util.StorageRefs.TIERS) {
                Block chest = com.simplebuilding.util.StorageRefs.trappedChestBlock(tier); // 26.3: Simple Storage's (split stage 8b)
                c.place(bx, 0, 1, facing(chest.defaultBlockState(), Direction.NORTH));
                c.place(bx + 1, 0, 1, Blocks.REDSTONE_LAMP);
                c.sign(bx, 0, 2, Direction.NORTH, chest.getName());
                bx += 3;
            }
        }
        c.title(0, TcCanvas.SHELF_LINES, wallZ, TcText.t("section.storage", "Storage"),
                TcText.t("section.storage.sub", "plain, enchanted, dyed"));
        int end = c.shelves(0, wallZ, lines);
        c.shelfWalls(0, Math.max(end, bx), wallZ, TcCanvas.shelfCount(lines.size()));
        return c;
    }

    /**
     * Abgestellte Buendel ({@code PlacedBundles}) neben den Rucksaecken: jede Stufe mit etwas Inhalt
     * (schleichend hinsehen zeigt das oberste Item, Rechtsklick nimmt es), dazu ein gefaerbtes und ein
     * farbiges Vanilla-Buendel. Liefert die naechste freie x-Position.
     */
    /** {@code state} with the property {@code name} set to {@code value} by name (no class of the owning mod needed). */
    private static net.minecraft.world.level.block.state.BlockState withProperty(net.minecraft.world.level.block.state.BlockState state, String name, String value) {
        net.minecraft.world.level.block.state.properties.Property<?> property = state.getBlock().getStateDefinition().getProperty(name);
        return property == null ? state : withValue(state, property, value);
    }

    private static <T extends Comparable<T>> net.minecraft.world.level.block.state.BlockState withValue(
            net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.block.state.properties.Property<T> property, String value) {
        return property.getValue(value).map(v -> state.setValue(property, v)).orElse(state);
    }

    private static int placedBundles(TcCanvas c, int bx) {
        c.sign(bx, 0, 2, Direction.NORTH, TcText.bold(TcText.t("storage.placed_bundle", "Placed bundles")),
                TcText.t("storage.placed_bundle.sub", "sneak + wheel: pick item"),
                TcText.t("storage.placed_bundle.sub2", "right-click out, sneak: put in"));
        bx++;
        List<ItemStack> bundles = new ArrayList<>();
        bundles.add(new ItemStack(Items.BUNDLE));
        // Split stage 8c-1: by id (on 26.3 the bundles and the placed bundle are Simple Storage's; its block entity is filled by key).
        bundles.add(new ItemStack(com.simplebuilding.util.StorageRefs.item("reinforced_bundle")));
        bundles.add(new ItemStack(com.simplebuilding.util.StorageRefs.item("netherite_bundle")));
        bundles.add(new ItemStack(com.simplebuilding.util.StorageRefs.item("enderite_bundle")));
        ItemStack dyed = new ItemStack(com.simplebuilding.util.StorageRefs.item("reinforced_bundle"));
        dyed.set(DataComponents.DYED_COLOR, new DyedItemColor(dyeRgb(DyeColor.LIME)));
        bundles.add(dyed);
        // Per Id: 26.2 fuehrt die farbigen Buendel als ColorCollection, 1.21.11 je Farbe ein Feld.
        bundles.add(new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
                net.minecraft.resources.Identifier.withDefaultNamespace("blue_bundle"))));
        List<ItemStack> sample = List.of(new ItemStack(Items.TORCH, 16), new ItemStack(Items.APPLE, 3),
                new ItemStack(Items.COBBLESTONE, 32), new ItemStack(Items.FEATHER, 5));
        for (ItemStack bundle : bundles) {
            com.simplebuilding.util.PlacedBundles.setContents(bundle, sample);
            String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(bundle.getItem()).getPath();
            String tier = path.endsWith("_bundle") && !"minecraft".equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(bundle.getItem()).getNamespace())
                    ? path.substring(0, path.length() - "_bundle".length()) : "bundle";
            net.minecraft.world.level.block.state.BlockState state = com.simplebuilding.util.StorageRefs.placedBundle().defaultBlockState();
            state = withProperty(state, "tier", tier);
            state = withProperty(state, "dyed", String.valueOf(com.simplebuilding.util.PlacedBundles.dyeColor(bundle) != com.simplebuilding.util.DyedStorage.UNDYED));
            c.place(bx, 0, 1, state);
            c.contents(bx, 0, 1, List.of(bundle));
            bx += 2;
        }
        return bx;
    }

    // =====================================================================================
    // 7. Essen und Rohstoffe
    // =====================================================================================

    public static TcCanvas food(TcContext ctx) {
        TcCanvas c = new TcCanvas();
        int wallZ = 2;
        List<ItemStack> items = new ArrayList<>();
        for (Item item : sortedModItems()) {
            ItemStack stack = new ItemStack(item);
            if (stack.has(DataComponents.FOOD)) {
                items.add(stack);
            }
        }
        for (Item item : List.of(Items.APPLE, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE, Items.CARROT, Items.GOLDEN_CARROT)) {
            items.add(new ItemStack(item));
        }
        List<List<Component>> labels = new ArrayList<>();
        for (ItemStack stack : items) {
            labels.add(List.of(stack.getHoverName()));
        }
        c.title(0, 3, wallZ, TcText.t("section.food", "Food"), TcText.t("section.food.sub", "eat to test"));
        int end = c.frameGrid(1, 0, wallZ, items, labels, 2);
        c.backWall(0, end, wallZ, 5);
        return c;
    }

    /** Befiederungstisch (B14): der Tisch zum Ausprobieren und alle Pfeile, eine Zeile je Spitze. */
    public static TcCanvas arrows(TcContext ctx) {
        TcCanvas c = new TcCanvas();
        int wallZ = 2;
        if (!com.simplebuilding.version.McVersion.FLETCHING) {
            return c;
        }
        List<TcCanvas.Line> lines = new ArrayList<>();
        for (var row : ModItemGroupsContent.arrowsRows()) {
            lines.add(new TcCanvas.Line(row.stacks().get(0).getHoverName(), row.stacks()));
        }
        c.title(0, TcCanvas.SHELF_LINES, wallZ, TcText.t("section.arrows", "Arrows"), TcText.t("section.arrows.sub", "Fletching Table"));
        int end = c.shelves(0, wallZ, lines);
        c.place(1, 0, 0, net.minecraft.world.level.block.Blocks.FLETCHING_TABLE);
        c.shelfWalls(0, end, wallZ, TcCanvas.shelfCount(lines.size()));
        return c;
    }

    public static TcCanvas materials(TcContext ctx) {
        TcCanvas c = new TcCanvas();
        int wallZ = 2;
        // SimpleMaterials und die seit 2026-10-02 eigene Nahrung (SimpleFood) an einer Wand, wie bisher.
        List<ItemStack> items = new ArrayList<>(ctx.tab(ModItemGroupsContent.Tab.MATERIALS));
        items.addAll(ctx.tab(ModItemGroupsContent.Tab.FOOD));
        List<List<Component>> labels = new ArrayList<>();
        for (ItemStack stack : items) {
            labels.add(List.of(stack.getHoverName()));
        }
        c.title(0, 4, wallZ, TcText.t("section.materials", "Materials"), TcText.t("section.materials.sub", "tab SimpleMaterials"));
        if (ModBlocks.PLACED_SMALL_PARTS != null) {
            // Kleinteile auf einem Fleck (bis zu 4, gemischt): ein Ei allein, ein voller Mix, drei Eier, zwei Vanilla-Teile.
            // Schleichen + Rechtsklick legt dazu; Abbauen mit / ohne Behutsamkeit zeigt Eier zurueck / Zerbrechen.
            List<List<ItemStack>> piles = List.of(
                    List.of(new ItemStack(net.minecraft.world.item.Items.EGG)),
                    List.of(new ItemStack(ModItems.STONE_PEBBLE), new ItemStack(ModItems.FLINT_CHIP), new ItemStack(ModItems.FLINT_CHIP),
                            new ItemStack(net.minecraft.world.item.Items.BLUE_EGG)),
                    List.of(new ItemStack(net.minecraft.world.item.Items.BROWN_EGG), new ItemStack(net.minecraft.world.item.Items.EGG),
                            new ItemStack(net.minecraft.world.item.Items.BLUE_EGG)),
                    List.of(new ItemStack(net.minecraft.world.item.Items.STICK), new ItemStack(net.minecraft.world.item.Items.IRON_INGOT)));
            int x = 1;
            for (List<ItemStack> pile : piles) {
                c.place(x, 0, 0, ModBlocks.PLACED_SMALL_PARTS);
                c.contents(x++, 0, 0, pile);
            }
            // Altbestand: ein einzeln gelegtes Ei aus alten Welten (wird zum Haeufchen, sobald man etwas dazulegt).
            c.place(6, 0, 0, ModBlocks.PLACED_EGG.defaultBlockState()
                    .setValue(com.simplebuilding.blocks.custom.PlacedEggBlock.EGG, com.simplebuilding.blocks.custom.PlacedEggBlock.Egg.BLUE));
        }
        if (ModBlocks.IRON_POLE != null) {
            // Eisen-, Gold-, Diamant-, Netherit- und Enderitstange aufgestellt (ziehen im Gewitter Blitze in 32, 64, 80,
            // 96 bzw. 128 Bloecken an), nach einer Luecke hinter dem einzelnen Ei (x = 6).
            c.place(8, 0, 0, ModBlocks.IRON_POLE);
            c.place(9, 0, 0, ModBlocks.GOLD_POLE);
            c.place(10, 0, 0, ModBlocks.DIAMOND_POLE);
            c.place(11, 0, 0, ModBlocks.NETHERITE_POLE);
            c.place(12, 0, 0, ModBlocks.ENDERITE_POLE);
        }
        if (com.simplebuilding.version.McVersion.SILENT_DANDELION) {
            c.place(13, -1, 0, Blocks.DIRT);
            c.place(13, 0, 0, ModBlocks.SILENT_DANDELION);
            c.place(14, 0, 0, ModBlocks.POTTED_SILENT_DANDELION);
            c.place(14, 0, 2, TcCanvas.TRIM);
            c.sign(14, 0, 1, Direction.NORTH,
                    TcText.t("silent_dandelion.use", "Use on a mob"),
                    TcText.t("silent_dandelion.toggle", "Sound on / off"),
                    TcText.t("silent_dandelion.wait", "Wait 2 seconds"),
                    TcText.t("silent_dandelion.decoration", "Planted: decoration"));
            c.place(15, 0, 0, ModBlocks.PLACED_SMALL_PARTS);
            c.contents(15, 0, 0, List.of(new ItemStack(ModItems.YARN_BALL)));
        }
        int end = c.frameGrid(1, 0, wallZ, items, labels, 2);
        c.backWall(0, end, wallZ, TcCanvas.SHELF_HEIGHT);
        return c;
    }

    // =====================================================================================
    // 8. Bloecke: Musterwand, Schachbrett-Boeden, Schwebe-Sand
    // =====================================================================================

    /**
     * Baubloecke (TZ22-5, Besitzer 22.10.: "gruppiert statt einer langen Reihe"): je Tab-Zeile (Astralit, Nihilith,
     * Enderquarz, Schachbretter, Scheiben ...) eine Gruppe im Bodenraster wie im Holzbereich ({@link TcCanvas#exhibition});
     * die Holzwerk- und Schach-Zeilen fallen weg, sie stehen schon im Holzbereich bzw. an der Schach-Station. Die Schachbretter liegen zusaetzlich als 3 x 3
     * Bodenflaechen in einem Feld daneben.
     */
    public static TcCanvas blocks(TcContext ctx) {
        TcCanvas c = new TcCanvas();
        int wallZ = TcCanvas.EXHIBIT_WALL_Z;
        c.title(0, 1, wallZ, TcText.t("section.blocks", "Building Blocks"), TcText.t("section.blocks.sub", "tab SimpleBuilding"));
        List<TcCanvas.Group> groups = new ArrayList<>();
        List<Block> checkers = new ArrayList<>();
        for (com.simplebuilding.items.CreativeTabLayout.Row row : ModItemGroupsContent.buildingBlocksRows()) {
            if (row.name().startsWith("woodwork_") || row.name().startsWith("chess_")) {
                continue;
            }
            List<ItemStack> stacks = row.stacks().stream().filter(stack -> !stack.isEmpty() && !TcContext.isSpacer(stack.getItem())).toList();
            groups.add(new TcCanvas.Group(TcText.t("blocks.row." + row.name(), pretty(row.name())), stacks));
            for (ItemStack stack : stacks) {
                if (stack.getItem() instanceof BlockItem blockItem && TcContext.id(blockItem.getBlock()).getPath().endsWith("_checker")) {
                    checkers.add(blockItem.getBlock());
                }
            }
        }
        int x = c.exhibition(1, groups);
        // Schachbretter als Bodenflaechen: sechs je Reihe, zwei Reihen (z 1-3 und 5-7), eine Spalte Luft dazwischen.
        int fieldX = x + 1;
        for (int i = 0; i < checkers.size(); i++) {
            int fx = fieldX + (i % 6) * 4;
            int fz = 1 + ((i / 6) % 2) * 4;
            c.fill(fx, -1, fz, fx + 2, -1, fz + 2, checkers.get(i).defaultBlockState());
        }
        int end = checkers.isEmpty() ? x : fieldX + Math.min(6, checkers.size()) * 4 - 1;
        c.backWall(0, end, wallZ, TcCanvas.EXHIBIT_WALL_HEIGHT);
        return c;
    }

    /** Dunkelraum mit Baulichtern: Monster duerfen trotz Licht spawnen. */
    public static TcCanvas lightRoom(TcContext ctx) {
        TcCanvas c = new TcCanvas();
        BlockState wall = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        int size = 13;
        int z0 = 1;
        for (int x = 0; x < size; x++) {
            for (int z = z0; z < z0 + size; z++) {
                c.place(x, -1, z, Blocks.STONE);
                c.place(x, 5, z, wall);
                boolean edge = x == 0 || x == size - 1 || z == z0 || z == z0 + size - 1;
                for (int y = 0; y < 5; y++) {
                    if (edge) {
                        c.place(x, y, z, wall);
                    }
                }
            }
        }
        // Tuer in der Vorderwand.
        int door = size / 2;
        BlockState lower = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH);
        c.place(door, 0, z0, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        c.place(door, 1, z0, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        for (int x = 3; x < size - 1; x += 4) {
            for (int z = z0 + 3; z < z0 + size - 1; z += 4) {
                c.place(x, 4, z, ModBlocks.CONSTRUCTION_LIGHT);
            }
        }
        c.anchor("mob_spawn", door, 0, z0 + size / 2);
        c.sign(door - 2, 1, z0 - 1, Direction.NORTH, TcText.bold(TcText.t("section.lightroom", "Dark Room")),
                TcText.t("section.lightroom.sub", "construction lights"), TcText.t("section.lightroom.sub2", "mobs may spawn"));
        return c;
    }

    // =====================================================================================
    // 9. Maschinen: Trichterketten in Oefen, Kolben
    // =====================================================================================

    public static TcCanvas machines(TcContext ctx) {
        TcCanvas c = new TcCanvas();
        int wallZ = 2;
        List<Item> hoppers = ctx.rowItems("hoppers");
        Map<String, Item> inputs = Map.of("furnaces", Items.COBBLESTONE, "smokers", Items.BEEF, "blast_furnaces", Items.RAW_IRON);
        c.title(0, 5, wallZ, TcText.t("section.machines", "Machines"), TcText.t("section.machines.sub", "hopper -> machine -> hopper"));
        int x = 1;
        for (String family : List.of("furnaces", "smokers", "blast_furnaces")) {
            List<Item> tier = ctx.rowItems(family);
            for (int i = 0; i < tier.size(); i++) {
                if (!(tier.get(i) instanceof BlockItem machineItem)) {
                    continue;
                }
                Item hopperItem = i < hoppers.size() ? hoppers.get(i) : Items.HOPPER;
                BlockState hopper = ((BlockItem) hopperItem).getBlock().defaultBlockState();
                int z = wallZ - 1;
                c.place(x, 0, z, facing(Blocks.CHEST.defaultBlockState(), Direction.NORTH));
                c.place(x, 1, z, with(hopper, BlockStateProperties.FACING_HOPPER, Direction.DOWN));
                c.place(x, 2, z, facing(machineItem.getBlock().defaultBlockState(), Direction.NORTH));
                c.place(x, 3, z, with(hopper, BlockStateProperties.FACING_HOPPER, Direction.DOWN));
                c.place(x, 4, z, facing(Blocks.CHEST.defaultBlockState(), Direction.NORTH));
                c.contents(x, 4, z, List.of(new ItemStack(inputs.get(family), 64)));
                c.place(x + 1, 2, z, with(hopper, BlockStateProperties.FACING_HOPPER, Direction.WEST));
                c.place(x + 1, 3, z, facing(Blocks.CHEST.defaultBlockState(), Direction.NORTH));
                c.contents(x + 1, 3, z, List.of(new ItemStack(Items.COAL, 64)));
                c.wallSign(x + 1, 4, wallZ, machineItem.getBlock().getName(), new ItemStack(hopperItem).getHoverName());
                x += 3;
            }
        }
        // TZ22-5: Wand 6 hoch (Schilder auf y 4, Titel auf y 5).
        c.backWall(0, x, wallZ, TcCanvas.SHELF_HEIGHT);

        // Kolben: jede Stufe mit Hebel obenauf, Bahn nach Sueden. Die Reihe steht NEBEN den Maschinen
        // an der Gangkante - hinter der Rueckwand (frueher wallZ + 3) sah sie vom Gang aus niemand.
        int pz = 1;
        int px0 = x + 2;
        int px = px0 + 1;
        c.sign(px0, 1, pz - 1, Direction.NORTH, TcText.bold(TcText.t("machines.pistons", "Pistons")),
                TcText.t("machines.pistons.sub", "flip the lever"), TcText.t("machines.pistons.sub2", "13 blocks: vanilla fails"));
        c.place(px0, 1, pz, TcCanvas.TRIM);
        c.place(px0, 0, pz, TcCanvas.TRIM);
        for (Item item : ctx.rowItems("pistons")) {
            if (!(item instanceof BlockItem blockItem)) {
                continue;
            }
            Block piston = blockItem.getBlock();
            String path = TcContext.id(piston).getPath();
            c.place(px, 0, pz, with(piston.defaultBlockState(), BlockStateProperties.FACING, Direction.SOUTH));
            c.place(px, 1, pz, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR)
                    .setValue(LeverBlock.FACING, Direction.SOUTH));
            Component hint;
            if (path.startsWith("enderite")) {
                // Durchbruch: unzerstoerbarer Block vor dem Kolben, Redstoneblock als Brennstoff dahinter.
                c.place(px, 0, pz + 1, Blocks.REINFORCED_DEEPSLATE);
                c.place(px + 1, 0, pz - 1, facing(Blocks.CHEST.defaultBlockState(), Direction.NORTH));
                c.contents(px + 1, 0, pz - 1, List.of(new ItemStack(Items.REDSTONE_BLOCK, 16)));
                hint = TcText.t("machines.breach", "redstone block behind");
            } else if (path.startsWith("netherite")) {
                c.place(px, 0, pz + 1, Blocks.STONE);
                hint = TcText.t("machines.breaker", "breaks the block");
            } else {
                for (int i = 1; i <= 13; i++) {
                    c.place(px, 0, pz + i, Blocks.STONE);
                }
                hint = TcText.t("machines.push13", "pushes 13?");
            }
            c.sign(px, 0, pz - 1, Direction.NORTH, piston.getName(), hint);
            px += 3;
        }

        // Auto-Schmied: Truhe -> Trichter -> Schmied -> Truhe davor, Hebel daneben (eine Flanke = ein Schmiedevorgang).
        if (ModBlocks.AUTO_SMITHER != null) {
            int sx = px + 1;
            c.place(sx, 0, pz, with(ModBlocks.AUTO_SMITHER.defaultBlockState(), BlockStateProperties.FACING, Direction.SOUTH));
            c.place(sx, 0, pz + 1, facing(Blocks.CHEST.defaultBlockState(), Direction.NORTH));
            c.place(sx, 1, pz, with(Blocks.HOPPER.defaultBlockState(), BlockStateProperties.FACING_HOPPER, Direction.DOWN));
            c.place(sx, 2, pz, facing(Blocks.CHEST.defaultBlockState(), Direction.NORTH));
            c.contents(sx, 2, pz, List.of(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 8), new ItemStack(Items.DIAMOND_SWORD),
                    new ItemStack(Items.DIAMOND_PICKAXE), new ItemStack(Items.DIAMOND_CHESTPLATE), new ItemStack(Items.NETHERITE_INGOT, 8)));
            c.place(sx + 1, 0, pz, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR)
                    .setValue(LeverBlock.FACING, Direction.SOUTH));
            c.sign(sx, 0, pz - 1, Direction.NORTH, ModBlocks.AUTO_SMITHER.getName(),
                    TcText.t("machines.auto_smither", "flip the lever: smiths once"));
        }
        // Autonomer Crafter: Truhe -> Trichter -> Crafter (Bretter als Rezept) -> Trichter -> Truhe; Hebel = Redstone-Stopp.
        if (ModBlocks.AUTONOMOUS_CRAFTER != null) {
            int ax = px + 3;
            c.place(ax, 0, pz, facing(Blocks.CHEST.defaultBlockState(), Direction.NORTH));
            c.place(ax, 1, pz, with(Blocks.HOPPER.defaultBlockState(), BlockStateProperties.FACING_HOPPER, Direction.DOWN));
            c.place(ax, 2, pz, ModBlocks.AUTONOMOUS_CRAFTER.defaultBlockState());
            c.contents(ax, 2, pz, List.of(new ItemStack(Items.OAK_PLANKS, 16), ItemStack.EMPTY, ItemStack.EMPTY,
                    new ItemStack(Items.OAK_PLANKS, 16)));
            c.place(ax + 1, 2, pz, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL)
                    .setValue(LeverBlock.FACING, Direction.EAST));
            c.sign(ax, 0, pz - 1, Direction.NORTH, ModBlocks.AUTONOMOUS_CRAFTER.getName(),
                    TcText.t("machines.autonomous_crafter", "crafts into the hopper; lever stops it"));
        }
        // Separate 9x9 exhibit bays leave room for posts, cloth, ropes and a front aisle.
        if (!ModBlocks.HAMMOCKS.isEmpty()) {
            int hx = ModBlocks.AUTONOMOUS_CRAFTER != null ? px + 6 : ModBlocks.AUTO_SMITHER != null ? px + 4 : px + 1;
            c.sign(hx, 0, 0, Direction.NORTH, TcText.bold(TcText.t("machines.hammocks", "Hammocks")),
                    TcText.t("machines.hammocks.sub", "rest: time runs faster"));
            int[][] spans = {{3, 0}, {4, 0}, {5, 0}, {0, 3}, {0, 4}, {0, 5},
                    {3, 3}, {4, 4}, {5, 5}, {3, 1}, {4, 1}, {5, 2}, {3, 2}, {4, 3}, {-3, 2}};
            int index = 0;
            for (int[] span : spans) {
                hammockExhibit(c, hx, index++, span[0], span[1], Blocks.OAK_FENCE.defaultBlockState(),
                        Blocks.OAK_FENCE.getName());
            }
            hammockExhibit(c, hx, index++, 3, 2, Blocks.OAK_LOG.defaultBlockState(), Blocks.OAK_LOG.getName());
            if (ModBlocks.STANDING_ROD != null) {
                for (var rod : com.simplebuilding.blocks.custom.StandingRodBlock.Rod.values()) {
                    hammockExhibit(c, hx, index++, 3, 2, ModBlocks.STANDING_ROD.defaultBlockState()
                            .setValue(com.simplebuilding.blocks.custom.StandingRodBlock.ROD, rod),
                            new ItemStack(rod.item()).getHoverName());
                }
            }
            if (ModBlocks.NETHERITE_POLE != null) {
                hammockExhibit(c, hx, index, 3, 2, ModBlocks.NETHERITE_POLE.defaultBlockState()
                        .setValue(BlockStateProperties.FACING, Direction.UP), ModBlocks.NETHERITE_POLE.getName());
            }
        }
        return c;
    }

    private static void hammockExhibit(TcCanvas c, int hx, int index, int dx, int dz,
                                       BlockState post, Component anchorName) {
        int x = hx + 1 + (index % 6) * 9;
        int z = 2 + (index / 6) * 9;
        var spot = new com.simplebuilding.blocks.custom.HammockLayout.Spot(
                new BlockPos(x - Math.min(0, dx), 1, z), dx, dz);
        for (BlockPos anchor : List.of(spot.anchor(), spot.otherAnchor())) {
            c.place(anchor.getX(), 0, anchor.getZ(), post.is(ModBlocks.NETHERITE_POLE)
                    ? Blocks.OAK_LOG.defaultBlockState() : post);
            c.place(anchor.getX(), 1, anchor.getZ(), post);
        }
        Block cloth = ModBlocks.HAMMOCKS.get(index % ModBlocks.HAMMOCKS.size());
        c.hammock(spot, cloth);
        c.sign(x, 0, z - 1, Direction.NORTH, TcText.lit(dx + ":" + dz),
                TcText.t("machines.hammocks.gap", "%s free (main axis)", Math.max(Math.abs(dx), Math.abs(dz)) - 1),
                anchorName, cloth.getName());
    }

    // =====================================================================================
    // 10. Erzdetektor-Feld
    // =====================================================================================

    static final int[] ORE_DISTANCES = {3, 7, 11, 15};

    public static TcCanvas ores(TcContext ctx) {
        TcCanvas c = new TcCanvas();
        Map<Block, List<Block>> groups = new LinkedHashMap<>();
        groups.put(Blocks.STONE, new ArrayList<>());
        groups.put(Blocks.DEEPSLATE, new ArrayList<>());
        groups.put(Blocks.NETHERRACK, new ArrayList<>());
        groups.put(Blocks.END_STONE, new ArrayList<>());
        List<Block> ores = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            String path = TcContext.id(block).getPath();
            if (path.endsWith("_ore") || block == Blocks.ANCIENT_DEBRIS) {
                ores.add(block);
            }
        }
        ores.sort(Comparator.comparing(b -> TcContext.id(b).toString()));
        for (Block ore : ores) {
            String path = TcContext.id(ore).getPath();
            Block host = TcContext.isMod(TcContext.id(ore)) ? Blocks.END_STONE
                    : path.startsWith("deepslate_") ? Blocks.DEEPSLATE
                    : (path.startsWith("nether_") || ore == Blocks.ANCIENT_DEBRIS) ? Blocks.NETHERRACK
                    : Blocks.STONE;
            groups.get(host).add(ore);
        }
        int depth = ORE_DISTANCES[ORE_DISTANCES.length - 1] + 3;
        int gx = 0;
        boolean first = true;
        for (Map.Entry<Block, List<Block>> group : groups.entrySet()) {
            List<Block> lanes = group.getValue();
            if (lanes.isEmpty()) {
                continue;
            }
            int width = 2 * lanes.size() + 1;
            c.fill(gx + 1, 0, 1, gx + width, 2, depth, group.getKey().defaultBlockState());
            for (int i = 0; i < lanes.size(); i++) {
                Block ore = lanes.get(i);
                int laneX = gx + 2 + 2 * i;
                int d = ORE_DISTANCES[i % ORE_DISTANCES.length];
                OreDetectorItem.OreClass oreClass = OreDetectorItem.classify(ore.defaultBlockState());
                c.place(laneX, 1, 1 + d, ore);
                c.sign(laneX, 1, 0, Direction.NORTH, ore.getName(),
                        TcText.t("ores.class." + oreClass.name().toLowerCase(Locale.ROOT), pretty(oreClass.name().toLowerCase(Locale.ROOT))),
                        TcText.t("ores.distance", "distance %s", d), TcText.t("ores.range", "range %s", oreClass.range(false)));
            }
            // Linke Kante jeder Gruppe: Wirtsgestein mit Detektor und Anleitung.
            c.place(gx + 1, 0, 0, group.getKey());
            c.frame(gx + 1, 1, 0, Direction.UP, new ItemStack(ModItems.ORE_DETECTOR));
            if (first) {
                c.sign(gx + 1, 2, 0, Direction.NORTH, TcText.bold(TcText.t("section.ores", "Ore Detector")),
                        TcText.t("section.ores.sub", "stand here, use"), TcText.t("section.ores.sub2", "ore at distance d"));
                c.place(gx + 1, 2, 1, group.getKey());
                first = false;
            }
            gx += width + 2;
        }
        // Abgelegter Detector (Besitzer 2026-09-29): auf Diamanterz kalibriert, zwei Diamanterze in Stein
        // daneben. Wer Diamanterz in der Hand haelt, loest ihn ebenfalls aus (Rahmen mit Diamanterz).
        int px = gx + 1;
        c.fill(px, 0, 1, px + 6, 2, 7, Blocks.STONE.defaultBlockState());
        c.place(px + 3, 1, 5, Blocks.DIAMOND_ORE);
        c.place(px + 5, 0, 3, Blocks.DEEPSLATE_DIAMOND_ORE);
        ItemStack armed = OreDetectorItem.calibrate(new ItemStack(ModItems.ORE_DETECTOR), Blocks.DIAMOND_ORE.defaultBlockState());
        placedTemplate(c, px + 1, 3, 2, AttachFace.FLOOR, Direction.SOUTH, armed);
        c.place(px, 0, 0, Blocks.STONE);
        c.frame(px, 1, 0, Direction.UP, new ItemStack(Items.DIAMOND_ORE));
        c.sign(px + 1, 1, 0, Direction.NORTH, TcText.bold(TcText.t("ores.placed", "Placed detector")),
                TcText.t("ores.placed.sub", "calibrated: diamond ore"), TcText.t("ores.placed.sub2", "hold ore = trigger"));
        return c;
    }

    // =====================================================================================
    // 11. Oktant, Blaupause, Baustab-Modi
    // =====================================================================================

    public static TcCanvas planning(TcContext ctx) {
        TcCanvas c = new TcCanvas();
        int wallZ = 2;
        List<ItemStack> octants = new ArrayList<>();
        octants.add(new ItemStack(ModItems.OCTANT));
        for (DyeColor color : DyeColor.values()) {
            Item colored = ModItems.COLORED_OCTANT_ITEMS.get(color);
            if (colored != null) {
                octants.add(new ItemStack(colored));
            }
        }
        c.title(0, 4, wallZ, TcText.t("section.planning", "Planning"), TcText.t("section.planning.sub", "octant, blueprint, wand"));
        // Bauplanung (seit 2026-09-29 in SimpleTools neben den Baustaeben): Blaupause, Kartografentisch
        // und der Enderit-Baustab, der sie baut.
        List<Item> wands = ctx.rowItems("building_wands");
        ItemStack wand = wands.isEmpty() ? ItemStack.EMPTY : new ItemStack(wands.getLast());
        List<ItemStack> planningRow = new ArrayList<>(ctx.row("building_planning"));
        if (!wand.isEmpty()) {
            planningRow.add(wand.copy());
        }
        int end = c.rowsPanel(0, 3, wallZ, List.of(
                new TcCanvas.Line(TcText.t("planning.octants", "Octants"), octants),
                new TcCanvas.Line(TcText.t("planning.row", "Building planning"), planningRow)));
        c.backWall(0, end, wallZ, 6);

        // Baustab-Modi: je Modus eine Flaeche mit Stab auf dem Pfosten.
        record Mode(String key, String fallback, ResourceKey<Enchantment> enchantment) {
        }
        List<Mode> modes = List.of(
                new Mode("linear", "Linear", ModEnchantments.LINEAR),
                new Mode("bridge", "Bridge", ModEnchantments.BRIDGE),
                new Mode("cover", "Cover", ModEnchantments.COVER),
                new Mode("palette", "Color Palette", ModEnchantments.COLOR_PALETTE),
                new Mode("octant", "Octant fill", null),
                new Mode("roof", "Roof", null));
        int pz = wallZ + 3;
        int px = 1;
        for (Mode mode : modes) {
            ItemStack stack = mode.enchantment() == null ? wand.copy()
                    : ctx.enchantment(mode.enchantment()).map(e -> TcContext.enchanted(wand, List.of(e))).orElse(wand.copy());
            post(c, px, pz, stack, TcText.bold(TcText.t("planning.mode." + mode.key(), mode.fallback())),
                    TcText.t("planning.mode." + mode.key() + ".sub", "try the wand here"));
            c.fill(px + 1, -1, pz + 2, px + 5, -1, pz + 6, vanilla("white_concrete").defaultBlockState());
            if (mode.key().equals("bridge")) {
                // Ein Graben vor der Kante: die Bruecke baut auf Hoehe des Bodens unter den Fuessen -
                // auf durchgehend flachem Boden gaebe es nichts zu ueberbruecken.
                c.fill(px + 1, -1, pz + 3, px + 5, -1, pz + 6, Blocks.AIR.defaultBlockState());
            } else if (!mode.key().equals("roof")) {
                c.place(px + 3, 0, pz + 4, Blocks.STONE_BRICKS);
            }
            if (mode.key().equals("roof")) {
                // Prisma mit Spitze oben (3 breit, 2 hoch, 5 lang) und Treppen/Stufen in der Truhe:
                // Oktant in die Nebenhand, Treppen als ersten Baublock in die Hotbar, auf den Boden klicken.
                c.octantFrame(px, 2, pz + 2, Direction.UP, new BlockPos(px + 2, 0, pz + 2), new BlockPos(px + 4, 1, pz + 6), "TRIANGLE");
                c.place(px, 1, pz + 2, TcCanvas.TRIM);
                c.place(px, 0, pz + 2, TcCanvas.TRIM);
                c.place(px, 0, pz + 3, facing(Blocks.CHEST.defaultBlockState(), Direction.EAST));
                c.contents(px, 0, pz + 3, List.of(new ItemStack(Items.OAK_STAIRS, 64), new ItemStack(Items.OAK_SLAB, 64)));
            }
            if (mode.key().equals("octant")) {
                // Auswahl schon gesetzt: ein 3x3x3-Wuerfel ueber der Flaeche.
                c.octantFrame(px, 2, pz + 2, Direction.UP, new BlockPos(px + 2, 0, pz + 3), new BlockPos(px + 4, 2, pz + 5));
                c.place(px, 1, pz + 2, TcCanvas.TRIM);
                c.place(px, 0, pz + 2, TcCanvas.TRIM);
            }
            px += 7;
        }

        // Blaupause: kleines Beispielhaus, Kartografentisch, Oktant und leere Blaupause.
        int hx = px + 1;
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                for (int y = 0; y < 4; y++) {
                    boolean edge = x == 0 || x == 4 || z == 0 || z == 4;
                    if (y == 3) {
                        c.place(hx + x, y, pz + 2 + z, Blocks.OAK_PLANKS);
                    } else if (edge) {
                        boolean window = y == 1 && (x == 2 || z == 2);
                        c.place(hx + x, y, pz + 2 + z, window ? Blocks.GLASS : Blocks.STONE_BRICKS);
                    }
                }
            }
        }
        c.place(hx + 6, 0, pz + 2, Blocks.CARTOGRAPHY_TABLE);
        // Oktant mit der Hausauswahl und die daraus gescannte Blaupause, auf Pfosten neben dem Tisch.
        BlockPos houseMin = new BlockPos(hx, 0, pz + 2);
        BlockPos houseMax = new BlockPos(hx + 4, 3, pz + 6);
        c.place(hx + 6, 0, pz + 4, TcCanvas.TRIM);
        c.octantFrame(hx + 6, 1, pz + 4, Direction.UP, houseMin, houseMax);
        c.place(hx + 6, 0, pz + 5, TcCanvas.TRIM);
        c.blueprintFrame(hx + 6, 1, pz + 5, Direction.UP, houseMin, houseMax, new BlockPos(hx + 6, 0, pz + 2));
        post(c, hx + 7, pz, new ItemStack(ModItems.BLUEPRINT), TcText.bold(TcText.t("planning.blueprint", "Blueprint")),
                TcText.t("planning.blueprint.sub", "mark house with octant"), TcText.t("planning.blueprint.sub2", "then cartography table"));
        return c;
    }

    // =====================================================================================
    // 12. Vielseitigkeit, Adern, Tunnel
    // =====================================================================================

    public static TcCanvas mining(TcContext ctx) {
        TcCanvas c = new TcCanvas();
        // Vielseitigkeit: gemischte Wand.
        List<Block> mixed = List.of(Blocks.DIRT, Blocks.STONE, Blocks.OAK_LOG, Blocks.SAND, Blocks.GRAVEL, vanilla("white_wool"),
                Blocks.NETHERRACK, Blocks.CLAY, Blocks.OAK_PLANKS, Blocks.DEEPSLATE, Blocks.GLASS, Blocks.HAY_BLOCK);
        for (int i = 0; i < mixed.size(); i++) {
            c.place(1 + i % 6, i / 6, 3, mixed.get(i));
            c.place(1 + i % 6, 2, 3, mixed.get((i + 5) % mixed.size()));
        }
        postSign(c, 8, 0, TcText.bold(TcText.t("section.mining", "Mining")), TcText.t("section.mining.sub", "tool enchantments"));
        post(c, 0, 0, toolWith(ctx, ModEnchantments.VERSATILITY), TcText.bold(TcText.t("mining.versatility", "Versatility")),
                TcText.t("mining.versatility.sub", "mine the mixed wall"));

        // Aderabbau: Eisenader und Kohleader in Stein.
        int vx = 10;
        c.fill(vx, 0, 2, vx + 6, 3, 5, Blocks.STONE.defaultBlockState());
        int[][] iron = {{1, 0, 0}, {2, 0, 0}, {2, 1, 0}, {2, 1, 1}, {3, 1, 1}, {3, 2, 1}, {3, 2, 2}, {4, 2, 2}};
        for (int[] p : iron) {
            c.place(vx + p[0], p[1], 2 + p[2], Blocks.IRON_ORE);
        }
        int[][] coal = {{5, 0, 0}, {5, 1, 0}, {6, 1, 1}, {5, 2, 1}, {5, 3, 2}};
        for (int[] p : coal) {
            c.place(vx + p[0], p[1], 2 + p[2], Blocks.COAL_ORE);
        }
        post(c, vx - 1, 0, toolWith(ctx, ModEnchantments.VEIN_MINER), TcText.bold(TcText.t("mining.vein", "Vein Miner")),
                TcText.t("mining.vein.sub", "break one ore"));

        // Tunnelabbau: lange Steinwand.
        int sx = 20;
        c.fill(sx, 0, 2, sx + 4, 2, 13, Blocks.STONE.defaultBlockState());
        post(c, sx - 1, 0, toolWith(ctx, ModEnchantments.STRIP_MINER), TcText.bold(TcText.t("mining.strip", "Strip Miner")),
                TcText.t("mining.strip.sub", "dig into the wall"));
        return c;
    }

    /** Das hoechste Werkzeug aus SimpleTools, das die Verzauberung traegt, damit verzaubert. */
    static ItemStack toolWith(TcContext ctx, ResourceKey<Enchantment> key) {
        Optional<Holder<Enchantment>> enchantment = ctx.enchantment(key);
        if (enchantment.isEmpty()) {
            return new ItemStack(Items.NETHERITE_PICKAXE);
        }
        for (String family : List.of("pickaxes", "sledgehammers", "shovels", "axes", "hoes")) {
            List<Item> items = ctx.rowItems(family);
            for (int i = items.size() - 1; i >= 0; i--) {
                ItemStack stack = new ItemStack(items.get(i));
                if (enchantment.get().value().isSupportedItem(stack)) {
                    return TcContext.enchanted(stack, List.of(enchantment.get()));
                }
            }
        }
        return TcContext.book(enchantment.get());
    }

    // =====================================================================================
    // 13. Befehlsbloecke
    // =====================================================================================

    /** Ein Knopf der Steuerwand: Befehl und Beschriftung. */
    public record Control(String command, Component label, Component sub) {
    }

    /** Abstand zweier Befehlsbloecke der Steuerwand: Block, Glassaeule, Luft. */
    static final int CONTROL_PITCH = 3;
    /** Knoepfe je Reihe der Steuerwand (TZ22-5: Reihen statt einer 172 Bloecke langen Wand). */
    static final int CONTROLS_PER_ROW = 16;
    /** Abstand der Reihen der Steuerwand in z: Befehlsbloecke, zwei Felder Gang, Knopf und Schild der naechsten Reihe. */
    static final int CONTROL_ROW_PITCH = 4;

    public static TcCanvas controls(List<Control> controls) {
        TcCanvas c = new TcCanvas();
        int z = 2;
        c.wallSign(0, 2, z, TcText.bold(TcText.t("section.controls", "Controls")), TcText.t("section.controls.sub", "press the buttons"));
        c.place(0, 1, z, TcCanvas.TRIM);
        c.place(0, 0, z, TcCanvas.TRIM);
        for (int i = 0; i < controls.size(); i++) {
            Control control = controls.get(i);
            int x = 2 + (i % CONTROLS_PER_ROW) * CONTROL_PITCH;
            int rz = z + (i / CONTROLS_PER_ROW) * CONTROL_ROW_PITCH;
            // Ein Knopf versorgt den Block, an dem er haengt, STARK - und ein stark versorgter
            // Befehlsblock (ein Leiter) versorgt seine Nachbarn mit. Dicht an dicht loeste darum jeder
            // Knopf auch den Nachbarbefehl aus. Seit 2026-09-28: jeder Befehlsblock steht auf Glas, links
            // Luft, rechts eine Glassaeule, kein Stein verbindet zwei Bloecke (TestCentreTests prueft das
            // statisch und drueckt jeden Knopf).
            c.command(x, 1, rz, Direction.NORTH, control.command(), control.label(), control.sub());
            c.place(x + 1, 0, rz, Blocks.GLASS.defaultBlockState());
            c.place(x + 1, 1, rz, Blocks.GLASS.defaultBlockState());
        }
        return c;
    }

    // =====================================================================================
    // 14. Weitere Geraete: Tab-Zeilen, die kein Abschnitt eigens liest
    // =====================================================================================

    /** Zeilen aus SimpleTools, die eigene Abschnitte zeigen (Werkzeuge, Ruestung, Oktanten, Buecher). */
    static final Set<String> KNOWN_TOOL_ROWS = Set.of("chisels", "building_wands", "sledgehammers", "pickaxes", "shovels",
            "hoes", "axes", "swords", "spears", "gadgets", "helmets", "chestplates", "leggings", "boots", "colored_octants",
            "enchanted_books", "building_planning");
    /** Zeilen aus Maschinen &amp; Lager, die eigene Abschnitte zeigen. */
    static final Set<String> KNOWN_FUNCTIONAL_ROWS = Set.of("hoppers", "pistons", "furnaces", "smokers", "blast_furnaces",
            "bundles_and_quivers", "backpacks", "chest_vehicles", "rail_vehicles");

    /**
     * Neue Tab-Zeilen erscheinen hier von selbst - als Rahmen an der Wand und, wenn es Bloecke sind,
     * zum Ausprobieren auf dem Boden davor. Das ist die Grundversorgung; ein Feature, das mehr als
     * Anschauen braucht, bekommt besser eine eigene Station.
     */
    public static TcCanvas devices(TcContext ctx) {
        TcCanvas c = new TcCanvas();
        if (com.simplebuilding.version.McVersion.END_SYSTEMS) {
            // Redstone-Linien (2026-10-11): Hebel (aus) - 15 Pulver - Lampe der Linie (N44: Nihilith-/Astral-Lampe,
            // schaltet wie eine Kupferlampe bei jedem Puls um); die dritte Spur
            // fuehrt Redstone ueber Bruecken-Redstone in Astral-Redstone.
            BlockState lever = Blocks.LEVER.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.LeverBlock.FACE, net.minecraft.world.level.block.state.properties.AttachFace.FLOOR);
            for (int lane = 0; lane < 3; lane++) {
                int z = 5 + lane;
                for (int x = 0; x <= 17; x++) c.place(x, -1, z, Blocks.STONE);
                c.place(0, 0, z, lever);
                for (int x = 1; x <= 15; x++) {
                    c.place(x, 0, z, lane == 0 ? ModBlocks.NIHIL_REDSTONE : lane == 1 ? ModBlocks.ASTRAL_REDSTONE
                            : x <= 5 ? Blocks.REDSTONE_WIRE : x <= 10 ? ModBlocks.BRIDGE_REDSTONE : ModBlocks.ASTRAL_REDSTONE);
                }
                c.place(16, 0, z, lane == 0 ? ModBlocks.NIHILITH_LAMP : ModBlocks.ASTRALIT_LAMP);
            }
            // Astral-/Nihil-Kolben (2026-10-03): Hebel (aus) - Pulver - Kolben auf Obsidian (unbeweglich),
            // Sandstein drumherum: Astral drueckt die anliegenden weg, Nihil holt die mit einem Feld Luecke heran.
            for (int lane = 0; lane < 2; lane++) {
                boolean astral = lane == 0;
                int z = 9 + lane * 5;
                for (int x = 0; x <= 6; x++) for (int dz = -2; dz <= 2; dz++) c.place(x, -1, z + dz, Blocks.STONE);
                c.place(0, 0, z, lever);
                // Zwei Pulver: das hintere liegt fuer den Nihil-Kolben im Zugabstand, sein Zielfeld ist aber belegt.
                for (int x = 1; x <= 2; x++) c.place(x, 0, z, astral ? ModBlocks.ASTRAL_REDSTONE : ModBlocks.NIHIL_REDSTONE);
                c.place(3, -1, z, Blocks.OBSIDIAN);
                c.place(3, 0, z, astral ? ModBlocks.ASTRAL_PISTON : ModBlocks.NIHIL_PISTON);
                int gap = astral ? 1 : 2;
                c.place(3 + gap, 0, z, Blocks.SANDSTONE);
                c.place(3, gap, z, Blocks.SANDSTONE);
                c.place(3, 0, z + gap, Blocks.SANDSTONE);
                c.place(3, 0, z - gap, Blocks.SANDSTONE);
            }
            // Redstone-Bloecke der Linien (N51): drei Stationen nebeneinander (z 10..12), je ein Block, westlich
            // Nihil-Pulver + Nihilith-Lampe, oestlich Astral-Pulver + Astral-Lampe, noerdlich Redstone + Redstone-Lampe.
            // Der Block speist nur Pulver seiner Linie (Bruecke: alle drei), es leuchtet also nur die passende Lampe.
            net.minecraft.world.level.block.Block[] lineBlocks = {ModBlocks.NIHIL_REDSTONE_BLOCK, ModBlocks.ASTRAL_REDSTONE_BLOCK, ModBlocks.BRIDGE_REDSTONE_BLOCK};
            for (int i = 0; i < lineBlocks.length; i++) {
                int bx = 10 + i * 6, z = 12;
                for (int x = bx - 2; x <= bx + 2; x++) for (int dz = -2; dz <= 0; dz++) c.place(x, -1, z + dz, Blocks.STONE);
                c.place(bx, 0, z, lineBlocks[i]);
                c.place(bx - 1, 0, z, ModBlocks.NIHIL_REDSTONE);
                c.place(bx - 2, 0, z, ModBlocks.NIHILITH_LAMP);
                c.place(bx + 1, 0, z, ModBlocks.ASTRAL_REDSTONE);
                c.place(bx + 2, 0, z, ModBlocks.ASTRALIT_LAMP);
                c.place(bx, 0, z - 1, Blocks.REDSTONE_WIRE);
                c.place(bx, 0, z - 2, Blocks.REDSTONE_LAMP);
            }
        }
        if (com.simplebuilding.version.McVersion.END_RAILS) {
            // TZ22-6: die Teststrecke der Astral-/Nihil-Schienen mit Loren, Knoepfen und Hebeln (endRailTracks).
            c.append(endRailTracks(), 0, END_RAILS_Z);
        }
        // TZ22-5 (Besitzer 22.10.: die "riesige Wand" mit Truhen, Oefen, Trichtern und anderen Speichern): je Tab-Zeile eine
        // Gruppe im Bodenraster wie im Holzbereich, Nicht-Setzbares im Rahmen an der niedrigen Rueckwand - rechts neben den
        // Redstone-, Kolben- und Schienenstrecken (die bleiben fuer TZ22-6, wie sie sind).
        int x0 = com.simplebuilding.version.McVersion.END_RAILS ? 38 : com.simplebuilding.version.McVersion.END_SYSTEMS ? 27 : 0;
        int wallZ = TcCanvas.EXHIBIT_WALL_Z;
        List<com.simplebuilding.items.CreativeTabLayout.Row> rows = new ArrayList<>();
        for (com.simplebuilding.items.CreativeTabLayout.Row row : ctx.toolRows()) {
            if (!KNOWN_TOOL_ROWS.contains(row.name())) {
                rows.add(row);
            }
        }
        for (com.simplebuilding.items.CreativeTabLayout.Row row : ModItemGroupsContent.functionalRows()) {
            if (!KNOWN_FUNCTIONAL_ROWS.contains(row.name()) && !TweaksStation.ROWS.contains(row.name())) {
                rows.add(row);
            }
        }
        List<TcCanvas.Group> groups = new ArrayList<>();
        for (com.simplebuilding.items.CreativeTabLayout.Row row : rows) {
            List<ItemStack> stacks = row.stacks().stream().filter(s -> !s.isEmpty() && !TcContext.isSpacer(s.getItem())).toList();
            if (com.simplebuilding.version.McVersion.MEGA_GUIDES && (row.name().equals("guide_books") || row.name().equals("vanilla_guide_books"))) {
                List<ItemStack> guides = new ArrayList<>(stacks);
                for (ItemStack base : stacks) {
                    var guide = (com.simplebuilding.items.custom.GuideBookItem) base.getItem();
                    ItemStack filled = base.copy();
                    for (var topic : guide.book().shelf().topics()) filled = com.simplebuilding.guide.GuideBooks.withChapter(filled, topic);
                    guides.add(filled);
                }
                stacks = guides;
            }
            groups.add(new TcCanvas.Group(TcText.t("devices." + row.name(), pretty(row.name())), stacks));
        }
        if (groups.isEmpty()) {
            return c;
        }
        c.title(x0, 1, wallZ, TcText.t("section.devices", "More devices"), TcText.t("section.devices.sub", "new tab rows"));
        int end = c.exhibition(x0 + 1, groups);
        c.backWall(x0, end, wallZ, TcCanvas.EXHIBIT_WALL_HEIGHT);
        return c;
    }

    /** Lage der Schienen-Teststrecke im Abschnitt "devices" (z der Knopfreihe), hinter den Kolben-Bahnen. */
    public static final int END_RAILS_Z = 17;
    /** Schienen-Teststrecke (lokal): Bahn A (Astral) bei z 3, Bahn B (Vanilla) bei z 5, Start-Stein bei x 0. */
    public static final int RAIL_LANE_ASTRAL = 3, RAIL_LANE_VANILLA = 5;
    /** Astral: 1..14 gespeist (Anschub), bis 22 ungespeist (Hoechstgeschwindigkeit), 23..27 Nihil (Bremse), 28 Ende. */
    public static final int RAIL_LAUNCH_END = 14, RAIL_GLIDE_END = 22, RAIL_BRAKE_START = 23, RAIL_END = 28;
    /** Vanilla: 1..9 Antriebsschienen (ein Hebel speist 1, die Schienen reichen 8 weiter), Schienen, ab 23 Halt. */
    public static final int RAIL_VANILLA_POWERED_END = 9;

    /**
     * Teststrecke der Astral- und Nihil-Schienen (TZ22-6, Besitzer 22.10.: "keine richtigen Tests ..., sondern nur
     * vorgebaut"). Zwei Bahnen nebeneinander, gleich lang: A aus Astral-Schienen, B aus Vanilla-Antriebsschienen.
     * <ul>
     *   <li>Knopf "Loren aufstellen" setzt je eine Lore an den Start (neben den Anschlag-Stein), "Loren entfernen" raeumt
     *       die Strecke.</li>
     *   <li>Start-Hebel zwischen den Bahnen: speist A1 und B1 direkt; an A liegt Astral-Redstone (die 2. Linie) bis x 14,
     *       B leitet Antriebsschienen selbst weiter. Beide Loren fahren gleichzeitig los: Astral schiebt staerker und hebt
     *       die Hoechstgeschwindigkeit an (auch auf den ungespeisten Astral-Schienen danach), Vanilla bleibt bei 8 Bl./s.</li>
     *   <li>Bremse-Hebel: Redstone (1. Linie) ueber zwei Bruecken-Redstone in Nihil-Redstone (3. Linie) an den Nihil-Schienen;
     *       an: die Astral-Lore haelt dort, aus: sie rollt bis zum Ende. Bahn B haelt auf ungespeisten Antriebsschienen.</li>
     * </ul>
     * Alle Hebel sind beim Bau aus (der Bau setzt ohne Nachbar-Updates; erst das Umlegen schaltet). Der Test
     * {@code endRailTracksRunTheirCarts} baut genau diese Leinwand und faehrt sie.
     */
    public static TcCanvas endRailTracks() {
        TcCanvas c = new TcCanvas();
        int a = RAIL_LANE_ASTRAL, b = RAIL_LANE_VANILLA;
        BlockState astral = ModBlocks.ASTRAL_RAIL.defaultBlockState()
                .setValue(com.simplebuilding.blocks.custom.EndRailBlock.SHAPE, net.minecraft.world.level.block.state.properties.RailShape.EAST_WEST);
        BlockState nihil = ModBlocks.NIHIL_RAIL.defaultBlockState()
                .setValue(com.simplebuilding.blocks.custom.EndRailBlock.SHAPE, net.minecraft.world.level.block.state.properties.RailShape.EAST_WEST);
        BlockState powered = Blocks.POWERED_RAIL.defaultBlockState()
                .setValue(net.minecraft.world.level.block.PoweredRailBlock.SHAPE, net.minecraft.world.level.block.state.properties.RailShape.EAST_WEST);
        BlockState rail = Blocks.RAIL.defaultBlockState()
                .setValue(net.minecraft.world.level.block.RailBlock.SHAPE, net.minecraft.world.level.block.state.properties.RailShape.EAST_WEST);
        BlockState lever = Blocks.LEVER.defaultBlockState()
                .setValue(net.minecraft.world.level.block.LeverBlock.FACE, net.minecraft.world.level.block.state.properties.AttachFace.FLOOR);
        int brakeLever = RAIL_END + 3;
        c.fill(0, -1, 0, brakeLever, -1, b + 2, Blocks.STONE.defaultBlockState());
        // Bahn A: Anschlag, Astral (Anschub + Gleiten), Nihil (Bremse), Ende.
        c.place(0, 0, a, Blocks.STONE);
        for (int x = 1; x <= RAIL_GLIDE_END; x++) c.place(x, 0, a, astral);
        for (int x = RAIL_BRAKE_START; x < RAIL_END; x++) c.place(x, 0, a, nihil);
        c.place(RAIL_END, 0, a, Blocks.STONE);
        // Bahn B: Anschlag, Antriebsschienen, Schienen, ungespeiste Antriebsschienen (Vanilla-Halt), Ende.
        c.place(0, 0, b, Blocks.STONE);
        for (int x = 1; x <= RAIL_VANILLA_POWERED_END; x++) c.place(x, 0, b, powered);
        for (int x = RAIL_VANILLA_POWERED_END + 1; x < RAIL_BRAKE_START; x++) c.place(x, 0, b, rail);
        for (int x = RAIL_BRAKE_START; x < RAIL_END; x++) c.place(x, 0, b, powered);
        c.place(RAIL_END, 0, b, Blocks.STONE);
        // Start: Hebel zwischen den Bahnen, Astral-Redstone (2. Linie) neben dem Anschub von A.
        c.place(1, 0, a + 1, lever);
        for (int x = 2; x <= RAIL_LAUNCH_END; x++) c.place(x, 0, a + 1, ModBlocks.ASTRAL_REDSTONE);
        // Bremse: Hebel - Redstone (1. Linie) - Bruecke - Nihil-Redstone (3. Linie) neben den Nihil-Schienen.
        c.place(brakeLever, 0, a - 1, lever);
        c.place(brakeLever - 1, 0, a - 1, Blocks.REDSTONE_WIRE);
        c.place(brakeLever - 2, 0, a - 1, ModBlocks.BRIDGE_REDSTONE);
        c.place(brakeLever - 3, 0, a - 1, ModBlocks.BRIDGE_REDSTONE);
        for (int x = RAIL_BRAKE_START; x < RAIL_END; x++) c.place(x, 0, a - 1, ModBlocks.NIHIL_REDSTONE);
        // Loren per Knopf (Befehlsbloecke bei z 1, Knopf bei z 0): aufstellen an A1/B1, entfernen auf der ganzen Strecke.
        c.command(PLACE_CARTS_X, 1, 1, Direction.NORTH, "execute align xyz positioned ~" + (1 - PLACE_CARTS_X + 0.5) + " ~-1 ~"
                        + (a - 1 + 0.5) + " summon minecraft:minecart positioned ~ ~ ~" + (b - a) + " run summon minecraft:minecart ~ ~ ~",
                TcText.t("end_rails.place", "Place carts"), TcText.t("end_rails.place.sub", "one per lane"));
        c.command(REMOVE_CARTS_X, 1, 1, Direction.NORTH, "execute align xyz positioned ~" + (-REMOVE_CARTS_X) + " ~-1 ~" + (a - 1)
                        + " run kill @e[type=minecraft:minecart,dx=" + (brakeLever - 1) + ",dy=2,dz=" + (b - a + 1) + "]",
                TcText.t("end_rails.remove", "Remove carts"), TcText.t("end_rails.remove.sub", "on this track"));
        // Schilder an einer niedrigen Wand hinter Bahn B.
        int wallZ = b + 2;
        for (int x = 0; x <= RAIL_END; x++) c.place(x, 0, wallZ, TcCanvas.WALL);
        int topSpeed = (int) Math.round(com.simplebuilding.blocks.custom.EndRailPhysics.maxSpeedPerTick() * 20);
        c.wallSign(1, 1, wallZ, TcText.bold(TcText.t("end_rails.title", "End rails")), TcText.t("end_rails.how", "1. button: carts"),
                TcText.t("end_rails.how2", "2. lever: go"), TcText.t("end_rails.how3", "front: Astral, back: vanilla"));
        c.wallSign(8, 1, wallZ, TcText.bold(TcText.t("end_rails.launch", "Astral launch")), TcText.t("end_rails.launch.sub", "Astral Redstone,"),
                TcText.t("end_rails.launch.sub2", "the 2nd line"));
        c.wallSign(18, 1, wallZ, TcText.bold(TcText.t("end_rails.speed", "Top speed")), TcText.t("end_rails.speed.sub", "Astral: up to %s b/s", topSpeed),
                TcText.t("end_rails.speed.sub2", "vanilla: 8 b/s"));
        c.wallSign(25, 1, wallZ, TcText.bold(TcText.t("end_rails.brake", "Nihil brake")), TcText.t("end_rails.brake.sub", "lever, redstone, bridge,"),
                TcText.t("end_rails.brake.sub2", "Nihil Redstone (3rd line)"));
        c.anchor("end_rails_start_lever", 1, 0, a + 1);
        c.anchor("end_rails_brake_lever", brakeLever, 0, a - 1);
        c.anchor("end_rails_astral_start", 1, 0, a);
        c.anchor("end_rails_vanilla_start", 1, 0, b);
        c.anchor("end_rails_place_button", PLACE_CARTS_X, 1, 0);
        c.anchor("end_rails_remove_button", REMOVE_CARTS_X, 1, 0);
        return c;
    }

    /** x der Befehlsbloecke "Loren aufstellen"/"Loren entfernen" in der Schienen-Teststrecke. */
    static final int PLACE_CARTS_X = 3, REMOVE_CARTS_X = 6;

    // =====================================================================================
    // 14. Unsortiert: was noch keinen Abschnitt hat
    // =====================================================================================

    /**
     * Galerie: jeder Mod-Block, den kein Abschnitt setzt (etwa nur als Rahmen zeigt), steht hier einmal gesetzt - neue
     * Bloecke also von selbst, auch die, die {@link Exhibits} sonst lieber im Rahmen zeigt (dafuer mit einem Feld Abstand,
     * drei Reihen tief). Ausgenommen sind Bloecke mit Wirkung auf die Umgebung ({@link TestCentreLayout#frameOnly}).
     */
    public static TcCanvas gallery(List<Block> blocks) {
        TcCanvas c = new TcCanvas();
        if (blocks.isEmpty()) {
            return c;
        }
        int wallZ = 7;
        c.title(0, 1, wallZ, TcText.t("section.gallery", "More blocks"), TcText.t("section.gallery.sub", "placed nowhere else"));
        for (int i = 0; i < blocks.size(); i++) {
            Block block = blocks.get(i);
            List<BlockState> states = Exhibits.floorStates(new ItemStack(block.asItem()));
            if (states.isEmpty() || states.getFirst().getBlock() != block) {
                states = List.of(facing(block.defaultBlockState(), Direction.NORTH));
            }
            for (int y = 0; y < states.size(); y++) {
                c.place(1 + 2 * (i / 3), y, 1 + 2 * (i % 3), states.get(y));
            }
        }
        c.backWall(0, 1 + 2 * ((blocks.size() + 2) / 3), wallZ, TcCanvas.EXHIBIT_WALL_HEIGHT);
        return c;
    }

    /** Hoechstens so breit wird eine Reihe von Modul-Buchten; die naechste Reihe liegt dahinter. */
    static final int MODULE_ROW_WIDTH = 90;
    /** Abstand der Buchten-Reihen in z: Bucht (Raster, Gang, Wand) und zwei Felder Gang. */
    static final int MODULE_ROW_PITCH = TcCanvas.EXHIBIT_WALL_Z + 3;

    /**
     * Die Modul-Ausstellung (Modulaufteilung 3c, TZ22-1): je Modul-Namensraum der Familie ({@link TcContext#FAMILY}) eine
     * Bucht im Stil des Holzbereichs ({@link TcCanvas#exhibition}) - die angemeldeten Stationen
     * ({@code framework.api.TestCentreStations}, unbekannte Ids fallen weg) als Gruppen, dazu "Weitere": alles aus dem
     * Register des Moduls, was keine Station nennt. Was eine andere Station schon sichtbar zeigt ({@code shown}), kommt
     * nicht doppelt. Buchten-Reihen hoechstens {@link #MODULE_ROW_WIDTH} breit.
     */
    public static TcCanvas modules(List<ModuleStations.Station> stations, Set<Item> shown, Set<Block> placed) {
        Map<String, List<TcCanvas.Group>> bays = new LinkedHashMap<>();
        Set<Item> taken = new HashSet<>(shown);
        taken.add(net.minecraft.world.item.Items.AIR);
        for (ModuleStations.Station station : stations) {
            List<ItemStack> stacks = new ArrayList<>();
            for (String id : station.items()) {
                net.minecraft.resources.Identifier key = net.minecraft.resources.Identifier.tryParse(id);
                Item item = key == null ? null : BuiltInRegistries.ITEM.getOptional(key).orElse(null);
                if (item != null && taken.add(item)) {
                    stacks.add(new ItemStack(item));
                }
            }
            bays.computeIfAbsent(station.owner(), owner -> new ArrayList<>())
                    .add(new TcCanvas.Group(Component.translatable(station.titleKey()), stacks));
        }
        Map<String, List<ItemStack>> rest = new LinkedHashMap<>();
        for (Item item : sortedItems()) {
            net.minecraft.resources.Identifier id = TcContext.id(item);
            if (TcContext.isFamily(id) && !TcContext.isMod(id) && !TestCentreLayout.EXCLUDED.containsKey(id.toString())
                    && !TcContext.isSpacer(item) && taken.add(item)) {
                rest.computeIfAbsent(id.getNamespace(), ns -> new ArrayList<>()).add(new ItemStack(item));
            }
        }
        rest.keySet().stream().sorted().forEach(ns -> {
            List<TcCanvas.Group> groups = bays.computeIfAbsent(ns, owner -> new ArrayList<>());
            groups.add(new TcCanvas.Group(groups.isEmpty() ? TcText.t("modules.all", "All items") : TcText.t("modules.more", "More"),
                    rest.get(ns)));
        });
        // Bloecke ohne Item, die keine Station setzt (Achtel-Zellen, Bauzwischenstufen wie der halbfertige Tiegel): gesetzt in
        // der Bucht ihres Moduls; Wand-/Topf-Varianten, Fluessigkeiten und begruendete Ausnahmen nicht.
        Map<String, List<BlockState>> itemless = new java.util.TreeMap<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            net.minecraft.resources.Identifier id = TcContext.id(block);
            if (!TcContext.isFamily(id) || TcContext.isMod(id) || TcContext.itemOf(block) != net.minecraft.world.item.Items.AIR
                    || placed.contains(block) || TestCentreLayout.technical(block) != null
                    || TestCentreLayout.EXCLUDED.containsKey(id.toString())) {
                continue;
            }
            List<BlockState> states = Exhibits.blockStates(block);
            if (!states.isEmpty()) {
                itemless.computeIfAbsent(id.getNamespace(), ns -> new ArrayList<>()).addAll(states);
            }
        }
        itemless.forEach((ns, states) -> bays.computeIfAbsent(ns, owner -> new ArrayList<>())
                .add(new TcCanvas.Group(TcText.t("modules.itemless", "Without item"), List.of(), states)));

        TcCanvas c = new TcCanvas();
        int x = 0;
        int z = 0;
        for (Map.Entry<String, List<TcCanvas.Group>> bay : bays.entrySet()) {
            if (bay.getValue().stream().allMatch(group -> group.stacks().isEmpty() && group.blocks().isEmpty())) {
                continue;
            }
            TcCanvas b = new TcCanvas();
            b.title(0, 1, TcCanvas.EXHIBIT_WALL_Z, TcText.lit(modName(bay.getKey())), TcText.lit(bay.getKey()));
            int end = b.exhibition(1, bay.getValue());
            b.backWall(0, end, TcCanvas.EXHIBIT_WALL_Z, TcCanvas.EXHIBIT_WALL_HEIGHT);
            if (x > 0 && x + b.width() > MODULE_ROW_WIDTH) {
                x = 0;
                z += MODULE_ROW_PITCH;
            }
            c.append(b, x, z);
            x += b.width() + 2;
        }
        return c;
    }

    /** "simplestorage" -> "Simple Storage" (der Modulname ohne Klassen des Moduls). */
    static String modName(String namespace) {
        if (!namespace.startsWith("simple") || namespace.length() == "simple".length()) {
            return pretty(namespace);
        }
        String rest = namespace.substring("simple".length());
        return "Simple " + Character.toUpperCase(rest.charAt(0)) + rest.substring(1);
    }

    public static TcCanvas unsorted(List<Item> leftovers) {
        TcCanvas c = new TcCanvas();
        if (leftovers.isEmpty()) {
            return c;
        }
        int wallZ = TcCanvas.EXHIBIT_WALL_Z;
        c.title(0, 1, wallZ, TcText.t("section.unsorted", "Unsorted"), TcText.t("section.unsorted.sub", "add to a section!"));
        List<ItemStack> stacks = leftovers.stream().map(ItemStack::new).toList();
        int end = c.exhibition(1, List.of(new TcCanvas.Group(TcText.t("section.unsorted", "Unsorted"), stacks)));
        c.backWall(0, end, wallZ, TcCanvas.EXHIBIT_WALL_HEIGHT);
        return c;
    }

    // =====================================================================================
    // Helfer
    // =====================================================================================

    static List<Item> sortedItems() {
        List<Item> out = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            out.add(item);
        }
        out.sort(Comparator.comparing(TcContext::id, TcContext.MOD_FIRST));
        return out;
    }

    static List<Item> sortedModItems() {
        return sortedItems().stream().filter(item -> TcContext.isMod(TcContext.id(item))).toList();
    }

    static <T extends Comparable<T>> BlockState with(BlockState state, Property<T> property, T value) {
        return state.hasProperty(property) && property.getPossibleValues().contains(value) ? state.setValue(property, value) : state;
    }

    static BlockState facing(BlockState state, Direction direction) {
        return with(state, BlockStateProperties.HORIZONTAL_FACING, direction);
    }

    /** Vanilla-Block ueber die Id - die Farb-Konstanten heissen je Linie anders. */
    static Block vanilla(String path) {
        return BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(path));
    }

    static String pretty(String id) {
        String text = id.replace('_', ' ');
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}

package com.simplebuilding.dev.testcentre;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.LevitatingBlock;
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
                    {"both", "Glowing + Emitting"}};
            for (String[] variant : variants) {
                List<ItemStack> gear = new ArrayList<>();
                for (Item piece : List.of(helmets.get(top), chests.get(top), legs.get(top), boots.get(top))) {
                    ItemStack stack = trimmed(piece, material, pattern);
                    if (!variant[0].equals("emitting")) {
                        GlowingTrimUtils.setGlowLevel(stack, 2);
                    }
                    if (!variant[0].equals("glowing")) {
                        setEmission(stack, 5);
                    }
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

        // Rueckwand: Vorlagen, Besatzmaterialien, dann je Muster eine Spalte mit vier Teilen.
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
        c.title(0, 7, wallZ, TcText.t("section.armour", "Armour & Trims"),
                TcText.t("section.armour.sub", "sets, upgrades, patterns"));
        int end = c.rowsPanel(0, 6, wallZ, List.of(
                new TcCanvas.Line(TcText.t("armour.templates", "Templates"), templates),
                new TcCanvas.Line(TcText.t("armour.materials", "Trim materials"), materialItems)));
        List<ItemStack> pieces = new ArrayList<>();
        if (tiers > 0) {
            int top = tiers - 1;
            for (int i = 0; i < patterns.size() && !materials.isEmpty(); i++) {
                Holder<TrimMaterial> material = materials.get(i % materials.size());
                for (Item piece : List.of(helmets.get(top), chests.get(top), legs.get(top), boots.get(top))) {
                    pieces.add(trimmed(piece, material, patterns.get(i)));
                }
            }
        }
        c.wallSign(0, 3, wallZ, TcText.t("armour.patterns", "Trim patterns"), TcText.t("armour.patterns.sub", "one column each"));
        end = Math.max(end, c.frameGrid(1, 0, wallZ, pieces, null, 4));
        c.backWall(0, Math.max(end, x), wallZ, 9);
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
        c.title(0, 5, wallZ, TcText.t("section.books", "Enchanted Books"),
                TcText.t("section.books.sub", "mod first, then vanilla"));
        int end = c.frameGrid(1, 0, wallZ, books, labels, 3);
        c.backWall(0, end, wallZ, 7);
        return c;
    }

    // =====================================================================================
    // 3. Werkzeuge und Waffen
    // =====================================================================================

    /** Familien in der Reihenfolge des Tabs SimpleTools. */
    static final List<String> TOOL_FAMILIES = List.of("chisels", "building_wands", "sledgehammers", "pickaxes",
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

        int top = plain.size();
        c.title(0, top + 1, wallZ, TcText.t("section.tools", "Tools & Weapons"), TcText.t("tools.plain", "plain"));
        int end = c.rowsPanel(0, top, wallZ, plain);
        int x = end + 1;
        c.wallSign(x, top + 1, wallZ, TcText.bold(TcText.t("tools.enchanted", "max. enchanted")));
        end = c.rowsPanel(x, top, wallZ, enchanted);
        x = end + 1;
        c.wallSign(x, top + 1, wallZ, TcText.bold(TcText.t("tools.variants", "Dev tab variants")),
                TcText.t("tools.variants.sub", "every exclusive set"));
        end = c.frameGrid(x, 1, wallZ, DevEnchantedTab.variants(ctx.lookup()), null, top);
        c.backWall(0, end, wallZ, top + 3);
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
            c.frame(x, 1, z0, Direction.UP, touchIfNeeded(ctx, new ItemStack(chain.chisel()), chain.touch()));
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
    private static void postSign(TcCanvas c, int x, int z, Component... lines) {
        c.place(x, 0, z + 1, TcCanvas.TRIM);
        c.place(x, 1, z + 1, TcCanvas.TRIM);
        c.sign(x, 1, z, Direction.NORTH, lines);
    }

    /** Pfosten mit Rahmen obenauf und Schild davor. */
    private static void post(TcCanvas c, int x, int z, ItemStack stack, Component... lines) {
        postSign(c, x, z, lines);
        c.frame(x, 2, z + 1, Direction.UP, stack);
    }

    // =====================================================================================
    // 5. In-World-Umwandlungen: je eine Station
    // =====================================================================================

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
                TcText.t("inworld.reshape.sub", "hold right-click"), TcText.t("inworld.reshape.sub2", "sneak = back"));
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
        c.place(x, 0, 2, Blocks.DIAMOND_BLOCK);
        c.wallFrame(x, 2, wallZ, hammer);
        c.wallSign(x, 1, wallZ, TcText.bold(TcText.t("inworld.crush", "Crush")),
                TcText.t("inworld.crush.sub", "break with hammer"), TcText.t("inworld.crush.sub2", "-> diamond pebbles"));
        x += 3;

        // c) Maschinen-Aufwertung mit Nugget und Hammer.
        c.wallSign(x, 3, wallZ, TcText.bold(TcText.t("inworld.upgrade", "Machine upgrades")),
                TcText.t("inworld.upgrade.sub", "nugget in off hand"), TcText.t("inworld.upgrade.sub2", "hold use with hammer"));
        x++;
        for (Block block : BuiltInRegistries.BLOCK) {
            SledgehammerUpgrades.Upgrade upgrade = SledgehammerUpgrades.upgradeOf(block);
            if (upgrade == null || upgrade.from() != block) {
                continue;
            }
            c.place(x, 0, 2, facing(upgrade.from().defaultBlockState(), Direction.NORTH));
            c.wallFrame(x, 1, wallZ, new ItemStack(upgrade.nugget()));
            Item needed = null;
            for (Item candidate : hammers) {
                if (SledgehammerUpgrades.hammerRank(candidate) >= upgrade.minHammerRank()) {
                    needed = candidate;
                    break;
                }
            }
            if (needed != null) {
                c.wallFrame(x, 2, wallZ, new ItemStack(needed));
            }
            c.wallSign(x, 3, wallZ, TcText.t("inworld.to", "-> %s", upgrade.to().getName()));
            x += 2;
        }
        x++;

        // d) Scheren an Wolle.
        c.place(x, 0, 2, vanilla("white_wool"));
        c.wallFrame(x, 2, wallZ, new ItemStack(Items.SHEARS));
        c.wallSign(x, 1, wallZ, TcText.bold(TcText.t("inworld.shears", "Shears")), TcText.t("inworld.shears.sub", "use on wool"),
                TcText.t("inworld.shears.sub2", "-> string"));
        x += 3;

        // e) Besatzvorlage im Rahmen aufwerten.
        Item template = null;
        for (Item item : sortedItems()) {
            if (SledgehammerEntityInteraction.isTrimTemplate(item)) {
                template = item;
                break;
            }
        }
        c.wallSign(x, 3, wallZ, TcText.bold(TcText.t("inworld.trim", "Trim template")),
                TcText.t("inworld.trim.sub", "hit frame with hammer"), TcText.t("inworld.trim.sub2", "catalyst in off hand"));
        x++;
        for (Map.Entry<Item, Item> upgrade : SledgehammerEntityInteraction.trimUpgrades().entrySet()) {
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
        c.wallSign(x, 1, wallZ, TcText.bold(TcText.t("inworld.wash", "Cauldron wash")),
                TcText.t("inworld.wash.sub", "dyed octant -> plain"));
        x += 2;

        c.backWall(0, x, wallZ, 6);
        return c;
    }

    // =====================================================================================
    // 6. Lager: Buendel, Koecher, Rucksaecke
    // =====================================================================================

    static final List<String> STORAGE_FAMILIES = List.of("bundles", "quivers", "backpacks");
    static final List<DyeColor> DYE_EXAMPLES = List.of(DyeColor.RED, DyeColor.LIME, DyeColor.BLUE, DyeColor.YELLOW);

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
                        copy.set(DataComponents.DYED_COLOR, new DyedItemColor(color.getTextureDiffuseColor()));
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
        c.title(0, lines.size() + 1, wallZ, TcText.t("section.storage", "Storage"),
                TcText.t("section.storage.sub", "plain, enchanted, dyed"));
        int end = c.rowsPanel(0, lines.size(), wallZ, lines);
        c.backWall(0, Math.max(end, bx), wallZ, lines.size() + 3);
        return c;
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

    public static TcCanvas materials(TcContext ctx) {
        TcCanvas c = new TcCanvas();
        int wallZ = 2;
        List<ItemStack> items = ctx.tab(ModItemGroupsContent.Tab.MATERIALS);
        List<List<Component>> labels = new ArrayList<>();
        for (ItemStack stack : items) {
            labels.add(List.of(stack.getHoverName()));
        }
        c.title(0, 5, wallZ, TcText.t("section.materials", "Materials"), TcText.t("section.materials.sub", "tab SimpleMaterials"));
        int end = c.frameGrid(1, 0, wallZ, items, labels, 3);
        c.backWall(0, end, wallZ, 7);
        return c;
    }

    // =====================================================================================
    // 8. Bloecke: Musterwand, Schachbrett-Boeden, Schwebe-Sand
    // =====================================================================================

    public static TcCanvas blocks(TcContext ctx) {
        TcCanvas c = new TcCanvas();
        int wallZ = 7;
        c.title(0, 4, wallZ, TcText.t("section.blocks", "Building Blocks"), TcText.t("section.blocks.sub", "tab SimpleBuilding"));
        int x = 1;
        int checkerX = 1;
        for (ItemStack stack : ctx.tab(ModItemGroupsContent.Tab.BUILDING_BLOCKS)) {
            if (!(stack.getItem() instanceof BlockItem blockItem)) {
                continue;
            }
            Block block = blockItem.getBlock();
            String path = TcContext.id(block).getPath();
            if (block instanceof LevitatingBlock) {
                // Steigt ohne Decke auf; unter Glas bleibt er liegen.
                c.place(x, 0, wallZ - 1, block);
                c.place(x, 1, wallZ - 1, Blocks.GLASS);
            } else if (path.startsWith("suspended_")) {
                // Frei schwebend, ohne Stuetze.
                c.place(x, 2, wallZ - 1, block);
            } else {
                for (int y = 0; y < 3; y++) {
                    c.place(x, y, wallZ - 1, block);
                }
            }
            c.wallSign(x, 3, wallZ, block.getName());
            if (path.contains("checker")) {
                // Schachbretter zusaetzlich als Bodenflaeche.
                c.fill(checkerX, -1, 1, checkerX + 2, -1, 3, block.defaultBlockState());
                checkerX += 4;
            }
            x++;
        }
        c.backWall(0, Math.max(x, checkerX), wallZ, 5);
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
        c.backWall(0, x, wallZ, 7);

        // Kolben: jede Stufe mit Hebel obenauf, Bahn nach Sueden.
        int pz = wallZ + 3;
        int px = 1;
        c.sign(0, 1, pz - 1, Direction.NORTH, TcText.bold(TcText.t("machines.pistons", "Pistons")),
                TcText.t("machines.pistons.sub", "flip the lever"), TcText.t("machines.pistons.sub2", "13 blocks: vanilla fails"));
        c.place(0, 1, pz, TcCanvas.TRIM);
        c.place(0, 0, pz, TcCanvas.TRIM);
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
        return c;
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
        int end = c.rowsPanel(0, 3, wallZ, List.of(
                new TcCanvas.Line(TcText.t("planning.octants", "Octants"), octants),
                new TcCanvas.Line(TcText.t("planning.row", "Building planning"), ctx.row("building_planning"))));
        c.backWall(0, end, wallZ, 6);

        // Baustab-Modi: je Modus eine Flaeche mit Stab auf dem Pfosten.
        List<Item> wands = ctx.rowItems("building_wands");
        ItemStack wand = wands.isEmpty() ? ItemStack.EMPTY : new ItemStack(wands.getLast());
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
            c.place(px + 3, 0, pz + 4, Blocks.STONE_BRICKS);
            if (mode.key().equals("octant")) {
                c.frame(px, 2, pz + 2, Direction.UP, new ItemStack(ModItems.OCTANT));
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
    private static ItemStack toolWith(TcContext ctx, ResourceKey<Enchantment> key) {
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

    public static TcCanvas controls(List<Control> controls) {
        TcCanvas c = new TcCanvas();
        int z = 2;
        c.wallSign(0, 2, z, TcText.bold(TcText.t("section.controls", "Controls")), TcText.t("section.controls.sub", "press the buttons"));
        c.place(0, 1, z, TcCanvas.TRIM);
        c.place(0, 0, z, TcCanvas.TRIM);
        int x = 1;
        for (Control control : controls) {
            c.place(x, 0, z, TcCanvas.TRIM);
            c.command(x, 1, z, Direction.NORTH, control.command(), control.label(), control.sub());
            x++;
        }
        return c;
    }

    // =====================================================================================
    // 14. Unsortiert: was noch keinen Abschnitt hat
    // =====================================================================================

    public static TcCanvas unsorted(List<Item> leftovers) {
        TcCanvas c = new TcCanvas();
        int wallZ = 2;
        List<ItemStack> stacks = new ArrayList<>();
        List<List<Component>> labels = new ArrayList<>();
        for (Item item : leftovers) {
            stacks.add(new ItemStack(item));
            labels.add(List.of(new ItemStack(item).getHoverName()));
        }
        c.title(0, 5, wallZ, TcText.t("section.unsorted", "Unsorted"),
                TcText.t("section.unsorted.sub", "add to a section!"));
        int end = c.frameGrid(1, 0, wallZ, stacks, labels, 3);
        c.backWall(0, end, wallZ, 7);
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

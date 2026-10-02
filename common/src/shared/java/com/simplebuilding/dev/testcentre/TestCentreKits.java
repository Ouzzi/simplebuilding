package com.simplebuilding.dev.testcentre;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.util.SledgehammerUpgrades;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Die Ausruestung je Station ({@code /sbtestcentre give <station> [Spieler]}, Knopf vorn links an jeder
 * Station). Das Kit ersetzt das Inventar: Haupthand und Nebenhand fuer die Interaktionstests der Station,
 * dazu alles, was die Station zeigt - Rahmen, Ruestungsstaender, Behaelterinhalte und die gesetzten
 * Bloecke (die ganze Blockpalette), jeweils als voller Stapel.
 *
 * <p>Der Inhalt kommt aus der Planung der Station, nicht aus festen Listen: was eine Station neu zeigt,
 * ist damit auch im Kit. Nur Haupt- und Nebenhand sind je Station festgelegt ({@link #hands}); ohne
 * Festlegung nimmt die Haupthand das erste gezeigte Item. Was nicht mehr ins Inventar passt, zaehlt
 * {@link Kit#leftOut()} - gezeigt Gegenstaende (Rahmen, Staender) kommen vor gesetzten Bloecken, Mod
 * vor Vanilla.
 */
public final class TestCentreKits {

    /** Hauptplaetze des Inventars (Hotbar + drei Reihen); einer davon traegt die Haupthand. */
    public static final int MAIN_SLOTS = 36;

    /** Ein Kit: Haupthand, Nebenhand, Rest ins Inventar; {@code leftOut} passte nicht mehr hinein. */
    public record Kit(ItemStack mainHand, ItemStack offHand, List<ItemStack> inventory, int leftOut) {
        public boolean isEmpty() {
            return mainHand.isEmpty() && offHand.isEmpty() && inventory.isEmpty();
        }

        /** Alle Stapel des Kits (Haupthand, Nebenhand, Inventar). */
        public List<ItemStack> all() {
            List<ItemStack> out = new ArrayList<>();
            if (!mainHand.isEmpty()) {
                out.add(mainHand);
            }
            if (!offHand.isEmpty()) {
                out.add(offHand);
            }
            out.addAll(inventory);
            return out;
        }
    }

    /** Geruest der Zentrale: Waende, Boden, Schilder, Befehlsbloecke - gehoert in kein Kit. */
    static final Set<Block> INFRASTRUCTURE = Set.of(TcCanvas.WALL.getBlock(), TcCanvas.TRIM.getBlock(), Blocks.GLASS,
            TestCentreBuilder.FLOOR.getBlock(), TestCentreBuilder.SECTION_FLOOR.getBlock(), TestCentreBuilder.LAMP.getBlock(),
            TweaksStation.FLIGHT_FIELD.getBlock(), Blocks.COMMAND_BLOCK, Blocks.STONE_BUTTON, Blocks.OAK_WALL_SIGN, Blocks.OAK_SIGN,
            Blocks.CHEST, Blocks.BARREL, Blocks.AIR, Blocks.WATER_CAULDRON, Blocks.DEEPSLATE_BRICKS);

    private TestCentreKits() {
    }

    /** Das Kit einer Station aus ihren (lokalen oder absoluten) Planungsschritten. */
    public static Kit of(TcContext ctx, String section, List<TcOp> ops) {
        List<ItemStack> shown = new ArrayList<>();
        List<ItemStack> modBlocks = new ArrayList<>();
        List<ItemStack> vanillaBlocks = new ArrayList<>();
        for (TcOp op : ops) {
            switch (op) {
                case TcOp.Frame frame -> shown.add(frame.stack());
                case TcOp.OctantFrame frame -> shown.add(new ItemStack(ModItems.OCTANT));
                case TcOp.BlueprintFrame frame -> shown.add(new ItemStack(ModItems.BLUEPRINT));
                case TcOp.Stand stand -> {
                    shown.addAll(stand.gear());
                    if (stand.dummy()) {
                        shown.add(new ItemStack(ModItems.STRAW_ARMOR_STAND));
                    }
                }
                case TcOp.Fill fill -> shown.addAll(fill.contents());
                case TcOp.Place place -> {
                    Block block = place.state().getBlock();
                    Item item = block.asItem();
                    if (item != Items.AIR && !INFRASTRUCTURE.contains(block)) {
                        (TcContext.isMod(TcContext.id(block)) ? modBlocks : vanillaBlocks).add(new ItemStack(item));
                    }
                }
                case TcOp.Sign sign -> {
                }
                case TcOp.Command command -> {
                }
            }
        }
        List<ItemStack> candidates = new ArrayList<>();
        addAll(candidates, shown);
        addAll(candidates, modBlocks);
        addAll(candidates, vanillaBlocks);

        ItemStack[] hands = hands(ctx, section);
        ItemStack main = hands[0];
        ItemStack off = hands[1];
        if (main.isEmpty() && !candidates.isEmpty()) {
            main = candidates.getFirst();
        }
        List<ItemStack> inventory = new ArrayList<>();
        for (ItemStack stack : candidates) {
            if (!ItemStack.isSameItemSameComponents(stack, main) && !ItemStack.isSameItemSameComponents(stack, off)) {
                inventory.add(stack);
            }
        }
        int room = MAIN_SLOTS - (main.isEmpty() ? 0 : 1);
        int leftOut = Math.max(0, inventory.size() - room);
        if (leftOut > 0) {
            inventory = new ArrayList<>(inventory.subList(0, room));
        }
        return new Kit(main, off, List.copyOf(inventory), leftOut);
    }

    /** Fuegt Stapel ohne Doppelte (gleiches Item, gleiche Komponenten) als volle Stapel an. */
    private static void addAll(List<ItemStack> into, List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (stack.isEmpty() || TcContext.isSpacer(stack.getItem())) {
                continue;
            }
            boolean known = false;
            for (ItemStack present : into) {
                if (ItemStack.isSameItemSameComponents(present, stack)) {
                    known = true;
                    break;
                }
            }
            if (!known) {
                into.add(full(stack));
            }
        }
    }

    private static ItemStack full(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = stack.copy();
        copy.setCount(Math.max(stack.getCount(), stack.getMaxStackSize()));
        return copy;
    }

    /**
     * Haupt- und Nebenhand je Station, passend zu dem, was man dort ausprobiert. Leer heisst: die
     * Haupthand nimmt das erste gezeigte Item, die Nebenhand bleibt frei.
     */
    static ItemStack[] hands(TcContext ctx, String section) {
        ItemStack main = ItemStack.EMPTY;
        ItemStack off = ItemStack.EMPTY;
        switch (section) {
            case "armour" -> main = top(ctx, "swords");
            case "archery" -> {
                main = new ItemStack(Items.BOW);
                off = new ItemStack(Items.ARROW, 64);
            }
            case "tools" -> {
                main = ctx.maxEnchanted(top(ctx, "pickaxes"));
                off = ctx.maxEnchanted(top(ctx, "chisels"));
            }
            case "chisel" -> {
                main = top(ctx, "chisels");
                ItemStack chisel = main;
                off = ctx.enchantment(ModEnchantments.CONSTRUCTORS_TOUCH)
                        .map(touch -> TcContext.enchanted(chisel, List.of(touch))).orElse(ItemStack.EMPTY);
            }
            case "inworld" -> {
                main = top(ctx, "sledgehammers");
                off = full(firstUpgradeNugget());
            }
            case "templates" -> {
                main = top(ctx, "sledgehammers");
                off = full(new ItemStack(Items.GLOWSTONE_DUST));
            }
            case "blocks" -> main = top(ctx, "building_wands");
            case "planning" -> {
                main = top(ctx, "building_wands");
                off = new ItemStack(ModItems.OCTANT);
            }
            case "ores" -> main = new ItemStack(ModItems.ORE_DETECTOR);
            case "mining" -> main = TestCentreSections.toolWith(ctx, ModEnchantments.VERSATILITY);
            case "lightroom" -> main = full(new ItemStack(ModBlocks.CONSTRUCTION_LIGHT));
            case "tweaks" -> main = full(new ItemStack(Items.WIND_CHARGE));
            case "states" -> {
                // Leerer Resonanzstab und sein Lade-Material: am Amboss der Station aufladen.
                main = FeatureStations.worn(new ItemStack(com.simplebuilding.tweaks.item.TweaksItems.LASER_POINTER), 1F);
                off = full(new ItemStack(Items.AMETHYST_SHARD));
            }
            case "placeables" -> {
                List<Item> parts = FeatureStations.smallParts();
                if (!parts.isEmpty()) {
                    main = full(new ItemStack(parts.getFirst()));
                }
            }
            case "sinkdamper" -> main = full(new ItemStack(Items.LADDER));
            default -> {
            }
        }
        return new ItemStack[]{main, off};
    }

    /** Hoechste Stufe einer Tab-Zeile, oder leer. */
    private static ItemStack top(TcContext ctx, String row) {
        List<Item> items = ctx.rowItems(row);
        return items.isEmpty() ? ItemStack.EMPTY : new ItemStack(items.getLast());
    }

    /** Das Nugget der ersten Maschinen-Aufwertung (fuer die Nebenhand an der In-World-Station). */
    private static ItemStack firstUpgradeNugget() {
        for (Block block : BuiltInRegistries.BLOCK) {
            SledgehammerUpgrades.Upgrade upgrade = SledgehammerUpgrades.upgradeOf(block);
            // Nur Mod-Maschinen: die Vanilla-Kupfertruhen (erste Truhenstufe, Rissiger Diamant) stehen
            // in der Registry vorn, sind aber nicht die Maschinen-Aufwertung dieser Station.
            if (upgrade != null && upgrade.from() == block
                    && "simplebuilding".equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace())) {
                return new ItemStack(upgrade.nugget());
            }
        }
        return ItemStack.EMPTY;
    }
}

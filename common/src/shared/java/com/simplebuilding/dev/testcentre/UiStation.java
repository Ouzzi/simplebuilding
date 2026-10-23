package com.simplebuilding.dev.testcentre;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * UI-Bloecke und Tooltip-UIs (Nachtrag 13, Besitzer 06.10.: "alle UI-Bloecke und Tooltip-UIs, auch die Shulker- und
 * Endertruhen-Tooltips, mit Befuell-Knopf"): jeder Block mit Menue der Familie und von Vanilla (Varianten einmal) im
 * Bodenraster, je Namensraum eine Gruppe. Vorn der Befuell-Knopf ({@code /sbtestcentre fill ui @p}: jeder Behaelter des
 * Abschnitts bekommt Beispiel-Items, die Endertruhe des Spielers auch) und die Tooltip-Truhe mit gefuellten Shulkerkisten
 * und Buendeln - der Ausgabe-Knopf legt sie ins Inventar, Hovern zeigt den Inhalt (Simple Interfaces).
 */
public final class UiStation {

    public static final String SECTION = "ui";
    public static final String FILL_COMMAND = "sbtestcentre fill " + SECTION + " @p";
    /** Lage der Tooltip-Truhe (lokal). */
    static final BlockPos TOOLTIP_CHEST = new BlockPos(1, 0, 3);

    private UiStation() {
    }

    /** Ob ein Block ein Menue oeffnet: seine Block-Entity ist ein {@link MenuProvider}, oder er liefert selbst eines. */
    public static boolean hasMenu(Block block) {
        if (block instanceof EntityBlock entityBlock) {
            try {
                if (entityBlock.newBlockEntity(BlockPos.ZERO, block.defaultBlockState()) instanceof MenuProvider) {
                    return true;
                }
            } catch (RuntimeException e) {
                // Block-Entities, die eine Welt brauchen, zaehlen nur ueber die Klasse.
            }
        }
        for (Class<?> type = block.getClass(); type != null && type != BaseEntityBlock.class && type != Block.class
                && type != BlockBehaviour.class; type = type.getSuperclass()) {
            try {
                type.getDeclaredMethod("getMenuProvider", BlockState.class, Level.class, BlockPos.class);
                return true;
            } catch (NoSuchMethodException e) {
                // weiter zur Oberklasse
            }
        }
        return false;
    }

    /** Vanilla-Varianten, die nur einmal stehen: gefaerbte Shulkerkisten, verwitterte/gewachste Kupfertruhen, Ambossstufen. */
    static boolean variant(Identifier id) {
        if (!id.getNamespace().equals("minecraft")) {
            return false;
        }
        String path = id.getPath();
        return path.endsWith("_shulker_box") || path.contains("copper_chest") && !path.equals("copper_chest")
                || path.equals("chipped_anvil") || path.equals("damaged_anvil");
    }

    /**
     * Die UI-Bloecke: Familie zuerst, dann Vanilla, je nach Id; nur mit Item und ruhig setzbar. Die Endertruhe oeffnet ihr
     * Menue ohne {@link MenuProvider} (Inhalt beim Spieler) und steht wegen ihres Tooltips ausdruecklich dabei.
     */
    public static List<Block> uiBlocks() {
        List<Block> out = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            Identifier id = TcContext.id(block);
            if (!TcContext.isFamily(id) && !id.getNamespace().equals("minecraft") || block.asItem() == Items.AIR || variant(id)
                    || !hasMenu(block) && block != Blocks.ENDER_CHEST || Exhibits.blockStates(block).isEmpty()) {
                continue;
            }
            out.add(block);
        }
        out.sort(Comparator.comparing((Block block) -> !TcContext.isFamily(TcContext.id(block))).thenComparing(block -> TcContext.id(block).toString()));
        return out;
    }

    /** Beispiel-Items zum Befuellen; je Platz nimmt ein Behaelter das erste, das er annimmt (Ofen: Erz und Kohle ...). */
    public static List<ItemStack> samples() {
        return List.of(new ItemStack(Items.RAW_IRON, 16), new ItemStack(Items.COAL, 16), new ItemStack(Items.COBBLESTONE, 64),
                new ItemStack(Items.OAK_LOG, 32), new ItemStack(Items.BREAD, 8), new ItemStack(Items.DIAMOND, 3),
                new ItemStack(Items.REDSTONE, 32), new ItemStack(Items.IRON_INGOT, 9), new ItemStack(Items.NETHER_WART, 4),
                new ItemStack(Items.BLAZE_POWDER, 4), new ItemStack(Items.POTION), new ItemStack(Items.WHITE_WOOL, 16),
                new ItemStack(Items.ARROW, 16), new ItemStack(Items.BONE_MEAL, 12), new ItemStack(Items.PAPER, 8));
    }

    /**
     * Die Tooltip-Items: jede Shulkerkiste der Familie und die Vanilla-Grundform gefuellt, jedes Buendel (Komponente
     * Buendelinhalt) der Familie und das Vanilla-Buendel mit einigen Stapeln.
     */
    public static List<ItemStack> tooltipItems() {
        List<ItemStack> filler = samples();
        List<ItemStack> out = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier id = TcContext.id(item);
            boolean family = TcContext.isFamily(id);
            if (!family && !id.getNamespace().equals("minecraft") || variant(id)) {
                continue;
            }
            ItemStack stack = new ItemStack(item);
            if (item instanceof BlockItem block && (block.getBlock() instanceof ShulkerBoxBlock || id.getPath().endsWith("shulker_box"))) {
                stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(filler));
                out.add(stack);
            } else if (stack.has(DataComponents.BUNDLE_CONTENTS) && (family || item == Items.BUNDLE)) {
                stack.set(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY.copyWithContents(
                        java.util.stream.Stream.of(new ItemStack(Items.COBBLESTONE, 16), new ItemStack(Items.DIAMOND, 2), new ItemStack(Items.BREAD, 4))));
                out.add(stack);
            }
        }
        return out;
    }

    public static TcCanvas build(TcContext ctx) {
        TcCanvas c = new TcCanvas();
        List<Block> blocks = uiBlocks();
        if (blocks.isEmpty()) {
            return c;
        }
        c.command(1, 1, 1, Direction.NORTH, FILL_COMMAND, TcText.t("ui.fill", "Fill"), TcText.t("ui.fill.sub", "containers +"),
                TcText.t("ui.fill.sub2", "your ender chest"));
        c.place(TOOLTIP_CHEST.getX(), 0, TOOLTIP_CHEST.getZ(), TestCentreSections.facing(Blocks.CHEST.defaultBlockState(), Direction.NORTH));
        c.contents(TOOLTIP_CHEST.getX(), 0, TOOLTIP_CHEST.getZ(), tooltipItems());
        c.place(TOOLTIP_CHEST.getX(), 0, TOOLTIP_CHEST.getZ() + 1, TcCanvas.TRIM);
        c.wallSign(TOOLTIP_CHEST.getX(), 1, TOOLTIP_CHEST.getZ() + 1, TcText.bold(TcText.t("ui.tooltips", "Tooltips")),
                TcText.t("ui.tooltips.sub", "filled shulkers,"), TcText.t("ui.tooltips.sub2", "bundles: hover"));
        Map<String, List<BlockState>> groups = new LinkedHashMap<>();
        for (Block block : blocks) {
            groups.computeIfAbsent(TcContext.id(block).getNamespace(), ns -> new ArrayList<>()).add(Exhibits.blockStates(block).getFirst());
        }
        List<TcCanvas.Group> exhibits = new ArrayList<>();
        groups.forEach((ns, states) -> exhibits.add(new TcCanvas.Group(TcText.lit(ns), List.of(), states)));
        c.title(0, 1, TcCanvas.EXHIBIT_WALL_Z, TcText.t("section.ui", "UI blocks"), TcText.t("section.ui.sub", "open, fill, hover"));
        int end = c.exhibition(3, exhibits);
        c.backWall(0, end, TcCanvas.EXHIBIT_WALL_Z, TcCanvas.EXHIBIT_WALL_HEIGHT);
        c.anchor(SECTION, 1, 0, 0);
        return c;
    }

    /**
     * Der Befuell-Knopf: jeder Behaelter im Abschnitt {@code sectionId} bekommt seine geplanten Inhalte zurueck (die
     * Tooltip-Truhe) oder Beispiel-Items, die Endertruhe jedes Spielers ebenso. Liefert die Zahl gefuellter Behaelter.
     */
    public static int fill(ServerLevel level, TestCentreLayout.Plan plan, String sectionId, Collection<ServerPlayer> players) {
        TestCentreLayout.Section section = plan.section(sectionId);
        Map<BlockPos, List<ItemStack>> planned = new HashMap<>();
        for (TcOp op : section.ops()) {
            if (op instanceof TcOp.Fill fill) {
                planned.put(fill.pos(), fill.contents());
            }
        }
        BoundingBox box = section.box(plan.origin());
        int filled = 0;
        for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            if (level.getBlockEntity(pos) instanceof Container container) {
                List<ItemStack> contents = planned.get(pos.immutable());
                if (contents != null) {
                    container.clearContent();
                    for (int i = 0; i < contents.size() && i < container.getContainerSize(); i++) {
                        container.setItem(i, contents.get(i).copy());
                    }
                    container.setChanged();
                } else {
                    fillWithSamples(container);
                }
                if (!container.isEmpty()) {
                    filled++;
                }
            }
        }
        for (ServerPlayer player : players) {
            fillWithSamples(player.getEnderChestInventory());
        }
        return filled;
    }

    /** Jeder Platz bekommt das erste Beispiel (ab einem wechselnden Start), das der Behaelter dort annimmt. */
    public static void fillWithSamples(Container container) {
        List<ItemStack> samples = samples();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            for (int k = 0; k < samples.size(); k++) {
                ItemStack sample = samples.get((slot + k) % samples.size()).copy();
                if (container.canPlaceItem(slot, sample)) {
                    container.setItem(slot, sample);
                    break;
                }
            }
        }
        container.setChanged();
    }
}

package com.simplebuilding.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Rueckgaengig fuer den Baustab: nimmt die <b>letzte</b> Bau-Aktion eines Spielers zurueck -
 * Flaechenbau, Linie, Bruecke, Abdeckung, Oktant-Fuellung, Dach oder Blaupausen-Bau
 * (Entscheidung des Besitzers, 2026-09-25).
 *
 * <p>Regeln:
 * <ul>
 *   <li>Ausloeser: Schleichen + Rechtsklick in die Luft mit dem Baustab in der Haupthand.</li>
 *   <li>Nur die letzte Aktion; jede neue Aktion ersetzt sie. Ein zweites Rueckgaengig tut nichts.</li>
 *   <li>Nur in derselben Sitzung: gemerkt wird im Speicher am Spieler-Objekt. Nach Abmelden,
 *       Dimensionswechsel oder Serverneustart ist sie weg.</li>
 *   <li>Eine Stelle wird nur geraeumt, wenn dort noch derselbe Block steht, den die Aktion gesetzt
 *       hat - mit derselben Menge ({@link #AMOUNT_PROPERTIES}: eine inzwischen doppelte Stufe, vier
 *       statt einer Kerze bleiben stehen, Audit 2026-09-26, P3 #31) und denselben Flaechen (eine von
 *       Hand erweiterte Gluehflechte bleibt stehen) -, der Spieler dort bauen darf (dieselbe Pruefung wie
 *       beim Bauen: Welthoehe, Weltgrenze, Spawn-Schutz, Claim-Mods) und nichts verloren geht: ein
 *       Behaelter muss leer sein, jedes andere Block-Entity so, wie es gesetzt wurde - ein Lesepult mit
 *       Buch, ein Lagerfeuer mit Essen, ein Bienenstock mit Bienen bleiben stehen (Nach-Audit
 *       2026-09-27 N13; frueher schuetzte nur ein Behaelter, und Buch oder Essen gingen verloren). Im
 *       Zweifel bleibt der Block stehen (ein Sculk-Sensor, der seitdem etwas gehoert hat).</li>
 *   <li>Die Items gehen ins Inventar, was nicht passt, faellt vor die Fuesse. Im Kreativmodus
 *       gesetzte Bloecke kosteten nichts und geben nichts zurueck.</li>
 *   <li>Haltbarkeit und Hunger werden nicht erstattet.</li>
 *   <li>Aktionen ueber {@link #LIMIT} Bloecke werden nicht aufgezeichnet (Speicher); sie lassen sich
 *       nicht rueckgaengig machen, die Aktionsleiste sagt das.</li>
 * </ul>
 */
public final class WandUndo {
    /** Groesste Aktion, die noch aufgezeichnet wird. */
    public static final int LIMIT = 65_536;

    private static final Map<UUID, Record> RECORDS = new HashMap<>();

    /**
     * Eigenschaften, die sagen, wie viele Items in einem Block stecken. Steht dort nicht mehr die
     * gesetzte Menge, bleibt die Stelle stehen: sonst raeumte das Rueckgaengig eine von Hand
     * aufgedoppelte Stufe und erstattete nur die eine gesetzte.
     */
    private static final List<Property<?>> AMOUNT_PROPERTIES = List.of(
            BlockStateProperties.SLAB_TYPE,
            BlockStateProperties.CANDLES,
            BlockStateProperties.PICKLES,
            BlockStateProperties.EGGS,
            BlockStateProperties.FLOWER_AMOUNT,
            BlockStateProperties.SEGMENT_AMOUNT,
            BlockStateProperties.LAYERS);

    private WandUndo() {
    }

    private static final class Record {
        final Player player;
        final ResourceKey<Level> dimension;
        final boolean creative;
        final List<BlockPos> positions = new ArrayList<>();
        final List<BlockState> states = new ArrayList<>();
        final List<Item> items = new ArrayList<>();
        final List<Integer> counts = new ArrayList<>();
        /** Daten des Block-Entitys direkt nach dem Setzen, oder {@code null} (keins). */
        final List<CompoundTag> blockData = new ArrayList<>();
        boolean overflow;

        Record(Player player, Level level) {
            this.player = player;
            this.dimension = level.dimension();
            this.creative = player.getAbilities().instabuild;
        }
    }

    /** Beginnt eine neue Aktion; die vorige ist damit nicht mehr rueckgaengig zu machen. */
    public static void begin(Player player, Level level) {
        if (level.isClientSide()) {
            return;
        }
        RECORDS.values().removeIf(r -> r.player.isRemoved());
        RECORDS.put(player.getUUID(), new Record(player, level));
    }

    /**
     * Merkt eine gesetzte Stelle der laufenden Aktion: welcher Block dort steht und was dafuer
     * verbraucht wurde ({@code count} Stueck {@code item}; 0 fuer kostenlose Gegenstuecke).
     */
    public static void record(Player player, Level level, BlockPos pos, BlockState placed, Item item, int count) {
        Record record = RECORDS.get(player.getUUID());
        if (record == null || record.player != player || !record.dimension.equals(level.dimension()) || record.overflow) {
            return;
        }
        if (record.positions.size() >= LIMIT) {
            record.overflow = true;
            record.positions.clear();
            record.states.clear();
            record.items.clear();
            record.counts.clear();
            record.blockData.clear();
            return;
        }
        record.positions.add(pos.immutable());
        record.states.add(placed);
        record.items.add(item);
        record.counts.add(count);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        record.blockData.add(blockEntity == null ? null : blockEntity.saveWithoutMetadata(level.registryAccess()));
    }

    /** Gibt es fuer diesen Spieler etwas zurueckzunehmen? */
    public static boolean hasUndo(Player player) {
        Record record = RECORDS.get(player.getUUID());
        return record != null && record.player == player && (record.overflow || !record.positions.isEmpty());
    }

    /** Vergisst die gemerkte Aktion (Spieltests). */
    public static void forget(UUID player) {
        RECORDS.remove(player);
    }

    /**
     * Nimmt die letzte Aktion zurueck. Liefert die Zahl der geraeumten Stellen, oder -1, wenn es
     * nichts gab (dann sagt die Methode nichts, und der Klick geht an die Nebenhand weiter).
     */
    public static int undo(Player player, Level level) {
        Record record = RECORDS.get(player.getUUID());
        if (record == null || record.player != player) {
            return -1;
        }
        if (record.overflow) {
            RECORDS.remove(player.getUUID());
            player.displayClientMessage(Component.translatable("simplebuilding.wand.undo.too_big", LIMIT).withStyle(ChatFormatting.YELLOW), true);
            return 0;
        }
        if (record.positions.isEmpty()) {
            return -1;
        }
        RECORDS.remove(player.getUUID());
        if (!record.dimension.equals(level.dimension())) {
            player.displayClientMessage(Component.translatable("simplebuilding.wand.undo.other_dimension").withStyle(ChatFormatting.YELLOW), true);
            return 0;
        }
        // Erst pruefen, dann raeumen: das Entfernen einer Tuerhaelfte nimmt die andere mit, deren
        // Stelle soll trotzdem erstattet werden.
        int n = record.positions.size();
        boolean[] valid = new boolean[n];
        ItemStack wand = player.getMainHandItem();
        for (int i = 0; i < n; i++) {
            BlockPos pos = record.positions.get(i);
            // Zuerst das Recht (es fragt auch, ob der Chunk geladen ist - erst dann wird gelesen).
            if (!com.simplebuilding.items.custom.BuildingWandItem.mayBuildAt(level, player, pos, Direction.UP, wand)) {
                continue;
            }
            BlockState current = level.getBlockState(pos);
            valid[i] = sameAmount(current, record.states.get(i))
                    && sameFaces(current, record.states.get(i))
                    && unchangedContent(level, pos, record.blockData.get(i));
        }
        Map<Item, Integer> refund = new LinkedHashMap<>();
        int removed = 0;
        for (int i = n - 1; i >= 0; i--) {
            if (!valid[i]) {
                continue;
            }
            BlockPos pos = record.positions.get(i);
            if (level.getBlockState(pos).getBlock() == record.states.get(i).getBlock()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            removed++;
            if (!record.creative && record.counts.get(i) > 0 && record.items.get(i) != Items.AIR) {
                refund.merge(record.items.get(i), record.counts.get(i), Integer::sum);
            }
        }
        for (Map.Entry<Item, Integer> entry : refund.entrySet()) {
            give(player, entry.getKey(), entry.getValue());
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6F, 0.8F);
        player.displayClientMessage(Component.translatable("simplebuilding.wand.undo.done", removed, n - removed)
                .withStyle(ChatFormatting.GREEN), true);
        return removed;
    }

    /** Derselbe Block mit derselben Menge (Ausrichtung und Verbindungen duerfen sich aendern). */
    static boolean sameAmount(BlockState current, BlockState placed) {
        if (current.getBlock() != placed.getBlock()) {
            return false;
        }
        for (Property<?> property : AMOUNT_PROPERTIES) {
            if (placed.hasProperty(property) && current.hasProperty(property)
                    && !placed.getValue(property).equals(current.getValue(property))) {
                return false;
            }
        }
        return true;
    }

    /** Mehrflaechen-Bloecke (Gluehflechte, Sculk-Ader, Harzklumpen): dieselbe Zahl Flaechen wie gesetzt. */
    static boolean sameFaces(BlockState current, BlockState placed) {
        if (!(placed.getBlock() instanceof MultifaceBlock)) {
            return true;
        }
        return faceCount(current) == faceCount(placed);
    }

    private static int faceCount(BlockState state) {
        int n = 0;
        for (Direction direction : Direction.values()) {
            if (MultifaceBlock.hasFace(state, direction)) {
                n++;
            }
        }
        return n;
    }

    /**
     * Steht im Block-Entity noch, was beim Setzen darin stand? Ein Buch im Lesepult, Essen auf dem
     * Lagerfeuer, Items in einer Truhe - alles, was der Spieler seitdem hineingelegt hat, ginge beim
     * Raeumen verloren (ohne Drop, das Rueckgaengig erstattet nur den Block).
     */
    private static boolean unchangedContent(Level level, BlockPos pos, CompoundTag placedData) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return true;
        }
        if (blockEntity instanceof Container container) {
            // Behaelter zaehlen nur ihren Inhalt: Abkuehl- und Brennzeiten aendern sich von selbst.
            return container.isEmpty();
        }
        return placedData != null && placedData.equals(blockEntity.saveWithoutMetadata(level.registryAccess()));
    }

    private static void give(Player player, Item item, int count) {
        int max = Math.max(1, new ItemStack(item).getMaxStackSize());
        while (count > 0) {
            int part = Math.min(max, count);
            count -= part;
            ItemStack stack = new ItemStack(item, part);
            if (!player.getInventory().add(stack) || !stack.isEmpty()) {
                player.drop(stack, false);
            }
        }
    }
}

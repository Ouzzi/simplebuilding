package com.simplebuilding.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.state.BlockState;

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
 *       hat, der Spieler dort bauen darf und der Block keinen Inhalt hat (Truhen, Faesser,
 *       Shulkerkisten bleiben stehen).</li>
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

    private WandUndo() {
    }

    private static final class Record {
        final Player player;
        final ResourceKey<Level> dimension;
        final boolean creative;
        final List<BlockPos> positions = new ArrayList<>();
        final List<Block> blocks = new ArrayList<>();
        final List<Item> items = new ArrayList<>();
        final List<Integer> counts = new ArrayList<>();
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
            record.blocks.clear();
            record.items.clear();
            record.counts.clear();
            return;
        }
        record.positions.add(pos.immutable());
        record.blocks.add(placed.getBlock());
        record.items.add(item);
        record.counts.add(count);
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
            player.sendOverlayMessage(Component.translatable("simplebuilding.wand.undo.too_big", LIMIT).withStyle(ChatFormatting.YELLOW));
            return 0;
        }
        if (record.positions.isEmpty()) {
            return -1;
        }
        RECORDS.remove(player.getUUID());
        if (!record.dimension.equals(level.dimension())) {
            player.sendOverlayMessage(Component.translatable("simplebuilding.wand.undo.other_dimension").withStyle(ChatFormatting.YELLOW));
            return 0;
        }
        // Erst pruefen, dann raeumen: das Entfernen einer Tuerhaelfte nimmt die andere mit, deren
        // Stelle soll trotzdem erstattet werden.
        int n = record.positions.size();
        boolean[] valid = new boolean[n];
        for (int i = 0; i < n; i++) {
            BlockPos pos = record.positions.get(i);
            BlockState current = level.getBlockState(pos);
            valid[i] = current.getBlock() == record.blocks.get(i)
                    && level.mayInteract(player, pos)
                    && !(level.getBlockEntity(pos) instanceof Container);
        }
        Map<Item, Integer> refund = new LinkedHashMap<>();
        int removed = 0;
        for (int i = n - 1; i >= 0; i--) {
            if (!valid[i]) {
                continue;
            }
            BlockPos pos = record.positions.get(i);
            if (level.getBlockState(pos).getBlock() == record.blocks.get(i)) {
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
        player.sendOverlayMessage(Component.translatable("simplebuilding.wand.undo.done", removed, n - removed)
                .withStyle(ChatFormatting.GREEN));
        return removed;
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

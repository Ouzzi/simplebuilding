package com.simplebuilding.component;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Inhalt eines Rucksacks als Item-Komponente {@code simplebuilding:backpack_contents}.
 *
 * <p>Warum nicht Vanillas {@code minecraft:container}: dessen Codec laesst nur Stapel von 1 bis 99
 * zu, und {@code ItemStackTemplate#create()} verwirft auf 26.2 jeden Stapel ueber der normalen
 * Stapelgroesse still (Warnung im Log, leerer Stapel). Mit Tiefe Taschen liegen im Rucksack aber
 * bis zu viermal so grosse Stapel. Dieser Wert speichert deshalb eigene Eintraege
 * {@code (slot, id, count, components)} mit einer Zaehlgrenze von {@link #MAX_COUNT} und baut
 * daraus nie ueber {@code create()}, sondern direkt mit {@code new ItemStack(...)} Stapel.
 *
 * <p>Die Eintraege sind {@link ItemStackTemplate}s, nicht {@link ItemStack}s: ein Template ist ein
 * unveraenderlicher Record mit Wertgleichheit, und sein Codec liest die Item-Id ohne gebundene
 * Komponenten (auf 26.2 werden Komponenten erst nach dem Datapack-Laden gebunden). Wertgleichheit
 * braucht der getragene Rucksack: der offene Menue-Container vergleicht vor jedem Rueckschreiben
 * mit dem zuletzt geschriebenen Wert.
 *
 * <p>{@code slot} ist die stufenunabhaengige Komponenten-Id aus
 * {@link com.simplebuilding.items.custom.BackpackTier#componentSlot(int)}. Doppelte oder fuer eine
 * Stufe unbekannte Ids werden nie verworfen, sondern beim Oeffnen beiseitegelegt und beim
 * Zurueckschreiben unveraendert wieder angehaengt.
 */
public final class BackpackContents {
    /** Hoechste Komponenten-Slot-Id; die groesste Stufe braucht 0 bis 49. */
    public static final int MAX_SLOT = 63;
    /** Tiefe Taschen II auf der groessten Vanilla-Stapelgroesse (99). */
    public static final int MAX_COUNT = 99 * 4;

    public static final BackpackContents EMPTY = new BackpackContents(List.of());

    /** Ein belegter Slot. */
    public record Entry(int slot, ItemStackTemplate stack) {
        public ItemStack toStack() {
            return new ItemStack(this.stack.item(), this.stack.count(), this.stack.components());
        }

        public static Entry of(int slot, ItemStack stack) {
            return new Entry(slot, new ItemStackTemplate(stack.typeHolder(), stack.getCount(), stack.getComponentsPatch()));
        }
    }

    private static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(i -> i.group(
            ExtraCodecs.intRange(0, MAX_SLOT).fieldOf("slot").forGetter(Entry::slot),
            Item.CODEC.fieldOf("id").forGetter(e -> e.stack().item()),
            ExtraCodecs.intRange(1, MAX_COUNT).fieldOf("count").forGetter(e -> e.stack().count()),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(e -> e.stack().components())
    ).apply(i, (slot, item, count, patch) -> new Entry(slot, new ItemStackTemplate(item, count, patch))));

    private static final Codec<List<Entry>> STRICT_LIST_CODEC = ENTRY_CODEC.sizeLimitedListOf(MAX_SLOT + 1);

    /**
     * Writes exactly like a plain list of entries; reads entry by entry. One entry that no longer
     * decodes (an item id that is gone - a removed mod, or a vanilla item renamed by a Minecraft
     * update that did not pass through {@link com.simplebuilding.datafix.ModDataFixer}) used to fail
     * the whole list, and the backpack opened empty. Now only that entry is dropped, with a warning.
     */
    private static final Codec<List<Entry>> LENIENT_LIST_CODEC = new Codec<>() {
        @Override
        public <T> DataResult<T> encode(List<Entry> input, DynamicOps<T> ops, T prefix) {
            return STRICT_LIST_CODEC.encode(input, ops, prefix);
        }

        @Override
        public <T> DataResult<Pair<List<Entry>, T>> decode(DynamicOps<T> ops, T input) {
            return ops.getStream(input).flatMap(stream -> {
                List<T> raw = stream.toList();
                if (raw.size() > MAX_SLOT + 1) {
                    return DataResult.error(() -> "Backpack contents too long: " + raw.size() + " > " + (MAX_SLOT + 1));
                }
                List<Entry> entries = new ArrayList<>(raw.size());
                for (T element : raw) {
                    ENTRY_CODEC.parse(ops, element)
                            .resultOrPartial(error -> LOGGER.warn("Dropping unreadable backpack entry {}: {}", element, error))
                            .ifPresent(entries::add);
                }
                return DataResult.success(Pair.of(entries, ops.empty()));
            });
        }
    };

    public static final Codec<BackpackContents> CODEC = LENIENT_LIST_CODEC
            .xmap(BackpackContents::new, BackpackContents::entries);

    private static final Logger LOGGER = LoggerFactory.getLogger("simplebuilding");

    private static final StreamCodec<RegistryFriendlyByteBuf, Entry> ENTRY_STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Entry::slot,
            Item.STREAM_CODEC, e -> e.stack().item(),
            ByteBufCodecs.VAR_INT, e -> e.stack().count(),
            DataComponentPatch.STREAM_CODEC, e -> e.stack().components(),
            BackpackContents::decodeEntry);

    public static final StreamCodec<RegistryFriendlyByteBuf, BackpackContents> STREAM_CODEC = ENTRY_STREAM_CODEC
            .apply(ByteBufCodecs.list(MAX_SLOT + 1))
            .map(BackpackContents::new, BackpackContents::entries);

    private static Entry decodeEntry(int slot, Holder<Item> item, int count, DataComponentPatch patch) {
        if (slot < 0 || slot > MAX_SLOT || count < 1 || count > MAX_COUNT) {
            throw new DecoderException("Invalid backpack entry: slot " + slot + ", count " + count);
        }
        return new Entry(slot, new ItemStackTemplate(item, count, patch));
    }

    private final List<Entry> entries;

    public BackpackContents(List<Entry> entries) {
        this.entries = List.copyOf(entries);
    }

    public List<Entry> entries() {
        return this.entries;
    }

    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    public int size() {
        return this.entries.size();
    }

    /** Frische Kopie des Stapels am Eintrag {@code index}. */
    public ItemStack stackAt(int index) {
        return this.entries.get(index).toStack();
    }

    /** Frische Kopien aller Stapel, in Eintragsreihenfolge. */
    public List<ItemStack> stacks() {
        List<ItemStack> result = new ArrayList<>(this.entries.size());
        for (Entry entry : this.entries) {
            result.add(entry.toStack());
        }
        return result;
    }

    /** Index des ersten Eintrags, dessen Stapel {@code test} erfuellt, sonst -1. */
    public int indexOf(Predicate<ItemStack> test) {
        for (int i = 0; i < this.entries.size(); i++) {
            if (test.test(this.entries.get(i).toStack())) {
                return i;
            }
        }
        return -1;
    }

    /** Neuer Wert mit {@code stack} am Eintrag {@code index}; ein leerer Stapel entfernt den Eintrag. */
    public BackpackContents withStackAt(int index, ItemStack stack) {
        List<Entry> copy = new ArrayList<>(this.entries);
        if (stack.isEmpty()) {
            copy.remove(index);
        } else {
            copy.set(index, Entry.of(copy.get(index).slot(), stack));
        }
        return new BackpackContents(copy);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof BackpackContents that && this.entries.equals(that.entries));
    }

    @Override
    public int hashCode() {
        return this.entries.hashCode();
    }

    @Override
    public String toString() {
        return "BackpackContents" + this.entries;
    }
}

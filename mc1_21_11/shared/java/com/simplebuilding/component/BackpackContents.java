package com.simplebuilding.component;

import com.mojang.serialization.Codec;
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

/**
 * Inhalt eines Rucksacks als Item-Komponente {@code simplebuilding:backpack_contents}.
 *
 * <p>Warum nicht Vanillas {@code minecraft:container}: dessen Codec laesst nur Stapel von 1 bis 99
 * zu. Mit Tiefe Taschen liegen im Rucksack aber bis zu viermal so grosse Stapel. Dieser Wert
 * speichert deshalb eigene Eintraege {@code (slot, id, count, components)} mit einer Zaehlgrenze von
 * {@link #MAX_COUNT}.
 *
 * <p>MC 1.21.11: Die Eintraege halten {@link ItemStack}s (auf 26.2 unveraenderliche
 * {@code ItemStackTemplate}s, die es hier noch nicht gibt). Deshalb kopiert dieser Wert jeden
 * Stapel beim Hinein- und Herausgeben und vergleicht per {@link ItemStack#matches} statt per
 * Identitaet - der offene Menue-Container vergleicht vor jedem Rueckschreiben mit dem zuletzt
 * geschriebenen Wert.
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

    /** Ein belegter Slot; der Stapel wird nie herausgereicht, nur Kopien davon. */
    public static final class Entry {
        private final int slot;
        private final ItemStack stack;

        private Entry(int slot, ItemStack stack) {
            this.slot = slot;
            this.stack = stack;
        }

        public int slot() {
            return this.slot;
        }

        public ItemStack toStack() {
            return this.stack.copy();
        }

        public static Entry of(int slot, ItemStack stack) {
            return new Entry(slot, stack.copy());
        }

        private Holder<Item> item() {
            return this.stack.getItemHolder();
        }

        @Override
        public boolean equals(Object other) {
            return this == other || (other instanceof Entry that && this.slot == that.slot && ItemStack.matches(this.stack, that.stack));
        }

        @Override
        public int hashCode() {
            return 31 * (31 * this.slot + ItemStack.hashItemAndComponents(this.stack)) + this.stack.getCount();
        }

        @Override
        public String toString() {
            return this.slot + "=" + this.stack;
        }
    }

    private static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(i -> i.group(
            ExtraCodecs.intRange(0, MAX_SLOT).fieldOf("slot").forGetter(Entry::slot),
            Item.CODEC.fieldOf("id").forGetter(Entry::item),
            ExtraCodecs.intRange(1, MAX_COUNT).fieldOf("count").forGetter(e -> e.stack.getCount()),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(e -> e.stack.getComponentsPatch())
    ).apply(i, (slot, item, count, patch) -> new Entry(slot, new ItemStack(item, count, patch))));

    public static final Codec<BackpackContents> CODEC = ENTRY_CODEC.sizeLimitedListOf(MAX_SLOT + 1)
            .xmap(BackpackContents::new, BackpackContents::entries);

    private static final StreamCodec<RegistryFriendlyByteBuf, Entry> ENTRY_STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Entry::slot,
            Item.STREAM_CODEC, Entry::item,
            ByteBufCodecs.VAR_INT, e -> e.stack.getCount(),
            DataComponentPatch.STREAM_CODEC, e -> e.stack.getComponentsPatch(),
            BackpackContents::decodeEntry);

    public static final StreamCodec<RegistryFriendlyByteBuf, BackpackContents> STREAM_CODEC = ENTRY_STREAM_CODEC
            .apply(ByteBufCodecs.list(MAX_SLOT + 1))
            .map(BackpackContents::new, BackpackContents::entries);

    private static Entry decodeEntry(int slot, Holder<Item> item, int count, DataComponentPatch patch) {
        if (slot < 0 || slot > MAX_SLOT || count < 1 || count > MAX_COUNT) {
            throw new DecoderException("Invalid backpack entry: slot " + slot + ", count " + count);
        }
        return new Entry(slot, new ItemStack(item, count, patch));
    }

    private final List<Entry> entries;

    public BackpackContents(List<Entry> entries) {
        List<Entry> nonEmpty = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            if (!entry.stack.isEmpty()) {
                nonEmpty.add(entry);
            }
        }
        this.entries = List.copyOf(nonEmpty);
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

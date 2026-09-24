package com.simplebuilding.screen;

import com.simplebuilding.component.BackpackContents;
import com.simplebuilding.items.custom.BackpackItem;
import com.simplebuilding.items.custom.BackpackTier;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Die Slots eines geoeffneten (oder abgestellten) Rucksacks.
 *
 * <p>Bewusst ein eigener {@link Container} statt {@code SimpleContainer}: dessen {@code setItem}
 * kuerzt jeden Stapel auf {@code getMaxStackSize(stack)}. Wurde Tiefe Taschen am Schleifstein
 * entfernt, liegen aber weiter uebergrosse Stapel im Rucksack; sie duerfen herausgenommen, aber
 * nicht beim Laden abgeschnitten werden. Hier wird deshalb nie gekuerzt - die Grenzen setzen die
 * Slots ({@link BackpackSlot}) beim Einlegen.
 *
 * <p>Eintraege aus der Komponente, die diese Stufe nicht kennt (doppelte oder fremde Slot-Ids,
 * etwa aus {@code /item}), landen in {@link #overflow} und werden beim Zurueckschreiben
 * unveraendert wieder angehaengt: verworfen wird nie etwas.
 */
public class BackpackContainer implements Container {
    protected final BackpackTier tier;
    protected final int multiplier;
    private final NonNullList<ItemStack> items;
    private final List<BackpackContents.Entry> overflow = new ArrayList<>();
    private Runnable changeListener = () -> {};

    public BackpackContainer(BackpackTier tier, int multiplier) {
        this.tier = tier;
        this.multiplier = Math.max(1, multiplier);
        this.items = NonNullList.withSize(tier.slotCount(), ItemStack.EMPTY);
    }

    public BackpackTier tier() {
        return this.tier;
    }

    /** Stapelfaktor aus Tiefe Taschen (1, 2 oder 4). Ein abgestellter Rucksack liest ihn live. */
    public int multiplier() {
        return this.multiplier;
    }

    /** Wird nach jeder Aenderung gerufen (abgestellter Rucksack: {@code BlockEntity#setChanged}). */
    public void setChangeListener(Runnable listener) {
        this.changeListener = listener;
    }

    // --- Komponente <-> Slots ---

    /** Laedt {@code contents} und ersetzt alles, was bisher in den Slots lag. */
    public void load(BackpackContents contents) {
        for (int i = 0; i < this.items.size(); i++) {
            this.items.set(i, ItemStack.EMPTY);
        }
        this.overflow.clear();
        for (BackpackContents.Entry entry : contents.entries()) {
            int index = this.tier.containerIndex(entry.slot());
            if (index >= 0 && this.items.get(index).isEmpty()) {
                this.items.set(index, entry.toStack());
            } else {
                this.overflow.add(entry);
            }
        }
    }

    /** Der aktuelle Inhalt als Komponenten-Wert, beiseitegelegte Eintraege eingeschlossen. */
    public BackpackContents toContents() {
        List<BackpackContents.Entry> entries = new ArrayList<>();
        for (int i = 0; i < this.items.size(); i++) {
            ItemStack stack = this.items.get(i);
            if (!stack.isEmpty()) {
                entries.add(BackpackContents.Entry.of(this.tier.componentSlot(i), stack));
            }
        }
        entries.addAll(this.overflow);
        return entries.isEmpty() ? BackpackContents.EMPTY : new BackpackContents(entries);
    }

    /**
     * Legt so viel von {@code stack} ein wie passt - erst auf passende Stapel, dann in leere Slots -
     * und verkleinert {@code stack} entsprechend. Liefert die eingelegte Anzahl.
     */
    public int insert(ItemStack stack) {
        if (stack.isEmpty() || !BackpackItem.mayStore(stack)) {
            return 0;
        }
        int before = stack.getCount();
        int max = getMaxStackSize(stack);
        for (int i = 0; i < this.items.size() && !stack.isEmpty(); i++) {
            ItemStack existing = this.items.get(i);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < max) {
                int moved = Math.min(max - existing.getCount(), stack.getCount());
                existing.grow(moved);
                stack.shrink(moved);
            }
        }
        for (int i = 0; i < this.items.size() && !stack.isEmpty(); i++) {
            if (this.items.get(i).isEmpty()) {
                this.items.set(i, stack.split(Math.min(max, stack.getCount())));
            }
        }
        int inserted = before - stack.getCount();
        if (inserted > 0) {
            setChanged();
        }
        return inserted;
    }

    /** Liegt schon ein Stapel dieser Sorte (gleiche Komponenten) im Rucksack? */
    public boolean containsSameItem(ItemStack stack) {
        for (ItemStack existing : this.items) {
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack)) {
                return true;
            }
        }
        return false;
    }

    /** Vor jedem Lesen: externe Aenderungen einer Quelle uebernehmen. Hier: nichts zu tun. */
    public void syncFromSource() {
    }

    /** Ausstehende Aenderungen an die Quelle weitergeben. Hier: nichts zu tun. */
    public void flush() {
    }

    // --- Container ---

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return this.overflow.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < this.items.size() ? this.items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack removed = ContainerHelper.removeItem(this.items, slot, count);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        // Absichtlich ohne limitSize - siehe Klassenkommentar.
        this.items.set(slot, stack);
        setChanged();
    }

    @Override
    public int getMaxStackSize() {
        // Vanillas Container-Obergrenze (99) mal Faktor; die Grenze je Item setzt getMaxStackSize(stack).
        return 99 * multiplier();
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return BackpackItem.maxStackSizeIn(stack, multiplier());
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return BackpackItem.mayStore(stack);
    }

    @Override
    public void setChanged() {
        this.changeListener.run();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < this.items.size(); i++) {
            this.items.set(i, ItemStack.EMPTY);
        }
        this.overflow.clear();
        setChanged();
    }
}

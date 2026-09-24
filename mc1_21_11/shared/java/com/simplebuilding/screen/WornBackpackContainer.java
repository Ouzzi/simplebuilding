package com.simplebuilding.screen;

import com.simplebuilding.component.BackpackContents;
import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.items.custom.BackpackItem;
import java.util.Objects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Der Container eines <em>getragenen</em> Rucksacks: er schreibt jede Aenderung sofort in die
 * Komponente {@code simplebuilding:backpack_contents} des Brust-Stapels zurueck.
 *
 * <p><b>Sofort, nicht erst beim Schliessen.</b> Stirbt der Spieler mit offenem Menue, faellt der
 * Rucksack mit dem Inhalt heraus, der in seiner Komponente steht. Ein Gegenstand, der gerade aus
 * dem Inventar in den Rucksack gelegt wurde, waere sonst verloren: aus dem Inventar schon weg, in
 * der Komponente noch nicht angekommen. Geschrieben wird nur bei echter Aenderung
 * (Wertvergleich mit {@link #lastWritten}), damit kein Klick ohne Wirkung ein
 * Ausruestungs-Paket an die Umgebung ausloest.
 * Mehrere Schreibvorgaenge in einem Tick kosten trotzdem nur ein Paket: Vanilla vergleicht die
 * Ausruestung einmal pro Tick ({@code LivingEntity#detectEquipmentUpdates}) und schickt den
 * Brust-Stapel samt Inhalt nur dann an Besitzer und Umgebung - die bewusst in Kauf genommene
 * Bandbreite eines getragenen Rucksacks (wie bei einer gehaltenen Shulkerkiste).
 *
 * <p><b>Nur solange er getragen wird.</b> Geschrieben wird nur, wenn der Brust-Slot noch genau
 * <em>diesen</em> Stapel haelt (Identitaet). Ein Menue, dessen Rucksack weg ist, schliesst
 * {@link #stillValid} beim naechsten Tick.
 *
 * <p><b>Aenderungen von aussen gewinnen.</b> Der Bauzauberstab (Meisterbauer), Trichter,
 * Konstrukteurs Hand und die Meisterbauer-Blockwahl nehmen direkt aus der Komponente, auch
 * waehrend das Menue offen ist. {@link #syncFromSource()} laedt dann neu, bevor das Menue liest
 * oder schreibt - sonst wuerde das naechste Rueckschreiben den Verbrauch ungeschehen machen und
 * Gegenstaende verdoppeln.
 */
public class WornBackpackContainer extends BackpackContainer {
    private final Player owner;
    private final ItemStack backingStack;
    private BackpackContents lastWritten;

    public WornBackpackContainer(Player owner, ItemStack backingStack, int multiplier) {
        super(((BackpackItem) backingStack.getItem()).getTier(), multiplier);
        this.owner = owner;
        this.backingStack = backingStack;
        this.lastWritten = currentComponent();
        load(this.lastWritten);
    }

    public ItemStack backingStack() {
        return this.backingStack;
    }

    public boolean isStillWorn() {
        return this.owner.getItemBySlot(EquipmentSlot.CHEST) == this.backingStack;
    }

    private BackpackContents currentComponent() {
        return this.backingStack.getOrDefault(ModDataComponentTypes.BACKPACK_CONTENTS, BackpackContents.EMPTY);
    }

    @Override
    public void syncFromSource() {
        if (!isStillWorn()) {
            return;
        }
        BackpackContents current = currentComponent();
        if (!Objects.equals(current, this.lastWritten)) {
            this.lastWritten = current;
            load(current);
        }
    }

    @Override
    public void flush() {
        writeBack();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        writeBack();
    }

    private void writeBack() {
        if (!isStillWorn()) {
            return;
        }
        BackpackContents contents = toContents();
        if (contents.equals(this.lastWritten)) {
            return;
        }
        if (contents.isEmpty()) {
            this.backingStack.remove(ModDataComponentTypes.BACKPACK_CONTENTS);
        } else {
            this.backingStack.set(ModDataComponentTypes.BACKPACK_CONTENTS, contents);
        }
        this.lastWritten = contents;
    }

    @Override
    public boolean stillValid(Player player) {
        return player == this.owner && player.isAlive() && !player.isSpectator() && isStillWorn();
    }
}

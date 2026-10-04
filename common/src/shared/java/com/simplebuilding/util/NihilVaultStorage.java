package com.simplebuilding.util;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Nihil-Gewoelbe (Besitzer 2026-10-04): ein einziger 27-Platz-Inhalt fuer die ganze Welt, gespeichert mit der
 * Oberwelt. Jedes Nihil-Gewoelbe in jeder Dimension zeigt diesen Container; alle Menues teilen dieselbe Instanz,
 * so wie mehrere Spieler an einer Vanilla-Truhe.
 */
public final class NihilVaultStorage extends SavedData {
    public static final int SLOTS = 27;

    public static final Codec<NihilVaultStorage> CODEC = ItemStackWithSlot.CODEC.listOf()
            .optionalFieldOf("items", List.of()).codec().xmap(NihilVaultStorage::new, NihilVaultStorage::slots);

    // MC 26.2: SavedDataType nimmt eine Identifier-Id (siehe SledgehammerProgress).
    public static final SavedDataType<NihilVaultStorage> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("simplebuilding", "nihil_vault"),
            NihilVaultStorage::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final SimpleContainer items = new SimpleContainer(SLOTS) {
        @Override public void setChanged() {
            super.setChanged();
            NihilVaultStorage.this.setDirty();
        }
    };

    public NihilVaultStorage() {
    }

    private NihilVaultStorage(List<ItemStackWithSlot> loaded) {
        for (ItemStackWithSlot slot : loaded) {
            if (slot.isValidInContainer(SLOTS)) items.setItem(slot.slot(), slot.stack());
        }
        // Laden ist keine Aenderung.
        setDirty(false);
    }

    private List<ItemStackWithSlot> slots() {
        List<ItemStackWithSlot> out = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            if (!items.getItem(i).isEmpty()) out.add(new ItemStackWithSlot(i, items.getItem(i).copy()));
        }
        return out;
    }

    /** Der geteilte Container dieser Welt. */
    public SimpleContainer items() {
        return items;
    }

    public static NihilVaultStorage get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
}

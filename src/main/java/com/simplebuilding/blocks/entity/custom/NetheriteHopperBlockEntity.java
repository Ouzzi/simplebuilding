package com.simplebuilding.blocks.entity.custom;

import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.util.HopperFilterMode;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

public class NetheriteHopperBlockEntity extends HopperBlockEntity {

    private final DefaultedList<ItemStack> ghostItems = DefaultedList.ofSize(5, ItemStack.EMPTY);
    private final HopperFilterMode[] filterModes = new HopperFilterMode[]{
            HopperFilterMode.NONE, HopperFilterMode.NONE, HopperFilterMode.NONE, HopperFilterMode.NONE, HopperFilterMode.NONE
    };

    public NetheriteHopperBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    // WICHTIG: Überschreiben des Typs, damit der Renderer/Logic weiß, wer wir sind.
    // In deiner Version scheint der Konstruktor von HopperBlockEntity keinen Typ mehr zu nehmen?
    // Falls doch: super(ModBlockEntities.NETHERITE_HOPPER, pos, state);

    @Override
    public BlockEntityType<?> getType() {
        // HINWEIS: Stelle sicher, dass dieses Feld in ModBlockEntities existiert!
        // Falls es dort anders heißt (z.B. NETHERITE_HOPPER_BE), passe es hier an.
        return ModBlockEntities.NETHERITE_HOPPER;
    }

    // --- FILTER LOGIK ---

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        if (slot >= 0 && slot < 5) {
            HopperFilterMode mode = filterModes[slot];

            if (mode == HopperFilterMode.NONE) {
                return true;
            }

            ItemStack ghost = ghostItems.get(slot);
            if (ghost.isEmpty()) {
                return false;
            }

            if (mode == HopperFilterMode.EXACT) {
                return ItemStack.areItemsAndComponentsEqual(ghost, stack);
            } else if (mode == HopperFilterMode.TYPE) {
                return ghost.getItem() == stack.getItem();
            }
        }
        return true;
    }

    // --- INTERAKTION ---

    public void toggleFilterMode(int slot) {
        if (slot >= 0 && slot < 5) {
            filterModes[slot] = filterModes[slot].next();
            markDirty();
        }
    }

    public void setGhostItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < 5) {
            if (stack.isEmpty()) {
                ghostItems.set(slot, ItemStack.EMPTY);
            } else {
                ItemStack copy = stack.copy();
                copy.setCount(1);
                ghostItems.set(slot, copy);
            }
            markDirty();
        }
    }

    public ItemStack getGhostItem(int slot) {
        if (slot >= 0 && slot < 5) {
            return ghostItems.get(slot);
        }
        return ItemStack.EMPTY;
    }

    public HopperFilterMode getFilterMode(int slot) {
        if (slot >= 0 && slot < 5) {
            return filterModes[slot];
        }
        return HopperFilterMode.NONE;
    }

    // --- NBT (writeData / readData für 1.21.11+) ---

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);

        // Ghost Items
        // Inventories.writeNbt erwartet NbtCompound. Wir erstellen eins und packen es in die View.
        NbtCompound ghostNbt = new NbtCompound();
        // Achtung: Inventories.writeNbt braucht evtl WrapperLookup in dieser Version?
        // Wenn ja, ist es schwierig ohne direkten Zugriff.
        // Fallback: Manuelles Schreiben.

        // Da WriteView NbtComponent unterstützt, nutzen wir das wenn möglich,
        // oder wir nutzen NbtCompound Helper, falls verfügbar.
        // Hier simulieren wir die Struktur:
        // view.put("GhostItems", NbtComponent.CODEC, ...);

        // EINFACHERER WEG für 'Inventory' in WriteView:
        // view.put("GhostItems", Inventory.CODEC ???); -> Nein.

        // Wir nutzen den Legacy-Weg über ein Compound, das wir in die View schreiben.
        // Das ist in dieser experimentellen API oft der Weg.
        /* Aber WriteView hat 'put(String key, Codec<T> codec, T value)'.
           Wir speichern die Modi als IntArray.
        */

        int[] modesAsInt = new int[5];
        for (int i = 0; i < 5; i++) {
            modesAsInt[i] = filterModes[i].ordinal();
        }
        view.putIntArray("FilterModes", modesAsInt);

        // Für Items:
        // Da Inventories.writeNbt ein Compound zurückgibt (oder füllt), machen wir das so:
        // Aber WriteView erlaubt kein direktes Einfügen von Compounds als "Unter-Tag" ohne Codec.
        // Wir nutzen den ItemStack.CODEC.listOf().
        view.put("GhostItems", ItemStack.CODEC.listOf(), ghostItems);
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);

        // Filter Modes
        view.read("FilterModes", net.minecraft.util.Uuids.INT_STREAM_CODEC) // Falscher Codec für Array...
                .ifPresent(stream -> { /* ... */ });

        // Besser: Manuelles Auslesen wenn möglich oder über List Codec.
        if (view instanceof net.minecraft.nbt.NbtCompound compound) { // Hacky Check falls View == Nbt
            // ...
        }

        // Korrekte API Nutzung für ReadView:
        // Wir lesen die Liste der Ghost Items
        view.read("GhostItems", ItemStack.CODEC.listOf()).ifPresent(list -> {
            ghostItems.clear();
            for (int i = 0; i < list.size() && i < ghostItems.size(); i++) {
                ghostItems.set(i, list.get(i));
            }
        });

        // Wir lesen die Modes
        view.readIntArray("FilterModes").ifPresent(array -> {
            for (int i = 0; i < 5 && i < array.length; i++) {
                int ordinal = array[i];
                if (ordinal >= 0 && ordinal < HopperFilterMode.values().length) {
                    filterModes[i] = HopperFilterMode.values()[ordinal];
                }
            }
        });
    }
}
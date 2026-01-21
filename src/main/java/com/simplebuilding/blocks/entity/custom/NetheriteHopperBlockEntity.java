package com.simplebuilding.blocks.entity.custom;

import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.util.HopperFilterMode;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;

public class NetheriteHopperBlockEntity extends HopperBlockEntity {

    private final DefaultedList<ItemStack> ghostItems = DefaultedList.ofSize(5, ItemStack.EMPTY);
    private final HopperFilterMode[] filterModes = new HopperFilterMode[5];

    public NetheriteHopperBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
        // Initialisiere Arrays um NullPointer zu vermeiden
        Arrays.fill(filterModes, HopperFilterMode.NONE);
    }

    @Override
    public BlockEntityType<?> getType() {
        // FIX 1: Der Name in ModBlockEntities ist MOD_HOPPER_BE
        return ModBlockEntities.MOD_HOPPER_BE;
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
                // Wenn Filter an ist, aber kein Ghost Item gesetzt, lassen wir nichts rein (oder true, je nach Design)
                return true;
            }

            // FIX 2: EXACT existiert nicht im Enum, 'WHITELIST' ist das Äquivalent
            if (mode == HopperFilterMode.WHITELIST) {
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

    // --- NBT (1.21 API) ---

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);

        // Filter Modi als Int-Array speichern
        int[] modesAsInt = new int[5];
        for (int i = 0; i < 5; i++) {
            modesAsInt[i] = filterModes[i].ordinal();
        }
        view.putIntArray("FilterModes", modesAsInt);

        // Ghost Items speichern
        // Wir erstellen einen Sub-View (Compound) für die Items und nutzen Inventories Helper
        WriteView ghostView = view.get("GhostItems"); // oder .getOrCreateView("GhostItems") je nach Mapping
        if (ghostView != null) {
            Inventories.writeData(ghostView, ghostItems);
        } else {
            // Fallback: Manuell speichern via Codec, falls Inventories nicht mit WriteView kompatibel ist
            view.put("GhostItems", ItemStack.CODEC.listOf(), ghostItems);
        }
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);

        // FIX 3: getOptionalReadView statt Casting auf NbtCompound
        view.getOptionalReadView("GhostItems").ifPresent(ghostView -> {
            // Versuche Inventories.readData
            try {
                Inventories.readData(ghostView, ghostItems);
            } catch (Exception e) {
                // Fallback: Wenn Struktur anders ist (z.B. List Codec)
                ghostItems.clear();
            }
        });

        // Fallback Lesen über Codec falls Inventories fehlschlägt oder nicht genutzt wurde
        if (ghostItems.stream().allMatch(ItemStack::isEmpty)) {
            view.read("GhostItems", ItemStack.CODEC.listOf()).ifPresent(list -> {
                for (int i = 0; i < list.size() && i < ghostItems.size(); i++) {
                    ghostItems.set(i, list.get(i));
                }
            });
        }

        // FIX 4: getOptionalIntArray statt readIntArray
        view.getOptionalIntArray("FilterModes").ifPresent(array -> {
            for (int i = 0; i < 5 && i < array.length; i++) {
                int ordinal = array[i];
                if (ordinal >= 0 && ordinal < HopperFilterMode.values().length) {
                    filterModes[i] = HopperFilterMode.values()[ordinal];
                }
            }
        });
    }

}
package com.simplebuilding.blocks.entity.custom;

import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.util.HopperFilterMode;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

public class NetheriteHopperBlockEntity extends ModHopperBlockEntity { // Oder HopperBlockEntity wenn du keine Mod-Basis hast

    // 5 Slots für Ghost Items (separat vom echten Inventar)
    private final DefaultedList<ItemStack> ghostItems = DefaultedList.ofSize(5, ItemStack.EMPTY);
    private final HopperFilterMode[] filterModes = new HopperFilterMode[]{
            HopperFilterMode.NONE, HopperFilterMode.NONE, HopperFilterMode.NONE, HopperFilterMode.NONE, HopperFilterMode.NONE
    };

    public NetheriteHopperBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NETHERITE_HOPPER_BE, pos, state);
    }

    // --- FILTER LOGIK ---

    /**
     * Diese Methode wird von Minecraft (und Hoppern darüber) aufgerufen, um zu prüfen,
     * ob ein Item in einen bestimmten Slot darf.
     */
    @Override
    public boolean isValid(int slot, ItemStack stack) {
        HopperFilterMode mode = filterModes[slot];

        // 1. Wenn Modus NONE ist -> Alles erlauben (Vanilla Verhalten)
        if (mode == HopperFilterMode.NONE) {
            return true;
        }

        // 2. Wenn Modus an ist, aber kein Ghost Item gesetzt -> Nichts erlauben (oder alles blockieren)
        ItemStack ghost = ghostItems.get(slot);
        if (ghost.isEmpty()) {
            return false; // Filter ist an, aber nichts definiert -> Blockiert
        }

        // 3. Filter Prüfung
        if (mode == HopperFilterMode.EXACT) {
            // Muss exakt übereinstimmen (inkl. Komponenten/NBT)
            return ItemStack.areItemsAndComponentsEqual(ghost, stack);
        } else if (mode == HopperFilterMode.TYPE) {
            // Nur das Item muss stimmen (z.B. Diamantschwert == Diamantschwert, egal welche Enchants)
            return ghost.getItem() == stack.getItem();
        }

        return true;
    }

    // --- INTERAKTION ---

    public void toggleFilterMode(int slot) {
        filterModes[slot] = filterModes[slot].next();
        markDirty();
    }

    public void setGhostItem(int slot, ItemStack stack) {
        // Wir speichern nur eine Kopie (Größe 1), da es ein Ghost Item ist
        if (stack.isEmpty()) {
            ghostItems.set(slot, ItemStack.EMPTY);
        } else {
            ItemStack copy = stack.copy();
            copy.setCount(1);
            ghostItems.set(slot, copy);
        }
        markDirty();
    }

    public ItemStack getGhostItem(int slot) {
        return ghostItems.get(slot);
    }

    public HopperFilterMode getFilterMode(int slot) {
        return filterModes[slot];
    }

    // --- SPEICHERN & LADEN ---

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        
        // Ghost Items speichern
        NbtCompound ghostNbt = new NbtCompound();
        Inventories.writeNbt(ghostNbt, ghostItems, registryLookup);
        nbt.put("GhostItems", ghostNbt);

        // Modi speichern (als Int Array)
        int[] modesAsInt = new int[5];
        for (int i = 0; i < 5; i++) {
            modesAsInt[i] = filterModes[i].ordinal();
        }
        nbt.putIntArray("FilterModes", modesAsInt);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);

        // Ghost Items laden
        if (nbt.contains("GhostItems")) {
            Inventories.readNbt(nbt.getCompound("GhostItems"), ghostItems, registryLookup);
        }

        // Modi laden
        if (nbt.contains("FilterModes")) {
            int[] modesAsInt = nbt.getIntArray("FilterModes");
            for (int i = 0; i < 5; i++) {
                if (i < modesAsInt.length) {
                    // Safety check für Enum bounds
                    int ordinal = modesAsInt[i];
                    if (ordinal >= 0 && ordinal < HopperFilterMode.values().length) {
                        filterModes[i] = HopperFilterMode.values()[ordinal];
                    }
                }
            }
        }
    }
}
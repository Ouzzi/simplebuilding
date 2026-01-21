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

// WICHTIG: Erbt nun von ModHopperBlockEntity, um Typ-Konflikte im ScreenHandler zu vermeiden
public class NetheriteHopperBlockEntity extends ModHopperBlockEntity {

    private final DefaultedList<ItemStack> ghostItems = DefaultedList.ofSize(5, ItemStack.EMPTY);
    // ÄNDERUNG: Nur noch ein globaler Filter-Modus statt Array
    private HopperFilterMode currentFilterMode = HopperFilterMode.NONE;

    public NetheriteHopperBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public BlockEntityType<?> getType() {
        return ModBlockEntities.MOD_HOPPER_BE;
    }

    // --- FILTER LOGIK ---

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        // Wenn Filter deaktiviert ist (NONE), verhält er sich wie ein normaler Hopper
        if (currentFilterMode == HopperFilterMode.NONE) {
            return true;
        }

        if (slot >= 0 && slot < 5) {
            ItemStack ghost = ghostItems.get(slot);

            // Wenn ein Filter aktiv ist, aber KEIN Ghost Item definiert wurde:
            // Standardverhalten: Nichts darf rein (da nichts matcht).
            if (ghost.isEmpty()) {
                return false;
            }

            if (currentFilterMode == HopperFilterMode.WHITELIST) {
                // Exakter Match (Item + Komponenten/NBT)
                return ItemStack.areItemsAndComponentsEqual(ghost, stack);
            } else if (currentFilterMode == HopperFilterMode.TYPE) {
                // Nur der Item-Typ (z.B. Eisenbarren = Eisenbarren, egal welcher Name)
                return ghost.getItem() == stack.getItem();
            }
        }
        return true;
    }

    // --- INTERAKTION ---

    public void toggleFilterMode() {
        // Zyklisch durchschalten: NONE -> WHITELIST -> TYPE -> NONE ...
        this.currentFilterMode = this.currentFilterMode.next();
        markDirty();
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

    // Helper für GUI und Logik (Global)
    public HopperFilterMode getFilterMode() {
        return this.currentFilterMode;
    }

    // --- NBT (1.21 API) ---

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);

        // Speichere den globalen Modus als Integer
        view.putInt("FilterMode", currentFilterMode.ordinal());

        // Ghost Items speichern
        WriteView ghostView = view.get("GhostItems");
        if (ghostView != null) {
            Inventories.writeData(ghostView, ghostItems);
        } else {
            view.put("GhostItems", ItemStack.CODEC.listOf(), ghostItems);
        }
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);

        // Ghost Items laden
        view.getOptionalReadView("GhostItems").ifPresent(ghostView -> {
            try {
                Inventories.readData(ghostView, ghostItems);
            } catch (Exception e) {
                ghostItems.clear();
            }
        });

        // Fallback Codec
        if (ghostItems.stream().allMatch(ItemStack::isEmpty)) {
            view.read("GhostItems", ItemStack.CODEC.listOf()).ifPresent(list -> {
                for (int i = 0; i < list.size() && i < ghostItems.size(); i++) {
                    ghostItems.set(i, list.get(i));
                }
            });
        }

        // Globalen Modus laden
        view.getOptionalInt("FilterMode").ifPresent(ordinal -> {
            if (ordinal >= 0 && ordinal < HopperFilterMode.values().length) {
                currentFilterMode = HopperFilterMode.values()[ordinal];
            } else {
                currentFilterMode = HopperFilterMode.NONE;
            }
        });
    }
}
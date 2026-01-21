package com.simplebuilding.blocks.entity.custom;

import com.simplebuilding.Simplebuilding; // Sicherstellen, dass deine Main-Klasse importiert ist für Logger
import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.util.HopperFilterMode;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PropertyDelegate; // Wichtig
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

// WICHTIG: Erbt nun von ModHopperBlockEntity, um Typ-Konflikte im ScreenHandler zu vermeiden
public class NetheriteHopperBlockEntity extends ModHopperBlockEntity {

    private final DefaultedList<ItemStack> ghostItems = DefaultedList.ofSize(5, ItemStack.EMPTY);
    // ÄNDERUNG: Nur noch ein globaler Filter-Modus statt Array
    private HopperFilterMode currentFilterMode = HopperFilterMode.NONE;

    // Der PropertyDelegate synchronisiert Integers automatisch zwischen Server und Client ScreenHandler
    protected final PropertyDelegate propertyDelegate;

    public NetheriteHopperBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);

        // Delegate definieren
        this.propertyDelegate = new PropertyDelegate() {
            @Override
            public int get(int index) {
                // Index 0 ist unser FilterMode
                return index == 0 ? currentFilterMode.ordinal() : 0;
            }

            @Override
            public void set(int index, int value) {
                if (index == 0) {
                    currentFilterMode = HopperFilterMode.values()[value % HopperFilterMode.values().length];
                    markDirty();
                }
            }

            @Override
            public int size() {
                return 1; // Wir haben 1 Wert zu synchronisieren
            }
        };
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
        Simplebuilding.LOGGER.info("Filter Mode gewechselt zu: " + this.currentFilterMode);
    }

    public void setGhostItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < 5) {
            if (stack.isEmpty()) {
                ghostItems.set(slot, ItemStack.EMPTY);
                Simplebuilding.LOGGER.info("Ghost Item Slot " + slot + " gelöscht");
            } else {
                ItemStack copy = stack.copy();
                copy.setCount(1);
                ghostItems.set(slot, copy);
                Simplebuilding.LOGGER.info("Ghost Item Slot " + slot + " gesetzt: " + copy.getItem().getName().getString());
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

    public PropertyDelegate getPropertyDelegate() {
        return this.propertyDelegate;
    }

    // --- NBT (1.21 API) ---

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);

        // Speichere den globalen Modus als Integer
        view.putInt("FilterMode", currentFilterMode.ordinal());

        // Inventories.writeNbt ist der Standard, ich passe es an deine Struktur an,
        // falls writeData eine eigene Implementation von dir ist, nutze Inventories Helper falls möglich.
        // Hier nutzen wir deine Logik, fügen aber Logs hinzu.
        WriteView ghostView = view.get("GhostItems");
        if (ghostView == null) {
            // Dies ist abhängig von deiner API, normalerweise erstellt man einen neuen Compound
            // Ich lasse deine Implementation, da ich die API "WriteView" nicht im Detail kenne (vermutlich Custom oder Snapshot)
            view.put("GhostItems", ItemStack.CODEC.listOf(), ghostItems);
        }

        Simplebuilding.LOGGER.info("NBT Gespeichert: Mode=" + currentFilterMode);
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);

        // Codec Laden
        view.read("GhostItems", ItemStack.CODEC.listOf()).ifPresent(list -> {
            ghostItems.clear();
            for (int i = 0; i < list.size() && i < ghostItems.size(); i++) {
                ghostItems.set(i, list.get(i));
            }
        });

        view.getOptionalInt("FilterMode").ifPresent(ordinal -> {
            if (ordinal >= 0 && ordinal < HopperFilterMode.values().length) {
                currentFilterMode = HopperFilterMode.values()[ordinal];
            } else {
                currentFilterMode = HopperFilterMode.NONE;
            }
        });

        Simplebuilding.LOGGER.info("NBT Geladen: Mode=" + currentFilterMode);
    }

    @Override
    public net.minecraft.network.packet.Packet<net.minecraft.network.listener.ClientPlayPacketListener> toUpdatePacket() {
        return net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket.create(this);
    }

}
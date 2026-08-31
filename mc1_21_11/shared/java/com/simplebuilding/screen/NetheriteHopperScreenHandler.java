package com.simplebuilding.screen;

import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.util.HopperFilterMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;

public class NetheriteHopperScreenHandler extends ModHopperScreenHandler {

    private final ContainerData propertyDelegate;
    private final Level world;
    private final BlockPos pos;

    // ÄNDERUNG 1: Client Constructor akzeptiert jetzt BlockPos direkt
    public NetheriteHopperScreenHandler(int syncId, Inventory playerInventory, BlockPos pos) {
        this(syncId, playerInventory, new SimpleContainer(5), null, new SimpleContainerData(1), pos);
    }

    // 2. Server Constructor
    // Dieser wird aufgerufen, wenn der Server das GUI öffnet.
    public NetheriteHopperScreenHandler(int syncId, Inventory playerInventory, Container inventory, ModHopperBlockEntity blockEntity) {
        this(syncId, playerInventory, inventory, blockEntity,
             blockEntity.getPropertyDelegate(),
             blockEntity.getBlockPos());
    }

    // 3. Interner Constructor
    protected NetheriteHopperScreenHandler(int syncId, Inventory playerInventory, Container inventory, ModHopperBlockEntity blockEntity, ContainerData propertyDelegate, BlockPos pos) {
        super(syncId, playerInventory, inventory, blockEntity);
        this.propertyDelegate = propertyDelegate;
        this.world = playerInventory.player.level();
        this.pos = pos;

        this.addDataSlots(propertyDelegate);
    }

    @Override
    public MenuType<?> getType() {
        return ModScreenHandlers.NETHERITE_HOPPER_SCREEN_HANDLER;
    }

    // ÄNDERUNG 2: Rückgabetyp korrigiert (ModHopperBlockEntity statt BlockEntity)
    // Damit passt es zur Override-Regel der Elternklasse.
    @Override
    public ModHopperBlockEntity getBlockEntity() {
        if (this.world != null && this.pos != null) {
            // Wir casten sicherheitshalber, falls an der Pos was anderes ist (sollte nicht passieren)
            if (this.world.getBlockEntity(this.pos) instanceof ModHopperBlockEntity be) {
                return be;
            }
        }
        return super.getBlockEntity();
    }

    public HopperFilterMode getSyncedFilterMode() {
        int ordinal = this.propertyDelegate.get(0);
        if (ordinal >= 0 && ordinal < HopperFilterMode.values().length) {
            return HopperFilterMode.values()[ordinal];
        }
        return HopperFilterMode.NONE;
    }
}
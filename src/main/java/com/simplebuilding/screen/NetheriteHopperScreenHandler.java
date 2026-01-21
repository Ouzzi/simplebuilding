package com.simplebuilding.screen;

import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.util.HopperFilterMode;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class NetheriteHopperScreenHandler extends ModHopperScreenHandler {

    private final PropertyDelegate propertyDelegate;
    private final World world;
    private final BlockPos pos;

    // ÄNDERUNG 1: Client Constructor akzeptiert jetzt BlockPos direkt
    public NetheriteHopperScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos pos) {
        this(syncId, playerInventory, new SimpleInventory(5), null, new ArrayPropertyDelegate(1), pos);
    }

    // 2. Server Constructor
    // Dieser wird aufgerufen, wenn der Server das GUI öffnet.
    public NetheriteHopperScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, ModHopperBlockEntity blockEntity) {
        this(syncId, playerInventory, inventory, blockEntity,
             blockEntity.getPropertyDelegate(),
             blockEntity.getPos());
    }

    // 3. Interner Constructor
    protected NetheriteHopperScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, ModHopperBlockEntity blockEntity, PropertyDelegate propertyDelegate, BlockPos pos) {
        super(syncId, playerInventory, inventory, blockEntity);
        this.propertyDelegate = propertyDelegate;
        this.world = playerInventory.player.getEntityWorld();
        this.pos = pos;

        this.addProperties(propertyDelegate);
    }

    @Override
    public ScreenHandlerType<?> getType() {
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
package com.simplebuilding.entity.vehicle;

import com.simplebuilding.blocks.custom.ChestTier;
import com.simplebuilding.screen.NetheriteHopperScreenHandler;
import com.simplebuilding.util.FilterHopper;
import com.simplebuilding.util.HopperFilterMode;
import com.simplebuilding.util.ItemFilter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecartContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Hopper cart of a mod hopper tier (Queue N23). Vanilla's {@code MinecartHopper} takes in one item per tick; this
 * cart takes up to {@link VehicleTiers#hopperItemsPerTick} (2/4/8 - the mod hopper's speed over vanilla's hopper).
 * Filter key and filter principle like the mod hopper block (docs/ai/PRINZIPIEN-FILTER.md): with a filter on, a slot
 * takes only what matches the real item lying in it, and one item always stays - hoppers below pull only the second
 * and every further one. Same menu and screen as the block.
 */
public class TieredHopperMinecart extends AbstractMinecartContainer implements Hopper, FilterHopper {
    private final ChestTier tier;
    private boolean enabled = true;
    private int takenThisTick;
    private HopperFilterMode filterMode = HopperFilterMode.NONE;
    private final ContainerData propertyDelegate = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? filterMode.ordinal() : 0;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                filterMode = HopperFilterMode.values()[Math.floorMod(value, HopperFilterMode.values().length)];
            }
        }

        @Override
        public int getCount() {
            return 1;
        }
    };

    public TieredHopperMinecart(EntityType<? extends TieredHopperMinecart> type, Level level, ChestTier tier) {
        super(type, level);
        this.tier = tier;
        this.clearItemStacks();
    }

    public ChestTier tier() {
        return this.tier;
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return VehicleTiers.hopper(this.tier).defaultBlockState();
    }

    @Override
    public int getDefaultDisplayOffset() {
        return 1;
    }

    @Override
    public int getContainerSize() {
        return 5;
    }

    @Override
    public void activateMinecart(ServerLevel level, int xt, int yt, int zt, boolean state) {
        this.enabled = !state;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public double getLevelX() {
        return this.getX();
    }

    @Override
    public double getLevelY() {
        return this.getY() + 0.5;
    }

    @Override
    public double getLevelZ() {
        return this.getZ();
    }

    @Override
    public boolean isGridAligned() {
        return false;
    }

    // --- taking in ---

    @Override
    public void tick() {
        this.takenThisTick = 0;
        super.tick();
        this.tryConsumeItems();
    }

    @Override
    protected double makeStepAlongTrack(BlockPos pos, RailShape shape, double movementLeft) {
        double left = super.makeStepAlongTrack(pos, shape, movementLeft);
        this.tryConsumeItems();
        return left;
    }

    /** Up to the tier's items per tick, spread over the rail steps of the tick like vanilla's one item. */
    private void tryConsumeItems() {
        if (this.level().isClientSide() || !this.isAlive() || !this.enabled) {
            return;
        }
        int limit = VehicleTiers.hopperItemsPerTick(this.tier);
        while (this.takenThisTick < limit && this.suckInItems()) {
            this.takenThisTick++;
        }
    }

    /** One item from the container above, or a lying stack (as much as fits), like vanilla's hopper cart. */
    public boolean suckInItems() {
        if (HopperBlockEntity.suckInItems(this.level(), this)) {
            return true;
        }
        for (ItemEntity entity : this.level().getEntitiesOfClass(ItemEntity.class,
                this.getBoundingBox().inflate(0.25, 0.0, 0.25), EntitySelector.ENTITY_STILL_ALIVE)) {
            if (HopperBlockEntity.addItem(this, entity)) {
                return true;
            }
        }
        return false;
    }

    // --- filter (principle: the real item in a slot is its filter) ---

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= 0 && slot < 5 && ItemFilter.accepts(this.filterMode, getItem(slot), stack);
    }

    @Override
    public boolean canTakeItem(Container target, int slot, ItemStack stack) {
        return ItemFilter.movable(this.filterMode, getItem(slot)) > 0;
    }

    @Override
    public boolean mayPlayerPlace(int slot, ItemStack stack) {
        ItemStack held = getItem(slot);
        return this.filterMode == HopperFilterMode.NONE || held.isEmpty() || ItemFilter.matches(this.filterMode, held, stack);
    }

    @Override
    public void toggleFilterMode() {
        this.filterMode = this.filterMode.next();
    }

    @Override
    public HopperFilterMode getFilterMode() {
        return this.filterMode;
    }

    @Override
    public ContainerData getPropertyDelegate() {
        return this.propertyDelegate;
    }

    // --- item, menu, saving ---

    @Override
    protected Item getDropItem() {
        return VehicleTiers.hopperMinecart(this.tier);
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(getDropItem());
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return NetheriteHopperScreenHandler.forCart(containerId, inventory, this);
    }

    /** The mod hopper menu needs its opening data on every loader (vanilla's {@code openMenu(this)} sends none). */
    @Override
    public InteractionResult interactWithContainerVehicle(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            com.simplebuilding.platform.HopperMenus.openMenu(serverPlayer, this, this.blockPosition());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Enabled", this.enabled);
        output.putInt("FilterMode", this.filterMode.ordinal());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.enabled = input.getBooleanOr("Enabled", true);
        int mode = input.getIntOr("FilterMode", 0);
        this.filterMode = mode >= 0 && mode < HopperFilterMode.values().length ? HopperFilterMode.values()[mode] : HopperFilterMode.NONE;
    }
}

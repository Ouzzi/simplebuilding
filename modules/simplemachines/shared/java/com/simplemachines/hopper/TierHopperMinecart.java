package com.simplemachines.hopper;

import com.simplemachines.config.MachinesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
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
 * Hopper cart of a mod hopper tier (SimpleBuilding's {@code TieredHopperMinecart}, Queue N23, stage 3; saved data unchanged:
 * {@code Enabled}, {@code FilterMode}). Vanilla's {@code MinecartHopper} takes in one item per tick; this cart takes up to the
 * tier's hopper speed (2/4/8, {@code hoppers.*}). Filter key and filter principle like the hopper block; same menu.
 */
public class TierHopperMinecart extends AbstractMinecartContainer implements Hopper, FilterHopper {
    private final HopperTier tier;
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
            if (index == 0) filterMode = HopperFilterMode.values()[Math.floorMod(value, HopperFilterMode.values().length)];
        }

        @Override
        public int getCount() {
            return 1;
        }
    };

    public TierHopperMinecart(EntityType<? extends TierHopperMinecart> type, Level level, HopperTier tier) {
        super(type, level);
        this.tier = tier;
        this.clearItemStacks();
    }

    public HopperTier tier() {
        return this.tier;
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return Hoppers.block(this.tier).defaultBlockState();
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

    /** Items a cart of {@code tier} may take in per tick: the speed of the hopper of the tier (2/4/8; Vanilla's cart 1). */
    public static int itemsPerTick(HopperTier tier) {
        return MachinesConfig.hopperSpeed(tier);
    }

    /** Up to the tier's items per tick, spread over the rail steps of the tick like Vanilla's one item. */
    private void tryConsumeItems() {
        if (this.level().isClientSide() || !this.isAlive() || !this.enabled) return;
        int limit = itemsPerTick(this.tier);
        while (this.takenThisTick < limit && this.suckInItems()) {
            this.takenThisTick++;
        }
    }

    /** One item from the container above, or a lying stack (as much as fits), like Vanilla's hopper cart. */
    public boolean suckInItems() {
        if (HopperBlockEntity.suckInItems(this.level(), this)) return true;
        for (ItemEntity entity : this.level().getEntitiesOfClass(ItemEntity.class,
                this.getBoundingBox().inflate(0.25, 0.0, 0.25), EntitySelector.ENTITY_STILL_ALIVE)) {
            if (HopperBlockEntity.addItem(this, entity)) return true;
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
        return Hoppers.cartItem(this.tier);
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(getDropItem());
    }

    /** Vanilla's {@code interactWithContainerVehicle} opens this menu (the tier is the menu type, no opening data). */
    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new TierHopperMenu(this.tier, containerId, inventory, this, this);
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
        this.filterMode = HopperFilterMode.of(input.getIntOr("FilterMode", 0));
    }
}

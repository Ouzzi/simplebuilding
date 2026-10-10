package com.simplebuilding.entity.vehicle;

import com.simplebuilding.blocks.custom.ChestTier;
import com.simplebuilding.items.custom.BackpackItem;
import com.simplebuilding.platform.TieredChestMenus;
import com.simplebuilding.screen.TieredChestMenu;
import com.simplebuilding.screen.TieredChestOpenData;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractChestBoat;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Chest boat of a chest tier (Queue N19): vanilla's chest boat with the slots and stack size of the tier's chest
 * (36/45/54, x1/x2/x4) and its menu. One entity type per tier; the wood ({@link BoatWoods}) is synced data, comes
 * from the item and goes back onto the dropped item. Bamboo rides like vanilla's chest raft.
 */
public class TieredChestBoat extends AbstractChestBoat implements TieredStorageVehicle {
    private static final EntityDataAccessor<String> DATA_WOOD = SynchedEntityData.defineId(TieredChestBoat.class, EntityDataSerializers.STRING);
    private final ChestTier tier;
    /** While saving: the list with stacks above 99 cut to 99 (vanilla's item codec stores no more). */
    private @Nullable NonNullList<ItemStack> saving;

    public TieredChestBoat(EntityType<? extends TieredChestBoat> type, Level level, ChestTier tier) {
        super(type, level, () -> VehicleTiers.chestBoat(tier));
        this.tier = tier;
        this.clearItemStacks();
    }

    @Override
    public ChestTier tier() {
        return this.tier;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_WOOD, BoatWoods.DEFAULT);
    }

    public String wood() {
        return BoatWoods.normalize(this.entityData.get(DATA_WOOD));
    }

    public void setWood(String wood) {
        this.entityData.set(DATA_WOOD, BoatWoods.normalize(wood));
    }

    @Override
    protected double rideHeight(EntityDimensions dimensions) {
        return BoatWoods.isRaft(wood()) ? dimensions.height() * 0.8888889F : dimensions.height() / 3.0F;
    }

    @Override
    public int getContainerSize() {
        return this.tier.slots();
    }

    @Override
    public int getMaxStackSize() {
        return 99 * this.tier.stackMultiplier();
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return BackpackItem.maxStackSizeIn(stack, this.tier.stackMultiplier());
    }

    // --- menu ---

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        if (this.getContainerLootTable() != null && player.isSpectator()) {
            return null;
        }
        this.unpackLootTable(inventory.player);
        return TieredChestMenu.server(containerId, inventory, this, this.tier, false);
    }

    /** The tier menu needs its opening data on every loader (vanilla's {@code openMenu(this)} sends none). */
    @Override
    public InteractionResult interactWithContainerVehicle(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            TieredChestMenus.open(serverPlayer, this, TieredChestOpenData.of(this.tier, false));
        }
        return InteractionResult.SUCCESS;
    }

    /** The inventory key while riding. */
    @Override
    public void openCustomInventoryScreen(Player player) {
        this.interactWithContainerVehicle(player);
        if (player.level() instanceof ServerLevel level) {
            this.gameEvent(GameEvent.CONTAINER_OPEN, player);
            PiglinAi.angerNearbyPiglins(level, player, true);
        }
    }

    // --- drop: the tier boat of this wood ---

    @Override
    public void destroy(ServerLevel level, Item dropItem) {
        this.kill(level);
        if (level.getGameRules().get(GameRules.ENTITY_DROPS)) {
            ItemStack stack = BoatWoods.stack(dropItem, wood());
            stack.set(DataComponents.CUSTOM_NAME, this.getCustomName());
            this.spawnAtLocation(level, stack);
        }
    }

    // --- saving (wood, stacks above 99) ---

    @Override
    public NonNullList<ItemStack> getItemStacks() {
        return this.saving != null ? this.saving : super.getItemStacks();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        NonNullList<ItemStack> real = super.getItemStacks();
        this.saving = OversizedStacks.codecSafeCopy(real);
        try {
            super.addAdditionalSaveData(output);
        } finally {
            this.saving = null;
        }
        OversizedStacks.write(output, real);
        output.putString("Wood", wood());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        OversizedStacks.read(input, super.getItemStacks());
        setWood(input.getStringOr("Wood", BoatWoods.DEFAULT));
    }
}

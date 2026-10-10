package com.simplebuilding.entity.vehicle;

import com.simplebuilding.blocks.custom.ChestTier;
import com.simplebuilding.items.custom.BackpackItem;
import com.simplebuilding.platform.TieredChestMenus;
import com.simplebuilding.screen.TieredChestMenu;
import com.simplebuilding.screen.TieredChestOpenData;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecartContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Chest cart of a chest tier (Queue N19): vanilla's {@code MinecartChest} with the slots and stack size of the
 * tier's chest (36/45/54, x1/x2/x4) and the tier chest's menu. Stacks above 99 are saved like the chest saves them.
 */
public class TieredChestMinecart extends AbstractMinecartContainer implements TieredStorageVehicle {
    private final ChestTier tier;
    /** While saving: the list with stacks above 99 cut to 99 (vanilla's item codec stores no more). */
    private @Nullable NonNullList<ItemStack> saving;

    public TieredChestMinecart(EntityType<? extends TieredChestMinecart> type, Level level, ChestTier tier) {
        super(type, level);
        this.tier = tier;
        this.clearItemStacks();
    }

    @Override
    public ChestTier tier() {
        return this.tier;
    }

    @Override
    protected Item getDropItem() {
        return VehicleTiers.chestMinecart(this.tier);
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(getDropItem());
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

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return VehicleTiers.chest(this.tier).defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH);
    }

    @Override
    public int getDefaultDisplayOffset() {
        return 8;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
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

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        InteractionResult result = super.interact(player, hand, location);
        if (result.consumesAction() && player.level() instanceof ServerLevel serverLevel) {
            this.gameEvent(GameEvent.CONTAINER_OPEN, player);
            PiglinAi.angerNearbyPiglins(serverLevel, player, true);
        }
        return result;
    }

    @Override
    public void stopOpen(ContainerUser containerUser) {
        this.level().gameEvent(GameEvent.CONTAINER_CLOSE, this.position(), GameEvent.Context.of(containerUser.getLivingEntity()));
    }

    // --- stacks above 99 ---

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
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        OversizedStacks.read(input, super.getItemStacks());
    }
}

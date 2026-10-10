package com.simplebuilding.entity.vehicle;

import com.simplebuilding.blocks.custom.ChestTier;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Furnace cart of a furnace tier (Queue N19). Vanilla's {@code MinecartFurnace}, with its own fields because vanilla
 * keeps the fuel private: a fuel item burns {@link VehicleTiers#fuelFactor} times as long (the speed of the furnace of
 * the tier), the cart holds that many times vanilla's 32000 ticks, and it runs at {@link VehicleTiers#furnaceSpeed}
 * of a plain cart's top speed instead of half of it.
 */
public class TieredFurnaceMinecart extends AbstractMinecart {
    private static final EntityDataAccessor<Boolean> DATA_ID_FUEL = SynchedEntityData.defineId(TieredFurnaceMinecart.class, EntityDataSerializers.BOOLEAN);
    private final ChestTier tier;
    private int fuel;
    public Vec3 push = Vec3.ZERO;

    public TieredFurnaceMinecart(EntityType<? extends TieredFurnaceMinecart> type, Level level, ChestTier tier) {
        super(type, level);
        this.tier = tier;
    }

    public ChestTier tier() {
        return this.tier;
    }

    /** Ticks of fuel left (server). */
    public int fuel() {
        return this.fuel;
    }

    @Override
    public boolean isFurnace() {
        return true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_ID_FUEL, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            if (this.fuel > 0) {
                this.fuel--;
            }
            if (this.fuel <= 0) {
                this.push = Vec3.ZERO;
            }
            this.setHasFuel(this.fuel > 0);
        }
        if (this.hasFuel() && this.random.nextInt(4) == 0) {
            this.level().addParticle(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 0.8, this.getZ(), 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected double getMaxSpeed(ServerLevel level) {
        return this.isInWater() ? super.getMaxSpeed(level) * 0.75 : super.getMaxSpeed(level) * VehicleTiers.furnaceSpeed(this.tier);
    }

    @Override
    protected Item getDropItem() {
        return VehicleTiers.furnaceMinecart(this.tier);
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(getDropItem());
    }

    @Override
    protected Vec3 applyNaturalSlowdown(Vec3 deltaMovement) {
        Vec3 slowed;
        if (this.push.lengthSqr() > 1.0E-7) {
            this.push = this.push.horizontalDistanceSqr() > 1.0E-4 && deltaMovement.horizontalDistanceSqr() > 0.001
                    ? this.push.projectedOn(deltaMovement).normalize().scale(this.push.length())
                    : this.push;
            slowed = deltaMovement.multiply(0.8, 0.0, 0.8).add(this.push);
            if (this.isInWater()) {
                slowed = slowed.scale(0.1);
            }
        } else {
            slowed = deltaMovement.multiply(0.98, 0.0, 0.98);
        }
        return super.applyNaturalSlowdown(slowed);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        InteractionResult result = super.interact(player, hand, location);
        if (result.consumesAction()) {
            return result;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (this.addFuel(player.position(), stack)) {
            stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }

    /** One fuel item from {@code interactingPos}: pushes away from there, like vanilla's cart. */
    public boolean addFuel(Vec3 interactingPos, ItemStack stack) {
        int factor = VehicleTiers.fuelFactor(this.tier);
        int perItem = VehicleTiers.VANILLA_FUEL_PER_ITEM * factor;
        if (stack.is(ItemTags.FURNACE_MINECART_FUEL) && this.fuel + perItem <= VehicleTiers.VANILLA_MAX_FUEL * factor) {
            this.fuel += perItem;
            this.push = this.position().subtract(interactingPos).horizontal();
            return true;
        }
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putDouble("PushX", this.push.x);
        output.putDouble("PushZ", this.push.z);
        output.putInt("Fuel", this.fuel);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.push = new Vec3(input.getDoubleOr("PushX", 0.0), 0.0, input.getDoubleOr("PushZ", 0.0));
        this.fuel = input.getIntOr("Fuel", 0);
    }

    protected boolean hasFuel() {
        return this.entityData.get(DATA_ID_FUEL);
    }

    protected void setHasFuel(boolean fuel) {
        this.entityData.set(DATA_ID_FUEL, fuel);
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return VehicleTiers.furnace(this.tier).defaultBlockState()
                .setValue(AbstractFurnaceBlock.FACING, Direction.NORTH).setValue(AbstractFurnaceBlock.LIT, this.hasFuel());
    }
}

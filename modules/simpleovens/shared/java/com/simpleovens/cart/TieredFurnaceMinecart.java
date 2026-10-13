package com.simpleovens.cart;

import com.simpleovens.OvenTier;
import com.simpleovens.OvensRegistry;
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
 * Furnace cart of a furnace tier (moved from SimpleBuilding, Queue N19). Vanilla's {@code MinecartFurnace}, with its own
 * fields because Vanilla keeps the fuel private: a fuel item burns {@link #fuelFactor} times as long (the speed of the
 * furnace of the tier), the cart holds that many times Vanilla's 32000 ticks, and it runs at {@link #topSpeedShare}
 * of a plain cart's top speed instead of half of it. Saved keys as before ({@code Fuel}, {@code PushX/Z}).
 */
public class TieredFurnaceMinecart extends AbstractMinecart {
    /** Vanilla furnace cart: ticks of fuel per fuel item and the most it holds. */
    public static final int VANILLA_FUEL_PER_ITEM = 3600;
    public static final int VANILLA_MAX_FUEL = 32000;
    private static final EntityDataAccessor<Boolean> DATA_ID_FUEL = SynchedEntityData.defineId(TieredFurnaceMinecart.class, EntityDataSerializers.BOOLEAN);
    private final OvenTier tier;
    private int fuel;
    public Vec3 push = Vec3.ZERO;

    public TieredFurnaceMinecart(EntityType<? extends TieredFurnaceMinecart> type, Level level, OvenTier tier) {
        super(type, level);
        this.tier = tier;
    }

    public OvenTier tier() {
        return this.tier;
    }

    /** How much longer a fuel item burns than in Vanilla's cart: the speed of the furnace of the tier (2/4/8). */
    public static int fuelFactor(OvenTier tier) {
        return tier.speed();
    }

    /**
     * Top speed as a share of a plain cart's (Vanilla's furnace cart: 0.5). Enderite reaches a plain cart's top speed,
     * never more - faster carts leave the rails on curves.
     */
    public static double topSpeedShare(OvenTier tier) {
        return switch (tier) {
            case REINFORCED -> 0.625;
            case NETHERITE -> 0.75;
            case ENDERITE -> 1.0;
        };
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

    /** The top speed now (blocks per tick), for tests and tools. */
    public double topSpeed(ServerLevel level) {
        return getMaxSpeed(level);
    }

    @Override
    protected double getMaxSpeed(ServerLevel level) {
        return this.isInWater() ? super.getMaxSpeed(level) * 0.75 : super.getMaxSpeed(level) * topSpeedShare(this.tier);
    }

    @Override
    protected Item getDropItem() {
        return OvensRegistry.CART_ITEMS.get(this.tier);
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
        int factor = fuelFactor(this.tier);
        int perItem = VANILLA_FUEL_PER_ITEM * factor;
        if (stack.is(ItemTags.FURNACE_MINECART_FUEL) && this.fuel + perItem <= VANILLA_MAX_FUEL * factor) {
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
        return OvensRegistry.FURNACES.get(this.tier).defaultBlockState()
                .setValue(AbstractFurnaceBlock.FACING, Direction.NORTH).setValue(AbstractFurnaceBlock.LIT, this.hasFuel());
    }
}

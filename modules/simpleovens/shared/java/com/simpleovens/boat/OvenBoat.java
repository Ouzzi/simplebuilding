package com.simpleovens.boat;

import com.simpleovens.OvenTier;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Furnace boat (BOAT40): a Vanilla boat or raft with a furnace where the chest boat has its chest. The player steers, the
 * furnace pushes like the furnace minecart: right-click with coal or charcoal adds fuel (3600 ticks each, at most 32000),
 * while fuel lasts and a player sits in it the boat gets a gentle forward push in water. Drops its own item.
 * <p>One boat per furnace tier (b-small04, owner 03.11.): the plain furnace ({@code tier == null}) and the Reinforced, Netherite and
 * Enderite furnace. The upper tiers burn fuel longer (as many times as the furnace of the tier cooks faster, like the tiered furnace
 * minecart: 2/4/8) and push harder and faster (x1.25 / x1.5 / x2, the minecart's top-speed ratio 0.625 / 0.75 / 1.0 against 0.5).
 */
public class OvenBoat extends AbstractBoat {
    /** Same fuel numbers as Vanilla's furnace minecart. */
    public static final int FUEL_PER_ITEM = 3600;
    public static final int MAX_FUEL = 32000;
    /** Push per tick in blocks/tick^2 (a held W adds 0.04) and the speed up to which it pushes (an ice boat reaches ~0.7). */
    public static final double THRUST = 0.02;
    public static final double THRUST_SPEED_CAP = 0.3;
    private static final EntityDataAccessor<Boolean> DATA_LIT = SynchedEntityData.defineId(OvenBoat.class, EntityDataSerializers.BOOLEAN);
    private final boolean raft;
    private final OvenTier tier;
    private int fuel;

    /** {@code tier} null = the plain furnace boat. */
    public OvenBoat(EntityType<? extends OvenBoat> type, Level level, Supplier<Item> dropItem, boolean raft, OvenTier tier) {
        super(type, level, dropItem);
        this.raft = raft;
        this.tier = tier;
    }

    /** The furnace tier of this boat; null for the plain furnace. */
    public OvenTier tier() {
        return this.tier;
    }

    /** How many times longer a fuel item burns and how much the furnace holds: 1 for the plain furnace, else the tier's furnace speed (2/4/8). */
    public static int fuelFactor(OvenTier tier) {
        return tier == null ? 1 : tier.speed();
    }

    /** Push and speed cap against the plain boat: 1, 1.25, 1.5, 2. Enderite then reaches 0.6 blocks/tick, still under an ice boat's ~0.7. */
    public static double powerFactor(OvenTier tier) {
        if (tier == null) return 1.0;
        return switch (tier) {
            case REINFORCED -> 1.25;
            case NETHERITE -> 1.5;
            case ENDERITE -> 2.0;
        };
    }

    public int fuelPerItem() {
        return FUEL_PER_ITEM * fuelFactor(this.tier);
    }

    public int maxFuel() {
        return MAX_FUEL * fuelFactor(this.tier);
    }

    public double speedCap() {
        return THRUST_SPEED_CAP * powerFactor(this.tier);
    }

    public boolean isRaft() {
        return this.raft;
    }

    @Override
    protected double rideHeight(EntityDimensions dimensions) {
        return this.raft ? dimensions.height() * 0.8888889F : dimensions.height() / 3.0F;
    }

    /** One seat, set back like the chest boat's. */
    @Override
    protected float getSinglePassengerXOffset() {
        return 0.15F;
    }

    @Override
    protected int getMaxPassengers() {
        return 1;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_LIT, false);
    }

    /** Ticks of fuel left (server only). */
    public int fuel() {
        return this.fuel;
    }

    /** The furnace is burning (synced to clients: light and smoke). */
    public boolean isLit() {
        return this.entityData.get(DATA_LIT);
    }

    /** The push per tick now: zero without fuel. */
    public double thrust() {
        return isLit() ? THRUST * powerFactor(this.tier) : 0.0;
    }

    /** One fuel item: false when it is no fuel or the boat is full. */
    public boolean addFuel(ItemStack stack) {
        if (!stack.is(ItemTags.FURNACE_MINECART_FUEL) || this.fuel + fuelPerItem() > maxFuel()) return false;
        this.fuel += fuelPerItem();
        this.entityData.set(DATA_LIT, true);
        return true;
    }

    /** One tick of burning; fuel only burns while somebody sits in the boat. */
    public void burn(boolean occupied) {
        if (this.fuel > 0 && occupied) this.fuel--;
        this.entityData.set(DATA_LIT, this.fuel > 0);
    }

    /** Pushes forward along the boat's heading; false when without fuel, not in water or already at the cap. */
    public boolean applyThrust() {
        double push = thrust();
        if (push <= 0.0 || !this.isInWater()) return false;
        Vec3 motion = this.getDeltaMovement();
        if (motion.horizontalDistanceSqr() >= speedCap() * speedCap()) return false;
        float yaw = this.getYRot() * (float) (Math.PI / 180.0);
        this.setDeltaMovement(motion.add(Mth.sin(-yaw) * push, 0.0, Mth.cos(yaw) * push));
        return true;
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide()) {
            burn(this.isVehicle());
        } else if (this.isLit() && this.random.nextInt(3) == 0) {
            float yaw = this.getYRot() * (float) (Math.PI / 180.0);
            // the furnace sits 0.5 blocks behind the middle, where the chest boat has its chest
            this.level().addParticle(ParticleTypes.LARGE_SMOKE, this.getX() + 0.5 * Mth.sin(yaw), this.getY() + 0.95 + (this.raft ? 0.3 : 0.0),
                    this.getZ() - 0.5 * Mth.cos(yaw), 0.0, 0.02, 0.0);
        }
        if (this.isLocalInstanceAuthoritative() && this.getControllingPassenger() instanceof Player) applyThrust();
        super.tick();
    }

    /** Fuel in the hand fills the furnace; otherwise (or when full) the player gets in like in any boat. */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(ItemTags.FURNACE_MINECART_FUEL) && !player.isSecondaryUseActive()) {
            if (this.level().isClientSide()) return InteractionResult.SUCCESS;
            if (addFuel(stack)) {
                stack.consume(1, player);
                return InteractionResult.SUCCESS;
            }
        }
        return super.interact(player, hand, location);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Fuel", this.fuel);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.fuel = Mth.clamp(input.getIntOr("Fuel", 0), 0, maxFuel());
        this.entityData.set(DATA_LIT, this.fuel > 0);
    }
}

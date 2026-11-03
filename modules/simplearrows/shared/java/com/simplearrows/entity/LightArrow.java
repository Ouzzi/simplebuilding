package com.simplearrows.entity;

import com.simplearrows.ArrowsRegistry;
import com.simplearrows.config.ArrowsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The Light Arrow (ARROWS01, owner 03.11.: "despawnen können wie ein normaler pfeil"): stuck in a block it puts Vanilla's invisible
 * light block ({@code minecraft:light}, level 15) into the air in front of the face it hit. The light belongs to the arrow
 * ({@code light_pos} in its save data) and goes with it: Vanilla despawn (1200 ticks in the ground, {@link #tickDespawn}), picking
 * it up, {@code /kill}, falling out when the block goes. Unloading the chunk keeps both. Several Light Arrows in the same air
 * share the light; it goes when the last one does. Only a block that is still {@code minecraft:light} is removed.
 */
public class LightArrow extends AbstractArrow {
    private @Nullable BlockPos lightPos;

    public LightArrow(EntityType<? extends LightArrow> type, Level level) {
        super(type, level);
    }

    public LightArrow(Level level, LivingEntity owner, ItemStack pickupItemStack, @Nullable ItemStack firedFromWeapon) {
        super(ArrowsRegistry.LIGHT_ARROW_ENTITY, owner, level, pickupItemStack, firedFromWeapon);
    }

    public LightArrow(Level level, double x, double y, double z, ItemStack pickupItemStack, @Nullable ItemStack firedFromWeapon) {
        super(ArrowsRegistry.LIGHT_ARROW_ENTITY, x, y, z, level, pickupItemStack, firedFromWeapon);
    }

    /** The light this arrow keeps burning, or null (flying, hit a mob, no air in front, switched off). */
    public @Nullable BlockPos lightPos() {
        return this.lightPos;
    }

    /** Public so the GameTest can stick the arrow without flying. */
    @Override
    public void onHitBlock(BlockHitResult hitResult) {
        super.onHitBlock(hitResult);
        if (this.level().isClientSide() || !ArrowsConfig.lightArrow) return;
        releaseLight();
        BlockPos at = hitResult.getBlockPos().relative(hitResult.getDirection());
        if (this.level().getBlockState(at).isAir()) {
            this.level().setBlock(at, Blocks.LIGHT.defaultBlockState(), 3);
        }
        if (this.level().getBlockState(at).is(Blocks.LIGHT)) this.lightPos = at.immutable();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            if (this.isInGround() && this.inGroundTime % 10 == 0) {
                this.level().addParticle(ParticleTypes.WAX_OFF, this.getRandomX(0.3), this.getRandomY(), this.getRandomZ(0.3), 0.0, 0.0, 0.0);
            }
        } else if (this.lightPos != null && !this.isInGround()) {
            releaseLight(); // fell out: the block it stuck in is gone
        }
    }

    /** Vanilla's despawn clock (public for the GameTest): 1200 ticks in the ground, then {@link #discard}. */
    @Override
    public void tickDespawn() {
        super.tickDespawn();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason.shouldDestroy() && !this.level().isClientSide()) releaseLight();
        super.remove(reason);
    }

    /** Takes the light away unless another stuck Light Arrow still holds the same spot. */
    private void releaseLight() {
        BlockPos pos = this.lightPos;
        this.lightPos = null;
        if (pos == null) return;
        boolean shared = !this.level().getEntitiesOfClass(LightArrow.class, new AABB(pos).inflate(1.5),
                other -> other != this && !other.isRemoved() && pos.equals(other.lightPos)).isEmpty();
        if (!shared && this.level().getBlockState(pos).is(Blocks.LIGHT)) {
            this.level().removeBlock(pos, false);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.storeNullable("light_pos", BlockPos.CODEC, this.lightPos);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.lightPos = input.read("light_pos", BlockPos.CODEC).orElse(null);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ArrowsRegistry.LIGHT_ARROW);
    }
}

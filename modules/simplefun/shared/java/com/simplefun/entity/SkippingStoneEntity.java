package com.simplefun.entity;

import com.simplefun.registry.ModToys;
import com.simplefun.toys.FunToys;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * FUN11-5 Steinehüpfen: a flat stone that hops over water up to {@link #MAX_SKIPS} times (splash, ring, action bar count)
 * and then sinks. No damage, no block change; a hit on a block or entity just ends the throw.
 */
public class SkippingStoneEntity extends ThrowableItemProjectile {
  public static final int MAX_SKIPS = 5, MAX_TICKS = 200;
  /** Horizontal speed (blocks/tick) below which the stone sinks instead of hopping. */
  public static final double MIN_SKIP_SPEED = 0.3;

  private int skips;

  public SkippingStoneEntity(EntityType<? extends SkippingStoneEntity> type, Level level) {
    super(type, level);
  }

  public SkippingStoneEntity(Level level, LivingEntity owner, ItemStack stack) {
    super(ModToys.SKIPPING_STONE_ENTITY, owner, level, stack);
  }

  @Override
  protected Item getDefaultItem() {
    return ModToys.SKIPPING_STONE;
  }

  public int skips() {
    return skips;
  }

  /**
   * The hop: the next velocity after touching water with {@code v}, or null when the stone sinks (too slow, too steep
   * – more than about 50° down – or already {@link #MAX_SKIPS} hops).
   */
  public static Vec3 bounce(Vec3 v, int skipsSoFar) {
    double horizontal = Math.sqrt(v.x * v.x + v.z * v.z);
    if (skipsSoFar >= MAX_SKIPS || horizontal < MIN_SKIP_SPEED || -v.y > horizontal * 1.2) return null;
    return new Vec3(v.x * 0.8, 0.08 + horizontal * 0.06, v.z * 0.8);
  }

  @Override
  public void tick() {
    super.tick();
    if (!(level() instanceof ServerLevel level) || isRemoved()) return;
    if (tickCount > MAX_TICKS) {
      finish();
      return;
    }
    BlockPos pos = blockPosition();
    if (!level.getFluidState(pos).is(FluidTags.WATER) || getDeltaMovement().y >= 0) return;
    Vec3 next = bounce(getDeltaMovement(), skips);
    if (next == null) {
      level.sendParticles(ParticleTypes.BUBBLE, getX(), getY(), getZ(), 4, 0.1, 0.1, 0.1, 0.0);
      finish();
      return;
    }
    skips++;
    BlockPos top = pos;
    for (int i = 0; i < 4 && level.getFluidState(top.above()).is(FluidTags.WATER); i++) top = top.above();
    double surface = top.getY() + level.getFluidState(top).getHeight(level, top);
    setPos(getX(), surface + 0.05, getZ());
    setDeltaMovement(next);
    for (int i = 0; i < 8; i++) {
      double a = Math.PI * 2 * i / 8;
      level.sendParticles(ParticleTypes.SPLASH, getX() + Math.cos(a) * 0.45, surface + 0.05, getZ() + Math.sin(a) * 0.45, 1, 0, 0, 0, 0);
    }
    level.playSound(null, getX(), surface, getZ(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.35f, 1.4f + skips * 0.08f);
    if (getOwner() instanceof ServerPlayer player)
      player.sendOverlayMessage(Component.translatable("simplefun.toys.skip_stone.count", skips));
  }

  @Override
  protected void onHit(HitResult result) {
    // No damage, no block effect: the throw just ends (the stone is gone).
    if (!level().isClientSide()) finish();
  }

  private void finish() {
    FunToys.recordSkips(getOwner(), skips);
    discard();
  }

  @Override
  protected boolean canHitEntity(Entity entity) {
    return false;
  }
}

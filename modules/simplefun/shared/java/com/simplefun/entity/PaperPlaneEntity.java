package com.simplefun.entity;

import com.simplefun.registry.ModToys;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * FUN11-8 Papierflieger: glides (low gravity, the sink rate is capped), passes through entities without harm and lands
 * as its item (with the folded note) on the first block or water; after {@link #MAX_TICKS} (60 s) it drops where it is.
 * The best distance comes from a throw about 20° upwards.
 */
public class PaperPlaneEntity extends ThrowableItemProjectile {
  public static final int MAX_TICKS = 1200;
  public static final double MAX_SINK = 0.06;

  public PaperPlaneEntity(EntityType<? extends PaperPlaneEntity> type, Level level) {
    super(type, level);
  }

  public PaperPlaneEntity(Level level, LivingEntity owner, ItemStack stack) {
    super(ModToys.PAPER_PLANE_ENTITY, owner, level, stack);
  }

  @Override
  protected Item getDefaultItem() {
    return ModToys.PAPER_PLANE;
  }

  @Override
  protected double getDefaultGravity() {
    return 0.006;
  }

  @Override
  protected float getAirDrag() {
    return 0.985f;
  }

  @Override
  public void tick() {
    super.tick();
    if (isRemoved()) return;
    Vec3 v = getDeltaMovement();
    if (v.y < -MAX_SINK) setDeltaMovement(v.x, -MAX_SINK, v.z);
    if (!level().isClientSide() && (tickCount > MAX_TICKS || isInWater())) land();
  }

  @Override
  protected boolean canHitEntity(Entity entity) {
    return false;
  }

  @Override
  protected void onHit(HitResult result) {
    if (!level().isClientSide()) land();
  }

  /** Drops the plane (with its note) where it is, once. */
  public ItemEntity land() {
    if (isRemoved() || !(level() instanceof ServerLevel level)) return null;
    Vec3 back = position().subtract(getDeltaMovement().normalize().scale(0.2));
    ItemEntity item = new ItemEntity(level, back.x, back.y, back.z, getItem().copy());
    item.setDeltaMovement(Vec3.ZERO);
    item.setDefaultPickUpDelay();
    level.addFreshEntity(item);
    discard();
    return item;
  }
}

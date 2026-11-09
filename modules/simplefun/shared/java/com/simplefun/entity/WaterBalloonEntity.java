package com.simplefun.entity;

import com.simplefun.registry.ModToys;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * FUN11-6 Wasserbombe: hits only players and blocks (never mobs). On impact it douses fire, lit candles and campfires
 * within one block, fills a hit cauldron by one level and makes a hit player wet (fire out, drips). No damage, nothing
 * broken. Where water evaporates (the Nether) it turns into steam without any effect.
 */
public class WaterBalloonEntity extends ThrowableItemProjectile {
  public WaterBalloonEntity(EntityType<? extends WaterBalloonEntity> type, Level level) {
    super(type, level);
  }

  public WaterBalloonEntity(Level level, LivingEntity owner, ItemStack stack) {
    super(ModToys.WATER_BALLOON_ENTITY, owner, level, stack);
  }

  @Override
  protected Item getDefaultItem() {
    return ModToys.WATER_BALLOON;
  }

  @Override
  protected double getDefaultGravity() {
    return 0.05;
  }

  @Override
  protected boolean canHitEntity(Entity entity) {
    return hits(entity) && super.canHitEntity(entity);
  }

  /** Only players are hit; mobs and every other entity are passed through. */
  public static boolean hits(Entity entity) {
    return entity instanceof Player;
  }

  @Override
  protected void onHit(HitResult result) {
    if (!(level() instanceof ServerLevel level)) return;
    BlockPos pos = BlockPos.containing(result.getLocation());
    boolean evaporates = level.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, pos);
    splash(
        level,
        result.getLocation(),
        result instanceof BlockHitResult block ? block : null,
        result instanceof EntityHitResult entity ? entity.getEntity() : null,
        getOwner(),
        evaporates);
    discard();
  }

  /**
   * The splash (also used by the GameTests): returns how many blocks it changed (fire doused, cauldron filled). With
   * {@code evaporates} only steam.
   */
  public static int splash(ServerLevel level, Vec3 at, BlockHitResult block, Entity hit, Entity owner, boolean evaporates) {
    if (evaporates) {
      level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 10, 0.3, 0.2, 0.3, 0.02);
      level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.6f, 1.6f);
      return 0;
    }
    level.sendParticles(ParticleTypes.SPLASH, at.x, at.y, at.z, 24, 0.4, 0.2, 0.4, 0.1);
    level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_SPLASH, SoundSource.NEUTRAL, 0.7f, 1.3f);
    int changed = 0;
    if (hit != null) {
      hit.clearFire();
      level.sendParticles(ParticleTypes.DRIPPING_WATER, hit.getX(), hit.getY() + hit.getBbHeight() * 0.7, hit.getZ(), 12, 0.3, 0.4, 0.3, 0.0);
      level.playSound(null, hit.getX(), hit.getY(), hit.getZ(), SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 0.4f, 1.2f);
    }
    BlockPos center = BlockPos.containing(at);
    if (block != null) {
      BlockPos hitPos = block.getBlockPos();
      BlockState state = level.getBlockState(hitPos);
      if (state.is(Blocks.CAULDRON)) {
        level.setBlockAndUpdate(hitPos, Blocks.WATER_CAULDRON.defaultBlockState());
        level.gameEvent(owner, GameEvent.BLOCK_CHANGE, hitPos);
        changed++;
      } else if (state.is(Blocks.WATER_CAULDRON) && state.getValue(LayeredCauldronBlock.LEVEL) < LayeredCauldronBlock.MAX_FILL_LEVEL) {
        level.setBlockAndUpdate(hitPos, state.setValue(LayeredCauldronBlock.LEVEL, state.getValue(LayeredCauldronBlock.LEVEL) + 1));
        level.gameEvent(owner, GameEvent.BLOCK_CHANGE, hitPos);
        changed++;
      }
      center = hitPos.relative(block.getDirection());
    }
    for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1)))
      if (douse(level, pos.immutable(), owner)) changed++;
    return changed;
  }

  /** Like Vanilla splash water: fire goes out, lit candles and campfires are put out. Nothing else changes. */
  static boolean douse(ServerLevel level, BlockPos pos, Entity owner) {
    BlockState state = level.getBlockState(pos);
    if (state.is(BlockTags.FIRE)) {
      level.removeBlock(pos, false);
      level.levelEvent(null, 1009, pos, 0);
      return true;
    }
    if (AbstractCandleBlock.isLit(state)) {
      AbstractCandleBlock.extinguish(null, state, level, pos);
      return true;
    }
    if (CampfireBlock.isLitCampfire(state)) {
      level.levelEvent(null, 1009, pos, 0);
      CampfireBlock.douse(owner, level, pos, state);
      level.setBlock(pos, state.setValue(CampfireBlock.LIT, false), Block.UPDATE_ALL);
      return true;
    }
    return false;
  }
}

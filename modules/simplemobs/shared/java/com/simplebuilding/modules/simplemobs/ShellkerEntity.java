package com.simplebuilding.modules.simplemobs;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Shellker: a Shulker in a bedrock-hard shell. Stays 1x1x1 and only opens a slit to shoot (the renderer shows the
 * slit and a violet eye). Closed it takes no damage; it never teleports (pistons can still push it). Its bullets fly
 * 1.5x faster and give High Gravity instead of Levitation. Four guard every End gateway ({@link EndNetherServer}).
 */
public class ShellkerEntity extends Shulker {
    /** Raw peek while open: just enough for Vanilla's "open" rules (arrows hurt, ambient sound), no growth or push. */
    public static final byte OPEN_PEEK = 1;

    public ShellkerEntity(EntityType<? extends ShellkerEntity> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Shulker.createAttributes().add(Attributes.MAX_HEALTH, EndNetherLogic.SHELLKER_HEALTH);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F, 0.02F, true));
        this.goalSelector.addGoal(4, new ShellkerAttackGoal());
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, this.getClass()).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true) {
            @Override public boolean canUse() { return mob.level().getDifficulty() != Difficulty.PEACEFUL && super.canUse(); }
        });
    }

    public boolean isShut() {
        return this.entityData.get(DATA_PEEK_ID) == 0;
    }

    /** Opens the slit (true) or closes the shell (false), with the Shulker sounds. */
    public void setOpen(boolean open) {
        if (open == !isShut()) return;
        this.entityData.set(DATA_PEEK_ID, open ? OPEN_PEEK : (byte) 0);
        if (!this.level().isClientSide()) {
            this.playSound(open ? SoundEvents.SHULKER_OPEN : SoundEvents.SHULKER_CLOSE, 1.0F, 0.8F);
            this.gameEvent(open ? GameEvent.CONTAINER_OPEN : GameEvent.CONTAINER_CLOSE);
        }
    }

    /** Sets the face the Shellker clings to (gateway guards sit on the bedrock below). */
    public void attachTo(Direction face) {
        this.entityData.set(DATA_ATTACH_FACE_ID, face);
    }

    @Override
    protected boolean teleportSomewhere() {
        return false;
    }

    /** Never null: without any carrying face it simply stays (no teleport). */
    @Override
    protected @Nullable Direction findAttachableSurface(BlockPos target) {
        return EndNetherLogic.attachFace(this.getAttachFace(), d -> this.level().loadedAndEntityCanStandOnFace(target.relative(d), this, d.getOpposite()));
    }

    @Override
    protected AABB makeBoundingBox(Vec3 position) {
        return getProgressAabb(this.getScale(), this.getAttachFace().getOpposite(), 0.0F, position);
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return this.getType().getDimensions().scale(this.getScale());
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return !EndNetherMobs.isGravity(effect.getEffect()) && super.canBeAffected(effect);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (EndNetherLogic.shellBlocks(isShut(), source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))) {
            this.playSound(SoundEvents.STONE_HIT, 1.0F, 0.8F + this.random.nextFloat() * 0.2F);
            return false;
        }
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && source.getDirectEntity() instanceof ShellkerBullet) cloneFromBullet(level);
        return hurt;
    }

    /** Shulker-style reproduction: an open Shellker hit by a Shellker bullet buds a new one into a free neighbour cell. */
    void cloneFromBullet(ServerLevel level) {
        int near = level.getEntities(EndNetherMobs.SHELLKER, this.getBoundingBox().inflate(8.0), Entity::isAlive).size();
        if (!EndNetherLogic.cloneSucceeds(near, this.random.nextFloat())) return;
        for (Direction d : Direction.values()) {
            BlockPos cell = this.blockPosition().relative(d);
            if (!level.isEmptyBlock(cell) || !level.noCollision(new AABB(cell).deflate(1.0E-6))) continue;
            Direction face = null;
            for (Direction f : Direction.values()) {
                if (level.loadedAndEntityCanStandOnFace(cell.relative(f), this, f.getOpposite())) { face = f; break; }
            }
            if (face == null) continue;
            ShellkerEntity baby = EndNetherMobs.SHELLKER.create(level, EntitySpawnReason.BREEDING);
            if (baby == null) return;
            baby.attachTo(face);
            baby.snapTo(cell.getX() + 0.5, cell.getY(), cell.getZ() + 0.5);
            level.addFreshEntity(baby);
            return;
        }
    }

    /** Vanilla Shulker attack, but only the slit opens and the bullet is a Shellker bullet. */
    private class ShellkerAttackGoal extends Goal {
        private int attackTime;

        ShellkerAttackGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && level().getDifficulty() != Difficulty.PEACEFUL;
        }

        @Override
        public void start() {
            this.attackTime = 20;
            setOpen(true);
        }

        @Override
        public void stop() {
            setOpen(false);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (level().getDifficulty() == Difficulty.PEACEFUL) return;
            this.attackTime--;
            LivingEntity target = getTarget();
            if (target == null) return;
            getLookControl().setLookAt(target, 180.0F, 180.0F);
            if (distanceToSqr(target) < 400.0) {
                if (this.attackTime <= 0) {
                    this.attackTime = 20 + random.nextInt(10) * 20 / 2;
                    level().addFreshEntity(new ShellkerBullet(level(), ShellkerEntity.this, target, getAttachFace().getAxis()));
                    playSound(SoundEvents.SHULKER_SHOOT, 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 0.8F);
                }
            } else {
                setTarget(null);
            }
            super.tick();
        }
    }
}

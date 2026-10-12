package com.simplebuilding.modules.simplemobs;

import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Seelenfeuer-Lohe (Soulfire Blaze): a Blaze of soul fire. Same size, flight, health and weaknesses (water, snowballs);
 * fires three-shot volleys of {@link SoulFireball}s and glows with soul flames while charging. Spawns rarely in Soul
 * Sand Valleys ({@link EndNetherServer}); drops the Soulfire Rod.
 */
public class SoulfireBlaze extends Blaze {
    private static final EntityDataAccessor<Boolean> DATA_SOUL_CHARGED = SynchedEntityData.defineId(SoulfireBlaze.class, EntityDataSerializers.BOOLEAN);

    public SoulfireBlaze(EntityType<? extends SoulfireBlaze> type, Level level) {
        super(type, level);
        this.xpReward = 12;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_SOUL_CHARGED, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(4, new SoulAttackGoal(this));
        this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0, 0.0F));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public boolean isSoulCharged() {
        return this.entityData.get(DATA_SOUL_CHARGED);
    }

    public void setSoulCharged(boolean charged) {
        this.entityData.set(DATA_SOUL_CHARGED, charged);
    }

    @Override
    public void aiStep() {
        if (this.level().isClientSide()) {
            int n = isSoulCharged() ? 3 : 1;
            for (int i = 0; i < n; i++) {
                if (this.random.nextInt(3) == 0 || isSoulCharged()) {
                    this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, this.getRandomX(0.5), this.getRandomY(), this.getRandomZ(0.5), 0.0, 0.01, 0.0);
                }
            }
        }
        super.aiStep();
    }

    /** Vanilla's Blaze attack with soul fireballs: charge 3 s, three shots 0.3 s apart, then 5 s rest. */
    static class SoulAttackGoal extends Goal {
        private final SoulfireBlaze blaze;
        private int attackStep, attackTime, lastSeen;

        SoulAttackGoal(SoulfireBlaze blaze) {
            this.blaze = blaze;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.blaze.getTarget();
            return target != null && target.isAlive() && this.blaze.canAttack(target);
        }

        @Override
        public void start() {
            this.attackStep = 0;
        }

        @Override
        public void stop() {
            this.blaze.setSoulCharged(false);
            this.lastSeen = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            this.attackTime--;
            LivingEntity target = this.blaze.getTarget();
            if (target == null) return;
            boolean sight = this.blaze.getSensing().hasLineOfSight(target);
            this.lastSeen = sight ? 0 : this.lastSeen + 1;
            double distance = this.blaze.distanceToSqr(target);
            double follow = this.blaze.getAttributeValue(Attributes.FOLLOW_RANGE);
            if (distance < 4.0) {
                if (!sight) return;
                if (this.attackTime <= 0) {
                    this.attackTime = 20;
                    this.blaze.doHurtTarget(getServerLevel(this.blaze), target);
                }
                this.blaze.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0);
            } else if (distance < follow * follow && sight) {
                double xd = target.getX() - this.blaze.getX(), yd = target.getY(0.5) - this.blaze.getY(0.5), zd = target.getZ() - this.blaze.getZ();
                if (this.attackTime <= 0) {
                    this.attackStep++;
                    if (this.attackStep == 1) {
                        this.attackTime = 60;
                        this.blaze.setSoulCharged(true);
                    } else if (this.attackStep <= 4) {
                        this.attackTime = 6;
                    } else {
                        this.attackTime = 100;
                        this.attackStep = 0;
                        this.blaze.setSoulCharged(false);
                    }
                    if (this.attackStep > 1) {
                        double spread = Math.sqrt(Math.sqrt(distance)) * 0.5;
                        if (!this.blaze.isSilent()) this.blaze.level().levelEvent(null, 1018, this.blaze.blockPosition(), 0);
                        Vec3 dir = new Vec3(this.blaze.getRandom().triangle(xd, 2.297 * spread), yd, this.blaze.getRandom().triangle(zd, 2.297 * spread));
                        SoulFireball ball = new SoulFireball(this.blaze.level(), this.blaze, dir.normalize());
                        ball.setPos(ball.getX(), this.blaze.getY(0.5) + 0.5, ball.getZ());
                        this.blaze.level().addFreshEntity(ball);
                    }
                }
                this.blaze.getLookControl().setLookAt(target, 10.0F, 10.0F);
            } else if (this.lastSeen < 5) {
                this.blaze.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0);
            }
            super.tick();
        }
    }
}

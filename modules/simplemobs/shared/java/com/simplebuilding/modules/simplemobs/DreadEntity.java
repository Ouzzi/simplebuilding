package com.simplebuilding.modules.simplemobs;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Dread (Schreckensritter), fear mob F-A: a 2.6 block knight of rusted armor with soul flames in its empty helmet,
 * dragging a chain mace. Summoned only ({@link DreadPattern}). Slow, every attack announced: the mace swing (18 damage,
 * a blocked hit disables the shield for 10 s) and the ground stomp (ring of 6 blocks, jumping dodges it). Its Shivering
 * aura and all its damage weaken in bright light (12 or more): half damage, aura at most level I.
 */
public class DreadEntity extends Monster {
    public static final int ACT_NONE = 0, ACT_SWING = 1, ACT_STOMP = 2;
    private static final EntityDataAccessor<Integer> DATA_ACTION = SynchedEntityData.defineId(DreadEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_RISE = SynchedEntityData.defineId(DreadEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_WEAK = SynchedEntityData.defineId(DreadEntity.class, EntityDataSerializers.BOOLEAN);

    private int actionTicks, swingCooldown = 20, stompCooldown = 60;
    /** Client: ticks since the current action began (for the renderer), and the previous value. */
    private int clientActionAge;

    public DreadEntity(EntityType<? extends DreadEntity> type, Level level) {
        super(type, level);
        this.xpReward = FearLogic.DREAD_XP;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, FearLogic.DREAD_HEALTH)
                .add(Attributes.ARMOR, FearLogic.DREAD_ARMOR)
                .add(Attributes.ARMOR_TOUGHNESS, FearLogic.DREAD_TOUGHNESS)
                .add(Attributes.ATTACK_DAMAGE, FearLogic.DREAD_HIT)
                .add(Attributes.MOVEMENT_SPEED, FearLogic.DREAD_SPEED)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ApproachGoal());
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                (e, level) -> FearAura.affects(e)));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ACTION, ACT_NONE);
        builder.define(DATA_RISE, 0);
        builder.define(DATA_WEAK, false);
    }

    // ---- state ------------------------------------------------------------------------------

    public int action() { return this.entityData.get(DATA_ACTION); }
    public int actionTicks() { return actionTicks; }
    public int riseTicks() { return this.entityData.get(DATA_RISE); }
    public boolean rising() { return riseTicks() > 0; }
    /** Standing in bright light (12 or more): half damage, aura at most I. */
    public boolean weakened() { return this.entityData.get(DATA_WEAK); }
    public int clientActionAge() { return clientActionAge; }

    public void startRising() { this.entityData.set(DATA_RISE, FearLogic.RISE_TICKS); }

    /** Screenshot/test hook: start an action wind-up now. */
    public void playAction(int action) {
        this.entityData.set(DATA_ACTION, action);
        actionTicks = action == ACT_SWING ? FearLogic.SWING_WINDUP : action == ACT_STOMP ? FearLogic.STOMP_WINDUP : 0;
    }

    public int light() { return this.level().getMaxLocalRawBrightness(BlockPos.containing(this.getX(), this.getEyeY(), this.getZ())); }

    // ---- rules ------------------------------------------------------------------------------

    @Override public float getSecondsToDisableBlocking() { return FearLogic.SHIELD_DISABLE_SECONDS; }
    @Override public boolean removeWhenFarAway(double distSqr) { return false; }
    @Override protected boolean isImmobile() { return super.isImmobile() || rising(); }
    @Override public boolean isPushable() { return false; }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (rising() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        return super.hurtServer(level, source, damage);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("Rise", riseTicks());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        this.entityData.set(DATA_RISE, in.getIntOr("Rise", 0));
    }

    // ---- sounds (Vanilla) -------------------------------------------------------------------

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.WITHER_SKELETON_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.IRON_GOLEM_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.WITHER_SKELETON_DEATH; }
    @Override public float getVoicePitch() { return 0.45f + this.random.nextFloat() * 0.1f; }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.IRON_GOLEM_STEP, 1.0f, 0.6f);
        this.playSound(SoundEvents.CHAIN_STEP, 0.6f, 0.7f);
    }

    // ---- tick -------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            clientActionAge = action() == ACT_NONE ? 0 : clientActionAge + 1;
            if (!weakened() && this.random.nextInt(3) == 0) {
                this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, this.getX() + (this.random.nextDouble() - 0.5) * 0.3,
                        this.getY() + 2.35, this.getZ() + (this.random.nextDouble() - 0.5) * 0.3, 0, 0.02, 0);
            }
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        int rise = riseTicks();
        if (rise > 0) {
            this.entityData.set(DATA_RISE, rise - 1);
            if (rise % 4 == 0) level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 0.3, this.getZ(), 4, 0.6, 0.2, 0.6, 0.02);
            if (rise == 1) this.playSound(SoundEvents.WITHER_SKELETON_AMBIENT, 2.0f, 0.35f);
        }
        if (this.tickCount % 10 == 0) {
            boolean weak = FearLogic.bright(light());
            this.entityData.set(DATA_WEAK, weak);
            if (weak) level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 2.35, this.getZ(), 2, 0.15, 0.05, 0.15, 0.01);
        }
        if (this.tickCount % FearLogic.AURA_INTERVAL == 0 && !rising()) FearAura.pulse(level, this, weakened());
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (rising()) { this.getNavigation().stop(); return; }
        if (swingCooldown > 0) swingCooldown--;
        if (stompCooldown > 0) stompCooldown--;
        LivingEntity target = this.getTarget();
        if (target != null && !FearAura.affects(target)) { this.setTarget(null); target = null; }
        int act = action();
        if (act != ACT_NONE) {
            this.getNavigation().stop();
            if (target != null) this.getLookControl().setLookAt(target, 30f, 30f);
            if (act == ACT_STOMP && actionTicks % 6 == 0) ring(level, ParticleTypes.SOUL, FearLogic.STOMP_RADIUS, 24);
            if (--actionTicks <= 0) {
                this.entityData.set(DATA_ACTION, ACT_NONE);
                if (act == ACT_SWING) { swingCooldown = FearLogic.SWING_COOLDOWN; if (target != null) swing(level, target); }
                else stomp(level);
            }
            return;
        }
        if (target == null) return;
        double dist = this.distanceTo(target);
        if (stompCooldown == 0 && dist < FearLogic.STOMP_RADIUS - 1.0 && this.onGround()) {
            stompCooldown = FearLogic.STOMP_COOLDOWN;
            playAction(ACT_STOMP);
            this.playSound(SoundEvents.RAVAGER_ROAR, 1.5f, 0.5f);
            this.playSound(SoundEvents.CHAIN_BREAK, 1.5f, 0.5f);
            ring(level, ParticleTypes.SOUL, FearLogic.STOMP_RADIUS, 32);
        } else if (swingCooldown == 0 && dist <= reach(target)) {
            playAction(ACT_SWING);
            this.playSound(SoundEvents.CHAIN_PLACE, 1.5f, 0.5f);
            this.playSound(SoundEvents.ANVIL_PLACE, 0.4f, 0.5f);
        }
    }

    private double reach(LivingEntity target) {
        return this.getBbWidth() / 2 + target.getBbWidth() / 2 + FearLogic.SWING_REACH;
    }

    /** The mace lands: 18 damage (halved in light) if still in reach; a shield blocking it is disabled for 10 s. */
    public boolean swing(ServerLevel level, LivingEntity target) {
        this.playSound(SoundEvents.ANVIL_LAND, 0.8f, 0.6f);
        if (this.distanceTo(target) > reach(target) + 0.5) return false;
        boolean hit = target.hurtServer(level, this.damageSources().mobAttack(this), FearLogic.dreadDamage(FearLogic.DREAD_HIT, light()));
        Vec3 away = target.position().subtract(this.position()).multiply(1, 0, 1);
        if (hit && away.lengthSqr() > 1.0e-4) {
            away = away.normalize();
            target.push(away.x * 1.2, 0.4, away.z * 1.2);
        }
        return hit;
    }

    /** The stomp lands: everyone on the ground in the ring takes 10 damage (halved in light) and is thrown out. */
    public int stomp(ServerLevel level) {
        this.playSound(SoundEvents.ANVIL_LAND, 2.0f, 0.4f);
        this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 0.8f, 0.6f);
        for (int r = 1; r <= (int) FearLogic.STOMP_RADIUS; r++) ring(level, ParticleTypes.SOUL_FIRE_FLAME, r, 6 + r * 4);
        float damage = FearLogic.dreadDamage(FearLogic.DREAD_STOMP, light());
        int hit = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(FearLogic.STOMP_RADIUS, 2, FearLogic.STOMP_RADIUS), FearAura::affects)) {
            Vec3 away = e.position().subtract(this.position()).multiply(1, 0, 1);
            if (!FearLogic.stompHits(away.length(), e.getY() - this.getY(), e.onGround())) continue;
            if (e.hurtServer(level, this.damageSources().mobAttack(this), damage)) hit++;
            away = away.lengthSqr() > 1.0e-4 ? away.normalize() : Vec3.ZERO;
            e.push(away.x * 0.8, 0.6, away.z * 0.8);
        }
        return hit;
    }

    private void ring(ServerLevel level, ParticleOptions particle, double radius, int points) {
        for (int i = 0; i < points; i++) {
            double a = i * Math.PI * 2 / points;
            level.sendParticles(particle, this.getX() + Math.cos(a) * radius, this.getY() + 0.15, this.getZ() + Math.sin(a) * radius, 1, 0, 0.05, 0, 0.01);
        }
    }

    /** Walks to its target while no action runs. */
    private class ApproachGoal extends Goal {
        private int repath;

        ApproachGoal() { this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK)); }

        @Override public boolean canUse() { return !rising() && action() == ACT_NONE && getTarget() != null; }
        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public void start() { repath = 0; }
        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) return;
            getLookControl().setLookAt(target, 30f, 30f);
            if (--repath <= 0) {
                repath = 10;
                if (distanceTo(target) > reach(target) - 0.4) getNavigation().moveTo(target, 1.0);
                else getNavigation().stop();
            }
        }
    }
}

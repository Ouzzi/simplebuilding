package com.simplebuilding.modules.simplemobs;

import java.util.EnumSet;
import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Brimguard (Schirmling, BS25-25, owner 01.11.: sombrero hat M, venting yes): a squat 0.6 x 1.5 End boss under a wide
 * bedrock sombrero (brim edge 0.8 above its feet, hat radius 1.1). It cannot be hit from above: every hit is traced to
 * its point on the body and its slope ({@link BossLogic#hatBlocks}); what the hat covers is caught (no damage, sparks,
 * anvil ring). It ducks to 1.0 block (heights x 2/3, the brim covers more) when shot at steeply or when its target keeps
 * away. Phase 1: hat slam (radius 3, 16 damage). Phase 2 (below 65 %): charge (6 blocks, 20 damage) and venting - after
 * every 2nd slam the brim lifts by 0.5 for 2.5 s and it takes 1.5 x damage. Phase 3 (below 30 %): the hat decays (brim
 * +0.1 per 10 % below 30 %), it walks faster and ducks shorter. Summoned only ({@link BossRituals}). Drops the Ender Gem.
 */
public class BrimguardEntity extends Monster {
    public static final int ACT_NONE = 0, ACT_SLAM = 1, ACT_CHARGE_WINDUP = 2, ACT_CHARGE = 3;
    private static final EntityDataAccessor<Integer> DATA_ACTION = SynchedEntityData.defineId(BrimguardEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_DUCK = SynchedEntityData.defineId(BrimguardEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_VENT = SynchedEntityData.defineId(BrimguardEntity.class, EntityDataSerializers.INT);
    private static final Identifier SPEED_ID = Identifier.fromNamespaceAndPath(MobsRegistry.ID, "brimguard_phase");
    private static final Identifier DUCK_ID = Identifier.fromNamespaceAndPath(MobsRegistry.ID, "brimguard_duck");

    private final ServerBossEvent bossEvent = new ServerBossEvent(Mth.createInsecureUUID(this.random), this.getDisplayName(),
            BossEvent.BossBarColor.WHITE, BossBars.overlayFor(BossLogic.BRIM_PHASE2, BossLogic.BRIM_PHASE3));

    private int actionTicks, slamCooldown = 40, chargeCooldown = 60, duckCooldown, duckTicks, slams;
    private Vec3 chargeDir = Vec3.ZERO;
    /** Client: ticks the current action has run (for the renderer). */
    private int clientActionAge;

    public BrimguardEntity(EntityType<? extends BrimguardEntity> type, Level level) {
        super(type, level);
        this.xpReward = BossLogic.BRIM_XP;
        bossEvent.setName(BossBars.title(this.getDisplayName(), false, BossBars.phaseName("brimguard", 1)));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, BossLogic.BRIM_HEALTH)
                .add(Attributes.ARMOR, BossLogic.BRIM_ARMOR)
                .add(Attributes.ARMOR_TOUGHNESS, BossLogic.BRIM_TOUGHNESS)
                .add(Attributes.ATTACK_DAMAGE, BossLogic.BRIM_HIT)
                .add(Attributes.MOVEMENT_SPEED, BossLogic.BRIM_SPEED)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    public ServerBossEvent bossEvent() { return bossEvent; }

    public int phase() { return BossLogic.brimPhase(this.getHealth() / this.getMaxHealth()); }
    public int action() { return this.entityData.get(DATA_ACTION); }
    public boolean ducking() { return this.entityData.get(DATA_DUCK); }
    public boolean venting() { return this.entityData.get(DATA_VENT) > 0; }
    public int ventTicks() { return this.entityData.get(DATA_VENT); }
    public int slams() { return slams; }
    public int clientActionAge() { return clientActionAge; }

    /** Extra brim height now: venting plus the phase 3 decay. */
    public double brimLift() {
        return (venting() ? BossLogic.VENT_LIFT : 0.0) + BossLogic.decayLift(this.getHealth() / this.getMaxHealth());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ACTION, ACT_NONE);
        builder.define(DATA_DUCK, false);
        builder.define(DATA_VENT, 0);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_DUCK.equals(key)) this.refreshDimensions();
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        EntityDimensions base = super.getDefaultDimensions(pose);
        return ducking() ? base.scale(1.0f, BossLogic.CROUCH_SCALE) : base;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ApproachGoal());
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ---- state changes ----------------------------------------------------------------------

    private void setAction(int action, int ticks) {
        this.entityData.set(DATA_ACTION, action);
        actionTicks = ticks;
    }

    /** Duck for the given ticks (speed halved meanwhile). */
    public void duck(int ticks) {
        duckTicks = ticks;
        this.entityData.set(DATA_DUCK, true);
        duckCooldown = BossLogic.DUCK_COOLDOWN + ticks;
        this.playSound(SoundEvents.SHULKER_CLOSE, 1.0f, 0.6f);
    }

    /** Lift the brim: 2.5 s of venting (hit from above, 1.5 x damage). */
    public void vent() {
        this.entityData.set(DATA_VENT, BossLogic.VENT_TICKS);
        this.playSound(SoundEvents.SHULKER_OPEN, 1.2f, 0.5f);
        this.playSound(SoundEvents.FIRE_EXTINGUISH, 0.8f, 0.6f);
    }

    // ---- the hat rule -----------------------------------------------------------------------

    /**
     * Where and how steeply a hit reaches the body: [height above the feet, slope in radians], or empty when the damage
     * has no direction (fire, magic, ...). Projectiles: their path through the body box; melee: the attacker's line of
     * sight, else the body point nearest to its eyes; explosions: the body point nearest to the blast.
     */
    public Optional<double[]> hitGeometry(DamageSource source) {
        AABB box = this.getBoundingBox();
        Entity direct = source.getDirectEntity();
        Entity attacker = source.getEntity();
        Vec3 from, to;
        if (direct != null && direct != attacker) {
            Vec3 v = direct.getDeltaMovement();
            if (v.lengthSqr() < 1.0e-6) v = box.getCenter().subtract(direct.position());
            from = direct.position().subtract(v.scale(3.0));
            to = direct.position().add(v.scale(3.0));
            Optional<Vec3> hit = box.inflate(0.3).clip(from, to);
            Vec3 point = clampInto(box, hit.orElse(direct.position()));
            return Optional.of(new double[] {point.y - this.getY(), slope(v)});
        }
        if (attacker != null) {
            Vec3 eye = attacker.getEyePosition();
            Vec3 look = attacker.getViewVector(1.0f);
            Optional<Vec3> hit = box.clip(eye, eye.add(look.scale(8.0)));
            Vec3 point = hit.orElseGet(() -> clampInto(box, eye));
            return Optional.of(new double[] {point.y - this.getY(), slope(point.subtract(eye))});
        }
        Vec3 pos = source.getSourcePosition();
        if (pos != null) {
            Vec3 point = clampInto(box, pos);
            return Optional.of(new double[] {point.y - this.getY(), slope(point.subtract(pos))});
        }
        return Optional.empty();
    }

    private static Vec3 clampInto(AABB box, Vec3 p) {
        return new Vec3(Mth.clamp(p.x, box.minX, box.maxX), Mth.clamp(p.y, box.minY, box.maxY), Mth.clamp(p.z, box.minZ, box.maxZ));
    }

    /** Downward slope of a direction in radians (positive = coming down from above). */
    static double slope(Vec3 dir) {
        return Math.atan2(-dir.y, Math.max(1.0e-4, dir.horizontalDistance()));
    }

    /** Whether the hat catches this damage (from above, a falling block or anvil, or a shot whose line the brim covers). */
    public boolean hatCatches(DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        if (source.is(DamageTypes.FALLING_ANVIL) || source.is(DamageTypes.FALLING_BLOCK) || source.is(DamageTypes.FALLING_STALACTITE)) return true;
        Optional<double[]> g = hitGeometry(source);
        return g.isPresent() && BossLogic.hatBlocks(g.get()[0], g.get()[1], ducking(), brimLift());
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.is(DamageTypeTags.IS_FALL)) return false;
        Optional<double[]> g = hitGeometry(source);
        if (hatCatches(source)) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + BossLogic.brimHeight(ducking(), brimLift()) + 0.1, this.getZ(),
                    10, 0.5, 0.1, 0.5, 0.15);
            this.playSound(SoundEvents.ANVIL_LAND, 0.6f, 1.5f);
            // shot at steeply: duck under the hat
            if (g.isPresent() && !ducking() && BossLogic.shouldDuck(duckCooldown, Math.toDegrees(g.get()[1]), 0.0)) duck(duckTicks());
            return false;
        }
        if (venting()) damage *= BossLogic.VENT_DAMAGE;
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && g.isPresent() && !ducking() && BossLogic.shouldDuck(duckCooldown, Math.toDegrees(g.get()[1]), 0.0)) duck(duckTicks());
        return hurt;
    }

    private int duckTicks() { return phase() >= 3 ? BossLogic.DUCK_TICKS_P3 : BossLogic.DUCK_TICKS; }

    @Override public boolean removeWhenFarAway(double distSqr) { return false; }
    @Override public boolean isPushable() { return false; }

    @Override
    public void die(DamageSource source) {
        bossEvent.removeAllPlayers();
        super.die(source);
    }

    @Override
    public void remove(RemovalReason reason) {
        bossEvent.removeAllPlayers();
        super.remove(reason);
    }

    // ---- sounds (Vanilla) -------------------------------------------------------------------

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.SHULKER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.SHULKER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.SHULKER_DEATH; }
    @Override public float getVoicePitch() { return 0.55f + this.random.nextFloat() * 0.1f; }

    // ---- tick -------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            clientActionAge = action() == ACT_NONE ? 0 : clientActionAge + 1;
            if (venting() && this.random.nextInt(2) == 0) {
                this.level().addParticle(ParticleTypes.CLOUD, this.getRandomX(0.6), this.getY() + BossLogic.brimHeight(ducking(), brimLift()) + 0.2,
                        this.getRandomZ(0.6), 0, 0.05, 0);
            }
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        int phase = phase();
        syncBossBar(level, phase);
        if (duckTicks > 0 && --duckTicks == 0) this.entityData.set(DATA_DUCK, false);
        int vent = this.entityData.get(DATA_VENT);
        if (vent > 0) this.entityData.set(DATA_VENT, vent - 1);
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            if (phase >= 3) speed.addOrUpdateTransientModifier(new AttributeModifier(SPEED_ID, BossLogic.PHASE2_SPEED_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            else speed.removeModifier(SPEED_ID);
            if (ducking()) speed.addOrUpdateTransientModifier(new AttributeModifier(DUCK_ID, -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            else speed.removeModifier(DUCK_ID);
        }
        if (slamCooldown > 0) slamCooldown--;
        if (chargeCooldown > 0) chargeCooldown--;
        if (duckCooldown > 0) duckCooldown--;
        LivingEntity target = this.getTarget();
        int act = action();
        if (act == ACT_SLAM) {
            this.getNavigation().stop();
            if (actionTicks % 3 == 0) level.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 0.9, this.getZ(), 6, 0.9, 0.05, 0.9, 0.05);
            if (--actionTicks <= 0) { setAction(ACT_NONE, 0); slam(level); }
            return;
        }
        if (act == ACT_CHARGE_WINDUP) {
            this.getNavigation().stop();
            if (target != null) this.getLookControl().setLookAt(target, 30f, 30f);
            level.sendParticles(ParticleTypes.DUST_PLUME, this.getX(), this.getY() + 0.05, this.getZ(), 2, 0.3, 0.0, 0.3, 0.02);
            if (--actionTicks <= 0) {
                Vec3 to = target != null ? target.position().subtract(this.position()) : this.getLookAngle();
                chargeDir = new Vec3(to.x, 0, to.z);
                chargeDir = chargeDir.lengthSqr() > 1.0e-4 ? chargeDir.normalize() : new Vec3(1, 0, 0);
                this.playSound(SoundEvents.RAVAGER_ROAR, 1.2f, 1.2f);
                setAction(ACT_CHARGE, BossLogic.CHARGE_TICKS);
            }
            return;
        }
        if (act == ACT_CHARGE) {
            this.getNavigation().stop();
            // 6 blocks over 16 ticks
            this.setDeltaMovement(chargeDir.x * 0.4, this.getDeltaMovement().y, chargeDir.z * 0.4);
            if (target != null && this.getBoundingBox().inflate(0.5).intersects(target.getBoundingBox())) {
                chargeHit(level, target);
                setAction(ACT_NONE, 0);
                return;
            }
            if (--actionTicks <= 0 || this.horizontalCollision) setAction(ACT_NONE, 0);
            return;
        }
        if (target == null) return;
        double dist = this.distanceTo(target);
        if (!ducking() && BossLogic.shouldDuck(duckCooldown, 0.0, dist)) { duck(duckTicks()); return; }
        if (slamCooldown <= 0 && dist <= BossLogic.SLAM_RADIUS) {
            slamCooldown = BossLogic.SLAM_COOLDOWN;
            setAction(ACT_SLAM, BossLogic.SLAM_WINDUP);
            this.playSound(SoundEvents.SHULKER_HURT_CLOSED, 1.5f, 0.5f);
        } else if (phase >= 2 && chargeCooldown <= 0 && dist >= 4.0 && dist <= 10.0 && !ducking() && this.onGround()) {
            chargeCooldown = BossLogic.CHARGE_COOLDOWN;
            setAction(ACT_CHARGE_WINDUP, BossLogic.CHARGE_WINDUP);
            this.playSound(SoundEvents.RAVAGER_STEP, 1.5f, 0.6f);
        }
    }

    /** The hat comes down: everyone in radius 3 takes 16 damage and is pushed out; every 2nd slam (phase 2+) vents. */
    public int slam(ServerLevel level) {
        this.playSound(SoundEvents.ANVIL_LAND, 1.5f, 0.5f);
        level.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.5, this.getZ(), 1, 0, 0, 0, 0);
        int hit = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(BossLogic.SLAM_RADIUS, 1.5, BossLogic.SLAM_RADIUS),
                e -> e != this && e.isAlive() && !(e instanceof Player p && (p.isCreative() || p.isSpectator())))) {
            if (e.distanceTo(this) > BossLogic.SLAM_RADIUS + 0.5) continue;
            if (e.hurtServer(level, this.damageSources().mobAttack(this), BossLogic.SLAM_DAMAGE)) hit++;
            Vec3 away = e.position().subtract(this.position()).multiply(1, 0, 1);
            away = away.lengthSqr() > 1.0e-4 ? away.normalize() : Vec3.ZERO;
            e.push(away.x * 0.9, 0.4, away.z * 0.9);
        }
        slams++;
        if (phase() >= 2 && BossLogic.ventsAfter(slams)) vent();
        return hit;
    }

    /** The charge hits: 20 damage and a hard push. */
    public boolean chargeHit(ServerLevel level, LivingEntity target) {
        boolean hit = target.hurtServer(level, this.damageSources().mobAttack(this), BossLogic.CHARGE_DAMAGE);
        if (hit) target.push(chargeDir.x * 1.4, 0.5, chargeDir.z * 1.4);
        this.playSound(SoundEvents.ANVIL_LAND, 1.0f, 0.8f);
        return hit;
    }

    /** Test hooks. */
    public void startSlam() { setAction(ACT_SLAM, BossLogic.SLAM_WINDUP); }
    public void startCharge() { setAction(ACT_CHARGE_WINDUP, BossLogic.CHARGE_WINDUP); }

    private void syncBossBar(ServerLevel level, int phase) {
        if (!MobsConfig.minibossBars) { bossEvent.removeAllPlayers(); return; }
        BossBars.apply(bossEvent, BossBars.title(this.getDisplayName(), false, BossBars.phaseName("brimguard", phase)), BossEvent.BossBarColor.WHITE,
                BossBars.overlayFor(BossLogic.BRIM_PHASE2, BossLogic.BRIM_PHASE3), this.getHealth() / this.getMaxHealth());
        BossBars.track(bossEvent, this, level, MobsConfig.bossBarRange);
    }

    /** Walks to its target while no action runs. */
    private class ApproachGoal extends Goal {
        private int repath;

        ApproachGoal() { this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK)); }

        @Override public boolean canUse() { return action() == ACT_NONE && getTarget() != null; }
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
                if (distanceTo(target) > 2.0) getNavigation().moveTo(target, 1.0);
                else getNavigation().stop();
            }
        }
    }
}

package com.simplebuilding.modules.simplemobs;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Hollow Watcher (Hohler Wächter), fear mob F-C: a 4 block shadow with many eyes and End stone splinters, rare on the
 * outer End islands. It moves only while nobody looks at it (like the Creaking); a look through a spyglass freezes it
 * for 3 s. Unwatched it teleports behind its target (announced by portal particles at the spot), grips for 2 s or
 * claws; a target that stares at it for 5 s makes it scream (Shivering II and a short Blindness), so staring is no
 * lasting answer. Its Shivering aura is the fear mobs' shared one.
 */
public class HollowWatcherEntity extends Monster {
    public static final int ACT_NONE = 0, ACT_GRIP_WINDUP = 1, ACT_GRIP = 2, ACT_CLAW = 3, ACT_SCREAM = 4, ACT_TELEPORT = 5;
    private static final EntityDataAccessor<Integer> DATA_ACTION = SynchedEntityData.defineId(HollowWatcherEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FROZEN = SynchedEntityData.defineId(HollowWatcherEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_WATCHED = SynchedEntityData.defineId(HollowWatcherEntity.class, EntityDataSerializers.BOOLEAN);

    private int actionTicks, gripCooldown = 40, clawCooldown, teleportCooldown = 60, screamCooldown = 200, stareTicks, fleeTicks;
    private @Nullable Vec3 teleportTo, gripAt;
    private int grippedId = -1;

    public HollowWatcherEntity(EntityType<? extends HollowWatcherEntity> type, Level level) {
        super(type, level);
        this.xpReward = FearLogic.WATCHER_XP;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, FearLogic.WATCHER_HEALTH)
                .add(Attributes.ARMOR, FearLogic.WATCHER_ARMOR)
                .add(Attributes.ATTACK_DAMAGE, FearLogic.WATCHER_CLAW)
                .add(Attributes.MOVEMENT_SPEED, FearLogic.WATCHER_SPEED)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new StalkGoal());
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.5));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, false, false,
                (e, level) -> FearAura.affects(e)));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ACTION, ACT_NONE);
        builder.define(DATA_FROZEN, 0);
        builder.define(DATA_WATCHED, false);
    }

    // ---- state ------------------------------------------------------------------------------

    public int action() { return this.entityData.get(DATA_ACTION); }
    public int frozenTicks() { return this.entityData.get(DATA_FROZEN); }
    public boolean frozen() { return frozenTicks() > 0; }
    /** Someone looks at it: it stands still and starts no attack (a running grip or scream goes on). */
    public boolean watched() { return this.entityData.get(DATA_WATCHED); }
    public @Nullable Vec3 teleportTarget() { return teleportTo; }
    public int stareTicks() { return stareTicks; }

    private void setAction(int action, int ticks) {
        this.entityData.set(DATA_ACTION, action);
        actionTicks = ticks;
    }

    /** Screenshot/test hook. */
    public void playAction(int action, int ticks) { setAction(action, ticks); }

    public void freeze() {
        if (!frozen()) this.playSound(SoundEvents.PLAYER_HURT_FREEZE, 1.0f, 0.5f);
        this.entityData.set(DATA_FROZEN, FearLogic.FREEZE_TICKS);
        releaseGrip(null, false);
        if (action() != ACT_NONE) setAction(ACT_NONE, 0);
        teleportTo = null;
    }

    /** A hit starts a 1 s countdown after which it blinks away (announced by portal particles and a sound). */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && isAlive() && fleeTicks == 0) {
            fleeTicks = FearLogic.HIT_TELEPORT_TICKS;
            this.playSound(SoundEvents.ENDERMAN_STARE, 1.5f, 0.5f);
        }
        return hurt;
    }

    /** Hit-and-run: the countdown runs on even while frozen; at 0 it vanishes to a free spot 12-20 blocks away. */
    private void tickFlee(ServerLevel level) {
        if (fleeTicks <= 0) return;
        if (fleeTicks % 2 == 0) level.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 2.0, this.getZ(), 10, 0.5, 1.5, 0.5, 0.05);
        if (--fleeTicks > 0) return;
        Vec3 dest = fleeSpot(level);
        if (dest == null) return;
        releaseGrip(null, false);
        setAction(ACT_NONE, 0);
        teleportTo = null;
        level.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 2.0, this.getZ(), 40, 0.4, 1.5, 0.4, 0.1);
        this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.2f, 0.5f);
        this.teleportTo(dest.x, dest.y, dest.z);
        this.getNavigation().stop();
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, dest.x, dest.y + 2.0, dest.z, 30, 0.4, 1.5, 0.4, 0.1);
        level.playSound(null, dest.x, dest.y, dest.z, SoundEvents.ENDERMAN_TELEPORT, this.getSoundSource(), 1.2f, 0.5f);
    }

    /** A free standing spot 12-20 blocks away in a random direction; null if ten tries find none. */
    public @Nullable Vec3 fleeSpot(ServerLevel level) {
        for (int i = 0; i < 10; i++) {
            double ang = this.random.nextDouble() * Math.PI * 2;
            double d = FearLogic.HIT_TELEPORT_MIN + this.random.nextDouble() * (FearLogic.HIT_TELEPORT_MAX - FearLogic.HIT_TELEPORT_MIN);
            Vec3 p = groundSpot(level, this.getX() + Math.cos(ang) * d, Math.floor(this.getY()), this.getZ() + Math.sin(ang) * d);
            if (p != null) return p;
        }
        return null;
    }

    // ---- rules ------------------------------------------------------------------------------

    @Override protected boolean isImmobile() { return super.isImmobile() || frozen() || watched(); }
    @Override public boolean removeWhenFarAway(double distSqr) { return false; }
    @Override protected SoundEvent getAmbientSound() { return watched() || frozen() ? null : SoundEvents.ENDERMAN_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ENDERMAN_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENDERMAN_DEATH; }
    @Override public float getVoicePitch() { return 0.45f + this.random.nextFloat() * 0.1f; }

    /** Whether the player looks at it: the Creaking's view cone (sight line, several heights); blind players see nothing. */
    public boolean seenBy(Player p) {
        if (p.isSpectator() || !p.isAlive() || p.hasEffect(MobEffects.BLINDNESS) || p.distanceToSqr(this) > 64 * 64) return false;
        return this.isLookingAtMe(p, 0.5, false, true, this.getEyeY(), this.getY() + 1.0, this.getY() + 2.0);
    }

    /** Looking at it through a spyglass (a narrow cone, like the Enderman's stare). */
    public boolean scopedBy(Player p) {
        return p.isScoping() && !p.isSpectator() && p.isAlive() && p.distanceToSqr(this) < 128 * 128
                && this.isLookingAtMe(p, 0.05, true, true, this.getEyeY(), this.getY() + 2.0);
    }

    /** Updates watched/frozen from all players around; returns whether it is watched. */
    public boolean updateGaze(ServerLevel level) {
        boolean seen = false, targetStares = false;
        for (Player p : level.players()) {
            if (scopedBy(p)) freeze();
            if (seenBy(p)) {
                seen = true;
                if (p == this.getTarget()) targetStares = true;
            }
        }
        LivingEntity target = this.getTarget();
        if (target != null && !(target instanceof Player) && target.entityTags().contains(FearAura.TEST_TAG)) targetStares = seen;
        stareTicks = targetStares ? stareTicks + 1 : 0;
        this.entityData.set(DATA_WATCHED, seen);
        return seen;
    }

    // ---- tick -------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            if (frozen() && this.random.nextInt(2) == 0)
                this.level().addParticle(ParticleTypes.SNOWFLAKE, this.getRandomX(0.6), this.getRandomY(), this.getRandomZ(0.6), 0, -0.02, 0);
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        if (this.tickCount % FearLogic.AURA_INTERVAL == 0) FearAura.pulse(level, this, false);
        boolean watched = updateGaze(level);
        tickFlee(level);
        int frozen = frozenTicks();
        if (frozen > 0) {
            this.entityData.set(DATA_FROZEN, frozen - 1);
            this.getNavigation().stop();
            this.setDeltaMovement(0, Math.min(0, this.getDeltaMovement().y), 0);
            return;
        }
        if (this.isNoAi()) return;
        if (gripCooldown > 0) gripCooldown--;
        if (clawCooldown > 0) clawCooldown--;
        if (teleportCooldown > 0) teleportCooldown--;
        if (screamCooldown > 0) screamCooldown--;
        if (watched) {
            this.getNavigation().stop();
            this.setDeltaMovement(0, Math.min(0, this.getDeltaMovement().y), 0);
        }
        LivingEntity target = this.getTarget();
        if (target != null && !FearAura.affects(target)) { this.setTarget(null); target = null; }
        int act = action();
        if (act != ACT_NONE) { tickAction(level, act, target, watched); return; }
        if (target == null) return;
        double dist = this.distanceTo(target);
        if (screamCooldown == 0 && (stareTicks >= FearLogic.STARE_LIMIT || !watched && dist < FearLogic.SCREAM_RADIUS && this.hasLineOfSight(target))) {
            startScream();
            return;
        }
        if (watched) return;
        if (dist > FearLogic.TELEPORT_MIN_DIST && teleportCooldown == 0) {
            Vec3 dest = behind(level, target);
            teleportCooldown = FearLogic.TELEPORT_COOLDOWN;
            if (dest != null) {
                teleportTo = dest;
                setAction(ACT_TELEPORT, FearLogic.TELEPORT_WINDUP);
                level.playSound(null, dest.x, dest.y, dest.z, SoundEvents.ENDERMAN_TELEPORT, this.getSoundSource(), 1.0f, 0.5f);
            }
        } else if (dist <= reach(target)) {
            this.getLookControl().setLookAt(target, 30f, 30f);
            if (gripCooldown == 0) {
                setAction(ACT_GRIP_WINDUP, FearLogic.GRIP_WINDUP);
                this.playSound(SoundEvents.ENDERMAN_STARE, 1.2f, 0.6f);
            } else if (clawCooldown == 0) {
                setAction(ACT_CLAW, FearLogic.CLAW_WINDUP);
                this.playSound(SoundEvents.ENDERMAN_AMBIENT, 1.0f, 1.3f);
            }
        }
    }

    private void tickAction(ServerLevel level, int act, @Nullable LivingEntity target, boolean watched) {
        // Being watched stops wind-ups of moves it starts; a running grip and the scream go on.
        if (watched && (act == ACT_GRIP_WINDUP || act == ACT_CLAW || act == ACT_TELEPORT)) {
            setAction(ACT_NONE, 0);
            teleportTo = null;
            return;
        }
        int t = --actionTicks;
        switch (act) {
            case ACT_TELEPORT -> {
                Vec3 d = teleportTo;
                if (d != null && t % 2 == 0) level.sendParticles(ParticleTypes.REVERSE_PORTAL, d.x, d.y + 2.0, d.z, 8, 0.3, 1.2, 0.3, 0.02);
                if (t <= 0) {
                    setAction(ACT_NONE, 0);
                    if (d != null && level.noCollision(this, this.getDimensions(this.getPose()).makeBoundingBox(d))) {
                        level.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 2.0, this.getZ(), 30, 0.3, 1.5, 0.3, 0.1);
                        this.teleportTo(d.x, d.y, d.z);
                        this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0f, 0.5f);
                        if (target != null) this.lookAt(target, 180f, 90f);
                    }
                    teleportTo = null;
                }
            }
            case ACT_GRIP_WINDUP -> {
                if (t <= 0) {
                    gripCooldown = FearLogic.GRIP_COOLDOWN;
                    if (target != null && this.distanceTo(target) <= reach(target) + 0.5) grip(level, target);
                    else setAction(ACT_NONE, 0);
                }
            }
            case ACT_GRIP -> {
                Entity held = level.getEntity(grippedId);
                if (!(held instanceof LivingEntity h) || !h.isAlive() || gripAt == null) { releaseGrip(null, false); return; }
                h.teleportTo(gripAt.x, gripAt.y, gripAt.z);
                h.setDeltaMovement(Vec3.ZERO);
                h.needsSync = true;
                if (t <= 0) releaseGrip(h, true);
            }
            case ACT_CLAW -> {
                if (t <= 0) {
                    setAction(ACT_NONE, 0);
                    clawCooldown = FearLogic.CLAW_COOLDOWN;
                    if (target != null && this.distanceTo(target) <= reach(target) + 0.5) {
                                        target.hurtServer(level, this.damageSources().mobAttack(this), FearLogic.WATCHER_CLAW);
                    }
                }
            }
            case ACT_SCREAM -> {
                if (t % 4 == 0) level.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getEyeY(), this.getZ(), 6, 0.4, 0.3, 0.4, 0.05);
                if (t <= 0) { setAction(ACT_NONE, 0); scream(level); }
            }
            default -> setAction(ACT_NONE, 0);
        }
    }

    private double reach(LivingEntity target) {
        return this.getBbWidth() / 2 + target.getBbWidth() / 2 + FearLogic.WATCHER_REACH;
    }

    /** The grip closes: 6 damage, the target is held in place for 2 s. */
    public void grip(ServerLevel level, LivingEntity target) {
        if (target.hurtServer(level, this.damageSources().mobAttack(this), FearLogic.GRIP_DAMAGE) || target.isAlive()) {
            grippedId = target.getId();
            gripAt = target.position();
            setAction(ACT_GRIP, FearLogic.GRIP_TICKS);
            this.playSound(SoundEvents.ENDERMAN_SCREAM, 0.8f, 1.4f);
        }
    }

    private void releaseGrip(@Nullable LivingEntity held, boolean hurt) {
        if (action() == ACT_GRIP) setAction(ACT_NONE, 0);
        grippedId = -1;
        gripAt = null;
        if (held != null && hurt && this.level() instanceof ServerLevel level) {
            held.hurtServer(level, this.damageSources().mobAttack(this), FearLogic.GRIP_RELEASE_DAMAGE);
            Vec3 away = held.position().subtract(this.position()).multiply(1, 0, 1);
            if (away.lengthSqr() > 1.0e-4) { away = away.normalize(); held.push(away.x * 1.0, 0.5, away.z * 1.0); }
        }
    }

    /** Whether it holds this entity right now. */
    public boolean holds(Entity e) { return action() == ACT_GRIP && grippedId == e.getId(); }

    public void startScream() {
        screamCooldown = FearLogic.SCREAM_COOLDOWN;
        stareTicks = 0;
        setAction(ACT_SCREAM, FearLogic.SCREAM_WINDUP);
        this.playSound(SoundEvents.ENDERMAN_STARE, 2.0f, 0.4f);
    }

    /** The scream lands: Shivering II (10 s) and Blindness (3 s) for everyone around (immune players keep their calm). */
    public int scream(ServerLevel level) {
        this.playSound(SoundEvents.ENDERMAN_SCREAM, 3.0f, 0.4f);
        level.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getEyeY(), this.getZ(), 1, 0, 0, 0, 0);
        Holder<MobEffect> shivering = FearMobs.shivering();
        int hit = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(FearLogic.SCREAM_RADIUS), FearAura::affects)) {
            if (e.distanceTo(this) > FearLogic.SCREAM_RADIUS) continue;
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, FearLogic.SCREAM_BLIND_TICKS, 0), this);
            if (!FearAura.cure(e) && shivering != null) FearAura.give(e, shivering, 1, FearLogic.SCREAM_SHIVER_TICKS);
            hit++;
        }
        return hit;
    }

    /** A free spot 2.5 blocks behind the target (opposite its look), on solid ground; null if there is none. */
    public @Nullable Vec3 behind(ServerLevel level, LivingEntity target) {
        Vec3 look = target.getLookAngle().multiply(1, 0, 1);
        if (look.lengthSqr() < 1.0e-4) look = new Vec3(0, 0, 1);
        Vec3 base = target.position().subtract(look.normalize().scale(FearLogic.TELEPORT_BEHIND));
        return groundSpot(level, base.x, Math.floor(target.getY()), base.z);
    }

    /** A free standing spot on solid ground within 2 blocks of the given height; null if there is none. */
    private @Nullable Vec3 groundSpot(ServerLevel level, double x, double y, double z) {
        for (int dy : new int[] {0, 1, -1, 2, -2}) {
            Vec3 p = new Vec3(x, y + dy, z);
            BlockPos below = BlockPos.containing(p.x, p.y - 0.5, p.z);
            AABB box = this.getDimensions(this.getPose()).makeBoundingBox(p);
            if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) && level.noCollision(this, box) && !level.containsAnyLiquid(box)) return p;
        }
        return null;
    }

    /** Follows its target while unwatched and idle. */
    private class StalkGoal extends Goal {
        private int repath;

        StalkGoal() { this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK)); }

        @Override public boolean canUse() { return action() == ACT_NONE && getTarget() != null && !watched() && !frozen(); }
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
                if (distanceTo(target) > reach(target) - 0.3) getNavigation().moveTo(target, 1.0);
                else getNavigation().stop();
            }
        }
    }
}

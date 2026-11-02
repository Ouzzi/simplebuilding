package com.simplebuilding.modules.simplemobs;

import java.util.EnumSet;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Void Runner (Leerenläufer, BS25-24, owner 01.11.: variant A1 "fencer with forearm blades"): a slim 0.5 x 1.9 End boss,
 * pure melee, no magic - very fast and hard to hit. It circles its target at 5 blocks and sprints in for a hit, steps
 * aside after every hit it takes (2.5 blocks) and sidesteps 60 % of arrows and tridents. Phase 2 (below 66 %): faster
 * and the lunge (8 ticks crouch, a jump of up to 6 blocks, 18 damage) - a shield that blocks the lunge makes it stagger
 * for 1.5 s. Phase 3 (below 33 %): rage, steps aside twice as often, hits 1.25 x as hard. Summoned only
 * ({@link BossRituals}). Drops the Ender Heart.
 */
public class VoidRunnerEntity extends Monster {
    public static final int ACT_NONE = 0, ACT_WINDUP = 1, ACT_LUNGE = 2, ACT_STAGGER = 3;
    private static final EntityDataAccessor<Integer> DATA_ACTION = SynchedEntityData.defineId(VoidRunnerEntity.class, EntityDataSerializers.INT);
    private static final Identifier SPEED_ID = Identifier.fromNamespaceAndPath(MobsRegistry.ID, "void_runner_phase");

    private final ServerBossEvent bossEvent = new ServerBossEvent(Mth.createInsecureUUID(this.random), this.getDisplayName(),
            BossEvent.BossBarColor.PURPLE, BossBars.overlayFor());

    private int actionTicks, hitCooldown, dodgeCooldown, lungeCooldown = 60, circleTicks;
    /** +1 / -1: which way it circles; flips after every dodge. */
    private int circleDir = 1;
    /** Client: ticks the current action has run, ticks left of the blade strike (for the renderer). */
    private int clientActionAge, clientStrike;

    public VoidRunnerEntity(EntityType<? extends VoidRunnerEntity> type, Level level) {
        super(type, level);
        this.xpReward = BossLogic.RUNNER_XP;
        bossEvent.setName(BossBars.title(this.getDisplayName(), false, BossBars.phaseName("void_runner", 1)));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, BossLogic.RUNNER_HEALTH)
                .add(Attributes.ARMOR, BossLogic.RUNNER_ARMOR)
                .add(Attributes.ARMOR_TOUGHNESS, BossLogic.RUNNER_TOUGHNESS)
                .add(Attributes.ATTACK_DAMAGE, BossLogic.RUNNER_HIT)
                .add(Attributes.MOVEMENT_SPEED, BossLogic.RUNNER_SPEED)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    public ServerBossEvent bossEvent() { return bossEvent; }

    public int phase() { return BossLogic.runnerPhase(this.getHealth() / this.getMaxHealth()); }
    public int action() { return this.entityData.get(DATA_ACTION); }
    public int clientActionAge() { return clientActionAge; }
    public int clientStrike() { return clientStrike; }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) clientStrike = 8;
        else super.handleEntityEvent(id);
    }
    public int dodgeCooldown() { return dodgeCooldown; }
    public int lungeCooldown() { return lungeCooldown; }

    /** Test hook: clear the cooldowns. */
    public void resetCooldowns() { dodgeCooldown = 0; lungeCooldown = 0; hitCooldown = 0; }

    private void setAction(int action, int ticks) {
        this.entityData.set(DATA_ACTION, action);
        actionTicks = ticks;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ACTION, ACT_NONE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new CircleGoal());
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ---- rules ------------------------------------------------------------------------------

    @Override public boolean removeWhenFarAway(double distSqr) { return false; }
    @Override public boolean isPushable() { return false; }
    @Override protected boolean isImmobile() { return super.isImmobile() || action() == ACT_STAGGER; }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurtServer(level, source, damage);
        if (source.is(DamageTypeTags.IS_FALL)) return false; // its own jumps
        boolean staggered = action() == ACT_STAGGER;
        // arrows and tridents: sidestep most of them (a speed check, no shield or magic)
        if (!staggered && source.getDirectEntity() instanceof Projectile p && this.random.nextFloat() < BossLogic.PROJECTILE_DODGE) {
            dash(level, p.getDeltaMovement());
            return false;
        }
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && !staggered && dodgeCooldown <= 0 && this.isAlive()) {
            Entity from = source.getEntity();
            dash(level, from != null ? this.position().subtract(from.position()) : this.getLookAngle());
            dodgeCooldown = BossLogic.dodgeCooldown(phase());
        }
        return hurt;
    }

    /** Sideways dash (2.5 blocks) away from the line of an attack, with a puff of portal particles. */
    public void dash(ServerLevel level, Vec3 incoming) {
        Vec3 flat = new Vec3(incoming.x, 0, incoming.z);
        if (flat.lengthSqr() < 1.0e-4) flat = this.getLookAngle().multiply(1, 0, 1);
        if (flat.lengthSqr() < 1.0e-4) flat = new Vec3(1, 0, 0);
        flat = flat.normalize();
        Vec3 side = new Vec3(-flat.z * circleDir, 0, flat.x * circleDir);
        // about 2.5 blocks with ground friction
        double v = BossLogic.DODGE_DISTANCE * 0.45;
        this.setDeltaMovement(side.x * v, 0.18, side.z * v);
        this.needsSync = true;
        circleDir = -circleDir;
        level.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 1.0, this.getZ(), 16, 0.2, 0.5, 0.2, 0.3);
        this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 1.6f);
    }

    /** A shield that blocks the lunge: the runner staggers for 1.5 s and cannot step aside meanwhile. */
    @Override
    protected void blockedByItem(LivingEntity defender, DamageSource source, float damage, boolean fullyBlocked) {
        super.blockedByItem(defender, source, damage, fullyBlocked);
        if (action() == ACT_LUNGE) stagger();
    }

    public void stagger() {
        setAction(ACT_STAGGER, BossLogic.STAGGER_TICKS);
        this.setDeltaMovement(this.getDeltaMovement().multiply(-0.3, 1.0, -0.3));
        this.playSound(SoundEvents.SHIELD_BLOCK.value(), 1.0f, 0.6f);
    }

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

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ENDERMAN_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ENDERMAN_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENDERMAN_DEATH; }
    @Override public float getVoicePitch() { return 1.3f + this.random.nextFloat() * 0.1f; }

    // ---- tick -------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            clientActionAge = action() == ACT_NONE ? 0 : clientActionAge + 1;
            if (clientStrike > 0) clientStrike--;
            if (this.getDeltaMovement().horizontalDistanceSqr() > 0.04 && this.random.nextInt(2) == 0) {
                this.level().addParticle(ParticleTypes.PORTAL, this.getRandomX(0.4), this.getY() + 1.4, this.getRandomZ(0.4), 0, 0, 0);
            }
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        int phase = phase();
        syncBossBar(level, phase);
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            if (phase >= 2) speed.addOrUpdateTransientModifier(new AttributeModifier(SPEED_ID, BossLogic.PHASE2_SPEED_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            else speed.removeModifier(SPEED_ID);
        }
        if (hitCooldown > 0) hitCooldown--;
        if (dodgeCooldown > 0) dodgeCooldown--;
        if (lungeCooldown > 0) lungeCooldown--;
        LivingEntity target = this.getTarget();
        int act = action();
        if (act == ACT_STAGGER) {
            this.getNavigation().stop();
            if (actionTicks % 5 == 0) level.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 1.9, this.getZ(), 3, 0.2, 0.1, 0.2, 0.05);
            if (--actionTicks <= 0) setAction(ACT_NONE, 0);
            return;
        }
        if (act == ACT_WINDUP) {
            this.getNavigation().stop();
            if (target != null) this.getLookControl().setLookAt(target, 60f, 60f);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 0.3, this.getZ(), 3, 0.3, 0.05, 0.3, 0.02);
            if (--actionTicks <= 0) {
                if (target == null) { setAction(ACT_NONE, 0); return; }
                Vec3 to = target.position().subtract(this.position());
                Vec3 flat = new Vec3(to.x, 0, to.z);
                double dist = Math.min(BossLogic.LUNGE_RANGE, flat.length());
                flat = flat.lengthSqr() > 1.0e-4 ? flat.normalize() : this.getLookAngle().multiply(1, 0, 1);
                // a flat leap: about the target distance (max 6 blocks)
                this.setDeltaMovement(flat.x * dist * 0.24, 0.42, flat.z * dist * 0.24);
                this.needsSync = true;
                this.playSound(SoundEvents.BREEZE_JUMP, 1.2f, 0.8f);
                setAction(ACT_LUNGE, 24);
            }
            return;
        }
        if (act == ACT_LUNGE) {
            if (target != null && this.getBoundingBox().inflate(0.4).intersects(target.getBoundingBox())) {
                lungeHit(level, target);
                setAction(ACT_NONE, 0);
                return;
            }
            if (--actionTicks <= 0 || (actionTicks < 20 && this.onGround())) setAction(ACT_NONE, 0);
            return;
        }
        if (target == null) return;
        double dist = this.distanceTo(target);
        if (BossLogic.lungeReady(phase, lungeCooldown, this.onGround(), dist)) {
            lungeCooldown = BossLogic.LUNGE_COOLDOWN;
            setAction(ACT_WINDUP, BossLogic.LUNGE_WINDUP);
            this.playSound(SoundEvents.ENDERMAN_SCREAM, 1.0f, 1.5f);
            return;
        }
        if (hitCooldown <= 0 && dist <= reach(target)) {
            hitCooldown = BossLogic.RUNNER_HIT_COOLDOWN;
            level.broadcastEntityEvent(this, (byte) 4);
            float dmg = BossLogic.runnerDamage((float) this.getAttributeValue(Attributes.ATTACK_DAMAGE), phase);
            if (target.hurtServer(level, this.damageSources().mobAttack(this), dmg)) {
                this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.4f);
            }
            circleTicks = 0; // back out to the circle
        }
    }

    /** The lunge lands on the target: 18 damage (x1.25 in rage) and a push, unless a shield blocks it (then it staggers). */
    public boolean lungeHit(ServerLevel level, LivingEntity target) {
        float dmg = BossLogic.runnerDamage(BossLogic.LUNGE_DAMAGE, phase());
        boolean hit = target.hurtServer(level, this.damageSources().mobAttack(this), dmg);
        if (hit) {
            Vec3 away = target.position().subtract(this.position()).multiply(1, 0, 1);
            if (away.lengthSqr() > 1.0e-4) { away = away.normalize(); target.push(away.x * 1.0, 0.35, away.z * 1.0); }
        }
        return hit;
    }

    /** Test/screenshot hook: start the lunge wind-up now. */
    public void startLunge() { setAction(ACT_WINDUP, BossLogic.LUNGE_WINDUP); }

    /** Test hook: mark the runner as mid-lunge. */
    public void markLunging() { setAction(ACT_LUNGE, 24); }

    private double reach(LivingEntity target) {
        return this.getBbWidth() / 2 + target.getBbWidth() / 2 + 1.6;
    }

    private void syncBossBar(ServerLevel level, int phase) {
        if (!MobsConfig.minibossBars) { bossEvent.removeAllPlayers(); return; }
        BossBars.apply(bossEvent, BossBars.title(this.getDisplayName(), false, BossBars.phaseName("void_runner", phase)),
                phase == 3 ? BossEvent.BossBarColor.PINK : BossEvent.BossBarColor.PURPLE, BossBars.overlayFor(), this.getHealth() / this.getMaxHealth());
        BossBars.track(bossEvent, this, level, MobsConfig.bossBarRange);
    }

    /** Circles the target at 5 blocks, then sprints in for a hit; steady repath, no special pathing. */
    private class CircleGoal extends Goal {
        private int repath;

        CircleGoal() { this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK)); }

        @Override public boolean canUse() { return action() == ACT_NONE && getTarget() != null; }
        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public void start() { repath = 0; }
        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) return;
            getLookControl().setLookAt(target, 60f, 60f);
            circleTicks++;
            if (--repath > 0) return;
            repath = 5;
            boolean sprintIn = circleTicks > BossLogic.RUNNER_CIRCLE_TICKS && hitCooldown <= 0;
            if (sprintIn) {
                getNavigation().moveTo(target, 1.35);
                return;
            }
            Vec3 from = position().subtract(target.position());
            double a = Math.atan2(from.z, from.x) + circleDir * 0.6;
            double x = target.getX() + Math.cos(a) * BossLogic.RUNNER_CIRCLE, z = target.getZ() + Math.sin(a) * BossLogic.RUNNER_CIRCLE;
            getNavigation().moveTo(x, target.getY(), z, 1.0);
        }
    }
}

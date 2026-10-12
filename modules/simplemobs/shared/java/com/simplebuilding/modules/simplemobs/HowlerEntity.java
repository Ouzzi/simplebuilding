package com.simplebuilding.modules.simplemobs;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ShriekParticleOption;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.GameEventTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Hollow Howler (Tiefenheuler): summoned Deep Dark boss, a little stronger than the Warden. Blind like the Warden, it
 * hunts what it hears; bites with a skull-and-jaw head (the opening jaws are its weak spot). Special attacks, each
 * announced by sound and particles (Nachtrag 46): ground shockwave up close, echo shriek and charge at range, sculk
 * tendrils under the target. Phases: hunger at 50 % (faster, longer reach, sculk-jaw traps), rage at 20 % (shorter
 * cooldowns). No reinforcements.
 */
public class HowlerEntity extends Monster implements VibrationSystem {
    private static final EntityDataAccessor<Integer> DATA_BITE = SynchedEntityData.defineId(HowlerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_EMERGE = SynchedEntityData.defineId(HowlerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SHRIEK = SynchedEntityData.defineId(HowlerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PHASE = SynchedEntityData.defineId(HowlerEntity.class, EntityDataSerializers.INT);
    /** Special attack winding up or running (ordinal of {@link HowlerLogic.Attack}), for the renderer's telegraph pose. */
    private static final EntityDataAccessor<Integer> DATA_ATTACK = SynchedEntityData.defineId(HowlerEntity.class, EntityDataSerializers.INT);
    private static final Identifier SB_JAW = Identifier.fromNamespaceAndPath("simplebuilding", "sculk_jaw");
    private static final Codec<List<Long>> TRAP_CODEC = Codec.LONG.listOf();

    private final DynamicGameEventListener<VibrationSystem.Listener> dynamicListener;
    private final VibrationSystem.User vibrationUser;
    private VibrationSystem.Data vibrationData;

    private int biteCooldown, shriekCooldown = 60, trapCooldown = HowlerLogic.TRAP_INTERVAL_TICKS, trailTicks, trailPlaced;
    private int vibrationCooldown, sinceHeard, idleTicks;
    private int shockwaveCooldown = 40, chargeCooldown = 100, tendrilCooldown = 80, patternGap;
    /** Ticks left in the running special attack (wind-up, then the charge run) or the stagger after a wall. */
    private int attackTicks, staggerTicks;
    private Vec3 chargeDir = Vec3.ZERO;
    private @Nullable BlockPos disturbance;
    /** Placed trap jaws: position and game time when they vanish again (saved, so they never stay behind). */
    private final List<Long> trapPos = new ArrayList<>(), trapUntil = new ArrayList<>();
    /** Vanilla fallback traps (no Simple Building): position and game time when they snap. */
    private final List<Long> snarePos = new ArrayList<>(), snareAt = new ArrayList<>();

    public HowlerEntity(EntityType<? extends HowlerEntity> type, Level level) {
        super(type, level);
        this.xpReward = HowlerLogic.XP;
        this.vibrationUser = new HowlerVibrationUser();
        this.vibrationData = new VibrationSystem.Data();
        this.dynamicListener = new DynamicGameEventListener<>(new VibrationSystem.Listener(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HowlerLogic.MAX_HEALTH)
                .add(Attributes.ARMOR, HowlerLogic.ARMOR)
                .add(Attributes.ATTACK_DAMAGE, HowlerLogic.ATTACK)
                .add(Attributes.MOVEMENT_SPEED, HowlerLogic.SPEED)
                .add(Attributes.KNOCKBACK_RESISTANCE, HowlerLogic.KNOCKBACK_RESISTANCE)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new HuntGoal());
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BITE, 0);
        builder.define(DATA_EMERGE, 0);
        builder.define(DATA_SHRIEK, 0);
        builder.define(DATA_PHASE, 0);
        builder.define(DATA_ATTACK, 0);
    }

    // ---- state for renderer and tests ---------------------------------------------------

    /** Ticks left until the jaws snap shut (0: no bite running). */
    public int biteTicks() { return this.entityData.get(DATA_BITE); }
    public int emergeTicks() { return this.entityData.get(DATA_EMERGE); }
    public int shriekTicks() { return this.entityData.get(DATA_SHRIEK); }
    public HowlerLogic.Phase phase() { return HowlerLogic.Phase.values()[Math.max(0, Math.min(2, this.entityData.get(DATA_PHASE)))]; }
    public boolean jawsOpening() { return biteTicks() > 0; }
    public boolean emerging() { return emergeTicks() > 0; }
    /** The special attack winding up or running (NONE otherwise). */
    public HowlerLogic.Attack attack() { return HowlerLogic.Attack.values()[Math.max(0, Math.min(HowlerLogic.Attack.values().length - 1, this.entityData.get(DATA_ATTACK)))]; }
    public int attackTicks() { return attackTicks; }
    public boolean staggered() { return staggerTicks > 0; }

    /** Client: ticks left of the antenna twitch after a heard vibration (entity event 61, like the Warden). */
    private int tendrilTicks, tendrilTicksO;

    public float tendrilAnimation(float partialTicks) { return net.minecraft.util.Mth.lerp(partialTicks, tendrilTicksO, tendrilTicks) / 10.0f; }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 61) tendrilTicks = 10;
        else super.handleEntityEvent(id);
    }

    public void startEmerging() { this.entityData.set(DATA_EMERGE, HowlerLogic.EMERGE_TICKS); }

    /** Test/screenshot hook: open the jaws for a bite (no damage unless a target is in reach when they snap). */
    public void startBite() { this.entityData.set(DATA_BITE, HowlerLogic.BITE_WINDUP_TICKS); }

    /** Screenshot hook (with no AI): jaws wide open just before the snap. */
    public void holdJawsOpen() { this.entityData.set(DATA_BITE, 1); }

    // ---- save ---------------------------------------------------------------------------

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.store("listener", VibrationSystem.Data.CODEC, this.vibrationData);
        out.putInt("TrailPlaced", trailPlaced);
        out.putInt("Emerge", emergeTicks());
        out.store("TrapPos", TRAP_CODEC, List.copyOf(trapPos));
        out.store("TrapUntil", TRAP_CODEC, List.copyOf(trapUntil));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        this.vibrationData = in.read("listener", VibrationSystem.Data.CODEC).orElseGet(VibrationSystem.Data::new);
        this.trailPlaced = in.getIntOr("TrailPlaced", 0);
        this.entityData.set(DATA_EMERGE, in.getIntOr("Emerge", 0));
        List<Long> pos = in.read("TrapPos", TRAP_CODEC).orElse(List.of()), until = in.read("TrapUntil", TRAP_CODEC).orElse(List.of());
        trapPos.clear();
        trapUntil.clear();
        for (int i = 0; i < Math.min(pos.size(), until.size()); i++) { trapPos.add(pos.get(i)); trapUntil.add(until.get(i)); }
    }

    // ---- sounds -------------------------------------------------------------------------

    @Override protected SoundEvent getAmbientSound() { return emerging() ? null : SoundEvents.WARDEN_LISTENING; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.SKELETON_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.WARDEN_DEATH; }
    @Override public float getVoicePitch() { return 0.7f + this.random.nextFloat() * 0.1f; }
    @Override protected void playStepSound(BlockPos pos, BlockState state) { this.playSound(SoundEvents.WARDEN_STEP, 0.8f, 1.15f); }

    // ---- rules --------------------------------------------------------------------------

    /** Silent: its own steps never trip sculk sensors, Wardens or other Howlers. */
    @Override public boolean dampensVibrations() { return true; }
    @Override public boolean removeWhenFarAway(double distSqr) { return false; }
    @Override protected boolean isImmobile() { return super.isImmobile() || emerging(); }
    @Override public boolean canBeAffected(MobEffectInstance effect) { return !effect.is(MobEffects.DARKNESS) && super.canBeAffected(effect); }

    /** Only players are its prey (Sculk creatures and other mobs are left alone). */
    public boolean canHunt(@Nullable Entity e) {
        return e instanceof Player p && e.isAlive() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(p) && this.level().getWorldBorder().isWithinBounds(p.getBoundingBox());
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (emerging() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        boolean weak = jawsOpening();
        boolean hurt = super.hurtServer(level, source, HowlerLogic.damageTaken(damage, weak));
        if (hurt && weak) level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, this.getX(), this.getY() + 2.1, this.getZ(), 8, 0.3, 0.2, 0.3, 0.05);
        if (hurt && canHunt(source.getEntity())) hear((Player) source.getEntity());
        return hurt;
    }

    private void hear(Player p) {
        this.setTarget(p);
        sinceHeard = 0;
        disturbance = null;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            tendrilTicksO = tendrilTicks;
            if (tendrilTicks > 0) tendrilTicks--;
        }
        if (this.level() instanceof ServerLevel level) {
            VibrationSystem.Ticker.tick(level, this.vibrationData, this.vibrationUser);
            int e = emergeTicks();
            if (e > 0) {
                this.entityData.set(DATA_EMERGE, e - 1);
                if (e % 4 == 0) level.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY() + 0.2, this.getZ(), 4, 0.6, 0.1, 0.6, 0.02);
            }
            if (shriekTicks() > 0) this.entityData.set(DATA_SHRIEK, shriekTicks() - 1);
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        updatePhase();
        tickTraps(level);
        if (emerging()) { this.getNavigation().stop(); return; }
        if (vibrationCooldown > 0) vibrationCooldown--;
        if (biteCooldown > 0) biteCooldown--;
        if (shriekCooldown > 0) shriekCooldown--;
        if (trapCooldown > 0) trapCooldown--;
        if (shockwaveCooldown > 0) shockwaveCooldown--;
        if (chargeCooldown > 0) chargeCooldown--;
        if (tendrilCooldown > 0) tendrilCooldown--;
        if (patternGap > 0) patternGap--;
        tickTrail(level);
        if (staggerTicks > 0) {
            staggerTicks--;
            this.getNavigation().stop();
            if (staggerTicks % 5 == 0) level.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY() + 2.6, this.getZ(), 2, 0.3, 0.1, 0.3, 0.01);
            return;
        }

        LivingEntity target = this.getTarget();
        if (target != null && (!canHunt(target) || ++sinceHeard > HowlerLogic.FORGET_TICKS)) {
            // Blind: without hearing it again it loses the trail.
            this.setTarget(null);
            disturbance = target.blockPosition();
            target = null;
        }
        if (target == null) {
            if (attack() != HowlerLogic.Attack.NONE) endAttack();
            boolean playerNear = level.getNearestPlayer(this, 64.0) != null;
            idleTicks = playerNear ? 0 : idleTicks + 1;
            if (idleTicks > MobsConfig.howlerDespawnMinutes * 1200) this.discard();
            return;
        }
        idleTicks = 0;
        HowlerLogic.Phase phase = phase();
        int bite = biteTicks();
        if (bite > 0) {
            this.entityData.set(DATA_BITE, bite - 1);
            if (bite == 1) snap(level, target);
            return;
        }
        if (attack() != HowlerLogic.Attack.NONE) {
            tickAttack(level, phase);
            return;
        }
        double dist = this.distanceTo(target);
        switch (HowlerLogic.choose(dist, reach(target), this.hasLineOfSight(target), patternGap, biteCooldown, shockwaveCooldown, shriekCooldown, chargeCooldown, tendrilCooldown)) {
            case BITE -> {
                startBite();
                this.playSound(SoundEvents.WARDEN_ROAR, 0.6f, 1.6f);
            }
            case SHOCKWAVE -> startShockwave(level);
            case SHRIEK -> startShriek(level);
            case CHARGE -> startCharge(level, target.position().subtract(this.position()));
            case TENDRILS -> {
                tendrilCooldown = HowlerLogic.cooldown(HowlerLogic.TENDRIL_COOLDOWN_TICKS, phase);
                patternGap = HowlerLogic.PATTERN_GAP_TICKS;
                spitTendrils(level, target);
            }
            case NONE -> {
                if (phase != HowlerLogic.Phase.ONE && MobsConfig.howlerHunger && trapCooldown == 0 && patternGap == 0) {
                    trapCooldown = HowlerLogic.TRAP_INTERVAL_TICKS;
                    spitTraps(level, target);
                }
            }
        }
    }

    // ---- special attacks: every one is announced by sound and particles before it lands ---

    private void setAttack(HowlerLogic.Attack attack, int ticks) {
        this.entityData.set(DATA_ATTACK, attack.ordinal());
        attackTicks = ticks;
    }

    private void endAttack() {
        setAttack(HowlerLogic.Attack.NONE, 0);
        patternGap = HowlerLogic.PATTERN_GAP_TICKS;
    }

    /** Ground shockwave wind-up: Warden charge sound and a particle ring that marks the radius. */
    public void startShockwave(ServerLevel level) {
        shockwaveCooldown = HowlerLogic.cooldown(HowlerLogic.SHOCKWAVE_COOLDOWN_TICKS, phase());
        setAttack(HowlerLogic.Attack.SHOCKWAVE, HowlerLogic.SHOCKWAVE_WINDUP_TICKS);
        this.getNavigation().stop();
        this.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 2.0f, 0.7f);
        ring(level, ParticleTypes.SCULK_CHARGE_POP, HowlerLogic.SHOCKWAVE_RADIUS, 28);
    }

    /** Echo shriek wind-up: head thrown back, shrieker sound; it lands at the end if you are still in sight. */
    public void startShriek(ServerLevel level) {
        shriekCooldown = HowlerLogic.shriekCooldown(phase());
        setAttack(HowlerLogic.Attack.SHRIEK, HowlerLogic.SHRIEK_WINDUP_TICKS);
        this.entityData.set(DATA_SHRIEK, HowlerLogic.SHRIEK_WINDUP_TICKS + 10);
        this.getNavigation().stop();
        this.playSound(SoundEvents.SCULK_SHRIEKER_SHRIEK, 2.0f, 0.5f);
        level.sendParticles(new ShriekParticleOption(0), this.getX(), this.getY() + 2.4, this.getZ(), 1, 0, 0, 0, 0);
    }

    /** Charge wind-up: crouches facing a direction locked now; the run follows that line, so a sidestep dodges it. */
    public void startCharge(ServerLevel level, Vec3 toward) {
        Vec3 flat = new Vec3(toward.x, 0, toward.z);
        if (flat.lengthSqr() < 1.0e-4) return;
        chargeCooldown = HowlerLogic.cooldown(HowlerLogic.CHARGE_COOLDOWN_TICKS, phase());
        chargeDir = flat.normalize();
        setAttack(HowlerLogic.Attack.CHARGE, HowlerLogic.CHARGE_WINDUP_TICKS + HowlerLogic.CHARGE_RUN_TICKS);
        this.getNavigation().stop();
        this.setYRot((float) (Math.atan2(chargeDir.z, chargeDir.x) * 180.0 / Math.PI) - 90.0f);
        this.yBodyRot = this.getYRot();
        this.playSound(SoundEvents.RAVAGER_ROAR, 1.5f, 0.6f);
    }

    public Vec3 chargeDirection() { return chargeDir; }

    private void tickAttack(ServerLevel level, HowlerLogic.Phase phase) {
        HowlerLogic.Attack attack = attack();
        int t = --attackTicks;
        this.getNavigation().stop();
        switch (attack) {
            case SHOCKWAVE -> {
                if (t % 5 == 0) ring(level, ParticleTypes.SCULK_CHARGE_POP, HowlerLogic.SHOCKWAVE_RADIUS, 20);
                if (t <= 0) { shockwave(level); endAttack(); }
            }
            case SHRIEK -> {
                if (t <= 0) { shriek(level, phase); endAttack(); }
            }
            case CHARGE -> {
                if (t > HowlerLogic.CHARGE_RUN_TICKS) {
                    // telegraph: dust kicked up along the locked line
                    if (t % 4 == 0) for (int i = 1; i <= 4; i++) {
                        Vec3 p = this.position().add(chargeDir.scale(i * 1.5));
                        level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, p.x, p.y + 0.1, p.z, 2, 0.2, 0.02, 0.2, 0.01);
                    }
                    return;
                }
                if (t == HowlerLogic.CHARGE_RUN_TICKS) this.playSound(SoundEvents.WARDEN_ROAR, 1.5f, 1.2f);
                if (t <= 0 || chargeStep(level)) endAttack();
            }
            default -> endAttack();
        }
    }

    /** One tick of the charge run; true when it ends (hit a player or a wall). */
    private boolean chargeStep(ServerLevel level) {
        this.setDeltaMovement(chargeDir.x * HowlerLogic.CHARGE_SPEED, this.getDeltaMovement().y, chargeDir.z * HowlerLogic.CHARGE_SPEED);
        this.setYRot((float) (Math.atan2(chargeDir.z, chargeDir.x) * 180.0 / Math.PI) - 90.0f);
        this.yBodyRot = this.getYRot();
        for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(0.4).expandTowards(chargeDir.scale(HowlerLogic.CHARGE_SPEED)), this::canHunt)) {
            p.hurtServer(level, this.damageSources().mobAttack(this), HowlerLogic.CHARGE_DAMAGE);
            p.push(chargeDir.x * 1.2, 0.45, chargeDir.z * 1.2);
            this.playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 1.5f, 0.8f);
            return true;
        }
        if (this.horizontalCollision) {
            staggerTicks = HowlerLogic.STAGGER_TICKS;
            this.playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 1.5f, 0.5f);
            level.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY() + 2.6, this.getZ(), 8, 0.4, 0.2, 0.4, 0.02);
            return true;
        }
        return false;
    }

    /** The shockwave lands: players on the ground within the radius take damage and are thrown out. Jumping dodges it. */
    public int shockwave(ServerLevel level) {
        this.playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 2.0f, 0.6f);
        for (int r = 1; r <= (int) HowlerLogic.SHOCKWAVE_RADIUS; r++) ring(level, ParticleTypes.SCULK_SOUL, r, 6 + r * 4);
        int hit = 0;
        for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(HowlerLogic.SHOCKWAVE_RADIUS, 2, HowlerLogic.SHOCKWAVE_RADIUS), this::canHunt)) {
            Vec3 away = p.position().subtract(this.position()).multiply(1, 0, 1);
            if (!HowlerLogic.shockwaveHits(away.length(), p.getY() - this.getY(), p.onGround())) continue;
            if (p.hurtServer(level, this.damageSources().mobAttack(this), HowlerLogic.SHOCKWAVE_DAMAGE)) hit++;
            away = away.lengthSqr() > 1.0e-4 ? away.normalize() : Vec3.ZERO;
            p.push(away.x * 0.9, 0.5, away.z * 0.9);
        }
        return hit;
    }

    private void ring(ServerLevel level, net.minecraft.core.particles.ParticleOptions particle, double radius, int points) {
        for (int i = 0; i < points; i++) {
            double a = i * Math.PI * 2 / points;
            level.sendParticles(particle, this.getX() + Math.cos(a) * radius, this.getY() + 0.15, this.getZ() + Math.sin(a) * radius, 1, 0, 0.05, 0, 0.01);
        }
    }

    private double reach(LivingEntity target) {
        return this.getBbWidth() / 2 + target.getBbWidth() / 2 + 1.4 + HowlerLogic.extraReach(phase());
    }

    private void updatePhase() {
        HowlerLogic.Phase phase = HowlerLogic.phase(this.getHealth(), this.getMaxHealth());
        if (phase.ordinal() == this.entityData.get(DATA_PHASE)) return;
        this.entityData.set(DATA_PHASE, phase.ordinal());
        var speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(HowlerLogic.SPEED * HowlerLogic.speedFactor(phase));
        if (phase != HowlerLogic.Phase.ONE) this.playSound(SoundEvents.WARDEN_ROAR, 2.0f, phase == HowlerLogic.Phase.RAGE ? 0.6f : 0.8f);
    }

    /** The jaws snap shut: bite if still in reach (14 damage, Hard 21 via difficulty scaling, Darkness 4 s). */
    private void snap(ServerLevel level, LivingEntity target) {
        biteCooldown = HowlerLogic.BITE_COOLDOWN_TICKS;
        this.playSound(SoundEvents.EVOKER_FANGS_ATTACK, 1.5f, 0.6f);
        if (this.distanceTo(target) <= reach(target) + 0.5 && this.doHurtTarget(level, target)) {
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, HowlerLogic.DARKNESS_TICKS, 0), this);
        }
    }

    /** Echo shriek lands: every player within 10 blocks and in its line of sight takes 8 damage and is pushed away. Calls no help. */
    public int shriek(ServerLevel level, HowlerLogic.Phase phase) {
        shriekCooldown = Math.max(shriekCooldown, HowlerLogic.shriekCooldown(phase) - HowlerLogic.SHRIEK_WINDUP_TICKS);
        this.entityData.set(DATA_SHRIEK, 10);
        this.playSound(SoundEvents.SCULK_SHRIEKER_SHRIEK, 3.0f, 0.6f);
        for (int i = 0; i < 3; i++) level.sendParticles(new ShriekParticleOption(i * 5), this.getX(), this.getY() + 2.4, this.getZ(), 1, 0, 0, 0, 0);
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI * 2 / 24;
            level.sendParticles(ParticleTypes.SCULK_SOUL, this.getX() + Math.cos(a) * HowlerLogic.SHRIEK_RADIUS, this.getY() + 0.3, this.getZ() + Math.sin(a) * HowlerLogic.SHRIEK_RADIUS, 1, 0, 0.1, 0, 0.01);
        }
        int hit = 0;
        for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(HowlerLogic.SHRIEK_RADIUS), this::canHunt)) {
            if (p.distanceTo(this) > HowlerLogic.SHRIEK_RADIUS || !this.hasLineOfSight(p)) continue;
            if (p.hurtServer(level, this.damageSources().mobAttack(this), HowlerLogic.SHRIEK_DAMAGE)) hit++;
            Vec3 away = p.position().subtract(this.position()).multiply(1, 0, 1);
            if (away.lengthSqr() > 1.0e-4) away = away.normalize();
            p.push(away.x, 0.35, away.z);
        }
        return hit;
    }

    // ---- sculk trail and phase-two traps ------------------------------------------------

    private boolean griefing(ServerLevel level) { return level.getGameRules().get(GameRules.MOB_GRIEFING); }

    private void tickTrail(ServerLevel level) {
        if (!MobsConfig.howlerSculkTrail || trailPlaced >= MobsConfig.howlerSculkTrailMax || !this.onGround()) return;
        if (++trailTicks < HowlerLogic.TRAIL_INTERVAL_TICKS) return;
        trailTicks = 0;
        if (!griefing(level)) return;
        BlockPos below = this.blockPosition().below();
        if (level.getBlockState(below).is(HowlerPattern.REPLACEABLE)) {
            level.setBlock(below, Blocks.SCULK.defaultBlockState(), 3);
            trailPlaced++;
        }
    }

    /** Spits three traps around the target: Simple Building's Sculk Jaws (20 s), else Vanilla sculk snares. */
    public int spitTraps(ServerLevel level, LivingEntity target) {
        Block jaw = BuiltInRegistries.BLOCK.getOptional(SB_JAW).orElse(null);
        boolean blocks = jaw != null && griefing(level);
        int placed = 0;
        for (int i = 0; i < HowlerLogic.TRAP_COUNT; i++) {
            double a = this.random.nextDouble() * Math.PI * 2, r = 1.5 + this.random.nextDouble() * 1.5;
            BlockPos spot = BlockPos.containing(target.getX() + Math.cos(a) * r, target.getY(), target.getZ() + Math.sin(a) * r);
            if (!level.getBlockState(spot).isAir() || !level.getBlockState(spot.below()).isFaceSturdy(level, spot.below(), Direction.UP)) continue;
            Vec3 c = Vec3.atBottomCenterOf(spot);
            level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, c.x, c.y + 0.1, c.z, 10, 0.3, 0.05, 0.3, 0.02);
            if (blocks) {
                level.setBlock(spot, jaw.defaultBlockState(), 3);
                trapPos.add(spot.asLong());
                trapUntil.add(level.getGameTime() + HowlerLogic.TRAP_LIFETIME_TICKS);
            } else {
                addTendril(level, spot);
            }
            placed++;
        }
        if (placed > 0) this.playSound(SoundEvents.SCULK_BLOCK_SPREAD, 1.5f, 0.6f);
        return placed;
    }

    /** Sculk tendrils: one mark under the target and the rest around it; they pulse 1.5 s, then erupt. */
    public int spitTendrils(ServerLevel level, LivingEntity target) {
        int placed = 0;
        for (int i = 0; i < HowlerLogic.TENDRIL_COUNT; i++) {
            double a = this.random.nextDouble() * Math.PI * 2, r = i == 0 ? 0 : 2.0 + this.random.nextDouble() * 1.5;
            BlockPos spot = BlockPos.containing(target.getX() + Math.cos(a) * r, target.getY() + 0.2, target.getZ() + Math.sin(a) * r);
            if (!level.getBlockState(spot).getCollisionShape(level, spot).isEmpty()) continue;
            addTendril(level, spot);
            placed++;
        }
        if (placed > 0) this.playSound(SoundEvents.SCULK_CATALYST_BLOOM, 2.0f, 0.6f);
        return placed;
    }

    private void addTendril(ServerLevel level, BlockPos spot) {
        Vec3 c = Vec3.atBottomCenterOf(spot);
        level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, c.x, c.y + 0.1, c.z, 10, 0.3, 0.05, 0.3, 0.02);
        snarePos.add(spot.asLong());
        snareAt.add(level.getGameTime() + HowlerLogic.TENDRIL_WINDUP_TICKS);
    }

    /** Test hook: tendrils still waiting to erupt. */
    public int pendingTendrils() { return snarePos.size(); }

    private void tickTraps(ServerLevel level) {
        long now = level.getGameTime();
        for (int i = trapPos.size() - 1; i >= 0; i--) {
            if (now < trapUntil.get(i) && this.isAlive()) continue;
            removeTrap(level, BlockPos.of(trapPos.get(i)));
            trapPos.remove(i);
            trapUntil.remove(i);
        }
        for (int i = snarePos.size() - 1; i >= 0; i--) {
            Vec3 c = Vec3.atBottomCenterOf(BlockPos.of(snarePos.get(i)));
            if (now < snareAt.get(i)) {
                // telegraph: the mark pulses until it erupts
                if ((snareAt.get(i) - now) % 5 == 0) level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, c.x, c.y + 0.1, c.z, 4, 0.35, 0.02, 0.35, 0.01);
                continue;
            }
            eruptTendril(level, c);
            snarePos.remove(i);
            snareAt.remove(i);
        }
    }

    /** A tendril erupts: players standing on the mark take damage and get Slowness II. Returns how many were caught. */
    public int eruptTendril(ServerLevel level, Vec3 c) {
        level.sendParticles(ParticleTypes.SCULK_SOUL, c.x, c.y + 0.2, c.z, 8, 0.3, 0.4, 0.3, 0.02);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.SCULK_BLOCK_BREAK, SoundSource.HOSTILE, 1.0f, 0.6f);
        int caught = 0;
        double r = HowlerLogic.TENDRIL_RADIUS;
        for (Player p : level.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(c, c).inflate(r, 1.5, r), this::canHunt)) {
            if (p.position().multiply(1, 0, 1).distanceTo(c.multiply(1, 0, 1)) > r) continue;
            p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, HowlerLogic.TENDRIL_SLOW_TICKS, 1), this);
            p.hurtServer(level, this.damageSources().mobAttack(this), HowlerLogic.TENDRIL_DAMAGE);
            caught++;
        }
        return caught;
    }

    private void removeTrap(ServerLevel level, BlockPos pos) {
        Block jaw = BuiltInRegistries.BLOCK.getOptional(SB_JAW).orElse(null);
        if (jaw != null && level.getBlockState(pos).is(jaw)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    @Override
    public void die(DamageSource source) {
        if (this.level() instanceof ServerLevel level) {
            for (long p : trapPos) removeTrap(level, BlockPos.of(p));
            trapPos.clear();
            trapUntil.clear();
        }
        super.die(source);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason.shouldDestroy() && this.level() instanceof ServerLevel level) for (long p : trapPos) removeTrap(level, BlockPos.of(p));
        super.remove(reason);
    }

    /** Without Simple Building there is no Sculk Jaw to drop; one more echo shard instead. */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        if (!BuiltInRegistries.ITEM.containsKey(SB_JAW)) this.spawnAtLocation(level, new ItemStack(Items.ECHO_SHARD));
    }

    // ---- hearing ------------------------------------------------------------------------

    @Override
    public void updateDynamicGameEventListener(BiConsumer<DynamicGameEventListener<?>, ServerLevel> action) {
        if (this.level() instanceof ServerLevel level) action.accept(this.dynamicListener, level);
    }

    @Override public VibrationSystem.Data getVibrationData() { return this.vibrationData; }
    @Override public VibrationSystem.User getVibrationUser() { return this.vibrationUser; }

    /** Test hook: what the Howler does when it hears something at {@code pos}. */
    public void onHeard(BlockPos pos, @Nullable Entity source, @Nullable Entity projectileOwner) {
        vibrationCooldown = 20;
        if (this.level() instanceof ServerLevel level) level.broadcastEntityEvent(this, (byte) 61);
        this.playSound(SoundEvents.WARDEN_TENDRIL_CLICKS, 3.0f, 0.8f);
        if (projectileOwner == null && canHunt(source)) {
            hear((Player) source);
        } else if (this.getTarget() == null) {
            disturbance = pos.immutable();
        }
    }

    public @Nullable BlockPos disturbance() { return disturbance; }

    private class HowlerVibrationUser implements VibrationSystem.User {
        private final PositionSource positionSource = new EntityPositionSource(HowlerEntity.this, HowlerEntity.this.getEyeHeight());

        @Override public int getListenerRadius() { return HowlerLogic.HEARING_RADIUS; }
        @Override public PositionSource getPositionSource() { return positionSource; }
        @Override public TagKey<GameEvent> getListenableEvents() { return GameEventTags.WARDEN_CAN_LISTEN; }

        @Override
        public boolean canReceiveVibration(ServerLevel level, BlockPos pos, Holder<GameEvent> event, GameEvent.Context context) {
            if (HowlerEntity.this.isNoAi() || HowlerEntity.this.isDeadOrDying() || emerging() || vibrationCooldown > 0) return false;
            if (!level.getWorldBorder().isWithinBounds(pos)) return false;
            Entity src = context.sourceEntity();
            return !(src instanceof HowlerEntity) && !(src instanceof net.minecraft.world.entity.monster.warden.Warden);
        }

        @Override
        public void onReceiveVibration(ServerLevel level, BlockPos pos, Holder<GameEvent> event, @Nullable Entity sourceEntity, @Nullable Entity projectileOwner, float distance) {
            if (!HowlerEntity.this.isDeadOrDying()) onHeard(pos, sourceEntity, projectileOwner);
        }
    }

    /** Walks to its target, or to the last thing it heard; stops next to the target (the bite is in the AI step). */
    private class HuntGoal extends Goal {
        private int repath;

        HuntGoal() { this.setFlags(java.util.EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK)); }

        @Override public boolean canUse() { return !emerging() && attack() == HowlerLogic.Attack.NONE && staggerTicks == 0 && (getTarget() != null || disturbance != null); }
        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public void start() { repath = 0; }
        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target != null) {
                getLookControl().setLookAt(target, 30f, 30f);
                if (--repath <= 0) {
                    repath = 10;
                    if (distanceTo(target) > reach(target) - 0.3) getNavigation().moveTo(target, 1.0);
                    else getNavigation().stop();
                }
                return;
            }
            BlockPos d = disturbance;
            if (d == null) return;
            if (--repath <= 0) {
                repath = 20;
                if (blockPosition().closerThan(d, 2.0) || !getNavigation().moveTo(d.getX() + 0.5, d.getY(), d.getZ() + 0.5, 0.9)) disturbance = null;
            }
        }
    }
}

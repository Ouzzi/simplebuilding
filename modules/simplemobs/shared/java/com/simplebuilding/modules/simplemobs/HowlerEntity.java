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
 * Hollow Howler (Tiefenheuler): summoned Deep Dark mid-boss. Blind like the Warden, it hunts what it hears; bites with
 * a skull-and-jaw head (the opening jaws are its weak spot) and shrieks at targets out of reach. Phases: hunger at
 * 50 % (faster, longer reach, sculk-jaw traps), rage at 20 % (shrieks twice as often, no knockback). No reinforcements.
 */
public class HowlerEntity extends Monster implements VibrationSystem {
    private static final EntityDataAccessor<Integer> DATA_BITE = SynchedEntityData.defineId(HowlerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_EMERGE = SynchedEntityData.defineId(HowlerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SHRIEK = SynchedEntityData.defineId(HowlerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PHASE = SynchedEntityData.defineId(HowlerEntity.class, EntityDataSerializers.INT);
    private static final Identifier SB_JAW = Identifier.fromNamespaceAndPath("simplebuilding", "sculk_jaw");
    private static final Codec<List<Long>> TRAP_CODEC = Codec.LONG.listOf();

    private final DynamicGameEventListener<VibrationSystem.Listener> dynamicListener;
    private final VibrationSystem.User vibrationUser;
    private VibrationSystem.Data vibrationData;

    private int biteCooldown, shriekCooldown = 60, trapCooldown = HowlerLogic.TRAP_INTERVAL_TICKS, trailTicks, trailPlaced;
    private int vibrationCooldown, sinceHeard, idleTicks;
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
    }

    // ---- state for renderer and tests ---------------------------------------------------

    /** Ticks left until the jaws snap shut (0: no bite running). */
    public int biteTicks() { return this.entityData.get(DATA_BITE); }
    public int emergeTicks() { return this.entityData.get(DATA_EMERGE); }
    public int shriekTicks() { return this.entityData.get(DATA_SHRIEK); }
    public HowlerLogic.Phase phase() { return HowlerLogic.Phase.values()[Math.max(0, Math.min(2, this.entityData.get(DATA_PHASE)))]; }
    public boolean jawsOpening() { return biteTicks() > 0; }
    public boolean emerging() { return emergeTicks() > 0; }

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
        tickTrail(level);

        LivingEntity target = this.getTarget();
        if (target != null && (!canHunt(target) || ++sinceHeard > HowlerLogic.FORGET_TICKS)) {
            // Blind: without hearing it again it loses the trail.
            this.setTarget(null);
            disturbance = target.blockPosition();
            target = null;
        }
        if (target == null) {
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
        double dist = this.distanceTo(target);
        if (biteCooldown == 0 && dist <= reach(target)) {
            startBite();
            this.playSound(SoundEvents.WARDEN_ROAR, 0.6f, 1.6f);
        } else if (shriekCooldown == 0 && dist > HowlerLogic.SHRIEK_MIN_DISTANCE && dist <= HowlerLogic.SHRIEK_RADIUS + 0.5) {
            shriek(level, phase);
        } else if (phase != HowlerLogic.Phase.ONE && MobsConfig.howlerHunger && trapCooldown == 0) {
            trapCooldown = HowlerLogic.TRAP_INTERVAL_TICKS;
            spitTraps(level, target);
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
        var kb = this.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kb != null) kb.setBaseValue(HowlerLogic.knockbackResistance(phase));
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

    /** Echo shriek: every player within 10 blocks takes 8 damage and is pushed away. Calls no help. */
    public int shriek(ServerLevel level, HowlerLogic.Phase phase) {
        shriekCooldown = HowlerLogic.shriekCooldown(phase);
        this.entityData.set(DATA_SHRIEK, 20);
        this.playSound(SoundEvents.SCULK_SHRIEKER_SHRIEK, 3.0f, 0.6f);
        for (int i = 0; i < 3; i++) level.sendParticles(new ShriekParticleOption(i * 5), this.getX(), this.getY() + 2.4, this.getZ(), 1, 0, 0, 0, 0);
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI * 2 / 24;
            level.sendParticles(ParticleTypes.SCULK_SOUL, this.getX() + Math.cos(a) * HowlerLogic.SHRIEK_RADIUS, this.getY() + 0.3, this.getZ() + Math.sin(a) * HowlerLogic.SHRIEK_RADIUS, 1, 0, 0.1, 0, 0.01);
        }
        int hit = 0;
        for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(HowlerLogic.SHRIEK_RADIUS), this::canHunt)) {
            if (p.distanceTo(this) > HowlerLogic.SHRIEK_RADIUS) continue;
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
                snarePos.add(spot.asLong());
                snareAt.add(level.getGameTime() + 20);
            }
            placed++;
        }
        if (placed > 0) this.playSound(SoundEvents.SCULK_BLOCK_SPREAD, 1.5f, 0.6f);
        return placed;
    }

    private void tickTraps(ServerLevel level) {
        long now = level.getGameTime();
        for (int i = trapPos.size() - 1; i >= 0; i--) {
            if (now < trapUntil.get(i) && this.isAlive()) continue;
            removeTrap(level, BlockPos.of(trapPos.get(i)));
            trapPos.remove(i);
            trapUntil.remove(i);
        }
        for (int i = snarePos.size() - 1; i >= 0; i--) {
            if (now < snareAt.get(i)) continue;
            Vec3 c = Vec3.atBottomCenterOf(BlockPos.of(snarePos.get(i)));
            level.sendParticles(ParticleTypes.SCULK_SOUL, c.x, c.y + 0.2, c.z, 6, 0.3, 0.1, 0.3, 0.02);
            for (Player p : level.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(c, c).inflate(1.2, 1.5, 1.2), this::canHunt)) {
                p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1), this);
                p.hurtServer(level, this.damageSources().mobAttack(this), 4.0f);
            }
            snarePos.remove(i);
            snareAt.remove(i);
        }
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
        this.playSound(SoundEvents.SCULK_CLICKING, 1.5f, 0.6f);
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

        @Override public boolean canUse() { return !emerging() && (getTarget() != null || disturbance != null); }
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

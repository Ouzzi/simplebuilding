package com.simplebuilding.modules.simplemobs;

import com.simplebuilding.modules.simplemobs.DeceiverLogic.Phase;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundSource;
import org.jspecify.annotations.Nullable;

/**
 * The Deceiver: small hooded trickster. Never attacks itself; it summons mobs that are mostly
 * harmless fakes (one hit kills them) mixed with a few real ones, and clouds the player's
 * perception with Mirage / Reverse Mirage. Fight rules live in {@link DeceiverLogic}.
 */
public class DeceiverEntity extends PathfinderMob {
    public static final int ACT_NONE = 0, ACT_SUMMON = 1, ACT_TELEPORT = 2, ACT_DRINK = 3, ACT_UNMASK = 4;
    public static final String TAG_SUMMON = "simplemobs_summon";
    public static final String TAG_FAKE = "simplemobs_fake";
    private static final Identifier NO_LOOT = Identifier.fromNamespaceAndPath("simplemobs", "entities/summoned");
    private static final EntityDataAccessor<Integer> DATA_ACTION = SynchedEntityData.defineId(DeceiverEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_ARMORED = SynchedEntityData.defineId(DeceiverEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HOSTILE = SynchedEntityData.defineId(DeceiverEntity.class, EntityDataSerializers.BOOLEAN);

    public final AnimationState idleState = new AnimationState();
    public final AnimationState summonState = new AnimationState();
    public final AnimationState teleportState = new AnimationState();
    public final AnimationState drinkState = new AnimationState();
    public final AnimationState unmaskState = new AnimationState();
    private int clientAction = -1;

    private boolean rolled;
    private int stareTicks, shoves, shoveCooldown, actionTicks, healCooldown, pulseIndex, noTargetTicks;
    private int stageTicks = 100;
    private DeceiverLogic.Stage stage = DeceiverLogic.Stage.OBSERVE;
    private int pendingTeleport = -1;
    private boolean peacefulTheme;
    private final List<UUID> summons = new ArrayList<>();
    private final List<Long> summonBorn = new ArrayList<>();

    public DeceiverEntity(EntityType<? extends DeceiverEntity> type, Level level) {
        super(type, level);
        this.xpReward = 40;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 10.0F, 1.0, 1.35, e -> this.isHostileMode()));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ACTION, ACT_NONE);
        builder.define(DATA_ARMORED, false);
        builder.define(DATA_HOSTILE, false);
    }

    public int getAction() { return this.entityData.get(DATA_ACTION); }
    public boolean isArmored() { return this.entityData.get(DATA_ARMORED); }
    public boolean isHostileMode() { return this.entityData.get(DATA_HOSTILE); }

    private void setAction(int action, int ticks) {
        this.entityData.set(DATA_ACTION, action);
        this.actionTicks = ticks;
    }

    // ---- save ---------------------------------------------------------------------------

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("Armored", isArmored());
        out.putBoolean("Hostile", isHostileMode());
        out.putBoolean("Rolled", rolled);
        out.putBoolean("PeacefulTheme", peacefulTheme);
        out.putInt("PulseIndex", pulseIndex);
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput in) {
        super.readAdditionalSaveData(in);
        this.entityData.set(DATA_ARMORED, in.getBooleanOr("Armored", false));
        this.entityData.set(DATA_HOSTILE, in.getBooleanOr("Hostile", false));
        this.rolled = in.getBooleanOr("Rolled", false);
        this.peacefulTheme = in.getBooleanOr("PeacefulTheme", false);
        this.pulseIndex = in.getIntOr("PulseIndex", 0);
    }

    // ---- sounds (vanilla sounds combined) -----------------------------------------------

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ILLUSIONER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ILLUSIONER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ILLUSIONER_DEATH; }
    @Override public boolean removeWhenFarAway(double distanceToClosestPlayer) { return !isHostileMode(); }

    // ---- client animation ---------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) tickAnimations();
    }

    private void tickAnimations() {
        int a = getAction();
        if (a != clientAction) {
            clientAction = a;
            summonState.stop(); teleportState.stop(); drinkState.stop(); unmaskState.stop();
            switch (a) {
                case ACT_SUMMON -> summonState.start(this.tickCount);
                case ACT_TELEPORT -> teleportState.start(this.tickCount);
                case ACT_DRINK -> drinkState.start(this.tickCount);
                case ACT_UNMASK -> unmaskState.start(this.tickCount);
                default -> { }
            }
        }
        idleState.startIfStopped(this.tickCount);
    }

    // ---- server behavior ----------------------------------------------------------------

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (!rolled) {
            rolled = true;
            if (this.random.nextFloat() < 0.3f) {
                this.entityData.set(DATA_ARMORED, true);
                this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(120.0);
                this.setHealth(120.0f);
            }
        }
        if (actionTicks > 0 && --actionTicks == 0) setAction(ACT_NONE, 0);
        if (shoveCooldown > 0) shoveCooldown--;
        if (healCooldown > 0) healCooldown--;
        pruneSummons(level);

        if (pendingTeleport >= 0 && --pendingTeleport < 0) {
            Player p = nearestPlayer(level);
            if (p != null && teleportNear(level, p)) setAction(ACT_TELEPORT, 20);
        }

        if (!isHostileMode()) {
            tickPeaceful(level);
        } else {
            tickHostile(level);
        }
    }

    private @Nullable Player nearestPlayer(ServerLevel level) {
        return level.getNearestPlayer(this.getX(), this.getY(), this.getZ(), 32.0, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
    }

    private void tickPeaceful(ServerLevel level) {
        boolean looked = false;
        for (Player p : level.players()) {
            if (p.isSpectator() || p.isCreative() || p.distanceToSqr(this) > 144.0) continue;
            Vec3 view = p.getViewVector(1.0f).normalize();
            Vec3 to = new Vec3(this.getX() - p.getX(), this.getEyeY() - p.getEyeY(), this.getZ() - p.getZ());
            double d = to.length();
            if (d < 1.0e-3) continue;
            if (view.dot(to.scale(1.0 / d)) > 1.0 - 0.03 / d && p.hasLineOfSight(this)) { looked = true; break; }
        }
        stareTicks = DeceiverLogic.stare(stareTicks, looked);
        if (DeceiverLogic.provoked(stareTicks, shoves, false)) becomeHostile();
    }

    @Override
    public void push(Entity entity) {
        super.push(entity);
        if (!this.level().isClientSide() && entity instanceof Player && shoveCooldown <= 0) {
            shoves++;
            shoveCooldown = 20;
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && source.getEntity() instanceof Player) {
            boolean wasHostile = isHostileMode();
            becomeHostile();
            setAction(ACT_UNMASK, 16);
            pendingTeleport = 14;
            if (wasHostile && stage == DeceiverLogic.Stage.OBSERVE) stageTicks = Math.min(stageTicks, 30);
        }
        return hurt;
    }

    private void becomeHostile() {
        if (isHostileMode()) return;
        this.entityData.set(DATA_HOSTILE, true);
        this.setPersistenceRequired();
        stage = DeceiverLogic.Stage.OBSERVE;
        stageTicks = 40;
        this.playSound(SoundEvents.ILLUSIONER_PREPARE_MIRROR, 1.0f, 0.9f);
    }

    private Phase phase() { return DeceiverLogic.phase(this.getHealth(), this.getMaxHealth()); }

    private void tickHostile(ServerLevel level) {
        Player target = nearestPlayer(level);
        if (target == null) {
            if (++noTargetTicks > 400) calmDown();
            return;
        }
        noTargetTicks = 0;
        Phase phase = phase();
        this.getLookControl().setLookAt(target, 30.0f, 30.0f);

        if (actionTicks == 0 && DeceiverLogic.mayHeal(phase, getHealth(), getMaxHealth(), healCooldown)) {
            healCooldown = DeceiverLogic.HEAL_COOLDOWN_TICKS;
            setAction(ACT_DRINK, 24);
            this.playSound(SoundEvents.WITCH_DRINK, 1.0f, 1.0f);
            this.heal(getMaxHealth() * 0.1f);
            this.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 0));
            return;
        }
        if (--stageTicks > 0) {
            if (stage == DeceiverLogic.Stage.TELEGRAPH && stageTicks % 4 == 0) telegraphParticles(level, target);
            return;
        }
        if (stage == DeceiverLogic.Stage.OBSERVE) {
            pulse(level, phase);
            stage = DeceiverLogic.Stage.TELEGRAPH;
            stageTicks = DeceiverLogic.TELEGRAPH_TICKS;
            peacefulTheme = this.random.nextBoolean();
            setAction(ACT_SUMMON, DeceiverLogic.TELEGRAPH_TICKS + 10);
            this.playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 1.0f, 1.2f);
        } else {
            summonWave(level, target, phase);
            teleportNear(level, target);
            setAction(ACT_TELEPORT, 20);
            stage = DeceiverLogic.Stage.OBSERVE;
            stageTicks = DeceiverLogic.observeTicks(phase) + this.random.nextInt(40);
        }
    }

    private void calmDown() {
        this.entityData.set(DATA_HOSTILE, false);
        stareTicks = 0;
        shoves = 0;
        noTargetTicks = 0;
    }

    private void telegraphParticles(ServerLevel level, Player target) {
        for (int i = 0; i < 3; i++) {
            double a = this.random.nextDouble() * Math.PI * 2, r = 5 + this.random.nextDouble() * 4;
            level.sendParticles(ParticleTypes.WITCH, target.getX() + Math.cos(a) * r, target.getY() + 0.2, target.getZ() + Math.sin(a) * r, 2, 0.2, 0.4, 0.2, 0.02);
        }
        level.sendParticles(ParticleTypes.ENCHANT, this.getX(), this.getY() + 1.0, this.getZ(), 6, 0.4, 0.6, 0.4, 0.3);
    }

    /** Applies Mirage and/or Reverse Mirage (from Simple Building, if present) to players around. */
    private void pulse(ServerLevel level, Phase phase) {
        boolean m = DeceiverLogic.pulseMirage(phase, pulseIndex), r = DeceiverLogic.pulseReverse(phase, pulseIndex);
        pulseIndex++;
        for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(24.0), EntitySelector.NO_CREATIVE_OR_SPECTATOR)) {
            if (m) applyEffect(p, "mirage");
            if (r) applyEffect(p, "reverse_mirage");
        }
    }

    private static void applyEffect(Player p, String path) {
        BuiltInRegistries.MOB_EFFECT.get(Identifier.fromNamespaceAndPath("simplebuilding", path)).ifPresent(h -> {
            MobEffectInstance cur = p.getEffect(h);
            if (cur == null || cur.getDuration() < 40) p.addEffect(new MobEffectInstance(h, DeceiverLogic.PULSE_TICKS, 0, false, true, true));
        });
    }

    // ---- summoning ----------------------------------------------------------------------

    private void pruneSummons(ServerLevel level) {
        long now = level.getGameTime();
        for (int i = summons.size() - 1; i >= 0; i--) {
            Entity e = level.getEntity(summons.get(i));
            boolean expired = now - summonBorn.get(i) > DeceiverLogic.FAKE_LIFETIME_TICKS;
            if (e == null || !e.isAlive() || expired) {
                if (e != null && e.isAlive() && e.getTags().contains(TAG_FAKE)) e.discard();
                summons.remove(i);
                summonBorn.remove(i);
            }
        }
    }

    public int aliveSummons() { return summons.size(); }

    /** Test hook: forces the hostile mode and a wave for the current phase; returns the number of living summons. */
    public int summonNow(ServerLevel level, Player target, boolean peaceful) {
        becomeHostile();
        peacefulTheme = peaceful;
        summonWave(level, target, phase());
        return summons.size();
    }

    private void summonWave(ServerLevel level, Player target, Phase phase) {
        int want = DeceiverLogic.allowed(DeceiverLogic.waveSize(phase, this.random), summons.size(), DeceiverLogic.HARD_CAP);
        List<String> kinds = DeceiverLogic.pickKinds(phase, peacefulTheme, this.random);
        int specials = 0;
        for (int i = 0; i < want; i++) {
            Vec3 spot = findSpot(level, target.position(), 5.0, 10.0, null);
            if (spot == null) continue;
            String kind = kinds.get(this.random.nextInt(kinds.size()));
            boolean real = this.random.nextFloat() < DeceiverLogic.realChance(phase);
            if (!peacefulTheme && specials < DeceiverLogic.maxSpecials() && this.random.nextFloat() < DeceiverLogic.specialChance(phase)) {
                String s = DeceiverLogic.pickSpecial(phase, this.random);
                if (s != null) { kind = s; real = true; specials++; }
            }
            if (!DeceiverLogic.fakeSafe(kind)) real = true;
            spawnSummon(level, kind, real, spot, phase, target);
        }
        level.sendParticles(ParticleTypes.WITCH, this.getX(), this.getY() + 0.8, this.getZ(), 30, 0.5, 0.7, 0.5, 0.1);
        this.playSound(SoundEvents.ILLUSIONER_CAST_SPELL, 1.2f, 1.0f);
    }

    private void spawnSummon(ServerLevel level, String kind, boolean real, Vec3 pos, Phase phase, Player target) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.withDefaultNamespace(kind)).orElse(null);
        if (type == null) return;
        CompoundTag tag = new CompoundTag();
        tag.putString("DeathLootTable", NO_LOOT.toString());
        var input = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag);
        Entity e = EntityType.create(type, input, level, EntitySpawnReason.MOB_SUMMONED).orElse(null);
        if (e == null) return;
        e.snapTo(pos.x, pos.y, pos.z, this.random.nextFloat() * 360f, 0f);
        e.addTag(TAG_SUMMON);
        if (e instanceof Mob mob) {
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(pos)), EntitySpawnReason.MOB_SUMMONED, null);
            for (EquipmentSlot s : EquipmentSlot.values()) mob.setDropChance(s, 0.0f);
            if (real) equip(mob, phase); else makeFake(mob);
            if (mob.getType().getCategory() == net.minecraft.world.entity.MobCategory.MONSTER) mob.setTarget(target);
        }
        level.addFreshEntity(e);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y + 0.5, pos.z, 8, 0.3, 0.5, 0.3, 0.05);
        summons.add(e.getUUID());
        summonBorn.add(level.getGameTime());
    }

    private void makeFake(Mob mob) {
        mob.addTag(TAG_FAKE);
        mob.setSilent(true);
        var hp = mob.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(1.0);
        mob.setHealth(1.0f);
        var dmg = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) dmg.setBaseValue(0.0);
        for (EquipmentSlot s : EquipmentSlot.values()) mob.setItemSlot(s, ItemStack.EMPTY);
        mob.setPersistenceRequired();
    }

    private void equip(Mob mob, Phase phase) {
        int tier = DeceiverLogic.tier(phase);
        if (tier >= 1 && mob.getType().getCategory() == net.minecraft.world.entity.MobCategory.MONSTER) {
            boolean dia = tier == 2;
            mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(dia ? Items.DIAMOND_HELMET : Items.IRON_HELMET));
            mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(dia ? Items.DIAMOND_CHESTPLATE : Items.IRON_CHESTPLATE));
            if (dia) {
                mob.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
                mob.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
            }
        }
        if (tier >= 1) mob.addEffect(new MobEffectInstance(MobEffects.SPEED, 2400, 0));
        if (tier == 2) {
            mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 2400, 0));
            mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 2400, 0));
        }
    }

    @Override
    public void die(DamageSource source) {
        if (this.level() instanceof ServerLevel level) {
            for (UUID id : summons) {
                Entity e = level.getEntity(id);
                if (e != null && e.getTags().contains(TAG_FAKE)) e.discard();
            }
            summons.clear();
            summonBorn.clear();
        }
        super.die(source);
    }

    // ---- placement helpers --------------------------------------------------------------

    /** A free standing spot on (about) the center's height within the ring; optionally in sight of a viewer. */
    private @Nullable Vec3 findSpot(ServerLevel level, Vec3 center, double min, double max, @Nullable Player viewer) {
        for (int i = 0; i < 16; i++) {
            double a = this.random.nextDouble() * Math.PI * 2, r = min + this.random.nextDouble() * (max - min);
            double x = center.x + Math.cos(a) * r, z = center.z + Math.sin(a) * r;
            for (int dy = 2; dy >= -2; dy--) {
                BlockPos feet = BlockPos.containing(x, center.y + dy, z);
                BlockPos below = feet.below();
                if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) continue;
                if (!level.noCollision(this, this.getBoundingBox().move(feet.getX() + 0.5 - this.getX(), feet.getY() - this.getY(), feet.getZ() + 0.5 - this.getZ()))) continue;
                if (viewer != null) {
                    if (!level.canSeeSky(feet)) continue;
                    Vec3 eye = new Vec3(feet.getX() + 0.5, feet.getY() + 0.8, feet.getZ() + 0.5);
                    if (level.clip(new ClipContext(viewer.getEyePosition(), eye, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, viewer)).getType() != HitResult.Type.MISS) continue;
                }
                return new Vec3(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
            }
        }
        return null;
    }

    /** Chorus-fruit style jump on the player's height, in sight (no caves). */
    private boolean teleportNear(ServerLevel level, Player target) {
        Vec3 spot = findSpot(level, target.position(), 8.0, 14.0, target);
        if (spot == null) return false;
        level.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 0.8, this.getZ(), 25, 0.3, 0.6, 0.3, 0.3);
        this.playSound(SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0f, 1.0f);
        this.teleportTo(spot.x, spot.y, spot.z);
        this.getNavigation().stop();
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, spot.x, spot.y + 0.8, spot.z, 25, 0.3, 0.6, 0.3, 0.3);
        level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.0f, 0.8f);
        return true;
    }
}

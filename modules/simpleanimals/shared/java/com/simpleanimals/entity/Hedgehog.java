package com.simpleanimals.entity;

import com.simpleanimals.AnimalVariants;
import com.simpleanimals.AnimalsRegistry;
import com.simpleanimals.AnimalsSpawn;
import com.simpleanimals.config.AnimalsConfig;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Hedgehog (concept WP-18, owner choice ABN29b-3): a small forest and meadow animal.
 * <ul>
 * <li><b>Hunts silverfish</b> within {@code hedgehogHuntRadius} blocks (default 8): it walks to the nearest one and a bite
 * kills it (silverfish in the open; hidden ones in blocks are not entities and stay).</li>
 * <li><b>Rolls up</b> like the armadillo, but far faster: curled within {@link #CURL_TICKS} ticks (0.3 s, the armadillo needs
 * about 1 s), unrolled {@link #UNROLL_DELAY} ticks after the last threat. A threat is a monster (not a silverfish: that is prey), a wolf, a fox, a sprinting
 * player within four blocks, or whoever hurt it last. Curled it stands still, takes half damage (never zero) and
 * pricks a melee attacker at once with a {@link #PRICK_CHANCE} chance for 1 to 4 damage (IG31-17; switch
 * {@code hedgehogThorns}).</li>
 * </ul>
 * Breeds with sweet berries. Vanilla goals only; no block damage.
 */
public class Hedgehog extends Animal {
    /** Client animation: ticks from open to curled (0.3 s). */
    public static final int CURL_TICKS = 6;
    /** Ticks without a threat before it unrolls (2 s). */
    public static final int UNROLL_DELAY = 40;
    /** Chance that a melee hit on the curled hedgehog is paid back, and the damage range (Vanilla Thorns I: 1 to 4). */
    public static final float PRICK_CHANCE = 0.5F;
    public static final int PRICK_MIN = 1, PRICK_MAX = 4;
    private static final EntityDataAccessor<Boolean> CURLED = SynchedEntityData.defineId(Hedgehog.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(Hedgehog.class, EntityDataSerializers.INT);

    private int lastThreat = -1000;
    private int nextScan;
    /** Client: 0 open ... 1 curled, moved by 1 / {@link #CURL_TICKS} per tick. */
    private float curl, curlO;

    public Hedgehog(EntityType<? extends Hedgehog> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new CurledGoal(this));
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.1, this::isFood, false));
        this.goalSelector.addGoal(5, new HuntSilverfishGoal(this));
        this.goalSelector.addGoal(6, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 5.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder data) {
        super.defineSynchedData(data);
        data.define(CURLED, false);
        data.define(VARIANT, 0);
    }

    public boolean isCurled() {
        return this.entityData.get(CURLED);
    }

    /** 0 = brown (standard), 1 desert, 2 white-breasted, 3 four-toed; by biome, see {@link AnimalVariants}. */
    public int variant() {
        return this.entityData.get(VARIANT);
    }

    public void setVariant(int v) {
        this.entityData.set(VARIANT, AnimalVariants.clamp(AnimalVariants.HEDGEHOG, v));
    }

    /** Client: how far it is curled, 0..1, smooth between ticks. */
    public float curlAmount(float partialTicks) {
        return Mth.lerp(partialTicks, curlO, curl);
    }

    public void setCurled(boolean value) {
        if (value == isCurled()) return;
        this.entityData.set(CURLED, value);
        if (value) {
            getNavigation().stop();
            resetLove();
            playSound(SoundEvents.ARMADILLO_ROLL, 1.0F, 1.4F);
        } else {
            playSound(SoundEvents.ARMADILLO_UNROLL_FINISH, 1.0F, 1.4F);
        }
    }

    /** True when something near is a reason to roll up: monsters, wolves, foxes, a sprinting player, the one who hurt it. */
    public boolean threatened() {
        LivingEntity hurtBy = getLastHurtByMob();
        if (hurtBy != null && hurtBy.isAlive() && tickCount - getLastHurtByMobTimestamp() < 100) return true;
        List<LivingEntity> near = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(4.0, 2.0, 4.0), e -> e != this && isThreat(e));
        return !near.isEmpty();
    }

    private static boolean isThreat(LivingEntity e) {
        if (e instanceof Player player) return !player.isSpectator() && !player.isCreative() && player.isSprinting();
        return (e instanceof Monster && !(e instanceof Silverfish)) || e instanceof Wolf || e instanceof Fox;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (tickCount % 5 == 0 && !isBaby() && threatened()) {
            lastThreat = tickCount;
            if (!isCurled() && canCurl()) setCurled(true);
        }
        if (isCurled() && (tickCount - lastThreat > UNROLL_DELAY || !canCurl())) setCurled(false);
    }

    private boolean canCurl() {
        return !isInLiquid() && !isLeashed() && !isPassenger() && !isVehicle();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            curlO = curl;
            curl = Mth.clamp(curl + (isCurled() ? 1.0F : -1.0F) / CURL_TICKS, 0.0F, 1.0F);
        } else if (isCurled()) {
            setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
        }
    }

    /** Thorns-like prick: 0 (no luck) or {@link #PRICK_MIN} to {@link #PRICK_MAX}. */
    public static float prick(net.minecraft.util.RandomSource random) {
        return random.nextFloat() < PRICK_CHANCE ? PRICK_MIN + random.nextInt(PRICK_MAX - PRICK_MIN + 1) : 0.0F;
    }

    /** Curled: half damage, and a melee attacker may be pricked straight away. */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean curled = isCurled();
        if (curled) damage /= 2.0F;
        boolean hurt = super.hurtServer(level, source, damage);
        if (curled && AnimalsConfig.hedgehogThorns && source.getDirectEntity() instanceof LivingEntity attacker && attacker != this
                && source.getEntity() == attacker) {
            float prick = prick(this.random);
            if (prick > 0.0F) attacker.hurtServer(level, damageSources().thorns(this), prick);
        }
        if (hurt && source.getEntity() instanceof LivingEntity && !isBaby() && !isCurled() && canCurl()) {
            lastThreat = tickCount;
            setCurled(true);
        }
        return hurt;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Variant", variant());
        output.putBoolean("Curled", isCurled());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setVariant(input.getIntOr("Variant", 0));
        this.entityData.set(CURLED, input.getBooleanOr("Curled", false));
        lastThreat = tickCount;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData group) {
        setVariant(AnimalVariants.pick(AnimalVariants.HEDGEHOG, level.getBiome(blockPosition()), this.random));
        return super.finalizeSpawn(level, difficulty, reason, group);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.SWEET_BERRIES);
    }

    @Override
    public boolean canFallInLove() {
        return super.canFallInLove() && !isCurled();
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        Hedgehog baby = AnimalsRegistry.HEDGEHOG.create(level, EntitySpawnReason.BREEDING);
        if (baby != null) baby.setVariant(this.random.nextBoolean() || !(partner instanceof Hedgehog other) ? variant() : other.variant());
        return baby;
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        if (AnimalsSpawn.natural(reason)) {
            return AnimalsSpawn.allowed(AnimalsConfig.hedgehog, this.random) && AnimalsSpawn.onFarmGround(level, blockPosition());
        }
        return super.checkSpawnRules(level, reason);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 2;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return isCurled() ? null : SoundEvents.RABBIT_AMBIENT;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return isCurled() ? SoundEvents.ARMADILLO_HURT_REDUCED : SoundEvents.RABBIT_HURT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return SoundEvents.RABBIT_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.ARMADILLO_STEP, 0.1F, 1.3F);
    }

    /** Curled: holds every other goal back (no walking, no looking) until it unrolls. */
    static final class CurledGoal extends Goal {
        private final Hedgehog hedgehog;

        CurledGoal(Hedgehog hedgehog) {
            this.hedgehog = hedgehog;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return hedgehog.isCurled();
        }

        @Override
        public boolean canContinueToUse() {
            return hedgehog.isCurled();
        }

        @Override
        public void start() {
            hedgehog.getNavigation().stop();
        }
    }

    /** Walks to the nearest silverfish in range; one bite kills it. */
    static final class HuntSilverfishGoal extends Goal {
        private final Hedgehog hedgehog;
        private @Nullable Silverfish prey;
        private int repath;

        HuntSilverfishGoal(Hedgehog hedgehog) {
            this.hedgehog = hedgehog;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!AnimalsConfig.hedgehogHunts || hedgehog.isBaby() || hedgehog.isCurled() || hedgehog.tickCount < hedgehog.nextScan) return false;
            hedgehog.nextScan = hedgehog.tickCount + 20;
            double r = AnimalsConfig.hedgehogHuntRadius;
            Silverfish best = null;
            for (Silverfish s : hedgehog.level().getEntitiesOfClass(Silverfish.class, hedgehog.getBoundingBox().inflate(r), Silverfish::isAlive)) {
                if (best == null || hedgehog.distanceToSqr(s) < hedgehog.distanceToSqr(best)) best = s;
            }
            prey = best;
            return prey != null;
        }

        @Override
        public boolean canContinueToUse() {
            return prey != null && prey.isAlive() && !hedgehog.isCurled() && hedgehog.distanceToSqr(prey) <= 4.0 * AnimalsConfig.hedgehogHuntRadius * AnimalsConfig.hedgehogHuntRadius;
        }

        @Override
        public void start() {
            repath = 0;
        }

        @Override
        public void stop() {
            prey = null;
            hedgehog.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (prey == null) return;
            hedgehog.getLookControl().setLookAt(prey, 30.0F, 30.0F);
            if (hedgehog.distanceToSqr(prey) < 1.6 && hedgehog.level() instanceof ServerLevel level) {
                prey.hurtServer(level, hedgehog.damageSources().mobAttack(hedgehog), 100.0F);
                hedgehog.heal(1.0F);
                hedgehog.playSound(SoundEvents.FOX_EAT, 0.8F, 1.5F);
                prey = null;
            } else if (--repath <= 0) {
                repath = 10;
                hedgehog.getNavigation().moveTo(prey, 1.3);
            }
        }
    }
}

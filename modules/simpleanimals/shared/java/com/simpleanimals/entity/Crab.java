package com.simpleanimals.entity;

import com.simpleanimals.AnimalVariants;
import com.simpleanimals.AnimalsSpawn;
import com.simpleanimals.config.AnimalsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Crab (concept WP-18, owner choice ABN29b-3): a small, harmless beach animal, Vanilla-near (turtle-sized shell, two big
 * claws, eyes on stalks) that walks sideways. Rare: low spawn weight, single animals, only on beaches and in mangrove
 * swamps, preferably by day. Drops raw crab (the value of raw cod), which cooks to cooked-cod value.
 * It does not breed (a wild resource like fish, A12: no new farm).
 * <p>Two models (owner choice 31.10.): {@link #MODEL_CRAB} (model A, two pincers, shell colour by biome) and the rarer
 * {@link #MODEL_FIDDLER} (model B, fiddler crab with one big claw). Like a Vanilla animal variant the model is rolled at spawn
 * ({@link #rollModel}, 1 fiddler to 3 crabs), synchronised to the client and stored in the NBT ({@code Model}). Crabs have no
 * bucket and do not breed, so nothing else carries it.
 */
public class Crab extends PathfinderMob {
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(Crab.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MODEL = SynchedEntityData.defineId(Crab.class, EntityDataSerializers.INT);
    public static final int MODEL_CRAB = 0;
    public static final int MODEL_FIDDLER = 1;
    /** One fiddler crab per {@code FIDDLER_ODDS} crabs: 1 in 4, i.e. 1:3. */
    public static final int FIDDLER_ODDS = 4;

    public Crab(EntityType<? extends Crab> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.5));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 5.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder data) {
        super.defineSynchedData(data);
        data.define(VARIANT, 0);
        data.define(MODEL, MODEL_CRAB);
    }

    /** 0 = red shell (standard), 1 = coral red; by biome, see {@link AnimalVariants}. */
    public int variant() {
        return this.entityData.get(VARIANT);
    }

    public void setVariant(int v) {
        this.entityData.set(VARIANT, AnimalVariants.clamp(AnimalVariants.CRAB, v));
    }

    /** {@link #MODEL_CRAB} or {@link #MODEL_FIDDLER}. */
    public int model() {
        return this.entityData.get(MODEL);
    }

    public void setModel(int m) {
        this.entityData.set(MODEL, Math.max(MODEL_CRAB, Math.min(MODEL_FIDDLER, m)));
    }

    /** The spawn roll: a fiddler crab one time in {@link #FIDDLER_ODDS}. */
    public static int rollModel(net.minecraft.util.RandomSource random) {
        return random.nextInt(FIDDLER_ODDS) == 0 ? MODEL_FIDDLER : MODEL_CRAB;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Variant", variant());
        output.putInt("Model", model());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setVariant(input.getIntOr("Variant", 0));
        setModel(input.getIntOr("Model", MODEL_CRAB));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData group) {
        setVariant(AnimalVariants.pick(AnimalVariants.CRAB, level.getBiome(blockPosition()), this.random));
        setModel(rollModel(this.random));
        return super.finalizeSpawn(level, difficulty, reason, group);
    }

    /** Sand, mud or dirt below; daylight needed (so crabs are seen by day only now and then). */
    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        if (AnimalsSpawn.natural(reason)) {
            BlockPos below = blockPosition().below();
            boolean ground = level.getBlockState(below).is(BlockTags.SAND) || level.getBlockState(below).is(BlockTags.DIRT);
            return AnimalsSpawn.allowed(AnimalsConfig.crab, this.random) && ground && AnimalsSpawn.bright(level, blockPosition());
        }
        return super.checkSpawnRules(level, reason);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 1;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.TURTLE_AMBIENT_LAND;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.TURTLE_HURT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return SoundEvents.TURTLE_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.TURTLE_SHAMBLE, 0.08F, 1.4F);
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }
}

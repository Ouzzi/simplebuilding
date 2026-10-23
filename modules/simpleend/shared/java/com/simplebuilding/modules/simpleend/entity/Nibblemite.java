package com.simplebuilding.modules.simpleend.entity;

import com.simplebuilding.modules.simpleend.EndRegistry;
import com.simplebuilding.modules.simpleend.SimpleEnd;
import com.simplebuilding.modules.simpleend.world.EndBiomes;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Nibblemite (owner 2026-10-12, stage 9): a small, passive Endermite kin of Ash Valley and Dawnlands, in small groups.
 * It nibbles End stone the way sheep graze - the block under it becomes Burrowed End Stone, which recovers by itself.
 * Never hurts players; not an Endermite, so Endermen ignore it. Bred and tempted with chorus fruit.
 * Variant 0 = Ash Valley colouring, 1 = Dawnlands colouring (chosen by the spawn biome, inherited by the young).
 *
 * <p>Tamable (B23-M15, owner 2026-10-23): a ripe Drift Fruit tames a wild one with a chance of 1 in {@link #TAME_CHANCE}
 * (hearts, else smoke, like a wolf). A tamed one follows its owner; the owner's right-click (without food) switches between
 * <em>follow</em> and <em>normal</em> (it wanders and nibbles on its own and does not teleport). "Normal" is Vanilla's
 * {@code orderedToSit} flag without a sit goal, so it is saved and keeps every follow/teleport check of
 * {@link TamableAnimal} off. Young of a tamed parent are tamed to the same owner. Tamed ones glow brighter (renderer).
 */
public class Nibblemite extends TamableAnimal {
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(Nibblemite.class, EntityDataSerializers.INT);
    /** Mean ticks between two nibbles of an adult (sheep: 1/1000 per tick); young ones nibble more often. */
    public static final int NIBBLE_CHANCE = 600;

    /** Entity event: the mite starts a nibble (client plays the chew cycle). */
    public static final byte EVENT_CHEW = 10;
    /** Length of the chew cycle in ticks (the NibbleGoal runs 30). */
    public static final int CHEW_TICKS = 30;
    /** A Drift Fruit tames a wild Nibblemite with a chance of 1 in this. */
    public static final int TAME_CHANCE = 3;
    /** Client only: ticks left of the chew cycle. */
    private int chewTicks;

    public Nibblemite(EntityType<? extends Nibblemite> type, Level level) {
        super(type, level);
    }

    /** Client: 0 when not chewing, else the progress 0..1 of the chew cycle (partial-tick smooth). */
    public float chewProgress(float partialTicks) {
        return chewTicks > 0 ? Mth.clamp(1.0F - (chewTicks - partialTicks) / CHEW_TICKS, 0.001F, 1.0F) : 0.0F;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() && chewTicks > 0) chewTicks--;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_CHEW) chewTicks = CHEW_TICKS;
        else super.handleEntityEvent(id);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.4));
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.1, 6.0F, 2.0F));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1, s -> isFood(s) || isTamingFood(s), false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(5, new NibbleGoal(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder data) {
        super.defineSynchedData(data);
        data.define(VARIANT, 0);
    }

    public int variant() {
        return this.entityData.get(VARIANT);
    }

    public void setVariant(int v) {
        this.entityData.set(VARIANT, v == 1 ? 1 : 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Variant", variant());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setVariant(input.getIntOr("Variant", 0));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData group) {
        setVariant(level.getBiome(blockPosition()).is(EndBiomes.DAWNLANDS) ? 1 : 0);
        return super.finalizeSpawn(level, difficulty, reason, group);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.CHORUS_FRUIT) || stack.is(Items.POPPED_CHORUS_FRUIT);
    }

    /** The ripe Drift Fruit tames (B23-M15); breeding stays with chorus fruit. */
    public static boolean isTamingFood(ItemStack stack) {
        return EndRegistry.DRIFT_FRUIT != null && stack.is(EndRegistry.DRIFT_FRUIT);
    }

    /** Tamed and following its owner (not switched to "normal"). */
    public boolean isFollowing() {
        return isTame() && !isOrderedToSit();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isTame() && isTamingFood(stack) && SimpleEnd.on()) {
            if (!level().isClientSide()) {
                usePlayerItem(player, hand, stack);
                tryToTame(player);
            }
            return InteractionResult.SUCCESS;
        }
        if (isTame() && isOwnedBy(player) && !isFood(stack) && hand == InteractionHand.MAIN_HAND) {
            if (!level().isClientSide()) toggleMode(player);
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    /** One taming attempt with a Drift Fruit: hearts and tamed (following) on success, smoke otherwise. */
    public boolean tryToTame(Player player) {
        if (this.random.nextInt(TAME_CHANCE) == 0) {
            tame(player);
            this.navigation.stop();
            setOrderedToSit(false);
            level().broadcastEntityEvent(this, (byte) 7);
            return true;
        }
        level().broadcastEntityEvent(this, (byte) 6);
        return false;
    }

    /** Owner's right-click: follow &lt;-&gt; normal, with a short note above the hotbar. */
    public void toggleMode(Player player) {
        setOrderedToSit(!isOrderedToSit());
        this.navigation.stop();
        player.sendOverlayMessage(Component.translatable(isOrderedToSit()
                ? "entity.simpleend.nibblemite.mode_normal" : "entity.simpleend.nibblemite.mode_follow"));
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        Nibblemite baby = EndRegistry.NIBBLEMITE.create(level, EntitySpawnReason.BREEDING);
        if (baby != null) {
            baby.setVariant(this.random.nextBoolean() || !(partner instanceof Nibblemite other) ? variant() : other.variant());
            if (isTame()) {
                baby.setOwnerReference(getOwnerReference());
                baby.setTame(true, true);
            }
        }
        return baby;
    }

    /** Spawns stand on End stone (or its burrowed form); the End's darkness must not count against them. */
    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return isEndGround(level.getBlockState(pos.below())) ? 10.0F : super.getWalkTargetValue(pos, level);
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        if (reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION) {
            return isEndGround(level.getBlockState(blockPosition().below()));
        }
        return super.checkSpawnRules(level, reason);
    }

    public static boolean isEndGround(BlockState state) {
        return state.is(Blocks.END_STONE) || state.is(EndRegistry.BURROWED_END_STONE);
    }

    /** One nibble: End stone below becomes Burrowed End Stone (with crumbs); the mite is fed (young ones grow). */
    public boolean nibble() {
        if (!SimpleEnd.on()) return false;
        BlockPos below = blockPosition().below();
        BlockState state = level().getBlockState(below);
        if (!state.is(Blocks.END_STONE)) return false;
        level().levelEvent(2001, below, Block.getId(state));
        level().setBlock(below, EndRegistry.BURROWED_END_STONE.defaultBlockState(), 2);
        if (isBaby()) ageUp(60);
        else heal(1.0F);
        return true;
    }

    static final class NibbleGoal extends Goal {
        private final Nibblemite mite;
        private int timer;

        NibbleGoal(Nibblemite mite) {
            this.mite = mite;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (mite.getRandom().nextInt(mite.isBaby() ? NIBBLE_CHANCE / 4 : NIBBLE_CHANCE) != 0) return false;
            return mite.level().getBlockState(mite.blockPosition().below()).is(Blocks.END_STONE);
        }

        @Override
        public void start() {
            timer = adjustedTickDelay(CHEW_TICKS);
            mite.getNavigation().stop();
            mite.level().broadcastEntityEvent(mite, EVENT_CHEW);
        }

        @Override
        public boolean canContinueToUse() {
            return timer > 0;
        }

        @Override
        public void tick() {
            timer = Math.max(0, timer - 1);
            if (timer == adjustedTickDelay(4)) mite.nibble();
        }
    }
}

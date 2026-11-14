package com.simpledinos.entity;

import com.simpledinos.DinoKind;
import com.simpledinos.DinosRegistry;
import com.simpledinos.SimpleDinos;
import com.simpledinos.config.DinosConfig;
import com.simpledinos.entity.part.ParasaurolophusBones;
import com.simpledinos.entity.part.PartRig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Parasaurolophus, the "lead animal" (concept docs/ai/KONZEPT-A-DINO36-2026-11-12.md 4.4, a-dinorest3). A quick quadruped mount that never
 * jumps: the jump key blows the crest call ({@link #press}). Farm animals within {@link DinosConfig#callRadius} blocks follow it for
 * {@link DinosConfig#callSeconds}, at most {@link DinosConfig#callMaxAnimals}, the nearest first: with an empty main hand every animal of the tag
 * {@link #HERD}, otherwise only those that like the held item as food (Vanilla {@code Animal#isFood}: wheat calls cows, sheep and goats, carrots
 * pigs and rabbits, seeds chickens). Animals tamed by someone else, leashed, ridden or riding, named ({@link DinosConfig#callIgnoreNamed}) or
 * standing in a claim the rider may not use (SimpleLib's {@code InteractionGuard}) stay. The call cools down for
 * {@link DinosConfig#callCooldownSeconds}; for the first {@link #DISMISS_TICKS} ticks the jump bar is free and a second press dismisses the herd.
 *
 * <p>No Vanilla AI goal is touched (no mixin): the Parasaurolophus steers its followers itself, each one every {@link #FOLLOW_STEP} ticks
 * (staggered), with the followers' own navigation; their own goals, breeding and luring stay Vanilla. The herd is not saved: it ends when the
 * Parasaurolophus unloads or dies, after the time, or when dismissed. It swims with its rider like a horse floats (entity tag
 * {@code minecraft:can_float_while_ridden}), but deeper ({@link #SWIM_DEPTH}) and without throwing the rider off (it is not in
 * {@code minecraft:dismounts_underwater}); ridden it swims at about 0.6 x its land speed. It never fights: hurt, it flees (without a rider) and
 * gives a warning call.
 */
public class Parasaurolophus extends Dino {
    /** Drawn at three quarters of life size, as the other dinosaurs. */
    public static final float MODEL_SCALE = 0.75F;
    /**
     * Part boxes: neck and head share the look yaw, the head takes half the pitch; "pulled in" raises the long neck and tucks the chin (neck
     * -0.5 rad, head +1.0 rad: the bill comes back from 2.57 to 1.88 blocks, the crest stays clear of the rider). The neck base stands in front
     * of the shoulders, so the head is solid like the Triceratops' (it stops at walls). The tail bends 0.3 rad per step.
     */
    public static final PartRig RIG = new PartRig(ParasaurolophusBones.SKELETON, MODEL_SCALE, "neck", "head",
            new String[] {"neck", "head", "jaw", "crest", "crest_mid", "crest_tip"}, new String[] {"tail1", "tail2", "tail3"},
            0.35F, 0.4F, 0.5F, -0.5F, 1.0F, 0.3F, true);
    /** Walking speed (concept 4.4: 0.24, between a slow and a good horse). */
    public static final double SPEED = 0.24;
    /**
     * Ridden speed in water x this. With the attribute {@code water_movement_efficiency} 1 (like Depth Strider III) the Vanilla water travel
     * gives about 0.6 x the land speed while swimming (about 0.8 x while wading).
     */
    public static final float WATER_SPEED = 0.8F;
    /** Water depth (blocks, grown up) the Parasaurolophus floats at; the rider sits high above it. */
    public static final double SWIM_DEPTH = 1.3;
    /** Ticks after a call in which a second press dismisses the herd (the jump bar is free meanwhile). */
    public static final int DISMISS_TICKS = 100;
    /** Ticks between two steering steps of one follower (staggered by its place in the herd) and between two note particles over it. */
    public static final int FOLLOW_STEP = 20, NOTE_TICKS = 40;
    /** Ticks between two warning calls. */
    public static final int WARN_TICKS = 60;
    /** Speed of a follower (x its own speed, like Vanilla luring) and how close it may come (path reach). */
    public static final double FOLLOW_SPEED = 1.2;
    public static final int FOLLOW_REACH = 2;
    /** A follower farther away than this is lost (blocks). */
    public static final double LOST_DISTANCE = 64.0;
    /** Volume of the call: 4 = heard 64 blocks away. */
    public static final float CALL_VOLUME = 4.0F;
    /** Entity event: the call (client: head up, bill open). */
    public static final byte EVENT_CALL = 101;

    public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, SimpleDinos.id("parasaurolophus_food"));
    /** Animals the call may gather (servers can add more). */
    public static final TagKey<EntityType<?>> HERD = TagKey.create(Registries.ENTITY_TYPE, SimpleDinos.id("herd_followers"));

    private static final EntityDataAccessor<Integer> DISMISS = SynchedEntityData.defineId(Parasaurolophus.class, EntityDataSerializers.INT);

    /** Server: the herd (UUIDs, in call order) and the game time when it stops following. */
    private final List<UUID> herd = new ArrayList<>();
    private long herdUntil;
    /** Server: game time of the last warning call. */
    private long lastWarn = Long.MIN_VALUE / 2;
    /** Client: the call pose 0..1 (previous tick, now) and its ticks left. */
    private float callO, call;
    private int callTicks;

    public Parasaurolophus(EntityType<? extends Parasaurolophus> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractHorse.createBaseHorseAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.JUMP_STRENGTH, 0.42)
                .add(Attributes.STEP_HEIGHT, 1.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
                .add(Attributes.FALL_DAMAGE_MULTIPLIER, 0.5)
                .add(Attributes.WATER_MOVEMENT_EFFICIENCY, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DISMISS, 0);
    }

    @Override
    public PartRig rig() {
        return RIG;
    }

    @Override
    public DinoKind kind() {
        return DinoKind.PARASAUROLOPHUS;
    }

    @Override
    protected void addBehaviourGoals() {
        super.addBehaviourGoals();
        // a-dinorest3: a grown Parasaurolophus flees too when hurt (the hatchling's panic is in Dino); goals only run without a rider
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.5) {
            @Override
            public boolean canUse() {
                return !Parasaurolophus.this.isBaby() && super.canUse();
            }
        });
    }

    // ------------------------------------------------------------------------------------------------ food

    /** Sweet berries (favourite), wheat, hay bale, beetroot: tag simpledinos:parasaurolophus_food. */
    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(FOOD);
    }

    @Override
    public boolean isFavoriteFood(ItemStack stack) {
        return stack.is(Items.SWEET_BERRIES);
    }

    // ------------------------------------------------------------------------------------------------ jump key: the crest call

    @Override
    protected boolean usesJumpAbility() {
        return true;
    }

    /** The jump key works with a saddle on a grown Parasaurolophus; it never jumps itself. */
    @Override
    public boolean canJump() {
        return isSaddled() && !isBaby();
    }

    /** Rider's client: no jump (the key only tells the server, which calls or dismisses). */
    @Override
    public void onPlayerJump(int jumpAmount) {
    }

    @Override
    public void handleStopJump() {
    }

    /** Server, jump key released by the rider: the call, or the dismissal right after one. */
    @Override
    public void handleStartJump(int jumpScale) {
        if (getControllingPassenger() instanceof Player rider) press(rider);
    }

    /** Ticks left in which a second press dismisses the herd (synchronized: the jump bar is free meanwhile). */
    public int dismissTicks() {
        return this.entityData.get(DISMISS);
    }

    /** Vanilla jump bar: free during the dismiss window (a second press dismisses), then the rest of the call's cooldown. */
    @Override
    public int getJumpCooldown() {
        return dismissTicks() > 0 ? 0 : super.getJumpCooldown();
    }

    /** What a press of the jump key did. */
    public enum Press { CALLED, DISMISSED, COOLING, NONE }

    /** Server: a press of the jump key by {@code rider}: dismisses the herd right after a call, calls when the cooldown is over. */
    public Press press(Player rider) {
        if (!(level() instanceof ServerLevel server) || isBaby()) return Press.NONE;
        if (dismissTicks() > 0) {
            dismiss(server);
            return Press.DISMISSED;
        }
        if (abilityCooldown() > 0) return Press.COOLING;
        call(server, rider);
        return Press.CALLED;
    }

    /**
     * Server: the crest call. Gathers the herd ({@link #candidates}; a new call replaces the old herd), starts the follow time, the cooldown
     * and the dismiss window; the call is heard 64 blocks away (Vanilla goat horn, deep, until the own recordings), notes ring around the
     * crest and sculk sensors hear an instrument. Returns the followers.
     */
    public List<Animal> call(ServerLevel level, Player rider) {
        release();
        List<Animal> picked = candidates(level, rider);
        for (Animal a : picked) herd.add(a.getUUID());
        herdUntil = level.getGameTime() + DinosConfig.callTicks();
        setAbilityCooldown(DinosConfig.callCooldownTicks());
        this.entityData.set(DISMISS, DISMISS_TICKS);
        List<Holder.Reference<SoundEvent>> horns = SoundEvents.GOAT_HORN_SOUND_VARIANTS;
        int[] calls = {5, 6, 0}; // "call", "yearn", "ponder": three takes until the own recordings (R14)
        Holder<SoundEvent> horn = horns.get(calls[this.random.nextInt(calls.length)]);
        level.playSound(null, getX(), getEyeY(), getZ(), horn, SoundSource.NEUTRAL, CALL_VOLUME, 0.55F + this.random.nextFloat() * 0.1F);
        level.broadcastEntityEvent(this, EVENT_CALL);
        gameEvent(GameEvent.INSTRUMENT_PLAY);
        AABB crest = null;
        for (AABB b : headBoxes(headTuck())) crest = crest == null ? b : crest.minmax(b);
        if (crest != null) {
            for (int k = 0; k < 8; k++) {
                double a = k * Math.PI / 4.0;
                level.sendParticles(ParticleTypes.NOTE, crest.getCenter().x + Math.cos(a) * 0.7, crest.maxY + 0.2, crest.getCenter().z + Math.sin(a) * 0.7,
                        0, k / 8.0, 0.0, 0.0, 1.0);
            }
        }
        return picked;
    }

    /**
     * The animals a call by {@code rider} gathers now, nearest first, at most {@link DinosConfig#callMaxAnimals}: in the tag {@link #HERD},
     * within {@link DinosConfig#callRadius}, liking the item in the rider's main hand (empty hand: every one), and not excluded
     * ({@link #mayLead}); the claim probe runs once per chunk.
     */
    public List<Animal> candidates(ServerLevel level, Player rider) {
        double r = DinosConfig.callRadius;
        ItemStack hand = rider.getMainHandItem();
        List<Animal> near = level.getEntitiesOfClass(Animal.class, getBoundingBox().inflate(r), a -> distanceToSqr(a) <= r * r && mayLead(a, rider, hand));
        near.sort(Comparator.comparingDouble(this::distanceToSqr));
        List<Animal> out = new ArrayList<>();
        Map<Long, Boolean> claims = new HashMap<>();
        for (Animal a : near) {
            if (out.size() >= DinosConfig.callMaxAnimals) break;
            BlockPos pos = a.blockPosition();
            if (claims.computeIfAbsent(ChunkPos.containing(pos).pack(), c -> com.simplelib.api.InteractionGuard.permission.test(rider, pos))) out.add(a);
        }
        return out;
    }

    /**
     * Whether a call by {@code rider} with {@code hand} in the main hand may lead {@code animal} (all but the claim probe): a living herd
     * animal of the tag that likes the item (empty hand: any), not tamed by someone else, not leashed, not ridden and not riding, not named
     * while {@link DinosConfig#callIgnoreNamed} is on.
     */
    public boolean mayLead(Animal animal, Player rider, ItemStack hand) {
        if (animal == this || !animal.isAlive() || !animal.is(HERD)) return false;
        if (!hand.isEmpty() && !animal.isFood(hand)) return false;
        if (animal instanceof OwnableEntity owned && owned.getOwnerReference() != null && !owned.getOwnerReference().getUUID().equals(rider.getUUID())) return false;
        if (animal.isLeashed() || animal.isVehicle() || animal.isPassenger()) return false;
        return !(DinosConfig.callIgnoreNamed && animal.hasCustomName());
    }

    /** Server: the herd (UUIDs in call order; a copy). */
    public List<UUID> herd() {
        return List.copyOf(herd);
    }

    /** Server: the game time when the herd stops following (0 without a herd). */
    public long herdUntil() {
        return herd.isEmpty() ? 0L : herdUntil;
    }

    /** Server: a second press right after the call - the herd goes its own way, the rest of the cooldown runs. */
    public void dismiss(ServerLevel level) {
        release();
        this.entityData.set(DISMISS, 0);
        level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.CAMEL_AMBIENT, SoundSource.NEUTRAL, 1.0F, 0.6F);
    }

    /** Server: lets every follower go (their navigation stops) and forgets the herd. */
    public void release() {
        if (level() instanceof ServerLevel server) {
            for (UUID id : herd) {
                if (server.getEntity(id) instanceof Mob mob) mob.getNavigation().stop();
            }
        }
        herd.clear();
        herdUntil = 0L;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel server) {
            if (dismissTicks() > 0) this.entityData.set(DISMISS, dismissTicks() - 1);
            if (!herd.isEmpty()) herdStep(server, server.getGameTime());
        }
    }

    /**
     * Server, every tick while a herd follows ({@code now} = game time): ends it after the time; otherwise each follower whose turn it is
     * (one step every {@link #FOLLOW_STEP} ticks, staggered by its place) walks toward the Parasaurolophus or stops near it, and every second
     * step a note shows over it. A follower that is gone, leashed, ridden or riding, or farther than {@link #LOST_DISTANCE} drops out.
     */
    public void herdStep(ServerLevel level, long now) {
        if (now >= herdUntil) {
            release();
            return;
        }
        for (int i = herd.size() - 1; i >= 0; i--) {
            long turn = now + i;
            if (turn % FOLLOW_STEP != 0) continue;
            Entity e = level.getEntity(herd.get(i));
            if (!(e instanceof Mob mob) || !stillFollows(mob)) {
                herd.remove(i);
                continue;
            }
            lead(level, mob, turn % NOTE_TICKS == 0);
        }
    }

    /** Whether {@code mob} still follows: alive, here, not leashed, not ridden and not riding, within {@link #LOST_DISTANCE}. */
    public boolean stillFollows(Mob mob) {
        return mob.isAlive() && !mob.isRemoved() && mob.level() == level() && !mob.isLeashed() && !mob.isVehicle() && !mob.isPassenger()
                && mob.distanceToSqr(this) <= LOST_DISTANCE * LOST_DISTANCE;
    }

    /** One steering step for a follower: toward the Parasaurolophus, or stop when it is close (a note over it when {@code note}). */
    public void lead(ServerLevel level, Mob mob, boolean note) {
        double stop = getBbWidth() / 2.0 + mob.getBbWidth() / 2.0 + FOLLOW_REACH;
        if (mob.distanceToSqr(this) > stop * stop) mob.getNavigation().moveTo(this, FOLLOW_REACH, FOLLOW_SPEED);
        else mob.getNavigation().stop();
        mob.getLookControl().setLookAt(this, 10.0F, mob.getMaxHeadXRot());
        if (note) level.sendParticles(ParticleTypes.NOTE, mob.getX(), mob.getY() + mob.getBbHeight() + 0.4, mob.getZ(), 0, 0.3, 0.0, 0.0, 1.0);
    }

    /** The herd ends with the Parasaurolophus (death, unloading, discarding): it is never saved. */
    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!level().isClientSide()) release();
        super.remove(reason);
    }

    // ------------------------------------------------------------------------------------------------ swimming

    /** Floats this deep (x its size); used by Vanilla for floating while ridden (tag can_float_while_ridden) and by the float goal. */
    @Override
    public double getFluidJumpThreshold() {
        return SWIM_DEPTH * partScale();
    }

    /** Public for the GameTest: in water the ridden speed is {@link #WATER_SPEED} x the land speed (see there). */
    @Override
    public float getRiddenSpeed(Player controller) {
        float speed = super.getRiddenSpeed(controller);
        return isInWater() ? speed * WATER_SPEED : speed;
    }

    // ------------------------------------------------------------------------------------------------ defence (no fighting)

    /** Hurt by any living thing but its rider: a warning call (sound only; the flight is the panic goal without a rider). */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && isAlive() && source.getEntity() instanceof LivingEntity attacker && !hasPassenger(attacker)) warn(level);
        return hurt;
    }

    /** Server: the warning call (goat horn "seek", higher), at most every {@link #WARN_TICKS} ticks; false while it waits. */
    public boolean warn(ServerLevel level) {
        long now = level.getGameTime();
        if (now - lastWarn < WARN_TICKS) return false;
        lastWarn = now;
        level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(2), SoundSource.NEUTRAL, 2.0F, 0.9F);
        gameEvent(GameEvent.INSTRUMENT_PLAY);
        return true;
    }

    // ------------------------------------------------------------------------------------------------ client: the call pose

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_CALL) callTicks = 30;
        else super.handleEntityEvent(id);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            callO = call;
            call += Mth.clamp((callTicks > 0 ? 1.0F : 0.0F) - call, -0.15F, 0.25F);
            if (callTicks > 0) callTicks--;
        }
    }

    /** Client: the call pose 0..1 (head up, bill open). */
    public float callPose(float partialTicks) {
        return Mth.lerp(partialTicks, callO, call);
    }

    // ------------------------------------------------------------------------------------------------ armor, sounds

    /** 0 = no armor, 1 leather, 2 iron, 3 diamond (the renderer picks the texture by it). */
    public static int armorTier(ItemStack bodyArmor) {
        if (bodyArmor.is(DinosRegistry.PARASAUROLOPHUS_ARMOR_LEATHER)) return 1;
        if (bodyArmor.is(DinosRegistry.PARASAUROLOPHUS_ARMOR_IRON)) return 2;
        if (bodyArmor.is(DinosRegistry.PARASAUROLOPHUS_ARMOR_DIAMOND)) return 3;
        return 0;
    }

    public int armorTier() {
        return armorTier(getItemBySlot(EquipmentSlot.BODY));
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (!state.liquid()) playSound(SoundEvents.CAMEL_STEP, 0.6F, 0.8F);
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return com.simpledinos.DinoSounds.PARASAUROLOPHUS_AMBIENT;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return com.simpledinos.DinoSounds.PARASAUROLOPHUS_HURT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return com.simpledinos.DinoSounds.PARASAUROLOPHUS_DEATH;
    }

    @Override
    protected @Nullable SoundEvent getAngrySound() {
        return com.simpledinos.DinoSounds.PARASAUROLOPHUS_AMBIENT;
    }
}

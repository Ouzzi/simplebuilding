package com.simpledinos.entity;

import com.simpledinos.DinoKind;
import com.simpledinos.DinosRegistry;
import com.simpledinos.SimpleDinos;
import com.simpledinos.config.DinosConfig;
import com.simpledinos.entity.part.PartRig;
import com.simpledinos.entity.part.UtahraptorBones;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Utahraptor, the "climber" (concept docs/ai/KONZEPT-A-DINO36-2026-11-12.md 4.3, b-utah48). A quick biped mount (speed 0.30, the best horses
 * stay faster) that jumps like a horse (about 3 blocks) and climbs walls with its rider: walking against a wall climbs it like a spider
 * ({@link #onClimbable}: the Vanilla ladder/spider rule, so the rider's client and the server move it the same way). The stamina lasts
 * {@link DinosConfig#raptorClimbSeconds}; when it is used up the raptor is exhausted, slides down slowly ({@link #SLIDE_SPEED}, no fall
 * damage) and climbs again only after a full recharge on the ground ({@link #RECHARGE_TICKS}); meanwhile the jump bar shows the recharge.
 * Walls of the block tag {@link #UNCLIMBABLE} (ice, glass; servers can add more) cannot be climbed; it climbs only grown up, saddled and
 * ridden (no AI climbing paths), and not at all with {@code raptorClimbSeconds} 0. Half fall damage and 3 more safe blocks, for the raptor
 * and its rider. Brushing a grown raptor gives a raptor feather every {@link DinosConfig#raptorFeatherMinutes}. It never fights: hurt, it
 * flees fast.
 *
 * <p>Its voice is its own (release point 8, b-dino82): chirps, a squawk, a falling screech, a hiss and light talon steps, synthesized by
 * tools/sounds/make_raptor_sounds.py ({@code DinoSounds.UTAHRAPTOR_*}). The scratching at a wall stays the wall's own hit sound.
 */
public class Utahraptor extends Dino {
    /** Drawn at three quarters of life size, as the other dinosaurs. */
    public static final float MODEL_SCALE = 0.75F;
    /**
     * Part boxes (N45: one head box, one tail box): neck and head share the look yaw, the head takes half the pitch. Pulled in, the S-neck
     * stands up and leans back (-1.15 rad) and the head points up a little (+0.15 rad): the snout comes back from 1.27 to 0.6 blocks, inside
     * the front of the core box, so the head is not solid (it rises along a wall instead of stopping the raptor before it). The stiff tail
     * bends 0.3 rad per step.
     */
    public static final PartRig RIG = new PartRig(UtahraptorBones.SKELETON, MODEL_SCALE, "neck", "head", new String[] {"neck", "head", "jaw", "crest"},
            new String[] {"tail1", "tail2", "tail3"}, 0.4F, 0.4F, 0.5F, -1.15F, 0.15F, 0.3F);
    /** Walking speed (concept 4.3: 0.30, quick like a good horse; the best horses reach 0.3375). */
    public static final double SPEED = 0.30;
    /** Full stamina (per mille). */
    public static final int STAMINA_FULL = 1000;
    /** Ticks on the ground from empty to full stamina (5 s). */
    public static final int RECHARGE_TICKS = 100;
    /** Exhausted on a wall it slides down at most this fast (blocks per tick, a ladder lets one fall 0.15). */
    public static final double SLIDE_SPEED = 0.1;
    /** How far a wall may be from the side of the core box to count as touched (blocks). */
    public static final double WALL_REACH = 0.06;
    /** Ticks between two scratch sounds and particle puffs while it climbs. */
    public static final int SCRATCH_TICKS = 5;

    public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, SimpleDinos.id("utahraptor_food"));
    /** Blocks the raptor's claws find no hold on (ice, glass; servers can add more). */
    public static final TagKey<Block> UNCLIMBABLE = TagKey.create(Registries.BLOCK, SimpleDinos.id("raptor_unclimbable"));

    private static final EntityDataAccessor<Integer> STAMINA = SynchedEntityData.defineId(Utahraptor.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> EXHAUSTED = SynchedEntityData.defineId(Utahraptor.class, EntityDataSerializers.BOOLEAN);

    /** Game time from which the next feather can be brushed off (saved). */
    private long featherReadyAt;
    /** Both sides: the wall test of this tick and place (onClimbable is asked several times per tick). */
    private int wallTick = Integer.MIN_VALUE;
    private double wallX = Double.NaN, wallY, wallZ;
    private @Nullable BlockPos wallPos;
    /** Server: height of the last tick (a ridden raptor is moved by its rider's packets between the ticks). */
    private double lastY = Double.NaN;
    /** Client: the climb pose 0..1 (previous tick, now). */
    private float climbO, climb;

    public Utahraptor(EntityType<? extends Utahraptor> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractHorse.createBaseHorseAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.JUMP_STRENGTH, 0.7)
                .add(Attributes.STEP_HEIGHT, 1.5)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1)
                .add(Attributes.FALL_DAMAGE_MULTIPLIER, 0.5)
                .add(Attributes.SAFE_FALL_DISTANCE, 6.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STAMINA, STAMINA_FULL);
        builder.define(EXHAUSTED, false);
    }

    @Override
    public PartRig rig() {
        return RIG;
    }

    @Override
    public DinoKind kind() {
        return DinoKind.UTAHRAPTOR;
    }

    @Override
    protected void addBehaviourGoals() {
        super.addBehaviourGoals();
        // b-utah48: a grown raptor flees fast when hurt (the hatchling's panic is in Dino); goals only run without a rider
        this.goalSelector.addGoal(1, new PanicGoal(this, 2.0) {
            @Override
            public boolean canUse() {
                return !Utahraptor.this.isBaby() && super.canUse();
            }
        });
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putLong("FeatherReadyAt", featherReadyAt);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        featherReadyAt = input.getLongOr("FeatherReadyAt", 0L);
    }

    // ------------------------------------------------------------------------------------------------ food

    /** Meat (tag simpledinos:utahraptor_food = #minecraft:meat); raw chicken and raw rabbit are the favourites. */
    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(FOOD);
    }

    @Override
    public boolean isFavoriteFood(ItemStack stack) {
        return stack.is(Items.CHICKEN) || stack.is(Items.RABBIT);
    }

    // ------------------------------------------------------------------------------------------------ climbing

    /** Stamina left, per mille of {@link DinosConfig#raptorClimbSeconds} (synchronized, counted by the server). */
    public int stamina() {
        return this.entityData.get(STAMINA);
    }

    /** Used up its stamina: no climbing until it is full again (synchronized). */
    public boolean exhausted() {
        return this.entityData.get(EXHAUSTED);
    }

    /** For tests and the config: sets the stamina (clamped) and the exhausted flag. */
    public void setStamina(int perMille, boolean exhausted) {
        this.entityData.set(STAMINA, Mth.clamp(perMille, 0, STAMINA_FULL));
        this.entityData.set(EXHAUSTED, exhausted);
    }

    /** Whether the raptor may climb at all now: switched on, grown up, saddled, steered by a player. */
    public boolean climbAllowed() {
        return DinosConfig.raptorClimbSeconds > 0 && !isBaby() && isSaddled() && getControllingPassenger() instanceof Player;
    }

    /**
     * Whether a climbable wall touches a side of the core box (a solid block shape within {@link #WALL_REACH}, not in {@link #UNCLIMBABLE},
     * not the ground below nor a ceiling above). Computed once per tick and position.
     */
    public boolean touchesClimbableWall() {
        if (wallTick != this.tickCount || wallX != getX() || wallY != getY() || wallZ != getZ()) {
            wallTick = this.tickCount;
            wallX = getX();
            wallY = getY();
            wallZ = getZ();
            wallPos = findWall();
        }
        return wallPos != null;
    }

    /** Forgets the wall test of this tick (tests that change the blocks around a standing raptor). */
    public void forgetWall() {
        wallTick = Integer.MIN_VALUE;
    }

    /** The wall block found by {@link #touchesClimbableWall} this tick (for particles and sounds), or null. */
    public @Nullable BlockPos wallPos() {
        return touchesClimbableWall() ? wallPos : null;
    }

    private @Nullable BlockPos findWall() {
        AABB box = getBoundingBox();
        AABB probe = new AABB(box.minX - WALL_REACH, box.minY + 0.05, box.minZ - WALL_REACH, box.maxX + WALL_REACH, box.maxY, box.maxZ + WALL_REACH);
        AABB inside = box.deflate(0.01);
        Level level = level();
        CollisionContext context = CollisionContext.of(this);
        for (BlockPos pos : BlockPos.betweenClosed(Mth.floor(probe.minX), Mth.floor(probe.minY), Mth.floor(probe.minZ),
                Mth.floor(probe.maxX), Mth.floor(probe.maxY - 1.0E-4), Mth.floor(probe.maxZ))) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.is(UNCLIMBABLE)) continue;
            VoxelShape shape = state.getCollisionShape(level, pos, context);
            if (shape.isEmpty()) continue;
            for (AABB part : shape.toAabbs()) {
                AABB placed = part.move(pos);
                if (placed.intersects(probe) && !placed.intersects(inside)) return pos.immutable();
            }
        }
        return null;
    }

    /**
     * Vanilla climbing (spider, ladder): true at a climbable wall while it may climb and has stamina. Walking against the wall then climbs
     * (0.2 up per tick), letting go lets it glide down slowly; both sides run this rule (the rider's client moves it, the server checks).
     */
    @Override
    public boolean onClimbable() {
        if (climbAllowed() && !exhausted() && stamina() > 0 && touchesClimbableWall()) return true;
        return super.onClimbable();
    }

    /** Exhausted at a wall in the air: it slides down slowly, without fall damage (both sides). */
    public boolean slidesDown() {
        return exhausted() && climbAllowed() && !onGround() && touchesClimbableWall();
    }

    /** Client: climbing or sliding at a wall in the air (the climb pose). */
    public boolean onWall() {
        return climbAllowed() && !onGround() && touchesClimbableWall();
    }

    /** Rider's client (and the server in tests): after the Vanilla move, an exhausted raptor at a wall falls no faster than it slides. */
    @Override
    public void travel(Vec3 input) {
        super.travel(input);
        if (slidesDown()) {
            Vec3 d = getDeltaMovement();
            if (d.y < -SLIDE_SPEED) setDeltaMovement(d.x, -SLIDE_SPEED, d.z);
            resetFallDistance();
        }
    }

    /** Server (rider's moves) and client: sliding down a wall builds up no fall height. */
    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
        if (slidesDown()) {
            resetFallDistance();
            ya = 0.0;
        }
        super.checkFallDamage(ya, onGround, onState, pos);
    }

    /** The rider falls as softly as the raptor: 3 blocks more are safe and the damage is halved (concept 4.3). */
    @Override
    protected void propagateFallToPassengers(double fallDistance, float damageModifier, DamageSource damageSource) {
        super.propagateFallToPassengers(fallDistance - 3.0, damageModifier * 0.5F, damageSource);
    }

    /** Vanilla jump bar: grey while it recharges after being exhausted (the client sends no jump meanwhile). */
    @Override
    public int getJumpCooldown() {
        return exhausted() ? (STAMINA_FULL - stamina() + STAMINA_FULL / RECHARGE_TICKS - 1) / (STAMINA_FULL / RECHARGE_TICKS) : 0;
    }

    /**
     * Server, once per tick: uses stamina at a wall in the air (empty = exhausted), recharges it on the ground or in water (full = rested),
     * scratches (wall particles and the wall's hit sound) while it climbs and hisses softly while it slides.
     */
    public void staminaStep(ServerLevel level) {
        int full = STAMINA_FULL, ticks = Math.max(1, DinosConfig.raptorClimbTicks());
        int stamina = stamina();
        boolean exhausted = exhausted();
        boolean wall = climbAllowed() && !onGround() && touchesClimbableWall();
        double dy = Double.isNaN(lastY) ? 0.0 : getY() - lastY;
        lastY = getY();
        if (wall && !exhausted) {
            stamina = Math.max(0, stamina - (full + ticks - 1) / ticks);
            if (stamina == 0) exhausted = true;
            if (dy > 0.02 && this.tickCount % SCRATCH_TICKS == 0) scratch(level);
        } else if (onGround() || isInWater()) {
            stamina = Math.min(full, stamina + full / RECHARGE_TICKS);
            if (stamina == full) exhausted = false;
        }
        if (wall && exhausted && this.tickCount % 10 == 0) level.playSound(null, getX(), getY(), getZ(), SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.NEUTRAL, 0.6F, 1.2F);
        if (stamina != stamina() || exhausted != exhausted()) setStamina(stamina, exhausted);
    }

    /** Server: claw marks on the wall (block particles) and the wall's hit sound. */
    private void scratch(ServerLevel level) {
        BlockPos pos = wallPos();
        if (pos == null) return;
        BlockState state = level.getBlockState(pos);
        Vec3 c = Vec3.atCenterOf(pos);
        Vec3 at = position().add(c.subtract(position()).multiply(0.5, 0.0, 0.5)).add(0.0, getBbHeight() * 0.4, 0.0);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), at.x, at.y, at.z, 4, 0.15, 0.2, 0.15, 0.05);
        level.playSound(null, at.x, at.y, at.z, state.getSoundType().getHitSound(), SoundSource.NEUTRAL, 0.7F, 1.3F);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel server) staminaStep(server);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            climbO = climb;
            climb += Mth.clamp((onWall() ? 1.0F : 0.0F) - climb, -0.2F, 0.2F);
        }
    }

    /** Client: the climb pose 0..1 (body raised along the wall, arms forward). */
    public float climbPose(float partialTicks) {
        return Mth.lerp(partialTicks, climbO, climb);
    }

    // ------------------------------------------------------------------------------------------------ raptor feathers (brush)

    /** Game time from which the next feather can be brushed off. */
    public long featherReadyAt() {
        return featherReadyAt;
    }

    /**
     * Server: brushes a feather off a grown raptor at game time {@code now} (like the armadillo's scute): one raptor feather, then a pause
     * of {@link DinosConfig#raptorFeatherMinutes}. False (nothing happens) for a hatchling or during the pause.
     */
    public boolean brushFeather(ServerLevel level, long now) {
        if (isBaby() || now < featherReadyAt) return false;
        featherReadyAt = now + DinosConfig.raptorFeatherTicks();
        spawnAtLocation(level, new ItemStack(DinosRegistry.RAPTOR_FEATHER));
        playSound(SoundEvents.ARMADILLO_BRUSH);
        gameEvent(GameEvent.ENTITY_INTERACT);
        return true;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.BRUSH) && !isBaby() && !isVehicle()) {
            if (level() instanceof ServerLevel server) {
                if (!brushFeather(server, server.getGameTime())) return InteractionResult.FAIL;
                stack.hurtAndBreak(16, player, hand.asEquipmentSlot());
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    // ------------------------------------------------------------------------------------------------ armor, sounds

    /** 0 = no armor, 1 leather, 2 iron, 3 diamond, 4..9 the metal tiers of DinosRegistry.ARMOR_TIER_NAMES (the renderer picks the texture by it). */
    public static int armorTier(ItemStack bodyArmor) {
        if (bodyArmor.is(DinosRegistry.UTAHRAPTOR_ARMOR_LEATHER)) return 1;
        if (bodyArmor.is(DinosRegistry.UTAHRAPTOR_ARMOR_IRON)) return 2;
        if (bodyArmor.is(DinosRegistry.UTAHRAPTOR_ARMOR_DIAMOND)) return 3;
        return DinosRegistry.metalArmorTier(bodyArmor, "utahraptor");   // N89A: the metal tiers (codes 4..9)
    }

    public int armorTier() {
        return armorTier(getItemBySlot(EquipmentSlot.BODY));
    }

    // Release point 8 (b-dino82): the raptor's own voice, synthesized (tools/sounds/make_raptor_sounds.py), no Vanilla placeholders.
    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (!state.liquid()) playSound(com.simpledinos.DinoSounds.UTAHRAPTOR_STEP, 0.6F, isBaby() ? 1.3F : 1.0F);
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return com.simpledinos.DinoSounds.UTAHRAPTOR_AMBIENT;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return com.simpledinos.DinoSounds.UTAHRAPTOR_HURT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return com.simpledinos.DinoSounds.UTAHRAPTOR_DEATH;
    }

    @Override
    protected @Nullable SoundEvent getAngrySound() {
        return com.simpledinos.DinoSounds.UTAHRAPTOR_ANGRY;
    }

    /** The own takes are made at the grown raptor's pitch: it varies a little around 1.0, a hatchling chirps higher. */
    @Override
    public float getVoicePitch() {
        return (this.random.nextFloat() - this.random.nextFloat()) * 0.15F + (isBaby() ? 1.35F : 1.0F);
    }

    /** Hurt by a living thing other than its rider: an angry hiss (the flight is the panic goal without a rider). */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && isAlive() && source.getEntity() instanceof Entity attacker && !hasPassenger(attacker)) playSound(com.simpledinos.DinoSounds.UTAHRAPTOR_ANGRY, 1.0F, getVoicePitch());
        return hurt;
    }
}

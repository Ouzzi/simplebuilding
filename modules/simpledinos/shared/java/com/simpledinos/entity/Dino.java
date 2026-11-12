package com.simpledinos.entity;

import com.simpledinos.DinoKind;
import com.simpledinos.DinoVariants;
import com.simpledinos.config.DinosConfig;
import com.simpledinos.entity.part.PartRig;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Base of the dinosaurs (concept 3.5): a horse-family mount without breeding. It comes only from an egg ({@link #hatch}), is
 * tame from its first second and belongs to the player who sat longest on the egg. The hatchling grows from 25 % to 100 % of
 * its size over {@code growMinutes} (the baby age counts down as for every animal; the synchronized {@link #GROWTH} percent
 * gives the smooth size). Saddle, armor, inventory and riding are the Vanilla horse code; the AI is a handful of Vanilla goals.
 */
public abstract class Dino extends AbstractHorse {
    private static final EntityDataAccessor<Integer> GROWTH = SynchedEntityData.defineId(Dino.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COAT = SynchedEntityData.defineId(Dino.class, EntityDataSerializers.INT);
    /** a-dino36b: head pose 0 (stretched) .. {@link PartRig#TUCK_STEPS} (pulled in) and tail bend -2..2, chosen by the server. */
    private static final EntityDataAccessor<Byte> HEAD_TUCK = SynchedEntityData.defineId(Dino.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> TAIL_BEND = SynchedEntityData.defineId(Dino.class, EntityDataSerializers.BYTE);
    /**
     * a-dinorest1 (jump key frame, concept DINO36 8): ticks until the kind's jump-key ability is ready again. Counted by the server
     * (server authority); the rider's client shows it in the Vanilla jump bar ({@link #getJumpCooldown}) and sends no jump meanwhile.
     */
    private static final EntityDataAccessor<Integer> ABILITY_COOLDOWN = SynchedEntityData.defineId(Dino.class, EntityDataSerializers.INT);
    public static final float MIN_SCALE = 0.25F;
    /** Ticks between two part checks, and ticks a part stays at a pose before it may move one step back toward rest. */
    static final int PART_CHECK_TICKS = 2, PART_RELAX_TICKS = 8;
    private int headRelax, tailRelax;
    /** Client: smoothed tuck (0..1) and bend (steps) for drawing, previous tick and now. */
    private float tuckO, tuck, bendO, bend;

    protected Dino(EntityType<? extends Dino> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GROWTH, 100);
        builder.define(COAT, 0);
        builder.define(HEAD_TUCK, (byte) 0);
        builder.define(TAIL_BEND, (byte) 0);
        builder.define(ABILITY_COOLDOWN, 0);
    }

    /** The dinosaur kind (egg, eggshell, fossil weights). */
    public abstract DinoKind kind();

    // ------------------------------------------------------------------------------------------------ jump key frame (a-dinorest1)

    /**
     * Whether the jump key triggers this kind's own ability instead of a jump (Triceratops: charge; later Stegosaurus: plow,
     * Parasaurolophus: call). The T. rex jumps like a horse (false).
     */
    protected boolean usesJumpAbility() {
        return false;
    }

    public int abilityCooldown() {
        return this.entityData.get(ABILITY_COOLDOWN);
    }

    public void setAbilityCooldown(int ticks) {
        this.entityData.set(ABILITY_COOLDOWN, Math.max(0, ticks));
    }

    /** Vanilla jump bar: shows the cooldown of the ability (the client sends no jump while it is above 0). */
    @Override
    public int getJumpCooldown() {
        return usesJumpAbility() ? abilityCooldown() : super.getJumpCooldown();
    }

    /** Coat variant, an index into {@link DinoVariants#COATS} (the rearing biome, see there); every dinosaur kind has it. */
    public int coat() {
        return this.entityData.get(COAT);
    }

    public void setCoat(int coat) {
        this.entityData.set(COAT, DinoVariants.clamp(coat));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("CoatVariant", coat());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setCoat(input.getIntOr("CoatVariant", 0));
    }

    /** Spawned by command, spawner or egg: the coat of the biome where it stands (an egg sets the coat itself before it hatches). */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData group) {
        setCoat(DinoVariants.pick(level.getBiome(blockPosition()), this.random));
        return super.finalizeSpawn(level, difficulty, reason, group);
    }

    /** Size factor for a growth percentage (0 = just hatched, 100 = grown up). */
    public static float scaleFor(int percent) {
        return MIN_SCALE + (1.0F - MIN_SCALE) * Math.max(0, Math.min(100, percent)) / 100.0F;
    }

    /** Growth percentage for a baby age (negative ticks left to grow) over {@code total} ticks. */
    public static int growthFor(int age, int total) {
        if (age >= 0) return 100;
        return (int) Math.max(0L, Math.min(100L, 100L - (long) (-age) * 100L / Math.max(1, total)));
    }

    public int growth() {
        return this.entityData.get(GROWTH);
    }

    @Override
    public float getAgeScale() {
        return isBaby() ? scaleFor(growth()) : 1.0F;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (GROWTH.equals(accessor)) refreshDimensions();
    }

    /** Called by the egg: tame, owned, a hatchling that needs {@code growMinutes} to grow up. */
    public void hatch(Player owner) {
        setTamed(true);
        setOwner(owner);
        setPersistenceRequired();
        setAge(-DinosConfig.growTicks());
        this.entityData.set(GROWTH, 0);
        setHealth(getMaxHealth());
    }

    /** Server: copies the baby age into the synchronized growth percentage (smooth size on the client). */
    public void syncGrowth() {
        int growth = isBaby() ? growthFor(getAge(), DinosConfig.growTicks()) : 100;
        if (growth != growth()) this.entityData.set(GROWTH, growth);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide() && (this.tickCount % 20 == 0)) syncGrowth();
        if (!level().isClientSide() && (this.tickCount % PART_CHECK_TICKS == 0)) updateParts();
        if (!level().isClientSide() && abilityCooldown() > 0) setAbilityCooldown(abilityCooldown() - 1);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            // the drawn pose follows the server's choice smoothly (a full tuck in about 5 ticks)
            tuckO = tuck;
            bendO = bend;
            tuck += Mth.clamp(headTuck() / (float) PartRig.TUCK_STEPS - tuck, -0.2F, 0.2F);
            bend += Mth.clamp(tailBend() - bend, -0.4F, 0.4F);
        }
    }

    // ------------------------------------------------------------------------------------------------ part hitboxes (a-dino36b)

    /**
     * The head and tail boxes of this kind, or null when it has none. The core hitbox is the Vanilla box of the entity type; the head and
     * the tail reach beyond it and turn away from walls and living things on their own (see {@link #updateParts}).
     */
    public @Nullable PartRig rig() {
        return null;
    }

    /** Head pose chosen by the server: 0 = stretched .. {@link PartRig#TUCK_STEPS} = pulled in over the chest. */
    public int headTuck() {
        return this.entityData.get(HEAD_TUCK);
    }

    /** Tail bend chosen by the server: -2..2 steps to one side (0 = straight back). */
    public int tailBend() {
        return this.entityData.get(TAIL_BEND);
    }

    /** Client: smoothed tuck 0..1 for drawing. */
    public float headTuck(float partialTicks) {
        return Mth.lerp(partialTicks, tuckO, tuck);
    }

    /** Client: smoothed tail bend in steps for drawing. */
    public float tailBend(float partialTicks) {
        return Mth.lerp(partialTicks, bendO, bend);
    }

    /** Size factor of the drawn model and of the part boxes: growth x the scale attribute (the core hitbox uses the same two). */
    public float partScale() {
        return getAgeScale() * getScale();
    }

    /** Look yaw of the head relative to the body, as the renderer draws it (degrees, limited like the head turn). */
    public float lookYaw() {
        return Mth.clamp(Mth.wrapDegrees(getYHeadRot() - this.yBodyRot), -getMaxHeadYRot(), getMaxHeadYRot());
    }

    /** World boxes of neck, head and jaw for a tuck step (and the current look). */
    public List<AABB> headBoxes(int tuckStep) {
        List<AABB> out = new ArrayList<>();
        PartRig rig = rig();
        if (rig != null) {
            rig.headBoxes(rig.pose(lookYaw(), getXRot(), tuckStep / (float) PartRig.TUCK_STEPS, 0.0F), partScale(), this.yBodyRot,
                    getX(), getY(), getZ(), out);
        }
        return out;
    }

    /** World boxes of the tail segments for a bend step. */
    public List<AABB> tailBoxes(int bendStep) {
        List<AABB> out = new ArrayList<>();
        PartRig rig = rig();
        if (rig != null) rig.tailBoxes(rig.pose(0.0F, 0.0F, 0.0F, bendStep), partScale(), this.yBodyRot, getX(), getY(), getZ(), out);
        return out;
    }

    /** How far the stretched head reaches in front of the entity position now (blocks), 0 without part boxes. */
    public double headReach() {
        PartRig rig = rig();
        return rig == null ? 0.0 : rig.reach * partScale();
    }

    /**
     * Server: picks the head and tail pose. The head takes the most stretched of its five poses whose neck, head and jaw boxes are
     * free; the tail the straightest free bend (its current side first). Free = no solid block shape (leaves do not count: head and
     * tail brush through foliage, the dinosaur never breaks blocks) and no living thing except its own rider and the target it bites.
     * Pulling in happens at once, stretching out again one step at a time ({@link #PART_RELAX_TICKS}), so the head does not flicker.
     */
    public void updateParts() {
        PartRig rig = rig();
        if (rig == null) return;
        boolean[] head = new boolean[PartRig.TUCK_STEPS + 1];
        int want = PartRig.TUCK_STEPS;
        for (int step = PartRig.TUCK_STEPS; step >= 0; step--) {
            head[step] = partsFree(headBoxes(step));
            if (head[step]) want = step;
        }
        int now = headTuck();
        int next = settle(now, want, head[now], now - 1 >= 0 && head[now - 1], headRelax);
        headRelax = next == now && want < now ? headRelax + PART_CHECK_TICKS : 0;
        if (next != now) this.entityData.set(HEAD_TUCK, (byte) next);

        int bendNow = tailBend(), side = bendNow < 0 ? -1 : 1;
        boolean[] tail = new boolean[2 * PartRig.BEND_STEPS + 1];
        for (int b = -PartRig.BEND_STEPS; b <= PartRig.BEND_STEPS; b++) tail[b + PartRig.BEND_STEPS] = partsFree(tailBoxes(b));
        int bendWant = bendNow;
        for (int b : new int[] {0, side, -side, 2 * side, -2 * side}) {
            if (tail[b + PartRig.BEND_STEPS]) {
                bendWant = b;
                break;
            }
        }
        int toward = bendNow - Integer.signum(bendNow);
        int bendNext = Math.abs(bendWant) > Math.abs(bendNow) || !tail[bendNow + PartRig.BEND_STEPS] ? bendWant
                : Math.abs(bendWant) < Math.abs(bendNow) && tailRelax >= PART_RELAX_TICKS && tail[toward + PartRig.BEND_STEPS] ? toward : bendNow;
        tailRelax = bendNext == bendNow && Math.abs(bendWant) < Math.abs(bendNow) ? tailRelax + PART_CHECK_TICKS : 0;
        if (bendNext != bendNow) this.entityData.set(TAIL_BEND, (byte) bendNext);
    }

    /**
     * Next tuck step: more tucked at once when needed; back toward rest one step at a time after {@link #PART_RELAX_TICKS}, and only onto
     * a free step; straight to {@code want} when the current step itself is blocked.
     */
    static int settle(int now, int want, boolean nowFree, boolean lowerFree, int relaxed) {
        if (want > now || !nowFree) return want;
        if (want < now && relaxed >= PART_RELAX_TICKS && lowerFree) return now - 1;
        return now;
    }

    /** Whether none of the boxes touches a solid block (leaves excepted) or a living thing other than the rider and the bite target. */
    public boolean partsFree(List<AABB> boxes) {
        Level level = level();
        CollisionContext context = CollisionContext.of(this);
        for (AABB raw : boxes) {
            AABB box = raw.deflate(0.02); // grazing contact is no overlap
            for (BlockPos pos : BlockPos.betweenClosed(Mth.floor(box.minX), Mth.floor(box.minY), Mth.floor(box.minZ),
                    Mth.floor(box.maxX), Mth.floor(box.maxY), Mth.floor(box.maxZ))) {
                BlockState state = level.getBlockState(pos);
                if (state.isAir() || state.is(BlockTags.LEAVES)) continue;
                VoxelShape shape = state.getCollisionShape(level, pos, context);
                if (shape.isEmpty()) continue;
                for (AABB part : shape.toAabbs()) {
                    if (part.move(pos).intersects(box)) return false;
                }
            }
            if (!level.getEntities(this, box, this::partBlocker).isEmpty()) return false;
        }
        return true;
    }

    private boolean partBlocker(Entity entity) {
        return entity instanceof LivingEntity living && living.isAlive() && !living.isSpectator() && !(entity instanceof ArmorStand)
                && !hasPassenger(entity) && entity != getTarget();
    }

    /** Bite reach = core box (Vanilla) plus the stretched head with half a block of snap: it bites from its head, not by walking into the target. */
    @Override
    protected AABB getAttackBoundingBox(double horizontalExpansion) {
        AABB box = super.getAttackBoundingBox(horizontalExpansion);
        for (AABB head : headBoxes(0)) box = box.minmax(head.inflate(0.5));
        return box;
    }

    /** Distance at which the dinosaur stops when it follows its owner: outside the reach of its head (at least {@code min}). */
    public double followStop(double min) {
        return Math.max(min, headReach() + 1.0);
    }

    /**
     * a-dinorest1, solid head ({@link PartRig#solidHead}): the head stops the walk like the body does. Both sides run this (the rider's
     * client moves a ridden dinosaur, the server checks the move with the same rule), so the head never walks into a wall.
     */
    @Override
    public void move(MoverType type, Vec3 movement) {
        PartRig rig = rig();
        if (rig != null && rig.solidHead && (movement.x != 0.0 || movement.z != 0.0)) movement = clipHead(movement);
        super.move(type, movement);
    }

    /**
     * The horizontal part of {@code movement} that the head boxes (current pose) can make without running into a solid block shape.
     * Leaves do not count (the head brushes through foliage), nor does anything within the step height above the feet (the body steps
     * up, the head follows), nor a block the head already touches (after a turn it can move out again).
     */
    public Vec3 clipHead(Vec3 movement) {
        double dx = movement.x, dz = movement.z;
        Level level = level();
        CollisionContext context = CollisionContext.of(this);
        double floor = getY() + maxUpStep() + 0.01;
        for (AABB raw : headBoxes(headTuck())) {
            if (raw.maxY <= floor) continue;
            AABB box = new AABB(raw.minX, Math.max(raw.minY, floor), raw.minZ, raw.maxX, raw.maxY, raw.maxZ).deflate(0.02);
            AABB swept = box.expandTowards(dx, 0.0, dz);
            List<VoxelShape> shapes = new ArrayList<>();
            for (BlockPos pos : BlockPos.betweenClosed(Mth.floor(swept.minX), Mth.floor(swept.minY), Mth.floor(swept.minZ),
                    Mth.floor(swept.maxX), Mth.floor(swept.maxY), Mth.floor(swept.maxZ))) {
                BlockState state = level.getBlockState(pos);
                if (state.isAir() || state.is(BlockTags.LEAVES)) continue;
                VoxelShape shape = state.getCollisionShape(level, pos, context);
                if (shape.isEmpty()) continue;
                VoxelShape placed = shape.move(pos.getX(), pos.getY(), pos.getZ());
                boolean inside = false;
                for (AABB part : placed.toAabbs()) inside |= part.intersects(box);
                if (!inside) shapes.add(placed);
            }
            if (shapes.isEmpty()) continue;
            double cx = Shapes.collide(Direction.Axis.X, box, shapes, dx);
            double cz = Shapes.collide(Direction.Axis.Z, box.move(cx, 0.0, 0.0), shapes, dz);
            dx = Math.abs(cx) < Math.abs(dx) ? cx : dx;
            dz = Math.abs(cz) < Math.abs(dz) ? cz : dz;
        }
        return new Vec3(dx, movement.y, dz);
    }

    /** a-dinorest1, solid head: living things in front of the head are pushed aside like by the body (Vanilla push strength). */
    @Override
    protected void pushEntities() {
        super.pushEntities();
        PartRig rig = rig();
        if (rig == null || !rig.solidHead) return;
        java.util.Set<Entity> done = new java.util.HashSet<>();
        for (AABB box : headBoxes(headTuck())) {
            for (Entity other : level().getPushableEntities(this, box)) {
                if (other.isPassengerOfSameVehicle(this) || hasPassenger(other) || !done.add(other) || other.getBoundingBox().intersects(getBoundingBox())) continue;
                Vec3 c = box.getCenter();
                double px = other.getX() - c.x, pz = other.getZ() - c.z, d = Math.max(0.1, Math.sqrt(px * px + pz * pz));
                if (!other.isVehicle() && other.isPushable()) other.push(px / d * 0.05, 0.0, pz / d * 0.05);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ behaviour

    @Override
    protected boolean canPerformRearing() {
        return false;
    }

    @Override
    protected void addBehaviourGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.5) {
            @Override
            public boolean canUse() {
                return Dino.this.isBaby() && super.canUse();
            }
        });
        // a-dinorest1: no combat goals here - only the T. rex fights (Trex#addBehaviourGoals), every other kind has its own defence
        this.goalSelector.addGoal(5, new FollowOwnerLiteGoal(this, 1.1, 10.0, 3.0));
    }

    /** Walks toward the owner when farther than {@code start} blocks until within {@code stop} blocks (Vanilla goals need a TamableAnimal). */
    private static final class FollowOwnerLiteGoal extends Goal {
        private final Dino dino;
        private final double speed, start, stop;
        private int recalc;

        FollowOwnerLiteGoal(Dino dino, double speed, double start, double stop) {
            this.dino = dino;
            this.speed = speed;
            this.start = start;
            this.stop = stop;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity owner = dino.getOwner();
            return dino.isTamed() && !dino.isVehicle() && owner != null && !owner.isSpectator() && dino.distanceToSqr(owner) > start * start;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity owner = dino.getOwner();
            double keep = dino.followStop(stop); // a-dino36b: stops outside the reach of its head
            return !dino.isVehicle() && owner != null && dino.distanceToSqr(owner) > keep * keep;
        }

        @Override
        public void start() {
            recalc = 0;
        }

        @Override
        public void stop() {
            dino.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity owner = dino.getOwner();
            if (owner == null) return;
            dino.getLookControl().setLookAt(owner, 10.0F, dino.getMaxHeadXRot());
            if (--recalc <= 0) {
                recalc = adjustedTickDelay(10);
                dino.getNavigation().moveTo(owner, speed);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ interaction

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ItemTags.MEAT);
    }

    /** The favourite food heals an adult more (concept DINO36 4); every food speeds a hatchling's growth up (Vanilla: 10 % of the rest). */
    public boolean isFavoriteFood(ItemStack stack) {
        return isFood(stack);
    }

    /** Health an adult gets back from one piece of food: 8 for the favourite food, 4 for other food. */
    public float healFor(ItemStack stack) {
        return isFavoriteFood(stack) ? 8.0F : 4.0F;
    }

    @Override
    public boolean canMate(Animal partner) {
        return false; // dinosaurs come from eggs only
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isBaby() && !stack.isEmpty() && isFood(stack)) {
            if (getHealth() < getMaxHealth()) {
                if (!level().isClientSide()) {
                    heal(healFor(stack));
                    stack.consume(1, player);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        if (!isBaby() && !isTamed() && !isVehicle()) {
            makeMad(); // a dinosaur without owner (spawned by command) cannot be ridden
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean canUseSlot(EquipmentSlot slot) {
        if (slot == EquipmentSlot.BODY) return isAlive() && !isBaby();
        return super.canUseSlot(slot);
    }

    // ------------------------------------------------------------------------------------------------ sounds (Vanilla until the owner picks the recordings)

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (!state.liquid()) playSound(SoundEvents.RAVAGER_STEP, 0.5F, 0.7F);
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.RAVAGER_AMBIENT;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }

    @Override
    protected @Nullable SoundEvent getAngrySound() {
        return SoundEvents.RAVAGER_ROAR;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (isBaby() ? 1.3F : 0.65F);
    }
}

package com.simpledinos.entity;

import com.simpledinos.DinoKind;
import com.simpledinos.DinosRegistry;
import com.simpledinos.SimpleDinos;
import com.simpledinos.config.DinosConfig;
import com.simpledinos.entity.part.PartRig;
import com.simpledinos.entity.part.TriceratopsBones;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Triceratops, the "tree shaker" (concept docs/ai/KONZEPT-A-DINO36-2026-11-12.md 4.1, a-dinorest1). A heavy quadruped mount without
 * a jump: the jump key charges a run (hold = charge, Vanilla jump bar like the camel's dash; release = a charge of about 4.5 to 7
 * blocks, {@link DinosConfig#ramCooldownSeconds} cooldown). When the stretched head meets a tree trunk during the charge, the tree
 * shakes ({@link #shake}): the Vanilla hand drops of half its natural leaves fall (saplings, sticks, apples), oak and dark oak add a
 * windfall roll, ripe cocoa gives its beans; the tree stays, every tree rests 10 minutes (a resting or a built tree only knocks
 * dully). Mobs in the way are pushed aside without damage, players never. It does not fight: a monster that hits it is pushed away.
 *
 * <p>The charge follows the Vanilla camel: the rider's client moves the ridden animal (faster, forward), the server does the effects
 * (tree, push, sounds) with its own cooldown and a few ticks of grace for the delay of the rider's moves. The head is solid
 * ({@link PartRig#solidHead}): the frill and the horns stand in front of the shoulders and cannot be pulled in, so the head stops at
 * walls like the body and pushes living things aside.
 */
public class Triceratops extends Dino {
    /** Drawn at three quarters of life size, as the T. rex. */
    public static final float MODEL_SCALE = 0.75F;
    /**
     * Part boxes: neck and head share the look yaw, the head takes half the pitch; "pulled in" lifts the head (neck -0.8 rad, head
     * +0.4 rad: the snout rises from 0.7 to 1.3 blocks); the tail bends 0.35 rad per step. The head is solid.
     */
    public static final PartRig RIG = new PartRig(TriceratopsBones.SKELETON, MODEL_SCALE, "neck", "head",
            new String[] {"neck", "head", "horn_nose", "horn_right", "horn_left", "frill"}, new String[] {"tail1", "tail2", "tail3"},
            0.35F, 0.45F, 0.5F, -0.8F, 0.4F, 0.35F, true);
    /** Ticks of the charge run; the server keeps looking for a hit for {@link #RAM_GRACE} more ticks. */
    public static final int RAM_TICKS = 12, RAM_GRACE = 6;
    /** Extra speed of the run at a full charge (speed x (1 + boost x charge)). */
    public static final float RAM_BOOST = 1.5F;
    /** Push of a mob in the run and of a monster that hits the Triceratops (Vanilla knockback strength; no damage). */
    public static final double RAM_PUSH = 1.2, DEFENCE_PUSH = 2.0;
    /** Trees remembered with their rest time; leaf rolls per shake at most; scan limits. */
    public static final int MAX_TREES = 32, MAX_ROLLS = 40, SCAN_RADIUS = 7, MAX_LOGS = 64, MAX_LEAVES = 96;

    public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, SimpleDinos.id("triceratops_food"));
    /** Trees whose leaves add the windfall roll (oak, dark oak: the Vanilla apple trees). */
    public static final TagKey<Block> WINDFALL_LEAVES = TagKey.create(Registries.BLOCK, SimpleDinos.id("windfall_leaves"));
    public static final ResourceKey<LootTable> WINDFALL = ResourceKey.create(Registries.LOOT_TABLE, SimpleDinos.id("shake/windfall"));

    private static final EntityDataAccessor<Boolean> RAMMING = SynchedEntityData.defineId(Triceratops.class, EntityDataSerializers.BOOLEAN);

    /** Controlling side (the rider's client): ticks left of the run and its charge 0..1. */
    private int ramTicks;
    private float ramPower;
    /** Server: ticks left to look for a hit, whether the run hit a trunk already, mobs pushed in this run. */
    private int serverRam;
    private boolean ramHit;
    private final Set<Integer> pushed = new HashSet<>();
    /** Server: tree base position -> game time when it may shake again (oldest first, at most {@link #MAX_TREES}). */
    private final LinkedHashMap<Long, Long> resting = new LinkedHashMap<>();
    /** Client: the lowered head of the run, smoothed (previous tick, now). */
    private float ramAnimO, ramAnim;

    public Triceratops(EntityType<? extends Triceratops> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractHorse.createBaseHorseAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.MOVEMENT_SPEED, 0.20)
                .add(Attributes.JUMP_STRENGTH, 0.42)
                .add(Attributes.STEP_HEIGHT, 1.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FALL_DAMAGE_MULTIPLIER, 0.6);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RAMMING, false);
    }

    @Override
    public PartRig rig() {
        return RIG;
    }

    @Override
    public DinoKind kind() {
        return DinoKind.TRICERATOPS;
    }

    // ------------------------------------------------------------------------------------------------ food

    /** Apple (favourite), wheat, hay bale, carrot: tag simpledinos:triceratops_food. */
    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(FOOD);
    }

    @Override
    public boolean isFavoriteFood(ItemStack stack) {
        return stack.is(Items.APPLE);
    }

    // ------------------------------------------------------------------------------------------------ charge (jump key)

    @Override
    protected boolean usesJumpAbility() {
        return true;
    }

    /** The head is lowered for the charge: synchronized while the server runs it, or while this client drives it. */
    public boolean isRamming() {
        return this.entityData.get(RAMMING) || ramTicks > 0;
    }

    @Override
    public boolean canJump() {
        return isSaddled() && !isBaby();
    }

    /** Rider's client, jump key released: the run starts on the ground once the cooldown is over. */
    @Override
    public void onPlayerJump(int jumpAmount) {
        if (canJump() && abilityCooldown() <= 0 && ramTicks <= 0 && jumpAmount > 0) {
            this.playerJumpPendingScale = getPlayerJumpPendingScale(jumpAmount);
        }
    }

    /** Controlling side: no jump, a run forward ({@link #RAM_TICKS} ticks, faster with the charge). */
    @Override
    protected void executeRidersJump(float amount, Vec3 input) {
        this.ramTicks = RAM_TICKS;
        this.ramPower = Mth.clamp(amount, 0.0F, 1.0F);
    }

    @Override
    public int getJumpCooldown() {
        return Math.max(super.getJumpCooldown(), ramTicks);
    }

    @Override
    protected void tickRidden(Player controller, Vec3 riddenInput) {
        super.tickRidden(controller, riddenInput);
        if (ramTicks > 0) {
            ramTicks--;
            if (this.horizontalCollision) ramTicks = 0; // bumped into something: the run ends
        }
    }

    @Override
    protected Vec3 getRiddenInput(Player controller, Vec3 selfInput) {
        if (ramTicks > 0) return new Vec3(controller.xxa * 0.2F, 0.0, 1.0);
        return super.getRiddenInput(controller, selfInput);
    }

    @Override
    protected float getRiddenSpeed(Player controller) {
        float speed = super.getRiddenSpeed(controller);
        return ramTicks > 0 ? speed * (1.0F + RAM_BOOST * ramPower) : speed;
    }

    /** Server, jump key released by the rider: the effects of the run (tree, pushes) and the cooldown. */
    @Override
    public void handleStartJump(int jumpScale) {
        startRam();
    }

    @Override
    public void handleStopJump() {
    }

    /** Server: starts the effect window of a run; false while the cooldown runs (server authority) or for a hatchling. */
    public boolean startRam() {
        if (level().isClientSide() || isBaby() || abilityCooldown() > 0) return false;
        serverRam = RAM_TICKS + RAM_GRACE;
        ramHit = false;
        pushed.clear();
        setAbilityCooldown(DinosConfig.ramCooldownTicks());
        this.entityData.set(RAMMING, true);
        playSound(com.simpledinos.DinoSounds.TRICERATOPS_ANGRY, 1.2F, 1.0F);
        gameEvent(GameEvent.ENTITY_ACTION);
        return true;
    }

    public int serverRamTicks() {
        return serverRam;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            ramAnimO = ramAnim;
            ramAnim += Mth.clamp((isRamming() ? 1.0F : 0.0F) - ramAnim, -0.25F, 0.34F);
        }
    }

    /** Client: how far the head is lowered for the run (0..1). */
    public float ramAnim(float partialTicks) {
        return Mth.lerp(partialTicks, ramAnimO, ramAnim);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel server && serverRam > 0) {
            serverRam--;
            ramStep(server);
            if (serverRam == 0) this.entityData.set(RAMMING, false);
        }
    }

    /** One server tick of a run: the first trunk the stretched head touches shakes; mobs at the head are pushed once per run. */
    public void ramStep(ServerLevel level) {
        if (!ramHit) {
            BlockPos log = hitLog();
            if (log != null) {
                ramHit = true;
                shake(level, log);
            }
        }
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        for (AABB box : headBoxes(0)) {
            for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, box.inflate(0.5), this::rammable)) {
                if (pushed.add(other.getId())) other.knockback(RAM_PUSH, Mth.sin(yaw), -Mth.cos(yaw), damageSources().mobAttack(this), 0.0F);
            }
        }
    }

    /** Mobs a run pushes: living, not players, not its rider, not an armor stand, not anybody's tamed animal. */
    public boolean rammable(Entity entity) {
        if (!(entity instanceof LivingEntity living) || !living.isAlive() || entity == this || entity instanceof Player
                || entity instanceof ArmorStand || hasPassenger(entity) || entity.isPassenger()) return false;
        return !(entity instanceof OwnableEntity owned && owned.getOwnerReference() != null);
    }

    /** The first log the stretched head (part boxes, a quarter block of reach) touches, or null. */
    public @Nullable BlockPos hitLog() {
        Level level = level();
        for (AABB raw : headBoxes(0)) {
            AABB box = raw.inflate(0.25, 0.0, 0.25);
            for (BlockPos pos : BlockPos.betweenClosed(Mth.floor(box.minX), Mth.floor(box.minY), Mth.floor(box.minZ),
                    Mth.floor(box.maxX), Mth.floor(box.maxY), Mth.floor(box.maxZ))) {
                if (level.getBlockState(pos).is(BlockTags.LOGS)) return pos.immutable();
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------------ tree shaking

    /** Outcome of a hit: fruit fell, the tree rests, it is no natural tree (no natural leaves), or shaking is off. */
    public enum Shake { SHAKEN, RESTING, NOT_NATURAL, OFF }

    /**
     * A tree found from a hit log: its base (lowest log of the hit column), the natural leaves ({@code persistent=false}), the placed
     * leaves counted, ripe cocoa pods on its logs, and whether it is a windfall tree ({@link #WINDFALL_LEAVES}).
     */
    public record Tree(BlockPos base, List<BlockPos> natural, int placed, List<BlockPos> cocoa, boolean windfall) {}

    /**
     * Walks from {@code hit} over connected logs and leaves (26 neighbours, at most {@link #SCAN_RADIUS} blocks to the side, from one
     * below the base to 24 above the hit, {@link #MAX_LOGS} logs and {@link #MAX_LEAVES} leaves).
     */
    public static Tree scan(Level level, BlockPos hit) {
        BlockPos base = hit;
        while (hit.getY() - base.getY() < 32 && level.getBlockState(base.below()).is(BlockTags.LOGS)) base = base.below();
        List<BlockPos> natural = new ArrayList<>(), cocoa = new ArrayList<>();
        int placed = 0, logs = 1;
        boolean windfall = false;
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<Long> seen = new HashSet<>();
        queue.add(hit);
        seen.add(hit.asLong());
        while (!queue.isEmpty() && natural.size() + placed < MAX_LEAVES) {
            BlockPos pos = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos next = pos.offset(dx, dy, dz);
                        if (Math.abs(next.getX() - hit.getX()) > SCAN_RADIUS || Math.abs(next.getZ() - hit.getZ()) > SCAN_RADIUS
                                || next.getY() < base.getY() - 1 || next.getY() > hit.getY() + 24 || !seen.add(next.asLong())) continue;
                        BlockState state = level.getBlockState(next);
                        if (state.is(BlockTags.LOGS)) {
                            if (logs < MAX_LOGS) {
                                logs++;
                                queue.add(next);
                            }
                        } else if (state.is(BlockTags.LEAVES) && natural.size() + placed < MAX_LEAVES) {
                            boolean persistent = !state.hasProperty(LeavesBlock.PERSISTENT) || state.getValue(LeavesBlock.PERSISTENT);
                            if (persistent) {
                                placed++;
                            } else {
                                natural.add(next);
                                windfall |= state.is(WINDFALL_LEAVES);
                            }
                            queue.add(next);
                        } else if (state.is(Blocks.COCOA) && state.getValue(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE) {
                            cocoa.add(next);
                        }
                    }
                }
            }
        }
        return new Tree(base, natural, placed, cocoa, windfall);
    }

    /** Whether the tree with this base rests at game time {@code now}. */
    public boolean resting(BlockPos base, long now) {
        Long until = resting.get(base.asLong());
        return until != null && now < until;
    }

    private void remember(BlockPos base, long until) {
        resting.remove(base.asLong());
        resting.put(base.asLong(), until);
        Iterator<Map.Entry<Long, Long>> it = resting.entrySet().iterator();
        while (resting.size() > MAX_TREES && it.hasNext()) {
            it.next();
            it.remove();
        }
    }

    /**
     * Server: the run hit {@code log}. A natural tree that does not rest drops the Vanilla hand drops of {@code shakeShare} of its
     * natural leaves (at most {@link #MAX_ROLLS}), the windfall roll (oak, dark oak) and the beans of ripe cocoa (the pods start over);
     * the items pop out on the trunk's side toward the Triceratops. Nothing is broken, the leaves stay.
     */
    public Shake shake(ServerLevel level, BlockPos log) {
        Tree tree = scan(level, log);
        long now = level.getGameTime();
        Shake outcome = !DinosConfig.triceratopsShake ? Shake.OFF : tree.natural().isEmpty() ? Shake.NOT_NATURAL
                : resting(tree.base(), now) ? Shake.RESTING : Shake.SHAKEN;
        if (outcome != Shake.SHAKEN) {
            // a dull knock: nothing falls (resting tree, built tree or switched off) - the learning signal
            level.playSound(null, log, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 1.0F, 0.5F);
            return outcome;
        }
        remember(tree.base(), now + DinosConfig.treeRestTicks());
        List<ItemStack> drops = new ArrayList<>();
        List<BlockPos> leaves = new ArrayList<>(tree.natural());
        Collections.shuffle(leaves, new java.util.Random(this.random.nextLong()));
        int rolls = Math.min(MAX_ROLLS, (int) Math.round(leaves.size() * DinosConfig.shakeShare));
        for (int i = 0; i < rolls; i++) {
            BlockPos pos = leaves.get(i);
            drops.addAll(Block.getDrops(level.getBlockState(pos), level, pos, null));
        }
        if (tree.windfall() && this.random.nextDouble() < DinosConfig.shakeAppleChance) {
            LootTable table = level.getServer().reloadableRegistries().getLootTable(WINDFALL);
            drops.addAll(table.getRandomItems(new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(log))
                    .create(LootContextParamSets.CHEST)));
        }
        for (BlockPos pod : tree.cocoa()) {
            BlockState state = level.getBlockState(pod);
            drops.addAll(Block.getDrops(state, level, pod, null));
            level.setBlock(pod, state.setValue(CocoaBlock.AGE, 0), Block.UPDATE_CLIENTS);
        }
        Vec3 toward = new Vec3(getX() - (log.getX() + 0.5), 0.0, getZ() - (log.getZ() + 0.5));
        toward = toward.lengthSqr() < 1.0E-4 ? Vec3.ZERO : toward.normalize();
        for (ItemStack stack : drops) {
            if (stack.isEmpty()) continue;
            double x = log.getX() + 0.5 + toward.x * 0.9 + (this.random.nextDouble() - 0.5) * 1.2;
            double z = log.getZ() + 0.5 + toward.z * 0.9 + (this.random.nextDouble() - 0.5) * 1.2;
            ItemEntity item = new ItemEntity(level, x, log.getY() + 1.4, z, stack);
            item.setDefaultPickUpDelay();
            level.addFreshEntity(item);
        }
        // leaves rustle and fall around the crown, the ram hits like a goat's
        for (int i = 0; i < Math.min(12, leaves.size()); i++) {
            BlockPos pos = leaves.get(i);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(pos)), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                    3, 0.35, 0.1, 0.35, 0.02);
        }
        level.playSound(null, log, SoundEvents.GOAT_RAM_IMPACT, SoundSource.NEUTRAL, 1.0F, 0.8F);
        BlockState leaf = level.getBlockState(leaves.get(0));
        level.playSound(null, leaves.get(0), leaf.getSoundType().getBreakSound(), SoundSource.BLOCKS, 0.9F, 0.8F);
        return outcome;
    }

    // ------------------------------------------------------------------------------------------------ defence (no fighting)

    /** A monster that hits the grown-up Triceratops in melee is pushed away hard (no damage; it never targets anything itself). */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && !isBaby() && source.getEntity() instanceof Monster monster && monster.isAlive()
                && !(source.getDirectEntity() instanceof Projectile) && distanceToSqr(monster) < 36.0) {
            defend(monster);
        }
        return hurt;
    }

    /** Pushes {@code monster} away from the Triceratops (strength {@link #DEFENCE_PUSH}, no damage). */
    public void defend(LivingEntity monster) {
        monster.knockback(DEFENCE_PUSH, getX() - monster.getX(), getZ() - monster.getZ(), damageSources().mobAttack(this), 0.0F);
        playSound(SoundEvents.GOAT_RAM_IMPACT, 1.0F, 0.9F);
    }

    // ------------------------------------------------------------------------------------------------ armor, storage, sounds

    /** 0 = no armor, 1 leather, 2 iron, 3 diamond, 4..9 the metal tiers of DinosRegistry.ARMOR_TIER_NAMES (the renderer picks the texture by it). */
    public static int armorTier(ItemStack bodyArmor) {
        if (bodyArmor.is(DinosRegistry.TRICERATOPS_ARMOR_LEATHER)) return 1;
        if (bodyArmor.is(DinosRegistry.TRICERATOPS_ARMOR_IRON)) return 2;
        if (bodyArmor.is(DinosRegistry.TRICERATOPS_ARMOR_DIAMOND)) return 3;
        return DinosRegistry.metalArmorTier(bodyArmor, "triceratops");   // N89A: the metal tiers (codes 4..9)
    }

    public int armorTier() {
        return armorTier(getItemBySlot(EquipmentSlot.BODY));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        long now = level().getGameTime();
        int[] trees = new int[resting.size() * 4];
        int i = 0;
        for (Map.Entry<Long, Long> e : resting.entrySet()) {
            BlockPos pos = BlockPos.of(e.getKey());
            trees[i++] = pos.getX();
            trees[i++] = pos.getY();
            trees[i++] = pos.getZ();
            trees[i++] = (int) Math.max(0L, Math.min(Integer.MAX_VALUE, e.getValue() - now));
        }
        output.putIntArray("RestingTrees", trees);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        resting.clear();
        long now = level().getGameTime();
        int[] trees = input.getIntArray("RestingTrees").orElse(new int[0]);
        for (int i = 0; i + 3 < trees.length; i += 4) {
            if (trees[i + 3] > 0) remember(new BlockPos(trees[i], trees[i + 1], trees[i + 2]), now + trees[i + 3]);
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return com.simpledinos.DinoSounds.TRICERATOPS_AMBIENT;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return com.simpledinos.DinoSounds.TRICERATOPS_HURT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return com.simpledinos.DinoSounds.TRICERATOPS_DEATH;
    }

    @Override
    protected @Nullable SoundEvent getAngrySound() {
        return com.simpledinos.DinoSounds.TRICERATOPS_ANGRY;
    }
}

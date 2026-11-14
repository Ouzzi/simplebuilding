package com.simpledinos.entity;

import com.simpledinos.DinoKind;
import com.simpledinos.DinosRegistry;
import com.simpledinos.SimpleDinos;
import com.simpledinos.config.DinosConfig;
import com.simpledinos.entity.part.PartRig;
import com.simpledinos.entity.part.StegosaurusBones;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockTransformers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Stegosaurus, the "plow animal" (concept docs/ai/KONZEPT-A-DINO36-2026-11-12.md 4.2, a-dinorest2). A slow, heavy quadruped mount that
 * never jumps. It pulls the Dino Plow: right-click the own grown Stegosaurus with a plow to put it on, shears take it off (before saddle
 * and armor), it drops on death. The jump key lowers or raises the plow; lowered, it walks at {@link #PLOW_SPEED} and tills a strip of
 * {@link DinosConfig#stegosaurusPlowWidth} blocks (3) behind its hips exactly like a Vanilla hoe would (the 26.3 hoe block transformer:
 * grass, dirt and paths with air above to farmland, coarse dirt to dirt, rooted dirt to dirt and hanging roots), one durability per field,
 * and only where the rider may build (game mode, spawn protection, world border, claim mods through SimpleLib's {@code InteractionGuard}).
 * With seeds in the rider's off hand (tag {@link #PLOW_SEEDS}) it sows every empty field of the strip ({@link DinosConfig#stegosaurusSow}).
 * It never tramples farmland. It does not fight: a monster that hits it in melee gets 2 damage from the tail spikes (never a player).
 *
 * <p>Riding is moved by the rider's client (the lowered plow is a synchronized value, so the slower pace matches on both sides); the
 * fields, the seeds and the durability are the server's work.
 */
public class Stegosaurus extends Dino {
    /** Drawn at three quarters of life size, as the T. rex and the Triceratops. */
    public static final float MODEL_SCALE = 0.75F;
    /**
     * Part boxes: neck and head share the look yaw, the head takes half the pitch; "pulled in" lowers the head to the ground (neck +0.9 rad,
     * head -0.5 rad: the snout comes back from 2.55 to 1.8 blocks, a grazing pose); the tail bends 0.3 rad per step, the four spikes hang
     * on it. The head is solid (it stays in front of the shoulders even when lowered).
     */
    public static final PartRig RIG = new PartRig(StegosaurusBones.SKELETON, MODEL_SCALE, "neck", "head", new String[] {"neck", "head"},
            new String[] {"tail1", "tail2", "tail3"}, new String[] {"spike_right_front", "spike_left_front", "spike_right_back", "spike_left_back"},
            0.4F, 0.4F, 0.5F, 0.9F, -0.5F, 0.3F, true);
    /** Walking speed (attribute) and the pace with the lowered plow (concept 4.2: 0.17 / 0.12). */
    public static final double SPEED = 0.17, PLOW_SPEED = 0.12;
    /** The shares stand this far behind the entity position (blocks; from the model: the plow's cross beam). */
    public static final double PLOW_BEHIND = 1.95;
    /** Ticks the jump key is locked after lowering or raising the plow (shown in the jump bar); ticks between two plow steps. */
    public static final int TOGGLE_TICKS = 10, PLOW_STEP_TICKS = 2;
    /** Ticks without claim probes after a refused one (claim mods often answer with a chat line). */
    public static final int REFUSED_PAUSE = 20;
    /** Damage of the tail spikes to a monster that hits the grown Stegosaurus in melee. */
    public static final float SPIKE_DAMAGE = 2.0F;
    /** Entity event: the tail swipes (client animation). */
    public static final byte EVENT_SWIPE = 100;

    public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, SimpleDinos.id("stegosaurus_food"));
    /** Seeds the plow sows from the rider's off hand (any block item whose plant can stand on farmland; servers may add more). */
    public static final TagKey<Item> PLOW_SEEDS = TagKey.create(Registries.ITEM, SimpleDinos.id("plow_seeds"));

    private static final EntityDataAccessor<Boolean> HAS_PLOW = SynchedEntityData.defineId(Stegosaurus.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> PLOW_DOWN = SynchedEntityData.defineId(Stegosaurus.class, EntityDataSerializers.BOOLEAN);

    /** Server: the plow worn (one Dino Plow with its damage and enchantments), empty without. */
    private ItemStack plow = ItemStack.EMPTY;
    /** Server: game time until which no claim probe runs after a refusal. */
    private long probePauseUntil;
    /** Client: plow lift (0 lowered .. 1 raised) and tail swipe, smoothed (previous tick, now). */
    private float liftO = 1.0F, lift = 1.0F, swipeO, swipe;
    private int swipeTicks;

    public Stegosaurus(EntityType<? extends Stegosaurus> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractHorse.createBaseHorseAttributes()
                .add(Attributes.MAX_HEALTH, 90.0)
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.JUMP_STRENGTH, 0.42)
                .add(Attributes.STEP_HEIGHT, 1.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7)
                .add(Attributes.FALL_DAMAGE_MULTIPLIER, 0.6);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HAS_PLOW, false);
        builder.define(PLOW_DOWN, false);
    }

    @Override
    public PartRig rig() {
        return RIG;
    }

    @Override
    public DinoKind kind() {
        return DinoKind.STEGOSAURUS;
    }

    // ------------------------------------------------------------------------------------------------ food

    /** Fern and large fern (favourites), wheat, hay bale, sweet berries: tag simpledinos:stegosaurus_food. */
    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(FOOD);
    }

    @Override
    public boolean isFavoriteFood(ItemStack stack) {
        return stack.is(Items.FERN) || stack.is(Items.LARGE_FERN);
    }

    // ------------------------------------------------------------------------------------------------ the plow item

    public boolean hasPlow() {
        return this.entityData.get(HAS_PLOW);
    }

    /** Whether the plow is lowered (synchronized: the rider's client slows down with it, the server tills). */
    public boolean plowDown() {
        return hasPlow() && this.entityData.get(PLOW_DOWN);
    }

    /** Server: the worn plow (a copy). */
    public ItemStack plowStack() {
        return plow.copy();
    }

    /** Server: puts {@code stack} on as the plow (empty = takes it off; the plow is raised either way). */
    public void setPlow(ItemStack stack) {
        this.plow = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        this.entityData.set(HAS_PLOW, !this.plow.isEmpty());
        this.entityData.set(PLOW_DOWN, false);
    }

    /** Server: lowers ({@code true}) or raises the plow; false without a plow. */
    public boolean setPlowDown(boolean down) {
        if (!hasPlow()) return false;
        this.entityData.set(PLOW_DOWN, down);
        return true;
    }

    /** The owner puts a plow on the grown Stegosaurus (like a chest on a donkey); anything else goes on to the Vanilla horse code. */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(DinosRegistry.DINO_PLOW) && !isBaby() && isTamed() && player.equals(getOwner()) && !hasPlow() && !isVehicle()) {
            if (!level().isClientSide()) {
                setPlow(stack);
                stack.consume(1, player);
                playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 1.0F, 0.8F);
                gameEvent(GameEvent.EQUIP, player);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    /** Shears take the plow off first (Vanilla: shears take saddle and armor off), then the Vanilla rule goes on. */
    @Override
    protected boolean attemptToShearEquipment(Player player, InteractionHand hand, ItemStack heldItem) {
        if (!hasPlow()) return super.attemptToShearEquipment(player, hand, heldItem);
        heldItem.hurtAndBreak(1, player, hand.asEquipmentSlot());
        if (level() instanceof ServerLevel server) {
            spawnAtLocation(server, plow.copy(), new Vec3(0.0, 1.0, 0.0));
            setPlow(ItemStack.EMPTY);
        }
        gameEvent(GameEvent.SHEAR, player);
        playSound(SoundEvents.HORSE_ARMOR_UNEQUIP.value(), 1.0F, 0.8F);
        return true;
    }

    @Override
    protected void dropEquipment(ServerLevel level) {
        super.dropEquipment(level);
        if (!plow.isEmpty()) {
            spawnAtLocation(level, plow.copy());
            setPlow(ItemStack.EMPTY);
        }
    }

    // ------------------------------------------------------------------------------------------------ jump key: lower / raise the plow

    @Override
    protected boolean usesJumpAbility() {
        return true;
    }

    /** The jump key works with a saddle and a plow; the Stegosaurus itself never jumps. */
    @Override
    public boolean canJump() {
        return isSaddled() && !isBaby() && hasPlow();
    }

    /** Rider's client: no jump (the key only tells the server, which lowers or raises the plow). */
    @Override
    public void onPlayerJump(int jumpAmount) {
    }

    /** Server, jump key released by the rider: lowers or raises the plow. */
    @Override
    public void handleStartJump(int jumpScale) {
        togglePlow();
    }

    @Override
    public void handleStopJump() {
    }

    /** Server: lowers a raised plow or raises a lowered one; false without a plow, for a hatchling or while the key is locked. */
    public boolean togglePlow() {
        if (level().isClientSide() || isBaby() || !hasPlow() || abilityCooldown() > 0) return false;
        boolean down = !plowDown();
        setPlowDown(down);
        setAbilityCooldown(TOGGLE_TICKS);
        playSound(down ? SoundEvents.HOE_TILL.value() : SoundEvents.ARMOR_EQUIP_CHAIN.value(), 1.0F, down ? 0.7F : 0.9F);
        gameEvent(GameEvent.ENTITY_ACTION);
        return true;
    }

    /** With the lowered plow it walks at {@link #PLOW_SPEED} (the rider's client moves it, the plow state is synchronized). */
    @Override
    protected float getRiddenSpeed(Player controller) {
        float speed = super.getRiddenSpeed(controller);
        return plowDown() ? speed * (float) (PLOW_SPEED / SPEED) : speed;
    }

    // ------------------------------------------------------------------------------------------------ plowing and sowing

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel server && plowDown() && !isBaby() && this.tickCount % PLOW_STEP_TICKS == 0
                && getControllingPassenger() instanceof Player rider) {
            plowStep(server, rider);
        }
    }

    /** The blocks under the shares now: a row across the body {@link #PLOW_BEHIND} blocks behind it, {@code stegosaurusPlowWidth} wide. */
    public List<BlockPos> strip() {
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yaw), fz = Mth.cos(yaw);          // forward (Vanilla: yaw 0 faces +z)
        double sx = Mth.cos(yaw), sz = Mth.sin(yaw);           // across
        int width = DinosConfig.stegosaurusPlowWidth;
        int y = Mth.floor(getY() - 0.5);
        List<BlockPos> out = new ArrayList<>();
        for (int i = 0; i < width; i++) {
            double off = i - (width - 1) / 2.0;
            BlockPos pos = BlockPos.containing(getX() - fx * PLOW_BEHIND + sx * off, y, getZ() - fz * PLOW_BEHIND + sz * off);
            if (!out.contains(pos)) out.add(pos);
        }
        return out;
    }

    /** What one plow step did: fields tilled, seeds sown, fields refused for lack of build rights. */
    public record Step(int tilled, int sown, int refused) {}

    /**
     * Server: one step of the lowered plow under {@code rider}. Every block of the strip the Vanilla hoe could till (top face) is tilled
     * when the rider may build there (one durability each; a broken plow is gone); with seeds in the rider's off hand every farmland of the
     * strip with air above gets one seed. Particles per field, one hoe sound per step.
     */
    public Step plowStep(ServerLevel level, Player rider) {
        int tilled = 0, sown = 0, refused = 0;
        BlockTransformer hoe = level.registryAccess().lookupOrThrow(Registries.BLOCK_TRANSFORMER).get(BlockTransformers.HOE)
                .map(h -> h.value()).orElse(null);
        for (BlockPos pos : strip()) {
            if (plow.isEmpty()) break;
            BlockState old = level.getBlockState(pos);
            if (hoe != null && !(old.getBlock() instanceof FarmlandBlock)) {
                for (BlockTransformer.BlockTransformData data : hoe.transforms()) {
                    if (data.disallowedFaces().contains(Direction.UP)) continue;
                    BlockState next = data.blockStateProvider().value().getOptionalState(level, level.getRandom(), pos);
                    if (next == null) continue;
                    if (!mayWork(level, rider, pos)) {
                        refused++;
                        break;
                    }
                    till(level, rider, pos, old, data, next);
                    tilled++;
                    break;
                }
            }
            if (DinosConfig.stegosaurusSow && sow(level, rider, pos)) sown++;
        }
        if (tilled > 0) level.playSound(null, blockPosition(), SoundEvents.HOE_TILL.value(), SoundSource.NEUTRAL, 0.8F, 0.85F);
        return new Step(tilled, sown, refused);
    }

    private void till(ServerLevel level, Player rider, BlockPos pos, BlockState old, BlockTransformer.BlockTransformData data, BlockState next) {
        data.loot().ifPresent(table -> Block.dropFromBlockInteractLootTable(level, table, pos, old, level.getBlockEntity(pos), plow, rider,
                (l, drop) -> Block.popResourceFromFace(l, pos, Direction.UP, drop)));
        BlockState placed = data.updateFromNeighbors() ? Block.updateFromNeighbourShapes(next, level, pos) : next;
        level.setBlock(pos, placed, Block.UPDATE_ALL_IMMEDIATE);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(rider, placed));
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, old), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                4, 0.3, 0.05, 0.3, 0.05);
        plow.hurtAndBreak(1, level, rider instanceof ServerPlayer sp ? sp : null, broken -> {
            level.playSound(null, blockPosition(), SoundEvents.ITEM_BREAK.value(), SoundSource.NEUTRAL, 1.0F, 0.8F);
        });
        if (plow.isEmpty()) setPlow(ItemStack.EMPTY); // worn out: the plow is gone
    }

    /** Sows one seed from the rider's off hand on the farmland at {@code pos} when the plant can stand there and the rider may build. */
    private boolean sow(ServerLevel level, Player rider, BlockPos pos) {
        ItemStack seeds = rider.getOffhandItem();
        if (!seeds.is(PLOW_SEEDS) || !(seeds.getItem() instanceof BlockItem item)) return false;
        BlockPos above = pos.above();
        if (!(level.getBlockState(pos).getBlock() instanceof FarmlandBlock) || !level.getBlockState(above).isAir()) return false;
        BlockState plant = item.getBlock().defaultBlockState();
        if (!plant.canSurvive(level, above) || !mayWork(level, rider, pos)) return false;
        level.setBlock(above, plant, Block.UPDATE_ALL);
        level.gameEvent(GameEvent.BLOCK_PLACE, above, GameEvent.Context.of(rider, plant));
        level.playSound(null, above, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 0.6F, 1.0F);
        seeds.consume(1, rider);
        return true;
    }

    /**
     * Whether the rider may change the block at {@code pos} like with a hoe in hand: not a spectator, may build (not adventure mode),
     * not under spawn protection and inside the world border ({@code mayInteract}), may use the plow there, and the claim probe of
     * SimpleLib's {@code InteractionGuard} agrees (claim mods). After a refused probe no probe runs for {@link #REFUSED_PAUSE} ticks.
     */
    public boolean mayWork(ServerLevel level, Player rider, BlockPos pos) {
        if (rider.isSpectator() || !rider.mayBuild() || !level.mayInteract(rider, pos) || !rider.mayUseItemAt(pos, Direction.UP, plow)) return false;
        if (level.getGameTime() < probePauseUntil) return false;
        if (com.simplelib.api.InteractionGuard.permission.test(rider, pos)) return true;
        probePauseUntil = level.getGameTime() + REFUSED_PAUSE;
        return false;
    }

    /** It never tramples farmland (it does not jump; stepping down onto a field keeps it): fall damage as usual, but no field breaks. */
    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
        land(ya, onGround, onState, pos);
    }

    /** {@link #checkFallDamage} (public for the GameTest): on farmland the fall hurts as usual, but the block's trample code never runs. */
    public void land(double ya, boolean onGround, BlockState onState, BlockPos pos) {
        if (onGround && onState.getBlock() instanceof FarmlandBlock) {
            double fall = this.fallDistance + (ya < 0.0 && !isInWater() ? -ya : 0.0);
            if (fall > 0.0) causeFallDamage(fall, 1.0F, damageSources().fall());
            resetFallDistance();
            super.checkFallDamage(0.0, true, onState, pos);
            return;
        }
        super.checkFallDamage(ya, onGround, onState, pos);
    }

    // ------------------------------------------------------------------------------------------------ defence (no fighting)

    /** A monster that hits the grown Stegosaurus in melee gets {@link #SPIKE_DAMAGE} from the tail spikes (never a player; no hunt). */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && !isBaby() && source.getEntity() instanceof LivingEntity attacker && attacker instanceof Enemy && !(attacker instanceof Player)
                && attacker.isAlive() && !(source.getDirectEntity() instanceof Projectile) && distanceToSqr(attacker) < 36.0) {
            spike(level, attacker);
        }
        return hurt;
    }

    /** The tail swipes: {@link #SPIKE_DAMAGE} to {@code attacker} (thorns damage), a swipe on every client. */
    public void spike(ServerLevel level, LivingEntity attacker) {
        attacker.hurtServer(level, damageSources().thorns(this), SPIKE_DAMAGE);
        level.broadcastEntityEvent(this, EVENT_SWIPE);
        playSound(SoundEvents.THORNS_HIT, 1.0F, 0.7F);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_SWIPE) swipeTicks = 10;
        else super.handleEntityEvent(id);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            liftO = lift;
            lift += Mth.clamp((plowDown() ? 0.0F : 1.0F) - lift, -0.2F, 0.2F);
            swipeO = swipe;
            swipe = swipeTicks > 0 ? Mth.sin((10 - swipeTicks) / 10.0F * Mth.PI) : 0.0F;
            if (swipeTicks > 0) swipeTicks--;
        }
    }

    /** Client: plow lift 0 (lowered) .. 1 (raised). */
    public float plowLift(float partialTicks) {
        return Mth.lerp(partialTicks, liftO, lift);
    }

    /** Client: tail swipe 0..1..0. */
    public float swipe(float partialTicks) {
        return Mth.lerp(partialTicks, swipeO, swipe);
    }

    // ------------------------------------------------------------------------------------------------ armor, storage, sounds

    /** 0 = no armor, 1 leather, 2 iron, 3 diamond (the renderer picks the texture by it). */
    public static int armorTier(ItemStack bodyArmor) {
        if (bodyArmor.is(DinosRegistry.STEGOSAURUS_ARMOR_LEATHER)) return 1;
        if (bodyArmor.is(DinosRegistry.STEGOSAURUS_ARMOR_IRON)) return 2;
        if (bodyArmor.is(DinosRegistry.STEGOSAURUS_ARMOR_DIAMOND)) return 3;
        return 0;
    }

    public int armorTier() {
        return armorTier(getItemBySlot(EquipmentSlot.BODY));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (!plow.isEmpty()) output.store("Plow", ItemStack.CODEC, plow);
        output.putBoolean("PlowDown", plowDown());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setPlow(input.read("Plow", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        setPlowDown(input.getBooleanOr("PlowDown", false));
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (!state.liquid()) playSound(SoundEvents.SNIFFER_STEP, 0.6F, 0.7F);
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return com.simpledinos.DinoSounds.STEGOSAURUS_AMBIENT;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return com.simpledinos.DinoSounds.STEGOSAURUS_HURT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return com.simpledinos.DinoSounds.STEGOSAURUS_DEATH;
    }

    @Override
    protected @Nullable SoundEvent getAngrySound() {
        return SoundEvents.SNIFFER_SNIFFING;
    }
}

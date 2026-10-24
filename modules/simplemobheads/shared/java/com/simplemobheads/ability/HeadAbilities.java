package com.simplemobheads.ability;

import com.simplemobheads.MobHeads;
import com.simplemobheads.SimpleMobHeads;
import com.simplemobheads.data.HeadDef;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What the heads do while worn (concept §3.5; owner 2026-09-29/10-02: fun or a small use, nothing that changes the game a
 * lot, can be exploited or makes other things pointless). Every ability has its own server switch ({@code abilities.<id>}),
 * the master switch turns all off. A head counts as worn in the helmet slot and in every extra head slot a mod offers
 * through framework {@code WornHeads} (SimpleBuilding's extra head slot). The mixins in {@code simplemobheads.mixin} ask
 * here at the place where Vanilla decides.
 *
 * <p>Moved in unchanged: blaze, enderman, husk, spider, cave spider, stray, bogged, slime, silverfish (item attributes),
 * breeze, shulker, drowned (SimpleBuilding); cow, sheep, chicken, pig (Simple Fun). New (owner 13.10.): bee, fox, frog,
 * goat, cat, mooshroom, sniffer, allay, turtle, polar bear, wolf, warden; the secret Herobrine head (owner KONZ20 + 20.10.:
 * torches flicker, hostile mobs hesitate in the dark); the EH21 easter eggs (crowned pig: pigs look, the crown glints; giant and
 * giant cow: size, step height, speed in {@code easter.Giants}, and they count as their kind's head). Every head: camouflage against its own mob
 * (Vanilla {@code mob_visibility}), its mob's note block sound, and a quiet greeting when sneaking.
 */
public final class HeadAbilities {
    /** Bare-hand speed on cobwebs with the cave spider head - that of a sword (Vanilla: 15). */
    public static final float COBWEB_SPEED = 15.0F;
    public static final int SHEAR_COOLDOWN = 2400;
    public static final int EGG_MIN = 6000;
    public static final int EGG_RANDOM = 6000;
    public static final int TRUFFLE_SNIFF_TICKS = 60;
    public static final int TRUFFLE_COOLDOWN = 2400;
    public static final int SNIFF_TICKS = 100;
    public static final int SNIFF_COOLDOWN = 2400;
    /** Frog head: jump strength from lily pads and out of water (x1.5, like a small frog leap). */
    public static final float FROG_JUMP = 1.5F;
    /** Polar bear head: ice is at most this slippery (Vanilla ice 0.98, ordinary ground 0.6). */
    public static final float POLAR_BEAR_FRICTION = 0.8F;
    /** Cat head: creepers keep this distance (as from a cat). */
    public static final float CAT_AVOID_DISTANCE = 6.0F;
    /** Allay head: dropped items within this range float up a little. */
    public static final double ALLAY_RANGE = 3.0;
    /** Goat head: a ram only pushes with this share of its knockback. */
    public static final double GOAT_KNOCKBACK = 0.4;
    /** Greeting: one at most every 5 seconds. */
    public static final int GREETING_COOLDOWN = 100;
    /** Herobrine head: torches within this range flicker, at most {@link #FLICKER_MAX} of them each second. */
    public static final int FLICKER_RANGE = 8;
    public static final int FLICKER_MAX = 6;
    /** Herobrine head: in light up to this level (like monster spawns) hostile mobs that target the wearer hesitate. */
    public static final int GAZE_MAX_LIGHT = 7;
    public static final double GAZE_RANGE = 8.0;
    /** The hesitation: path stopped and Slowness IV for 2 seconds, each mob at most every 30 seconds. */
    public static final int GAZE_TICKS = 40;
    public static final int GAZE_AMPLIFIER = 3;
    public static final int GAZE_COOLDOWN = 600;

    private static final Map<UUID, Long> SHORN = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> STEW = new ConcurrentHashMap<>();
    private static final Map<Player, Long> NEXT_EGG = new WeakHashMap<>();
    private static final Map<Player, Integer> SNIFFING = new WeakHashMap<>();
    private static final Map<UUID, Long> SNIFF_READY = new ConcurrentHashMap<>();
    private static final Map<Player, Long> NEXT_GREETING = new WeakHashMap<>();
    private static final Map<Mob, Long> GAZE_READY = new WeakHashMap<>();

    private HeadAbilities() {
    }

    /**
     * Whether {@code entity} wears the head {@code headId} - helmet slot or an extra head slot (framework WornHeads). A head that
     * counts as another (EH21: the giant cow head as the cow head, the giant head as the head of its zombie kind) counts too.
     */
    public static boolean wears(Entity entity, String headId) {
        if (!(entity instanceof LivingEntity living) || MobHeads.item(headId) == null) {
            return false;
        }
        if (is(living.getItemBySlot(EquipmentSlot.HEAD), headId)) {
            return true;
        }
        for (Object extra : com.simplebuilding.framework.api.WornHeads.extra(living)) {
            if (extra instanceof ItemStack stack && is(stack, headId)) {
                return true;
            }
        }
        return false;
    }

    /** Whether {@code stack} is the head {@code headId} or a head that counts as it ({@link HeadDef#countsAs}). */
    public static boolean is(ItemStack stack, String headId) {
        HeadDef head = MobHeads.of(stack);
        if (head == null) {
            return false;
        }
        if (head.id().equals(headId)) {
            return true;
        }
        if (head.countsAs().equals("variant")) {
            String variant = stack.get(MobHeads.VARIANT);
            return headId.equals(variant != null && !variant.isEmpty() ? variant : head.firstVariant());
        }
        return head.countsAs().equals(headId);
    }

    /** Whether the ability of head {@code id} works for {@code entity} now: switches on and the head worn. */
    public static boolean active(Entity entity, String id) {
        return SimpleMobHeads.on() && SimpleMobHeads.config().ability(id) && wears(entity, id);
    }

    // --- damage, food, movement -------------------------------------------------------------------------------

    /**
     * Blaze: magma blocks and campfires do not burn. Enderman: ender pearl teleports cost no health. Fox: sweet berry
     * bushes do not prick. Goat: a ramming goat does no damage (it still pushes, see {@link #GOAT_KNOCKBACK}).
     */
    public static boolean ignoresDamage(LivingEntity entity, DamageSource source) {
        if ((source.is(DamageTypes.HOT_FLOOR) || source.is(DamageTypes.CAMPFIRE)) && active(entity, "blaze")) {
            return true;
        }
        if (source.is(DamageTypes.ENDER_PEARL) && active(entity, "enderman")) {
            return true;
        }
        if (source.is(DamageTypes.SWEET_BERRY_BUSH) && active(entity, "fox")) {
            return true;
        }
        return source.getEntity() instanceof Goat && active(entity, "goat");
    }

    /** Husk: food gives no Hunger. Bogged: food does not poison. Only food, the other effects of the same food stay. */
    public static boolean blocksFoodEffect(LivingEntity user, ItemStack consumed, MobEffectInstance effect) {
        if (!consumed.has(DataComponents.FOOD)) {
            return false;
        }
        if (effect.is(MobEffects.HUNGER) && active(user, "husk")) {
            return true;
        }
        return effect.is(MobEffects.POISON) && active(user, "bogged");
    }

    /** Spider: cobwebs do not slow the wearer (like the spider itself). */
    public static boolean ignoresCobweb(Entity entity, BlockState state) {
        return state.is(Blocks.COBWEB) && active(entity, "spider");
    }

    /** Cave spider: bare hands cut cobwebs at least as fast as a sword (shears are still needed for the web itself). */
    public static float cobwebBreakSpeed(LivingEntity entity, BlockState state, float speed) {
        if (state.is(Blocks.COBWEB) && speed < COBWEB_SPEED && active(entity, "cave_spider")) {
            return COBWEB_SPEED;
        }
        return speed;
    }

    /** Breeze: light as the wind, the wearer never tramples farmland. */
    public static boolean tramplesFarmland(Entity entity) {
        return !active(entity, "breeze");
    }

    /** Shulker: a shulker box opens even when a block blocks its lid. */
    public static boolean opensBlockedShulkerBoxes(Entity entity) {
        return active(entity, "shulker");
    }

    /** Drowned: a magma bubble column (downwards) does not drag the wearer down. */
    public static boolean resistsDownwardBubbles(Entity entity, boolean dragDown) {
        return dragDown && active(entity, "drowned");
    }

    /** Turtle: the wearer never tramples turtle eggs. */
    public static boolean sparesTurtleEggs(Entity entity) {
        return active(entity, "turtle");
    }

    /** Polar bear: ice is less slippery for the wearer. */
    public static float friction(LivingEntity entity, float friction) {
        return friction > POLAR_BEAR_FRICTION && active(entity, "polar_bear") ? POLAR_BEAR_FRICTION : friction;
    }

    /** Frog: higher jumps from lily pads and out of water. */
    public static float jumpPower(LivingEntity entity, float power) {
        if (!active(entity, "frog")) {
            return power;
        }
        BlockPos feet = entity.blockPosition();
        boolean lilyPad = entity.level().getBlockState(feet).is(Blocks.LILY_PAD) || entity.level().getBlockState(feet.below()).is(Blocks.LILY_PAD);
        return lilyPad || entity.isInWater() ? power * FROG_JUMP : power;
    }

    /** Bee: harvesting a hive or nest does not anger its bees at the wearer. */
    public static boolean calmsBees(Player player) {
        return active(player, "bee");
    }

    /** Wolf: wild (untamed) wolves never target the wearer. */
    public static boolean wolvesSpare(LivingEntity target) {
        return active(target, "wolf");
    }

    /** Cat: creepers avoid the wearer like a cat (predicate of the creeper's avoid goal). */
    public static boolean scaresCreepers(LivingEntity entity) {
        return active(entity, "cat");
    }

    /** Warden: every second footstep of the wearer sends no vibration (sculk hears less, not nothing). */
    public static boolean muffledStep(Entity entity) {
        return active(entity, "warden") && entity.level().getRandom().nextBoolean();
    }

    /** Goat: knockback factor of a goat's ram against the wearer. */
    public static double ramKnockback(LivingEntity target) {
        return active(target, "goat") ? GOAT_KNOCKBACK : 1.0;
    }

    // --- interaction ------------------------------------------------------------------------------------------

    /** A player right-clicks another player ({@code Entity#interact}): milk, shear, fill a bowl. Null = nothing to do. */
    public static InteractionResult interact(Player target, Player user, InteractionHand hand) {
        if (target == user || !SimpleMobHeads.on()) {
            return null;
        }
        ItemStack held = user.getItemInHand(hand);
        if (held.is(Items.BUCKET) && active(target, "cow")) {
            if (!user.level().isClientSide()) {
                user.playSound(SoundEvents.COW_MILK, 1.0F, 1.0F);
                user.setItemInHand(hand, ItemUtils.createFilledResult(held, user, Items.MILK_BUCKET.getDefaultInstance()));
            }
            return InteractionResult.SUCCESS;
        }
        if (held.is(Items.BOWL) && active(target, "mooshroom")) {
            if (user.level() instanceof ServerLevel level) {
                long now = level.getGameTime();
                if (STEW.getOrDefault(target.getUUID(), 0L) > now) {
                    return InteractionResult.PASS;
                }
                STEW.put(target.getUUID(), now + 20);
                user.playSound(SoundEvents.MOOSHROOM_MILK, 1.0F, 1.0F);
                user.setItemInHand(hand, ItemUtils.createFilledResult(held, user, Items.MUSHROOM_STEW.getDefaultInstance(), false));
            }
            return InteractionResult.SUCCESS;
        }
        if (held.is(Items.SHEARS) && active(target, "sheep")) {
            if (user.level() instanceof ServerLevel level) {
                long now = level.getGameTime();
                Long until = SHORN.get(target.getUUID());
                if (until != null && until > now) {
                    return InteractionResult.PASS;
                }
                SHORN.put(target.getUUID(), now + SHEAR_COOLDOWN);
                int wool = 1 + level.getRandom().nextInt(3);
                target.spawnAtLocation(level, new ItemStack(Items.WOOL.pick(DyeColor.WHITE), wool), 1.0F);
                level.playSound(null, target, SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1.0F, 1.0F);
                held.hurtAndBreak(1, user, hand);
            }
            return InteractionResult.SUCCESS;
        }
        return null;
    }

    // --- every server tick of a player --------------------------------------------------------------------------

    public static void tick(ServerPlayer player) {
        if (!SimpleMobHeads.on() || player.isSpectator() || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        if (active(player, "chicken")) {
            Long next = NEXT_EGG.get(player);
            if (next == null) {
                NEXT_EGG.put(player, now + EGG_MIN + level.getRandom().nextInt(EGG_RANDOM));
            } else if (now >= next) {
                layEgg(level, player);
                NEXT_EGG.put(player, now + EGG_MIN + level.getRandom().nextInt(EGG_RANDOM));
            }
        } else {
            NEXT_EGG.remove(player);
        }
        boolean pig = active(player, "pig");
        boolean sniffer = active(player, "sniffer");
        if ((pig || sniffer) && player.isShiftKeyDown() && player.onGround() && SNIFF_READY.getOrDefault(player.getUUID(), 0L) <= now
                && (pig ? truffleGround(level.getBlockState(player.getOnPos())) : sniffGround(level.getBlockState(player.getOnPos())))) {
            int ticks = SNIFFING.getOrDefault(player, 0) + 1;
            if (ticks >= (pig ? TRUFFLE_SNIFF_TICKS : SNIFF_TICKS)) {
                dig(level, player, pig ? truffle(level.getRandom()) : seed(level.getRandom()));
                SNIFF_READY.put(player.getUUID(), now + (pig ? TRUFFLE_COOLDOWN : SNIFF_COOLDOWN));
                ticks = 0;
            }
            SNIFFING.put(player, ticks);
        } else {
            SNIFFING.remove(player);
        }
        if (now % 10 == 0 && active(player, "allay")) {
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(ALLAY_RANGE))) {
                if (item.onGround()) {
                    item.setDeltaMovement(item.getDeltaMovement().add(0.0, 0.12, 0.0));
                }
            }
        }
        if (now % 10 == 0 && active(player, "herobrine")) {
            gaze(level, player, level.getMaxLocalRawBrightness(player.blockPosition()) <= GAZE_MAX_LIGHT);
            if (now % 20 == 0) {
                flickerTorches(level, player);
            }
        }
        if (now % 2 == 0) {
            com.simplemobheads.easter.Technoblade.charm(level, player, now);
        }
        if (player.isShiftKeyDown() && SimpleMobHeads.config().greeting) {
            greet(level, player);
        }
    }

    /**
     * Herobrine head, Herobrine's gaze (owner 20.10.: "one more ability like the mob heads"): in the dark the white eyes make
     * every hostile mob within {@link #GAZE_RANGE} that targets the wearer hesitate - its path stops, Slowness IV for 2 s; each
     * mob at most every 30 s, the wither and the warden do not care. Returns how many mobs hesitated.
     *
     * @param dark whether the wearer stands in the dark (light at most {@link #GAZE_MAX_LIGHT}); the tick reads the real light
     */
    public static int gaze(ServerLevel level, ServerPlayer player, boolean dark) {
        if (!dark || !active(player, "herobrine")) {
            return 0;
        }
        long now = level.getGameTime();
        int hesitated = 0;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(GAZE_RANGE),
                m -> m instanceof Enemy && m.getTarget() == player && !(m instanceof WitherBoss) && !(m instanceof Warden))) {
            if (GAZE_READY.getOrDefault(mob, Long.MIN_VALUE) > now) {
                continue;
            }
            GAZE_READY.put(mob, now + GAZE_COOLDOWN);
            mob.getNavigation().stop();
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, GAZE_TICKS, GAZE_AMPLIFIER), player);
            hesitated++;
        }
        return hesitated;
    }

    /**
     * Herobrine head (owner KONZ20 H5 f): torches near the wearer flicker - smoke and a small flame on up to
     * {@link #FLICKER_MAX} random torches (every {@code TorchBlock}: torch, soul and copper torch, their wall forms) within
     * {@link #FLICKER_RANGE}. Purely cosmetic particles; returns the torches that flickered.
     */
    public static java.util.List<BlockPos> flickerTorches(ServerLevel level, ServerPlayer player) {
        if (!active(player, "herobrine")) {
            return java.util.List.of();
        }
        java.util.List<BlockPos> torches = new java.util.ArrayList<>();
        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-FLICKER_RANGE, -FLICKER_RANGE, -FLICKER_RANGE),
                center.offset(FLICKER_RANGE, FLICKER_RANGE, FLICKER_RANGE))) {
            if (level.isLoaded(pos) && level.getBlockState(pos).getBlock() instanceof TorchBlock) {
                torches.add(pos.immutable());
            }
        }
        net.minecraft.util.Util.shuffle(torches, level.getRandom());
        java.util.List<BlockPos> flickered = java.util.List.copyOf(torches.subList(0, Math.min(FLICKER_MAX, torches.size())));
        for (BlockPos pos : flickered) {
            BlockState state = level.getBlockState(pos);
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.7;
            double z = pos.getZ() + 0.5;
            if (state.hasProperty(WallTorchBlock.FACING)) {
                Direction away = state.getValue(WallTorchBlock.FACING).getOpposite();
                x += 0.27 * away.getStepX();
                y += 0.22;
                z += 0.27 * away.getStepZ();
            }
            level.sendParticles(ParticleTypes.SMOKE, x, y, z, 3, 0.03, 0.05, 0.03, 0.01);
            level.sendParticles(ParticleTypes.SMALL_FLAME, x, y, z, 1, 0.02, 0.02, 0.02, 0.0);
        }
        return flickered;
    }

    /** The mob head {@code entity} wears: an extra head slot first (it has precedence, owner N55), then the helmet slot. */
    public static @Nullable HeadDef wornHead(LivingEntity entity) {
        for (Object extra : com.simplebuilding.framework.api.WornHeads.extra(entity)) {
            if (extra instanceof ItemStack stack && MobHeads.of(stack) != null) {
                return MobHeads.of(stack);
            }
        }
        return MobHeads.of(entity.getItemBySlot(EquipmentSlot.HEAD));
    }

    /** Sneaking with a mob head on: the mob's voice, quietly, with a few notes (cosmetic, moved in from Simple Fun). */
    public static boolean greet(ServerLevel level, ServerPlayer player) {
        HeadDef head = wornHead(player);
        if (head == null) {
            return false;
        }
        long now = level.getGameTime();
        if (now < NEXT_GREETING.getOrDefault(player, Long.MIN_VALUE)) {
            return false;
        }
        NEXT_GREETING.put(player, now + GREETING_COOLDOWN);
        level.sendParticles(ParticleTypes.NOTE, player.getX(), player.getEyeY() + 0.5, player.getZ(), 3, 0.2, 0.1, 0.2, 0.01);
        SoundEvent sound = SoundEvent.createVariableRangeEvent(Identifier.parse(head.sounds().get(0)));
        level.playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 0.2F, 1.0F);
        return true;
    }

    public static void layEgg(ServerLevel level, Player player) {
        level.playSound(null, player, SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS, 1.0F,
                (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.2F + 1.0F);
        player.spawnAtLocation(level, new ItemStack(Items.EGG), 0.2F);
    }

    public static boolean truffleGround(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MYCELIUM) || state.is(Blocks.PODZOL) || state.is(Blocks.MOSS_BLOCK);
    }

    public static boolean sniffGround(BlockState state) {
        return state.is(BlockTags.SNIFFER_DIGGABLE_BLOCK) || truffleGround(state);
    }

    /** What the pig's truffle nose digs up: weighted, the golden carrot rare. */
    public static ItemStack truffle(RandomSource random) {
        int roll = random.nextInt(100);
        if (roll < 30) return new ItemStack(Items.POTATO);
        if (roll < 60) return new ItemStack(Items.CARROT);
        if (roll < 80) return new ItemStack(Items.BEETROOT_SEEDS);
        if (roll < 90) return new ItemStack(Items.BROWN_MUSHROOM);
        if (roll < 98) return new ItemStack(Items.RED_MUSHROOM);
        return new ItemStack(Items.GOLDEN_CARROT);
    }

    /** What the sniffer's nose finds: seeds, the ancient ones (torchflower, pitcher) rare. */
    public static ItemStack seed(RandomSource random) {
        int roll = random.nextInt(100);
        if (roll < 35) return new ItemStack(Items.WHEAT_SEEDS);
        if (roll < 55) return new ItemStack(Items.BEETROOT_SEEDS);
        if (roll < 70) return new ItemStack(Items.PUMPKIN_SEEDS);
        if (roll < 85) return new ItemStack(Items.MELON_SEEDS);
        if (roll < 95) return new ItemStack(Items.TORCHFLOWER_SEEDS);
        return new ItemStack(Items.PITCHER_POD);
    }

    public static void dig(ServerLevel level, Player player, ItemStack found) {
        BlockPos ground = player.getOnPos();
        level.levelEvent(2001, ground, net.minecraft.world.level.block.Block.getId(level.getBlockState(ground)));
        level.playSound(null, player, SoundEvents.SNIFFER_DIGGING, SoundSource.PLAYERS, 0.6F, 1.4F);
        player.spawnAtLocation(level, found, 0.3F);
    }

    /**
     * Server stop (SimpleLib {@code PlayerState.onStop}): the cooldowns count game ticks of one world, a next world (single
     * player) starts with its own clock. Not on leaving: a relog must not reset the shearing cooldown.
     */
    public static void forgetAll() {
        SHORN.clear();
        STEW.clear();
        SNIFF_READY.clear();
        NEXT_EGG.clear();
        SNIFFING.clear();
        NEXT_GREETING.clear();
        GAZE_READY.clear();
    }
    /** Tests only: forget every cooldown of a player. */
    public static void resetForTests(Player player) {
        SHORN.remove(player.getUUID());
        STEW.remove(player.getUUID());
        SNIFF_READY.remove(player.getUUID());
        NEXT_EGG.remove(player);
        SNIFFING.remove(player);
        NEXT_GREETING.remove(player);
        GAZE_READY.clear();
    }
}

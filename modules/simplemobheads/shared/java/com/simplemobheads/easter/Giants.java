package com.simplemobheads.easter;

import com.simplemobheads.MobHeads;
import com.simplemobheads.SimpleMobHeads;
import com.simplemobheads.ability.HeadAbilities;
import com.simplemobheads.config.MobHeadsConfig;
import com.simplemobheads.data.HeadDef;
import com.simplemobheads.drop.HeadDropCondition;
import com.simplemobheads.drop.Variants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The giants (EH21 G-A, ZO-1, CO-2 + owner; docs/ai/PLAN-B-EH21-2026-10-22.md). A giant is the mob itself, three times as big
 * (Vanilla's {@code minecraft:scale}, marked by the modifier {@link #MARK}): it keeps its AI, its kind and its look, so every
 * zombie kind can be one (zombie, husk, drowned, zombie villager, zombified piglin) and Simple Mobs is not needed.
 * <ul>
 *   <li>Giant zombie: natural lightning (no player cause, not only visual) strikes an adult zombie on a full-moon night.</li>
 *   <li>Giant cow: every hay bale an adult cow eats is one try at {@code giantCowChance} (1 in 5000); a cow eats at most
 *   {@code giantCowHayLimit} (64). Same drops and milk as any cow.</li>
 *   <li>Heads: only a charged creeper's explosion, and then the giant's head (its kind/variant) instead of the normal one
 *   ({@link #creeperHead}). Vanilla's command-only {@code minecraft:giant} drops the giant head through its normal pool.</li>
 *   <li>Worn ({@link #wearerTick}): the wearer grows to 1.5x and steps up whole blocks; the giant head also walks 10 % faster.</li>
 * </ul>
 */
public final class Giants {
    /** Marker and size of every giant: scale x3. */
    public static final Identifier MARK = SimpleMobHeads.id("giant");
    public static final double SCALE = 2.0;
    public static final double STEP = 1.0;
    public static final double ZOMBIE_HEALTH = 80.0;
    public static final double ZOMBIE_DAMAGE = 3.0;
    public static final double ZOMBIE_KNOCKBACK_RESISTANCE = 0.5;
    public static final double COW_HEALTH = 20.0;
    /** The wearer of a giant head: 1.5x size, half a block more step height, 10 % faster (giant head only). */
    public static final double WEAR_SCALE = 0.5;
    public static final double WEAR_STEP = 0.5;
    public static final double WEAR_SPEED = 0.1;
    public static final Identifier WEAR_SCALE_ID = SimpleMobHeads.id("giant_head_scale");
    public static final Identifier WEAR_STEP_ID = SimpleMobHeads.id("giant_head_step");
    public static final Identifier WEAR_SPEED_ID = SimpleMobHeads.id("giant_head_speed");
    /** Entity tag (cow): hay bales eaten so far. */
    public static final String HAY = "simplemobheads.hay";
    public static final String ZOMBIE_HEAD = "giant";
    public static final String COW_HEAD = "giant_cow";

    /** Entity tag (cow): the lucky bale came while there was no room to grow; it grows as soon as there is ({@link #tick}). */
    public static final String PENDING = "simplemobheads.giant_pending";

    public enum Lightning { GIANT, DISABLED, NOT_NATURAL, NOT_FULL_MOON_NIGHT, BABY, ALREADY, NO_ROOM }

    private Giants() {
    }

    public static boolean isGiant(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }
        AttributeInstance scale = living.getAttribute(Attributes.SCALE);
        return scale != null && scale.hasModifier(MARK);
    }

    private static MobHeadsConfig config() {
        return SimpleMobHeads.config();
    }

    // --- giant zombie ------------------------------------------------------------------------------------------

    /**
     * {@code Entity#thunderHit} ({@code EasterThunderMixin}, before Vanilla's fire and damage): a natural bolt on a full-moon
     * night makes the zombie a giant.
     *
     * @param forcedFullMoonNight test hook: full-moon night or not instead of the world's time and moon ({@code null}: real)
     */
    public static Lightning onLightning(ServerLevel level, Zombie zombie, LightningBolt bolt, @Nullable Boolean forcedFullMoonNight) {
        if (!SimpleMobHeads.on() || !config().giantZombies) {
            return Lightning.DISABLED;
        }
        if (bolt.getCause() != null) {
            return Lightning.NOT_NATURAL;
        }
        if (isGiant(zombie)) {
            return Lightning.ALREADY;
        }
        if (zombie.isBaby()) {
            return Lightning.BABY;
        }
        if (!(forcedFullMoonNight != null ? forcedFullMoonNight : fullMoonNight(level, zombie.blockPosition()))) {
            return Lightning.NOT_FULL_MOON_NIGHT;
        }
        if (!hasRoom(zombie)) {
            // Under trees or a roof the giant would stand in the blocks and suffocate: the bolt changes nothing.
            return Lightning.NO_ROOM;
        }
        grow(zombie, ZOMBIE_HEALTH);
        add(zombie, Attributes.ATTACK_DAMAGE, SimpleMobHeads.id("giant_damage"), ZOMBIE_DAMAGE);
        add(zombie, Attributes.KNOCKBACK_RESISTANCE, SimpleMobHeads.id("giant_knockback_resistance"), ZOMBIE_KNOCKBACK_RESISTANCE);
        level.playSound(null, zombie, SoundEvents.ZOMBIE_AMBIENT, SoundSource.HOSTILE, 2.0F, 0.5F);
        return Lightning.GIANT;
    }

    public static boolean fullMoonNight(ServerLevel level, BlockPos pos) {
        return level.isDarkOutside() && level.environmentAttributes().getValue(EnvironmentAttributes.MOON_PHASE, pos) == MoonPhase.FULL_MOON;
    }

    // --- giant cow ---------------------------------------------------------------------------------------------

    /**
     * {@code AbstractCow#mobInteract} ({@code CowFeedMixin}): a player feeds a cow a hay bale. Null = not a hay bale for a
     * cow (Vanilla goes on).
     *
     * @param forcedRoll test hook: the roll in [0, 1) instead of a random one ({@code null}: random)
     */
    public static @Nullable InteractionResult feedHay(Cow cow, Player player, InteractionHand hand, @Nullable Float forcedRoll) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(Items.HAY_BLOCK) || cow.getType() != EntityTypes.COW || !SimpleMobHeads.on() || !config().giantCows) {
            return null;
        }
        if (cow.isBaby() || isGiant(cow) || cow.entityTags().contains(PENDING)) {
            return null;
        }
        if (!(cow.level() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        long eaten = EntityTimes.get(cow, HAY, 0L);
        if (eaten >= config().giantCowHayLimit) {
            // Full: it turns the hay down.
            level.sendParticles(ParticleTypes.SMOKE, cow.getX(), cow.getEyeY(), cow.getZ(), 4, 0.2, 0.1, 0.2, 0.01);
            return InteractionResult.PASS;
        }
        held.consume(1, player);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.HAY_BLOCK.defaultBlockState()),
                cow.getX(), cow.getEyeY(), cow.getZ(), 8, 0.2, 0.1, 0.2, 0.05);
        level.playSound(null, cow, SoundEvents.GENERIC_EAT.value(), SoundSource.NEUTRAL, 1.0F, 0.8F);
        float roll = forcedRoll != null ? forcedRoll : level.getRandom().nextFloat();
        if (roll < config().giantCowChance) {
            EntityTimes.clear(cow, HAY);
            if (hasRoom(cow)) {
                growCow(level, cow);
            } else {
                // A barn too low: the luck is kept and the cow grows once it stands in the open (no suffocating giant).
                cow.addTag(PENDING);
            }
        } else {
            EntityTimes.set(cow, HAY, eaten + 1);
        }
        return InteractionResult.SUCCESS;
    }

    /** Every second of a cow ({@code AnimalAiMixin}): a pending giant cow grows as soon as it has room. */
    public static void tick(ServerLevel level, Cow cow) {
        if (cow.tickCount % 20 == 0 && cow.entityTags().contains(PENDING) && SimpleMobHeads.on() && config().giantCows && hasRoom(cow)) {
            growCow(level, cow);
        }
    }

    private static void growCow(ServerLevel level, Cow cow) {
        cow.removeTag(PENDING);
        grow(cow, COW_HEALTH);
        if (!cow.hasCustomName()) {
            cow.setCustomName(Component.translatable("entity.simplemobheads.giant_cow"));
        }
        level.playSound(null, cow, SoundEvents.COW_SOUNDS.get(net.minecraft.world.entity.animal.cow.CowSoundVariants.SoundSet.CLASSIC).ambientSound().value(),
                SoundSource.NEUTRAL, 2.0F, 0.5F);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, cow.getX(), cow.getEyeY(), cow.getZ(), 20, 0.8, 0.8, 0.8, 0.0);
    }

    /** Whether the mob, three times as big, would stand free of blocks where it is now. */
    public static boolean hasRoom(Mob mob) {
        AABB grown = mob.getType().getDimensions().scale((float) (1.0 + SCALE)).makeBoundingBox(mob.position());
        return mob.level().noCollision(mob, grown);
    }

    /** Three times as big (the marker), more health (healed to full), a whole block more step height, never despawns. */
    private static void grow(Mob mob, double health) {
        add(mob, Attributes.SCALE, MARK, SCALE, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        add(mob, Attributes.MAX_HEALTH, SimpleMobHeads.id("giant_health"), health);
        add(mob, Attributes.STEP_HEIGHT, SimpleMobHeads.id("giant_step"), STEP);
        mob.setHealth(mob.getMaxHealth());
        mob.setPersistenceRequired();
        if (mob.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.POOF, mob.getX(), mob.getY() + 1.0, mob.getZ(), 30, 1.0, 1.0, 1.0, 0.02);
        }
    }

    private static void add(LivingEntity entity, Holder<Attribute> attribute, Identifier id, double amount) {
        add(entity, attribute, id, amount, AttributeModifier.Operation.ADD_VALUE);
    }

    private static void add(LivingEntity entity, Holder<Attribute> attribute, Identifier id, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.addOrReplacePermanentModifier(new AttributeModifier(id, amount, operation));
        }
    }

    // --- heads -------------------------------------------------------------------------------------------------

    /**
     * {@code Creeper#killedEntity} ({@code CreeperMixin}): the head a charged creeper's explosion gives a giant instead of the
     * normal head roll, or null (no giant, or its head switched off: Vanilla rolls as always).
     */
    public static @Nullable ItemStack creeperHead(LivingEntity victim) {
        if (!isGiant(victim) || !config().creeperDrops) {
            return null;
        }
        HeadDef head = MobHeads.head(victim instanceof Zombie ? ZOMBIE_HEAD : victim instanceof Cow ? COW_HEAD : "");
        if (head == null || MobHeads.item(head.id()) == null || !HeadDropCondition.allowed(head)) {
            return null;
        }
        return MobHeads.stack(head, Variants.of(head, victim));
    }

    /**
     * Every server tick of a player ({@code ServerPlayerTickMixin}): while a giant head or a giant cow head is worn (ability
     * switches {@code giant}, {@code giant_cow}), 1.5x size and more step height, with the giant head also a little speed.
     * Transient modifiers, added once and taken off with the head (or by the switches); nothing is saved with the player.
     */
    public static void wearerTick(ServerPlayer player) {
        boolean giant = HeadAbilities.active(player, ZOMBIE_HEAD);
        boolean big = giant || HeadAbilities.active(player, COW_HEAD);
        wear(player, Attributes.SCALE, WEAR_SCALE_ID, WEAR_SCALE, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, big);
        wear(player, Attributes.STEP_HEIGHT, WEAR_STEP_ID, WEAR_STEP, AttributeModifier.Operation.ADD_VALUE, big);
        wear(player, Attributes.MOVEMENT_SPEED, WEAR_SPEED_ID, WEAR_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, giant);
    }

    private static void wear(LivingEntity entity, Holder<Attribute> attribute, Identifier id, double amount, AttributeModifier.Operation operation, boolean on) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null || instance.hasModifier(id) == on) {
            return;
        }
        if (on) {
            instance.addTransientModifier(new AttributeModifier(id, amount, operation));
        } else {
            instance.removeModifier(id);
        }
    }
}

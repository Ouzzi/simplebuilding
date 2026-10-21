package com.simplestorage.rare;

import com.simplestorage.SimpleStorage;
import com.simplestorage.chest.ChestTier;
import com.simplestorage.config.StorageConfig;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Rare end city shulkers (owner 2026-10-02, split stage 8b-3 from SimpleBuilding): when an end city generates, each sentry
 * shulker becomes reinforced with {@code reinforcedShulkerPercent} (2 %, at most 10 %), Netherite with
 * {@code netheriteShulkerPercent} (1 %, at most 5 %) or Enderite with {@code enderiteShulkerPercent} (0.5 %, at most 5 %)
 * ({@link StorageConfig}).
 *
 * <p><b>Upgrading living shulkers</b> (owner 2026-10-02, Easter egg): right-click with the nugget of the next tier - the same chain
 * as the shells ({@link ShulkerShells#steps}): iron, Netherite, Enderite nugget. A shulker upgraded that way ({@link #UPGRADED_TAG})
 * drops no tier shells and calls no endermites, else upgrading + killing would be a cheap way to shells.
 *
 * <p><b>Tier = health modifier.</b> The tier is a modifier on the maximum health ({@link #REINFORCED_HEALTH_ID} +50 % = 1.5x,
 * {@link #ENDERITE_HEALTH_ID} +200 % = 3x). Vanilla saves it with the shulker and sends it to the clients (maximum health is
 * synced) - the renderer shows the tier's hull from it ({@code ShulkerRendererMixin}), without a data channel of its own.
 * Shulkers of worlds from before the split carry SimpleBuilding's modifier and tag names; they are read too ({@link #LEGACY_IDS}).
 *
 * <p><b>Drops:</b> 0-2 shells of their own tier besides the Vanilla loot ({@link #dropShells}).
 */
public final class RareShulkers {
    public static final Identifier REINFORCED_HEALTH_ID = SimpleStorage.id("reinforced_shulker");
    public static final Identifier NETHERITE_HEALTH_ID = SimpleStorage.id("netherite_shulker");
    public static final Identifier ENDERITE_HEALTH_ID = SimpleStorage.id("enderite_shulker");
    /** Health Netherite: 2x. */
    public static final double NETHERITE_HEALTH_FACTOR = 2.0;
    /** Upgraded by a player (entity tag, saved): no tier shells, no escort. */
    public static final String UPGRADED_TAG = "simplestorage.upgraded_shulker";
    /** Health reinforced: 1.5x. */
    public static final double REINFORCED_HEALTH_FACTOR = 1.5;
    /** Health Enderite: 3x. */
    public static final double ENDERITE_HEALTH_FACTOR = 3.0;
    /** At most this many tier shells per death (uniform 0..MAX). */
    public static final int MAX_SHELL_DROPS = 2;
    /** SimpleBuilding's names of the modifiers (by tier) and tags, in shulkers of older worlds. */
    public static final List<Identifier> LEGACY_IDS = List.of(SimpleStorage.old("reinforced_shulker"), SimpleStorage.old("netherite_shulker"),
            SimpleStorage.old("enderite_shulker"));
    public static final String LEGACY_UPGRADED_TAG = "simplebuilding.upgraded_shulker";
    public static final String LEGACY_ESCORT_TAG = "simplebuilding.endermite_escort";

    private RareShulkers() {
    }

    /** The tier of roll {@code r} in [0, 1): first Enderite, then Netherite, then reinforced, else none. */
    public static @Nullable ChestTier tierFor(double r, double enderiteChance, double netheriteChance, double reinforcedChance) {
        if (r < enderiteChance) {
            return ChestTier.ENDERITE;
        }
        if (r < enderiteChance + netheriteChance) {
            return ChestTier.NETHERITE;
        }
        return r < enderiteChance + netheriteChance + reinforcedChance ? ChestTier.REINFORCED : null;
    }

    /** End city sentry (mixin on {@code handleDataMarker}): rolls the tier and applies it. */
    public static void onEndCitySpawn(Shulker shulker, RandomSource random) {
        if (!SimpleStorage.on()) {
            return;
        }
        ChestTier tier = tierFor(random.nextDouble(), StorageConfig.enderiteShulkerChance(), StorageConfig.netheriteShulkerChance(),
                StorageConfig.reinforcedShulkerChance());
        if (tier != null) {
            apply(shulker, tier);
            if (StorageConfig.endermitesPerRareShulker > 0) {
                shulker.addTag(ESCORT_TAG);
            }
        }
    }

    // =====================================================================================
    // Endermite escort (owner 2026-10-02)
    // =====================================================================================

    /**
     * A rare shulker whose endermites are still due (entity tag, saved with the shulker). The endermites do not come when the
     * structure generates: Vanilla would remove them before a player arrives (monsters despawn far from every player, endermites
     * after 2 minutes). They appear once, as soon as a player (not creative/spectator) is within {@link #ESCORT_TRIGGER_RANGE}
     * blocks; the tag is gone then - nothing repeats.
     */
    public static final String ESCORT_TAG = "simplestorage.endermite_escort";
    /** From this player distance (blocks) the endermites appear. */
    public static final double ESCORT_TRIGGER_RANGE = 24.0;
    /** Endermites appear at most this far (horizontal, blocks) from the shulker. */
    public static final int ESCORT_RADIUS = 3;
    /** Attempts per endermite to find a safe spot. */
    private static final int ESCORT_ATTEMPTS = 12;
    /** Checked once a second only. */
    private static final int ESCORT_CHECK_INTERVAL = 20;

    private static boolean escortDue(Shulker shulker) {
        return shulker.entityTags().contains(ESCORT_TAG) || shulker.entityTags().contains(LEGACY_ESCORT_TAG);
    }

    /** Every server tick of a shulker (mixin on {@code Shulker#tick}). */
    public static void tickEscort(Shulker shulker) {
        if (!(shulker.level() instanceof ServerLevel level) || shulker.tickCount % ESCORT_CHECK_INTERVAL != 0
                || !escortDue(shulker) || level.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
            // Vanilla allows no endermites on Peaceful; the escort then waits instead of getting lost.
            return;
        }
        if (level.getNearestPlayer(shulker.getX(), shulker.getY(), shulker.getZ(), ESCORT_TRIGGER_RANGE,
                net.minecraft.world.entity.EntitySelector.NO_CREATIVE_OR_SPECTATOR) == null) {
            return;
        }
        triggerEscort(level, shulker);
    }

    /**
     * Calls the due escort (once: the tag goes). Returns the number of endermites, 0 without a due escort or on Peaceful (then it
     * stays due).
     */
    public static int triggerEscort(ServerLevel level, Shulker shulker) {
        if (!escortDue(shulker) || level.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
            return 0;
        }
        shulker.removeTag(ESCORT_TAG);
        shulker.removeTag(LEGACY_ESCORT_TAG);
        return spawnEscort(level, shulker, StorageConfig.endermitesPerRareShulker);
    }

    /**
     * Puts up to {@code count} endermites on safe spots near the shulker (air for the whole body, solid ground, no fluid); not
     * persistent (Vanilla removes them after 2 minutes). Returns the number.
     */
    public static int spawnEscort(ServerLevel level, Shulker shulker, int count) {
        RandomSource random = shulker.getRandom();
        BlockPos origin = shulker.blockPosition();
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            for (int attempt = 0; attempt < ESCORT_ATTEMPTS; attempt++) {
                BlockPos pos = origin.offset(random.nextInt(2 * ESCORT_RADIUS + 1) - ESCORT_RADIUS, random.nextInt(3) - 1,
                        random.nextInt(2 * ESCORT_RADIUS + 1) - ESCORT_RADIUS);
                if (!isSafeSpot(level, pos)) {
                    continue;
                }
                net.minecraft.world.entity.monster.Endermite mite = net.minecraft.world.entity.EntityTypes.ENDERMITE.spawn(
                        level, pos, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
                if (mite != null) {
                    spawned++;
                    break;
                }
            }
        }
        return spawned;
    }

    /** Solid ground below, no fluid, and the endermite fits without collision. */
    public static boolean isSafeSpot(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)
                || !level.getFluidState(pos).isEmpty()) {
            return false;
        }
        return level.noCollision(net.minecraft.world.entity.EntityTypes.ENDERMITE.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
    }

    /** Makes {@code shulker} the tier {@code tier} (1.5x/2x/3x health) with full health. */
    public static void apply(Shulker shulker, ChestTier tier) {
        AttributeInstance health = shulker.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) {
            return;
        }
        for (Identifier id : List.of(REINFORCED_HEALTH_ID, NETHERITE_HEALTH_ID, ENDERITE_HEALTH_ID)) health.removeModifier(id);
        for (Identifier id : LEGACY_IDS) health.removeModifier(id);
        Identifier id = switch (tier) {
            case REINFORCED -> REINFORCED_HEALTH_ID;
            case NETHERITE -> NETHERITE_HEALTH_ID;
            case ENDERITE -> ENDERITE_HEALTH_ID;
        };
        double factor = switch (tier) {
            case REINFORCED -> REINFORCED_HEALTH_FACTOR;
            case NETHERITE -> NETHERITE_HEALTH_FACTOR;
            case ENDERITE -> ENDERITE_HEALTH_FACTOR;
        };
        health.addPermanentModifier(new AttributeModifier(id, factor - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        shulker.setHealth(shulker.getMaxHealth());
    }

    /**
     * The tier {@code nugget} lifts a shulker of tier {@code current} (null = plain) to, or null: exactly one tier, with the same
     * nugget as the shell of that tier ({@link ShulkerShells#steps}).
     */
    public static @Nullable ChestTier upgradeOf(@Nullable ChestTier current, ItemStack nugget) {
        List<ShulkerShells.Step> steps = ShulkerShells.steps();
        int index = current == null ? 0 : current.ordinal() + 1;
        if (index >= steps.size() || nugget.isEmpty() || !nugget.is(steps.get(index).nugget())) {
            return null;
        }
        return ChestTier.values()[index];
    }

    /**
     * Right-click on a living shulker (mixin on {@code Mob#mobInteract}): lifts it one tier, uses one nugget (not in creative
     * mode), sound and particles. False when the nugget does not fit.
     */
    public static boolean upgradeLiving(Shulker shulker, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ChestTier next = upgradeOf(tierOf(shulker), stack);
        if (next == null || shulker.isDeadOrDying()) {
            return false;
        }
        if (!(shulker.level() instanceof ServerLevel level)) {
            return true;
        }
        apply(shulker, next);
        shulker.addTag(UPGRADED_TAG);
        shulker.removeTag(ESCORT_TAG);
        shulker.removeTag(LEGACY_ESCORT_TAG);
        stack.consume(1, player);
        level.playSound(null, shulker.blockPosition(), net.minecraft.sounds.SoundEvents.SMITHING_TABLE_USE,
                net.minecraft.sounds.SoundSource.HOSTILE, 0.8F, 1.2F);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.WAX_ON, shulker.getX(), shulker.getY() + 0.5, shulker.getZ(),
                12, 0.4, 0.4, 0.4, 0.05);
        return true;
    }

    /** The tier of a shulker (on server and client; also SimpleBuilding's modifiers of older worlds), or null. */
    public static @Nullable ChestTier tierOf(Shulker shulker) {
        AttributeInstance health = shulker.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) {
            return null;
        }
        if (health.getModifier(ENDERITE_HEALTH_ID) != null || health.getModifier(LEGACY_IDS.get(2)) != null) {
            return ChestTier.ENDERITE;
        }
        if (health.getModifier(NETHERITE_HEALTH_ID) != null || health.getModifier(LEGACY_IDS.get(1)) != null) {
            return ChestTier.NETHERITE;
        }
        return health.getModifier(REINFORCED_HEALTH_ID) != null || health.getModifier(LEGACY_IDS.get(0)) != null ? ChestTier.REINFORCED : null;
    }

    /**
     * The hull of a tier ({@code textures/entity/rare_shulker/<tier>.png}, Vanilla-derived through
     * {@code tools/textures/shulker_shell_textures.py}): the undyed tier shulker box with Vanilla's shulker head; a dyed tier shulker
     * shows it as well.
     */
    public static Identifier textureOf(ChestTier tier) {
        return SimpleStorage.id("textures/entity/rare_shulker/" + tier.textureName() + ".png");
    }

    /** Whether a player upgraded this shulker (this module's tag or SimpleBuilding's). */
    public static boolean upgradedByPlayer(Shulker shulker) {
        return shulker.entityTags().contains(UPGRADED_TAG) || shulker.entityTags().contains(LEGACY_UPGRADED_TAG);
    }

    /** On death (mixin on {@code Mob#dropCustomDeathLoot}): 0-2 shells of its own tier; upgraded ones none. */
    public static void dropShells(ServerLevel level, Shulker shulker) {
        ChestTier tier = upgradedByPlayer(shulker) ? null : tierOf(shulker);
        if (tier == null) {
            return;
        }
        Item shell = ShulkerShells.shellOf(tier);
        int count = shulker.getRandom().nextInt(MAX_SHELL_DROPS + 1);
        if (count > 0) {
            shulker.spawnAtLocation(level, new ItemStack(shell, count));
        }
    }
}

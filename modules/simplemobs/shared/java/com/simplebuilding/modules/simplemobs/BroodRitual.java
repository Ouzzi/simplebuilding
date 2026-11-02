package com.simplebuilding.modules.simplemobs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

/**
 * Brood hollow (Brutmulde, BS25-23, owner 01.11.: concept K1 "in-world ritual", no block, no screen).
 * <pre>
 *  T s T      top view; s = Soul Sand, P = Purpur Block (the hollow), T = Soul Sand with a Soul Torch on it
 *  s P s
 *  T s T
 * </pre>
 * Throw a Blank Egg, the essences of one species and the class catalyst onto the Purpur Block, then light it with Flint
 * and Steel. The brood runs for the class duration (an invisible marker, saved with the chunk); when a soul torch goes
 * out or the hollow breaks, everything comes back. The result is the species' Vanilla spawn egg. Bosses never
 * ({@code #simplemobs:no_egg}). Essence drops only from mobs a player kills that did not come from a spawner or an egg.
 */
public final class BroodRitual {
    public static final TagKey<EntityType<?>> NO_EGG = TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MobsRegistry.ID, "no_egg"));
    /** Entity tag of mobs that never drop essence (from a spawner, a spawn egg, a dispenser, a command or a trial spawner). */
    public static final String NO_ESSENCE = "simplemobs.no_essence";
    private static final Set<EntitySpawnReason> UNNATURAL = Set.of(EntitySpawnReason.SPAWNER, EntitySpawnReason.TRIAL_SPAWNER,
            EntitySpawnReason.SPAWN_ITEM_USE, EntitySpawnReason.DISPENSER, EntitySpawnReason.COMMAND);

    /** Class I..V: essence needed, brood time (ticks); the catalysts follow in {@link #catalysts}. */
    public static final int[] COST = {0, 4, 12, 16, 32, 64};
    public static final int[] TICKS = {0, 30 * 20, 120 * 20, 180 * 20, 360 * 20, 720 * 20};

    private static final Map<String, Integer> CLASS = Map.ofEntries(
            Map.entry("minecraft:villager", 2), Map.entry("minecraft:horse", 2), Map.entry("minecraft:donkey", 2), Map.entry("minecraft:mule", 2),
            Map.entry("minecraft:llama", 2), Map.entry("minecraft:camel", 2), Map.entry("minecraft:wolf", 2), Map.entry("minecraft:iron_golem", 2),
            Map.entry("minecraft:enderman", 2), Map.entry("minecraft:sniffer", 2), Map.entry("minecraft:allay", 2), Map.entry("minecraft:panda", 2),
            Map.entry("minecraft:wither_skeleton", 4), Map.entry("minecraft:piglin_brute", 4), Map.entry("minecraft:evoker", 4),
            Map.entry("minecraft:ravager", 4), Map.entry("minecraft:ghast", 4), Map.entry("minecraft:shulker", 4), Map.entry("minecraft:breeze", 4),
            Map.entry("simplemobs:shellker", 4), Map.entry("simplemobs:soulfire_blaze", 4),
            Map.entry("minecraft:elder_guardian", 5), Map.entry("simplemobs:deceiver", 5), Map.entry("simplemobs:hollow_howler", 5),
            Map.entry("simplemobs:dread", 5), Map.entry("simplemobs:hollow_watcher", 5));

    private BroodRitual() {}

    /** Egg class 1..5 of a species, 0 = no egg (bosses, players, no Vanilla spawn egg). Unknown monsters: III, others: I. */
    public static int eggClass(EntityType<?> type) {
        if (type.builtInRegistryHolder().is(NO_EGG) || SpawnEggItem.byId(type).isEmpty()) return 0;
        Integer fixed = CLASS.get(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
        if (fixed != null) return fixed;
        return type.getCategory() == MobCategory.MONSTER ? 3 : 1;
    }

    /** The class catalysts (one each). */
    public static List<Item> catalysts(int eggClass) {
        return switch (eggClass) {
            case 1 -> List.of(Items.WHEAT);
            case 2 -> List.of(Items.EMERALD);
            case 3 -> List.of(Items.SOUL_SAND, Items.LAPIS_LAZULI);
            case 4 -> List.of(Items.NETHERITE_SCRAP, Items.ENDER_EYE);
            case 5 -> List.of(Items.NETHER_STAR);
            default -> List.of();
        };
    }

    /** Essence chance of a kill: 5 % for monsters, 2 % else, +1 % per Looting level (config scales the base). */
    public static float essenceChance(boolean monster, int looting) {
        return (monster ? MobsConfig.essenceChance : MobsConfig.essenceChance * 0.4f) + 0.01f * Math.max(0, looting);
    }

    // ---- essence drop -----------------------------------------------------------------------

    /** Spawn hook: mobs from spawners, eggs, dispensers, commands and trial spawners are marked. */
    public static void onSpawn(Mob mob, EntitySpawnReason reason) {
        if (UNNATURAL.contains(reason)) mob.addTag(NO_ESSENCE);
    }

    /** Death hook (player kills only): maybe drops one essence. */
    public static void onDeath(LivingEntity entity, ServerPlayer killer) {
        if (!MobsRegistry.on() || !MobsConfig.eggRitualEnabled || !(entity instanceof Mob) || entity.entityTags().contains(NO_ESSENCE)) return;
        if (!(entity.level() instanceof ServerLevel level) || eggClass(entity.getType()) == 0) return;
        int looting = level.registryAccess().lookup(Registries.ENCHANTMENT).flatMap(r -> r.get(Enchantments.LOOTING))
                .map(h -> EnchantmentHelper.getItemEnchantmentLevel((Holder<net.minecraft.world.item.enchantment.Enchantment>) h, killer.getMainHandItem())).orElse(0);
        if (level.getRandom().nextFloat() >= essenceChance(entity.getType().getCategory() == MobCategory.MONSTER, looting)) return;
        entity.spawnAtLocation(level, MobEssenceItem.of(entity.getType(), 1));
    }

    // ---- the hollow -------------------------------------------------------------------------

    /** Soul sand ring around the Purpur Block at {@code hollow}, a soul torch on each corner. */
    public static boolean figure(Level level, BlockPos hollow) {
        if (!level.getBlockState(hollow).is(Blocks.PURPUR_BLOCK)) return false;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            if (dx == 0 && dz == 0) continue;
            BlockPos p = hollow.offset(dx, 0, dz);
            if (!level.getBlockState(p).is(Blocks.SOUL_SAND)) return false;
            if (dx != 0 && dz != 0 && !level.getBlockState(p.above()).is(Blocks.SOUL_TORCH)) return false;
        }
        return true;
    }

    /** What the offerings on the hollow would brew: the species, or null with the reason key in {@code why[0]}. */
    public static @Nullable EntityType<?> recipe(List<ItemEntity> items, String[] why) {
        if (BossRituals.count(items, s -> s.is(BossMobs.EGG_BLANK)) < 1) { why[0] = "blank"; return null; }
        EntityType<?> species = null;
        for (ItemEntity e : items) {
            EntityType<?> t = MobEssenceItem.species(e.getItem());
            if (t == null) continue;
            if (species != null && species != t) { why[0] = "mixed"; return null; }
            species = t;
        }
        if (species == null) { why[0] = "essence"; return null; }
        int cls = eggClass(species);
        if (cls == 0) { why[0] = "boss"; return null; }
        final EntityType<?> sp = species;
        if (BossRituals.count(items, s -> sp == MobEssenceItem.species(s)) < COST[cls]) { why[0] = "essence"; return null; }
        for (Item cat : catalysts(cls)) if (BossRituals.count(items, s -> s.is(cat)) < 1) { why[0] = "catalyst"; return null; }
        return species;
    }

    /** Flint and Steel on the hollow: starts the brood if figure and offerings are complete (server). */
    public static boolean tryStart(Level level, BlockPos hollow, @Nullable ServerPlayer player) {
        if (!(level instanceof ServerLevel server) || !MobsRegistry.on() || !MobsConfig.eggRitualEnabled || !figure(level, hollow)) return false;
        if (!server.getEntitiesOfClass(BroodRitualEntity.class, new net.minecraft.world.phys.AABB(hollow.above())).isEmpty()) return false;
        List<ItemEntity> items = BossRituals.offerings(level, hollow);
        String[] why = {""};
        EntityType<?> species = recipe(items, why);
        if (species == null) {
            BossRituals.fizzle(level, hollow);
            if (player != null) player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("simplemobs.brood.missing." + why[0]));
            return true; // the click is used, nothing consumed
        }
        int cls = eggClass(species);
        List<ItemStack> taken = new ArrayList<>();
        taken.add(new ItemStack(BossMobs.EGG_BLANK));
        BossRituals.take(items, s -> s.is(BossMobs.EGG_BLANK), 1);
        taken.add(MobEssenceItem.of(species, COST[cls]));
        BossRituals.take(items, s -> species == MobEssenceItem.species(s), COST[cls]);
        for (Item cat : catalysts(cls)) {
            taken.add(new ItemStack(cat));
            Predicate<ItemStack> is = s -> s.is(cat);
            BossRituals.take(items, is, 1);
        }
        BroodRitualEntity brood = BossMobs.BROOD_RITUAL.create(server, EntitySpawnReason.TRIGGERED);
        if (brood == null) return false;
        brood.setup(hollow, species, TICKS[cls], taken);
        server.addFreshEntity(brood);
        server.playSound(null, hollow, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 0.7F);
        server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, hollow.getX() + 0.5, hollow.getY() + 1.1, hollow.getZ() + 0.5, 20, 0.3, 0.1, 0.3, 0.02);
        return true;
    }

    /** The Vanilla spawn egg of a species (empty when it has none). */
    public static ItemStack eggOf(EntityType<?> species) {
        return SpawnEggItem.byId(species).map(h -> new ItemStack(h.value())).orElse(ItemStack.EMPTY);
    }
}

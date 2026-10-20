package com.simplemobheads.ritual;

import com.simplemobheads.MobHeads;
import com.simplemobheads.SimpleMobHeads;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Herobrine ritual (KONZEPT-HEROBRINE-KOPF-2026-10-19, owner KONZ20: H1 totem shrine, H2 ritual + haunting, H4 only at
 * night). The shrine, seen from the netherrack {@code N}: 3x3 gold blocks at {@code N-2}, a mossy cobblestone at {@code N-1}
 * and a redstone torch on each of the four edge gold blocks next to it. A player lights the netherrack with flint and steel or
 * a fire charge ({@code IgniteItemMixin}) - fire without a player (lightning, lava, dispensers) never counts, and the pattern is
 * only checked at that one spot, never searched for. At night: a visual-only lightning bolt, the torches go out, the moss
 * turns to cobblestone, the fire dies in soul flames and the Herobrine head lies on the shrine; gold and netherrack stay. By
 * day nothing is used up. Cooldowns per shrine position and per player keep the head from being farmed (in memory only: a
 * restart frees them, the shrine still costs its torches and moss).
 */
public final class HerobrineRitual {
    public enum Result { SUMMONED, DISABLED, NO_PLAYER, NO_SHRINE, DAY, NOT_ALLOWED, COOLDOWN }

    /** Chance of the hint book in a mineshaft chest. */
    public static final float HINT_BOOK_CHANCE = 0.05F;
    /** The visual lightning strikes this many blocks beside the shrine. */
    public static final int LIGHTNING_OFFSET = 3;

    private static final List<Direction> EDGES = List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);
    private static final Map<String, Long> SHRINE_READY = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> PLAYER_READY = new ConcurrentHashMap<>();

    private HerobrineRitual() {
    }

    /** {@code IgniteItemMixin}: a player's flint and steel or fire charge just set fire at {@code fire}. */
    public static void onIgnite(ServerLevel level, BlockPos fire, @Nullable ServerPlayer player) {
        if (level.getBlockState(fire.below()).is(Blocks.NETHERRACK)) {
            ignite(level, fire, player, null);
        }
    }

    /**
     * Runs the ritual for the fire at {@code fire} (the netherrack below it).
     *
     * @param forcedNight test hook: night or day instead of the world's time ({@code null}: {@code isDarkOutside})
     */
    public static Result ignite(ServerLevel level, BlockPos fire, @Nullable ServerPlayer player, @Nullable Boolean forcedNight) {
        if (!SimpleMobHeads.on() || !SimpleMobHeads.config().herobrineRitual) {
            return Result.DISABLED;
        }
        if (player == null) {
            return Result.NO_PLAYER;
        }
        BlockPos netherrack = fire.below();
        if (!level.getBlockState(fire).is(Blocks.FIRE) || !isShrine(level, netherrack)) {
            return Result.NO_SHRINE;
        }
        Vec3 flame = Vec3.atCenterOf(fire);
        if (!(forcedNight != null ? forcedNight : level.isDarkOutside())) {
            // Nobody answers by day: a puff of smoke, the fire burns on like any other.
            level.sendParticles(ParticleTypes.SMOKE, flame.x, flame.y, flame.z, 8, 0.2, 0.2, 0.2, 0.01);
            return Result.DAY;
        }
        if (!player.mayBuild() || !level.mayInteract(player, netherrack)) {
            return Result.NOT_ALLOWED;
        }
        long now = level.getGameTime();
        String shrine = level.dimension().identifier() + "@" + netherrack.asLong();
        if (SHRINE_READY.getOrDefault(shrine, Long.MIN_VALUE) > now || PLAYER_READY.getOrDefault(player.getUUID(), Long.MIN_VALUE) > now) {
            level.sendParticles(ParticleTypes.SOUL, flame.x, flame.y, flame.z, 2, 0.2, 0.2, 0.2, 0.01);
            return Result.COOLDOWN;
        }
        int cooldown = SimpleMobHeads.config().ritualCooldownTicks;
        SHRINE_READY.put(shrine, now + cooldown);
        PLAYER_READY.put(player.getUUID(), now + cooldown);

        BlockPos moss = netherrack.below();
        level.removeBlock(fire, false);
        for (Direction edge : EDGES) {
            level.removeBlock(moss.relative(edge), false);
        }
        level.setBlockAndUpdate(moss, Blocks.COBBLESTONE.defaultBlockState());
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, flame.x, flame.y - 0.3, flame.z, 24, 0.25, 0.3, 0.25, 0.02);
        level.sendParticles(ParticleTypes.SMOKE, flame.x, flame.y, flame.z, 12, 0.3, 0.3, 0.3, 0.01);
        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt != null) {
            bolt.setVisualOnly(true);
            bolt.snapTo(Vec3.atBottomCenterOf(moss.below().relative(Direction.Plane.HORIZONTAL.getRandomDirection(level.getRandom()), LIGHTNING_OFFSET)));
            level.addFreshEntity(bolt);
        }
        ItemEntity head = new ItemEntity(level, flame.x, flame.y - 0.4, flame.z, new ItemStack(MobHeads.item("herobrine")), 0.0, 0.0, 0.0);
        head.setDefaultPickUpDelay();
        level.addFreshEntity(head);
        HerobrineHaunt.sound(player, SoundEvents.AMBIENT_CAVE, SoundSource.AMBIENT, player.position(), 1.0F, 0.8F);
        if (SimpleMobHeads.config().hauntPhase) {
            HerobrineHaunt.start(player);
        }
        return Result.SUMMONED;
    }

    /** Whether the totem shrine stands under the netherrack at {@code netherrack} (only loaded blocks are read). */
    public static boolean isShrine(ServerLevel level, BlockPos netherrack) {
        if (!level.isLoaded(netherrack.below(2)) || !level.getBlockState(netherrack).is(Blocks.NETHERRACK)) {
            return false;
        }
        BlockPos moss = netherrack.below();
        if (!level.getBlockState(moss).is(Blocks.MOSSY_COBBLESTONE)) {
            return false;
        }
        for (Direction edge : EDGES) {
            BlockState torch = level.getBlockState(moss.relative(edge));
            if (!torch.is(Blocks.REDSTONE_TORCH)) {
                return false;
            }
        }
        BlockPos gold = moss.below();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!level.getBlockState(gold.offset(dx, 0, dz)).is(Blocks.GOLD_BLOCK)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** The hint book: "Herobrine Notes", three translated pages (title left blank so the item name is translated too). */
    public static ItemStack hintBook() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, content());
        book.set(DataComponents.ITEM_NAME, Component.translatable("book.simplemobheads.herobrine_notes.title"));
        return book;
    }

    private static WrittenBookContent content() {
        List<Filterable<Component>> pages = List.of(page(1), page(2), page(3));
        return new WrittenBookContent(Filterable.passThrough(""), "?", 3, pages, true);
    }

    private static Filterable<Component> page(int number) {
        return Filterable.passThrough(Component.translatable("book.simplemobheads.herobrine_notes." + number));
    }

    /** The mineshaft chest pool of the hint book (HeadDrops adds it when {@code hintBook} is on). */
    public static LootPool.Builder hintBookPool() {
        return LootPool.lootPool()
                .setRolls(ContextIntProviders.exactly(1))
                .add(LootItem.lootTableItem(Items.WRITTEN_BOOK)
                        .apply(SetComponentsFunction.setComponent(DataComponents.WRITTEN_BOOK_CONTENT, content()))
                        .apply(SetComponentsFunction.setComponent(DataComponents.ITEM_NAME,
                                (Component) Component.translatable("book.simplemobheads.herobrine_notes.title"))))
                .when(LootItemRandomChanceCondition.randomChance(HINT_BOOK_CHANCE));
    }

    /** Tests only: forget every ritual cooldown. */
    public static void resetForTests() {
        SHRINE_READY.clear();
        PLAYER_READY.clear();
    }
}

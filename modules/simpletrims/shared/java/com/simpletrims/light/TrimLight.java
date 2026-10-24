package com.simpletrims.light;

import com.simpletrims.SimpleTrims;
import com.simpletrims.TrimGlow;
import com.simplelib.api.OwnedLights;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Radiance (Emitting upgrade, moved from SimpleBuilding's {@code DynamicLightHandler}): worn emitting armor (or a torch, lantern or lava bucket in hand, N52) sets an
 * invisible light block at head height, an item frame with such a piece in its own block. Players: position in a table,
 * moves along every 2 ticks; armor stands, mobs and frames: position stored in the entity ({@link OwnedLightHolder}).
 * Light only replaces air, an existing light block or a water source (waterlogged then). Switched off ({@link #enabled})
 * every carrier sets level 0, so standing lights go away with the next tick of their carrier.
 */
public final class TrimLight {
    /** A standing light block with the dimension it was set in (a player may have left that dimension since). */
    private record Lit(ResourceKey<Level> dimension, BlockPos pos) {}
    private static final Map<UUID, Lit> PLAYER_LIGHTS = new HashMap<>();
    /** Lights whose chunk was unloaded when they should go (teleport away): removed once it loads again, never loaded just for that. */
    private static final List<Lit> STALE = new ArrayList<>();
    private static final int STALE_MAX = 1024;
    private static long staleSweep;
    /** Resting carriers check only every so many ticks (offset by entity id). */
    public static final int HOLDER_INTERVAL = 10;
    /** Walking carriers check more often so the light follows them. */
    public static final int MOB_INTERVAL = 4;
    public static final int PLAYER_INTERVAL = 2;
    /** Chance per tick and radiance level of a whole carrier: level 1 = one glint every 200 ticks on average. */
    public static final float MOTE_CHANCE_PER_LEVEL = 0.005f;
    /** Cap per carrier: at most one glint every 40 ticks on average. */
    public static final float MAX_MOTE_CHANCE = 0.025f;

    static {
        // After a crash the table is gone: only a player's light in PLAYER_LIGHTS is alive, resting carriers re-place theirs within HOLDER_INTERVAL ticks.
        OwnedLights.claim("simpletrims:light", (level, pos) -> PLAYER_LIGHTS.containsValue(new Lit(level.dimension(), pos)));
    }

    private TrimLight() {}

    public static boolean enabled() {
        return SimpleTrims.on();
    }

    /** 3 light points per level -> 5 levels = 15. */
    public static int lightLevelFor(int emissionPoints) {
        return Math.min(15, Math.max(0, emissionPoints) * 3);
    }

    public static int wornEmission(LivingEntity entity) {
        int total = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) total += TrimGlow.getEmissionLevel(entity.getItemBySlot(slot));
        }
        return total;
    }

    /**
     * Held light (N52, owner 2026-10-13): torches (also soul, copper, redstone), lanterns and lava buckets in the main or
     * off hand shine like the block (redstone torch weak, 7). Joins the radiance light with max, never adds.
     */
    public static int heldLight(LivingEntity entity) {
        return Math.max(itemLight(entity.getMainHandItem()), itemLight(entity.getOffhandItem()));
    }

    public static int itemLight(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String ns = id.getNamespace();
        String path = id.getPath();
        if (path.endsWith("lava_bucket") && (ns.equals("minecraft") || ns.equals("simplebuilding"))) {
            return path.contains("soul") ? 10 : 15;
        }
        if (!ns.equals("minecraft") || !(path.endsWith("torch") || path.endsWith("lantern"))) return 0;
        return Block.byItem(stack.getItem()).defaultBlockState().getLightEmission();
    }

    /** The light a carrier shines with: radiance of the worn armor or what it holds, whichever is brighter. */
    public static int lightOf(LivingEntity entity) {
        return Math.max(lightLevelFor(wornEmission(entity)), heldLight(entity));
    }

    /** Every tick from the entity tick mixin (server): players, armor stands and mobs. */
    public static void tickLiving(LivingEntity entity) {
        if (entity.level().isClientSide() || entity.isRemoved()) return;
        if (entity instanceof ServerPlayer player) {
            if (Math.floorMod(player.level().getGameTime(), PLAYER_INTERVAL) == 0) tickPlayer(player);
        } else if (entity instanceof OwnedLightHolder holder && !(entity instanceof Player)) {
            int interval = entity instanceof ArmorStand ? HOLDER_INTERVAL : MOB_INTERVAL;
            if (isHolderTick(entity, interval)) tickHolderLight(entity.level(), holder, entity.blockPosition().above(), lightOf(entity));
        }
    }

    public static void tickPlayer(Player player) {
        Level level = player.level();
        UUID uuid = player.getUUID();
        BlockPos head = player.blockPosition().above();
        int light = enabled() ? lightOf(player) : 0;
        sweepStale(level);
        Lit old = PLAYER_LIGHTS.get(uuid);
        if (old != null && !(old.pos().equals(head) && old.dimension().equals(level.dimension()))) {
            release(level, old);
            PLAYER_LIGHTS.remove(uuid);
            old = null;
        }
        if (light > 0) {
            if (placeOrUpdate(level, head, light, old != null)) PLAYER_LIGHTS.put(uuid, new Lit(level.dimension(), head));
        } else if (old != null) {
            release(level, old);
            PLAYER_LIGHTS.remove(uuid);
        }
    }

    /** Item frame: light in the frame's block, fed by the framed item. */
    public static void tickItemFrame(ItemFrame frame) {
        if (frame instanceof OwnedLightHolder holder && isHolderTick(frame, HOLDER_INTERVAL)) {
            tickHolder(frame.level(), holder, frame.getPos(), TrimGlow.getEmissionLevel(frame.getItem()));
        }
    }

    private static boolean isHolderTick(Entity entity, int interval) {
        return !entity.level().isClientSide() && !entity.isRemoved() && Math.floorMod(entity.level().getGameTime() + entity.getId(), interval) == 0;
    }

    public static void tickHolder(Level level, OwnedLightHolder holder, BlockPos target, int emissionPoints) {
        tickHolderLight(level, holder, target, lightLevelFor(emissionPoints));
    }

    public static void tickHolderLight(Level level, OwnedLightHolder holder, BlockPos target, int lightLevel) {
        int light = enabled() ? lightLevel : 0;
        BlockPos owned = holder.simpletrims$getOwnedLight();
        boolean ownsTarget = owned != null && owned.equals(target);
        if (owned != null && (light == 0 || !owned.equals(target))) {
            removeLight(level, owned);
            holder.simpletrims$setOwnedLight(null);
        }
        if (light > 0 && placeOrUpdate(level, target, light, ownsTarget)) holder.simpletrims$setOwnedLight(target);
    }

    /**
     * Sets or updates the light block - only in air, a water source or the carrier's own standing light block
     * ({@code ownsExisting}); foreign minecraft:light blocks (maps, adventure) are never touched (review F-04).
     */
    static boolean placeOrUpdate(Level level, BlockPos pos, int light, boolean ownsExisting) {
        BlockState current = level.getBlockState(pos);
        boolean water = current.getFluidState().is(FluidTags.WATER);
        if (!(current.isAir() || (ownsExisting && current.is(Blocks.LIGHT)) || (water && current.getFluidState().isSource()))) return false;
        if (!current.is(Blocks.LIGHT) || current.getValue(LightBlock.LEVEL) != light) {
            level.setBlock(pos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, light).setValue(LightBlock.WATERLOGGED, water), 3);
        }
        OwnedLights.mark(level, pos);
        return true;
    }

    /** Removes {@code lit} in its own dimension; an unloaded chunk is not loaded for it, the light waits in {@link #STALE}. */
    private static void release(Level level, Lit lit) {
        if (level.getServer() == null) return;
        ServerLevel target = level.getServer().getLevel(lit.dimension());
        if (target == null) return;
        if (target.hasChunkAt(lit.pos())) removeLight(target, lit.pos());
        else if (STALE.size() < STALE_MAX) STALE.add(lit);
    }

    /** Once a second (any player tick): lights whose chunk is loaded again go now. */
    private static void sweepStale(Level level) {
        if (STALE.isEmpty() || level.getServer() == null || level.getGameTime() - staleSweep < 20 && level.getGameTime() >= staleSweep) return;
        staleSweep = level.getGameTime();
        STALE.removeIf(lit -> {
            ServerLevel target = level.getServer().getLevel(lit.dimension());
            if (target == null) return true;
            if (!target.hasChunkAt(lit.pos())) return false;
            removeLight(target, lit.pos());
            return true;
        });
    }

    /** Pending lights (tests, server stop). */
    public static int staleCount() {
        return STALE.size();
    }

    private static void removeLight(Level level, BlockPos pos) {
        boolean ours = OwnedLights.isMarked(level, pos);
        OwnedLights.unmark(level, pos);
        BlockState state = level.getBlockState(pos);
        if (ours && state.is(Blocks.LIGHT)) {
            level.setBlock(pos, state.getValue(LightBlock.WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
        }
    }

    /** Server stop: the lights went with their world, drop the table. */
    public static void forgetAll() {
        PLAYER_LIGHTS.clear();
    }

    /** Drops a player's light and table entry (tests: mock players are never removed through the level). */
    public static void forget(Player player) {
        Lit lit = PLAYER_LIGHTS.remove(player.getUUID());
        if (lit != null) release(player.level(), lit);
    }

    /**
     * From {@code Entity#setRemoved} (HEAD, still in its old level). A player cleans up for every reason (also a
     * dimension change or disconnect); resting carriers only when really destroyed - unloading keeps their light and
     * its stored position.
     */
    public static void onEntityRemoved(Entity entity, Entity.RemovalReason reason) {
        Level level = entity.level();
        if (level == null || level.isClientSide()) return;
        if (entity instanceof ServerPlayer) {
            Lit lit = PLAYER_LIGHTS.remove(entity.getUUID());
            if (lit != null) release(level, lit);
        } else if (entity instanceof OwnedLightHolder holder) {
            BlockPos owned = holder.simpletrims$getOwnedLight();
            if (owned != null && reason.shouldDestroy()) {
                removeLight(level, owned);
                holder.simpletrims$setOwnedLight(null);
            }
        }
    }

    /** Fine warm glints on emitting pieces (client), rare on purpose and capped per carrier. Invisible carriers show nothing. */
    public static void tickGlowMotes(Entity entity, IntSupplier emissionPoints) {
        Level level = entity.level();
        if (!level.isClientSide() || !enabled() || (entity.isInvisible() && !(entity instanceof ItemFrame))) return;
        RandomSource random = entity.getRandom();
        float roll = random.nextFloat();
        if (roll >= MAX_MOTE_CHANCE || roll >= moteChance(emissionPoints.getAsInt())) return;
        double x;
        double y;
        double z;
        if (entity instanceof ItemFrame frame) {
            Direction face = frame.getDirection();
            x = frame.getX() + face.getStepX() * 0.15 + (random.nextDouble() - 0.5) * 0.6;
            y = frame.getY() + face.getStepY() * 0.15 + (random.nextDouble() - 0.5) * 0.6;
            z = frame.getZ() + face.getStepZ() * 0.15 + (random.nextDouble() - 0.5) * 0.6;
        } else {
            x = entity.getRandomX(0.6);
            y = entity.getRandomY();
            z = entity.getRandomZ(0.6);
        }
        level.addParticle(ParticleTypes.WAX_ON, x, y, z, 0.0, 0.01, 0.0);
    }

    public static float moteChance(int emissionPoints) {
        return Math.min(MAX_MOTE_CHANCE, MOTE_CHANCE_PER_LEVEL * Math.max(0, emissionPoints));
    }
}

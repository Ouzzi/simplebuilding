package com.simplelib.api.portal;

import com.simplelib.net.PortalPulsePayload;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Going through portal doors (BACK22 core §5.5, server side): every tick each player whose feet stand in the open doorway of a
 * portal door is moved to the other end - framed, link open, not riding/sleeping/gliding, at most once per {@link #COOLDOWN}
 * ticks. Arrival is in front of the same leaf of the target door, turned by the quarter turns between the two doors (look and
 * speed), so walking on feels seamless; the target chunk is loaded first (FIX172: the moved player joins a loaded entity section
 * at once). The rule decides the link state afterwards; the client gets a short fog pulse. Refusals say why in the action bar
 * (A14). Only players go through (A12: no item/animal logistics).
 */
public final class PortalTransit {
    public static final int COOLDOWN = 10, PULSE_TICKS = 4;
    /** Set by the loader entry: sends the fog pulse to a player (does nothing without a client channel). */
    public static BiConsumer<ServerPlayer, PortalPulsePayload> pulseSender = (player, payload) -> {};
    /** Called after every passage with the level left and the moved player (other mods: entries, progress, puzzles). */
    @FunctionalInterface
    public interface Listener {
        void moved(ServerLevel from, BlockPos door, ServerPlayer player);
    }

    private static final List<Listener> LISTENERS = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static final Map<UUID, Long> LAST = new HashMap<>();

    public static void listen(Listener listener) {
        LISTENERS.add(listener);
    }
    private static final Map<UUID, Long> HINTED = new HashMap<>();

    private PortalTransit() {}

    public static void tick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.players().isEmpty()) continue;
            for (ServerPlayer player : List.copyOf(level.players())) check(level, player);
        }
    }

    public static void forget(UUID player) {
        LAST.remove(player);
        HINTED.remove(player);
    }

    static void check(ServerLevel level, ServerPlayer player) {
        if (player.isSpectator() || player.isPassenger() || player.isVehicle() || player.isSleeping() || player.isFallFlying() || player.isRemoved()) return;
        BlockPos feet = player.blockPosition();
        BlockState state = level.getBlockState(feet);
        if (!(state.getBlock() instanceof PortalDoorBlock) || state.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER || !state.getValue(DoorBlock.OPEN)) return;
        transit(level, player, feet);
    }

    /** Moves {@code player} through the portal door at {@code pos}; returns the player at the other end, or null (refused). */
    public static @Nullable Entity transit(ServerLevel level, ServerPlayer player, BlockPos pos) {
        long now = level.getServer().getTickCount();
        Long last = LAST.get(player.getUUID());
        if (last != null && now - last < COOLDOWN) return null;
        PortalShape shape = PortalShape.of(level, pos);
        if (shape == null || !(level.getBlockState(shape.anchor()).getBlock() instanceof PortalDoorBlock door)) return null;
        if (!FrameRule.framed(level, shape)) {
            hint(player, now, PortalLook.UNFRAMED);
            return null;
        }
        PortalTarget target = door.resolve(level, shape);
        if (target == null) {
            hint(player, now, PortalLook.DORMANT);
            return null;
        }
        MinecraftServer server = level.getServer();
        PortalState linkState = PortalStates.get(server, target.linkId(), target.rule());
        if (!target.rule().canTransit(linkState)) {
            hint(player, now, PortalLook.of(linkState));
            return null;
        }
        ServerLevel to = server.getLevel(target.dimension());
        if (to == null) return null;
        int leaf = Math.min(shape.leaf(), target.width() - 1);
        BlockPos front = target.front(leaf);
        to.getChunk(front.getX() >> 4, front.getZ() >> 4);
        int quarters = quarters(shape.facing(), target.facing().getOpposite());
        Vec3 offset = player.position().subtract(Vec3.atBottomCenterOf(pos));
        Vec3 inside = rotate(new Vec3(offset.x, 0, offset.z), quarters);
        // keep the sideways offset inside the opening (not the depth: the traveller stands in front of the target door)
        Direction side = target.facing().getClockWise();
        double lateral = inside.x * side.getStepX() + inside.z * side.getStepZ();
        lateral = Math.max(-0.3, Math.min(0.3, lateral));
        Vec3 arrival = Vec3.atBottomCenterOf(front).add(side.getStepX() * lateral, 0, side.getStepZ() * lateral);
        Vec3 speed = rotate(player.getDeltaMovement(), quarters);
        float yaw = player.getYRot() + 90.0F * quarters;
        Entity moved = player.teleport(new TeleportTransition(to, arrival, speed, yaw, player.getXRot(), TeleportTransition.DO_NOTHING));
        if (moved == null) return null;
        LAST.put(moved.getUUID(), now);
        PortalState after = target.rule().afterTransit(linkState);
        if (after != linkState) {
            PortalStates.set(server, target.linkId(), target.rule(), after);
            if (after == PortalState.BROKEN) to.playSound(null, front, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.4F, 1.6F);
        }
        PortalDoorBlock.refresh(level, shape.anchor());
        PortalDoorBlock.refresh(to, target.anchor());
        to.playSound(null, front, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.5F, 0.7F);
        if (moved instanceof ServerPlayer p) {
            for (Listener l : LISTENERS) {
                try {
                    l.moved(level, pos, p);
                } catch (RuntimeException e) {
                    // a broken listener never stops the passage
                }
            }
            try {
                pulseSender.accept(p, new PortalPulsePayload(target.color(), PULSE_TICKS));
            } catch (RuntimeException e) {
                // a connection without the channel (test players): no pulse, the passage stands
            }
        }
        return moved;
    }

    /** Repair/activation of the link of {@code shape} (part or maintenance box): true when its state changed. */
    public static boolean mend(ServerLevel level, PortalShape shape, @Nullable Player by) {
        if (!(level.getBlockState(shape.anchor()).getBlock() instanceof PortalDoorBlock door)) return false;
        PortalTarget target = door.resolve(level, shape);
        if (target == null) return false;
        MinecraftServer server = level.getServer();
        PortalState now = PortalStates.get(server, target.linkId(), target.rule());
        PortalState next = target.rule().mend(now);
        if (next == null) return false;
        PortalStates.set(server, target.linkId(), target.rule(), next);
        PortalDoorBlock.refresh(level, shape.anchor());
        ServerLevel to = server.getLevel(target.dimension());
        if (to != null && to.isLoaded(target.anchor())) PortalDoorBlock.refresh(to, target.anchor());
        level.playSound(null, shape.anchor(), SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.8F, 1.2F);
        if (by instanceof ServerPlayer p) p.sendOverlayMessage(Component.translatable("message.simplelib.portal.mended"));
        return true;
    }

    private static void hint(ServerPlayer player, long now, PortalLook why) {
        Long last = HINTED.get(player.getUUID());
        if (last != null && now - last < 40) return;
        HINTED.put(player.getUUID(), now);
        player.sendOverlayMessage(Component.translatable("message.simplelib.portal." + why.getSerializedName()));
    }

    /** Quarter turns (clockwise seen from above, Minecraft yaw) from walking direction {@code from} to {@code to}. */
    public static int quarters(Direction from, Direction to) {
        return Math.floorMod(to.get2DDataValue() - from.get2DDataValue(), 4);
    }

    /** {@code v} turned by {@code quarters} clockwise quarter turns (Minecraft yaw: +90 turns south into west). */
    public static Vec3 rotate(Vec3 v, int quarters) {
        double x = v.x, z = v.z;
        for (int i = 0; i < Math.floorMod(quarters, 4); i++) {
            double nx = -z;
            z = x;
            x = nx;
        }
        return new Vec3(x, v.y, z);
    }
}

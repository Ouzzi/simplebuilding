package com.simplelib.api.portal;

import com.simplelib.SimpleLib;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Preloading the other side (BACK22 core §5.5): once a second every player looks for portal doors within {@link #RADIUS} blocks;
 * for each whose target lies in another dimension or a chunk that is not loaded, a {@code simplelib:portal} ticket (loading,
 * 200 ticks, renewed while the player stays near) generates/keeps the target chunk, so stepping through never waits. Doors of
 * the Backrooms stack sit in the same chunk as their partner, so for them this does nothing.
 */
public final class PortalPreload {
    public static final int RADIUS = 8, HEIGHT = 3, PERIOD = 20;
    public static TicketType TICKET;

    private PortalPreload() {}

    /** Registers the ticket type (loader entry, while registries are open). */
    public static void register() {
        if (TICKET == null) TICKET = Registry.register(BuiltInRegistries.TICKET_TYPE, SimpleLib.id("portal"),
                new TicketType(200L, TicketType.FLAG_LOADING | TicketType.FLAG_KEEP_DIMENSION_ACTIVE));
    }

    public static void tick(MinecraftServer server) {
        if (TICKET == null || server.getTickCount() % PERIOD != 0) return;
        for (ServerLevel level : server.getAllLevels()) {
            for (ServerPlayer player : level.players()) scan(level, player.blockPosition());
        }
    }

    /** Tickets for the targets of all portal doors near {@code centre}; returns how many doors were found. */
    public static int scan(ServerLevel level, BlockPos centre) {
        int found = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                for (int dy = -HEIGHT; dy <= HEIGHT; dy++) {
                    p.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
                    BlockState s = level.getBlockState(p);
                    if (!(s.getBlock() instanceof PortalDoorBlock door) || s.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER) continue;
                    PortalShape shape = PortalShape.of(level, p);
                    if (shape == null || shape.leaf() != 0) continue;
                    PortalTarget target = door.resolve(level, shape);
                    if (target == null) continue;
                    found++;
                    preload(level.getServer(), target);
                }
            }
        }
        return found;
    }

    /** Adds the ticket for the chunk in front of {@code target} (no-op when the dimension is missing). */
    public static void preload(MinecraftServer server, PortalTarget target) {
        ServerLevel to = server.getLevel(target.dimension());
        if (to == null || TICKET == null) return;
        BlockPos front = target.front(0);
        to.getChunkSource().addTicketWithRadius(TICKET, new ChunkPos(front.getX() >> 4, front.getZ() >> 4), 1);
    }
}

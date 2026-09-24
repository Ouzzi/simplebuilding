package com.simplebuilding.neoforge.clienttest;

import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * A real "everything the server sent has arrived" barrier for the NeoForge driver - the NeoForge
 * half of what Fabric's client test framework gives {@code Script.awaitPackets} for free. Ported
 * unchanged from the 26.2 line (same ping packet, same {@code handlePing}, same server calls on
 * 1.21.11); the races below were seen there, and this driver had the same instant "yes".
 *
 * <p>Until 2026-09-24 {@code packetsSettled} answered "yes" straight away and left the waiting to
 * the idle steps around it. That was a race, and on a loaded machine it lost: the sledgehammer
 * scene swapped the off hand to enderite nuggets, "waited for the packets" in zero ticks and
 * asked the hint predicate while the client still held the netherite ones; and the octant's
 * Control+scroll payload had not been applied by the lagging integrated server when the ten idle
 * ticks were up (the aim step right before it already needed 14 ticks instead of 0).
 *
 * <p>The barrier has two legs, both in order-preserving channels:
 * <ol>
 *   <li><b>Two full server ticks.</b> A command runs between ticks, but the slot it changed only
 *       goes out in the next tick's {@code broadcastChanges}; and a payload the client sent before
 *       the barrier is handled by the server's packet processor within those ticks. Skipped while
 *       the integrated server is paused - it does not tick then, and nothing is sent either.</li>
 *   <li><b>A ping round.</b> Then the server sends a {@link ClientboundPingPacket} with a fresh id.
 *       The client handles play packets in arrival order on the render thread, so when
 *       {@code PingHandledMixin} has seen that id, everything sent before it has been handled.</li>
 * </ol>
 */
public final class PacketBarrier {

    /** Ids far away from anything vanilla sends in play, so a stray ping cannot open the barrier. */
    private static final AtomicInteger NEXT_ID = new AtomicInteger(0x5B000000);
    private static final AtomicInteger LAST_HANDLED = new AtomicInteger(Integer.MIN_VALUE);

    private enum Stage { TICKS, PING, DONE }

    private Stage stage = Stage.TICKS;
    private int targetTick = Integer.MIN_VALUE;
    private int pingId;

    /** Called by the mixin on the render thread for every handled ping. */
    public static void pingHandled(int id) {
        LAST_HANDLED.set(id);
    }

    /** One poll, on the render thread. True once the barrier has been passed. */
    boolean poll() {
        Minecraft client = Minecraft.getInstance();
        MinecraftServer server = client.getSingleplayerServer();
        if (server == null) {
            // Nothing local to wait for; the scripts only ever run in a singleplayer world.
            return true;
        }

        if (stage == Stage.TICKS) {
            boolean paused = client.isPaused() || server.isPaused();
            if (targetTick == Integer.MIN_VALUE) {
                targetTick = server.getTickCount() + 2;
            }
            if (!paused && server.getTickCount() < targetTick) {
                return false;
            }
            pingId = NEXT_ID.incrementAndGet();
            int id = pingId;
            server.execute(() -> {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    player.connection.send(new ClientboundPingPacket(id));
                }
            });
            stage = Stage.PING;
            return false;
        }

        if (stage == Stage.PING) {
            if (LAST_HANDLED.get() >= pingId) {
                stage = Stage.DONE;
            } else {
                return false;
            }
        }
        return true;
    }
}

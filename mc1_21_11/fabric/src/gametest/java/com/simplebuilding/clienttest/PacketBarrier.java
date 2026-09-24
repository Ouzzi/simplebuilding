package com.simplebuilding.clienttest;

import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * A real "everything the server sent has arrived" barrier for the Fabric 1.21.11 driver.
 *
 * <p>The 26.2 line gets this from Fabric itself ({@code getConnection().waitForClientboundPackets()}).
 * fabric-client-gametest-api-v1 4.3.5, which the 1.21.11 line runs against, has neither, and until
 * 2026-09-24 {@code packetsSettled} answered "yes" straight away and left the waiting to the idle
 * steps around it. That is the race the NeoForge 26.2 driver lost twice in one run on a loaded
 * machine (an off hand swap read before it had arrived, a scroll payload not yet applied by the
 * server). This is the same barrier that driver builds - see its {@code PacketBarrier}.
 *
 * <p>Two legs, both in order-preserving channels:
 * <ol>
 *   <li><b>Two full server ticks.</b> A command runs between ticks, but the slot it changed only
 *       goes out in the next tick's {@code broadcastChanges}; and a payload the client sent before
 *       the barrier is handled by the server's packet processor within those ticks. Skipped while
 *       the integrated server is paused - it does not tick then, and nothing is sent either.</li>
 *   <li><b>A ping round.</b> Then the server sends a {@link ClientboundPingPacket} with a fresh id.
 *       The client handles play packets in arrival order on its own thread, so when
 *       {@code PingHandledMixin} has seen that id, everything sent before it has been handled.</li>
 * </ol>
 *
 * <p>{@link #poll()} touches {@code Minecraft.getInstance()} and must therefore run inside
 * {@code computeOnClient} - Fabric forbids it on the test thread.
 */
public final class PacketBarrier {

    /** Ids far away from anything vanilla sends in play, so a stray ping cannot open the barrier. */
    private static final AtomicInteger NEXT_ID = new AtomicInteger(0x5B000000);
    private static final AtomicInteger LAST_HANDLED = new AtomicInteger(Integer.MIN_VALUE);

    private enum Stage { TICKS, PING, DONE }

    private Stage stage = Stage.TICKS;
    private int targetTick = Integer.MIN_VALUE;
    private int pingId;

    /** Called by the mixin on the client thread for every handled ping. */
    public static void pingHandled(int id) {
        LAST_HANDLED.set(id);
    }

    /** One poll, on the client thread. True once the barrier has been passed. */
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

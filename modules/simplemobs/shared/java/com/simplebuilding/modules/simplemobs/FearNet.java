package com.simplebuilding.modules.simplemobs;

import io.netty.buffer.ByteBuf;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * The client's FOV for the Hollow Watcher's gaze zones (loader-neutral hooks like Simple Tweaks' deed packets): a client
 * near a Watcher reports its FOV (rate-limited, only on change); the server clamps it and falls back to 70 without it.
 */
public final class FearNet {
    /** Client to server: the player's FOV setting in degrees. */
    public record FovPayload(int fov) implements CustomPacketPayload {
        public static final Type<FovPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("simplemobs", "fov"));
        public static final StreamCodec<ByteBuf, FovPayload> CODEC = ByteBufCodecs.VAR_INT.map(FovPayload::new, FovPayload::fov).cast();
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static final Map<UUID, Integer> FOV = new ConcurrentHashMap<>();
    /** Installed by each loader's client side; the default drops the packet (no channel). */
    public static volatile Consumer<CustomPacketPayload> toServer = payload -> { };
    /** Installed by the client renderer: the FOV option, or 0 when unknown. */
    public static volatile IntSupplier fovSupplier = () -> 0;
    private static int lastSent;
    private static long lastTime = Long.MIN_VALUE / 2;

    private FearNet() {}

    /** Server: the player's reported FOV, clamped; 70 if none arrived. */
    public static int fov(ServerPlayer p) { return FOV.getOrDefault(p.getUUID(), FearLogic.FOV_REF); }

    public static void handle(FovPayload payload, ServerPlayer p) { FOV.put(p.getUUID(), FearLogic.clampFov(payload.fov())); }

    /** Test hook. */
    public static void set(ServerPlayer p, int fov) { FOV.put(p.getUUID(), FearLogic.clampFov(fov)); }

    public static void forget(ServerPlayer p) { FOV.remove(p.getUUID()); }

    /** Client: called from a Watcher's client tick; sends the FOV when it changed (at most every 2 s, resent every 30 s). */
    public static void report(long gameTime) {
        int fov = fovSupplier.getAsInt();
        if (fov <= 0 || gameTime - lastTime < 40 || fov == lastSent && gameTime - lastTime < 600) return;
        lastTime = gameTime;
        lastSent = fov;
        try { toServer.accept(new FovPayload(fov)); } catch (RuntimeException ignored) { }
    }
}

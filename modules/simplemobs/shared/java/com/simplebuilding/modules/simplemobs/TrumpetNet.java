package com.simplebuilding.modules.simplemobs;

import io.netty.buffer.ByteBuf;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Trumpet network (TROMP-3, owner 05.11.): the tone is a client-side loop, so the server only says "player X holds note N" (start or
 * change) and "player X stopped" to everyone within {@link #RADIUS} blocks. A change is sent only when the note differs from the last
 * one sent, at most every {@link #MIN_GAP} ticks; the current note is repeated every {@link #REFRESH} ticks for players who walk into range.
 */
public final class TrumpetNet {
    public static final double RADIUS = 32.0;
    public static final int MIN_GAP = 2, REFRESH = 40, STOP = -1;

    /** Server to client: note 0..24 of the entity's trumpet (start or change), {@link #STOP} when it stops. */
    public record TonePayload(int entityId, int note) implements CustomPacketPayload {
        public static final Type<TonePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("simplemobs", "trumpet_tone"));
        public static final StreamCodec<ByteBuf, TonePayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, TonePayload::entityId, ByteBufCodecs.VAR_INT, TonePayload::note, TonePayload::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private record Sent(int note, long tick) {}

    private static final Map<UUID, Sent> SENT = new ConcurrentHashMap<>();
    /** Installed by the loader's setup; the default drops the packet. */
    public static volatile BiConsumer<ServerPlayer, CustomPacketPayload> sender = (player, payload) -> { };

    private TrumpetNet() {}

    /** Called every use tick: sends start/change to the players in range when due. Returns true if the note was (re)sent. */
    public static boolean update(Collection<ServerPlayer> players, LivingEntity user, int note, long tick) {
        Sent last = SENT.get(user.getUUID());
        boolean due = last == null || last.note != note && tick - last.tick >= MIN_GAP || tick - last.tick >= REFRESH;
        if (!due) return false;
        SENT.put(user.getUUID(), new Sent(note, tick));
        broadcast(players, user, note);
        return true;
    }

    /** The trumpet stops (released, switched, died): everyone in range silences it at once. */
    public static void stop(Collection<ServerPlayer> players, LivingEntity user) {
        if (SENT.remove(user.getUUID()) != null) broadcast(players, user, STOP);
    }

    public static void forget(ServerPlayer p) { SENT.remove(p.getUUID()); }

    public static void forgetAll() { SENT.clear(); }

    private static void broadcast(Collection<ServerPlayer> players, LivingEntity user, int note) {
        TonePayload payload = new TonePayload(user.getId(), note);
        for (ServerPlayer p : players) {
            if (p.level() == user.level() && p.distanceToSqr(user) <= RADIUS * RADIUS * 1.5) sender.accept(p, payload);
        }
    }
}

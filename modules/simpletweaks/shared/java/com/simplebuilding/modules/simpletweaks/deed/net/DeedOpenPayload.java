package com.simplebuilding.modules.simpletweaks.deed.net;

import com.simplebuilding.modules.simpletweaks.claims.ClaimCode;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server to client: open the deed screen. Everything in it is a snapshot for display; the server re-checks every
 * answer. {@code code} is only sent to someone who may edit it (owner or manager); a plain reader gets {@code rights}.
 * {@code note}: "" or a reason the editor is read-only ({@code locked}, {@code too_complex}, {@code no_land}).
 */
public record DeedOpenPayload(int mode, int hand, UUID ownerId, String owner, String code, int revision, int rights,
                              int landChunks, int maxChunks, int maxPeople, int chunkX, int chunkZ, String dimension,
                              String note, List<String> online, List<String> known) implements CustomPacketPayload {
    public static final int CLAIM = 0, EDIT = 1, READ = 2;
    public static final int MAX_NAMES = 128;
    public static final Type<DeedOpenPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("simpletweaks", "deed_open"));
    private static final StreamCodec<ByteBuf, List<String>> NAMES = ByteBufCodecs.stringUtf8(16).apply(ByteBufCodecs.list(MAX_NAMES));
    public static final StreamCodec<ByteBuf, DeedOpenPayload> CODEC = StreamCodec.of(DeedOpenPayload::write, DeedOpenPayload::read);

    private static void write(ByteBuf b, DeedOpenPayload p) {
        ByteBufCodecs.VAR_INT.encode(b, p.mode); ByteBufCodecs.VAR_INT.encode(b, p.hand);
        UUIDUtil.STREAM_CODEC.encode(b, p.ownerId); ByteBufCodecs.stringUtf8(16).encode(b, p.owner);
        ByteBufCodecs.stringUtf8(ClaimCode.MAX_CHARS).encode(b, p.code);
        ByteBufCodecs.INT.encode(b, p.revision); ByteBufCodecs.VAR_INT.encode(b, p.rights);
        ByteBufCodecs.VAR_INT.encode(b, p.landChunks); ByteBufCodecs.VAR_INT.encode(b, p.maxChunks); ByteBufCodecs.VAR_INT.encode(b, p.maxPeople);
        ByteBufCodecs.INT.encode(b, p.chunkX); ByteBufCodecs.INT.encode(b, p.chunkZ);
        ByteBufCodecs.stringUtf8(128).encode(b, p.dimension); ByteBufCodecs.stringUtf8(32).encode(b, p.note);
        NAMES.encode(b, p.online); NAMES.encode(b, p.known);
    }
    private static DeedOpenPayload read(ByteBuf b) {
        return new DeedOpenPayload(ByteBufCodecs.VAR_INT.decode(b), ByteBufCodecs.VAR_INT.decode(b),
                UUIDUtil.STREAM_CODEC.decode(b), ByteBufCodecs.stringUtf8(16).decode(b),
                ByteBufCodecs.stringUtf8(ClaimCode.MAX_CHARS).decode(b), ByteBufCodecs.INT.decode(b), ByteBufCodecs.VAR_INT.decode(b),
                ByteBufCodecs.VAR_INT.decode(b), ByteBufCodecs.VAR_INT.decode(b), ByteBufCodecs.VAR_INT.decode(b),
                ByteBufCodecs.INT.decode(b), ByteBufCodecs.INT.decode(b), ByteBufCodecs.stringUtf8(128).decode(b),
                ByteBufCodecs.stringUtf8(32).decode(b), NAMES.decode(b), NAMES.decode(b));
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

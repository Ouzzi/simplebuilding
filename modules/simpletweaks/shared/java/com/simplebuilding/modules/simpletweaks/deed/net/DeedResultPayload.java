package com.simplebuilding.modules.simpletweaks.deed.net;

import com.simplebuilding.modules.simpletweaks.claims.ClaimCode;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server to client: answer to a save. At most {@link ClaimCode#MAX_ERRORS} errors, each with its source line. */
public record DeedResultPayload(boolean ok, int revision, List<Err> errors) implements CustomPacketPayload {
    public record Err(int line, String key, String arg) {
        static final StreamCodec<ByteBuf, Err> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Err::line, ByteBufCodecs.stringUtf8(32), Err::key, ByteBufCodecs.stringUtf8(64), Err::arg, Err::new);
    }
    public static final Type<DeedResultPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("simpletweaks", "deed_result"));
    public static final StreamCodec<ByteBuf, DeedResultPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, DeedResultPayload::ok, ByteBufCodecs.INT, DeedResultPayload::revision,
            Err.CODEC.apply(ByteBufCodecs.list(ClaimCode.MAX_ERRORS)), DeedResultPayload::errors, DeedResultPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

package com.simplelib.net;

import com.simplelib.SimpleLib;
import com.simplelib.api.ModpackGuide;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server to client (b-guide01): the asked chapter advancements the reader has completed ({@link ModpackGuideQueryPayload}). */
public record ModpackGuideStatePayload(List<Identifier> done) implements CustomPacketPayload {
    public static final Type<ModpackGuideStatePayload> TYPE = new Type<>(SimpleLib.id("modpack_guide_state"));
    public static final StreamCodec<ByteBuf, ModpackGuideStatePayload> CODEC = Identifier.STREAM_CODEC
            .apply(ByteBufCodecs.list(ModpackGuide.MAX_QUERY)).map(ModpackGuideStatePayload::new, ModpackGuideStatePayload::done);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

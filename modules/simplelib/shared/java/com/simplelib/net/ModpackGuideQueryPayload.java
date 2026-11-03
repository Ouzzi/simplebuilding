package com.simplelib.net;

import com.simplelib.SimpleLib;
import com.simplelib.api.ModpackGuide;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client to server (b-guide01): the Simple Modpack guide opened and asks which of its chapter advancements the reader has
 * ({@link ModpackGuide#done}); the server answers with {@link ModpackGuideStatePayload}. At most {@link ModpackGuide#MAX_QUERY} ids.
 */
public record ModpackGuideQueryPayload(List<Identifier> advancements) implements CustomPacketPayload {
    public static final Type<ModpackGuideQueryPayload> TYPE = new Type<>(SimpleLib.id("modpack_guide_query"));
    public static final StreamCodec<ByteBuf, ModpackGuideQueryPayload> CODEC = Identifier.STREAM_CODEC
            .apply(ByteBufCodecs.list(ModpackGuide.MAX_QUERY)).map(ModpackGuideQueryPayload::new, ModpackGuideQueryPayload::advancements);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

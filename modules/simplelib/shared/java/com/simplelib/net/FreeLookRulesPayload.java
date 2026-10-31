package com.simplelib.net;

import com.simplelib.SimpleLib;
import com.simplelib.config.LibConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client (IG31-29): the server's free look release mode ({@code playerTurns} = the player turns to the camera, else
 * the camera eases back) and whether the server enforces it over the client's own choice. Sent on join and after every save
 * of {@code simplelib-server.json}; the client keeps it until it leaves.
 */
public record FreeLookRulesPayload(boolean playerTurns, boolean enforce) implements CustomPacketPayload {
    public static final Type<FreeLookRulesPayload> TYPE = new Type<>(SimpleLib.id("free_look_rules"));
    public static final StreamCodec<ByteBuf, FreeLookRulesPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, FreeLookRulesPayload::playerTurns, ByteBufCodecs.BOOL, FreeLookRulesPayload::enforce, FreeLookRulesPayload::new);

    /** The rules of the current server config. */
    public static FreeLookRulesPayload current() {
        return new FreeLookRulesPayload(LibConfig.freeLookPlayerTurns(LibConfig.freeLookRelease), LibConfig.freeLookEnforce);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package com.simplelib.net;

import com.simplelib.SimpleLib;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: the {@code in_world_upgrades} files of the server's datapacks (file id -> JSON text), sent on join
 * and after every {@code /reload} ({@link com.simplelib.api.InWorldUpgrades#syncFiles}); the client applies them with
 * {@link com.simplelib.api.InWorldUpgrades#receive}. Not sent while a partner syncs the table itself.
 */
public record UpgradeTablePayload(Map<String, String> files) implements CustomPacketPayload {
    public static final Type<UpgradeTablePayload> TYPE = new Type<>(SimpleLib.id("in_world_upgrades_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UpgradeTablePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.stringUtf8(1024), ByteBufCodecs.stringUtf8(1 << 20)), UpgradeTablePayload::files,
            UpgradeTablePayload::new);

    /** The payload for the current files, or null when there is nothing to send. */
    public static UpgradeTablePayload current() {
        Map<String, String> files = com.simplelib.api.InWorldUpgrades.syncFiles();
        return files.isEmpty() ? null : new UpgradeTablePayload(files);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

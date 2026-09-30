package com.simplebuilding.modules.simplemodels;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
public record CataloguePayload(String json) implements CustomPacketPayload {
    public static final Type<CataloguePayload> ID = new Type<>(Identifier.fromNamespaceAndPath("simplemodels", "catalogue"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CataloguePayload> CODEC = StreamCodec.of(
            (buf, p) -> buf.writeUtf(p.json, ModelCatalogue.MAX_SNAPSHOT_CHARS),
            buf -> new CataloguePayload(buf.readUtf(ModelCatalogue.MAX_SNAPSHOT_CHARS)));
    public static CataloguePayload current() { return new CataloguePayload(ModelCatalogue.GSON.toJson(Models.server)); }
    public Type<? extends CustomPacketPayload> type() { return ID; }
}

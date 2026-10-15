package com.simplebuilding.modules.simpletweaks.deed;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * The binding of a claim deed to its owner ({@code simpletweaks:deed}). <b>Display only, never an authority</b>:
 * the server ledger decides who may do what; a forged or copied component only chooses whose land the deed names.
 * {@code generation}: 0 = the original, 1 = a copy (copies of copies are not possible).
 */
public record DeedData(UUID owner, String ownerName, int generation) {
    public static final int MAX_NAME = 16, MAX_GENERATION = 1;
    public static final Codec<DeedData> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("owner").forGetter(DeedData::owner),
            Codec.string(0, MAX_NAME).fieldOf("owner_name").forGetter(DeedData::ownerName),
            Codec.intRange(0, MAX_GENERATION).optionalFieldOf("generation", 0).forGetter(DeedData::generation)
    ).apply(i, DeedData::new));
    public static final StreamCodec<io.netty.buffer.ByteBuf, DeedData> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, DeedData::owner, ByteBufCodecs.stringUtf8(MAX_NAME), DeedData::ownerName,
            ByteBufCodecs.VAR_INT, DeedData::generation, DeedData::new);
    public static final DataComponentType<DeedData> TYPE = DataComponentType.<DeedData>builder()
            .persistent(CODEC).networkSynchronized(STREAM_CODEC).build();

    public DeedData {
        if (owner == null || ownerName == null || ownerName.length() > MAX_NAME || generation < 0 || generation > MAX_GENERATION)
            throw new IllegalArgumentException("Deed data");
    }

    public DeedData copy() { return new DeedData(owner, ownerName, generation + 1); }
    public boolean copyable() { return generation < MAX_GENERATION; }

    public static void register() {
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath("simpletweaks", "deed"), TYPE);
    }
}

package com.simplebuilding.version;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;

/**
 * Block codecs, 26.3 side of the version shim. MC 26.3 has no block codecs any more; the blocks
 * still declare their CODEC constants for 26.2, and these placeholders keep that code compiling. They
 * are never encoded or decoded on 26.3 (nothing asks a block for a codec).
 */
public abstract class BlockCodecs extends Block {

    private BlockCodecs() {
        super(null);
    }

    public static <B extends Block> MapCodec<B> simple(Function<BlockBehaviour.Properties, B> constructor) {
        return MapCodec.unit(() -> {
            throw new UnsupportedOperationException("MC 26.3 has no block codecs");
        });
    }

    public static <B extends Block> RecordCodecBuilder<B, BlockBehaviour.Properties> propertiesField() {
        return MapCodec.<BlockBehaviour.Properties>unit(() -> {
            throw new UnsupportedOperationException("MC 26.3 has no block codecs");
        }).forGetter(block -> null);
    }
}

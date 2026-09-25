package com.simplebuilding.version;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;

/**
 * Block codecs, 26.2 side of the version shim (see {@link McVersion}). 26.2 requires every block
 * type to expose a MapCodec via {@code codec()}; 26.3 removed block codecs altogether, so its twin
 * hands out inert placeholders and the blocks' {@code codec()} methods carry no {@code @Override}.
 *
 * <p>Extends Block only to reach the protected {@code BlockBehaviour.propertiesCodec()}; it is never
 * instantiated.
 */
public abstract class BlockCodecs extends Block {

    private BlockCodecs() {
        super(null);
    }

    public static <B extends Block> MapCodec<B> simple(Function<BlockBehaviour.Properties, B> constructor) {
        return BlockBehaviour.simpleCodec(constructor);
    }

    public static <B extends Block> RecordCodecBuilder<B, BlockBehaviour.Properties> propertiesField() {
        return BlockBehaviour.propertiesCodec();
    }
}

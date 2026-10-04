package com.simplesandwiches.sandwich;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;

/**
 * What a sandwich is made of: ingredient item ids from bottom to top (only ids, never the
 * ingredient stacks' own components) and whether the bread was buttered first. Equal contents give
 * equal derived components, so equal sandwiches stack and different ones never do.
 */
public record SandwichContents(List<Holder<Item>> ingredients, boolean buttered, int formula) {
    public static final int MAX = 5;
    public static final int FORMULA = 1;
    public static final SandwichContents EMPTY = new SandwichContents(List.of(), false, FORMULA);

    public static final Codec<SandwichContents> CODEC = RecordCodecBuilder.create(i -> i.group(
            Item.CODEC.listOf(0, MAX).optionalFieldOf("ingredients", List.of()).forGetter(SandwichContents::ingredients),
            Codec.BOOL.optionalFieldOf("buttered", false).forGetter(SandwichContents::buttered),
            Codec.INT.optionalFieldOf("formula", FORMULA).forGetter(SandwichContents::formula)
    ).apply(i, SandwichContents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SandwichContents> STREAM_CODEC = StreamCodec.composite(
            Item.STREAM_CODEC.apply(ByteBufCodecs.list(MAX)), SandwichContents::ingredients,
            ByteBufCodecs.BOOL, SandwichContents::buttered,
            ByteBufCodecs.VAR_INT, SandwichContents::formula,
            SandwichContents::new);

    public SandwichContents {
        ingredients = List.copyOf(ingredients.size() > MAX ? ingredients.subList(0, MAX) : ingredients);
    }

    public SandwichContents(List<Holder<Item>> ingredients, boolean buttered) {
        this(ingredients, buttered, FORMULA);
    }
}

package com.simplebuilding.modules.simplemoney.forge;
import com.simplemoney.SimpleMoney;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;


import net.minecraftforge.common.crafting.conditions.ICondition;

public record MoneyCondition(String flag) implements ICondition {
    public static final String ENABLE_VILLAGER_TRADES = "enableVillagerTrades";
    public static final String ENABLE_WANDERING_TRADES = "enableWanderingTrades";

    public static final MapCodec<MoneyCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("flag").forGetter(MoneyCondition::flag)
    ).apply(instance, MoneyCondition::new));

    @Override
    public boolean test(IContext context, com.mojang.serialization.DynamicOps<?> ops) {
        return SimpleMoney.enabled(this.flag);
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    @Override
    public String toString() {
        return "simplemoney:config(\"" + this.flag + "\")";
    }
}

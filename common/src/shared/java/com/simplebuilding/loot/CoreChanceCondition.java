package com.simplebuilding.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * Loot condition {@code simplebuilding:core_chance}: passes with probability {@code chance} times
 * the config factor {@code worldGen.buildingCoreLootChanceMultiplier} (at most 1), read when the
 * pool is rolled. The building core pools of the mod's loot injection tables use it, so a datapack
 * can change the base chance in {@code data/simplebuilding/loot_table/inject/...} and the server
 * config still scales it.
 */
public record CoreChanceCondition(float chance) implements LootItemCondition {

    public static final MapCodec<CoreChanceCondition> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.FLOAT.fieldOf("chance").forGetter(CoreChanceCondition::chance)
    ).apply(instance, CoreChanceCondition::new));

    @Override
    public MapCodec<CoreChanceCondition> codec() {
        return MAP_CODEC;
    }

    @Override
    public boolean test(LootContext context) {
        return context.getRandom().nextFloat() < ModLootTableModifications.coreChance(this.chance);
    }

    public static LootItemCondition.Builder coreChance(float chance) {
        return () -> new CoreChanceCondition(chance);
    }
}

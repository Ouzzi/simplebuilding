package com.simplebuilding.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * {@code simplebuilding:counter}, MC 26.3 side (see the 26.2 twin in common/src/mc26_2/java):
 * 26.3 stores the player condition of a criterion as a {@code Holder<LootItemCondition>}.
 */
public final class CounterTrigger extends SimpleCriterionTrigger<CounterTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    /** Fires every criterion of this player that waits for {@code counter} and whose threshold {@code value} reached. */
    public void trigger(ServerPlayer player, String counter, long value) {
        this.trigger(player, instance -> instance.counter().equals(counter) && value >= instance.min());
    }

    /** A criterion that is met once the player's {@code counter} reached {@code min}. */
    public Criterion<TriggerInstance> reached(String counter, long min) {
        return this.createCriterion(new TriggerInstance(Optional.empty(), counter, min));
    }

    public record TriggerInstance(Optional<Holder<LootItemCondition>> player, String counter, long min)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                LootItemCondition.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                Codec.STRING.fieldOf("counter").forGetter(TriggerInstance::counter),
                Codec.LONG.fieldOf("min").forGetter(TriggerInstance::min)
        ).apply(instance, TriggerInstance::new));
    }
}

package com.simplebuilding.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code simplebuilding:counter} - waits until one of the player's own counts ({@link ModCounters})
 * reaches {@code min}. 1.21.11 copy of the 26.x class (common/src/mc26_2/java).
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

    public record TriggerInstance(Optional<ContextAwarePredicate> player, String counter, long min)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                Codec.STRING.fieldOf("counter").forGetter(TriggerInstance::counter),
                Codec.LONG.fieldOf("min").forGetter(TriggerInstance::min)
        ).apply(instance, TriggerInstance::new));
    }
}

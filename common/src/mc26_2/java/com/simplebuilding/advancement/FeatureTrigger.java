package com.simplebuilding.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code simplebuilding:feature_used} - one advancement trigger for every mod action that no vanilla
 * trigger sees (a finished hammer upgrade, a chisel step, a wand build, a pad that launched you ...).
 * The criterion names the action in its {@code feature} field; the names live in {@link ModTriggers}.
 *
 * <p>MC 26.2 side: the player condition is a {@code ContextAwarePredicate}. Twin in
 * mc26_3/overlay/java (26.3 keeps a {@code Holder<LootItemCondition>} there instead).
 */
public final class FeatureTrigger extends SimpleCriterionTrigger<FeatureTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    /** Fires every criterion of this player that waits for exactly this feature. */
    public void trigger(ServerPlayer player, String feature) {
        this.trigger(player, instance -> instance.feature().equals(feature));
    }

    /** A criterion that is met once the player used {@code feature}. */
    public Criterion<TriggerInstance> used(String feature) {
        return this.createCriterion(new TriggerInstance(Optional.empty(), feature));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, String feature)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                Codec.STRING.fieldOf("feature").forGetter(TriggerInstance::feature)
        ).apply(instance, TriggerInstance::new));
    }
}

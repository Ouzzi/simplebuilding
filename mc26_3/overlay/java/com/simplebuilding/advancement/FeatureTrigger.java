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
 * {@code simplebuilding:feature_used}, MC 26.3 side (see the 26.2 twin in common/src/mc26_2/java):
 * 26.3 stores the player condition of a criterion as a {@code Holder<LootItemCondition>}.
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

    public record TriggerInstance(Optional<Holder<LootItemCondition>> player, String feature)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                LootItemCondition.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                Codec.STRING.fieldOf("feature").forGetter(TriggerInstance::feature)
        ).apply(instance, TriggerInstance::new));
    }
}

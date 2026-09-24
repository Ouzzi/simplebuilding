package com.simplebuilding.forge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.config.SimplebuildingConfig;
import net.minecraftforge.common.crafting.conditions.ICondition;

/**
 * Forge counterpart of the Fabric resource condition {@code simplebuilding:config}
 * ({@code src/main/java/com/simplebuilding/condition/ConfigResourceCondition.java}) and of the
 * NeoForge {@code ConfigLoadCondition}.
 *
 * <p>Registered under the same name {@code simplebuilding:config}, in Forge's registry
 * {@code forge:condition_codecs}. The trade jsons therefore carry a third key next to
 * {@code "fabric:load_conditions"} and {@code "neoforge:conditions"}: Forge 65 reads a single
 * condition object under {@code "forge:condition"} ({@code ICondition.DEFAULT_FIELD}); each loader
 * ignores the other loaders' keys, because the vanilla record codecs tolerate unknown fields.
 *
 * <p>When it takes effect: {@code villager_trade} and {@code trade_set} are data pack registries.
 * Forge patches {@code ResourceManagerRegistryLoadTask} to decode every entry through
 * {@code ConditionCodec.wrap(elementCodec)}; when the condition answers false the entry is skipped
 * ("Skipping ... conditions not met") and never registered - the same outcome as on Fabric and
 * NeoForge. This condition only reads the mod config, never a tag, so the context it is handed
 * does not matter.
 */
public record ConfigLoadCondition(String flag) implements ICondition {
    public static final String ENABLE_VILLAGER_TRADES = "enableVillagerTrades";
    public static final String ENABLE_WANDERING_TRADES = "enableWanderingTrades";

    public static final MapCodec<ConfigLoadCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("flag").forGetter(ConfigLoadCondition::flag)
    ).apply(instance, ConfigLoadCondition::new));

    @Override
    public boolean test(IContext context, DynamicOps<?> ops) {
        SimplebuildingConfig config = Simplebuilding.getConfig();
        if (config == null || config.worldGen == null) {
            // Only before the config exists (e.g. a data generation run): default = enabled.
            return true;
        }
        return switch (this.flag) {
            case ENABLE_VILLAGER_TRADES -> config.worldGen.enableVillagerTrades;
            case ENABLE_WANDERING_TRADES -> config.worldGen.enableWanderingTrades;
            default -> {
                Simplebuilding.LOGGER.warn("Unknown config flag in simplebuilding:config condition: {}", this.flag);
                yield true;
            }
        };
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    @Override
    public String toString() {
        return "simplebuilding:config(\"" + this.flag + "\")";
    }
}

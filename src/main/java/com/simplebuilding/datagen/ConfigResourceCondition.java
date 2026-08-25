package com.simplebuilding.datagen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.config.SimplebuildingConfig;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;

/**
 * Fabric-Resource-Condition "simplebuilding:config": lädt eine Daten-Ressource nur, wenn das
 * benannte Config-Flag aktiv ist. Wird von den Trade-JSONs unter data/simplebuilding/villager_trade/
 * genutzt, um enableVillagerTrades/enableWanderingTrades anzubinden. Ausgewertet beim Laden der
 * Datapacks (Weltstart bzw. /reload), nicht pro Tick.
 */
public record ConfigResourceCondition(String flag) implements ResourceCondition {
    public static final String ENABLE_VILLAGER_TRADES = "enableVillagerTrades";
    public static final String ENABLE_WANDERING_TRADES = "enableWanderingTrades";

    public static final MapCodec<ConfigResourceCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("flag").forGetter(ConfigResourceCondition::flag)
    ).apply(instance, ConfigResourceCondition::new));

    public static final ResourceConditionType<ConfigResourceCondition> TYPE = ResourceConditionType.create(
            Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "config"), CODEC);

    public static void register() {
        ResourceConditions.register(TYPE);
    }

    @Override
    public ResourceConditionType<?> getType() {
        return TYPE;
    }

    @Override
    public boolean test(RegistryOps.RegistryInfoLookup registryInfo) {
        SimplebuildingConfig config = Simplebuilding.getConfig();
        if (config == null) {
            // z.B. Datagen-Läufe, bevor die Config initialisiert ist: Standard = aktiviert
            return true;
        }
        return switch (this.flag) {
            case ENABLE_VILLAGER_TRADES -> config.worldGen.enableVillagerTrades;
            case ENABLE_WANDERING_TRADES -> config.worldGen.enableWanderingTrades;
            default -> {
                Simplebuilding.LOGGER.warn("Unbekanntes Config-Flag in simplebuilding:config-Bedingung: {}", this.flag);
                yield true;
            }
        };
    }
}

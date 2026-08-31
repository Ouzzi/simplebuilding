package com.simplebuilding.neoforge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.config.SimplebuildingConfig;
import net.neoforged.neoforge.common.conditions.ICondition;

/**
 * NeoForge-Gegenstueck zur Fabric-Resource-Condition {@code simplebuilding:config}
 * (siehe {@code src/main/java/com/simplebuilding/condition/ConfigResourceCondition.java}).
 *
 * <p>Registriert unter demselben Namen {@code simplebuilding:config}, aber in NeoForges
 * Registry {@code neoforge:condition_codecs}. In den Trade-JSONs steht deshalb neben
 * {@code "fabric:load_conditions"} zusaetzlich {@code "neoforge:conditions"}; der jeweils
 * fremde Schluessel wird vom Loader ignoriert (die Vanilla-RecordCodecs tolerieren
 * unbekannte Felder).
 *
 * <p>Wirkungszeitpunkt: {@code villager_trade} und {@code trade_set} sind Datapack-Registries
 * aus {@code RegistryDataLoader.WORLDGEN_REGISTRIES}. NeoForge patcht
 * {@code ResourceManagerRegistryLoadTask} so, dass die Eintraege mit
 * {@code ConditionalOps}/{@code ConditionalOps.createConditionalCodec} gelesen werden; schlaegt
 * eine Bedingung fehl, wird der Eintrag gar nicht erst registriert (SKIPPED_ELEMENT_MARKER in
 * {@code RegistryLoadTask}). Das entspricht exakt dem Fabric-Verhalten.
 *
 * <p>Der Kontext beim Laden der Datapack-Registries ist {@code ICondition.IContext.TAGS_INVALID},
 * d.h. Tag-Abfragen sind dort verboten. Diese Bedingung fragt nur die Mod-Config ab und ist
 * damit unproblematisch.
 */
public record ConfigLoadCondition(String flag) implements ICondition {
    public static final String ENABLE_VILLAGER_TRADES = "enableVillagerTrades";
    public static final String ENABLE_WANDERING_TRADES = "enableWanderingTrades";

    public static final MapCodec<ConfigLoadCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("flag").forGetter(ConfigLoadCondition::flag)
    ).apply(instance, ConfigLoadCondition::new));

    @Override
    public boolean test(IContext context) {
        SimplebuildingConfig config = Simplebuilding.getConfig();
        if (config == null || config.worldGen == null) {
            // Sollte auf NeoForge nicht vorkommen (Simplebuilding haelt eine Default-Config vor),
            // aber z.B. bei Datagen-Laeufen vor der Config-Initialisierung: Standard = aktiviert.
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

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    @Override
    public String toString() {
        return "simplebuilding:config(\"" + this.flag + "\")";
    }
}

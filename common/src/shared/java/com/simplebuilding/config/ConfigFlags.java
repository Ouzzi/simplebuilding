package com.simplebuilding.config;

import com.simplebuilding.Simplebuilding;

/**
 * Die Flags der Datenpaket-Bedingung {@code simplebuilding:config} (Fabric-Resource-Condition,
 * NeoForge- und Forge-{@code ICondition}): alle Loader fragen hier, damit dieselbe JSON auf jedem
 * Loader gleich entscheidet.
 *
 * <ul>
 *   <li>{@code enableVillagerTrades}, {@code enableWanderingTrades}: die Handels-Schalter (seit jeher).</li>
 *   <li>{@code astralitOre}, {@code nihilitOre} (2026-09-28): {@code server.oreGeneration.endOres} und der
 *       Schalter des Erzes - fuer die Biom-Modifikatoren der End-Erze.</li>
 *   <li>sonst der Pfad einer Schalter-Option ({@code server.features.backpack}).</li>
 * </ul>
 * Unbekannte Flags gelten als an (mit Warnung im Log), wie bisher.
 */
public final class ConfigFlags {

    public static final String ENABLE_VILLAGER_TRADES = "enableVillagerTrades";
    public static final String ENABLE_WANDERING_TRADES = "enableWanderingTrades";
    public static final String ASTRALIT_ORE = "astralitOre";
    public static final String NIHILIT_ORE = "nihilitOre";

    private ConfigFlags() {
    }

    public static boolean test(String flag) {
        SimplebuildingConfig config = Simplebuilding.getConfig();
        if (config == null || config.worldGen == null) {
            // Nur bevor die Config existiert (Datagen): Standard = an.
            return true;
        }
        switch (flag) {
            case ENABLE_VILLAGER_TRADES:
                return config.worldGen.enableVillagerTrades;
            case ENABLE_WANDERING_TRADES:
                return config.worldGen.enableWanderingTrades;
            case ASTRALIT_ORE:
                return oreEnabled(config, true);
            case NIHILIT_ORE:
                return oreEnabled(config, false);
            default:
                ConfigOptions.Option option = ConfigOptions.byPath(flag);
                if (option != null && option.type() == boolean.class && option.get(config) instanceof Boolean value) {
                    return value;
                }
                Simplebuilding.LOGGER.warn("Unknown config flag in simplebuilding:config condition: {}", flag);
                return true;
        }
    }

    /** End-Erz an: Hauptschalter und der des Erzes. */
    public static boolean oreEnabled(SimplebuildingConfig config, boolean astralit) {
        if (config.server == null || config.server.oreGeneration == null) {
            return true;
        }
        ServerTuningConfig.OreGeneration ores = config.server.oreGeneration;
        return ores.endOres && (astralit ? ores.astralitOre : ores.nihilitOre);
    }

    /** {@link #oreEnabled(SimplebuildingConfig, boolean)} mit der geladenen Config (Fabric-Biomwahl). */
    public static boolean oreEnabled(boolean astralit) {
        SimplebuildingConfig config = Simplebuilding.getConfig();
        return config == null || oreEnabled(config, astralit);
    }
}

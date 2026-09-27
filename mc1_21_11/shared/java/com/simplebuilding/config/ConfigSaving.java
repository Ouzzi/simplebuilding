package com.simplebuilding.config;

import com.simplebuilding.Simplebuilding;
import me.shedaniel.autoconfig.AutoConfig;

/**
 * Writes {@link SimplebuildingConfig} back to {@code config/simplebuilding.json} after a command
 * changed it (for example {@code /simplebuilding config setTrimMultiplier}). All three loaders
 * register the config with Cloth's {@code AutoConfig}, so one saver serves them all. Kept outside
 * the config class: Cloth builds its screen from that class's fields.
 *
 * <p>Game tests swap the saver ({@link #setSaver}) so a run never writes the options another test
 * is holding flipped at that moment into the file.
 */
public final class ConfigSaving {

    private static Runnable saver = ConfigSaving::saveThroughAutoConfig;

    private ConfigSaving() {
    }

    public static void save() {
        saver.run();
    }

    public static Runnable saver() {
        return saver;
    }

    public static void setSaver(Runnable newSaver) {
        saver = newSaver != null ? newSaver : ConfigSaving::saveThroughAutoConfig;
    }

    private static void saveThroughAutoConfig() {
        try {
            AutoConfig.getConfigHolder(SimplebuildingConfig.class).save();
        } catch (RuntimeException e) {
            Simplebuilding.LOGGER.warn("Could not save the SimpleBuilding config", e);
        }
    }
}

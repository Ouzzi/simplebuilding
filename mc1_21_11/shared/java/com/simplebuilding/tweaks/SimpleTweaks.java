package com.simplebuilding.tweaks;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.config.SimplebuildingConfig;
import net.minecraft.resources.Identifier;

/**
 * Einstieg in den aus Simple Tweaks uebernommenen Teil (docs/SIMPLETWEAKS-UEBERNAHME.md).
 * Loader-neutral; die Loader rufen {@link TweaksContent#init()} und haengen ihre Events an die
 * Methoden in {@code com.simplebuilding.tweaks.spawn} usw.
 */
public final class SimpleTweaks {

    private static final TweaksConfig DEFAULTS = new TweaksConfig();
    private static Runnable configSaver = () -> {};

    private SimpleTweaks() {
    }

    /** Der Tweaks-Abschnitt der geladenen Config; vor dem Laden (Datagen) die Standardwerte. */
    public static TweaksConfig config() {
        SimplebuildingConfig config = Simplebuilding.getConfig();
        if (config == null || config.tweaks == null) {
            return DEFAULTS;
        }
        return config.tweaks;
    }

    /** Speichert die Config nach einem Befehl; die Loader setzen, wie (Forge hat keine Persistenz). */
    public static void setConfigSaver(Runnable saver) {
        configSaver = saver != null ? saver : () -> {};
    }

    public static void saveConfig() {
        configSaver.run();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, path);
    }
}

package com.simplebuilding.tweaks;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.config.SimplebuildingConfig;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

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

    /**
     * Was der Client vom Server wissen muss (Audit 2026-09-26 #16): Raketen-Stapelgroesse (sonst
     * Stapel-Desync und Geister-Items), Boosts je Ladung (HUD), Laser an/aus und Reichweite (der
     * Server prueft beides). Der Server schickt sie beim Einloggen und nach jedem Tweaks-Befehl.
     */
    public record ServerValues(int rocketStackSize, int maxBoosts, boolean laserEnabled, int laserRange) {
    }

    /** Zuletzt vom Server gemeldet; null = nicht verbunden oder Server ohne diese Mod. */
    private static volatile @Nullable ServerValues serverValues;

    /** Die Werte aus der eigenen Config (Server-Seite; Inhalt des Sync-Pakets). */
    public static ServerValues localValues() {
        TweaksConfig config = config();
        return new ServerValues(config.balancing.rocketStackSize, config.spawn.boostCount(),
                config.laserPointer.enable, config.laserPointer.range);
    }

    /** Vom Client beim Empfang gesetzt, beim Trennen geloescht. */
    public static void setServerValues(@Nullable ServerValues values) {
        serverValues = values;
    }

    /**
     * Was gerade gilt: auf dem Client-Thread die Werte des Servers (falls gemeldet), sonst - Server,
     * auch der integrierte - die eigene Config. So liest der integrierte Server nie einen Wert, den
     * der Client noch nicht nachgezogen hat.
     */
    public static ServerValues effectiveValues() {
        ServerValues synced = serverValues;
        if (synced != null && TweaksClientHooks.onClientThread()) {
            return synced;
        }
        return localValues();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, path);
    }
}

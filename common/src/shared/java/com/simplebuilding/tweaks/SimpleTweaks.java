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
    private static boolean saveRequested;

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

    /**
     * Speichert die Config nach einem Befehl; die Loader setzen, wie (Forge hat keine Persistenz).
     * Wartet ein Speichern aus dem Laden ({@link #requestConfigSave}), laeuft es hier einmal.
     */
    public static void setConfigSaver(Runnable saver) {
        configSaver = saver != null ? saver : () -> {};
        if (saver != null && saveRequested) {
            saveRequested = false;
            configSaver.run();
        }
    }

    public static Runnable configSaver() {
        return configSaver;
    }

    public static void saveConfig() {
        configSaver.run();
    }

    /**
     * Die Config soll einmal geschrieben werden (Migration alter Schluessel beim Laden). Waehrend
     * {@code validatePostLoad} gibt es noch keinen Config-Halter, der speichern koennte; der Loader
     * setzt seinen Speicherer gleich danach, und {@link #setConfigSaver} holt es nach.
     */
    public static void requestConfigSave() {
        saveRequested = true;
    }

    /** Fuer Tests: ob ein Speichern aus dem Laden noch aussteht. */
    public static boolean configSaveRequested() {
        return saveRequested;
    }

    /**
     * Was der Client vom Server wissen muss (Audit 2026-09-26 #16): Raketen-Stapelgroesse (sonst
     * Stapel-Desync und Geister-Items), Boosts je Ladung (HUD), Laser an/aus und Reichweite (der
     * Server prueft beides), seit dem Config-Umbau 2026-09-28 auch die Luftsprung-Abklingzeit
     * ({@code airJumpCooldownTicks}: der Server-Waechter {@code AirJumpGuard} lehnte sonst Spruenge
     * ab, die ein Client mit kuerzerer eigener Abklingzeit schon zeigte). Der Server schickt sie beim
     * Einloggen, nach jedem Tweaks-Befehl und nach {@code /simplebuilding config set}.
     *
     * <p>{@code serverTuning} ist seit 2026-09-28 der ganze Reiter "Server & Modpack Tuning" als JSON
     * ({@link com.simplebuilding.config.ServerTuning}); ein String, damit zwei Staende mit
     * {@code equals} vergleichbar bleiben.
     */
    public record ServerValues(int rocketStackSize, int maxBoosts, boolean laserEnabled, int laserRange,
                               int airJumpCooldownTicks, String serverTuning) {
        /** Ohne Tuning-Abschnitt (Tests, alte Aufrufer): der Client nimmt dann seine eigene Datei. */
        public ServerValues(int rocketStackSize, int maxBoosts, boolean laserEnabled, int laserRange, int airJumpCooldownTicks) {
            this(rocketStackSize, maxBoosts, laserEnabled, laserRange, airJumpCooldownTicks, "");
        }
    }

    /** Zuletzt vom Server gemeldet; null = nicht verbunden oder Server ohne diese Mod. */
    private static volatile @Nullable ServerValues serverValues;

    /** Die Werte aus der eigenen Config (Server-Seite; Inhalt des Sync-Pakets). */
    public static ServerValues localValues() {
        TweaksConfig config = config();
        SimplebuildingConfig root = Simplebuilding.getConfig();
        return new ServerValues(config.balancing.rocketStackSize, config.spawn.boostCount(),
                config.laserPointer.enable, config.laserPointer.range,
                root == null ? 100 : Math.max(0, root.airJumpCooldownTicks),
                com.simplebuilding.config.ServerTuning.toJson(com.simplebuilding.config.ServerTuning.local()));
    }

    /** Der zuletzt vom Server gemeldete Stand oder null (fuer {@link com.simplebuilding.config.ServerTuning}). */
    public static @Nullable ServerValues syncedValues() {
        return serverValues;
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

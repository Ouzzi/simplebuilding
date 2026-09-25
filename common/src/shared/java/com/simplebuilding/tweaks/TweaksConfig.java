package com.simplebuilding.tweaks;

import me.shedaniel.autoconfig.annotation.ConfigEntry;

/**
 * Der aus Simple Tweaks uebernommene Teil der Config, als Abschnitt {@code tweaks} der
 * SimpleBuilding-Config. Feldnamen wie in Simple Tweaks ({@code SimpletweaksConfig}), dazu je
 * Druckplatten-Familie ein Schalter in {@link Pads} (docs/SIMPLETWEAKS-UEBERNAHME.md, Abschnitt 5).
 */
public class TweaksConfig {

    @ConfigEntry.Gui.CollapsibleObject
    public Pads pads = new Pads();

    @ConfigEntry.Gui.CollapsibleObject
    public Balancing balancing = new Balancing();

    @ConfigEntry.Gui.CollapsibleObject
    public Dimensions dimensions = new Dimensions();

    @ConfigEntry.Gui.CollapsibleObject
    public Spawn spawn = new Spawn();

    @ConfigEntry.Gui.CollapsibleObject
    public Commands commands = new Commands();

    @ConfigEntry.Gui.CollapsibleObject
    public Optimization optimization = new Optimization();

    @ConfigEntry.Gui.CollapsibleObject
    public LaserPointer laserPointer = new LaserPointer();

    /**
     * Aus = der Block bleibt platzierbar und abbaubar, tut aber nichts: Chunk-Loader geben ihre
     * Chunks frei, Flypads nehmen den Flug zurueck, Elytra-Pads verteilen keine Elytren, Teleporter
     * und Launchpads zaehlen nicht, Platten senden kein Signal.
     */
    public static class Pads {
        @ConfigEntry.Gui.Tooltip
        public boolean enableChunkLoaders = true;
        @ConfigEntry.Gui.Tooltip
        public boolean enableElytraPads = true;
        @ConfigEntry.Gui.Tooltip
        public boolean enableFlypads = true;
        @ConfigEntry.Gui.Tooltip
        public boolean enableSpawnTeleporters = true;
        @ConfigEntry.Gui.Tooltip
        public boolean enableLaunchpads = true;
        @ConfigEntry.Gui.Tooltip
        public boolean enableTimedCopperPlates = true;
        @ConfigEntry.Gui.Tooltip
        public boolean enableFilterPlates = true;
    }

    public static class Balancing {
        @ConfigEntry.Gui.Tooltip(count = 2)
        public int rocketStackSize = 64;
    }

    public static class Dimensions {
        @ConfigEntry.Gui.Tooltip
        public boolean allowNether = true;
        @ConfigEntry.Gui.Tooltip
        public boolean allowEnd = true;
    }

    public static class Spawn {
        /** Aus in SimpleBuilding standardmaessig aus (Simple Tweaks: an), damit bestehende Welten unveraendert bleiben. */
        @ConfigEntry.Gui.Tooltip
        public boolean forceExactSpawn = false;

        @ConfigEntry.Gui.Tooltip
        public boolean disableFallDamageInSpawn = true;

        /** Eigener Weltspawn beim Laden der Oberwelt; aus = Vanilla-Weltspawn bleibt. */
        @ConfigEntry.Gui.Tooltip
        public boolean useCustomWorldSpawn = false;
        @ConfigEntry.Gui.Tooltip
        public int xCoordSpawnPoint = 0;
        @ConfigEntry.Gui.Tooltip
        public int yCoordSpawnPoint = -1;
        @ConfigEntry.Gui.Tooltip
        public int zCoordSpawnPoint = 0;

        /** 0..64 Spawn-Teleporter beim ersten Betreten; 0 = keine (Besitzer-Wunsch 2026-09-26). */
        @ConfigEntry.Gui.Tooltip
        public int firstJoinTeleporterCount = 0;
        /** 0..64 Elytra-Pads beim ersten Betreten; 0 = keine. */
        @ConfigEntry.Gui.Tooltip
        public int firstJoinElytraPadCount = 0;
        /**
         * Alter Schluessel aus Simple Tweaks: EIN Wert fuer Teleporter und Pads, Standard 1. Wird nur
         * noch gelesen und von {@link #migrateLegacyFirstJoinCount()} auf die zwei neuen Werte
         * verteilt; {@code null} laesst Gson beim Speichern weg, der Schluessel verschwindet also
         * mit dem naechsten Speichern aus der Datei.
         */
        @ConfigEntry.Gui.Excluded
        public Integer spawnTeleporterCount = null;

        @ConfigEntry.Gui.Tooltip
        public boolean giveElytraOnSpawn = false;
        @ConfigEntry.Gui.Tooltip
        public int spawnElytraRadius = 25;
        @ConfigEntry.Gui.Tooltip
        public boolean useWorldSpawnAsCenter = false;
        @ConfigEntry.Gui.Tooltip
        public int customSpawnElytraX = 0;
        @ConfigEntry.Gui.Tooltip
        public int customSpawnElytraZ = 0;
        @ConfigEntry.Gui.Tooltip
        public int flightTimeSeconds = 300;
        @ConfigEntry.Gui.Tooltip
        public int maxBoosts = 3;
        @ConfigEntry.Gui.Tooltip
        public float boostStrength = 0.6f;

        /** Ziele der Spawn-Teleporter I-IV; y = -1000 heisst "nicht gesetzt" (dann Weltspawn). */
        public int spawn1X = 0, spawn1Y = -1000, spawn1Z = 0;
        public int spawn2X = 0, spawn2Y = -1000, spawn2Z = 0;
        public int spawn3X = 0, spawn3Y = -1000, spawn3Z = 0;
        public int spawn4X = 0, spawn4Y = -1000, spawn4Z = 0;

        /**
         * Uebernimmt einen alten {@code spawnTeleporterCount} aus einer bestehenden Config-Datei.
         * Ein vom Spieler geaenderter Wert (nicht der alte Standard 1) gilt wie frueher fuer beide
         * Geschenke; der alte Standard 1 wird zum neuen Standard 0, sonst bekaeme jede bestehende
         * Installation die Geschenke weiter, obwohl sie nie jemand eingestellt hat.
         *
         * @return ob ein alter Wert da war
         */
        public boolean migrateLegacyFirstJoinCount() {
            if (spawnTeleporterCount == null) {
                return false;
            }
            int legacy = Math.max(0, Math.min(64, spawnTeleporterCount));
            if (legacy != 1) {
                firstJoinTeleporterCount = legacy;
                firstJoinElytraPadCount = legacy;
            }
            spawnTeleporterCount = null;
            return true;
        }
    }

    public static class Commands {
        @ConfigEntry.Gui.Tooltip
        public boolean enableKillBoatsCommand = true;
        @ConfigEntry.Gui.Tooltip
        public boolean enableKillCartsCommand = false;
    }

    public static class Optimization {
        @ConfigEntry.Gui.Tooltip
        public boolean enableXpClumps = true;
        @ConfigEntry.Gui.Tooltip
        public boolean scaleXpOrbs = true;
    }

    public static class LaserPointer {
        @ConfigEntry.Gui.Tooltip
        public boolean enable = true;
        @ConfigEntry.Gui.Tooltip
        public int color = 0xFF0000;
        @ConfigEntry.Gui.Tooltip
        public float scale = 0.25f;
        @ConfigEntry.Gui.Tooltip
        public int range = 512;
        /** Schon in Simple Tweaks ohne Wirkung; der Schalter bleibt, damit alte Configs lesbar bleiben. */
        @ConfigEntry.Gui.Tooltip
        public boolean showLine = false;
    }
}

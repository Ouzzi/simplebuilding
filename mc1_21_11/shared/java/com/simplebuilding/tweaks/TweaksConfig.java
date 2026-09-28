package com.simplebuilding.tweaks;

import me.shedaniel.autoconfig.annotation.ConfigEntry;

/**
 * Der aus Simple Tweaks uebernommene Teil der Config, als Abschnitt {@code tweaks} der
 * SimpleBuilding-Config. Feldnamen wie in Simple Tweaks ({@code SimpletweaksConfig}), dazu je
 * Druckplatten-Familie ein Schalter in {@link Pads} (docs/SIMPLETWEAKS-UEBERNAHME.md, Abschnitt 5).
 */
public class TweaksConfig {

    // Reihenfolge = Anzeige im Reiter "Pads & Tweaks" (Config-Umbau 2026-09-28); Gson liest nach Namen.
    @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
    public Pads pads = new Pads();

    @ConfigEntry.Gui.CollapsibleObject
    public PadTuning padTuning = new PadTuning();

    @ConfigEntry.Gui.CollapsibleObject
    public LaserPointer laserPointer = new LaserPointer();

    @ConfigEntry.Gui.CollapsibleObject
    public Balancing balancing = new Balancing();

    @ConfigEntry.Gui.CollapsibleObject
    public Spawn spawn = new Spawn();

    @ConfigEntry.Gui.CollapsibleObject
    public Dimensions dimensions = new Dimensions();

    @ConfigEntry.Gui.CollapsibleObject
    public Commands commands = new Commands();

    @ConfigEntry.Gui.CollapsibleObject
    public Optimization optimization = new Optimization();

    /** Begrenzt handeditierte Werte (aus {@code SimplebuildingConfig#validatePostLoad}); fehlende Gruppen neu. */
    public void validate() {
        if (pads == null) pads = new Pads();
        if (padTuning == null) padTuning = new PadTuning();
        if (laserPointer == null) laserPointer = new LaserPointer();
        if (balancing == null) balancing = new Balancing();
        if (spawn == null) spawn = new Spawn();
        if (dimensions == null) dimensions = new Dimensions();
        if (commands == null) commands = new Commands();
        if (optimization == null) optimization = new Optimization();
        padTuning.teleporterTier1WarmupTicks = Math.max(1, padTuning.teleporterTier1WarmupTicks);
        padTuning.teleporterTier2WarmupTicks = Math.max(1, padTuning.teleporterTier2WarmupTicks);
        padTuning.teleporterTier3WarmupTicks = Math.max(1, padTuning.teleporterTier3WarmupTicks);
        padTuning.launchpadStrengthMultiplier = nonNegative(padTuning.launchpadStrengthMultiplier, 1.0);
        padTuning.potionPadChargeStepTicks = Math.max(1, padTuning.potionPadChargeStepTicks);
        padTuning.potionPadCooldownFactor = nonNegative(padTuning.potionPadCooldownFactor, 2.0);
        commands.killCommandRadius = Math.max(1, commands.killCommandRadius);
        optimization.xpClumpRadius = nonNegative(optimization.xpClumpRadius, 2.0);
        laserPointer.beamCostPerSecond = Math.max(0, laserPointer.beamCostPerSecond);
        laserPointer.effectCost = Math.max(0, laserPointer.effectCost);
        balancing.echoSounderJumpCooldownTicks = Math.max(0, balancing.echoSounderJumpCooldownTicks);
    }

    /** Endliche, nicht negative Zahl; sonst {@code fallback}. */
    static double nonNegative(double value, double fallback) {
        return Double.isFinite(value) ? Math.max(0.0, value) : fallback;
    }

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
        /** Aus: Trank-Pads geben keine Wirkungen mehr (Config-Umbau 2026-09-28). */
        @ConfigEntry.Gui.Tooltip
        public boolean enablePotionPads = true;
    }

    /**
     * Zeiten und Staerken der Pads (Config-Umbau 2026-09-28; Standard = das bisherige feste
     * Verhalten). Nur der Server liest sie; die Zugriffe unten begrenzen handeditierte Werte.
     */
    public static class PadTuning {
        /*
         * Stillstehen bis zum Sprung je Spawn-Teleporter-Stufe (Besitzer 2026-09-28: 50/20/5 s). Neue
         * Schluessel statt teleporterWarmupTicks/enderiteTeleporterWarmupTicks (100/60 fuer die alten fuenf
         * Stufen): eine damit gespeicherte Datei soll nicht still die alten Zeiten behalten.
         */
        /** Spawn-Teleporter I (Ticks; 1000 = 50 s). */
        @ConfigEntry.Gui.Tooltip
        public int teleporterTier1WarmupTicks = 1000;
        /** Spawn-Teleporter II (Ticks; 400 = 20 s). */
        @ConfigEntry.Gui.Tooltip
        public int teleporterTier2WarmupTicks = 400;
        /** Spawn-Teleporter III, Enderit (Ticks; 100 = 5 s). */
        @ConfigEntry.Gui.Tooltip
        public int teleporterTier3WarmupTicks = 100;
        /** Faktor auf den Schub der Launchpads (1,5 + 0,8 je Windkugel-Ladung). */
        @ConfigEntry.Gui.Tooltip
        public double launchpadStrengthMultiplier = 1.0;
        /** Dauer eines Aufladeschritts der Trank-Pads (Ticks; drei Schritte: 25/50/100 %). */
        @ConfigEntry.Gui.Tooltip
        public int potionPadChargeStepTicks = 20;
        /** Abklingzeit der Trank-Pads als Vielfaches der Wirkdauer; 0 = keine Abklingzeit. */
        @ConfigEntry.Gui.Tooltip
        public double potionPadCooldownFactor = 2.0;

        /** Wartezeit des Spawn-Teleporters der Stufe 1..3 (hoeher = 3), mindestens 1 Tick. */
        public int teleporterWarmup(int tier) {
            int ticks = tier >= 3 ? teleporterTier3WarmupTicks : tier == 2 ? teleporterTier2WarmupTicks : teleporterTier1WarmupTicks;
            return Math.max(1, ticks);
        }

        public double launchpadStrengthFactor() {
            return nonNegative(launchpadStrengthMultiplier, 1.0);
        }

        public int potionPadStepTicks() {
            return Math.max(1, potionPadChargeStepTicks);
        }

        public double potionPadCooldown() {
            return nonNegative(potionPadCooldownFactor, 2.0);
        }
    }

    public static class Balancing {
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 64)
        public int rocketStackSize = 64;
        /**
         * Abklingzeit des Echolots nach einem Sprung (Ticks; 480 = 24 s, Besitzer 2026-09-28: viermal die
         * frueheren 6 s), siehe EchoCompassItem. Neuer Schluessel statt echoSounderCooldownTicks (120), damit
         * eine damit gespeicherte Datei nicht die alte Zeit behaelt.
         */
        @ConfigEntry.Gui.Tooltip
        public int echoSounderJumpCooldownTicks = 480;
    }

    public static class Dimensions {
        @ConfigEntry.Gui.Tooltip
        public boolean allowNether = true;
        @ConfigEntry.Gui.Tooltip
        public boolean allowEnd = true;
    }

    public static class Spawn {
        /** Obergrenze der Flugzeit (24 h): {@code flightTimeSeconds * 20} lief frueher ueber (Audit #51). */
        public static final int MAX_FLIGHT_SECONDS = 86_400;
        /** Obergrenze der Boosts; mehr Teilstriche passen ohnehin nicht in die 182-Pixel-Leiste. */
        public static final int MAX_BOOSTS = 100;

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
        @ConfigEntry.BoundedDiscrete(min = 0, max = 64)
        public int firstJoinTeleporterCount = 0;
        /** 0..64 Elytra-Pads beim ersten Betreten; 0 = keine. */
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 64)
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

        /** Flugzeit in Ticks, auf 1 s .. {@link #MAX_FLIGHT_SECONDS} begrenzt (auch fuer handeditierte Dateien). */
        public int flightTicks() {
            return Math.max(1, Math.min(MAX_FLIGHT_SECONDS, flightTimeSeconds)) * 20;
        }

        /** Boosts je Ladung, auf 1 .. {@link #MAX_BOOSTS} begrenzt. */
        public int boostCount() {
            return Math.max(1, Math.min(MAX_BOOSTS, maxBoosts));
        }

        /**
         * Ziel aller Spawn-Teleporter (per {@code worldspawn setspawn1}); y = -1000 heisst "nicht gesetzt"
         * (dann Weltspawn). Die Ziele 2-4 der frueheren Stufen II-IV gibt es seit 2026-09-28 nicht mehr
         * (drei Stufen, die sich nur in der Wartezeit unterscheiden); alte Schluessel in einer
         * Config-Datei werden beim Laden ignoriert.
         */
        @ConfigEntry.Gui.Tooltip
        public int spawn1X = 0;
        @ConfigEntry.Gui.Tooltip
        public int spawn1Y = -1000;
        @ConfigEntry.Gui.Tooltip
        public int spawn1Z = 0;

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
        /** Reichweite von /killboats und /killcarts um den Spieler (Bloecke). */
        @ConfigEntry.Gui.Tooltip
        public int killCommandRadius = 100;
    }

    public static class Optimization {
        @ConfigEntry.Gui.Tooltip
        public boolean enableXpClumps = true;
        /** Wie weit eine XP-Kugel ihre Nachbarn einsammelt (Bloecke), siehe XpClumping. */
        @ConfigEntry.Gui.Tooltip
        public double xpClumpRadius = 2.0;
        @ConfigEntry.Gui.Tooltip
        public boolean scaleXpOrbs = true;
    }

    public static class LaserPointer {
        @ConfigEntry.Gui.Tooltip
        public boolean enable = true;
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.ColorPicker
        public int color = 0xFF0000;
        @ConfigEntry.Gui.Tooltip
        public float scale = 0.25f;
        @ConfigEntry.Gui.Tooltip
        public int range = 512;
        /** Ladung je angefangener Sekunde Strahlen (640 = voll, 10 je Redstone); 0 = kostenlos. */
        @ConfigEntry.Gui.Tooltip
        public int beamCostPerSecond = 1;
        /** Ladung je Wirkung auf einen Block oder ein Wesen (Schmelzen, Anzuenden, Trocknen ...). */
        @ConfigEntry.Gui.Tooltip
        public int effectCost = 5;
        /**
         * Schon in Simple Tweaks ohne Wirkung; der Schluessel bleibt, damit alte Configs lesbar
         * bleiben, erscheint aber nicht mehr im Config-Bildschirm (Audit #34).
         */
        @ConfigEntry.Gui.Excluded
        public boolean showLine = false;
    }
}

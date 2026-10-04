package com.simplebuilding.config;

import me.shedaniel.autoconfig.annotation.ConfigEntry;

/**
 * Reiter "Server & Modpack Tuning" (Besitzer 2026-09-28): jede Gameplay-Stellschraube, die ein
 * Server- oder Modpack-Ersteller drehen will, an einem Ort und <b>serverseitig verbindlich</b>. Der
 * Server liest seine eigene Datei; die Clients bekommen den ganzen Abschnitt beim Einloggen und nach
 * jedem Config-Befehl mit dem {@code TweaksConfigPayload} (als JSON) und rechnen Anzeige und
 * Vorhersage mit den Werten des Servers ({@link ServerTuning#get()}); die eigene Datei eines Clients
 * hat hier nichts zu sagen. Ausnahme: {@link Charges} legt die Haltbarkeit von Gegenstaenden fest,
 * die beim Start registriert wird - Neustart noetig, und Client und Server muessen dieselbe Datei
 * haben (Modpack).
 *
 * <p>Standard = bisheriges Verhalten (einzige gewollte Aenderung: Chunk-Loader laufen nur, solange
 * ihr Besitzer online ist, Besitzer-Entscheidung). Jede Geschwindigkeit und Reichweite hat eine
 * Obergrenze, die Vanilla nicht aus dem Gleichgewicht bringt; {@link #validate()} begrenzt beim
 * Laden und nach jedem Befehl, die Zugriffe in {@link ServerTuning} noch einmal.
 *
 * <p>Einzeilige Felder ({@code public typ name = wert;}): {@code wiki/generate.py} liest die Datei
 * zeilenweise.
 */
public class ServerTuningConfig {

    @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
    public Features features = new Features();

    @ConfigEntry.Gui.CollapsibleObject
    public ChunkLoaders chunkLoaders = new ChunkLoaders();

    @ConfigEntry.Gui.CollapsibleObject
    public DimensionLocks dimensionLocks = new DimensionLocks();

    @ConfigEntry.Gui.CollapsibleObject
    public Laser laser = new Laser();

    @ConfigEntry.Gui.CollapsibleObject
    public Arrows arrows = new Arrows();

    @ConfigEntry.Gui.CollapsibleObject
    public CraftyShulker craftyShulker = new CraftyShulker();

    @ConfigEntry.Gui.CollapsibleObject
    public Hammock hammock = new Hammock();

    @ConfigEntry.Gui.CollapsibleObject
    public Speakers speakers = new Speakers();

    @ConfigEntry.Gui.CollapsibleObject
    public OreGeneration oreGeneration = new OreGeneration();

    @ConfigEntry.Gui.CollapsibleObject
    public Pads pads = new Pads();

    @ConfigEntry.Gui.CollapsibleObject
    public Charges charges = new Charges();

    @ConfigEntry.Gui.CollapsibleObject
    public Tools tools = new Tools();

    @ConfigEntry.Gui.CollapsibleObject
    public Machines machines = new Machines();

    @ConfigEntry.Gui.CollapsibleObject
    public OreDetector oreDetector = new OreDetector();

    @ConfigEntry.Gui.CollapsibleObject
    public Loot loot = new Loot();

    @ConfigEntry.Gui.CollapsibleObject
    public Blueprint blueprint = new Blueprint();

    @ConfigEntry.Gui.CollapsibleObject
    public TrimStrengths trimStrengths = new TrimStrengths();

    /** Begrenzt handeditierte oder per Befehl gesetzte Werte; fehlende Gruppen neu. */
    public void validate() {
        if (features == null) features = new Features();
        if (machines != null) machines.endSignalRange = clamp(machines.endSignalRange, 1, 15);
        if (machines != null) machines.endPistonCooldownTicks = clamp(machines.endPistonCooldownTicks, 4, 100);
        if (chunkLoaders == null) chunkLoaders = new ChunkLoaders();
        if (dimensionLocks == null) dimensionLocks = new DimensionLocks();
        if (laser == null) laser = new Laser();
        if (arrows == null) arrows = new Arrows();
        if (craftyShulker == null) craftyShulker = new CraftyShulker();
        if (hammock == null) hammock = new Hammock();
        if (speakers == null) speakers = new Speakers();
        if (oreGeneration == null) oreGeneration = new OreGeneration();
        if (pads == null) pads = new Pads();
        if (charges == null) charges = new Charges();
        if (tools == null) tools = new Tools();
        if (machines == null) machines = new Machines();
        if (oreDetector == null) oreDetector = new OreDetector();
        if (loot == null) loot = new Loot();
        if (blueprint == null) blueprint = new Blueprint();
        if (trimStrengths == null) trimStrengths = new TrimStrengths();

        if (dimensionLocks.chunkLoaderBlockedDimensions == null) dimensionLocks.chunkLoaderBlockedDimensions = "";
        if (features.placeDisabledItems == null || features.placeDisabledItems.length() > 4096) features.placeDisabledItems = "";
        features.scarecrowRadius = clamp(features.scarecrowRadius, 0, com.simplebuilding.dummy.Scarecrow.MAX_RADIUS);
        if (dimensionLocks.flypadBlockedDimensions == null) dimensionLocks.flypadBlockedDimensions = "";
        if (dimensionLocks.echoSounderBlockedDimensions == null) dimensionLocks.echoSounderBlockedDimensions = "";

        pads.strangerPadBreakSeconds = clamp(pads.strangerPadBreakSeconds, 1, ServerTuning.MAX_BREAK_SECONDS);
        pads.strangerPlateBreakSeconds = clamp(pads.strangerPlateBreakSeconds, 1, ServerTuning.MAX_BREAK_SECONDS);

        charges.lensMaxCharge = clamp(charges.lensMaxCharge, ServerTuning.MIN_LENS, ServerTuning.MAX_LENS);
        charges.rotatorMaxCharge = clamp(charges.rotatorMaxCharge, ServerTuning.MIN_ROTATOR, ServerTuning.MAX_ROTATOR);
        charges.echoSounderMaxCharge = clamp(charges.echoSounderMaxCharge, ServerTuning.MIN_ECHO, ServerTuning.MAX_ECHO);

        tools.attractorMinimumDistance = clamp(tools.attractorMinimumDistance, 0.5, 2.0, 1.25);
        tools.sledgehammerUpgradeSeconds = clamp(tools.sledgehammerUpgradeSeconds, 1, ServerTuning.MAX_UPGRADE_SECONDS);
        tools.reinforcedUpgradeDamagePerHit = clamp(tools.reinforcedUpgradeDamagePerHit, 0, ServerTuning.MAX_UPGRADE_DAMAGE);
        tools.netheriteUpgradeDamagePerHit = clamp(tools.netheriteUpgradeDamagePerHit, 0, ServerTuning.MAX_UPGRADE_DAMAGE);
        tools.enderiteUpgradeDamagePerHit = clamp(tools.enderiteUpgradeDamagePerHit, 0, ServerTuning.MAX_UPGRADE_DAMAGE);
        tools.stoneChiselCooldownTicks = clamp(tools.stoneChiselCooldownTicks, ServerTuning.MIN_CHISEL_COOLDOWN, ServerTuning.MAX_CHISEL_COOLDOWN);
        tools.copperChiselCooldownTicks = clamp(tools.copperChiselCooldownTicks, ServerTuning.MIN_CHISEL_COOLDOWN, ServerTuning.MAX_CHISEL_COOLDOWN);
        tools.ironChiselCooldownTicks = clamp(tools.ironChiselCooldownTicks, ServerTuning.MIN_CHISEL_COOLDOWN, ServerTuning.MAX_CHISEL_COOLDOWN);
        tools.goldChiselCooldownTicks = clamp(tools.goldChiselCooldownTicks, ServerTuning.MIN_CHISEL_COOLDOWN, ServerTuning.MAX_CHISEL_COOLDOWN);
        tools.diamondChiselCooldownTicks = clamp(tools.diamondChiselCooldownTicks, ServerTuning.MIN_CHISEL_COOLDOWN, ServerTuning.MAX_CHISEL_COOLDOWN);
        tools.netheriteChiselCooldownTicks = clamp(tools.netheriteChiselCooldownTicks, ServerTuning.MIN_CHISEL_COOLDOWN, ServerTuning.MAX_CHISEL_COOLDOWN);
        tools.enderiteChiselCooldownTicks = clamp(tools.enderiteChiselCooldownTicks, ServerTuning.MIN_CHISEL_COOLDOWN, ServerTuning.MAX_CHISEL_COOLDOWN);

        machines.reinforcedHopperSpeed = clamp(machines.reinforcedHopperSpeed, 1, ServerTuning.MAX_MACHINE_SPEED);
        machines.netheriteHopperSpeed = clamp(machines.netheriteHopperSpeed, 1, ServerTuning.MAX_MACHINE_SPEED);
        machines.enderiteHopperSpeed = clamp(machines.enderiteHopperSpeed, 1, ServerTuning.MAX_MACHINE_SPEED);
        machines.reinforcedFurnaceSpeed = clamp(machines.reinforcedFurnaceSpeed, 1, ServerTuning.MAX_MACHINE_SPEED);
        machines.netheriteFurnaceSpeed = clamp(machines.netheriteFurnaceSpeed, 1, ServerTuning.MAX_MACHINE_SPEED);
        machines.enderiteFurnaceSpeed = clamp(machines.enderiteFurnaceSpeed, 1, ServerTuning.MAX_MACHINE_SPEED);

        oreDetector.rangeMultiplier = clamp(oreDetector.rangeMultiplier, ServerTuning.MIN_DETECTOR_RANGE, ServerTuning.MAX_DETECTOR_RANGE, 1.0);
        oreDetector.scanIntervalTicks = clamp(oreDetector.scanIntervalTicks, ServerTuning.MIN_SCAN_INTERVAL, ServerTuning.MAX_SCAN_INTERVAL);

        loot.globalLootMultiplier = clamp(loot.globalLootMultiplier, 0.0, ServerTuning.MAX_LOOT_MULTIPLIER, 1.0);
        loot.tradePriceMultiplier = clamp(loot.tradePriceMultiplier, ServerTuning.MIN_PRICE_MULTIPLIER, ServerTuning.MAX_PRICE_MULTIPLIER, 1.0);
        loot.betterChestPercent = clamp(loot.betterChestPercent, 0.0, ServerTuning.MAX_BETTER_CHEST_PERCENT, 1.0);
        loot.reinforcedShulkerPercent = clamp(loot.reinforcedShulkerPercent, 0.0, ServerTuning.MAX_REINFORCED_SHULKER_PERCENT, 2.0);
        loot.enderiteShulkerPercent = clamp(loot.enderiteShulkerPercent, 0.0, ServerTuning.MAX_ENDERITE_SHULKER_PERCENT, 0.5);
        loot.netheriteShulkerPercent = clamp(loot.netheriteShulkerPercent, 0.0, ServerTuning.MAX_NETHERITE_SHULKER_PERCENT, 1.0);
        loot.endermitesPerRareShulker = clamp(loot.endermitesPerRareShulker, 0, ServerTuning.MAX_ENDERMITES_PER_RARE_SHULKER);

        arrows.maxPerMob = clamp(arrows.maxPerMob, 1, ServerTuning.MAX_ARROWS_PER_MOB);
        craftyShulker.cooldownTicks = clamp(craftyShulker.cooldownTicks, ServerTuning.MIN_CRAFTY_COOLDOWN, ServerTuning.MAX_CRAFTY_COOLDOWN);
        craftyShulker.radius = clamp(craftyShulker.radius, ServerTuning.MIN_CRAFTY_RADIUS, ServerTuning.MAX_CRAFTY_RADIUS);
        hammock.timeFactor = clamp(hammock.timeFactor, 1, ServerTuning.MAX_HAMMOCK_FACTOR);
        speakers.maxSpeakers = clamp(speakers.maxSpeakers, 0, ServerTuning.MAX_SPEAKERS);
        speakers.boostPercent = clamp(speakers.boostPercent, 0, ServerTuning.MAX_SPEAKER_BOOST_PERCENT);

        blueprint.maxBlocksPerTick = clamp(blueprint.maxBlocksPerTick, 1, ServerTuning.MAX_BLUEPRINT_BLOCKS_PER_TICK);

        trimStrengths.validate();
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /** Wie {@link #clamp(int, int, int)}; NaN/Unendlich aus handeditierten Dateien werden {@code fallback}. */
    public static double clamp(double value, double min, double max, double fallback) {
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }

    /**
     * Welche Funktionen es auf diesem Server gibt. Aus = die Funktion tut nichts mehr (Meldung in der
     * Aktionsleiste), und ihre Rezepte verschwinden beim naechsten Laden der Datenpakete
     * ({@code /reload}, Weltstart). Gesetzte Bloecke und vorhandene Gegenstaende bleiben.
     */
    public static class Features {
        @ConfigEntry.Gui.Tooltip
        public boolean endSignals = true;
        /** Astral-/Nihil-Kolben bewegen Bloecke (nur mit endSignals); aus: sie bleiben stehen, Rezepte fallen weg. */
        @ConfigEntry.Gui.Tooltip
        public boolean endPistons = true;
        @ConfigEntry.Gui.Tooltip
        public boolean astralVault = true;
        /** Die Verzauberung Luftsprung wirkt (unabhaengig vom Client-Schalter enableDoubleJump). */
        @ConfigEntry.Gui.Tooltip
        public boolean airJump = true;
        /** Leuchtende Ruestung setzt Lichtbloecke; aus: vorhandene verschwinden beim naechsten Takt. */
        @ConfigEntry.Gui.Tooltip
        public boolean dynamicLight = true;
        /** Vanilla-Kleinteile (Stock, Barren, Edelsteine, Ziegel ...) lassen sich ablegen (2026-10-02). */
        @ConfigEntry.Gui.Tooltip
        public boolean placeVanillaItems = true;
        /** Item-IDs, die sich nicht ablegen lassen (Komma/Leerzeichen getrennt, ohne Namensraum = minecraft). */
        @ConfigEntry.Gui.Tooltip
        public String placeDisabledItems = "";
        /** Reparieren ohne neue Verzauberung erhoeht die Ambosskosten nicht (2026-10-02). */
        @ConfigEntry.Gui.Tooltip
        public boolean anvilRepairKeepsCost = true;
        @ConfigEntry.Gui.Tooltip
        public boolean backpack = true;
        /** Anziehungsgeraet (Attractor, frueher Magnet). */
        @ConfigEntry.Gui.Tooltip
        public boolean attractor = true;
        @ConfigEntry.Gui.Tooltip
        public boolean echoSounder = true;
        @ConfigEntry.Gui.Tooltip
        public boolean blueprint = true;
        @ConfigEntry.Gui.Tooltip
        public boolean oreDetector = true;
        /** Schwebender und haengender Sand/Kies: nur die Rezepte (gesetzte Bloecke bleiben, wie sie sind). */
        @ConfigEntry.Gui.Tooltip
        public boolean levitatingBlocks = true;
        /**
         * Vogelscheuche (2026-10-03): im Umkreis so vieler Bloecke um einen Stroh-Ruestungsstaender oder eine
         * Trainingspuppe zertrampeln Tiere und Monster kein Ackerland. 0 = aus, hoechstens 16.
         */
        @ConfigEntry.Gui.Tooltip
        public int scarecrowRadius = 8;
    }

    public static class ChunkLoaders {
        /**
         * Ein Chunk-Loader haelt seine Chunks nur, solange sein Besitzer online ist (Besitzer-Entscheidung
         * 2026-09-28: an). Loader ohne Besitzer (vor der Besitzer-Speicherung gesetzt) laufen immer.
         */
        @ConfigEntry.Gui.Tooltip
        public boolean requireOwnerOnline = true;
    }

    /**
     * Dimensionen, in denen eine Funktion nicht wirkt: Dimension-IDs, durch Komma oder Leerzeichen
     * getrennt ({@code minecraft:the_nether, minecraft:the_end}); leer = ueberall erlaubt.
     */
    public static class DimensionLocks {
        @ConfigEntry.Gui.Tooltip
        public String chunkLoaderBlockedDimensions = "";
        @ConfigEntry.Gui.Tooltip
        public String flypadBlockedDimensions = "";
        @ConfigEntry.Gui.Tooltip
        public String echoSounderBlockedDimensions = "";
    }

    /** Was der Strahl der Amethystlinse entzuenden darf (Schmelzen, Trocknen, Kerzen bleiben). */
    public static class Laser {
        @ConfigEntry.Gui.Tooltip
        public boolean igniteFlammables = true;
        @ConfigEntry.Gui.Tooltip
        public boolean igniteTnt = true;
        @ConfigEntry.Gui.Tooltip
        public boolean igniteEntities = true;
        /** Lebewesen im Strahl leuchten kurz auf (Scannen, 2026-10-02). */
        @ConfigEntry.Gui.Tooltip
        public boolean scanEntities = true;
        /** Auch andere Spieler (nur mit PvP) leuchten auf; aus: Spieler werden nie gescannt. */
        @ConfigEntry.Gui.Tooltip
        public boolean scanPlayers = false;
    }

    /** Pfeile von Spielern, die in einem Lebewesen stecken, fallen bei dessen Tod (2026-10-02). */
    public static class Arrows {
        @ConfigEntry.Gui.Tooltip
        public boolean recoverFromMobs = true;
        /** Hoechstens so viele Pfeile merkt sich ein Lebewesen (1 bis 64). */
        @ConfigEntry.Gui.Tooltip
        public int maxPerMob = 16;
    }

    /** Effekt Listiger Shulker (Trank, 2026-10-02): Teleport bei Treffer. */
    public static class CraftyShulker {
        /** Ticks zwischen zwei Teleports desselben Wesens (20 bis 1200). */
        @ConfigEntry.Gui.Tooltip
        public int cooldownTicks = 60;
        /** Groesster Abstand des Teleports in Bloecken (2 bis 16). */
        @ConfigEntry.Gui.Tooltip
        public int radius = 8;
    }

    /** Haengematte (2026-10-02): Zeitraffer, solange genug Spieler tagsueber darin liegen. */
    public static class Hammock {
        /** Uhr-Ticks je Tick (1 bis 20; 1 = kein Zeitraffer). */
        @ConfigEntry.Gui.Tooltip
        public int timeFactor = 8;
    }

    /**
     * Lautsprecher (2026-10-03): Astralit-Lautsprecher verstaerken angrenzende Plattenspieler, Nihilit-Lautsprecher
     * angrenzende Notenbloecke (Lautstaerke und Hoerweite, ohne Echo). Hoechstens 2,5-fach.
     */
    public static class Speakers {
        /** Hoechstens so viele angrenzende Lautsprecher zaehlen je Quelle (0 bis 3; 0 = aus). */
        @ConfigEntry.Gui.Tooltip
        public int maxSpeakers = 2;
        /** Verstaerkung je Lautsprecher in Prozent der Vanilla-Lautstaerke (0 bis 50). */
        @ConfigEntry.Gui.Tooltip
        public int boostPercent = 50;
    }

    /** Erzvorkommen im End; wirken beim naechsten Weltstart und nur fuer neu erzeugte Chunks. */
    public static class OreGeneration {
        /** Hauptschalter fuer beide End-Erze. */
        @ConfigEntry.Gui.Tooltip
        public boolean endOres = true;
        @ConfigEntry.Gui.Tooltip
        public boolean astralitOre = true;
        @ConfigEntry.Gui.Tooltip
        public boolean nihilitOre = true;
        /** Weisheitserz in der Oberwelt (Stein und Tiefenschiefer), unabhaengig von den End-Erzen. */
        @ConfigEntry.Gui.Tooltip
        public boolean sageOre = true;
        /** Dimensions-Schrott in Oberwelt, Nether und End. */
        @ConfigEntry.Gui.Tooltip
        public boolean dimensionalScrap = true;
    }

    /** Wie lange Fremde (nicht der Besitzer, nicht Kreativ) zum Abbauen fremder Platten brauchen. */
    public static class Pads {
        /** Pads, Teleporter, Launchpads (bisher fest 60 s). */
        @ConfigEntry.Gui.Tooltip
        public int strangerPadBreakSeconds = 60;
        /** Chunk-Loader, Kupfer- und Filterplatten (bisher fest 10 s). */
        @ConfigEntry.Gui.Tooltip
        public int strangerPlateBreakSeconds = 10;
    }

    /**
     * Hoechstladung = Haltbarkeit der Gegenstaende. Wird beim Registrieren der Gegenstaende gelesen:
     * erst nach einem Neustart wirksam, und Client und Server brauchen denselben Wert (Modpack-Datei),
     * sonst zeigt der Client einen falschen Ladebalken.
     */
    public static class Charges {
        @ConfigEntry.Gui.Tooltip
        public int lensMaxCharge = 640;
        @ConfigEntry.Gui.Tooltip
        public int rotatorMaxCharge = 1024;
        @ConfigEntry.Gui.Tooltip
        public int echoSounderMaxCharge = 1500;
    }

    public static class Tools {
        /** Shared held/placed attractor dead zone, in blocks (0.5 to 2). */
        @ConfigEntry.Gui.Tooltip
        public double attractorMinimumDistance = 1.25;
        /** Dauer einer Hammer-Aufwertung in Sekunden = Zahl der Schlaege (einer je Sekunde, der letzte baut um). */
        @ConfigEntry.Gui.Tooltip
        public int sledgehammerUpgradeSeconds = 5;
        @ConfigEntry.Gui.Tooltip
        public int reinforcedUpgradeDamagePerHit = 2;
        @ConfigEntry.Gui.Tooltip
        public int netheriteUpgradeDamagePerHit = 4;
        @ConfigEntry.Gui.Tooltip
        public int enderiteUpgradeDamagePerHit = 10;
        /** Abklingzeit der Meissel und Spachtel je Stufe (Ticks, vor Fast Chiseling). */
        @ConfigEntry.Gui.Tooltip
        public int stoneChiselCooldownTicks = 30;
        @ConfigEntry.Gui.Tooltip
        public int copperChiselCooldownTicks = 25;
        @ConfigEntry.Gui.Tooltip
        public int ironChiselCooldownTicks = 25;
        @ConfigEntry.Gui.Tooltip
        public int goldChiselCooldownTicks = 20;
        @ConfigEntry.Gui.Tooltip
        public int diamondChiselCooldownTicks = 10;
        @ConfigEntry.Gui.Tooltip
        public int netheriteChiselCooldownTicks = 5;
        @ConfigEntry.Gui.Tooltip
        public int enderiteChiselCooldownTicks = 5;
    }

    /** Tempo der Maschinenstufen als Vielfaches von Vanilla (1..8). */
    public static class Machines {
        @ConfigEntry.Gui.Tooltip
        public int endSignalRange = 15;
        /** Wartezeit nach dem Ausloesen eines Astral-/Nihil-Kolbens, 4..100 Ticks. */
        @ConfigEntry.Gui.Tooltip
        public int endPistonCooldownTicks = 8;
        @ConfigEntry.Gui.Tooltip
        public int reinforcedHopperSpeed = 2;
        @ConfigEntry.Gui.Tooltip
        public int netheriteHopperSpeed = 4;
        @ConfigEntry.Gui.Tooltip
        public int enderiteHopperSpeed = 8;
        /** Ofen, Raeucherofen und Schmelzofen derselben Stufe. */
        @ConfigEntry.Gui.Tooltip
        public int reinforcedFurnaceSpeed = 2;
        @ConfigEntry.Gui.Tooltip
        public int netheriteFurnaceSpeed = 4;
        @ConfigEntry.Gui.Tooltip
        public int enderiteFurnaceSpeed = 8;
    }

    public static class OreDetector {
        /** Faktor auf Reichweite und Suchkugel aller Erzklassen. */
        @ConfigEntry.Gui.Tooltip
        public double rangeMultiplier = 1.0;
        /** Ticks zwischen zwei Pings in der Haupthand; die Nebenhand pingt halb so oft. */
        @ConfigEntry.Gui.Tooltip
        public int scanIntervalTicks = 20;
    }

    /**
     * Beute und Handel der Mod. Wirkt beim Laden der Datenpakete ({@code /reload}, Weltstart), der
     * Preisfaktor fuer jedes neu erzeugte Angebot.
     */
    public static class Loot {
        /** Faktor auf alle Beute-Pools der Mod (0 = keine, 2 = doppelt so viel; Koepfe ausgenommen). */
        @ConfigEntry.Gui.Tooltip
        public double globalLootMultiplier = 1.0;
        @ConfigEntry.Gui.Tooltip
        public boolean strongholdLoot = true;
        @ConfigEntry.Gui.Tooltip
        public boolean endCityLoot = true;
        @ConfigEntry.Gui.Tooltip
        public boolean ancientCityLoot = true;
        @ConfigEntry.Gui.Tooltip
        public boolean bastionLoot = true;
        @ConfigEntry.Gui.Tooltip
        public boolean netherFortressLoot = true;
        @ConfigEntry.Gui.Tooltip
        public boolean pillagerOutpostLoot = true;
        @ConfigEntry.Gui.Tooltip
        public boolean woodlandMansionLoot = true;
        @ConfigEntry.Gui.Tooltip
        public boolean buriedTreasureLoot = true;
        @ConfigEntry.Gui.Tooltip
        public boolean dungeonLoot = true;
        @ConfigEntry.Gui.Tooltip
        public boolean shipwreckLoot = true;
        @ConfigEntry.Gui.Tooltip
        public boolean iglooLoot = true;
        @ConfigEntry.Gui.Tooltip
        public boolean mineshaftLoot = true;
        @ConfigEntry.Gui.Tooltip
        public boolean trialChambersLoot = true;
        @ConfigEntry.Gui.Tooltip
        public boolean ruinedPortalLoot = true;
        @ConfigEntry.Gui.Tooltip
        public boolean fishingLoot = true;
        /** Faktor auf den Preis aller Handelsangebote der Mod (Dorfbewohner und fahrender Haendler). */
        @ConfigEntry.Gui.Tooltip
        public double tradePriceMultiplier = 1.0;
        /**
         * Prozent je Loot-Truhe in Festung, Bastion/Netherfestung, End-Stadt/End-Schiff, die als Stufen-Truhe mit
         * doppelter Beute entsteht (BetterChests; gilt fuer neu erzeugte Strukturen). Hoechstens 5.
         */
        @ConfigEntry.Gui.Tooltip
        public double betterChestPercent = 1.0;
        /** Prozent je Shulker einer neu erzeugten End-Stadt, der verstaerkt wird (RareShulkers). Hoechstens 10. */
        @ConfigEntry.Gui.Tooltip
        public double reinforcedShulkerPercent = 2.0;
        /** Prozent je Shulker einer neu erzeugten End-Stadt, der zum Enderit-Shulker wird. Hoechstens 5. */
        @ConfigEntry.Gui.Tooltip
        public double enderiteShulkerPercent = 0.5;
        /** Prozent je Shulker einer neu erzeugten End-Stadt, der zum Netherit-Shulker wird. Hoechstens 5. */
        @ConfigEntry.Gui.Tooltip
        public double netheriteShulkerPercent = 1.0;
        /**
         * Endermiten je seltenem Shulker, einmalig, sobald ein Spieler in die Naehe kommt (RareShulkers; nicht dauerhaft).
         * Hoechstens 8.
         */
        @ConfigEntry.Gui.Tooltip
        public int endermitesPerRareShulker = 4;
    }

    public static class Blueprint {
        /** Hoechstens so viele Stellen setzt ein Blaupausen-Bau je Tick (der Bau dauert dann laenger). */
        @ConfigEntry.Gui.Tooltip
        public int maxBlocksPerTick = 32768;
    }

    /**
     * Staerke jeder Besatz-Wirkung als Faktor auf ihre Rate (0 = aus, 2 = doppelt); die Deckel
     * (Schadensboden, Hoechstwerte) gelten weiter.
     */
    public static class TrimStrengths {
        @ConfigEntry.Gui.Tooltip
        public double projectileProtection = 1.0;
        @ConfigEntry.Gui.Tooltip
        public double magicProtection = 1.0;
        @ConfigEntry.Gui.Tooltip
        public double thornProtection = 1.0;
        @ConfigEntry.Gui.Tooltip
        public double blastProtection = 1.0;
        @ConfigEntry.Gui.Tooltip
        public double drowningProtection = 1.0;
        @ConfigEntry.Gui.Tooltip
        public double breathSaving = 1.0;
        @ConfigEntry.Gui.Tooltip
        public double allProtection = 1.0;
        @ConfigEntry.Gui.Tooltip
        public double sonicProtection = 1.0;
        @ConfigEntry.Gui.Tooltip
        public double stealth = 1.0;
        @ConfigEntry.Gui.Tooltip
        public double fireProtection = 1.0;
        @ConfigEntry.Gui.Tooltip
        public double witherProtection = 1.0;
        @ConfigEntry.Gui.Tooltip
        public double witherShortening = 1.0;
        @ConfigEntry.Gui.Tooltip
        public double dragonBreathProtection = 1.0;
        @ConfigEntry.Gui.Tooltip
        public double fallProtection = 1.0;
        @ConfigEntry.Gui.Tooltip
        public double windChargeProtection = 1.0;
        @ConfigEntry.Gui.Tooltip
        public double lightningProtection = 1.0;
        @ConfigEntry.Gui.Tooltip
        public double walkingSpeed = 1.0;
        @ConfigEntry.Gui.Tooltip
        public double swimmingSpeed = 1.0;
        @ConfigEntry.Gui.Tooltip
        public double sprintHunger = 1.0;
        @ConfigEntry.Gui.Tooltip
        public double experience = 1.0;
        @ConfigEntry.Gui.Tooltip
        public double luck = 1.0;
        @ConfigEntry.Gui.Tooltip
        public double blockReach = 1.0;
        @ConfigEntry.Gui.Tooltip
        public double physicalProtection = 1.0;
        @ConfigEntry.Gui.Tooltip
        public double illagerProtection = 1.0;
        @ConfigEntry.Gui.Tooltip
        public double witherPiercingProtection = 1.0;
        @ConfigEntry.Gui.Tooltip
        public double healingChance = 1.0;
        @ConfigEntry.Gui.Tooltip
        public double knockbackResistance = 1.0;

        void validate() {
            for (java.lang.reflect.Field field : TrimStrengths.class.getFields()) {
                if (field.getType() != double.class || java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                try {
                    field.setDouble(this, clamp(field.getDouble(this), 0.0, ServerTuning.MAX_TRIM_STRENGTH, 1.0));
                } catch (IllegalAccessException e) {
                    // oeffentliche Felder: kommt nicht vor
                }
            }
        }

        /**
         * Faktor fuer einen Bonus-Schluessel aus {@code TrimBonusCatalog} ({@code fire_protection},
         * {@code walking_speed} ...), begrenzt auf 0..2; unbekannte Schluessel 1.
         */
        public float of(String bonusKey) {
            java.lang.reflect.Field field = ServerTuning.trimStrengthField(bonusKey);
            if (field == null) {
                return 1.0f;
            }
            try {
                return (float) clamp(field.getDouble(this), 0.0, ServerTuning.MAX_TRIM_STRENGTH, 1.0);
            } catch (IllegalAccessException e) {
                return 1.0f;
            }
        }
    }
}

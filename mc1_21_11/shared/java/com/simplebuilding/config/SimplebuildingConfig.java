package com.simplebuilding.config;

import com.simplebuilding.Simplebuilding;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

/**
 * Die Config der Mod ({@code config/simplebuilding.json}).
 *
 * <p><b>Aufbau des Bildschirms</b> (Config-Umbau 2026-09-28): jedes Feld der obersten Ebene traegt
 * eine {@link ConfigEntry.Category}; Cloth zeigt die Kategorien als Reiter in der Reihenfolge, in
 * der sie hier zuerst vorkommen - Wichtiges zuerst, Technisches zuletzt. Die Gruppen {@code tools},
 * {@code worldGen} und {@code tweaks} stehen mit {@link ConfigEntry.Gui.TransitiveObject} flach in
 * ihrem Reiter; so aendert sich am JSON-Aufbau nichts (Schluessel und Verschachtelung bleiben, alte
 * Dateien laden unveraendert). Die Reihenfolge der Felder bestimmt nur die Anzeige, Gson liest nach
 * Namen. Namen und Tooltips: {@code text.autoconfig.simplebuilding.option.<pfad>} in den Sprachdateien;
 * {@code ConfigOptionTests} verlangt fuer jede sichtbare Option beides auf Englisch und Deutsch.
 *
 * <p>Was der Client vom Server braucht, schickt der Server mit ({@code TweaksConfigPayload},
 * {@code PistonConfigPayload}, {@code TrimDataPayload}); reine Client-Optionen listet
 * {@link ConfigOptions#CLIENT_SIDE}. Der Reiter "Server & Modpack Tuning" ({@link ServerTuningConfig})
 * geht als Ganzes an die Clients und gilt dort statt ihrer eigenen Datei. Ueberblick: docs/CONFIG.md.
 */
@Config(name = Simplebuilding.MOD_ID)
public class SimplebuildingConfig implements ConfigData {

    // =====================================================================================
    // Reiter 1: Werkzeuge & Bauen
    // =====================================================================================

    @ConfigEntry.Category("building")
    @ConfigEntry.Gui.TransitiveObject
    public Tools tools = new Tools();

    // =====================================================================================
    // Reiter 2: Verzauberungen & Ruestung
    // =====================================================================================

    @ConfigEntry.Category("equipment")
    @ConfigEntry.Gui.Tooltip
    public boolean enableDoubleJump = true;

    // Air-jump cooldown (ticks) at DOUBLE_JUMP level 1; level 2 uses half of this. 20 ticks = 1s.
    // Der Server schickt seinen Wert an die Clients (TweaksConfigPayload), damit die Leiste und der
    // Server-Waechter (AirJumpGuard) dieselbe Abklingzeit rechnen.
    @ConfigEntry.Category("equipment")
    @ConfigEntry.Gui.Tooltip
    public int airJumpCooldownTicks = 100;

    @ConfigEntry.Category("equipment")
    @ConfigEntry.Gui.Tooltip
    public boolean enableArmorTrimBenefits = true;

    // Basis der Besatz-Resonanz (TrimMultiplierLogic). Wird gespeichert; der Befehl
    // /simplebuilding config setTrimMultiplier schreibt ihn in die Datei (ConfigSaving), und der
    // Server schickt ihn mit dem TrimDataPayload an die Clients (Anzeige im TrimStatsPanel).
    // Bis 2026-09-26 statisch: nach einem Neustart weg, und jeder Client zeigte seinen eigenen Wert.
    @ConfigEntry.Category("equipment")
    @ConfigEntry.Gui.Tooltip
    public double trimBenefitBaseMultiplier = 2.0;

    // Obergrenze des Befehls und der geladenen Datei; fest, nicht gespeichert, nicht im Bildschirm.
    @ConfigEntry.Gui.Excluded
    public static double maxMultiplierLimit = 10.0;

    // =====================================================================================
    // Reiter 3: Kolben
    // =====================================================================================

    // Haltbarkeit der Brecher (2026-09-28, ersetzt die Verschleissbudgets vom 2026-09-27):
    // Netheritkolben 226 (1/9 der Netheritspitzhacke), Enderitkolben 281 (1/9 der Enderitspitzhacke),
    // 1 je beim Ausfahren zerstoertem Block. Aufgebraucht zerfaellt Enderit zum Netheritkolben,
    // Netherit zum verstaerkten Kolben. Aus: die Brecher verlieren nie Haltbarkeit. Die
    // Hoechstwerte sind fest (Wertebereich der Blockeigenschaft, auf Server und Client gleich).
    // Siehe NetheriteBreakerPistonBlock.
    @ConfigEntry.Category("pistons")
    @ConfigEntry.Gui.Tooltip
    public boolean breakerPistonsLoseDurability = true;

    // Ob die Kolben der Mod Endportalrahmen schieben (verstaerkte Kolben) bzw. zerstoeren
    // (Netherit-/Enderitkolben). Aus (Standard seit 2026-09-26, Besitzer-Entscheidung): der Rahmen
    // zaehlt wie ein immuner Block, siehe PistonBreach.
    @ConfigEntry.Category("pistons")
    @ConfigEntry.Gui.Tooltip
    public boolean pistonsBreachEndPortalFrames = false;

    // Ob die Kolben der Mod auch unzerstoerbare Bloecke (Haerte -1) ANDERER Mods schieben bzw.
    // zerstoeren. Aus (Standard, Audit #24): nur der Namensraum minecraft und der Tag
    // simplebuilding:piston_breachable_extra, siehe PistonBreach.
    @ConfigEntry.Category("pistons")
    @ConfigEntry.Gui.Tooltip
    public boolean pistonsBreachModdedUnbreakables = false;

    // =====================================================================================
    // Reiter 4: Pads & Tweaks (aus Simple Tweaks, siehe TweaksConfig)
    // =====================================================================================

    @ConfigEntry.Category("tweaks")
    @ConfigEntry.Gui.TransitiveObject
    public com.simplebuilding.tweaks.TweaksConfig tweaks = new com.simplebuilding.tweaks.TweaksConfig();

    // =====================================================================================
    // Reiter 5: Welt, Beute & Handel
    // =====================================================================================

    @ConfigEntry.Category("world")
    @ConfigEntry.Gui.TransitiveObject
    public WorldGen worldGen = new WorldGen();

    // Beim ersten Betreten einer Welt bzw. eines Servers bekommt jeder Spieler einmal das
    // Einsteiger-Handbuch (GuideBooks, Spieler-Tag simplebuilding.guide_book_given). Aus: nichts
    // wird verschenkt und nichts gemerkt - spaeter eingeschaltet, bekommt es jeder einmal.
    @ConfigEntry.Category("world")
    @ConfigEntry.Gui.Tooltip
    public boolean giveGuideBookOnFirstJoin = true;

    // =====================================================================================
    // Reiter 6: Darstellung (nur Client)
    // =====================================================================================

    // Client: eigene Buch-Texturen fuer die Vanilla-Verzauberungen (VanillaBookTextures). Aus: das
    // verzauberte Buch zeigt fuer Vanilla-Verzauberungen wieder das Vanilla-Modell, damit
    // Ressourcenpakete oder andere Mods, die dieselben Buecher ueberschreiben, gewinnen.
    @ConfigEntry.Category("visuals")
    @ConfigEntry.Gui.Tooltip
    public boolean vanillaEnchantedBookTextures = true;

    // Client: eigene Buch-Texturen fuer die Mod-Verzauberungen (VanillaBookTextures.select). Aus: das
    // schlichte Vanilla-Buch (Paritaet mit Vanilla).
    @ConfigEntry.Category("visuals")
    @ConfigEntry.Gui.Tooltip
    public boolean modEnchantedBookTextures = true;

    // Client: sichtbare Besatzmuster auf den Icons von Vanilla- bzw. Mod-Ruestung (VisibleTrimIcons).
    // Aus: Vanillas Icon mit nur dem Materialfleck. Beide wirken sofort.
    @ConfigEntry.Category("visuals")
    @ConfigEntry.Gui.Tooltip
    public boolean visibleTrimIconsVanillaArmor = true;
    @ConfigEntry.Category("visuals")
    @ConfigEntry.Gui.Tooltip
    public boolean visibleTrimIconsModArmor = true;

    // Client: die HUD-Kaesten der Mod (Tacho, Entfernungsmesser, Luftsprung-Leiste, Spawn-Elytra).
    // showModHud schaltet auch die HUD-Taste (ModHud.toggle) und bleibt gespeichert; Position in
    // Prozent der Bildschirmbreite/-hoehe (0 = links/oben, 100 = rechts/unten), Groesse in Prozent.
    @ConfigEntry.Category("visuals")
    @ConfigEntry.Gui.Tooltip
    public boolean showModHud = true;
    @ConfigEntry.Category("visuals")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int hudPositionX = 0;
    @ConfigEntry.Category("visuals")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int hudPositionY = 50;
    @ConfigEntry.Category("visuals")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 50, max = 200)
    public int hudScale = 100;

    // =====================================================================================
    // Reiter 7: Kompatibilitaet & Erweitert
    // =====================================================================================

    // Ob der Kolben-Waechter vor jedem Kolben-Abbau das Abbau-Ereignis des Loaders mit einem
    // Fake-Spieler feuert (NeoForge BreakBlockEvent, Fabric PlayerBlockBreakEvents), damit
    // Schutz-Mods Claims schuetzen. Aus: nur das Kolben-Ereignis (NeoForge/Forge) und die
    // Weltgrenze; fuer Schutz-Mods, die jeden Fake-Spieler sperren, oder Quest-/Statistik-Mods,
    // die den Abbau sonst einem Spieler zuschreiben (Audit N4). Siehe PistonBreakGuard.
    @ConfigEntry.Category("advanced")
    @ConfigEntry.Gui.Tooltip
    public boolean pistonsFireBreakEvents = true;

    // Zeigt den Kreativ-Tab "SimpleEnchants (Dev)" auch ausserhalb einer
    // Entwicklungsumgebung (dort ist er immer da), siehe DevEnchantedTab. Wirkt beim naechsten
    // Neuaufbau der Kreativ-Tabs, also spaetestens nach dem erneuten Betreten der Welt.
    @ConfigEntry.Category("advanced")
    @ConfigEntry.Gui.Tooltip
    public boolean showDevEnchantedTab = false;

    // =====================================================================================
    // Reiter 8: Server & Modpack Tuning (Besitzer 2026-09-28)
    // =====================================================================================

    // Jede Gameplay-Stellschraube fuer Server- und Modpack-Ersteller, serverseitig verbindlich: die
    // Clients bekommen den Abschnitt vom Server (TweaksConfigPayload) und lesen ihn ueber
    // ServerTuning.get(). Siehe ServerTuningConfig.
    @ConfigEntry.Category("server")
    @ConfigEntry.Gui.TransitiveObject
    public ServerTuningConfig server = new ServerTuningConfig();

    /** Nach dem Laden (und fuer eine neue Datei): alte Schluessel auf ihre Nachfolger verteilen, Werte begrenzen. */
    @Override
    public void validatePostLoad() {
        if (tweaks != null && tweaks.spawn != null && tweaks.spawn.migrateLegacyFirstJoinCount()) {
            // Sofort zurueckschreiben, sonst bleibt der alte Schluessel bis zum naechsten Speichern in
            // der Datei und die Migration laeuft bei jedem Laden erneut (Nach-Audit N12).
            com.simplebuilding.tweaks.SimpleTweaks.requestConfigSave();
        }
        if (Double.isNaN(trimBenefitBaseMultiplier)) {
            trimBenefitBaseMultiplier = 2.0;
        }
        trimBenefitBaseMultiplier = Math.max(0.0, Math.min(maxMultiplierLimit, trimBenefitBaseMultiplier));
        airJumpCooldownTicks = Math.max(0, airJumpCooldownTicks);
        hudPositionX = Math.max(0, Math.min(100, hudPositionX));
        hudPositionY = Math.max(0, Math.min(100, hudPositionY));
        hudScale = Math.max(50, Math.min(200, hudScale));
        if (tools == null) {
            tools = new Tools();
        }
        tools.validate();
        if (worldGen == null) {
            worldGen = new WorldGen();
        }
        worldGen.validate();
        if (tweaks != null) {
            tweaks.validate();
        }
        if (server == null) {
            server = new ServerTuningConfig();
        }
        server.validate();
    }

    /** Endliche, nicht negative Zahl; sonst {@code fallback} (NaN/Unendlich aus handeditierten Dateien). */
    public static double nonNegative(double value, double fallback) {
        return Double.isFinite(value) ? Math.max(0.0, value) : fallback;
    }

    public static class Tools {
        // --- Spielmechanik (Server) ---

        // EXPERIMENTELL: Bauen mit dem Baustab (Flaeche und Blaupause) kostet Erschoepfung je Block,
        // billiger fuer staerkere Staebe; Kreativmodus ausgenommen. Tabelle in util/WandHunger.
        @ConfigEntry.Gui.Tooltip
        public boolean buildingWandHungerCost = true;

        // Faktor auf die Erschoepfung je bezahltem Baustab-Block (WandHunger.exhaust); 1.0 = die
        // Tabelle, 0.5 = halb so hungrig, 0 = kostenlos (wie der Schalter aus). Der Freibetrag bleibt.
        @ConfigEntry.Gui.Tooltip
        public double wandHungerMultiplier = 1.0;

        // Faktor auf die Anziehungsreichweite des Magneten (MagnetItem: 4 Bloecke, 8 mit Beruehrung
        // des Konstrukteurs, +2 je Radius-Stufe).
        @ConfigEntry.Gui.Tooltip
        public double magnetRangeMultiplier = 1.0;

        // Ladung, die eine Drehung des Rotators kostet (RotatorItem; 1024 Ladung = voll, 16
        // Enderperlen laden auf). 0 = Drehen kostet nichts.
        @ConfigEntry.Gui.Tooltip
        public int rotatorChargePerTurn = 1;

        // --- Bedienung (je Spieler) ---

        @ConfigEntry.Gui.Tooltip
        public boolean invertBundleInteractions = false;

        @ConfigEntry.Gui.Tooltip
        public boolean invertOctantSneak = false; // Constructor's Touch Invertierung

        // --- Darstellung (Client) ---

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
        public int buildingHighlightOpacity = 40;

        @ConfigEntry.Gui.Tooltip
        public boolean enableToolAnimations = true; // Hauptschalter
        @ConfigEntry.Gui.Tooltip
        public boolean enableChiselAnimation = true;

        void validate() {
            wandHungerMultiplier = nonNegative(wandHungerMultiplier, 1.0);
            magnetRangeMultiplier = nonNegative(magnetRangeMultiplier, 1.0);
            rotatorChargePerTurn = Math.max(0, rotatorChargePerTurn);
        }
    }

    public static class WorldGen {
        @ConfigEntry.Gui.Tooltip
        public boolean enableLootTableChanges = true;

        // Faktor auf die Kern-Chancen in Beutetruhen (ModLootTableModifications.rareCore; je Kiste
        // hoechstens 1). 0 = keine Baukerne in Truhen. Wirkt beim Laden der Datenpakete (/reload).
        @ConfigEntry.Gui.Tooltip
        public double buildingCoreLootChanceMultiplier = 1.0;

        @ConfigEntry.Gui.Tooltip
        public boolean enableVillagerTrades = true;

        @ConfigEntry.Gui.Tooltip
        public boolean enableWanderingTrades = true;

        void validate() {
            buildingCoreLootChanceMultiplier = nonNegative(buildingCoreLootChanceMultiplier, 1.0);
        }
    }
}

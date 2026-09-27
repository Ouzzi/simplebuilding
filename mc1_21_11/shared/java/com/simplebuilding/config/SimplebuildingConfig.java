package com.simplebuilding.config;

import com.simplebuilding.Simplebuilding;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = Simplebuilding.MOD_ID)
public class SimplebuildingConfig implements ConfigData {

    @ConfigEntry.Gui.CollapsibleObject
    public Tools tools = new Tools();

    @ConfigEntry.Gui.CollapsibleObject
    public WorldGen worldGen = new WorldGen();

    // Aus Simple Tweaks uebernommen (Druckplatten/Pads, Spawn, XP, Laser...), siehe TweaksConfig.
    @ConfigEntry.Gui.CollapsibleObject
    public com.simplebuilding.tweaks.TweaksConfig tweaks = new com.simplebuilding.tweaks.TweaksConfig();

    @ConfigEntry.Gui.Tooltip
    public boolean enableDoubleJump = true;

    /** Nach dem Laden (und fuer eine neue Datei): alte Schluessel auf ihre Nachfolger verteilen. */
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
        netheriteBreakerWearBudget = Math.max(0, netheriteBreakerWearBudget);
        enderitePistonWearBudget = Math.max(0, enderitePistonWearBudget);
    }

    // Air-jump cooldown (ticks) at DOUBLE_JUMP level 1; level 2 uses half of this. 20 ticks = 1s.
    @ConfigEntry.Gui.Tooltip
    public int airJumpCooldownTicks = 100;

    @ConfigEntry.Gui.Tooltip
    public boolean enableArmorTrimBenefits = true;

    // Ob die Kolben der Mod Endportalrahmen schieben (verstaerkte Kolben) bzw. zerstoeren
    // (Netherit-/Enderitkolben). Aus (Standard seit 2026-09-26, Besitzer-Entscheidung): der Rahmen
    // zaehlt wie ein immuner Block, siehe PistonBreach.
    @ConfigEntry.Gui.Tooltip
    public boolean pistonsBreachEndPortalFrames = false;

    // Ob die Kolben der Mod auch unzerstoerbare Bloecke (Haerte -1) ANDERER Mods schieben bzw.
    // zerstoeren. Aus (Standard, Audit #24): nur der Namensraum minecraft und der Tag
    // simplebuilding:piston_breachable_extra, siehe PistonBreach.
    @ConfigEntry.Gui.Tooltip
    public boolean pistonsBreachModdedUnbreakables = false;

    // Ob der Kolben-Waechter vor jedem Kolben-Abbau das Abbau-Ereignis des Loaders mit einem
    // Fake-Spieler feuert (NeoForge BreakBlockEvent, Fabric PlayerBlockBreakEvents), damit
    // Schutz-Mods Claims schuetzen. Aus: nur das Kolben-Ereignis (NeoForge/Forge) und die
    // Weltgrenze; fuer Schutz-Mods, die jeden Fake-Spieler sperren, oder Quest-/Statistik-Mods,
    // die den Abbau sonst einem Spieler zuschreiben (Audit N4). Siehe PistonBreakGuard.
    @ConfigEntry.Gui.Tooltip
    public boolean pistonsFireBreakEvents = true;

    // Verschleiss des Netherit-Brechers (Audit #23): so viele Haertepunkte (je Block
    // max(1, aufgerundete Haerte)) bricht er, bevor er zum verstaerkten Kolben zerfaellt.
    // 8 sichtbare Stufen je netheriteBreakerWearBudget / 8 Punkte. 0 = kein Verschleiss.
    // Siehe NetheriteBreakerPistonBlock.
    @ConfigEntry.Gui.Tooltip
    public int netheriteBreakerWearBudget = 1024;

    // Verschleiss des Enderitkolbens (Kolben-Balance 2026-09-27), wie beim Netherit-Brecher,
    // aber doppelt so viel: 2048 Punkte = 256 je Stufe, rund 1000 Steine oder 680 Tiefenschiefer
    // bis zur Reparatur (Enderitklumpen); verbraucht wird er zum Netheritkolben. 0 = kein
    // Verschleiss. Siehe EnderitePistonBlock.
    @ConfigEntry.Gui.Tooltip
    public int enderitePistonWearBudget = 2048;

    // Zeigt den Kreativ-Tab "SimpleEnchants (Dev)" auch ausserhalb einer
    // Entwicklungsumgebung (dort ist er immer da), siehe DevEnchantedTab. Wirkt beim naechsten
    // Neuaufbau der Kreativ-Tabs, also spaetestens nach dem erneuten Betreten der Welt.
    @ConfigEntry.Gui.Tooltip
    public boolean showDevEnchantedTab = false;

    // Client: eigene Buch-Texturen fuer die Vanilla-Verzauberungen (VanillaBookTextures). Aus: das
    // verzauberte Buch zeigt fuer Vanilla-Verzauberungen wieder das Vanilla-Modell, damit
    // Ressourcenpakete oder andere Mods, die dieselben Buecher ueberschreiben, gewinnen.
    @ConfigEntry.Gui.Tooltip
    public boolean vanillaEnchantedBookTextures = true;

    // Client: eigene Buch-Texturen fuer die Mod-Verzauberungen (VanillaBookTextures.select). Aus: das
    // schlichte Vanilla-Buch (Paritaet mit Vanilla).
    @ConfigEntry.Gui.Tooltip
    public boolean modEnchantedBookTextures = true;

    // Client: sichtbare Besatzmuster auf den Icons von Vanilla- bzw. Mod-Ruestung (VisibleTrimIcons).
    // Aus: Vanillas Icon mit nur dem Materialfleck. Beide wirken sofort.
    @ConfigEntry.Gui.Tooltip
    public boolean visibleTrimIconsVanillaArmor = true;
    @ConfigEntry.Gui.Tooltip
    public boolean visibleTrimIconsModArmor = true;

    // Basis der Besatz-Resonanz (TrimMultiplierLogic). Wird gespeichert; der Befehl
    // /simplebuilding config setTrimMultiplier schreibt ihn in die Datei (ConfigSaving), und der
    // Server schickt ihn mit dem TrimDataPayload an die Clients (Anzeige im TrimStatsPanel).
    // Bis 2026-09-26 statisch: nach einem Neustart weg, und jeder Client zeigte seinen eigenen Wert.
    @ConfigEntry.Gui.Tooltip
    public double trimBenefitBaseMultiplier = 2.0;
    // Obergrenze des Befehls und der geladenen Datei; fest, nicht gespeichert.
    @ConfigEntry.Gui.Tooltip
    public static double maxMultiplierLimit = 10.0;

    public static class Tools {
        @ConfigEntry.Gui.Tooltip
        public boolean invertOctantSneak = false; // Constructor's Touch Invertierung
        public int buildingHighlightOpacity = 40;

        @ConfigEntry.Gui.Tooltip
        public boolean enableToolAnimations = true; // Hauptschalter
        @ConfigEntry.Gui.Tooltip
        public boolean enableChiselAnimation = true;

        @ConfigEntry.Gui.Tooltip
        public boolean invertBundleInteractions = false;

        // EXPERIMENTELL: Bauen mit dem Baustab (Flaeche und Blaupause) kostet Erschoepfung je Block,
        // billiger fuer staerkere Staebe; Kreativmodus ausgenommen. Tabelle in util/WandHunger.
        @ConfigEntry.Gui.Tooltip
        public boolean buildingWandHungerCost = true;
    }

    public static class WorldGen {
        @ConfigEntry.Gui.Tooltip
        public boolean enableVillagerTrades = true;

        @ConfigEntry.Gui.Tooltip
        public boolean enableWanderingTrades = true;

        @ConfigEntry.Gui.Tooltip
        public boolean enableLootTableChanges = true;
    }


}
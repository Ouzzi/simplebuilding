# Plan A: Simple Beacons – Leuchtfeuer-Kräfte, Material-Reichweite, Strong Conduit (SP22-9, SP22-10)

Branch `a-beacons22` (Basis `claude-wave1`), Modul `modules/simplebeacons`. Besitzer-Wortlaut: `.claude/QUEUE.md`,
Abschnitt „Besitzer 22.10.“, SP22-9 und SP22-10.

## Ist-Zustand
- Starkes Leuchtfeuer (`StrongBeaconLogic.EFFECTS`): Ebene 1 Tempo/Eile, 2 Resistenz/Sprungkraft, 3 Stärke,
  4 Regeneration (Sekundär), 5 Nachtsicht/Wasseratmung/Langsamer Fall, 6 Feuerresistenz/Delfingunst/Absorption.
  Im Bildschirm stehen links fünf Zeilen (1, 2, 3, 5, 6); die Zeilen 5 und 6 hat der Besitzer als „Stufe 4 und 5“
  gezählt (sie haben schon 3 Kräfte). Reichweite `10 + 10 × Ebenen`, unabhängig vom Material.
- Vanilla-Leuchtfeuer: Reichweite `10 + 10 × Ebenen`, keine Material-Wirkung.
- Starker Aquisator: Reichweite `(Rahmenblöcke / 7) × 16`, Deckel `strongConduitMaxRange` 144 (96–144); mit dem
  5×5×5-Rahmen also genau Vanilla (96), nur der 7×7×7-Rahmen bringt 144. Wirkung Meereskraft + Delfingunst.
- Vanilla-Aquisator: kein Eingriff des Moduls (keine Mixins auf `ConduitBlockEntity`).

## SP22-9 Starker Aquisator: größerer Bereich
- Keine neue Fähigkeit; Delfingunst bleibt (Besitzer: „ist schon ziemlich gut“).
- Reichweite neu `(Rahmenblöcke / 7) × 24` (1,5 × Vanilla): 5×5×5-Rahmen 144 statt 96, 7×7×7-Rahmen 216 statt 144.
  Damit hebt er sich schon mit dem Vanilla-Rahmen ab. `strongConduitMaxRange` Standard 216, Bereich 96–256.
  Jenseits von 96 Blöcken zählen weiter nur Spieler wirklich unter Wasser (unverändert).
- „Normaler Conduit nur Wasseratmung prüfen“: neuer Gametest `vanilla_conduit_only_conduit_power` baut einen
  Vanilla-Aquisator mit 5×5×5-Rahmen und prüft nach dem Vanilla-Takt (40 Ticks): Schwimmer hat Meereskraft
  (= Wasseratmung), aber keine Delfingunst. Code-Befund: das Modul berührt den Vanilla-Aquisator nicht.

## SP22-10 Leuchtfeuer
### Kräfte je Stufe (nur Starkes Leuchtfeuer)
| Stufe | bisher | neu | Begründung |
|---|---|---|---|
| 1 | Tempo, Eile | **Glück** | Nutzkraft wie Eile, kaum Kampfwert (nur Angeln/Beute) – passt zur billigsten Stufe (9 Blöcke). |
| 2 | Resistenz, Sprungkraft | **Extraherzen I** (Health Boost) | Schutz-Thema wie Resistenz; +2 Herzen. Wird wie Absorption nur verlängert, nicht neu angewandt (sonst würde die Maximal-Gesundheit alle 4 s zurückgesetzt). |
| 3 | Stärke | **Unsichtbarkeit**, **Held des Dorfes I** | Unsichtbarkeit ist der einzige braubare Vanilla-Trank, der noch keine Leuchtfeuer-Kraft ist. Held des Dorfes (Raid-Belohnung) senkt Handelspreise – wirtschaftlich stark, deshalb erst ab Stufe 3 (83 Blöcke). |
| 4 | Regeneration (Sekundär) | – | unverändert |
| 5 | Nachtsicht, Wasseratmung, Langsamer Fall | – | hat schon 3 |
| 6 | Feuerresistenz, Delfingunst, Absorption | – | hat schon 3 |

Verworfen: Sättigung und Sofortheilung (wirken jeden Tick bzw. alle 4 s – stärker als Regeneration II),
Meereskraft und Atem des Nautilus (doppeln Wasseratmung), Leuchten (kaum Nutzen).
Alle neuen Kräfte haben wie die Stufe-5/6-Kräfte **keine Stufe II** (kein „II“ für Glück/Extraherzen/Held) und je einen
Server-Schalter in `strongBeaconEffects` (`luck`, `health_boost`, `invisibility`, `hero_of_the_village`, Standard an).
Das Vanilla-Leuchtfeuer bekommt **keine** neuen Kräfte: es bleibt Vanilla, und die Kräfte sind der Grund für die
Aufwertung (Modulgrenze: Vanilla-Listen `BeaconBlockEntity.BEACON_EFFECTS` sind `static final`, ein Eingriff würde
auch fremde Mods treffen, die sie lesen).

### Reichweite nach Basis-Material (Starkes und Vanilla-Leuchtfeuer)
Faktor je Block: Netheritblock 2,0 · Diamantblock 1,5 · Goldblock 1,1 · Smaragdblock 1,1 · Eisenblock 1,0 ·
alle anderen Basisblöcke (Modpack-Tag) 1,0.

Formel (nur Blöcke abgeschlossener Ebenen, also genau der `levels`, die der Scan zählt; anteilig nach Blockzahl):

    Faktor = Σ_{Ebene e = 1..L} Σ_{Block b in Ebene e} f(b)  /  Σ_{e = 1..L} (2e + 1)²

Gerechnet in ganzen Prozent (Mittelwert, kaufmännisch gerundet), damit Anzeige und Wirkung gleich sind.
Reichweite = Grundreichweite × Faktor (Vanilla `10 + 10 × L`, Stark `Grundwert + je Ebene × L`).

Beispiel (Besitzer: „unterste Ebene ganz Diamant ≈ ×1,25“), Vanilla 4 Ebenen = 164 Blöcke:
Ebenen 1–3 Eisen (9 + 25 + 49 = 83 × 1,0), Ebene 4 Diamant (81 × 1,5):
`(83 × 100 + 81 × 150) / 164 = 124,7 % → ×1,25`, Reichweite `50 × 1,25 = 62,5` Blöcke.
Starkes Leuchtfeuer 6 Ebenen (454 Blöcke), unterste Ebene (169) Diamant: `(285 × 100 + 169 × 150)/454 = 118,6 % → ×1,19`,
Reichweite `70 × 1,19 = 83,3`. Ganz Netherit: ×2,00 (140 Blöcke).

Umsetzung:
- `beacon/RangeFactor.java` (statisch, seitenfrei): Prozent je Block, Prozent einer Pyramide, Anzeigeformat.
- Starkes Leuchtfeuer: Block-Entity berechnet den Prozentwert beim Pyramiden-Scan (alle 80 Ticks), `applyEffects`
  nutzt ihn. Schalter `strongBeaconMaterialRange` (Standard an).
- Vanilla-Leuchtfeuer: Mixin `BeaconRangeMixin` auf `BeaconBlockEntity.applyEffects` (HEAD merkt den Prozentwert,
  `@ModifyArg` auf `AABB.inflate(D)` multipliziert). Schalter `vanillaBeaconMaterialRange`, **Standard an**: der
  Besitzer will es ausdrücklich auch für Vanilla; es verlängert nur die Reichweite, Eisen-Pyramiden bleiben exakt
  Vanilla (×1,00); wer reines Vanilla will, schaltet es aus. Bei abgeschaltetem Modul (`enabled`/Sub-Mod-Schalter) ×1.
- Anzeige: Mixin `BeaconMenuFactorMixin` hängt an jedes `BeaconMenu` (auch das starke, es erbt) einen zusätzlichen
  Daten-Slot mit dem Prozentwert des Servers (0 = Funktion aus → keine Anzeige); Zwischenspeicher 20 Ticks.
  - Starker Bildschirm: rechtes Feld neue Zeile „Material ×1,25“ (y 92) zwischen Pyramide und Reichweite; die
    Reichweite zeigt den Wert mit Faktor.
  - Vanilla-Bildschirm (Client-Mixin `client.BeaconScreenFactorMixin`, am Ende von `extractBackground`): im rechten
    Feld unter den Sekundär-Knöpfen „Material ×1,25“ und „Reichweite 62 Blöcke“, nur wenn der Server die Funktion an hat.
  - Lesbarkeit: Text #E0E0E0 auf dem Feld #212121 (Vanilla-Feldfarbe, gemessen aus beacon.png) = Kontrast 12,2:1.
    Auf dem Vanilla-Bildschirm liegt hinter dem Text ein deckendes #212121-Feld, damit es auch mit Simple-Interfaces-
    Stil lesbar bleibt. Dezimaltrenner aus der Sprache (EN Punkt, DE Komma).

### Bildschirm Starkes Leuchtfeuer
Die Vanilla-Knöpfe der Zeilen 1–3 werden auf ein 3er-Raster verschoben (gleiche Mitte x 76, Abstand 24), die neuen
Kräfte als eigene Knöpfe dazu (wie Zeilen 5/6). Tooltips je Kraft eine Zeile (EN/DE, ≤ 40 Zeichen).

## Dateien
`StrongBeaconLogic`, `StrongBeaconBlockEntity`, `StrongBeaconMenu` (Maske 10 Bits), `StrongBeaconScreen`,
neu `RangeFactor`, `RangeFactorView`, Mixins `BeaconRangeMixin`, `BeaconMenuFactorMixin`, `BeaconInvoker` (Test),
`client/BeaconScreenFactorMixin`; `BeaconsConfig` (+2 Schlüssel, +4 Kräfte, Aquisator-Werte), `StrongConduitLogic`,
Lang EN/DE, `wiki/manual.json`, `tools/check_data.py`, Mod-Beschreibungen, `docs/MODS-UND-FEATURES.md`,
generierte Guides/Wiki/Discover.

## Risiken
- `applyEffects`-Signatur auf NeoForge/Forge gepatcht → Mixin `require=1` fällt beim Start auf; die Modul-Testziele
  aller drei Loader starten das Spiel und decken das ab.
- Zusätzlicher Daten-Slot im `BeaconMenu`: Vanilla-Clients können ohnehin nicht beitreten (Modul registriert Blöcke).
- Held des Dorfes macht Dorfbewohner-Geschenke dauerhaft möglich → per Schalter abschaltbar, im Wiki genannt.

## Verifikation
Gametests (Modul-Katalog + Fabric-Adapter): `range_factor_by_material` (Prozent je Block, Beispielrechnung 125/119/200,
unvollständige Ebene zählt nicht), `vanilla_beacon_material_range` (Vanilla-`applyEffects` über Invoker: Spieler in
22 Blöcken nur mit Faktor erreicht, Schalter aus → Vanilla), `range_factor_in_menu` (Daten-Slot trägt den Wert),
`effects_per_level` (3 Kräfte je Primär-Stufe, Stufen, kein II), `effects_reach_players` (Extraherzen nicht
zurückgesetzt), `conduit_frames` (144/216), `vanilla_conduit_only_conduit_power`. Läufe über `/root/sbt`: Compile aller
Loader, `module-simplebeacons-{fabric,neoforge,forge,standalone-fabric}-263`, Generatoren, `./gradlew check -q`.
HTML-Vorschau beider Bildschirme: `/root/sb-tools-a/previews/beacons22-preview.html`. Keine Client-Läufe (Regel).

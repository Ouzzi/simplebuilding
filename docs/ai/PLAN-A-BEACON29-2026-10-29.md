# Plan A: Simple Beacons komplett (WP-08: BS28b-4, BS28b-5, REV2-A-1)

Branch `a-beacon29` (Basis `claude-wave1`), Modul `modules/simplebeacons` (+ kleiner Haken in `simplelib`/`simpletweaks`).
Besitzer-Wortlaut: `.claude/QUEUE.md` (BS28b-4/-5, REV2-A-1), Bild `/root/sb-tools-a/owner-img/28-beacon-stufen-icons.png`.
Nur MC 26.3 Fabric; Neo/Forge-Quellen nur mechanisch mitgezogen (nicht kompiliert).

## Ist-Zustand
- Ein Block `strong_beacon` (6 Ebenen; Stufen 1-3 je 3 Kräfte, Ebene 4 = Sekundär Regeneration, Ebenen 5/6 je 3 Kräfte), ein
  Block-Entity-Typ, ein Menü-Typ, ein Bildschirm (`StrongBeaconScreen`, 50 px höher, Vanilla-Hintergrund aus dem Client-Jar,
  Marker-Icons stammen aus Vanilla und sind ab Ebene 3 identisch).
- Vanilla-Paket `ServerboundSetBeaconPacket` überträgt nur Effekt-Holder (kein Verstärkerwert).
- Neue-Spieler-/PvP-Schutz liegen in `simpletweaks` (`NewPlayerShield`, `Pvp`) - das Beacon-Modul darf sie nicht direkt kennen.

## Entwurf
**`BeaconKind`** (Enum, ein Block-Entity, vier Blöcke): `STRONG` (Primär 1-4, Sekundär 5), `MIGHTY` (1-5 / 6), `EVIL` (1-4 / 5),
`MIGHTY_EVIL` (1-5 / 6). Die Kräftetabellen (je Ebene bis 3 Kräfte, `Power(effect, amplifier)`) stehen dort; `StrongBeaconLogic`
nimmt überall die Art als Parameter.
- **Strong** (abgeschwächt, Besitzer-Liste): E1 Tempo, Eile, Sättigung; E2 Sprungkraft, Resistenz, Langsamer Fall; E3 Stärke,
  Unsichtbarkeit, Gnade des Delfins; E4 Glück I; Sekundär (E5) Regeneration oder Primär als II (nur die fünf Vanilla-Kräfte).
  Wasseratmung bleibt Aquisator-exklusiv. Weg: Extraherzen (E2), Held des Dorfes, Nachtsicht, Feuerresistenz, Absorption.
- **Mighty**: E1-E3 wie Strong; E4 Glück, Nachtsicht, Platzhalter Resistenz II; E5 Extraherzen I (bestätigt), Platzhalter Tempo II,
  Sprungkraft II; Sekundär (E6) Regeneration oder die Primärkraft eine Stufe höher (Platzhalter II wird III, Extraherzen wird II =
  „verdoppelt“); Obergrenze Verstärker III. Feuerresistenz/Absorption gestrichen.
  Abweichung: Extraherzen sind Vanilla-„Gesundheitsschub I“ = +2 Herzen (nicht +1; ein +1-Herz-Effekt bräuchte einen eigenen
  MobEffect) – mit Sekundär +4. Wenn der Besitzer +1 Herz will, ist das ein eigener Effekt (offen, Rückfrage).
- **Platzhalter-Kräfte** haben denselben Effekt wie eine andere Zeile; das Vanilla-Paket trägt aber nur den Effekt. Lösung ohne
  neues Netzwerk-Paket: der Bildschirm schickt vor dem Bestätigen `clickMenuButton(10 + Verstärker)` (Vanillas Menüknopf-Paket),
  das Menü merkt sich den Verstärker, `updateEffects` prüft ihn gegen die Tabelle und schreibt ihn ins Block-Entity
  (`primary_amp`, fehlt = 0).
- **Rezepte**: Mighty = formgebunden `cig / dBn / UeU` (B = Strong Beacon, c/i/g/d/n/e = Kupfer-/Eisen-/Gold-/Diamant-/Netherit-/
  Enderitkern, U = Starke Aufwertung); ohne SimpleBuilding (keine Kerne) die Blockfassung (Kupfer, Eisen, Gold, Diamant,
  Netherit, Smaragd). Mighty Evil identisch mit Evil Beacon in der Mitte (Besitzer: „wie Mighty“, also mit Starken Aufwertungen).
  Evil Upgrade: formlos Starke Aufwertung + Weinender Obsidian + Netherherz + Rotes Nether-Juwel (`simplemobs`) -> 1 Böse
  Aufwertung (die Starke Aufwertung wird verbraucht; „Kopierrezept“ gäbe 2, das wäre billiger als die Starke Aufwertung selbst).
  Evil Beacon: Schmieden Leuchtfeuer + Böse Aufwertung + Netherstern; in der Welt wie die Starke Aufwertung (Nebenhand, 8 Schläge).
- **Evil-Kräfte** (leicht negativ, keine Schadens-über-Zeit-Effekte, kein Blind/Levitation, alles Stufe I, nie II):
  E1 Langsamkeit, Abbaumüdigkeit, Hunger; E2 Schwäche, Pech, Triefend (Schleime beim Tod); E3 Dunkelheit, Leuchten, Befall;
  E4 Gewebe (Spinnweben beim Tod); Sekundär Übelkeit. Mighty Evil: E1-E4 gleich, E5 Windgeladen + Platzhalter Hunger II,
  Abbaumüdigkeit II; Sekundär (E6) Übelkeit. Begründung: sie behindern (Tempo, Abbau, Sicht, Kampf), töten aber nie und lassen sich
  durch Verlassen der Reichweite beenden; Wirkung nur bei „Spielern“, Dauer wie bei jedem Leuchtfeuer (9 + 2/Ebene s).
- **Ausgewogenheit/Schutz** (`EvilGate`): wirkt erst, wenn die Statistik `time_since_death` >= `evilBeaconGraceMinutes` (5 min,
  0-60) ist; nicht auf Kreative/Zuschauer; nicht auf Spieler, die ein registrierter Schutz abschirmt: neue Klasse
  `simplelib.api.HarmShields` (Registrierung je Modul-Id), `simpletweaks` meldet dort Neulingsschutz und „PvP aus“ an (`Play.start`).
  Frisch beigetretene Spieler zählen ab Welt-Beitritt (der Neulingsschutz deckt sie ohnehin ab).
- **Bildschirm**: Strong/Evil 4 Zeilen (+25 px), Mighty-Arten 5 Zeilen (+50 px); die Art kommt mit dem ersten Menü-Daten-Sync (Daten
  7/8), der Bildschirm baut sich dann neu. Alle linken Vanilla-Knöpfe sind versteckt (eigene Knöpfe für jede Kraft); rechts bleibt
  bei Strong/Mighty Vanillas Regeneration + II (aktiv erst ab Sekundärebene, II nur wenn steigerbar), bei Evil eigener Knopf.
  Hintergrund: `strong_beacon_gui.py` malt Vanillas Marker mit der Feldfarbe zu; der Bildschirm zeichnet pro Ebene ein eigenes Icon
  (`textures/gui/container/pyramid_icons.png`, 6 Ebenen x grün/rot, eigene Pixel in Vanillas Paletten-Farben).
  Variante A (Standard): Pyramide mit 6 Stufen, die obersten N leuchten; Variante B: Pyramide mit N Stufen. Vorschau
  `/root/sb-tools-a/previews/a-beacon29-preview.html`; nicht ausgeliefert ohne Wahl (A ist eingebaut).
- **Texturen**: Mighty/Evil/Mighty Evil Blöcke und Evil Upgrade = Farbton-Verschiebung der Strong-Texturen (Platzhalter, Skript
  `tools/textures/beacons_a29_2026_10_29.py`).
- **Konfiguration**: Schalter `strongBeaconEffects.<id>` jetzt für die 19 Zusatzkräfte (7 Strong/Mighty, 12 Evil), neuer Schlüssel
  `evilBeaconGraceMinutes`.
- **REV2-A-1**: `StrongBeaconLogic.RANGE_CAP = 340` (50 + 20 x 6 = 170, x2 Netherit = 340 ist bereits das Maximum der Datei-
  Grenzen; der Deckel gilt hart). Spieler werden aus `level.players()` gefiltert statt `getEntitiesOfClass` (spart bei 680 x 680
  Blöcken das Absuchen hunderter Chunks alle 80 Ticks).

## Datenkompatibilität (bestehende Welten)
Gleicher Block-/BE-Typ-Id `simplebeacons:strong_beacon`, gleiche Schlüssel `primary_effect`/`secondary_effect`/`Levels`; neu nur
`primary_amp` (Standard 0). Beim Laden: gespeicherte Kräfte, die es auf der Art nicht mehr gibt (Extraherzen E2, Held, Nachtsicht,
Wasseratmung, Feuerresistenz, Absorption), werden gelöscht (Spieler wählt neu) statt Fehler; `Levels` > 5 wird auf 5 begrenzt
(der nächste Scan stimmt ohnehin). Test `block_entity_keeps_choice`. Alte Config-Schalter für gestrichene Kräfte entfallen
(die Datei wird vollständig neu geschrieben).

## Dateien
`beacon/BeaconKind.java`, `EvilGate.java` (neu), `StrongBeaconLogic/BlockEntity/Block/Menu`, `BeaconsRegistry`, `BeaconsConfig`,
`StrongUpgrades`, `client/StrongBeaconScreen`, Loader-Einträge (Fabric; Neo/Forge mechanisch), Rezepte/Advancements/Loot/Blockstates/
Modelle/Items/Texturen/Lang EN+DE, `questionary`, `wiki/manual.json`, `tools/check_data.py`, `simplemoney` (Preis-Ausschlüsse),
`simplelib/api/HarmShields.java`, `simpletweaks/play/Play.java`, `docs/MODS-UND-FEATURES.md`.

## Verifikation
Ein `sbt`-Lauf: Compile `:mc26_3:fabric` + `:modules:simplebeacons:fabric` (+ simplelib/simpletweaks), Targets
`module-simplebeacons-fabric-263`, `module-simplebeacons-standalone-fabric-263`, `check -q`; Generatoren (Guides, Wiki, Discover)
auf sb-test. Gametests: Stufen/Kräfte je Art, Rezepte (Mighty, Mighty Evil, Evil), Evil-Verzögerung + Schutz, Daten-Kompatibilität,
Reichweiten-Deckel, Evil-Upgrade in der Welt. Nicht getestet: Client-Sicht (Bildschirm-Layout, Icons) – Smoke-Test
`ModuleClientSmoke` angepasst, aber kein Client-Lauf.

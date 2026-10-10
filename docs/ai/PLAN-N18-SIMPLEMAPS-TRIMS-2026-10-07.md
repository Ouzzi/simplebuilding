# Nachtrag 18 – Plan (2026-10-07, Besitzer)

Status: **nur geplant**, nichts umgesetzt. Grundlagen: KONZEPT-SUPERMOD-SUBMOD-2026-10-06.md, PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md, UI-Stil refs-n12 Bild 3/4 (Rahmenmaße PLAN-CRUCIBLE-N12B).

## Grundsatz Konsistenz (verbindlich, alle Simple-Mods)
- Was mehrere Mods betrifft, gehört in **simplelib**: UI-Bausteine (Rahmen, Slots, Lesezeichen-Tabs, **Kontextmenü**, Knöpfe, Tooltips), In-World-Umwandlung, Stapelgrößen-Logik, Config-Gerüst („Enable Simple XY“, Kreativtab-Schalter), Kreativtab-Einsortierung.
- Neue UI-Konzepte werden als wiederverwendbare Bausteine mit Doku festgehalten (docs/ai/UI-BAUSTEINE.md, anzulegen): Rahmen, Slot, Lesezeichen-Tab, Kontextmenü, Bestätigung durch zweiten Klick (rot), Ausgrauen.

## A. Simple Trims (Sub-Mod von SimpleBuilding)
- Enthält alle Schmiedevorlagen (Templates) inkl. Platzierbarkeit wie heute.
- Werkzeug für die Vorlagen-Interaktion: mit SimpleBuilding der Vorschlaghammer, ohne SimpleBuilding die Axt (Prinzip „Vanilla-Fallback Axt“).
- Standalone-Target, SB requires Simple Trims (Super-/Sub-Konzept).
- Risiko: Umzug vieler Registrierungen → IDs bleiben `simplebuilding:*` oder Migration nötig (Frage F1).

## B. Simple Maps (Sub-Mod von SimpleBuilding) – Wegfinder-Karte
**Item „Wayfinder Map“**
- Unendliche Karte, folgt dem Spieler: Spieler immer in der Mitte, Terrain verschiebt sich.
- Aufdeck-Radius wie Vanilla-Karte, per Config einstellbar (harte Grenzen).
- Erweitern: normale (gefüllte) Karte + Wegfinder-Karte → Bereich der normalen Karte wird übernommen (freigeschaltet).
- Kopieren (wie Vanilla-Karten mit leerer Karte) und Kombinieren (zwei Wegfinder-Karten → Vereinigung der Bereiche), Kombinieren kostet etwas (Trade-off).
- Preis: relativ hoch, erreichbar im (frühen) Midgame; im Early Game nur mit gezielter Suche (Vorschlag in F3).
- In Haupt-/Nebenhand: funktioniert wie Karte, zusätzlich erscheinen Wegpunkte in der Locator-Bar neben den Spieler-Indikatoren.

**GUI (Rechtsklick)** – Größe etwa Doppeltruhe + Inventar, UI-Stil Bild 3/4
- Kartenfläche breiter als hoch (≈ 4:3), so bemessen, dass das Raster immer ganz passt.
- Schrittweise zoombar (Mausrad bzw. +/−), verschiebbar per Ziehen.
- **Lesezeichen links:** Snap an/aus (beim Verschieben am Raster einrasten), Raster an/aus, Zoom-Stufe.
- **Lesezeichen rechts:** „Spieler“ (oben) + Wegpunkt 1–8. Klick zentriert. Bei Snap an zentriert „Spieler“ das Rasterfeld/Chunk des Spielers, nicht den Spieler selbst.
- **Kontextmenü (Rechtsklick auf Karte):** „Wegpunkt erstellen“ → alles ausgegraut außer den 8 Wegpunkt-Lesezeichen; freies anklicken = sofort gespeichert; belegtes: 1. Klick färbt rot, 2. Klick ersetzt.
- **Kontextmenü (Rechtsklick auf Wegpunkt-Lesezeichen):** „Wegpunkt konfigurieren“ (Name; Farbe für die Locator-Bar ODER Mob-Kopf als Icon), „Wegpunkt löschen“.
- Daten: Freigeschaltete Bereiche und Wegpunkte gehören zur Karte (Item-Component bzw. Kartendaten), nicht zum Spieler (Frage F2).

**Weitere Feature-Vorschläge (zur Auswahl)**
1. Wegpunkt teilen: Karte kopieren überträgt Wegpunkte (optional ohne).
2. Todespunkt automatisch als temporärer Wegpunkt (verfällt nach Abholung).
3. Biom-/Struktur-Markierungen, die beim Erkunden automatisch erscheinen (Dörfer, Portale) – abschaltbar.
4. Kartenrahmen-Anzeige im Item-Frame: zeigt den Bereich um den Rahmen.
5. Höhenschattierung / Höhenlinien-Modus als drittes Lesezeichen.
6. Dimensionen getrennt: je Dimension eigene Bereiche, Umschalt-Lesezeichen.
7. Kompass-Fusion: Wegfinder-Karte + Bergungskompass zeigt den letzten Todespunkt.
8. Server-Config: Kombinieren/Kopieren abschaltbar, maximaler Bereich (harte Grenzen).

**Fragebogen Simple Maps**
- F2: Wegpunkte pro Karte (wandern mit der Karte, teilbar) oder pro Spieler? (Empfehlung: pro Karte)
- F3: Rezept-Vorschlag: 8 Karten + Kompass um Bergungskompass-Fragment? Oder Karte + Kompass + 4 Gold + 1 Echo-Splitter? Early-Game-Weg: selten in Kartografen-Truhen/Schiffswracks?
- F4: Kosten fürs Kombinieren: z. B. 1 Echo-Splitter / Erfahrungsstufen am Kartentisch? Oder Material-Kosten?
- F5: Wo wird erweitert/kombiniert/kopiert: Kartentisch (vanilla-nah) oder Werkbank?
- F6: Unendlich = wirklich unbegrenzt oder mit Config-Maximum (Speicher pro Karte)?
- F7: Wegpunkte auch ohne Karte in der Hand in der Locator-Bar (z. B. im Inventar)? (Besitzer sagte: Haupt-/Nebenhand)
- F8: Mehrere Dimensionen auf einer Karte (Vorschlag 6) oder eine Karte je Dimension?
- F9: Zoom-Stufen: wie Vanilla 1:1 bis 1:16, oder feiner?
- F10: Mob-Kopf-Icons: alle Vanilla-Köpfe frei wählbar oder nur solche, die man besitzt?

## C. Kleinere Punkte
- C1 Config (Super- und Sub-Mods): Schalter „Items in Kreativ-Tabs hinzufügen“ (an/aus) je Mod – in simplelib als Teil des Config-Gerüsts.
- C2 Guides: farbiger Strich an freigeschalteten Lesezeichen-Tabs entfernen.
- C3 Dev-Kreativtabs immer ganz ans Ende der Tab-Reihenfolge (Production und Dev bleiben nah beieinander).
- C4 Sandwiches appetitlicher (Texturen: sattere Zutatenfarben, mehr Kontrast/Glanz, dickere Füllung – Vorschau mit Varianten).
- C5 Maximale Stapelgröße aller 0,125er-Blöcke auf 128 (über simplelib-Stapelgrößen-Logik, gemeinsam mit Tiegel-Stapelgröße aus Nachtrag 15).

## Fragen an den Besitzer (übergreifend)
- F1: Simple Trims: sollen Item-IDs `simplebuilding:*` bleiben (keine Welt-Migration) oder auf `simpletrims:*` wechseln (mit Migration alter Welten)?

## Antworten Besitzer (2026-10-07)
- F1: Vorlagen-IDs in simpletrims (`simpletrims:*`, mit Migration alter Welten).
- F2: Wegpunkte gehören immer der Karte, die gerade genutzt wird.
- F3: offen – Besitzer möchte Rezept-Vorschläge.
- F4: Normale Karte + Wegfinder-Karte → nur die normale Karte wird verbraucht. Zwei Wegfinder-Karten → die zweite wird verbraucht, Wegpunkte der ersten bleiben immer erhalten.
- F5: Kartentisch.
- F6: Unbegrenzt, aber optimiert gespeichert (nur erkundete Bereiche, z. B. regionsweise/komprimiert); aufgedeckt wird, wo man hinreist; Navigation per Ziehen und Zoom.
- F7: Wegpunkte in der Locator-Bar nur mit Karte in Haupt- oder Nebenhand.
- F8: Je Dimension eine eigene Karte; Nether-Karte deutlich teurer, End-Karte noch teurer. Kompatibilität für weitere Dimensionen vorsehen (Erweiterung in dieser Mod oder per Config/Datapack anderer Mods).
- F9: Zoom: größte Stufe 1 Block = 1 Kartenpixel, dann 4, 8, 16, 64 … Blöcke je Pixel (zunächst zum Testen, Faktor ×2/×4).
- F10: Jeder Mob-Kopf wählbar.
- Feature 5 (Höhenlinien-Modus): ja.
- Feature 7 neu gefasst: Bergungskompass + Wegfinder-Karte gleichzeitig in den Händen → Todespunkt auf der Karte sichtbar, auch in unerkundetem Gebiet (Terrain bleibt dort verborgen, nur der Punkt erscheint).
- F3 entschieden: Rezept A (Karte + Kompass + 4 Goldbarren + 1 Echo-Splitter) plus seltener Fundort (Kartografen-Truhen, Schiffswracks ~5 %). Nether- und End-Karte zusätzlich mit seltenem Fundort (Vorschlag: Nether-Karte in Bastionen/Netherfestungen ~2 %, End-Karte in Endstadt-Truhen ~2 %).

## Antworten Besitzer (2026-10-09, Simple Maps)
- Feature 1 ja: Kopieren übernimmt die Wegpunkte.
- Feature 2 entfällt: Todespunkt nur über Feature 7 (Bergungskompass in der anderen Hand).
- Feature 4: Karte im Gegenstandsrahmen → Rechtsklick öffnet die Karten-UI, dort scrollbar; der Rahmen zeigt danach den Ausschnitt, zu dem man gescrollt hat.
- Feature 6 = F8 (eigene Karte je Dimension). Feature 7 ja. Feature 8 ja.
- Feature 3 (Struktur-Markierungen): keine Antwort → nicht umgesetzt.

## Umsetzung Simple Maps (Branch claude-q-maps, 2026-10-09)
Eigenes Modul `modules/simplemaps` (Mod-ID `simplemaps`, Paket `com.simplemaps`, Loader-Adapter
`com.simplebuilding.modules.simplemaps`). Allein spielbar (nur Vanilla + SimpleLib, gebündelt nach Regel 6a
für die UI-Bausteine); kennt SimpleBuilding nicht. Standalone-Target wie alle Module.

**Items.** `simplemaps:wayfinder_map` (Oberwelt und jede fremde Dimension), `nether_wayfinder_map`,
`end_wayfinder_map`. Welche Dimension eine Karte annimmt, steht in den Dimensionstyp-Tags
`simplemaps:nether_wayfinder` / `simplemaps:end_wayfinder` (Datapack-erweiterbar, F8); die Oberwelt-Karte
nimmt alles, was in keinem der beiden Tags steht. Eine Karte bindet sich beim ersten Benutzen an die
Dimension, in der sie ist; in anderen Dimensionen deckt sie nichts auf.
Rezept A (formlos): Karte + Kompass + 4 Gold + 1 Echo-Splitter. Nether: dazu 2 Echo + 1 Netherit-Platte
(statt 1 Echo); End: 2 Echo + 1 Shulker-Schale (Entscheidung Agent, Balancing offen). Fundorte: Kartografen-
Truhe und Schiffswrack-Kartentruhe 5 %, Bastion (übrige) und Netherfestung 2 % (Nether-Karte),
Endstadt-Schatz 2 % (End-Karte).

**Daten (Server).** Eine `SavedData` je Karten-ID (`simplemaps:wayfinder_<id>`, IDs zählt
`simplemaps:wayfinder_ids`). Gespeichert wird nur Erkundetes: Kacheln zu 128×128 Blöcken (1 Block = 1 Pixel),
je Kachel Farbbytes (Vanilla-`MapColor`-Packed-ID) und Höhenbytes ((y − minY)/2 + 1, 0 = unbekannt). Die
Datei ist wie alle `.dat` gzip-komprimiert (F6: unbegrenzt, aber optimiert). Obergrenze je Karte
`maxTilesPerMap` (Server-Config, harte Grenzen, Feature 8).
Aufdecken (wie Vanilla nur in Haupt-/Nebenhand): Scheibe mit `revealRadius` Blöcken (Config 16–128,
Standard 96), je Tick 1/16 der Spalten (Vanilla-Takt), nur geladene Chunks (lädt nie Chunks).
Farbe/Helligkeit wie Vanilla-`MapItem.update` bei Maßstab 1:1. Dimensionen mit Decke (Nether): Abtastung
unterhalb des Spielers statt Vanilla-Rauschen.
Wegpunkte gehören dem Stapel (F2): Komponente `simplemaps:waypoints` (Platz 1–8, x/z, Name ≤ 32, Farbe
oder Mob-Kopf). Karten-ID: `simplemaps:map_id`. Rahmen-Ausschnitt: `simplemaps:view` (Mitte x/z, Zoom).

**Kartentisch (F4/F5, Feature 1/8).** Mixin ersetzt die beiden Eingabe-Slots (nehmen zusätzlich
Wegfinder-/gefüllte Karten an) und übernimmt `setupResultSlot`, wenn eine Wegfinder-Karte beteiligt ist:
- Wegfinder + leere Karte → 2 Kopien (gleiche ID, Wegpunkte übernommen).
- Wegfinder + gefüllte Karte (gleiche Dimension) → Wegfinder; nur die gefüllte Karte wird verbraucht, ihr
  Bereich wird beim Entnehmen übernommen (Komponente `simplemaps:pending`, ausgewertet in
  `onCraftedPostProcess` wie Vanillas Maßstab/Sperre, Rückfall im Inventar-Tick).
- Wegfinder + Wegfinder (gleiche Dimension) → die erste bleibt, die zweite wird verbraucht; Bereiche
  vereinigt, Wegpunkte der ersten bleiben, freie Plätze bekommen die der zweiten.
Jede Funktion per Server-Config abschaltbar (`allowCopy`, `allowExtend`, `allowCombine`).

**Netzwerk.** C2S `tiles` (Karten-ID, Zoom, bis 48 Kacheln mit bekannter Version; höchstens alle 4 Ticks
je Spieler) → S2C `tile` (Deflate-komprimierte Farben+Höhen, Version) nur für geänderte Kacheln, dazu
S2C `map_state` (gebundene Dimension). Gröbere Zoomstufen rechnet der Server aus den 1:1-Kacheln
(Stichprobe je Pixel). C2S `waypoint` (Hand, setzen/konfigurieren/löschen; Server prüft Hand, Platz,
Namenslänge, Kopf-Liste) und C2S `frame_view` (Rahmen in Reichweite, Wegfinder darin).

**Client-UI.** Bildschirm ohne Menü (Rechtsklick mit der Karte), SimpleLib-Kasten im N12-Stil.
Kartenfläche 192×144 (4:3, 12×9 Rasterzellen zu 16 px). Zoom 1, 4, 8, 16, 64 Blöcke je Pixel (F9) per
Mausrad oder +/−; Ziehen verschiebt. Lesezeichen links: Snap, Raster, Zoomstufe, Höhenlinien (Feature 5).
Lesezeichen rechts: Spieler + Wegpunkt 1–8; Klick zentriert (Snap: Rasterzelle des Spielers).
Kontextmenü (neuer SimpleLib-Baustein `UiContextMenu`, Doku `docs/ai/UI-BAUSTEINE.md`): Karte →
„Wegpunkt erstellen“ (Auswahl-Modus, alles ausgegraut außer den 8 Lesezeichen; belegt = erst rot, zweiter
Klick ersetzt). Lesezeichen → „Wegpunkt konfigurieren“ (Name, 16 Farben oder Mob-Kopf), „Wegpunkt löschen“.
Bergungskompass in der anderen Hand → letzter Todespunkt auf der Karte, auch im Unerkundeten (Feature 7).
Locator-Bar: Wegpunkte der Karte in Haupt-/Nebenhand (F7), Farbe bzw. Kopf (Mixins `Hud`, `LocatorBar`).
In der Hand: Vanilla-Kartenansicht mit dem 128×128-Ausschnitt um den Spieler (Spieler mittig).
Gegenstandsrahmen (Feature 4): Rahmen zeigt den gespeicherten Ausschnitt; Rechtsklick öffnet die UI,
Verschieben/Zoomen schreibt den Ausschnitt zurück (Wegpunkte dort nur lesbar).

**Tests.** Modul-Targets `module-simplemaps-{fabric,neoforge}-263` und Standalone; GameTests für
Registrierung/Rezepte/Tags, Aufdecken + Speichern, Kachel-Codec, Kartentisch (Kopie/Erweitern/Kombinieren +
Verbrauch + Config-Schalter), Wegpunkt-Validierung, Dimensionsbindung, Config-Grenzen, Loot. Client-Smoke mit
Screenshots der UI. Forge 26.3: kompilieren im selben Zug (Testziel wie bei Sandwiches/Containers noch ohne).

## Simple Trims: Umsetzungsabschnitt (2026-10-10, claude-q-trims)

Besitzer: F1 mit „ja, starten“ beantwortet; IDs wechseln auf `simpletrims:*` mit Migration (siehe Antworten oben).

**Ist-Zustand (SB 26.3).** Drei Vorlagen (`glowing_`/`emitting_`/`pulsating_trim_template`, `ModItems`), ihre Wirkung (`TrimUpgrades`, `GlowingTrimUtils`, Komponenten `glow_level`/`pulsating`/Emission in `ModDataComponentTypes`, `DynamicLightHandler`, `TrimPulseTextures`, Mixin am Schmiedetisch), Platzierbarkeit (`PlacedTemplates`, `PlacedPlate`, Block `PLACED_SMITHING_TEMPLATE`), Hammer-Rezept (`SledgehammerCrafting`, `SledgehammerEntityInteraction`), Besatz-Effekte (`TrimEffectUtil`, `TrimAttributeHandler`, `TrimBonusCatalog`, `TrimMultiplierLogic`, `TrimStatsLayout`/Panel/Referenzbildschirm, Netzwerk `TrimDataPayload`/`TrimBenefitPayload`), AutoSmither, JEI/REI-Anzeigen, Guide `guide_book_trims`, Tests (`PlacedTemplateTests`, `PulsatingTrimTests`, `DynamicLightTests`, `DataIntegrityTests`, `InWorldExportTests`). Rund 25 SB-Dateien greifen auf `PlacedTemplates` zu, 16 auf `GlowingTrimUtils`.

**Kopplungsregel.** Module kennen sich nur über öffentliche IDs/Tags (wie `simplesandwiches` ↔ SB). SB darf Trims also nicht per Klasse importieren; was wandert, wandert komplett samt Komponenten, Tests und Daten. Daraus folgen die Stufen:

- **Stufe 1 (erledigt, claude-q-trims):** Modul `modules/simpletrims` als Gerüst auf Fabric/NeoForge/Forge (Standalone- und Integrations-Targets, `check_data.py`, `modules.json`, `enabled-mods.json`), Config-Schalter „Enable Simple Trims“ / „Simple Trims aktivieren“, Werkzeugregel `TemplateTools` (ohne SB jede Axt, mit SB Tag `simplebuilding:sledgehammer_tools`), Tests `config_defaults`, `template_tool_*`, `ids`. In SB ändert sich nichts.
- **Stufe 2:** Komponenten + Wirkung (`glow_level`, `pulsating`, Emission, Schmiedetisch-Mixin, `TrimUpgrades`, Dynamic Light, Pulsier-Render) ins Modul; die drei Vorlagen als `simpletrims:*` (Item, Modell, Textur, Rezept, Lang, Tags, Advancement). Migration alter Welten: `LegacyItemIds` um Namespace-Wechsel für Items und Komponenten-IDs erweitern, Test mit altem Stack.
- **Stufe 3:** Platzierbarkeit (`PlacedTemplates`/`PlacedPlate`, Block, BE, Renderer) und Werkzeug-Interaktion ins Modul (Axt ohne SB, SB-Hammer über Tag); SB-Hammerrezept wird zum Tag-Eintrag.
- **Stufe 4:** Besatz-Effekte, Stats-Panel, Referenzbildschirm, Netzwerk, AutoSmither-Anbindung, JEI/REI, Guide; SB bündelt Simple Trims (Fabric `include`, NeoForge `jarJar`, Forge Bundle) mit Schalter „Simple Trims aktivieren“; SB-Tests `trim*`/`data_integrity*` ziehen auf `simpletrims:`-Namespace um.

**Was in SB bleibt:** Hammer, Rüstungs-/Material-Katalog (Enderit, Nihilith, Astralit), Gesamt-Guide-Einstieg, Schalter. **Risiken:** Welt-Migration (Items und Komponenten), 1.21.11-Port nur im Port-Run, Datagen-Provider zerlegen.

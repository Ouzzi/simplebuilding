# Plan a-portal33 – Bogenportale in Simple Dimensions (N33, 12.11.2026)

Auftrag: `/root/b-auftrag-20261109.md` „## N33“, Konzept `/root/sb-tools-a/previews/a-portal33-preview.html`,
Agentenauftrag `/root/sb-tools-a/agenten-1530.md` §9. Branch `a-portal33`, Basis `hub/claude-wave1` 388f19991.
Besitzer-Entscheidungen: F1=B (kleinste Form 1 breit × 2 hoch), F2=A (Gegenseite baut Vanilla-Rechteck),
F3=A (Standard an, Config-Schalter), F4=A (gleiche Formregeln für die eigenen Dimensions-Portale), F5=A (max. 21 × 21);
dazu Formvorgabe `shape` je Dimensions-Portal-Rezept (rectangle | arch | any, Mindest-/Höchstmaße, optionale Schablone)
mit Aktionsleisten-Grund.

## 1. Ist-Stand (erhoben)
- `dev.simpledimension.common.portal`: `PortalFrameScanner` (nur Rechtecke, frameBlock + min/max), `PortalRecipe`/
  `PortalRecipeMatcher` (feste Schablonen aus Zeilen + Legende), `PortalActivationService.match` (Schablonen haben
  Vorrang, sonst Rechteck), `MatchedPortal` (Innenzellen + Rahmen, beliebige Form), `DimensionPortalConfig` (JSON-Schema).
- `PortalActivation.ignite` (Fabric `UseBlockCallback`, Feuerzeug/Feuerkugel) zündet die eigenen Dimensions-Portale;
  `DimensionRuntime.validFrame` lässt sie bei kaputtem Rahmen zerfallen. Obsidian ist für Vanilla reserviert
  (`ConfigLimits.validate`), Vanilla-Netherportale fasst das Modul bisher nicht an.
- Vanilla 26.3 (dekompiliert): `BaseFireBlock.onPlace` → `PortalShape.findEmptyPortalShape` (Rechteck 2..21 × 3..21,
  Rahmen = Tag `minecraft:nether_portal_frame`), `BaseFireBlock.isPortal` erlaubt Feuer ohne Halt nur in einem Rahmen,
  `NetherPortalBlock.updateShape` löscht bei unvollständigem Rechteck, Zielsuche über POI `nether_portal`
  (`PortalForcer.findClosestPortalPosition`, Filter: Block hat `HORIZONTAL_AXIS`), POI-Zuordnung allein über
  `PoiTypes.TYPE_BY_STATE`. Render-Ebene ergibt sich aus der Textur (kein `ItemBlockRenderTypes` mehr).

## 2. Regeln (Formalisierung der Besitzer-Regeln)
Innenfläche = 4-zusammenhängende Füllung ab der Zündzelle in einer Ebene (X oder Z):
1. **dicht:** jede 4-Nachbarzelle einer Innenzelle ist Innenzelle oder Rahmen (sonst Grund „gap“);
2. **begrenzt:** Umriss ≤ max. Breite × Höhe (Netherportal 21 × 21, F5), sonst „open“ (offen oder zu groß);
3. **gerader Boden + Seiten nur hoch/nach innen:** jede Innenzelle oberhalb der Bodenreihe steht auf einer Innenzelle.
   Daraus folgt: Bodenreihe durchgehend, Spalten lückenlos vom Boden, Außengrenzen je Zeile nie breiter. Grund
   „bottom“, wenn eine Rahmen-Beule aus der Bodenreihe ragt, sonst „sides“ (Vorsprung/Überhang);
4. **obere Kante frei** (Rundbogen, Spitzbogen, Treppe, Doppelbogen);
5. **Rahmen 8-verbunden:** der Rahmen ist genau die 4-Nachbarschaft der Innenfläche; bei einer lochfreien
   „Histogramm“-Fläche ist sie automatisch 8-verbunden – Ecken (Diagonalstufen) sind nie nötig, wie bei Vanilla;
6. **Mindestmaß:** Bodenbreite ≥ min. Breite, höchste Spalte ≥ min. Höhe (Netherportal 1 × 2, F1=B).
Bewusste Verschärfung gegenüber dem Wortlaut: ein Portalfeld, das auf einem schrägen „Stalaktiten“ sitzt
(Überhang innerhalb der Außengrenzen), ist ungültig – Regel 3 ist die klare Lesart von „gerader Boden, Seiten nur
nach oben oder innen“. Steht in der Vorschau als Hinweis, keine Frage.

## 3. Umsetzung (Dateien)
Reine Logik (`modules/simpledimensions/shared/java/dev/simpledimension/common/portal/`):
- NEU `ArchPortalScanner` (Modus RECTANGLE/ARCH/ANY, Maße, `scan` je Achse → `Result` mit `Verdict`
  OK/OPEN/GAP/BOTTOM/SIDES/TOO_SMALL/RECTANGLE_ONLY/ARCH_ONLY, `enclosed` = „sieht aus wie ein Versuch“).
- `DimensionPortalConfig`: `portalShape` (Ersatzform ohne Schablonen, Standard `any` = F4=A); je Rezept
  `shape` (`pattern` Standard | rectangle | arch | any), `frame`, `minWidth/maxWidth/minHeight/maxHeight`;
  `buildShapeRules()`; Schablonen-Rezepte unverändert.
- `PortalActivationService.match`: erst Schablonen (wie bisher), dann Form-Regeln (RECTANGLE über den
  bestehenden `PortalFrameScanner`, ARCH/ANY über `ArchPortalScanner`); `explain` liefert den Grund.
- `ConfigLimits.validate`: `portalShape`, Rezept-`shape`, `frame` (gültige ID, nicht Obsidian/Luft), Maße auf
  1..21 × 2..21 begrenzt; Schablonen-Prüfung nur für `pattern`.
MC-Teil (`com.simplebuilding.modules.simpledimensions`):
- NEU `ShapedNetherPortalBlock extends NetherPortalBlock` (`simpledimensions:shaped_nether_portal`): Teleport,
  Partikel, Klang, Abklingzeit, Piglin-Spawn geerbt; `updateShape` prüft die Bogenform (nicht Vanilla-Rechteck),
  `entityInside` nur bei eingeschaltetem Sub-Mod.
- NEU `ShapedNetherPortals`: Zünden (`tryLight` – Vanilla-Rechteck bleibt Vanilla), `canLight` (Feuer ohne Halt),
  `intact` (Abbau), `hint` (Aktionsleisten-Grund), POI-Eintrag als `nether_portal`, Rückzug bei Portal-Ersatz-Mods.
- `DimensionSettings.shapedNetherPortals` (Standard an, F3=A) + Config-Bildschirm + `config-options.json`.
- `PortalActivation.hint`: nach PASS Grund in der Aktionsleiste (Netherbogen und eigene Form-Regeln).
- `DimensionRegistry.shapedPortal()` – NUR Fabric (Regel „nur 26.3 Fabric“); NeoForge/Forge-Port als TODO.
Fabric: `simpledimensions.fabric.mixins.json` mit `BaseFireBlockMixin` (onPlace HEAD, isPortal RETURN) und
`PoiTypesAccessor` (`TYPE_BY_STATE`); Aufruf in `DimensionsFabric`; `module-vanilla-derived.gradle` anwenden.
Textur: `tools/textures/shaped_portal_derived.py` – Vanilla `nether_portal.png` RGB × 0,8 (Alpha bleibt) beim Build,
`MODULE_OUTPUTS['simpledimensions']`, `.gitignore`; eigene Dateien: Blockstate, zwei Modelle mit Vanilla-Eltern
(`minecraft:block/nether_portal_ns|ew`), `.png.mcmeta`. Keine Mojang-Pixel im Repo. A8c-Runden entfallen
(mechanische Ableitung, Besitzer-Vorgabe „20 % dunkler“); Sichtprüfung alt/neu in der Vorschau.
Daten/Doku: Lang EN/DE (Block, 7 Gründe, Option, neue JSON-Felder), `wiki/manual.json` (Feature „arch_portals“,
Notiz Block, Formvorgabe), `tools/export_data.py` + `check_data.py`, `docs/MODS-UND-FEATURES.md`, PRINZIPIEN
Teil 2 Simple Dimensions, Generatoren (precheck --fix).

## 4. A12 Vanilla-Nutzen
(1) Nutzen: Netherportale passen in Bögen/Burgtore, kleinste Form 1 × 2 spart Obsidian (6 statt 10).
(2) Änderung: nur Form und (dunklere) Farbe; Kosten, Reise 1:8, Zielsuche, Abklingzeit, Piglins bleiben Vanilla.
(3) Schwacher Eingriff, abschaltbar (`shapedNetherPortals`), Rechteck bleibt reines Vanilla, Rückzug bei Portal-Mods.
AGENTS 3.4 (kein Text für Gadgets): Ausnahme auf ausdrücklichen Besitzer-Wunsch (Grund in der Aktionsleiste).

## 5. Risiken
- POI-Map ist privat → Accessor-Mixin (Fabric). Fehlt der Eintrag, findet der Rückweg den Bogen nicht (Test 4).
- `updateShape` darf nie `NetherPortalBlock.updateShape` aufrufen (Vanilla-Rechteckprüfung löscht den Bogen).
- Kaskade beim Abbau: je Zelle eine Füllung ≤ 441 Felder (wie Vanilla `findAnyShape` je Zelle).
- Zündung über `setBlock(..., 18)` wie Vanilla (keine Teil-Validierung während des Aufbaus).
- NeoForge/Forge registrieren den Block noch nicht (gemeinsamer Code kompiliert; keine geteilte Mixin-Änderung).

## 6. Prüfung
- JUnit (`fabric/src/test`): `ArchPortalScannerTest` (alle Beispielformen gültig/ungültig, Gründe, Modi, Maße),
  `DimensionPortalConfigTest` erweitert (Formregeln, Validierung). Lokal: javac + kleiner Reflexions-Runner.
- GameTests (`ShapedPortalTests`, nur Fabric-Katalog): gültige/ungültige Formen, Farbe (eigener Block vs. Vanilla),
  Abbau, Reise hin (Nether) / zurück (POI findet den Bogen), Schalter aus, Formvorgabe je Rezept, Grund-Texte.
- Lokal: javac aller geänderten Klassen gegen 26.3-Jar (Loom merged-deobf) + Fabric-API + Mixin; precheck --fast,
  check_data.py (alle Module), check_dup_impl, check_tooltip_text, wiki --all --check, check_missing_textures,
  layers.py check, Textur-Generator-Test. sb-test zurzeit nicht erreichbar → EIN gezielter Lauf, sobald erreichbar:
  `/root/sbt a-portal33 "python3.12 tools/testrunner/run.py --targets module-simpledimensions-fabric-263"`.

## 7. Ergebnis / Abweichungen
Plan-Abgleich: alle Punkte aus §3 umgesetzt. Abweichungen/Ergänzungen:
- **Neue Konfigfelder nullable** (`portalShape`, Rezept-`shape/frame/min*/max*`): Gson schreibt null nicht, die drei
  mitgelieferten Definitionen bleiben byteweise gleich (keine Unruhe in bestehenden Config-Dateien); null = any/pattern/1/21/2/21.
- **Vorfilter `enclosed`** vor jeder Füllung beim Zünden (notwendige Bedingung jeder gültigen Bogenzelle): Feuer im
  brennenden Wald kostet einen Blocklesezugriff je Achse statt einer Füllung; Vanillas Rechteckprüfung erst danach.
- **Grund-Unterscheidung bottom/sides:** „bottom“ nur für eine Beule zwischen Bodenzellen, sonst „sides“ (auch Trichter).
- **Questionary-Texte** EN/DE für den Block ergänzt (Pflichtdatei-Schema A11.5).
- `buildScanner()` bleibt (Unit-Test, gleiche Klemmung); Modus RECTANGLE nutzt weiter den bestehenden `PortalFrameScanner`.
Prüfung lokal (sb-agent, ohne Gradle): javac aller Quellen von simpledimensions (shared + fabric main inkl. Mixins,
GameTests, Config-Bildschirm) gegen das access-widened 26.3-Jar + Fabric-API 0.161.0 → Exit 0; JUnit über
Reflexions-Runner 36/36 grün (13 neu), Gegenprobe ohne Regel 3 → 2 rot; `tools/textures/tests` 67/67;
`precheck.py` 88 Prüfungen grün (inkl. aller check_data, check_dup_impl, check_tooltip_text, wiki --all --check,
Handbücher/Discover/Quests), `precheck --fast` grün, `check_missing_textures` grün (mit `--jar` nur der bekannte
Basisfehler jukebox_bottom, nicht von hier), `layers.py check` 0 Probleme. Abgeleitete Textur: Mittel RGB = 0,800 × Vanilla,
Alpha identisch.
Nicht getestet: GameTests im Spiel (sb-test „No route to host“) → TODO sbt; NeoForge/Forge (Regel „nur 26.3 Fabric“);
Client-Sicht (Render-Ebene ergibt sich aus der Textur-Transparenz wie bei Vanilla, `render_type` zusätzlich gesetzt).
Vorschau PORTAL33b: `/root/sb-tools-a/previews/a-portal33-preview2.html` (Schema-Bilder aus Spieltexturen, Urteile vom
echten Scanner).

# Plan A-LAYERS10 (08.11.2026) – Simple Layers: Wolkenmeer + Backrooms (Umsetzung)

Queue **IG31-56**. Branch `a-layers10` (Basis `claude-wave1` 8dd095555 + `a-konz09` 5566b3aaf). Nur **26.3 Fabric**.
Freigabe Besitzer 08.11.: „Bitte Konzept für die Wolken und Backrooms implementieren“ → Konzept
`docs/ai/KONZEPT-A-BIOMES09-2026-11-08.md` mit den Empfehlungen: **wolke=B** (eigene Himmels-Dimension, Übergang über y 700),
**backroom=B** (eigene Dimension, Zellen-Generator, Jigsaw-Sonderräume, Eingang „fehlerhaftes Grundgestein“), **ein Mod
„Simple Layers“** (`modules/simplelayers`, Sub-Mod von SimpleBuilding, optional gebündelt, allein spielbar mit SimpleLib).

## Offene Fragen des Konzepts (§5) – entschieden
| Frage | Entscheidung | Grund |
|---|---|---|
| 1 Nebel-Gruben bodenlos? | **Nein** (Standard). Unter jedem Nebel-Pfropfen liegt ein Hohlraum mit Wolken-Boden in der Insel; Config `bottomlessPits` (Standard aus) = „Schwierig“, dann geht die Grube durch die Insel. | Konzept §1.1 „nie ins Bodenlose außer Config Schwierig“; geringster Eingriff (Auftrag). |
| 2 „Der Lauschende“? | **Passive Atmosphäre** ohne Schaden und ohne Wesen: seltene Schritt-Geräusche, die sich hinter dem Spieler nähern und abbrechen; Config `listener` Standard **aus** (Konzept: aus bis zur Abnahme). | Auftrag: geringster Eingriff, nur Geräusche bzw. Erscheinung. Ein Wesen (Modell, KI) ist Stufe 4 des Konzepts. |
| 3 Ein Mod oder zwei? | **Ein Mod** Simple Layers, Schalter `clouds.enabled` / `backrooms.enabled`. | Konzept §3 Empfehlung. |

## Stufen (je Stufe Commit + Push `git push /root/sb.git a-layers10`)
1. **Gerüst:** `tools/newmod.py simplelayers --parent simplebuilding --bundle optional --config-switch true` (modules.json,
   Gradle, fabric.mod.json, Lang, Wiki, Testzentrum, Standalone-Target, SB-Schalter, sync.py, modtree); dazu Config
   `config/simplelayers-server.json` + `/simplelayers config` (ConfigCommands), Lang-Schema `text.autoconfig.simplelayers.*`,
   Kreativtab „Simple Layers“, Handbuch (`tools/guides/module_guides.py`), Testkatalog `LayersTests` + Adapter.
2. **Wolken-Blöcke:** Wolkenblock (Fallschaden ×0,7 per `fallOn`), Wolkenstein (×0,9), Nebelblock (keine Kollision, sieht aus wie
   Wolke, Rand leicht durchscheinend + seltene Dunst-Partikel, Config `fogHint`; Drop nur mit Behutsamkeit). Alle `instabreak()`.
   Texturen eigen (A8b, Ebenen-JSON), Vorschau `/root/sb-tools-b/previews/a-layers10-preview.html`.
3. **Himmels-Dimension `simplelayers:cloud_sea`:** Typ min_y 0 / Höhe 512, Rausch-Generator nach Vanilla `floating_islands`
   (eigene `noise_settings`, Oberflächenregel: Hülle Wolkenblock, Kern Wolkenstein), Biom `simplelayers:cloud_sea` (heller Nebel,
   Wolken unter den Inseln, Wind, kein Regen, keine Spawns), Features Nebel-Grube und Himmelstruhe (Vanilla-Loot).
   Übergang: Spieler (+ Reittier) steigt in der Overworld **aufwärts** über y 700 (Config 400–1000) → Wolkenmeer y 16 an derselben
   x/z (ohne Elytra-Flug: Wolken-Plattform 3×3); fällt unter y 0 → Overworld y 680 mit 3 s Sanftfall. Kein Pendeln (nur
   Aufwärts-Kreuzung, 20 Blöcke Abstand). **Simple Earth:** neuer SimpleLib-Vertrag `com.simplelib.api.HeightHandover`
   (Earth meldet seine Orbit-Höhe je Spieler, kein Import zwischen den Modulen); über y 500 im Wolkenmeer geht es zur Overworld
   über die Earth-Höhe; die Wolken-Grenze fängt nur unterhalb dieser Höhe ab. Ohne Earth: weiche Decke.
4. **Backrooms `simplelayers:backrooms`:** Typ min_y 0 / Höhe 64, kein Himmel, Decke, feste Zeit, gelber Nebel (Ende 48),
   Summ-Schleife + seltenes Klopfen (ElevenLabs, `CREDITS.md`), Staub. Eigener Chunk-Generator `simplelayers:backrooms`:
   Raster 8×8, Raumhöhe 5 (Boden y 1 feuchter Teppich, Decke y 7 Deckenplatte, Grundgestein y 0/8), Türen per Kanten-Hash
   (beide Nachbarn rechnen dasselbe, unendlich, deterministisch), Säulenhallen (4×4 Zellen), dunkle Räume, Tapete 3 Muster.
   Sonderräume als Jigsaw-Strukturen (Template-Pools, NBT 17×7×17 im Raster): Lager (Truhen-Loot inkl. Mandelwasser und
   Hintertür-Schlüssel), Wasserraum, dunkler Raum; Ausgangsraum eigene Struktur (Spacing 40 Chunks). Blöcke: feuchter Teppich
   (`dampens_vibrations`), Tapete ×3, Deckenplatte (+ Wasserfleck), Neonröhre (an/flackernd/kaputt, Flackern nur Textur),
   EXIT-Tafel (Licht 8), Ausgangstür, fehlerhaftes Grundgestein (nur Netherit-Spitzhacke, ≈ 30 s), Riss.
   Eingang: seltenes fehlerhaftes Grundgestein im Overworld-Bedrock; abgebaut wird es zum **Riss**, wer hineinfällt, landet in
   den Backrooms (Option `voidToBackrooms`, Standard aus: Fall unter min_y − 32). Ausgang: Ausgangstür → Overworld-Oberfläche an
   der Eintritts-x/z (SavedData), mit Schlüssel → weit entfernter Ort (1500–3000 Blöcke). Wände in den Backrooms für Spieler
   im Überleben nicht abbaubar (Config `unbreakableWalls`).
5. **Gametests** (Katalog `LayersTests`, Adapter `ModuleGameTest`): instabreak, Nebel ohne Kollision, Fallschaden-Faktoren,
   Dimensionen geladen, Übergang hin/zurück + Earth-Grenze, Raster (Türen symmetrisch, Generator-Chunk), Eingang/Ausgang,
   Netherit-Regel, Standalone (gleicher Katalog ohne SimpleBuilding).
6. **Prüfung:** einmal `/root/sbt a-layers10 "./gradlew check -q"` (bprep-gleich), dann Modultests
   `module-simplelayers-fabric-263` + `module-simplelayers-standalone-fabric-263` (nacheinander, nie parallel).

## A12
Vanilla-Nutzen: Elytra-Ziel nach dem End, Landepolster-Blöcke, Endgame-Abenteuer für Netherit-Spieler, Deko-Blöcke.
Änderung: zwei neue, abschaltbare Orte; ein seltener Block im Bedrock. Eingriff gering (Overworld unverändert, Übergänge aus = Dimensionen unerreichbar).

## Abweichungen vom Konzept (begründet)
- **Riss statt Leere** unter dem fehlerhaften Grundgestein: dauerhaft (übersteht Neustarts), gilt für jede Entität, kein
  Leere-Tod bei Lag. Leere-Weg bleibt als Option.
- **Jigsaw mit Größe 1** (ein Raum je Struktur, Raster-genau 17×17): Kind-Teile würden sich mit dem 1-Block-Wandraster
  überlappen (Jigsaw verbietet Überlappung). Mehrteilige Komplexe = spätere Stufe.
- **Tapeten-Ecke** und **Ebenen** (Level 1/2) und **Klang-Orientierung** („Summen tiefer Richtung Ausgang“) noch nicht gebaut
  (Konzept-Stufen 3/5) – offen in Memory/Queue.
- Wände: abbaufest per Abbau-Fortschritt in der Dimension; Explosionen (TNT) können sie zerstören (Explosionsschutz ist je
  Block, nicht je Ort).
- Wolkenmeer-Generator nutzt die Vanilla-Dichte `floating_islands` mit kleinem Abzug (Inseln etwas dünner); Feintuning nach
  Spieltest.

## Verifikation
Plan-Abgleich je Stufe; `precheck.py --fix`; ein sbt-Lauf `check`, dann die zwei Modul-Targets; Ergebnis im Bericht, auch
was nicht getestet ist (kein Client-Lauf: Optik/Klang nur Vorschau, Abnahme Besitzer).

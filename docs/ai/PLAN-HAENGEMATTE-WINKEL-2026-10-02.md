# Plan Hängematte v3: beliebiger Winkel (Besitzer-Wunsch 2026-10-04)

Wunsch: „Hängematte bitte in egal welchem Winkel platzierbar machen.“ Basis: v2 (`PLAN-HAENGEMATTE-2026-10-02.md`):
gerade + 45° per Zwei-Klick, 2–4 freie Zellen, statische Blockmodelle je Layout, Zustände `facing/diagonal/gap/index`.

## Ist-Zustand
- Jeder Block der Matte rechnet die ganze Matte aus seinen Zuständen (`HammockLayout.spotOf`). Für beliebige Winkel
  reicht das nicht: (dx, dz) bis 5×5 und bis 8 Zellen je Lage ergäben ~60 000 Blockzustände (globale Palette > 2^15).
- Rendering: 6 statische Modelle je Farbe (gerade/diagonal × 2–4) + Seil-Enden; 26.3-Elemente können nur um feste
  Achsen drehen, beliebige Winkel brauchen gedrehte Quads → Block-Entity-Renderer.
- BER-Muster im Repo: `PlacedSmallPartsRenderer` (shared, 26.3-Submit-API), Registrierung Fabric
  `SimplebuildingClient`, NeoForge `SimplebuildingNeoForgeClient`, Forge 26.3 `mc26_3/forge/.../SimplebuildingForgeClient`;
  BE-Typen in `ModBlockEntities` (Fabric) / `NeoForgeModRegistries` / `ForgeModRegistries`, nullable hinter Flag.
- Vanilla 26.3 (`LevelExtractor`): BEs werden je sichtbarer Section gezeichnet, „off-screen“-BEs global ohne Frustum;
  Forge (Patch) und NeoForge (`IBlockEntityRendererExtension#getRenderBoundingBox`) cullen mit einer AABB.

## Entscheidungen
- **Grenzen**: Anker A und B auf gleicher Höhe, Versatz (dx, dz) mit max(|dx|, |dz|) − 1 = 2…4 freie Zellen entlang
  der Hauptachse – genau die alte Regel für gerade und 45°, jetzt mit jedem Nebenversatz dazwischen (euklidisch 3,0 bis
  7,07 Blöcke). **Kein Höhenunterschied**: Vanillas Liegepose ist waagrecht und der Server legt den Spieler auf eine
  Zellmitte; eine schräge Matte ließe ihn schweben oder einsinken, Tuch-Kollision und Zellen würden dreidimensional.
- **Zellen** (exakt, ganzzahlig): Seil-Lage = alle Zellen, durch deren Inneres die Linie zwischen den Ankermitten läuft
  (durch eine Gitterecke diagonal weiter, wie bisher bei 45°); Tuch-Lage (eine darunter) = Zellen, deren Inneres der
  waagrechte Tuchbereich (Mitte ± cos 22,5° + 1/16 Block) schneidet. Ergibt für gerade/45° exakt die v2-Zellen.
  Alle müssen frei (ersetzbar) sein, beide Anker Anker (wie bisher).
- **Kopfteil** = Tuchzelle mit dem Kopfpunkt (Mitte + ½ Block Richtung B; auf einer Grenze die Zelle Richtung A).
  `facing` = nächste Himmelsrichtung von A→B (bei genau 45° wie v2: B − A ∝ facing + facing.getClockWise()).
- **Speicher**: Block-Entity `HammockBlockEntity` in jeder Zelle (Tuch und Seil) mit Anker A (relativ) und (dx, dz).
  Zustände schrumpfen: Tuch `facing/part/occupied/straight` (32 je Farbe statt 384), Seil ohne Eigenschaften.
  `diagonal/gap/index` entfallen (v2 ist unveröffentlicht → keine Welt-Kompatibilität nötig).
- **Zerfall**: `intact` prüft Anker + jede Zelle (Blocktyp, gleiche Spannung, eine Farbe, Kopf an seiner Stelle);
  `updateShape` nur serverseitig (Client wartet auf den Server, BE-Daten könnten fehlen); Entfernen plant Prüfungen
  aller Zellen, Kopfteil prüft sich alle 10 Ticks (schräge Nachbarn/Anker sind oft keine Flächennachbarn). Beute nur
  am Kopfteil → Drop genau einmal, Kreativ ohne Drop (wie v2).
- **Rendering**: ein BER am Kopfteil zeichnet die ganze Matte (Tuch in zwei um 22,5° geneigten Hälften mit Säumen,
  2 Spreizhölzer, 4 Seile, 2 Knoten, 2 Bindungen in den Anker) als Quads entlang der Ankerlinie, mittig. Gerade und 45°
  ziehen mit um (eine Geometrie, keine 110 Modelldateien). Blöcke selbst: leeres Modell (nur Partikeltextur).
  `shouldRenderOffScreen` = true (Matte überspannt bis 7 Blöcke und Section-Grenzen; sonst verschwindet sie am
  Bildrand, wenn die Kopf-Section aus dem Bild ist), Sichtweite 64 ab Mattenmitte; Forge/NeoForge cullen über
  `getRenderBoundingBox` (Anker + Durchhang). Fabric/Vanilla hat für globale BEs kein Frustum-Culling – Kosten ~40 Quads.
- **Liegen**: Client-Mixin verallgemeinert: Zusatzdrehung = −(Winkel facing → Linie), Verschiebung auf den Kopfpunkt
  (Kopfpunkt − Zellmitte + Vanilla-Kopfversatz entlang der Linie statt entlang facing). Server-Position bleibt Vanilla.
- **Platzieren**: ein Klick gerade wie bisher; zwei Klicks jeder erlaubte Winkel (`between`).
- **Testzentrale**: neuer Planungsschritt `TcOp.Hammock` (verknüpft die per `Place` gesetzten Zellen, setzt BE-Daten);
  zwei schräge Matten (3:1 und 5:2) ergänzt.

## Dateien
- Shared: `HammockLayout` (Spot = Anker + dx/dz, Zellen, Kopf, Geometrie), `HammockRopeBlock`, neu
  `blocks/entity/custom/HammockBlockEntity`, neu `client/render/HammockRenderer`, `HammockLivingRendererMixin`,
  `HammockItem`, Testzentrale (`TcOp`, `TcCanvas`, `TestCentreBuilder`, `TestCentreKits`, `TestCentreLayout`,
  `TestCentreSections`), `HammockTests` (+ Katalog, Fabric-Adapter).
- 26.3 `HammockBlock` (EntityBlock, Zustand `straight`), Zwilling 26.2 (gleiche Eigenschaften).
- BE-Typ + Renderer: Fabric `ModBlockEntities`/`SimplebuildingClient`, NeoForge `ModBlockEntities`/`NeoForgeModRegistries`/
  `SimplebuildingNeoForgeClient`, Forge `ModBlockEntities`/`ForgeModRegistries`/`mc26_3/forge/.../SimplebuildingForgeClient`
  (26.2-Forge-Client nur, wenn er kompilieren muss).
- `tools/textures/hammock.py`: keine Layout-Modelle mehr, Vorschau `previews/haengematte-winkel-vorschau.png`
  (Draufsicht + Seite für 5 Winkel, gleiche Rechnung wie Java). Lang EN/DE (Tooltip), Wiki `manual.json`.

## Risiken
- BE-Daten müssen gesetzt sein, bevor Formupdates laufen → erst alle Zustände ohne Formupdate, dann BE-Daten, dann
  Nachbarn benachrichtigen (wie v2).
- Client: BE-Sync über Update-Paket; bis dahin zeichnet der Renderer nichts.
- Exakte Gitterecken: Ganzzahl-Vergleich der Kreuzungszeiten, keine Gleitkomma-Gleichheit.

## Verifikation
GameTests: Zellen für gerade/45° gleich v2; schräge Winkel (3:1, 3:2, 4:1, 5:2, 5:3) – Platzierung per zwei Klicks,
belegte Zellen = erwartete Liste, Zellen zusammenhängend, Mittigkeit (Kopfpunkt/Tuchmitte rechnerisch), Zerfall bei
Anker-/Zellverlust mit genau einem Drop, Ablehnung zu weit (6) / zu nah (2) / blockierte Zelle / Höhenunterschied;
bestehende Hängematten-Tests angepasst grün. Gates wie vorgegeben. Nicht ohne Client prüfbar: Aussehen des Renderers,
Liegepose (nur rechnerisch + Vorschau mit derselben Geometrie).

## Stand nach Umsetzung (2026-10-05)
- Umgesetzt wie geplant. Abweichung: Rods-Test prüft den Zerfall jetzt nach 2 Ticks (vorher sofort), weil die Matte
  über die BE-gestützte Prüfung fällt. Wiki-Blockrenders der Hängematte entfallen (leere Blockmodelle).
- Tests: 13 GameTests `hammock_*` (neu: `slanted_hammocks_hang_with_two_clicks`,
  `slanted_hammock_falls_once_when_its_anchor_or_rope_goes`; Mittigkeit für alle 96 Versätze inkl. gezeichneter Boxen).
  `fabric-263` + `neoforge-263` 1884/1884 „alles gruen“; Compile 26.2 + Forge 26.3 grün; `check -q` grün; Datagen ohne
  Änderung; Wiki `--all --check` grün. Vorschau `previews/haengematte-winkel-vorschau.png`.
- Nicht getestet: Client-Sicht (Renderer, Licht, Culling, gedrehte Liegepose), Forge-26.3-GameTests (nur Compile).
